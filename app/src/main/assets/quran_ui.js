/**
 * المنطق البرمجي لغلاف التطبيق (Shell) - نسخة الإصلاح الشامل للمزامنة
 * تحديث: إضافة تقنيات منع التخزين المؤقت (Cache Busting) لضمان ظهور التعديلات فوراً
 */

window.surahNames = ["", "الفاتحة", "البقرة", "آل عمران", "النساء", "المائدة", "الأنعام", "الأعراف", "الأنفال", "التوبة", "يونس", "هود", "يوسف", "الرعد", "إبراهيم", "الحجر", "النحل", "الإسراء", "الكهف", "مريم", "طه", "الأنبياء", "الحج", "المؤمنون", "النور", "الفرقان", "الشعراء", "النمل", "القصص", "العنكبوت", "الروم", "لقمان", "السجدة", "الأحزاب", "سبأ", "فاطر", "يس", "الصافات", "ص", "الزمر", "غافر", "فصلت", "الشورى", "الزخرف", "الدخان", "الجاثية", "الأحقاف", "محمد", "الفتح", "الحجرات", "ق", "الذاريات", "الطور", "النجم", "القمر", "الرحمن", "الواقعة", "الحديد", "المجادلة", "الحشر", "الممتحنة", "الصف", "الجمعة", "المنافقون", "التغابن", "الطلاق", "التحريم", "الملك", "القلم", "الحاقة", "المعارج", "نوح", "الجن", "المزمل", "المدثر", "القيامة", "الإنسان", "المرسلات", "النبأ", "النازعات", "عبس", "التكوير", "الانفطار", "المطففين", "الانشقاق", "البروج", "الطارق", "الأعلى", "الغاشية", "الفجر", "البلد", "الشمس", "الليل", "الضحى", "الشرح", "التين", "العلق", "القدر", "البينة", "الزلزلة", "العاديات", "القارعة", "التكاثر", "العصر", "الهمزة", "الفيل", "قريش", "الماعون", "الكوثر", "الكافرون", "النصر", "المسد", "الإخلاص", "الفلق", "الناس"];

window.toAr = function(n) { return String(n).replace(/\d/g, function(d) { return "٠١٢٣٤٥٦٧٨٩"[d]; }); };

window.decodeSafeData = function(b64) {
    try {
        return decodeURIComponent(escape(atob(b64)));
    } catch (e) {
        try { return atob(b64); } catch(e2) { return b64; }
    }
};

/**
 * تعليق جراحي: إضافة وظيفة callNative للواجهة الرئيسية لتتمكن الإطارات الفرعية من استدعائها
 */
window.callNative = function(method, a1, a2, a3) {
    if (window.AndroidBridge && typeof window.AndroidBridge[method] !== 'undefined') {
        if (typeof a3 !== 'undefined') return window.AndroidBridge[method](a1, a2, a3);
        if (typeof a2 !== 'undefined') return window.AndroidBridge[method](a1, a2);
        if (typeof a1 !== 'undefined') return window.AndroidBridge[method](a1);
        return window.AndroidBridge[method]();
    }
    return null;
};

window.closeWelcomeScreen = function() {
    var splash = document.getElementById('splash-screen');
    if (window.AndroidBridge && window.AndroidBridge.stopSplashAudio) window.AndroidBridge.stopSplashAudio();
    if (splash) {
        splash.style.opacity = '0';
        setTimeout(function() { splash.style.display = 'none'; }, 700);
        if (window.AndroidBridge && window.AndroidBridge.vibrate) window.AndroidBridge.vibrate(30);
    }
};

window.loadPage = function(page, params) {
    var frame = document.getElementById('page-loader-frame');
    if (!frame) return;

    var pageName = (page === 'index') ? 'surah_list' : page;
    var url = pageName + ".html?v=" + new Date().getTime();

    if (params) {
        for (var key in params) { url += "&" + key + "=" + params[key]; }
    }
    frame.src = url;

    var nav = document.getElementById('global-nav');
    if (nav) {
        if (pageName === 'reading') {
            nav.style.display = 'none';
        } else {
            nav.style.display = 'flex';
        }
    }

    document.querySelectorAll('.nav-item').forEach(function(el) { el.classList.remove('active'); });
    var navTarget = (pageName === 'reading') ? 'surah_list' : pageName;
    var activeNav = document.getElementById('nav-' + navTarget);
    if (activeNav) activeNav.classList.add('active');
};

/**
 * تعليق جراحي: ميزة الرجوع الذكي - تقوم بفحص إذا كان الإطار الفرعي (مثل صحيح البخاري) لديه وظيفة رجوع داخلية
 */
window.goBack = function() {
    var frame = document.getElementById('page-loader-frame');
    if (frame && frame.contentWindow && typeof frame.contentWindow.goBack === 'function') {
        frame.contentWindow.goBack();
    } else {
        window.history.back();
    }
};

window.onload = function() {
    var urlParams = new URLSearchParams(window.location.search);
    window.loadPage(urlParams.get('page') || 'home');
};

function dispatchToFrame(fn) {
    var frame = document.getElementById('page-loader-frame');
    if (frame && frame.contentWindow) {
        if (typeof frame.contentWindow[fn] === 'function') {
            var args = Array.prototype.slice.call(arguments, 1);
            frame.contentWindow[fn].apply(frame.contentWindow, args);
        }
    }
}

window.onAudioUpdate = function(status, surah, ayah, repeat, reciterName, progress) {
    dispatchToFrame('onAudioUpdate', status, surah, ayah, repeat, reciterName, progress);
};

window.onQuranDataReceived = function(d) { dispatchToFrame('onQuranDataReceived', d); };
window.onTafsirReceived = function(d) { dispatchToFrame('onTafsirReceived', d); };
window.syncAyah = function(s, a) { dispatchToFrame('syncAyah', s, a); };
window.onQiblaUpdate = function(deg) { dispatchToFrame('onQiblaUpdate', deg); };
window.onPrayerTimesLoaded = function(d) { dispatchToFrame('onPrayerTimesLoaded', d); };
window.onDailyAyah = function(d) { dispatchToFrame('onDailyAyah', d); };
window.onHijriDateReceived = function(d) { dispatchToFrame('onHijriDateReceived', d); };
window.onRecitersLoaded = function(d) { dispatchToFrame('onRecitersLoaded', d); };
window.onDownloadedSurahsReceived = function(d) { dispatchToFrame('onDownloadedSurahsReceived', d); };
window.onDetailedDownloadsReceived = function(d) { dispatchToFrame('onDetailedDownloadsReceived', d); };
window.onLastRead = function(d) { dispatchToFrame('onLastRead', d); };
window.onHadithsLoaded = function(d) { dispatchToFrame('onHadithsLoaded', d); };
window.onDataLoaded = function(d) { dispatchToFrame('onDataLoaded', d); };
