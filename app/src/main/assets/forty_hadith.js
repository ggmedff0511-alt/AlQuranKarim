/**
 * المنطق البرمجي لشاشة الأربعون النووية - المصحف الكوني
 * تحديث جراحي: فصل المنطق البرمجي لضمان سهولة الصيانة
 */

let isUIHidden = false;

window.onload = function() {
    if (window.parent && window.parent.callNative) {
        // طلب بيانات الأربعون النووية من نظام أندرويد
        window.parent.callNative('getHadiths', 'forty_hadith', 'onHadithsLoaded');
    } else {
        // عرض بيانات تجريبية في حال عدم وجود اتصال بالنظام (للتطوير)
        showDemoData();
    }
};

/**
 * استقبال البيانات وعرضها بتنسيق البطاقات
 */
window.onHadithsLoaded = function(jsonB64) {
    try {
        const data = JSON.parse(window.parent.decodeSafeData(jsonB64));
        renderHadiths(data);
    } catch(e) {
        document.getElementById('hadith-container').innerHTML = '<p style="text-align:center; padding:20px;">خطأ في تحميل البيانات.</p>';
    }
};

function renderHadiths(hadiths) {
    const container = document.getElementById('hadith-container');
    if (!container) return;

    let html = '';
    if (hadiths && hadiths.length > 0) {
        hadiths.forEach((h, index) => {
            html += `
                <div class="hadith-card" id="h-${index}" onclick="event.stopPropagation(); selectHadith(${index})">
                    <div class="hadith-header">
                        <span class="hadith-title">${h.title || 'الحديث الشريف'}</span>
                        <span class="hadith-number">${window.parent.toAr ? window.parent.toAr(index + 1) : (index + 1)}</span>
                    </div>
                    <div class="hadith-text">${h.text}</div>
                    ${h.explanation ? `<div class="hadith-explanation">${h.explanation}</div>` : ''}
                </div>
            `;
        });
    }
    container.innerHTML = html;
}

window.selectHadith = function(index) {
    document.querySelectorAll('.hadith-card').forEach(c => c.classList.remove('active'));
    const el = document.getElementById('h-' + index);
    if(el) {
        el.classList.add('active');
        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
};

window.toggleNightMode = function() {
    document.body.classList.toggle('night-mode');
    if (window.parent && window.parent.callNative) {
        window.parent.callNative('vibrate', 30);
    }
};

window.toggleUI = function() {
    isUIHidden = !isUIHidden;
    const h = document.getElementById('lib-header');
    const p = document.getElementById('audio-player-ui');
    if(h) h.classList.toggle('ui-hidden', isUIHidden);
    if(p) p.classList.toggle('ui-hidden', isUIHidden);
};

function showDemoData() {
    const demo = [
        { title: "الأعمال بالنيات", text: "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى، فَمَنْ كَانَتْ هِجْرَتُهُ إِلَى اللَّهِ وَرَسُولِهِ فَهِجْرَتُهُ إِلَى اللَّهِ وَرَسُولِهِ..." }
    ];
    renderHadiths(demo);
}
