/**
 * المنطق البرمجي لشاشة الصلاة على النبي ﷺ - المصحف الكوني
 * تم التحديث لضمان عمل كافة المميزات (العداد، التذكير، المشاركة، والمتتالية)
 */

let goal = parseInt(localStorage.getItem('sn_goal')) || 500;
let salatNabiCount = parseInt(localStorage.getItem('q_sn_count') || 0);

/** تعليق: يتم استدعاء هذه الدالة عند فتح الشاشة لتنشيط العدادات وفحص المتتالية */
window.onload = function() {
    checkDailyReset();    // تعليق: تصفير عداد اليوم إذا تغير التاريخ
    checkStreakOnLoad();  // تعليق: فحص انقطاع المتتالية عند فتح التطبيق
    loadPersistentData();
    updateUI();
};

/** تعليق: تصفير العداد اليومي تلقائياً عند بداية يوم جديد */
function checkDailyReset() {
    const lastDate = localStorage.getItem('sn_last_count_date');
    const today = new Date().toDateString();
    if (lastDate && lastDate !== today) {
        salatNabiCount = 0;
        localStorage.setItem('q_sn_count', 0);
    }
    localStorage.setItem('sn_last_count_date', today);
}

/** تعليق: فحص انقطاع المتتالية (إذا مر أكثر من يوم بدون ذكر) */
function checkStreakOnLoad() {
    const lastStreakDate = localStorage.getItem('sn_last_streak_date');
    if (!lastStreakDate) return;

    const today = new Date();
    today.setHours(0,0,0,0);
    const last = new Date(lastStreakDate);
    last.setHours(0,0,0,0);

    const diffDays = Math.floor((today - last) / (1000 * 60 * 60 * 24));
    if (diffDays > 1) {
        localStorage.setItem('sn_streak', 0);
    }
}

function loadPersistentData() {
    salatNabiCount = parseInt(localStorage.getItem('q_sn_count') || 0);
    goal = parseInt(localStorage.getItem('sn_goal')) || 500;
}

/** تعليق: ميزة زيادة العداد - تعمل عند الضغط على الأيقونة المركزية لزيادة الذكر وتفعيل الاهتزاز */
window.incrementSalatNabi = function() {
    salatNabiCount++;
    localStorage.setItem('q_sn_count', salatNabiCount);

    updateStreakLogic(); // تعليق: تحديث المتتالية اليومية لضمان الاستمرارية

    // تعليق: إرسال أمر الاهتزاز لنظام أندرويد لتعزيز شعور المستخدم بالتفاعل
    if(typeof callNative === 'function') callNative('vibrate', 40);

    updateUI();
};

/** تعليق: منطق المتتالية - يحسب عدد الأيام المتصلة التي التزم فيها المستخدم بالورد */
function updateStreakLogic() {
    const lastStreakDate = localStorage.getItem('sn_last_streak_date');
    const today = new Date().toDateString();
    let streak = parseInt(localStorage.getItem('sn_streak')) || 0;

    if (lastStreakDate !== today) {
        const yesterday = new Date();
        yesterday.setDate(yesterday.getDate() - 1);

        if (lastStreakDate === yesterday.toDateString()) {
            streak++;
        } else {
            streak = 1;
        }
        localStorage.setItem('sn_streak', streak);
        localStorage.setItem('sn_last_streak_date', today);
    }
}

/** تعليق: تحديث كافة عناصر الواجهة (العداد، الدائرة، الحالة، الأصوات) */
window.updateUI = function() {
    const valEl = document.getElementById('salat-nabi-val');
    const progressEl = document.getElementById('sn-progress');
    const targetValEl = document.getElementById('sn-target-val');
    const goalDisplayEl = document.getElementById('sn-goal-display');
    const soundNameEl = document.getElementById('current-sound-name');
    const footerSoundNameEl = document.getElementById('footer-sound-name');
    const intervalTextEl = document.getElementById('sn-interval-text');
    const streakEl = document.getElementById('sn-streak');

    if (valEl) valEl.innerText = toArabic(salatNabiCount);
    if (targetValEl) targetValEl.innerText = toArabic(goal);
    if (goalDisplayEl) goalDisplayEl.innerText = toArabic(goal);
    if (streakEl) streakEl.innerText = toArabic(parseInt(localStorage.getItem('sn_streak')) || 0);

    // تعليق: ميزة الألماس التفاعلية - تضيء عند الوصول لـ 50% و 100% من الهدف
    const diamonds = document.querySelectorAll('.sn-diamond');
    if (diamonds.length >= 2) {
        const progress = salatNabiCount / goal;
        diamonds[0].className = 'sn-diamond ' + (progress >= 0.5 ? 'bright' : 'dim');
        diamonds[1].className = 'sn-diamond ' + (progress >= 1.0 ? 'bright' : 'dim');
    }

    // تحديث دائرة التقدم بصرياً بناءً على الهدف اليومي
    if (progressEl) {
        const circumference = 596.9;
        const offset = circumference - (Math.min(salatNabiCount / goal, 1) * circumference);
        progressEl.style.strokeDashoffset = offset;
    }

    // تعليق: تحديث حالة التذكير الصوتي (مفعل/معطل) في الواجهة
    const isEnabled = localStorage.getItem('salat_nabi_notify') === 'true';
    const toggle = document.getElementById('sn-rem-toggle');
    if (toggle) toggle.checked = isEnabled;
    const statusText = document.getElementById('sn-status-text');
    if (statusText) {
        statusText.innerText = isEnabled ? "مفعل" : "معطل";
        statusText.className = "sn-rem-item-val " + (isEnabled ? "status-active" : "status-inactive");
    }

    const currentSoundName = localStorage.getItem('salat_nabi_sound_name') || "صل على محمد";
    if (soundNameEl) soundNameEl.innerHTML = `${currentSoundName} <span class="material-symbols-rounded" style="font-size:16px !important; margin-right:4px;">equalizer</span>`;
    if (footerSoundNameEl) footerSoundNameEl.innerText = currentSoundName + " - الصلاة على النبي";

    const interval = parseInt(localStorage.getItem('salat_nabi_interval')) || 15;
    if (intervalTextEl) intervalTextEl.innerText = getIntervalName(interval);
};

function getIntervalName(minutes) {
    if (minutes === 5) return "كل ٥ دقائق";
    if (minutes === 15) return "كل ١٥ دقيقة";
    if (minutes === 30) return "كل ٣٠ دقيقة";
    if (minutes === 60) return "كل ساعة";
    if (minutes === 90) return "كل ساعة ونصف";
    if (minutes === 120) return "كل ساعتين";
    if (minutes === 180) return "كل ٣ ساعات";
    return `كل ${toArabic(minutes)} دقيقة`;
}

window.toArabic = (n) => String(n).replace(/\d/g, d => "٠١٢٣٤٥٦٧٨٩"[d]);

/** تعليق: ميزة تغيير الهدف - تتيح للمستخدم تحديد هدف يومي مخصص */
window.changeGoal = function() {
    let newGoal = prompt("حدد هدفك اليومي من الصلاة على النبي ﷺ:", goal);
    if (newGoal && !isNaN(newGoal)) {
        goal = parseInt(newGoal);
        localStorage.setItem('sn_goal', goal);
        updateUI();
    }
};

/** تعليق: ميزة تصفير العداد - لمسح سجل اليوم الحالي والبدء من جديد */
window.resetCounter = function() {
    if(confirm("هل تريد تصفير عداد اليوم؟")) {
        salatNabiCount = 0;
        localStorage.setItem('q_sn_count', 0);
        updateUI();
    }
};

/** تعليق: ميزة التبديل السريع - تفعيل أو تعطيل التذكيرات الصوتية في نظام أندرويد */
window.toggleReminder = function(checked) {
    localStorage.setItem('salat_nabi_notify', checked);
    if(typeof callNative === 'function') callNative('onSettingChanged', 'salat_nabi_notify', checked);
    updateUI();
};

/** تعليق: ميزة اختيار الصوت - تفتح قائمة لاختيار صوت التذكير المفضل */
window.openSoundPicker = function() {
    const modal = document.getElementById('sound-picker-modal');
    if (modal) {
        modal.style.display = 'block';
        renderSoundOptions();
    }
};

window.closeSoundPicker = function() {
    const modal = document.getElementById('sound-picker-modal');
    if (modal) modal.style.display = 'none';
};

function renderSoundOptions() {
    const container = document.getElementById('sound-options-list');
    if (!container) return;

    const sounds = [
        { id: "salli_ala_mohammad", name: "صل على محمد (افتراضي)" },
        { id: "salat_maher", name: "الشيخ ماهر المعيقلي" },
        { id: "in_allah_yasser", name: "الشيخ ياسر الدوسري" }
    ];

    const currentId = localStorage.getItem('salat_nabi_sound') || "salli_ala_mohammad";

    let html = '';
    sounds.forEach(s => {
        const active = s.id === currentId ? 'active' : '';
        html += `
            <div class="sound-option-card ${active}" onclick="selectSalatSound('${s.id}', '${s.name}')">
                <div class="sound-info">
                    <span class="material-symbols-rounded">${active ? 'check_circle' : 'music_note'}</span>
                    <span>${s.name}</span>
                </div>
            </div>
        `;
    });
    container.innerHTML = html;
}

window.selectSalatSound = function(id, name) {
    localStorage.setItem('salat_nabi_sound', id);
    localStorage.setItem('salat_nabi_sound_name', name);
    if(typeof callNative === 'function') callNative('onSettingChanged', 'salat_nabi_sound', id);
    updateUI();
    closeSoundPicker();
    playSample();
};

/** تعليق: ميزة فترة التكرار - تتيح التحكم في وتيرة ظهور التذكيرات الصوتية */
window.openIntervalPicker = function() {
    const modal = document.getElementById('interval-picker-modal');
    if (modal) {
        modal.style.display = 'block';
        renderIntervalOptions();
    }
};

window.closeIntervalPicker = function() {
    const modal = document.getElementById('interval-picker-modal');
    if (modal) modal.style.display = 'none';
};

function renderIntervalOptions() {
    const container = document.getElementById('interval-options-list');
    if (!container) return;

    const intervals = [
        { val: 5, name: "٥ دقائق" }, { val: 15, name: "١٥ دقيقة" }, { val: 30, name: "٣٠ دقيقة" },
        { val: 60, name: "ساعة" }, { val: 90, name: "ساعة ونصف" }, { val: 120, name: "ساعتين" }, { val: 180, name: "٣ ساعات" }
    ];

    const currentVal = parseInt(localStorage.getItem('salat_nabi_interval')) || 15;
    let html = '';
    intervals.forEach(item => {
        const active = item.val === currentVal ? 'active' : '';
        html += `
            <div class="sound-option-card ${active}" onclick="selectInterval(${item.val})">
                <div class="sound-info">
                    <span class="material-symbols-rounded">${active ? 'check_circle' : 'schedule'}</span>
                    <span>${item.name}</span>
                </div>
            </div>
        `;
    });
    container.innerHTML = html;
}

window.selectInterval = function(minutes) {
    localStorage.setItem('salat_nabi_interval', minutes);
    if(typeof callNative === 'function') callNative('onSettingChanged', 'salat_nabi_interval', minutes);
    updateUI();
    closeIntervalPicker();
};

/** تعليق: ميزة المعاينة الصوتية - تشغيل عينة من الصوت المختار للتأكد منه قبل الحفظ */
window.playSample = function() {
    const soundId = localStorage.getItem('salat_nabi_sound') || "salli_ala_mohammad";
    if(typeof callNative === 'function') callNative('playSalatSoundSample', soundId);
};

/** تعليق: ميزة المشاركة - تتيح مشاركة إنجاز الذكر اليومي مع الآخرين عبر تطبيقات التواصل */
window.shareStats = function() {
    const text = `أتممت بفضل الله ${toArabic(salatNabiCount)} صلاة على النبي ﷺ اليوم عبر تطبيق المصحف الكوني. اللهم صلِّ وسلم وبارك على نبينا محمد.`;
    if(typeof callNative === 'function') callNative('shareText', text);
};
