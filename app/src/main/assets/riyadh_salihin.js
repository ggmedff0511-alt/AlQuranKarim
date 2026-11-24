/**
 * المنطق البرمجي لشاشة رياض الصالحين - المصحف الكوني
 * تعديل جراحي: فصل المنطق البرمجي عن HTML
 */

window.onload = function() {
    // طلب بيانات رياض الصالحين من نظام أندرويد
    if (window.parent && window.parent.callNative) {
        window.parent.callNative('getHadiths', 'riyadh', 'onHadithsLoaded');
    }
};

/**
 * استقبال البيانات وعرضها ديناميكياً
 */
window.onHadithsLoaded = function(jsonB64) {
    try {
        const data = JSON.parse(window.parent.decodeSafeData(jsonB64));
        let html = '';
        data.forEach((h, i) => {
            html += `
                <div class="hadith-card">
                    <div class="hadith-header">
                        <span class="hadith-title">${h.title || 'باب'}</span>
                        <span class="hadith-number">${window.parent.toAr ? window.parent.toAr(i + 1) : (i + 1)}</span>
                    </div>
                    <div class="hadith-text">${h.text}</div>
                </div>
            `;
        });
        document.getElementById('hadith-container').innerHTML = html;
    } catch(e) {
        document.getElementById('hadith-container').innerHTML = '<p style="text-align:center; padding:20px;">تعذر تحميل البيانات حالياً.</p>';
    }
};
