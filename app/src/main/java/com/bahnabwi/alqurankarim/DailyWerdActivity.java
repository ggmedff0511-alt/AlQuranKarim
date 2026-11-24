package com.bahnabwi.alqurankarim;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class DailyWerdActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_werd);

        // إعداد زر الرجوع (Menu icon used as back in this context or close)
        ImageView btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> finish());

        // إعداد أزرار التنقل السفلي (يمكن ربطها بباقي الشاشات لاحقاً)
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        // هنا يمكن إضافة منطق التنقل بين الشاشات
        // حالياً سنقوم فقط بتمييز أيقونة المواقيت كأنها الشاشة النشطة بناءً على التصميم
    }
}
