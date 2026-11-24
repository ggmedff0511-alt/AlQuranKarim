/**
 * المحرك البرمجي للمصحف الكوني - نسخة المزامنة فائقة الدقة
 */
var API_URL = "https://api.quran.com/api/v4";

var getQueryParam = function(name) {
    var match = RegExp('[?&]' + name + '=([^&]*)').exec(window.location.search);
    return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
};

window.currentId = parseInt(getQueryParam('surah')) || 0;
// تفعيل المتابعة تلقائياً إذا كان هناك صوت يعمل في الخلفية
window.followAudio = true;
window.currentAyah = 1;
window.isFetching = false;

window.toArDigits = function(n) {
    if (n === null || n === undefined) return "٠";
    var id = ['٠','١','٢','٣','٤','٥','٦','٧','٨','٩'];
    return String(n).replace(/[0-9]/g, function(w) { return id[parseInt(w)]; });
};

/**
 * تعليق جراحي: تحسين آلية التواصل مع النظام (Native)
 * لضمان وصول الردود (Callbacks) حتى لو كانت الصفحة داخل iframe.
 */
window.callNative = function(method, a1, a2, a3, a4) {
    var bridge = window.AndroidBridge || (window.parent && window.parent.AndroidBridge);
    if (!bridge || typeof bridge[method] === 'undefined') return null;

    // إذا كان هناك callback، نضمن أنه متاح عالمياً للوصول إليه من Java
    if (method === 'getReciters' && a1) {
        window.onRecitersLoaded = window.onRecitersLoaded || function(data) {
             var frame = document.querySelector('iframe');
             if(frame && frame.contentWindow && frame.contentWindow.onRecitersLoaded) {
                 frame.contentWindow.onRecitersLoaded(data);
             }
        };
    }

    if (typeof a4 !== 'undefined') return bridge[method](a1, a2, a3, a4);
    if (typeof a3 !== 'undefined') return bridge[method](a1, a2, a3);
    if (typeof a2 !== 'undefined') return bridge[method](a1, a2);
    if (typeof a1 !== 'undefined') return bridge[method](a1);
    return bridge[method]();
};

window.decodeSafeData = function(b64) {
    try { return decodeURIComponent(escape(atob(b64))); } catch(e) { return atob(b64); }
};

window.surahNames = ["", "الفاتحة", "البقرة", "آل عمران", "النساء", "المائدة", "الأنعام", "الأعراف", "الأنفال", "التوبة", "يونس", "هود", "يوسف", "الرعد", "إبراهيم", "الحجر", "النحل", "الإسراء", "الكهف", "مريم", "طه", "الأنبياء", "الحج", "المؤمنون", "النور", "الفرقان", "الشعراء", "النمل", "القصص", "العنكبوت", "الروم", "لقمان", "السجدة", "الأحزاب", "سبأ", "فاطر", "يس", "الصافات", "ص", "الزمر", "غافر", "فصلت", "الشورى", "الزخرف", "الدخان", "الجاثية", "الأحقاف", "محمد", "الفتح", "الحجرات", "ق", "الذاريات", "الطور", "النجم", "القمر", "الرحمن", "الواقعة", "الحديد", "المجادلة", "الحشر", "الممتحنة", "الصف", "الجمعة", "المنافقون", "التغابن", "الطلاق", "التحريم", "الملك", "القلم", "الحاقة", "المعارج", "نوح", "الجن", "المزمل", "المدثر", "القيامة", "الإنسان", "المرسلات", "النبأ", "النازعات", "عبس", "التكوير", "الانفطار", "المطففين", "الانشقاق", "البروج", "الطارق", "الأعلى", "الغاشية", "الفجر", "البلد", "الشمس", "الليل", "الضحى", "الشرح", "التين", "العلق", "القدر", "البينة", "الزلزلة", "العاديات", "القارعة", "التكاثر", "العصر", "الهمزة", "الفيل", "قريش", "الماعون", "الكوثر", "الكافرون", "النصر", "المسد", "الإخلاص", "الفلق", "الناس"];

window.renderSurahContents = function(id, isManual) {
    var surahId = parseInt(id); if (isNaN(surahId) || surahId < 1) return;
    // إذا دخل المستخدم يدوياً، لا نقطع المتابعة إذا كان الصوت يعمل
    window.currentId = surahId; window.isFetching = true;
    if(document.getElementById('mushaf-surah-title')) document.getElementById('mushaf-surah-title').innerText = "سورة " + window.surahNames[surahId];
    var url = API_URL + "/quran/verses/uthmani?chapter_number=" + surahId;
    window.callNative('fetchQuranData', url, 'onQuranDataReceived');
};

window.onQuranDataReceived = function(b64) {
    window.isFetching = false; if (!b64) return;
    try {
        var data = JSON.parse(window.decodeSafeData(b64));
        displayVerses(data.verses);
    } catch(e) {}
};

function displayVerses(verses) {
    var container = document.getElementById('quran-render-content'); if(!container) return;
    var html = '';
    if (window.currentId != 1 && window.currentId != 9) html += '<div class="bismillah-calligraphy">بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ</div>';
    if (verses) {
        for (var i = 0; i < verses.length; i++) {
            var v = verses[i]; var ayahNum = v.verse_key ? parseInt(v.verse_key.split(':')[1]) : i + 1;
            var markHtml = '<div class="ayah-mark-wrap"><div class="ayah-mark-ornament"></div><div class="ayah-mark-num">' + window.toArDigits(ayahNum) + '</div></div>';
            html += '<span class="verse-item" id="v-' + window.currentId + ':' + ayahNum + '" onclick="handleVerseClick(\'' + window.currentId + ':' + ayahNum + '\')">' + v.text_uthmani + ' ' + markHtml + '</span> ';
        }
    }
    container.innerHTML = html;
    // تظليل فوري عند التحميل
    setTimeout(function() { window.highlightAyah(window.currentId + ":" + window.currentAyah); }, 100);
}

window.highlightAyah = function(key) {
    var el = document.getElementById('v-' + key);
    if(el) {
        // إزالة التظليل القديم بكفاءة
        var active = document.querySelector('.verse-item.active');
        if (active) active.classList.remove('active');

        el.classList.add('active');
        // تمرير ذكي وهادئ
        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
};

window.syncAyah = function(s, a) {
    var sInt = parseInt(s); var aInt = parseInt(a);
    if (sInt < 1) return;
    if (sInt !== window.currentId) {
        window.currentId = sInt; window.currentAyah = aInt;
        window.renderSurahContents(sInt, false); return;
    }
    window.currentAyah = aInt;
    window.highlightAyah(sInt + ":" + aInt);
};

window.onAudioUpdate = function(status, surah, ayah, repeat, reciterName) {
    // تحديث الواجهة بناءً على البث القادم من الخدمة
    var sInt = parseInt(surah); var aInt = parseInt(ayah);
    if (!isNaN(sInt) && sInt > 0 && !isNaN(aInt) && aInt > 0) {
        if (!window.isFetching && window.followAudio) {
            // تحديث التظليل فقط إذا تغيرت الآية أو فقد التظليل
            if (sInt !== window.currentId || aInt !== window.currentAyah || !document.querySelector('.verse-item.active')) {
                window.syncAyah(sInt, aInt);
            }
        }
    }

    // تحديث زر التشغيل
    var playBtn = document.getElementById('play-pause-btn');
    if (playBtn) {
        var icon = playBtn.querySelector('.material-symbols-rounded');
        if (icon) icon.innerText = (status && (status.toLowerCase().includes('playing') || status.toLowerCase().includes('buffering'))) ? 'pause' : 'play_arrow';
    }
};
