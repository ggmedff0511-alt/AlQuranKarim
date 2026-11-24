/**
 * المنطق البرمجي لشاشة مواقيت الصلاة والقبلة - المصحف الكوني
 * تحديث جراحي: إصلاح جسر التواصل لضمان ظهور البيانات والمؤقت حقيقياً
 */

window.goBack = () => {
    if (window.parent && window.parent.loadPage) {
        window.parent.loadPage('home');
    } else {
        window.history.back();
    }
};

function refreshData() {
    // تعديل جراحي: استخدام callNative المتاحة في النطاق الحالي (عبر script.js)
    if (typeof callNative === 'function') {
        callNative('requestLocationUpdate');
        callNative('getPrayerTimes', 'onPrayerTimesLoaded');
        // توحيد الاسم مع الجسر المعتمد في quran_ui.js
        callNative('getHijriDate', 'onHijriDateReceived');
    }
}

/** وظيفة الانتقال للواجهة الأصلية عبر الجسر المطور */
window.openNativePrayer = function() {
    if (typeof callNative === 'function') {
        callNative('openPrayerTimesNative');
    }
};

/** ميزة تجربة صوت الأذان يدوياً */
window.testAzanSound = function() {
    if (typeof callNative === 'function') {
        callNative('playAzanSound');
        callNative('showToast', 'جاري تجربة صوت الأذان الحقيقي...');
    }
};

function createTicks() {
    const container = document.getElementById('ticks-container');
    if(!container) return;
    container.innerHTML = '';
    for(let i=0; i<360; i+=30) {
        const tick = document.createElement('div');
        tick.className = 'tick' + (i % 90 === 0 ? ' major' : '');
        tick.style.transform = `translateX(-50%) rotate(${i}deg)`;
        container.appendChild(tick);
    }
}

window.onload = () => {
    createTicks();
    refreshData();
    if (typeof callNative === 'function') callNative('startQiblaUpdate');
};

window.onbeforeunload = () => {
    if (typeof callNative === 'function') callNative('stopQiblaUpdate');
};

/**
 * دالة استقبال المواقيت - تم التأكد من مطابقتها لما يرسله MainActivity
 */
window.onPrayerTimesLoaded = (json) => {
    try {
        const data = (typeof json === 'string') ? JSON.parse(json) : json;

        // تحديث أوقات الصلاة في الشبكة
        const keys = ['fajr', 'shurooq', 'dhuhr', 'asr', 'maghrib', 'isha'];
        keys.forEach(k => {
            const el = document.getElementById('time-' + k);
            if(el) el.innerText = data[k] || "--:--";
        });

        // تحديث عنوان الصلاة القادمة
        if(data.next) document.getElementById('next-prayer-title').innerText = "صلاة " + data.next;

        // تحديث اسم المدينة المكتشفة حقيقياً
        const cityEl = document.getElementById('city-text');
        if (cityEl) cityEl.innerText = data.city || 'القاهرة، مصر';

        // تفعيل العداد التنازلي الحقيقي
        azanPlayedForCurrent = false;
        startCountdown(data.countdown);

        // تمييز الصلاة القادمة في الشبكة بصرياً
        document.querySelectorAll('.p-grid-item').forEach(el => el.classList.remove('active'));
        const activeId = 'card-' + (data.activeKey || 'fajr');
        if(document.getElementById(activeId)) document.getElementById(activeId).classList.add('active');

    } catch(e) { console.error("Error updating prayer UI", e); }
};

/**
 * دالة استقبال التاريخ الهجري الموحدة
 */
window.onHijriDateReceived = (date) => {
    const el = document.getElementById('hijri-date-val');
    if (el) el.innerText = date;
};

/**
 * تحديث اتجاه القبلة بناءً على مستشعرات الموبايل
 */
window.onQiblaUpdate = (deg) => {
    const needle = document.getElementById('main-needle-pointer');
    const valEl = document.getElementById('qibla-val-text');
    if(needle) {
        needle.style.transform = `translateX(-50%) rotate(${deg}deg)`;
        if (Math.abs(deg) <= 5 || Math.abs(deg) >= 355) {
            needle.classList.add('qibla-correct');
            if (valEl) valEl.style.color = "#00FF00";
            if (typeof callNative === 'function') callNative('vibrate', 30);
        } else {
            needle.classList.remove('qibla-correct');
            if (valEl) valEl.style.color = "#E8C172";
        }
    }
    if (valEl) valEl.innerText = Math.round(deg) + "°";
};

let timerInterval;
let azanPlayedForCurrent = false;

function startCountdown(seconds) {
    clearInterval(timerInterval);
    if (!seconds || seconds <= 0) {
        document.getElementById('countdown-timer').innerText = "00:00:00";
        return;
    }

    let timeLeft = Math.floor(seconds);
    const el = document.getElementById('countdown-timer');

    timerInterval = setInterval(() => {
        if (timeLeft <= 0) {
            el.innerText = "00:00:00";
            if (!azanPlayedForCurrent && typeof callNative === 'function') {
                callNative('playAzanSound');
                azanPlayedForCurrent = true;
            }
            clearInterval(timerInterval);
            setTimeout(refreshData, 5000);
            return;
        }

        const h = Math.floor(timeLeft / 3600);
        const m = Math.floor((timeLeft % 3600) / 60);
        const s = timeLeft % 60;
        el.innerText = `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`;
        timeLeft--;
    }, 1000);
}
