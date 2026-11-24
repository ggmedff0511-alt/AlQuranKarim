/**
 * المنطق البرمجي للشاشة الرئيسية - المصحف الكوني
 */

window.onLastRead = function(b64) {
    try {
        const data = JSON.parse(window.parent.decodeSafeData(b64));
        document.getElementById('last-surah-name').innerText = "سورة " + window.parent.surahNames[data.surah];
        document.getElementById('last-ayah-info').innerText = "الآية " + window.parent.toAr(data.ayah);
    } catch(e) {}
};

window.onDailyAyah = function(b64) {
    try {
        const data = JSON.parse(window.parent.decodeSafeData(b64));
        if(data && data.text) {
            document.getElementById('daily-ayah-text').innerText = "﴿ " + data.text + " ﴾";
            document.getElementById('daily-ayah-ref').innerText = data.ref;
        }
    } catch(e) {}
};

/**
 * تعليق جراحي: دوال استقبال البيانات المضافة حديثاً (التوقيت والتاريخ الهجري)
 * لضمان عرض المحتوى المفقود في الشاشة الرئيسية.
 */
window.onPrayerTimesLoaded = function(jsonStr) {
    try {
        const data = JSON.parse(jsonStr);
        document.getElementById('next-prayer-name').innerText = "صلاة " + data.next;
        document.getElementById('next-prayer-time').innerText = window.parent.toAr(data[data.activeKey] || "--:--");
        document.getElementById('city-name-display').innerText = data.city;
    } catch(e) { console.error("Prayer Update Error", e); }
};

window.onHijriDateReceived = function(dateStr) {
    try {
        document.getElementById('hijri-date-display').innerText = dateStr;
    } catch(e) {}
};

window.onload = function() {
    if (window.parent && window.parent.AndroidBridge) {
        // تعليق جراحي: استدعاء الجسور الأصلية للحصول على البيانات المفقودة فور التحميل
        window.parent.AndroidBridge.getLastRead();
        window.parent.AndroidBridge.getDailyAyah();
        window.parent.AndroidBridge.getPrayerTimes('onPrayerTimesLoaded');
        window.parent.AndroidBridge.getHijriDate('onHijriDateReceived');
    }

    checkSleepAzkarVisibility();
};

function checkSleepAzkarVisibility() {
    try {
        var now = new Date();
        var hour = now.getHours();
        var today = now.toDateString();
        var lastShown = localStorage.getItem('last_sleep_azkar_date');
        var wrapper = document.getElementById('sleep-azkar-wrapper');

        if (hour >= 21 && lastShown !== today) {
            if (wrapper) {
                wrapper.style.display = 'block';
                localStorage.setItem('last_sleep_azkar_date', today);

                setTimeout(function() {
                    wrapper.style.display = 'none';
                }, 5000);
            }
        }
    } catch (e) { console.error("Sleep Azkar logic error", e); }
}
