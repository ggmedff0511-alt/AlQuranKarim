/**
 * المنطق البرمجي للبحث المتقدم - المصحف الكوني
 * تم تطويره لدعم البحث المتزامن في القرآن الكريم والأحاديث النبوية
 */

var searchTimeout = null;
var currentSearchTab = 'all';
var lastQuery = '';
var quranResults = [];
var hadithResults = [];

function debounceSearch(query) {
    clearTimeout(searchTimeout);
    lastQuery = query.trim();
    if (lastQuery.length < 2) {
        document.getElementById('search-results-container').innerHTML = '<div class="empty-state"><span class="material-symbols-rounded">manage_search</span><p>اكتب كلمة للبحث في كتاب الله وسنة نبيه ﷺ</p></div>';
        return;
    }
    searchTimeout = setTimeout(function() {
        performSearch(lastQuery);
    }, 500);
}

function performSearch(query) {
    quranResults = [];
    hadithResults = [];
    document.getElementById('search-results-container').innerHTML = '<div class="empty-state"><div class="loader-spinner"></div><p>جاري البحث في الكنوز الإسلامية...</p></div>';

    // استدعاء البحث في القرآن (Offline)
    window.parent.callNative('searchQuran', query, 'onQuranSearchReceived');

    // استدعاء البحث في الأحاديث (Room DB)
    window.parent.callNative('searchHadiths', query, 'onHadithSearchReceived');
}

window.onQuranSearchReceived = function(b64) {
    try {
        var data = JSON.parse(window.parent.decodeSafeData(b64));
        quranResults = data || [];
        renderAllResults();
    } catch(e) { console.error("Quran Search Error", e); }
};

window.onHadithSearchReceived = function(b64) {
    try {
        var data = JSON.parse(window.parent.decodeSafeData(b64));
        hadithResults = data || [];
        renderAllResults();
    } catch(e) { console.error("Hadith Search Error", e); }
};

function switchSearchTab(el, tab) {
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    el.classList.add('active');
    currentSearchTab = tab;
    renderAllResults();
}

function renderAllResults() {
    var container = document.getElementById('search-results-container');
    var html = '';

    var filteredQuran = (currentSearchTab === 'all' || currentSearchTab === 'quran') ? quranResults : [];
    var filteredHadith = (currentSearchTab === 'all' || currentSearchTab === 'hadith') ? hadithResults : [];

    if (filteredQuran.length === 0 && filteredHadith.length === 0) {
        container.innerHTML = '<div class="empty-state"><span class="material-symbols-rounded">search_off</span><p>لم يتم العثور على نتائج لـ "' + lastQuery + '"</p></div>';
        return;
    }

    // عرض نتائج القرآن
    filteredQuran.forEach(function(item) {
        html += '<div class="result-card" onclick="goToAyah(' + item.surah + ',' + item.ayah + ')">' +
                '<span class="result-type-tag quran-tag">القرآن الكريم</span>' +
                '<div class="result-text">' + highlightText(item.text, lastQuery) + '</div>' +
                '<div class="result-meta">' +
                '<span>سورة ' + window.parent.surahNames[item.surah] + '</span>' +
                '<span>الآية ' + window.parent.toAr(item.ayah) + '</span>' +
                '</div></div>';
    });

    // عرض نتائج الأحاديث
    filteredHadith.forEach(function(item) {
        html += '<div class="result-card" onclick="viewHadith(\'' + item.book + '\')">' +
                '<span class="result-type-tag hadith-tag">حديث شريف</span>' +
                '<div class="result-text">' + highlightText(item.text, lastQuery) + '</div>' +
                '<div class="result-meta">' +
                '<span>' + (item.title || 'باب') + '</span>' +
                '<span>المصدر: ' + getBookName(item.book) + '</span>' +
                '</div></div>';
    });

    container.innerHTML = html;
}

function highlightText(text, query) {
    if (!query) return text;
    var reg = new RegExp(query, "gi");
    return text.replace(reg, function(matched) {
        return '<span class="highlight">' + matched + '</span>';
    });
}

function getBookName(id) {
    var books = {
        'sahih_bukhari': 'صحيح البخاري',
        'sahih_muslim': 'صحيح مسلم',
        'sunan_abi_dawood': 'سنن أبي داود',
        'jamia_tirmidhi': 'جامع الترمذي',
        'riyadh_salihin': 'رياض الصالحين',
        'forty_hadith': 'الأربعون النووية'
    };
    return books[id] || 'المكتبة الإسلامية';
}

function goToAyah(surah, ayah) {
    window.parent.loadPage('reading', { surah: surah, ayah: ayah });
}

function viewHadith(bookId) {
    window.parent.loadPage(bookId);
}
