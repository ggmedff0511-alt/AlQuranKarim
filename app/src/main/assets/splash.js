function startApp() {
    // أنميشن الخروج من شاشة الترحيب
    const splash = document.getElementById('splash-screen');
    splash.style.opacity = '0';
    setTimeout(() => {
        splash.style.display = 'none';
        document.getElementById('main-app-content').style.display = 'flex';
        // تهيئة التطبيق الرئيسي
        if(window.onloadMain) window.onloadMain();
    }, 500);
}

// يمكن إضافة أي منطق خاص بالترحيب هنا (مثل التحقق من المرة الأولى للفتح)
