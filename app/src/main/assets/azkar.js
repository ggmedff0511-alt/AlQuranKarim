/**
 * المنطق البرمجي لشاشة الأذكار - المصحف الكوني
 * تحديث جراحي: دعم العداد التنازلي والاهتزاز والتكرار المتعدد ومشاركة الأذكار كصور محسنة
 */

window.handleCounter = function(el, target) {
    // جلب القيمة الحالية من السمة (attribute) أو استخدام الهدف الأولي
    let current = el.getAttribute('data-current');
    if (current === null) {
        current = target;
    } else {
        current = parseInt(current);
    }

    if (current <= 0) return;

    // تفعيل الاهتزاز عند كل نقرة
    if (window.parent && window.parent.AndroidBridge && window.parent.AndroidBridge.vibrate) {
        window.parent.AndroidBridge.vibrate(40);
    }

    current--;
    el.setAttribute('data-current', current);

    if (current > 0) {
        // تحديث النص لإظهار العدد المتبقي
        const countText = window.toArDigits ? window.toArDigits(current) : current;
        el.querySelector('span:last-child').innerText = 'المتبقي: ' + countText;
    } else {
        // حالة الانتهاء
        el.innerHTML = '<span class="material-symbols-rounded">check_circle</span> <span>تمت القراءة</span>';
        el.classList.add('done');
    }
};

/**
 * وظيفة مشاركة الذكر كصورة مع اسم المصحف واللمسات الجمالية الإضافية
 * تعليق جراحي: تقوم هذه الدالة باستخراج نص الذكر، التصنيف، وعدد التكرار وإرسالها للجافا
 */
window.shareZekr = function(btn) {
    const card = btn.closest('.zekr-card');
    const text = card.querySelector('.zekr-text').innerText;

    // محاولة جلب عدد التكرار من العداد
    const counterSpan = card.querySelector('.zekr-counter span:last-child');
    let count = "";
    if (counterSpan) {
        // استخراج الرقم فقط أو النص المعبر عن العدد
        count = counterSpan.innerText.replace('العدد: ', '').replace('المتبقي: ', '');
    }

    // محاولة جلب عنوان القسم (صباح/مساء/إلخ)
    let category = "";
    let prev = card.previousElementSibling;
    while (prev) {
        if (prev.classList.contains('azkar-section-header')) {
            category = prev.innerText;
            break;
        }
        prev = prev.previousElementSibling;
    }

    if (window.parent && window.parent.AndroidBridge && window.parent.AndroidBridge.shareZekrAsImage) {
        window.parent.AndroidBridge.shareZekrAsImage(text, category, count);
    } else {
        console.error("AndroidBridge.shareZekrAsImage not found");
        // fallback to standard text share
        if (window.parent && window.parent.AndroidBridge && window.parent.AndroidBridge.shareText) {
             let shareText = `*${category}*\n\n${text}\n\nالعدد: ${count}\n\nتمت المشاركة من تطبيق المصحف الكوني`;
             window.parent.AndroidBridge.shareText(shareText);
        }
    }
};

// تعليق جراحي: تم تحديث الدالة لتدعم الأذكار التي تتطلب تكراراً أكثر من مرة واحدة وميزة المشاركة كصورة مع تفاصيل إضافية
