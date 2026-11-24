package com.bahnabwi.alqurankarim;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.splashscreen.SplashScreen;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        // التعامل مع الحواف (Insets) للجهاز
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // ربط العناصر الجديدة (البطاقات)
        View btnDailyWerd = findViewById(R.id.btnDailyWerd);
        View btnReadQuran = findViewById(R.id.btnReadQuran);
        View btnListen = findViewById(R.id.btnListen);
        View btnAzkar = findViewById(R.id.btnAzkar);
        View btnPrayerTimes = findViewById(R.id.btnPrayerTimes);
        View btnLibrary = findViewById(R.id.btnLibrary);
        TextView tvPrivacyPolicy = findViewById(R.id.tvPrivacyPolicy);

        // التنقل بين الشاشات
        if (btnDailyWerd != null) {
            btnDailyWerd.setOnClickListener(v -> {
                startActivity(new Intent(HomeActivity.this, DailyWerdActivity.class));
            });
        }

        if (btnReadQuran != null) {
            btnReadQuran.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                intent.putExtra("start_page", "index"); 
                startActivity(intent);
            });
        }

        if (btnListen != null) {
            btnListen.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                intent.putExtra("start_page", "reciters"); 
                startActivity(intent);
            });
        }

        if (btnAzkar != null) {
            btnAzkar.setOnClickListener(v -> {
                startActivity(new Intent(HomeActivity.this, AzkarActivity.class));
            });
        }

        if (btnPrayerTimes != null) {
            btnPrayerTimes.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                intent.putExtra("start_page", "prayer");
                startActivity(intent);
            });
        }

        if (btnLibrary != null) {
            btnLibrary.setOnClickListener(v -> {
                startActivity(new Intent(HomeActivity.this, HadithLibraryActivity.class));
            });
        }

        if (tvPrivacyPolicy != null) {
            tvPrivacyPolicy.setOnClickListener(v -> {
                startActivity(new Intent(HomeActivity.this, PrivacyPolicyActivity.class));
            });
        }
    }
}
