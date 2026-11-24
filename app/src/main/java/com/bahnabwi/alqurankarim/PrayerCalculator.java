package com.bahnabwi.alqurankarim;

import android.content.Context;
import android.content.SharedPreferences;
import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.CalculationParameters;
import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.Madhab;
import com.batoulapps.adhan.Prayer;
import com.batoulapps.adhan.PrayerTimes;
import com.batoulapps.adhan.data.DateComponents;
import java.util.Calendar;
import java.util.Date;

public class PrayerCalculator {

    public static PrayerTimes getTimes(double lat, double lng) {
        return getTimes(lat, lng, new Date());
    }

    /**
     * تعليق جراحي: العودة للمواقيت الحقيقية وإلغاء إزاحة الاختبار السابقة لضمان دقة الأذان.
     */
    public static PrayerTimes getTimes(double lat, double lng, Date date) {
        Coordinates coordinates = new Coordinates(lat, lng);
        DateComponents dateComponents = DateComponents.from(date);
        
        // استخدام طريقة الهيئة العامة المصرية للمساحة (الافتراضية للأوقات في مصر)
        CalculationParameters parameters = CalculationMethod.EGYPTIAN.getParameters();
        parameters.madhab = Madhab.SHAFI;

        // تعليق جراحي: تم إزالة تعديلات الاختبار (adjustments.fajr) ليعمل الأذان في وقته الحقيقي تماماً
        return new PrayerTimes(coordinates, dateComponents, parameters);
    }

    public static long getNextPrayerTimeMillis(double lat, double lng) {
        PrayerTimes today = getTimes(lat, lng);
        Date now = new Date();
        
        Prayer[] prayers = {Prayer.FAJR, Prayer.SUNRISE, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA};
        for (Prayer p : prayers) {
            Date time = today.timeForPrayer(p);
            if (time != null && time.after(now)) return time.getTime();
        }

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        Date tomorrowDate = cal.getTime();
        PrayerTimes tomorrow = getTimes(lat, lng, tomorrowDate);
        Date fajrTomorrow = tomorrow.timeForPrayer(Prayer.FAJR);
        return fajrTomorrow != null ? fajrTomorrow.getTime() : 0;
    }
    
    public static String getPrayerName(long millis, double lat, double lng) {
        if (millis == 0) return "الصلاة";
        PrayerTimes times = getTimes(lat, lng, new Date(millis));
        if (times.timeForPrayer(Prayer.FAJR) != null && Math.abs(times.timeForPrayer(Prayer.FAJR).getTime() - millis) < 60000) return "الفجر";
        if (times.timeForPrayer(Prayer.SUNRISE) != null && Math.abs(times.timeForPrayer(Prayer.SUNRISE).getTime() - millis) < 60000) return "الشروق";
        if (times.timeForPrayer(Prayer.DHUHR) != null && Math.abs(times.timeForPrayer(Prayer.DHUHR).getTime() - millis) < 60000) return "الظهر";
        if (times.timeForPrayer(Prayer.ASR) != null && Math.abs(times.timeForPrayer(Prayer.ASR).getTime() - millis) < 60000) return "العصر";
        if (times.timeForPrayer(Prayer.MAGHRIB) != null && Math.abs(times.timeForPrayer(Prayer.MAGHRIB).getTime() - millis) < 60000) return "المغرب";
        if (times.timeForPrayer(Prayer.ISHA) != null && Math.abs(times.timeForPrayer(Prayer.ISHA).getTime() - millis) < 60000) return "العشاء";
        return "الصلاة";
    }

    public static String getPrayerKey(long millis, double lat, double lng) {
        if (millis == 0) return "fajr";
        PrayerTimes times = getTimes(lat, lng, new Date(millis));
        if (times.timeForPrayer(Prayer.FAJR) != null && Math.abs(times.timeForPrayer(Prayer.FAJR).getTime() - millis) < 60000) return "fajr";
        if (times.timeForPrayer(Prayer.SUNRISE) != null && Math.abs(times.timeForPrayer(Prayer.SUNRISE).getTime() - millis) < 60000) return "shurooq";
        if (times.timeForPrayer(Prayer.DHUHR) != null && Math.abs(times.timeForPrayer(Prayer.DHUHR).getTime() - millis) < 60000) return "dhuhr";
        if (times.timeForPrayer(Prayer.ASR) != null && Math.abs(times.timeForPrayer(Prayer.ASR).getTime() - millis) < 60000) return "asr";
        if (times.timeForPrayer(Prayer.MAGHRIB) != null && Math.abs(times.timeForPrayer(Prayer.MAGHRIB).getTime() - millis) < 60000) return "maghrib";
        if (times.timeForPrayer(Prayer.ISHA) != null && Math.abs(times.timeForPrayer(Prayer.ISHA).getTime() - millis) < 60000) return "isha";
        return "fajr";
    }
}
