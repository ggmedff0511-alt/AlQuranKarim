/**
 * المنطق البرمجي لشاشة حصن المسلم - المصحف الكوني
 * تعديل جراحي: فصل المنطق البرمجي عن HTML
 */

window.onload = function() {
    if (window.parent && window.parent.callNative) {
        // طلب بيانات حصن المسلم من أندرويد
        window.parent.callNative('getHadiths', 'hisn_muslim', 'onDataLoaded');
    }
};

/**
 * استقبال البيانات وعرضها
 */
window.onDataLoaded = function(jsonB64) {
    try {
        const data = JSON.parse(window.parent.decodeSafeData(jsonB64));
        renderContent(data);
    } catch(e) {
        document.getElementById('content-container').innerHTML = '<p style="text-align:center; padding:20px;">تعذر تحميل البيانات حالياً.</p>';
    }
};

function renderContent(items) {
    let html = '';
    items.forEach(item => {
        html += `
            <div class="zekr-card">
                <div class="zekr-text">${item.text}</div>
                <div class="zekr-meta">
                    <span>${item.source || ''}</span>
                    <span class="repeat-badge">التكرار: ${window.parent.toAr ? window.parent.toAr(item.count || 1) : (item.count || 1)}</span>
                </div>
            </div>
        `;
    });
    document.getElementById('content-container').innerHTML = html;
}
