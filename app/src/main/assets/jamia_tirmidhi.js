/**
 * المنطق البرمجي المطور لجامع الترمذي - المصحف الكوني
 * تم التحديث لدعم نظام الفهرس والقراءة المتزامن مع قاعدة البيانات الحقيقية.
 */

let currentView = 'index';

window.onload = function() {
    loadIndex();
};

function loadIndex() {
    currentView = 'index';
    document.querySelector('.lib-title').innerText = "جامع الترمذي";
    document.getElementById('hadith-container').innerHTML = '<div style="text-align:center; padding:50px;"><div class="loader-spinner"></div><p>جاري تحميل الفهرس...</p></div>';

    if (window.parent && window.parent.callNative) {
        // طلب الفهرس من النظام
        window.parent.callNative('getHadiths', 'jamia_tirmidhi', 'onHadithsLoaded');
    }
}

window.openChapter = function(chapterId, chapterTitle) {
    currentView = 'reading';
    document.querySelector('.lib-title').innerText = chapterTitle;
    document.getElementById('hadith-container').innerHTML = '<div style="text-align:center; padding:50px;"><div class="loader-spinner"></div><p>جاري تحميل الأحاديث...</p></div>';

    // طلب أحاديث الباب المختار
    window.parent.callNative('getHadiths', 'hadiths_' + chapterId, 'onHadithsLoaded');
};

window.onHadithsLoaded = function(jsonB64) {
    const container = document.getElementById('hadith-container');
    if (!container) return;

    try {
        const data = JSON.parse(window.parent.decodeSafeData(jsonB64));
        let html = '';

        if (currentView === 'index') {
            if (!data || data.length === 0 || data[0].id === -1) {
                html = `<div style="text-align:center; padding:40px; opacity:0.7;">
                            <p>${data[0] ? data[0].subtitle : 'المحتوى غير متوفر حالياً'}</p>
                        </div>`;
            } else {
                data.forEach(item => {
                    html += `
                        <div class="chapter-card" onclick="openChapter(${item.id}, '${item.title}')">
                            <div class="chapter-num-box">${window.parent.toAr ? window.parent.toAr(item.id) : item.id}</div>
                            <div class="chapter-info">
                                <span class="chapter-name">${item.title}</span>
                                <span class="chapter-meta">${item.subtitle}</span>
                            </div>
                            <span class="material-symbols-rounded">chevron_left</span>
                        </div>`;
                });
            }
        } else {
            if (!data || data.length === 0) {
                html = '<p style="text-align:center; padding:40px;">لا توجد أحاديث في هذا الباب.</p>';
            } else {
                data.forEach((h) => {
                    html += `
                        <div class="hadith-card-full">
                            <div class="hadith-header">
                                <span class="hadith-tag">${h.title}</span>
                                <div class="hadith-actions">
                                    <span class="material-symbols-rounded" onclick="window.parent.AndroidBridge.shareText('${h.text.replace(/'/g, "\\'")}')">share</span>
                                </div>
                            </div>
                            <div class="hadith-matn">${h.text}</div>
                            <div class="hadith-footer">درجة الحديث: <span class="gold-text">${h.grade || 'جامع الترمذي'}</span></div>
                        </div>`;
                });
            }
        }
        container.innerHTML = html;
        window.scrollTo(0, 0);
    } catch(e) {
        container.innerHTML = '<p style="text-align:center; padding:20px;">حدث خطأ أثناء معالجة البيانات.</p>';
    }
};

window.goBack = function() {
    if (currentView === 'reading') {
        loadIndex();
    } else {
        window.parent.loadPage('library');
    }
};
