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
                // تعليق جراحي: تغيير نظام الساعة إلى 12 ساعة (hh:mm:ss a) بدلاً من 24 ساعة
                tvClock.setText(new SimpleDateFormat("hh:mm:ss a", new Locale("ar")).format(new Date()));
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // تعليق جراحي: السماح للنشاط بالظهور فوق قفل الشاشة وتشغيل الشاشة تلقائياً
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null) km.requestDismissKeyguard(this, null);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        setContentView(R.layout.activity_azan);

        tvClock = findViewById(R.id.tvAzanMessage); // سنستخدم هذا الحقل للساعة
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
        // منع العودة لإجبار المستخدم على التفاعل مع زر "إيقاف الأذان"
    }
}
