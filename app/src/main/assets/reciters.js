/**
 * المنطق البرمجي لشاشة القراء - المصحف الكوني
 * تحديث جراحي: تحسين البحث والترتيب وتعديل هيكلية البطاقة لتطابق التصميم المطلوب (UI Redesign)
 */

window.onload = function() {
    renderReciters();
};

window.renderReciters = function() {
    if (typeof window.callNative === 'function') {
        window.callNative('getReciters', 'onRecitersLoaded');
    }
};

window.onRecitersLoaded = function(b64) {
    if (!b64) return;
    try {
        var jsonStr = window.decodeSafeData(b64);
        var reciters = JSON.parse(jsonStr);
        displayReciters(reciters);
    } catch (e) {
        console.error("Error loading reciters:", e);
    }
};

window.selectReciterAction = function(id, name, type, server) {
    if (typeof window.callNative === 'function') {
        window.callNative('setReciter', id, name, type, server);
        window.callNative('showToast', 'تم اختيار القارئ: ' + name);
    }
    localStorage.setItem('q_rec', id);
    localStorage.setItem('q_rec_name', name);

    // تعليق جراحي: التأكد من إبلاغ النافذة الأبوية بضرورة تفعيل المتابعة التلقائية قبل الانتقال لضمان حركة الشاشة
    if (window.parent) {
        window.parent.followAudio = true;
    }

    renderReciters();
    setTimeout(function() {
        if (window.parent && window.parent.loadPage) {
            window.parent.loadPage('reading');
        }
    }, 800);
};

function displayReciters(reciters) {
    var container = document.getElementById('reciters-list-container');
    if (!container) return;

    /**
     * تعديل جراحي: تصفية قائمة القراء للإبقاء فقط على المشايخ المحددين من قبل المستخدم وحذف البقية.
     * القائمة المختارة: عبدالباسط، الحصري، المعيقلي، العفاسي، السديس، فارس عباد، هاني الرفاعي، محمد جبريل، محمد أيوب، الطبلاوي، عبدالله بصفر، سعود الشريم.
     */
    var allowedKeywords = [
        "عبدالباسط", "الحصري", "المعيقلي", "المعقلي", "العفاسي", "السديس",
        "فارس عباد", "هاني الرفاعي", "محمد جبريل", "محمد أيوب", "محمد ايوب",
        "الطبلاوي", "عبدالله بصفر", "سعود الشريم", "سعودالشريم"
    ];

    reciters = reciters.filter(function(reciter) {
        return allowedKeywords.some(function(key) {
            return reciter.name.includes(key);
        });
    });

    var famousKeywords = ["basit", "husary", "maher", "alafasy", "sudais", "shuraim", "abbad", "rifai", "jibreel", "ayyub", "tablawi", "basfar"];

    reciters.sort(function(a, b) {
        var idxA = famousKeywords.findIndex(k => a.id.toLowerCase().includes(k));
        var idxB = famousKeywords.findIndex(k => b.id.toLowerCase().includes(k));
        if (idxA === -1) idxA = 999;
        if (idxB === -1) idxB = 999;
        if (idxA !== idxB) return idxA - idxB;
        return a.name.localeCompare(b.name, 'ar');
    });

    var html = '';
    var currentRecId = localStorage.getItem('q_rec');
    var seenIds = {};

    reciters.forEach(function(reciter) {
        if (seenIds[reciter.id]) return;
        seenIds[reciter.id] = true;
        var isActive = (reciter.id === currentRecId);

        /**
         * تعليق جراحي: تم تعديل ترتيب العناصر (أيقونة القارئ يميناً، النص وسيطاً، علامة الصح يساراً)
         * ليطابق التصميم المرفق في الصورة بدقة 100%
         */
        html += '<div class="reciter-fancy-card ' + (isActive ? 'active' : '') + '" onclick="window.selectReciterAction(\'' + reciter.id + '\', \'' + reciter.name + '\', \'' + reciter.type + '\', \'' + reciter.server + '\')">' +
                '<div class="reciter-card-main">' +
                '<div class="reciter-avatar"><span class="material-symbols-rounded">person</span></div>' +
                '<div class="reciter-names">' +
                '<div class="reciter-name">' + reciter.name + '</div>' +
                '<div class="reciter-desc">' + reciter.sub + '</div>' +
                '</div>' +
                '</div>' +
                (isActive ? '<div class="check-mark-box"><span class="material-symbols-rounded">check_circle</span></div>' : '') +
                '</div>';
    });

    container.innerHTML = html;
    var label = document.getElementById('reciters-count-label');
    if (label && typeof window.parent.toAr === 'function') {
        label.innerText = 'كل القراء (' + window.parent.toAr(Object.keys(seenIds).length) + ' قارئ)';
    }
}

window.filterReciters = function(query) {
    var cards = document.querySelectorAll('.reciter-fancy-card');
    var visibleCount = 0;
    var normalizedQuery = query.trim().toLowerCase();
    cards.forEach(function(card) {
        var name = card.querySelector('.reciter-name').innerText.toLowerCase();
        var isVisible = name.includes(normalizedQuery);
        card.style.display = isVisible ? 'flex' : 'none';
        if (isVisible) visibleCount++;
    });
    var label = document.getElementById('reciters-count-label');
    if (label && typeof window.parent.toAr === 'function') {
        label.innerText = (normalizedQuery === "" ? 'كل القراء (' : 'نتائج البحث (') + window.parent.toAr(visibleCount) + ' قارئ)';
    }
};
