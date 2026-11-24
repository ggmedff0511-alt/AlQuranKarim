/**
 * المحرك الرئيسي لإدارة الصفحات والمزامنة
 */
window.onAudioUpdate = function(status, surah, ayah, repeat, reciterName) {
    // تمرير التحديث إلى الصفحة الداخلية (iframe) إذا كانت هي شاشة القراءة
    const contentFrame = document.getElementById('content-frame');
    if (contentFrame && contentFrame.contentWindow && contentFrame.contentWindow.onAudioUpdate) {
        contentFrame.contentWindow.onAudioUpdate(status, surah, ayah, repeat, reciterName);
    }
};

window.loadPage = function(pageName) {
    const frame = document.getElementById('content-frame');
    if (frame) {
        frame.src = pageName + '.html';
    }
};
