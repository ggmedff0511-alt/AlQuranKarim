package com.bahnabwi.alqurankarim;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

/**
 * مستقبل تنبيهات الأذان المطور - المصحف الكوني
 * تعليق جراحي: تم دعم التنبيه المسبق (5 دقائق) والأذان الفعلي مع رسائل مخصصة ومعالجة أحداث النظام (إعادة التشغيل).
 */
public class PrayerAlarmReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "PrayerNotifications_v6";
    public static final String ACTION_PRAYER_ALARM = "com.bahnabwi.alqurankarim.ACTION_PRAYER_ALARM";
    private static final String TAG = "PrayerAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        // تعليق جراحي: إعادة جدولة التنبيهات عند إعادة التشغيل أو تغيير الوقت لضمان عدم ضياعها
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || 
            Intent.ACTION_TIMEZONE_CHANGED.equals(action) || 
            "android.intent.action.TIME_SET".equals(action)) {
            
            Log.d(TAG, "إعادة جدولة المواقيت بسبب حدث نظام: " + action);
            PrayerScheduler.scheduleNextPrayer(context);
            return;
        }

        if (!ACTION_PRAYER_ALARM.equals(action)) return;

        SharedPreferences prefs = context.getSharedPreferences("quran_prefs", Context.MODE_PRIVATE);
        // التحقق من تفعيل التنبيهات في الإعدادات
        if (!prefs.getBoolean("azan_notify", true)) return;

        String prayerName = intent.getStringExtra("prayerName");
        boolean isReminder = intent.getBooleanExtra("isReminder", false);
        if (prayerName == null) prayerName = "الصلاة";

        Log.d(TAG, "تم استقبال " + (isReminder ? "تنبيه مسبق" : "أذان") + " لصلاة " + prayerName);

        if (isReminder) {
            // إظهار إشعار التذكير قبل الأذان بـ 5 دقائق
            showNotification(context, "تذكير بالصلاة", "باقي على صلاة " + prayerName + " ٥ دقائق", true);
        } else {
            // تشغيل صوت الأذان الحقيقي وشاشة الأذان
            Intent serviceIntent = new Intent(context, AzanService.class);
            serviceIntent.putExtra("prayerName", prayerName);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            } catch (Exception e) {
                Log.e(TAG, "خطأ في بدء خدمة الأذان: " + e.getMessage());
            }
            showNotification(context, "موعد أذان " + prayerName, "حان وقت الصلاة، الله أكبر.", false);
        }
        
        // جدولة الحدث التالي
        PrayerScheduler.scheduleNextPrayer(context);
    }

    private void showNotification(Context context, String title, String message, boolean isReminder) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = nm.getNotificationChannel(CHANNEL_ID);
            if (channel == null) {
                channel = new NotificationChannel(CHANNEL_ID, "تنبيهات الصلاة", NotificationManager.IMPORTANCE_HIGH);
                channel.enableVibration(false);
                channel.setSound(null, null);
                channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                nm.createNotificationChannel(channel);
            }
        }

        Intent ni = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, ni, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_clock)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(isReminder ? NotificationCompat.CATEGORY_REMINDER : NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pi, !isReminder)
                .setAutoCancel(true);

        nm.notify(isReminder ? 1002 : 1001, builder.build());
    }
}
