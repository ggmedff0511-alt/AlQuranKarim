/**
 * المنطق البرمجي المطور لشاشة التحميلات - المصحف الكوني
 * تحديث جراحي: إصلاح آلية استلام البيانات وضمان ظهور السور المحملة فوراً
 */

window.onload = function() {
    refreshDownloads();
};

window.refreshDownloads = function() {
    var listContainer = document.getElementById('downloads-list');
    if (listContainer) {
        listContainer.innerHTML = '<div style="text-align:center; padding:100px; opacity:0.6;"><span class="material-symbols-rounded" style="font-size:48px; color:#E8C172;">sync</span><p>جاري تحديث قائمة التحميلات...</p></div>';
    }

    // طلب البيانات المفصلة من النظام بمرونة
    var bridge = window.AndroidBridge || (window.parent && window.parent.AndroidBridge);
    if (bridge && typeof bridge.getDetailedDownloads === 'function') {
        bridge.getDetailedDownloads();
    }

    // مؤقت أمان: إذا لم تصل البيانات خلال 3 ثوانٍ، نظهر الحالة الفارغة بدلاً من تعليق الشاشة
    setTimeout(function() {
        var container = document.getElementById('downloads-list');
        if (container && container.innerText.includes("جاري")) {
            showEmptyState();
        }
    }, 3500);
};

window.onDetailedDownloadsReceived = function(b64) {
    var listContainer = document.getElementById('downloads-list');
    if (!listContainer) return;

    try {
        if (!b64 || b64 === "") { showEmptyState(); return; }

        // فك التشفير بذكاء (دعم الأجهزة القديمة والحديثة)
        var decoder = window.decodeSafeData || (window.parent && window.parent.decodeSafeData);
        var jsonStr = decoder ? decoder(b64) : atob(b64);
        var downloads = JSON.parse(jsonStr);

        if (downloads && Array.isArray(downloads) && downloads.length > 0) {
            var html = '';
            downloads.sort(function(a, b) { return a.surahId - b.surahId; }).forEach(function(item) {
                var sName = (window.surahNames && window.surahNames[item.surahId]) || (window.parent && window.parent.surahNames && window.parent.surahNames[item.surahId]) || ("سورة " + item.surahId);
                var rName = item.reciterName || "قارئ غير معروف";

                html += '<div class="download-item" onclick="openDownloadedSurah(' + item.surahId + ', \'' + item.reciterId + '\', \'' + rName + '\')">' +
                        '<div class="dl-info" style="display:flex; flex-direction:column; gap:4px; text-align:right;">' +
                        '<div style="font-weight:800; font-size:1.15rem; color:#FFF;">' + sName + '</div>' +
                        '<div style="font-size:0.85rem; color:#D9B24C; opacity:0.9; font-weight:600;">بصوت: ' + rName + '</div>' +
                        '</div>' +
                        '<div style="display:flex; align-items:center;">' +
                        '<span class="material-symbols-rounded" style="color: #4CAF50; font-size:28px; font-variation-settings: \'FILL\' 1;">check_circle</span>' +
                        '</div>' +
                        '</div>';
            });
            listContainer.innerHTML = html;
        } else {
            showEmptyState();
        }
    } catch(e) {
        console.error("Processing detailed downloads error:", e);
        showEmptyState();
    }
};

function showEmptyState() {
    var listContainer = document.getElementById('downloads-list');
    if (!listContainer) return;
    listContainer.innerHTML = '<div class="empty-state" style="text-align:center; padding:120px 20px; opacity:0.6;">' +
        '<span class="material-symbols-rounded" style="font-size: 70px; margin-bottom: 20px; color:#A8B4C7;">cloud_off</span>' +
        '<p style="font-size:1.1rem;">لا توجد سور محملة حالياً</p>' +
        '<button onclick="window.parent.loadPage(\'surah_list\')" style="background: linear-gradient(135deg, #D9B24C, #B88E2F); border:none; padding:12px 25px; border-radius:15px; color:#02050A; font-weight:bold; margin-top:25px; cursor:pointer;">انتقل للفهرس للتحميل</button>' +
        '</div>';
}

window.openDownloadedSurah = function(surahId, reciterId, reciterName) {
    var bridge = window.AndroidBridge || (window.parent && window.parent.AndroidBridge);
    if (bridge && typeof bridge.setReciter === 'function') {
        bridge.setReciter(reciterId, reciterName, 'ayah', '');
        if (window.parent && window.parent.loadPage) {
            window.parent.loadPage('reading', {surah: surahId});
        }
    }
};

window.goBack = function() {
    if (window.parent && window.parent.loadPage) {
        window.parent.loadPage('home');
    } else {
        window.history.back();
    }
};
