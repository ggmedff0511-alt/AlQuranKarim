package com.bahnabwi.alqurankarim;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import com.batoulapps.adhan.Prayer;
import com.batoulapps.adhan.PrayerTimes;
import java.util.Calendar;
import java.util.Date;

/**
 * مجدول مواقيت الصلاة المطور - المصحف الكوني
 * يدعم التنبيه قبل الأذان بـ 5 دقائق والأذان الفعلي.
 */
public class PrayerScheduler {
    private static final String TAG = "PrayerScheduler";
    private static final String PREFS_NAME = "quran_prefs";

    public static void scheduleNextPrayer(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean("azan_notify", true);
        
        if (!enabled) {
            cancelAllAlarms(context);
            return;
        }

        float lat = prefs.getFloat("last_lat", 30.0444f);
        float lng = prefs.getFloat("last_lng", 31.2357f);

        long targetTime = 0;
        String prayerName = "";
        boolean isReminder = false;
        long now = System.currentTimeMillis();
        
        PrayerTimes today = PrayerCalculator.getTimes(lat, lng);
        Prayer[] prayers = {Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA};
        
        for (Prayer p : prayers) {
            Date pDate = today.timeForPrayer(p);
            if (pDate != null) {
                long actualPrayerTime = pDate.getTime();
                long reminderTime = actualPrayerTime - (5 * 60 * 1000); // 5 دقائق قبل الصلاة

                // تعليق جراحي: التحقق أولاً من التنبيه المسبق (5 دقائق)
                if (reminderTime > now + 2000) {
                    targetTime = reminderTime;
                    prayerName = PrayerCalculator.getPrayerName(actualPrayerTime, lat, lng);
                    isReminder = true;
                    break;
                } 
                // إذا فات وقت التنبيه المسبق، نجدول الأذان الفعلي
                else if (actualPrayerTime > now + 2000) {
                    targetTime = actualPrayerTime;
                    prayerName = PrayerCalculator.getPrayerName(actualPrayerTime, lat, lng);
                    isReminder = false;
                    break;
                }
            }
        }

        // إذا انتهت صلوات اليوم، نبحث في صلوات الغد (بدءاً بتنبيه الفجر)
        if (targetTime == 0) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, 1);
            PrayerTimes tomorrow = PrayerCalculator.getTimes(lat, lng, cal.getTime());
            Date fajrTomorrow = tomorrow.timeForPrayer(Prayer.FAJR);
            if (fajrTomorrow != null) {
                long actualFajr = fajrTomorrow.getTime();
                long reminderFajr = actualFajr - (5 * 60 * 1000);
                if (reminderFajr > now) {
                    targetTime = reminderFajr;
                    isReminder = true;
                } else {
                    targetTime = actualFajr;
                    isReminder = false;
                }
                prayerName = "الفجر";
            }
        }

        if (targetTime > 0) {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, PrayerAlarmReceiver.class);
            intent.setAction(PrayerAlarmReceiver.ACTION_PRAYER_ALARM);
            intent.putExtra("prayerName", prayerName);
            intent.putExtra("isReminder", isReminder);
            
            PendingIntent pi = PendingIntent.getBroadcast(context, 1002, intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            if (am != null) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        AlarmManager.AlarmClockInfo alarmInfo = new AlarmManager.AlarmClockInfo(targetTime, pi);
                        am.setAlarmClock(alarmInfo, pi);
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTime, pi);
                    } else {
                        am.setExact(AlarmManager.RTC_WAKEUP, targetTime, pi);
                    }
                    Log.d(TAG, "تم جدولة " + (isReminder ? "تنبيه (5د)" : "أذان") + " " + prayerName + " في " + new Date(targetTime));
                } catch (Exception e) {
                    am.set(AlarmManager.RTC_WAKEUP, targetTime, pi);
                }
            }
        }
    }

    public static void cancelAllAlarms(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, PrayerAlarmReceiver.class);
        intent.setAction(PrayerAlarmReceiver.ACTION_PRAYER_ALARM);
        PendingIntent pi = PendingIntent.getBroadcast(context, 1002, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (am != null) am.cancel(pi);
    }
}
