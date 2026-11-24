/**
 * المنطق البرمجي المطور لشاشة المسبحة الإلكترونية الذكية - المصحف الكوني
 * تحديث جراحي: إضافة ميزات الأهداف الذكية، إجمالي التسبيحات، وحلقة التقدم التفاعلية
 */

let count = parseInt(localStorage.getItem('q_tas')) || 0;
let cycle = parseInt(localStorage.getItem('q_tas_cycle')) || 1;
let totalCount = parseInt(localStorage.getItem('q_tas_total')) || 0;
let targetGoal = parseInt(localStorage.getItem('q_tas_goal')) || 33;

// تعليق جراحي: قائمة الأذكار الذكية المدعومة في المسبحة
const adhkarList = [
    "سبحان الله",
    "الحمد لله",
    "الله أكبر",
    "لا إله إلا الله",
    "أستغفر الله",
    "سبحان الله وبحمده",
    "لا حول ولا قوة إلا بالله",
    "اللهم صل على محمد"
];
let zekrIdx = parseInt(localStorage.getItem('q_tas_zekr_idx')) || 0;

window.onload = function() {
    initProgressRing();
    updateTasbeehUI();
};

window.goBack = function() {
    if (window.parent && window.parent.loadPage) {
        window.parent.loadPage('home');
    }
};

/**
 * تعليق جراحي: تهيئة حلقة التقدم الدائرية
 */
function initProgressRing() {
    const circle = document.getElementById('progress-bar');
    if (circle) {
        const radius = circle.r.baseVal.value;
        const circumference = radius * 2 * Math.PI;
        circle.style.strokeDasharray = `${circumference} ${circumference}`;
        circle.style.strokeDashoffset = circumference;
    }
}

/**
 * تعليق جراحي: دالة التسبيح الذكية
 * تدير العداد الحالي، الإجمالي، ومنطق الانتقال بين الأذكار تلقائياً
 */
window.runTasbeeh = function() {
    count++;
    totalCount++;

    // اهتزاز لمسي لتعزيز تجربة المستخدم
    const bridge = window.AndroidBridge || (window.parent && window.parent.AndroidBridge);
    if (bridge && typeof bridge.vibrate === 'function') {
        // اهتزاز أقوى عند اكتمال الهدف
        bridge.vibrate(count % targetGoal === 0 ? 80 : 40);

        // ميزة ذكية: تشغيل صوت خفيف عند اكتمال الدورة إذا كان مفعل في الإعدادات
        if (count % targetGoal === 0 && typeof bridge.playSalatSoundSample === 'function') {
             // يمكن استخدام صوت قصير للتنبيه باكمال العد
        }
    }

    // منطق ذكي: إذا وصل للهدف الحالي، يهتز بشكل أقوى ويبدأ دورة جديدة أو يكمل
    if (count >= targetGoal) {
        if (targetGoal === 33) {
             // الوضع التقليدي: سبحان الله (33) -> الحمد لله (33) -> الله أكبر (33)
             let step = Math.floor(((totalCount-1) % 99) / 33);
             if (step === 2) {
                 // اتممنا 99 تسبيحة (دورة كاملة)
                 count = 0;
                 cycle++;
             } else {
                 count = 0;
             }
        } else {
            // للأهداف الأخرى (100، 1000)
            count = 0;
            cycle++;
        }
    }

    saveData();
    updateTasbeehUI();
};

/**
 * تعليق جراحي: التبديل بين الأذكار يدوياً بنقرة واحدة على النص
 */
window.nextZekr = function() {
    zekrIdx = (zekrIdx + 1) % adhkarList.length;
    localStorage.setItem('q_tas_zekr_idx', zekrIdx);
    // تصفير العداد الحالي عند تغيير الذكر للبدء من جديد
    count = 0;
    updateTasbeehUI();
};

/**
 * تعليق جراحي: تغيير الهدف يدوياً أو عبر الخيارات السريعة
 */
window.setGoal = function(goal) {
    targetGoal = goal;
    count = 0;
    localStorage.setItem('q_tas_goal', targetGoal);
    updateTasbeehUI();
};

window.changeGoal = function() {
    let g = prompt("حدد هدفاً مخصصاً للتسبيح:", targetGoal);
    if (g && !isNaN(g)) {
        setGoal(parseInt(g));
    }
};

window.resetTasbeehAction = function() {
    if(confirm("هل تريد تصفير العداد الحالي والدورة؟")) {
        count = 0;
        cycle = 1;
        saveData();
        updateTasbeehUI();
    }
};

function saveData() {
    localStorage.setItem('q_tas', count);
    localStorage.setItem('q_tas_cycle', cycle);
    localStorage.setItem('q_tas_total', totalCount);
}

/**
 * تعليق جراحي: تحديث الواجهة والرسوميات (حلقة التقدم والنصوص)
 */
window.updateTasbeehUI = function() {
    const valEl = document.getElementById('tasbeeh-val');
    const txtEl = document.getElementById('tasbeeh-text');
    const cycleEl = document.getElementById('cycle-label');
    const totalEl = document.getElementById('total-tasbeeh');
    const goalEl = document.getElementById('goal-val');
    const progressBar = document.getElementById('progress-bar');
    const circleOuter = document.querySelector('.tasbeeh-circle-outer');

    const toAr = (n) => typeof toArDigits === 'function' ? toArDigits(n) : String(n).replace(/\d/g, d => "٠١٢٣٤٥٦٧٨٩"[d]);

    if (valEl) valEl.innerText = toAr(count);
    if (cycleEl) cycleEl.innerText = "الدورة: " + toAr(cycle);
    if (totalEl) totalEl.innerText = toAr(totalCount);
    if (goalEl) goalEl.innerText = toAr(targetGoal);

    // تحديث حلقة التقدم بصرياً
    if (progressBar) {
        const radius = progressBar.r.baseVal.value;
        const circumference = radius * 2 * Math.PI;
        const offset = circumference - (Math.min(count / targetGoal, 1)) * circumference;
        progressBar.style.strokeDashoffset = offset;

        // ميزة ذكية بنبض الدائرة عند الاقتراب من الهدف
        if (count >= targetGoal - 5 && count < targetGoal) {
            progressBar.style.stroke = "#FFD700";
        } else {
            progressBar.style.stroke = "#D9B24C";
        }
    }

    // منطق الذكر الذكي: يتغير النص بناءً على الهدف أو الاختيار اليدوي
    if (txtEl) {
        if (targetGoal === 33) {
            let step = Math.floor((totalCount % 99) / 33);
            if (step === 0) txtEl.innerText = "سبحان الله";
            else if (step === 1) txtEl.innerText = "الحمد لله";
            else txtEl.innerText = "الله أكبر";
        } else {
            txtEl.innerText = adhkarList[zekrIdx];
        }
    }
};
