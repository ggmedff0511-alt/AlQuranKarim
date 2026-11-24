/**
 * المنطق البرمجي المطور لصحيح البخاري - النسخة النهائية المرتبطة بقاعدة البيانات الحقيقية
 * تعليق جراحي: تم إلغاء طبقة "الأبواب" الوهمية التي كانت تسبب التكرار، والربط المباشر ببيانات المستودع.
 */

let currentView = 'index';
let lastBookId = null;
let lastBookTitle = "";

window.onload = function() {
    loadIndex();
};

/**
 * تعليق جراحي: جلب الفهرس الرئيسي (قائمة الكتب الحقيقية من قاعدة البيانات)
 */
function loadIndex() {
    currentView = 'index';
    const titleEl = document.querySelector('.lib-title');
    if (titleEl) titleEl.innerText = "صحيح البخاري";

    document.getElementById('hadith-container').innerHTML = '<div class="loader-spinner"></div>';
    if (window.parent && window.parent.callNative) {
        window.parent.callNative('getHadiths', 'bukhari_index', 'onHadithsLoaded');
    }
}

/**
 * تعليق جراحي: الانتقال المباشر لقراءة الأحاديث التابعة للكتاب المختار.
 * تم تعديل هذه الوظيفة لتجاوز مشكلة "الأبواب الثلاثة المكررة".
 */
window.openChapters = function(bookId, bookTitle) {
    currentView = 'reading'; // الانتقال للحالة 'reading' مباشرة لضمان جلب الأحاديث
    lastBookId = bookId;
    lastBookTitle = bookTitle;
    document.querySelector('.lib-title').innerText = bookTitle;
    document.getElementById('hadith-container').innerHTML = '<div class="loader-spinner"></div>';

    // استدعاء الأحاديث الحقيقية لهذا الكتاب من قاعدة البيانات عبر المعرف bookId
    window.parent.callNative('getHadiths', 'hadiths_' + bookId, 'onHadithsLoaded');
};

/**
 * وظيفة الرجوع الذكي
 */
window.goBack = function() {
    if (currentView === 'reading') {
        loadIndex();
    } else {
        if (window.parent && window.parent.loadPage) {
            window.parent.loadPage('library');
        } else {
            window.history.back();
        }
    }
};

/**
 * استقبال البيانات ومعالجتها جراحياً
 * تعليق جراحي: تم تنظيف هذه الدالة لتعرض إما الفهرس أو متن الأحاديث الحقيقية فقط.
 */
window.onHadithsLoaded = function(jsonB64) {
    try {
        const data = JSON.parse(window.parent.decodeSafeData(jsonB64));
        let html = '';

        if (currentView === 'index') {
            data.forEach(item => {
                html += `
                    <div class="chapter-card" onclick="openChapters(${item.id}, '${item.title}')">
                        <div class="chapter-num-box">${window.parent.toAr(item.id)}</div>
                        <div class="chapter-info">
                            <span class="chapter-name">${item.title}</span>
                            <span class="chapter-meta">${item.subtitle}</span>
                        </div>
                        <span class="material-symbols-rounded arrow-left">chevron_left</span>
                    </div>`;
            });
        }
        else if (currentView === 'reading') {
            if (!data || data.length === 0) {
                html = '<div style="text-align:center; padding:40px; opacity:0.7;">لم يتم العثور على أحاديث في هذا الكتاب. يرجى التأكد من تحميل صحيح البخاري.</div>';
            } else {
                data.forEach((h, i) => {
                    html += `
                        <div class="hadith-card-full">
                            <div class="hadith-header">
                                <span class="hadith-tag">${h.title || 'حديث نبوي'}</span>
                                <div class="hadith-actions">
                                    <span class="material-symbols-rounded" onclick="copyHadith('${h.matn}')">content_copy</span>
                                    <span class="material-symbols-rounded" onclick="shareHadith('${h.matn}')">share</span>
                                </div>
                            </div>
                            <div class="hadith-matn">${h.matn}</div>
                            <div class="hadith-footer">درجة الحديث: <span class="gold-text">${h.grade || 'صحيح البخاري'}</span></div>
                        </div>`;
                });
            }
        }

        document.getElementById('hadith-container').innerHTML = html;
        window.scrollTo(0, 0);
    } catch(e) {
        document.getElementById('hadith-container').innerHTML = '<p style="text-align:center;">تعذر تحميل البيانات الحقيقية.</p>';
    }
};

window.copyHadith = function(text) {
    window.parent.AndroidBridge.showToast("تم نسخ الحديث");
};

window.shareHadith = function(text) {
    window.parent.AndroidBridge.shareText(text);
};

window.filterChapters = function(query) {
    const cards = document.querySelectorAll('.chapter-card');
    cards.forEach(card => {
        const name = card.querySelector('.chapter-name').innerText;
        card.style.display = name.includes(query) ? 'flex' : 'none';
    });
};
