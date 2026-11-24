package com.bahnabwi.alqurankarim;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * اختبار شاشة مواقيت الصلاة والقبلة - المصحف الكوني
 * تعليق جراحي: تم إنشاء هذا الاختبار للتحقق من سلامة عرض عناصر واجهة مواقيت الصلاة
 * وضمان عدم تعطل الشاشة عند التشغيل.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class PrayerTimesActivityTest {

    @Rule
    public ActivityScenarioRule<PrayerTimesActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(PrayerTimesActivity.class);

    /**
     * التحقق من ظهور العناصر الأساسية في شاشة مواقيت الصلاة
     */
    @Test
    public void prayerTimesElementsVisibilityTest() {
        // التأكد من ظهور حاوية الهيدر وزر الرجوع
        onView(withId(R.id.headerPrayer)).check(matches(isDisplayed()));
        onView(withId(R.id.btnBackPrayer)).check(matches(isDisplayed()));

        // التحقق من ظهور التاريخ واسم المدينة
        onView(withId(R.id.tvDate)).check(matches(isDisplayed()));
        onView(withId(R.id.tvCity)).check(matches(isDisplayed()));

        // التحقق من ظهور قائمة مواقيت الصلاة
        onView(withId(R.id.gridPrayerTimes)).check(matches(isDisplayed()));

        // التحقق من ظهور عناصر البوصلة والقبلة
        onView(withId(R.id.tvQiblaTitle)).check(matches(isDisplayed()));
        onView(withId(R.id.ivCompassBase)).check(matches(isDisplayed()));
        onView(withId(R.id.ivCompassNeedle)).check(matches(isDisplayed()));
    }
}
