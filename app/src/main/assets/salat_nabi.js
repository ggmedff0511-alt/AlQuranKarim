/**
 * المنطق البرمجي لشاشة الصلاة على النبي ﷺ - المصحف الكوني
 * تم التحديث لضمان التزامن الكامل مع نظام أندرويد وتشغيل الأصوات
 */

let goal = parseInt(localStorage.getItem('sn_goal')) || 500;
let salatNabiCount = parseInt(localStorage.getItem('q_sn_count') || 0);

window.onload = function() {
    checkDailyReset();
    checkStreakOnLoad();
    loadPersistentData();
    updateUI();
};

function checkDailyReset() {
    const lastDate = localStorage.getItem('sn_last_count_date');
    const today = new Date().toDateString();
    if (lastDate && lastDate !== today) {
        salatNabiCount = 0;
        localStorage.setItem('q_sn_count', 0);
    }
    localStorage.setItem('sn_last_count_date', today);
}

function checkStreakOnLoad() {
    const lastStreakDate = localStorage.getItem('sn_last_streak_date');
    if (!lastStreakDate) return;
    const today = new Date();
    today.setHours(0,0,0,0);
    const last = new Date(lastStreakDate);
    const diffDays = Math.floor((today - last) / (1000 * 60 * 60 * 24));
    if (diffDays > 1) localStorage.setItem('sn_streak', 0);
}

function loadPersistentData() {
    salatNabiCount = parseInt(localStorage.getItem('q_sn_count') || 0);
    goal = parseInt(localStorage.getItem('sn_goal')) || 500;
}

window.incrementSalatNabi = function() {
    salatNabiCount++;
    localStorage.setItem('q_sn_count', salatNabiCount);
    updateStreakLogic();
    if(typeof callNative === 'function') callNative('vibrate', 40);
    updateUI();
};

function updateStreakLogic() {
    const lastStreakDate = localStorage.getItem('sn_last_streak_date');
    const today = new Date().toDateString();
    let streak = parseInt(localStorage.getItem('sn_streak')) || 0;
    if (lastStreakDate !== today) {
        const yesterday = new Date();
        yesterday.setDate(yesterday.getDate() - 1);
        if (lastStreakDate === yesterday.toDateString()) streak++; else streak = 1;
        localStorage.setItem('sn_streak', streak);
        localStorage.setItem('sn_last_streak_date', today);
    }
}

window.updateUI = function() {
    const valEl = document.getElementById('salat-nabi-val');
    const progressEl = document.getElementById('sn-progress');
    const targetValEl = document.getElementById('sn-target-val');
    const streakEl = document.getElementById('sn-streak');
    const goalDisplayEl = document.getElementById('sn-goal-display');

    if (valEl) valEl.innerText = toArabic(salatNabiCount);
    if (targetValEl) targetValEl.innerText = toArabic(goal);
    if (goalDisplayEl) goalDisplayEl.innerText = toArabic(goal);
    if (streakEl) streakEl.innerText = toArabic(parseInt(localStorage.getItem('sn_streak')) || 0);

    if (progressEl) {
        const circumference = 596.9;
        const offset = circumference - (Math.min(salatNabiCount / goal, 1) * circumference);
        progressEl.style.strokeDashoffset = offset;
    }

    const isEnabled = localStorage.getItem('salat_nabi_notify') === 'true';
    const toggle = document.getElementById('sn-rem-toggle');
    if (toggle) toggle.checked = isEnabled;

    const statusText = document.getElementById('sn-status-text');
    if (statusText) {
        statusText.innerText = isEnabled ? "مفعل" : "معطل";
        statusText.className = "sn-rem-item-val " + (isEnabled ? "status-active" : "status-inactive");
    }

    const currentSoundName = localStorage.getItem('salat_nabi_sound_name') || "صل على محمد";
    const soundNameEl = document.getElementById('current-sound-name');
    const footerSoundNameEl = document.getElementById('footer-sound-name');
    if (soundNameEl) soundNameEl.innerHTML = `${currentSoundName} <span class="material-symbols-rounded" style="font-size:16px !important; margin-right:4px;">equalizer</span>`;
    if (footerSoundNameEl) footerSoundNameEl.innerText = currentSoundName + " - الصلاة على النبي";

    const interval = parseInt(localStorage.getItem('salat_nabi_interval')) || 15;
    const intervalTextEl = document.getElementById('sn-interval-text');
    if (intervalTextEl) intervalTextEl.innerText = getIntervalName(interval);
};

function getIntervalName(minutes) {
    if (minutes === 60) return "كل ساعة";
    if (minutes >= 60) return `كل ${toArabic(minutes/60)} ساعة`;
    return `كل ${toArabic(minutes)} دقيقة`;
}

window.toArabic = (n) => String(n).replace(/\d/g, d => "٠١٢٣٤٥٦٧٨٩"[d]);

window.changeGoal = function() {
    let newGoal = prompt("حدد هدفك اليومي:", goal);
    if (newGoal && !isNaN(newGoal)) {
        goal = parseInt(newGoal);
        localStorage.setItem('sn_goal', goal);
        if(typeof callNative === 'function') callNative('onSettingChanged', 'sn_goal', goal.toString());
        updateUI();
    }
};

window.resetCounter = function() {
    if(confirm("هل تريد تصفير عداد اليوم؟")) {
        salatNabiCount = 0;
        localStorage.setItem('q_sn_count', 0);
        updateUI();
    }
};

window.toggleReminder = function(checked) {
    localStorage.setItem('salat_nabi_notify', checked);
    if(typeof callNative === 'function') callNative('onSettingChanged', 'salat_nabi_notify', checked);
    updateUI();
};

window.openSoundPicker = function() {
    const modal = document.getElementById('sound-picker-modal');
    if (modal) { modal.style.display = 'block'; renderSoundOptions(); }
};

window.closeSoundPicker = function() {
    const modal = document.getElementById('sound-picker-modal');
    if (modal) modal.style.display = 'none';
};

function renderSoundOptions() {
    const container = document.getElementById('sound-options-list');
    if (!container) return;
    const sounds = [
        { id: "salli_ala_mohammad", name: "صل على محمد" },
        { id: "salat_maher", name: "الشيخ ماهر المعيقلي" },
        { id: "in_allah_yasser", name: "الشيخ ياسر الدوسري" }
    ];
    const currentId = localStorage.getItem('salat_nabi_sound') || "salli_ala_mohammad";
    let html = '';
    sounds.forEach(s => {
        const active = s.id === currentId ? 'active' : '';
        html += `<div class="sound-option-card ${active}" onclick="selectSalatSound('${s.id}', '${s.name}')">
            <div class="sound-info"><span>${s.name}</span></div>
        </div>`;
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

window.openIntervalPicker = function() {
    const modal = document.getElementById('interval-picker-modal');
    if (modal) { modal.style.display = 'block'; renderIntervalOptions(); }
};

window.closeIntervalPicker = function() {
    const modal = document.getElementById('interval-picker-modal');
    if (modal) modal.style.display = 'none';
};

function renderIntervalOptions() {
    const container = document.getElementById('interval-options-list');
    if (!container) return;
    const intervals = [5, 15, 30, 60, 120, 180];
    const currentVal = parseInt(localStorage.getItem('salat_nabi_interval')) || 15;
    let html = '';
    intervals.forEach(val => {
        const active = val === currentVal ? 'active' : '';
        html += `<div class="sound-option-card ${active}" onclick="selectInterval(${val})">
            <div class="sound-info"><span>${getIntervalName(val)}</span></div>
        </div>`;
    });
    container.innerHTML = html;
}

window.selectInterval = function(minutes) {
    localStorage.setItem('salat_nabi_interval', minutes);
    if(typeof callNative === 'function') callNative('onSettingChanged', 'salat_nabi_interval', minutes.toString());
    updateUI();
    closeIntervalPicker();
};

window.playSample = function() {
    const soundId = localStorage.getItem('salat_nabi_sound') || "salli_ala_mohammad";
    if(typeof callNative === 'function') callNative('playSalatSoundSample', soundId);
};

window.shareStats = function() {
    const text = `أتممت بفضل الله ${toArabic(salatNabiCount)} صلاة على النبي ﷺ اليوم عبر تطبيق المصحف الكوني.`;
    if(typeof callNative === 'function') callNative('shareText', text);
};
