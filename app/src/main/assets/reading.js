/**
 * نسخة مستقرة ومبسطة لشاشة القراءة - التراجع عن التعقيدات البرمجية
 * تضمن هذه النسخة انتقال التظليل وتحرك الشاشة فوراً مع القارئ
 */

window.AudioStatus = 'PAUSED';
window._lastProcessedKey = null;
window.followAudio = true;
window._lastClickTime = 0;
window._lastClickKey = null;

window.onload = function() {
    var urlParams = new URLSearchParams(window.location.search);
    var surah = urlParams.get('surah') || localStorage.getItem('lastSurah') || 1;
    var ayah = urlParams.get('ayah') || localStorage.getItem('lastAyah') || 1;

    window.currentId = parseInt(surah);
    window.currentAyah = parseInt(ayah);
    window.followAudio = true;

    var savedReciterName = localStorage.getItem('q_rec_name') || "ماهر المعيقلي";
    var recNameEl = document.getElementById('mushaf-reciter-name');
    if (recNameEl) recNameEl.innerText = "بصوت " + savedReciterName;

    if (typeof renderSurahContents === 'function') {
        renderSurahContents(window.currentId, false);
    }

    window.callNative('requestAudioStatus');
    initTafsirDrag();
};

/**
 * دالة استقبال تحديثات الصوت - تم تحسينها لضمان المتابعة الدقيقة وحل مشكلة التكرار
 * التعديل الجراحي: إضافة تحديث أيقونة التكرار وحل مشكلة التوقف عند بداية السور (مثل الم)
 */
window.onAudioUpdate = function(status, surah, ayah, repeat, reciterName, progress) {
    window.AudioStatus = status;
    var sInt = parseInt(surah);
    var aInt = parseInt(ayah);
    var key = sInt + ":" + aInt;

    if (!isNaN(sInt) && sInt > 0 && !isNaN(aInt) && aInt > 0) {
        // تعليق جراحي: تحسين التحقق من حالة التشغيل ليشمل الحالات المتعددة (PLAYING, BUFFERING) لضمان تفعيل المتابعة
        if (status && (status.toString().toUpperCase().includes('PLAYING') || status.toString().toUpperCase().includes('BUFFERING'))) {
            window.followAudio = true;
        }

        if (sInt !== window.currentId) {
            // عند تغيير السورة، نصفر المفتاح لضمان إعادة التظليل بعد اكتمال التحميل
            window._lastProcessedKey = null;
            window.syncAyah(sInt, aInt);
        } else {
            window.currentAyah = aInt;
            // التحقق مما إذا كانت الآية قد تغيرت أو إذا كان التظليل مفقوداً من الشاشة
            if (window._lastProcessedKey !== key || !document.querySelector('.verse-item.active')) {
                // محاولة التظليل، وإذا نجحت نحدث المفتاح الأخير
                if (window.highlightAyah(key, true)) {
                    window._lastProcessedKey = key;
                }
            }
        }
    }

    // تحديث زر التشغيل
    var playBtn = document.getElementById('play-pause-btn');
    if (playBtn) {
        var icon = playBtn.querySelector('.material-symbols-rounded');
        if (icon) icon.innerText = (status && (status.toString().toUpperCase().includes('PLAYING') || status.toString().toUpperCase().includes('BUFFERING'))) ? 'pause' : 'play_arrow';
    }

    // تحديث حالة زر التكرار لضمان وضوحها للمستخدم (حل مشكلة التكرار غير المقصود)
    var repeatBtn = document.querySelector('button[onclick*="toggleRepeat"]');
    if (repeatBtn) {
        var rIcon = repeatBtn.querySelector('.material-symbols-rounded');
        if (rIcon) {
            if (repeat) {
                rIcon.innerText = 'repeat_one';
                rIcon.style.color = 'var(--gold-main)';
            } else {
                rIcon.innerText = 'repeat';
                rIcon.style.color = '#FFFFFF';
            }
        }
    }

    var recNameEl = document.getElementById('mushaf-reciter-name');
    if (recNameEl && reciterName) {
        recNameEl.innerText = "بصوت " + reciterName;
    }
};

/**
 * تظليل الآية وتحريك الشاشة لمنتصفها تماماً
 * تعديل جراحي: إضافة معامل forceScroll لضمان تحريك الشاشة مع صوت الشيخ باستمرار
 */
window.highlightAyah = function(key, forceScroll) {
    var el = document.getElementById('v-' + key);
    if(el) {
        var isAlreadyActive = el.classList.contains('active');

        // إزالة التظليل القديم وتفعيل الجديد فقط إذا لزم الأمر
        if (!isAlreadyActive) {
            var actives = document.querySelectorAll('.verse-item.active');
            for(var i=0; i<actives.length; i++) actives[i].classList.remove('active');
            el.classList.add('active');
        }

        // تحريك الشاشة لمنتصف الآية إذا كانت المتابعة مفعلة
        if (window.followAudio && (forceScroll || !isAlreadyActive)) {
            el.scrollIntoView({
                behavior: 'smooth',
                block: 'center'
            });
        }
        return true;
    }
    return false;
};

window.syncAyah = function(s, a) {
    window.currentId = s;
    window.currentAyah = a;
    if (typeof renderSurahContents === 'function') renderSurahContents(s, false);
};

window.handleVerseClick = function(key) {
    var now = Date.now();
    var delta = now - window._lastClickTime;

    if (delta < 300 && window._lastClickKey === key) {
        // Double Tap Detected
        var parts = key.split(':');
        window.showTafsir(parts[0], parts[1]);
    } else {
        // Single Tap
        var parts = key.split(':');
        window._lastProcessedKey = null;
        window.currentId = parseInt(parts[0]);
        window.currentAyah = parseInt(parts[1]);
        window.callNative('playAyah', window.currentId, window.currentAyah);
        window.followAudio = true;
        window.highlightAyah(key, true);
    }

    window._lastClickTime = now;
    window._lastClickKey = key;
};

window.togglePlayAction = function() {
    window.followAudio = true;
    if (window.AudioStatus === 'PLAYING' || window.AudioStatus === 'BUFFERING') {
        window.callNative('togglePlay');
    } else {
        window.callNative('playAyah', window.currentId, window.currentAyah);
    }
};

window.nextAyahAction = function() { window.followAudio = true; window.callNative('nextAyah'); };
window.prevAyahAction = function() { window.followAudio = true; window.callNative('prevAyah'); };

// --- تعديل جراحي: إضافة وظائف التحكم في الواجهة (UI Controls) ---

window.toggleUI = function() {
    var player = document.getElementById('audio-player-ui');
    if (player) {
        // إخفاء مشغل الصوت عند النقر على إغلاق
        player.style.display = (player.style.display === 'none') ? 'flex' : 'none';
    }
};

window.toggleBookmark = function() {
    var icon = document.getElementById('bookmark-icon');
    if (icon) {
        // تبديل أيقونة المفضلة واستدعاء النظام للحفظ
        if (icon.innerText === 'star') {
            icon.innerText = 'star_outline';
            window.callNative('removeBookmark', window.currentId, window.currentAyah);
        } else {
            icon.innerText = 'star';
            window.callNative('addBookmark', window.currentId, window.currentAyah);
        }
    }
};

window.toggleSleepPopup = function(e) {
    if (e) e.stopPropagation();
    var popup = document.getElementById('sleep-timer-popup');
    if (popup) popup.classList.toggle('active');
};

window.setSleepTimer = function(minutes) {
    // إرسال طلب مؤقت النوم للنظام وإغلاق القائمة المنبثقة
    window.callNative('setSleepTimer', minutes);
    var popup = document.getElementById('sleep-timer-popup');
    if (popup) popup.classList.remove('active');

    if (minutes > 0) {
        console.log("تم ضبط مؤقت النوم بعد " + minutes + " دقيقة");
    }
};

// إغلاق القوائم المنبثقة عند النقر خارجها
window.addEventListener('click', function() {
    var popup = document.getElementById('sleep-timer-popup');
    if (popup && popup.classList.contains('active')) popup.classList.remove('active');
});

window.showTafsir = function(s, a) {
    var sheet = document.getElementById('tafsir-bottom-sheet');
    var body = document.getElementById('tafsir-body-text');
    if (body) body.innerHTML = '<div style="text-align:center;padding:20px;"><div class="loader-spinner"></div><p>جاري تحميل التفسير...</p></div>';
    if (sheet) {
        sheet.classList.add('active');
        sheet.style.height = '60vh'; // ارتفاع مناسب عند الفتح
    }
    window.callNative('fetchTafsir', parseInt(s), parseInt(a), 'onTafsirReceived');
};

window.onTafsirReceived = function(b64) {
    var body = document.getElementById('tafsir-body-text');
    if (!body || !b64) return;
    try {
        var dataStr = window.decodeSafeData(b64);
        body.innerHTML = '<div style="padding:15px; font-size:1.1rem; line-height:1.6; color:#EEE;">' + (dataStr || "لا يوجد تفسير متاح لهذه الآية حالياً.") + '</div>';
    } catch(e) { body.innerText = "خطأ في تحميل التفسير."; }
};

window.closeTafsir = function() { document.getElementById('tafsir-bottom-sheet').classList.remove('active'); };

function initTafsirDrag() {
    var sheet = document.getElementById('tafsir-bottom-sheet');
    var handle = document.querySelector('.tafsir-handle');
    if (!sheet || !handle) return;
    var isDragging = false, startY, startH;
    handle.addEventListener('touchstart', function(e) {
        isDragging = true; startY = e.touches[0].clientY; startH = sheet.offsetHeight;
        sheet.style.transition = 'none';
    });
    window.addEventListener('touchmove', function(e) {
        if (!isDragging) return;
        var diff = startY - e.touches[0].clientY;
        var newH = startH + diff;
        if (newH > 30) sheet.style.height = newH + 'px';
    });
    window.addEventListener('touchend', function() {
        if (!isDragging) return;
        isDragging = false; sheet.style.transition = 'height 0.4s ease';
        if (sheet.offsetHeight > window.innerHeight * 0.6) sheet.style.height = '92vh';
        else if (sheet.offsetHeight < window.innerHeight * 0.2) window.closeTafsir();
        else sheet.style.height = '60vh';
    });
}
