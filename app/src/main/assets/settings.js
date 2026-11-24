/**
 * settings.js - المنطق البرمجي لشاشة الإعدادات المطور
 * تم التحديث جراحياً ليتطابق مع التصميم المطلوب ويقوم بحفظ الإعدادات فعلياً في النظام
 */

window.onload = function() {
    renderSettings();
};

/**
 * تعليق جراحي: بناء واجهة الإعدادات ديناميكياً بناءً على الحالة المخزنة
 */
window.renderSettings = function() {
    const container = document.getElementById('settings-container');
    if (!container) return;

    // تعريف قائمة الإعدادات ومفاتيح التخزين الخاصة بها
    const settingsSchema = [
        {
            group: "التنبيهات والذكر",
            items: [
                { id: "azan_notify", title: "تنبيهات الأذان", desc: "صوت الأذان عند دخول الوقت", icon: "notifications" },
                { id: "daily_ayah_notify", title: "تنبيه آية اليوم", desc: "يظهر كل يوم الساعة ٩ مساءً", icon: "menu_book" },
                { id: "salat_nabi_notify", title: "تذكير الصلاة على النبي", desc: "تنبيه صوتي دوري", icon: "favorite" }
            ]
        },
        {
            group: "", // تابع للمجموعة الأولى حسب التصميم
            items: [
                { id: "salat_nabi_sound", title: "صوت التذكير الحالي", type: "select", icon: "person_pin", options: [
                    { val: "salat_1", text: "صل على محمد" },
                    { val: "salat_2", text: "اللهم صل على محمد" },
                    { val: "salat_3", text: "صلوا عليه وسلموا تسليما" }
                ]}
            ]
        },
        {
            group: "تفضيلات الواجهة",
            items: [
                { id: "splash_audio_notify", title: "صوت البداية", desc: "تشغيل آية قرآنية عند فتح التطبيق", icon: "volume_up" }
            ]
        }
    ];

    let html = '';
    settingsSchema.forEach(group => {
        if (group.group) {
            html += `<div class="settings-group-label">${group.group}</div>`;
        }

        group.items.forEach(item => {
            const isChecked = localStorage.getItem(item.id) === 'true';
            const selectedVal = localStorage.getItem(item.id) || (item.options ? item.options[0].val : '');

            html += `<div class="setting-card">
                <div class="setting-info">
                    <span class="material-symbols-rounded setting-icon-box">${item.icon}</span>
                    <div class="setting-texts">
                        <div class="setting-title">${item.title}</div>
                        ${item.desc ? `<div class="setting-desc">${item.desc}</div>` : ''}
                        ${item.type === 'select' ? `
                            <select class="setting-select-box" onchange="updateSelectSetting('${item.id}', this.value)">
                                ${item.options.map(opt => `<option value="${opt.val}" ${opt.val === selectedVal ? 'selected' : ''}>${opt.text}</option>`).join('')}
                            </select>
                        ` : ''}
                    </div>
                </div>
                ${item.type !== 'select' ? `
                <label class="m3-switch">
                    <input type="checkbox" id="${item.id}" ${isChecked ? 'checked' : ''} onchange="toggleSettingAction('${item.id}', this.checked)">
                    <span class="m3-slider"></span>
                </label>
                ` : ''}
            </div>`;
        });
    });

    container.innerHTML = html;
};

/**
 * تعليق جراحي: تحديث الإعدادات في التخزين المحلي والنظام الأصلي (أندرويد)
 */
window.toggleSettingAction = function(id, isChecked) {
    localStorage.setItem(id, isChecked);
    if (window.callNative) {
        window.callNative('onSettingChanged', id, isChecked);
        window.callNative('vibrate', 30);
    }
};

window.updateSelectSetting = function(id, value) {
    localStorage.setItem(id, value);
    if (window.callNative) {
        window.callNative('onSettingChanged', id, value);
        // تجربة الصوت عند الاختيار لتعزيز تجربة المستخدم
        if (id === 'salat_nabi_sound') {
            window.callNative('playSalatSoundSample', value);
        }
    }
};

window.goBack = function() {
    if (window.parent && window.parent.loadPage) {
        window.parent.loadPage('home');
    } else {
        window.history.back();
    }
};
