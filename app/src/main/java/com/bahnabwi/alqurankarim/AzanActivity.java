package com.bahnabwi.alqurankarim;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * شاشة الأذان بملء الشاشة - المصحف الكوني
 * تعليق جراحي: تم إضافة ساعة رقمية تحدث نفسها لضمان تجربة منبه حقيقية بنظام 12 ساعة.
 */
public class AzanActivity extends AppCompatActivity {
    private TextView tvClock;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            if (tvClock != null) {
                // تعليق جراحي: تحديث الساعة بنظام 12 ساعة مع الثواني لضمان حيوية الشاشة
                tvClock.setText(new SimpleDateFormat("hh:mm:ss a", new Locale("ar")).format(new Date()));
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // تعليق جراحي: ضمان بقاء الشاشة مضاءة دائماً أثناء الأذان والسماح بالظهور فوق القفل لضمان تنبيه المستخدم
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null) km.requestDismissKeyguard(this, null);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        setContentView(R.layout.activity_azan);

        tvClock = findViewById(R.id.tvAzanMessage); // استخدام حقل الرسالة لعرض الساعة الحية
        String prayerName = getIntent().getStringExtra("prayerName");
        TextView tvPrayerName = findViewById(R.id.tvAzanPrayerName);
        if (tvPrayerName != null) tvPrayerName.setText(prayerName != null ? prayerName : "الصلاة");

        handler.post(clockRunnable);

        MaterialButton btnStop = findViewById(R.id.btnStopAzan);
        btnStop.setOnClickListener(v -> {
            Intent stopIntent = new Intent(this, AzanService.class);
            stopIntent.setAction("STOP_AZAN");
            startService(stopIntent);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(clockRunnable);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // تعليق جراحي: منع العودة بالزر الخلفي لإجبار المستخدم على الضغط على "إيقاف الأذان" لضمان استقرار الخدمة
    }
}
