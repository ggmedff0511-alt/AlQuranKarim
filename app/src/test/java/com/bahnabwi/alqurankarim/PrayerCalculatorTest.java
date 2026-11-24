package com.bahnabwi.alqurankarim;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.batoulapps.adhan.PrayerTimes;

import org.junit.Test;

/**
 * اختبار وحدة لمنطق حساب مواقيت الصلاة - المصحف الكوني
 * تعليق جراحي: التأكد من أن مكتبة Adhan تعمل بشكل صحيح مع الإحداثيات المدخلة
 */
public class PrayerCalculatorTest {

    @Test
    public void testPrayerTimesCalculation() {
        // إحداثيات القاهرة كمثال
        double lat = 30.0444;
        double lng = 31.2357;

        PrayerTimes times = PrayerCalculator.getTimes(lat, lng);

        assertNotNull("يجب ألا تكون مواقيت الصلاة فارغة", times);
        assertNotNull("الفجر يجب أن يكون له وقت", times.fajr);
        assertNotNull("المغرب يجب أن يكون له وقت", times.maghrib);
    }

    @Test
    public void testNextPrayerLogic() {
        double lat = 30.0444;
        double lng = 31.2357;

        long nextTime = PrayerCalculator.getNextPrayerTimeMillis(lat, lng);
        
        assertTrue("وقت الصلاة القادمة يجب أن يكون أكبر من 0", nextTime > 0);
    }
}
