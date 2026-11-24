package com.bahnabwi.alqurankarim;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class SalatReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "SalatNabiReminder";
    public static final String ACTION_SALAT_REMINDER = "com.bahnabwi.alqurankarim.ACTION_SALAT_REMINDER";
    private static final String TAG = "SalatReminderReceiver";
    private static final String PREFS_NAME = "quran_prefs";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive: " + action);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        boolean enabled = false;
        try {
            enabled = prefs.getBoolean("salat_nabi_notify", false);
        } catch (Exception e) {
            try {
                enabled = Boolean.parseBoolean(prefs.getString("salat_nabi_notify", "false"));
            } catch (Exception ignored) {}
        }

        int interval = 15;
        try {
            interval = prefs.getInt("salat_nabi_interval", 15);
        } catch (Exception e) {
            try {
                interval = Integer.parseInt(prefs.getString("salat_nabi_interval", "15"));
            } catch (Exception ignored) {}
        }

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || 
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
            Intent.ACTION_TIME_CHANGED.equals(action) ||
            Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            
            if (enabled) {
                Log.d(TAG, "Re-scheduling reminder after system event");
                scheduleReminder(context, interval);
            }
            return;
        }

        if (ACTION_SALAT_REMINDER.equals(action)) {
            if (enabled) {
                showNotification(context);
                playReminderSound(context);
                // إعادة الجدولة التلقائية للتذكير التالي
                scheduleReminder(context, interval);
            }
        }
    }

    private void showNotification(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "تذكير بالصلاة على النبي", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("تذكيرات دورية للصلاة على النبي ﷺ");
            nm.createNotificationChannel(channel);
        }

        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pi = PendingIntent.getActivity(context, 2002, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("تذكير بالصلاة على النبي ﷺ")
                .setContentText("اللهم صل وسلم على نبينا محمد")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(pi);

        nm.notify(2002, builder.build());
    }

    private void playReminderSound(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String soundId = prefs.getString("salat_nabi_sound", "salli_ala_mohammad");
            
            if (soundId.contains("/")) soundId = soundId.substring(soundId.lastIndexOf("/") + 1);
            
            // تحويل الأسماء العربية القديمة لضمان العمل
            if (soundId.contains("نغمة") || soundId.equals("default")) soundId = "salli_ala_mohammad";
            else if (soundId.contains("الصلاة") || soundId.contains("ماهر")) soundId = "salat_maher";
            else if (soundId.contains("إن_الله") || soundId.contains("ياسر")) soundId = "in_allah_yasser";

            int resId = context.getResources().getIdentifier(soundId, "raw", context.getPackageName());
            if (resId == 0) resId = R.raw.salli_ala_mohammad;

            MediaPlayer mp = MediaPlayer.create(context, resId);
            if (mp != null) {
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing reminder sound", e);
        }
    }

    public static void scheduleReminder(Context context, int intervalMinutes) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, SalatReminderReceiver.class);
        intent.setAction(ACTION_SALAT_REMINDER);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // حساب وقت التنبيه القادم بالدقائق
        long intervalMillis = (long) Math.max(1, intervalMinutes) * 60 * 1000L;
        long triggerTime = System.currentTimeMillis() + intervalMillis;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
            Log.d(TAG, "Scheduled next reminder in " + intervalMinutes + " minutes");
        } catch (SecurityException e) {
            // حل بديل في حال عدم توفر إذن التنبيهات الدقيقة
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            Log.w(TAG, "Exact alarm permission missing, used non-exact fallback");
        }
    }

    public static void cancelReminder(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SalatReminderReceiver.class);
        intent.setAction(ACTION_SALAT_REMINDER);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Cancelled salat reminders");
        }
    }
}
