package com.bahnabwi.alqurankarim;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AzkarActivity extends AppCompatActivity {

    private RecyclerView rvAzkar;
    private AzkarAdapter adapter;
    private TextView tvTitle;
    private SharedPreferences prefs;

    private LinearLayout layoutMorning, layoutEvening, layoutSleep;
    private ImageView ivMorningLogo, ivEveningLogo, ivSleepLogo;
    private TextView tvMorningState, tvEveningState, tvSleepState;
    private MaterialButton btnQuickStart;

    private enum AzkarType { MORNING, EVENING, SLEEP, NONE }
    private AzkarType recommendedType = AzkarType.MORNING;
    private ObjectAnimator pulseAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_azkar);

        prefs = getSharedPreferences("AzkarPrefs", Context.MODE_PRIVATE);

        tvTitle = findViewById(R.id.tvAzkarTitle);
        rvAzkar = findViewById(R.id.rvAzkar);
        rvAzkar.setLayoutManager(new LinearLayoutManager(this));

        // تهيئة لوحة التحكم العلوية
        layoutMorning = findViewById(R.id.layoutMorning);
        layoutEvening = findViewById(R.id.layoutEvening);
        layoutSleep = findViewById(R.id.layoutSleep);
        
        ivMorningLogo = findViewById(R.id.ivMorningLogo);
        ivEveningLogo = findViewById(R.id.ivEveningLogo);
        ivSleepLogo = findViewById(R.id.ivSleepLogo);

        tvMorningState = findViewById(R.id.tvMorningState);
        tvEveningState = findViewById(R.id.tvEveningState);
        tvSleepState = findViewById(R.id.tvSleepState);
        btnQuickStart = findViewById(R.id.btnQuickStart);

        findViewById(R.id.btnBackAzkar).setOnClickListener(v -> finish());

        // التصنيفات السفلية
        findViewById(R.id.btnSabah).setOnClickListener(v -> loadAzkarWithType(AzkarType.MORNING));
        findViewById(R.id.btnMasaa).setOnClickListener(v -> loadAzkarWithType(AzkarType.EVENING));
        findViewById(R.id.btnSleep).setOnClickListener(v -> loadAzkarWithType(AzkarType.SLEEP));
        findViewById(R.id.btnWake).setOnClickListener(v -> loadAzkar(AzkarData.getWakeUpAzkar(), getString(R.string.azkar_wake_up), AzkarType.NONE));
        findViewById(R.id.btnPray).setOnClickListener(v -> loadAzkar(AzkarData.getPrayerAzkar(), getString(R.string.azkar_prayer), AzkarType.NONE));

        // جعل الشعارات العلوية قابلة للضغط
        layoutMorning.setOnClickListener(v -> loadAzkarWithType(AzkarType.MORNING));
        layoutEvening.setOnClickListener(v -> loadAzkarWithType(AzkarType.EVENING));
        layoutSleep.setOnClickListener(v -> loadAzkarWithType(AzkarType.SLEEP));

        updateHubUI();

        btnQuickStart.setOnClickListener(v -> loadAzkarWithType(recommendedType));

        // التحميل الافتراضي بناءً على التوصية
        loadRecommendedDefault();
    }

    private void updateHubUI() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // تحديث حالة "تمت القراءة"
        updateStateText(tvMorningState, "morning", today);
        updateStateText(tvEveningState, "evening", today);
        updateStateText(tvSleepState, "sleep", today);

        // إعادة تعيين التصميم للجميع
        resetHubDesign();

        // تمييز التوصية حسب الوقت مع أنيميشن
        if (hour >= 4 && hour < 12) {
            recommendedType = AzkarType.MORNING;
            highlightLayout(layoutMorning, ivMorningLogo);
        } else if (hour >= 12 && hour < 21) {
            recommendedType = AzkarType.EVENING;
            highlightLayout(layoutEvening, ivEveningLogo);
        } else {
            recommendedType = AzkarType.SLEEP;
            highlightLayout(layoutSleep, ivSleepLogo);
        }

        // تحديث نص زر البدء السريع
        if (isReadToday(getPrefsKey(recommendedType), today)) {
            btnQuickStart.setText("أعد قراءة أذكار الوقت الحالي");
            btnQuickStart.setAlpha(0.8f);
        } else {
            btnQuickStart.setText(getString(R.string.azkar_quick_start));
            btnQuickStart.setAlpha(1.0f);
        }
    }

    private void updateStateText(TextView tv, String type, String today) {
        if (isReadToday("last_read_" + type, today)) {
            tv.setText(R.string.azkar_read);
            tv.setTextColor(0xFF4CAF50); // أخضر
        } else {
            tv.setText(R.string.azkar_unread);
            tv.setTextColor(0x88FFFFFF);
        }
    }

    private boolean isReadToday(String key, String today) {
        return today.equals(prefs.getString(key, ""));
    }

    private void resetHubDesign() {
        if (pulseAnimator != null) pulseAnimator.cancel();
        
        layoutMorning.setAlpha(0.4f); layoutMorning.setScaleX(1.0f); layoutMorning.setScaleY(1.0f);
        layoutEvening.setAlpha(0.4f); layoutEvening.setScaleX(1.0f); layoutEvening.setScaleY(1.0f);
        layoutSleep.setAlpha(0.4f); layoutSleep.setScaleX(1.0f); layoutSleep.setScaleY(1.0f);
        
        ivMorningLogo.setBackgroundResource(R.drawable.bg_stat_item);
        ivEveningLogo.setBackgroundResource(R.drawable.bg_stat_item);
        ivSleepLogo.setBackgroundResource(R.drawable.bg_stat_item);
    }

    private void highlightLayout(LinearLayout layout, ImageView logo) {
        layout.setAlpha(1.0f);
        logo.setBackgroundResource(R.drawable.bg_tab_selected); // خلفية مضيئة
        
        // أنيميشن النبض
        pulseAnimator = ObjectAnimator.ofFloat(layout, "scaleX", 1.05f, 1.15f);
        pulseAnimator.setDuration(1200);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        
        ObjectAnimator pulseY = ObjectAnimator.ofFloat(layout, "scaleY", 1.05f, 1.15f);
        pulseY.setDuration(1200);
        pulseY.setRepeatCount(ValueAnimator.INFINITE);
        pulseY.setRepeatMode(ValueAnimator.REVERSE);
        
        pulseAnimator.start();
        pulseY.start();
    }

    private void loadAzkarWithType(AzkarType type) {
        switch (type) {
            case MORNING: loadAzkar(AzkarData.getMorningAzkar(), getString(R.string.azkar_sabah), AzkarType.MORNING); break;
            case EVENING: loadAzkar(AzkarData.getEveningAzkar(), getString(R.string.azkar_masaa), AzkarType.EVENING); break;
            case SLEEP: loadAzkar(AzkarData.getSleepAzkar(), getString(R.string.azkar_sleep), AzkarType.SLEEP); break;
        }
    }

    private void loadRecommendedDefault() {
        loadAzkarWithType(recommendedType);
    }

    private void loadAzkar(List<AzkarData.Zekr> list, String title, AzkarType type) {
        tvTitle.setText(title);
        adapter = new AzkarAdapter(list);
        rvAzkar.setAdapter(adapter);

        // عند فتح الأذكار الأساسية، نعتبرها "تمت القراءة" لهذا اليوم
        if (type != AzkarType.NONE) {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            prefs.edit().putString(getPrefsKey(type), today).apply();
            updateHubUI();
        }
    }

    private String getPrefsKey(AzkarType type) {
        switch (type) {
            case MORNING: return "last_read_morning";
            case EVENING: return "last_read_evening";
            case SLEEP: return "last_read_sleep";
            default: return "";
        }
    }
}
