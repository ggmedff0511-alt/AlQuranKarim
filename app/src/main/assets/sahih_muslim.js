/**
 * المنطق البرمجي لشاشة صحيح مسلم - المصحف الكوني
 * تعديل جراحي: فصل المنطق البرمجي عن HTML
 */

window.onload = function() {
    if (window.parent && window.parent.callNative) {
        // طلب بيانات صحيح مسلم من نظام أندرويد
        window.parent.callNative('getHadiths', 'muslim', 'onHadithsLoaded');
    }
};

/**
 * استقبال البيانات وعرضها ديناميكياً بتنسيق البطاقات
 */
window.onHadithsLoaded = function(jsonB64) {
    const container = document.getElementById('hadith-container');
    if (!container) return;

    try {
        const data = JSON.parse(window.parent.decodeSafeData(jsonB64));
        let html = '';
        data.forEach((h, i) => {
            html += `
                <div class="hadith-card">
                    <div class="hadith-header">
                        <span class="hadith-title">${h.title || 'حديث شريف'}</span>
                        <span class="hadith-number">${window.parent.toAr ? window.parent.toAr(i + 1) : (i + 1)}</span>
                    </div>
                    <div class="hadith-text">${h.text}</div>
                </div>`;
        });
        container.innerHTML = html;
    } catch(e) {
        container.innerHTML = '<p style="text-align:center; padding:20px;">تعذر تحميل البيانات حالياً.</p>';
    }
};
