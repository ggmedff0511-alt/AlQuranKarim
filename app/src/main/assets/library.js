/**
 * المنطق البرمجي المطور لشاشة المكتبة الإسلامية - المصحف الكوني
 * تعليق جراحي: تم تفعيل المزامنة الحقيقية، والبحث المتقدم، وربط الشاشة الأصلية (Native).
 */

window.goBack = () => {
    if (window.parent && window.parent.loadPage) {
        window.parent.loadPage('home');
    } else {
        window.history.back();
    }
};

window.downloadBook = function(bookId, event) {
    if (event) event.stopPropagation();
    const idMap = {
        'sahih_bukhari': 'ara-bukhari',
        'sahih_muslim': 'ara-muslim',
        'sunan_abi_dawood': 'ara-abudawud',
        'jamia_tirmidhi': 'ara-tirmidhi',
        'riyadh_salihin': 'ara-riyadussalihin',
        'forty_hadith': 'ara-nawawi'
    };
    const dbId = idMap[bookId] || bookId;

    if (window.AndroidBridge && window.AndroidBridge.downloadHadithBook) {
        window.AndroidBridge.downloadHadithBook(dbId);
        window.AndroidBridge.showToast("جاري بدء التحميل...");
    }
    window.checkDownloads();
};

window.checkDownloads = function() {
    const books = ['sahih_bukhari', 'sahih_muslim', 'sunan_abi_dawood', 'jamia_tirmidhi', 'riyadh_salihin', 'forty_hadith'];
    const idMap = {
        'sahih_bukhari': 'ara-bukhari',
        'sahih_muslim': 'ara-muslim',
        'sunan_abi_dawood': 'ara-abudawud',
        'jamia_tirmidhi': 'ara-tirmidhi',
        'riyadh_salihin': 'ara-riyadussalihin',
        'forty_hadith': 'ara-nawawi'
    };

    books.forEach(id => {
        const dbId = idMap[id] || id;
        const wrapEl = document.getElementById('card-' + id);
        const statusEl = document.getElementById('status-' + id);
        const actionsEl = document.getElementById('actions-' + id);

        let isDownloaded = false;
        let isDownloading = false;

        if (window.AndroidBridge) {
            isDownloaded = window.AndroidBridge.isBookDownloaded(dbId);
            isDownloading = window.AndroidBridge.isBookDownloading(dbId);
        }

        if (isDownloading) {
            if (wrapEl) wrapEl.classList.remove('is-ready');
            if (statusEl) statusEl.innerText = "جاري التحميل والمزامنة...";
            if (actionsEl) actionsEl.innerHTML = `<div class="spinner"></div>`;
        } else if (isDownloaded) {
            if (wrapEl) wrapEl.classList.add('is-ready');
            if (statusEl) statusEl.innerText = "جاهز للقراءة أوفلاين";
            if (actionsEl) {
                actionsEl.innerHTML = `<div class="action-btn-circle btn-delete" onclick="event.stopPropagation(); window.deleteBook('${id}')"><span class="material-symbols-rounded">delete</span></div>`;
            }
        } else {
            if (wrapEl) wrapEl.classList.remove('is-ready');
            if (statusEl) statusEl.innerText = "يتطلب تحميل المحتوى";
            if (actionsEl) {
                actionsEl.innerHTML = `<div class="action-btn-circle btn-download" onclick="window.downloadBook('${id}', event)"><span class="material-symbols-rounded">download_for_offline</span></div>`;
            }
        }
    });
    window.updateStats();
};

window.openBook = function(bookId) {
    const idMap = { 'sahih_bukhari': 'ara-bukhari', 'sahih_muslim': 'ara-muslim', 'sunan_abi_dawood': 'ara-abudawud', 'jamia_tirmidhi': 'ara-tirmidhi', 'riyadh_salihin': 'ara-riyadussalihin', 'forty_hadith': 'ara-nawawi' };
    const dbId = idMap[bookId] || bookId;
    const bookTitle = document.querySelector(`#card-${bookId} .b-name`).innerText;

    if (window.AndroidBridge && window.AndroidBridge.isBookDownloaded(dbId)) {
        window.AndroidBridge.openHadithBookNative(dbId, bookTitle);
    } else if (window.AndroidBridge.isBookDownloading && window.AndroidBridge.isBookDownloading(dbId)) {
        window.AndroidBridge.showToast("يرجى الانتظار حتى اكتمال التحميل");
    } else {
        window.AndroidBridge.showToast("يرجى تحميل الكتاب أولاً");
    }
};

window.deleteBook = function(bookId) {
    const idMap = { 'sahih_bukhari': 'ara-bukhari', 'sahih_muslim': 'ara-muslim', 'sunan_abi_dawood': 'ara-abudawud', 'jamia_tirmidhi': 'ara-tirmidhi', 'riyadh_salihin': 'ara-riyadussalihin', 'forty_hadith': 'ara-nawawi' };
    const dbId = idMap[bookId] || bookId;
    if (window.AndroidBridge && window.AndroidBridge.deleteHadithBook) {
        window.AndroidBridge.deleteHadithBook(dbId);
        window.AndroidBridge.showToast("تم حذف الكتاب");
    }
    setTimeout(window.checkDownloads, 500);
};

window.updateStats = function() {
    if (window.AndroidBridge && window.AndroidBridge.getTotalDownloadedHadiths) {
        const count = window.AndroidBridge.getTotalDownloadedHadiths();
        const el = document.getElementById('total-hadiths-count');
        if (el) el.innerText = count.toLocaleString('ar-EG') + " حديث نبوي";
    }
};

/**
 * ميزة البحث والفلترة المضافة جراحياً
 */
window.filterBooks = function(query) {
    const cards = document.querySelectorAll('.book-card-h');
    const searchTerm = query.trim().toLowerCase();
    cards.forEach(card => {
        const title = card.querySelector('.b-name').innerText.toLowerCase();
        if (title.includes(searchTerm)) {
            card.style.display = 'flex';
        } else {
            card.style.display = 'none';
        }
    });
};

window.selectTab = function(el, type) {
    document.querySelectorAll('.tab-item').forEach(t => t.classList.remove('active'));
    el.classList.add('active');

    const cards = document.querySelectorAll('.book-card-h');
    cards.forEach(card => {
        if (type === 'all' || card.getAttribute('data-type') === type) {
            card.style.display = 'flex';
        } else {
            card.style.display = 'none';
        }
    });
};

window.onload = function() {
    window.checkDownloads();
    setInterval(window.checkDownloads, 3000);
};
