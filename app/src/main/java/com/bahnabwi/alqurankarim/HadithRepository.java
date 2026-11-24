package com.bahnabwi.alqurankarim;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.lifecycle.LiveData;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * مستودع البيانات المطور - نسخة الإصلاح النهائي الشامل للمكتبة الإسلامية.
 * تم إضافة حماية كاملة ضد القيم الفارغة (Null Safety) واستكمال التراجم.
 */
public class HadithRepository {
    private final BookDao bookDao;
    private final SectionDao sectionDao;
    private final HadithEntryDao hadithEntryDao;
    private final ExecutorService executorService;
    private final OkHttpClient client;
    private final Context context;

    public HadithRepository(Context context) {
        this.context = context;
        AppDatabase db = AppDatabase.getInstance(context);
        this.bookDao = db.bookDao();
        this.sectionDao = db.sectionDao();
        this.hadithEntryDao = db.hadithEntryDao();
        this.executorService = Executors.newFixedThreadPool(4);
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)
                .build();
        resetDownloadStates();
    }

    private void resetDownloadStates() {
        executorService.execute(() -> {
            try {
                List<BookEntity> books = bookDao.getAllBooksSync();
                if (books != null) {
                    for (BookEntity b : books) {
                        if (b.isDownloading) bookDao.updateDownloadStatus(b.bookId, b.isDownloaded, false);
                    }
                    notifyWebUI();
                }
            } catch (Exception ignored) {}
        });
    }

    public LiveData<List<BookEntity>> getAllBooks() { return bookDao.getAllBooks(); }
    public BookEntity getBookStatus(String bookId) { return bookId != null ? bookDao.getBookById(bookId) : null; }
    
    public boolean isBookDownloaded(String bookId) {
        if (bookId == null) return false;
        BookEntity book = bookDao.getBookById(bookId);
        return book != null && book.isDownloaded;
    }

    public boolean isBookDownloading(String bookId) {
        if (bookId == null) return false;
        BookEntity book = bookDao.getBookById(bookId);
        return book != null && book.isDownloading;
    }

    public int getTotalDownloadedHadiths() {
        try { return bookDao.getTotalDownloadedHadiths(); } catch (Exception e) { return 0; }
    }

    public List<SectionEntity> getSections(String bookId) {
        if (bookId == null) return new ArrayList<>();
        List<SectionEntity> sections = sectionDao.getSectionsByBook(bookId);
        if (sections == null) return new ArrayList<>();
        for (SectionEntity s : sections) {
            s.sectionTitle = getArabicTitle(bookId, s.sectionNumber, s.sectionTitle);
        }
        return sections;
    }

    public List<HadithEntity> getHadiths(String bookId, int sectionId) { 
        if (bookId == null) return new ArrayList<>();
        List<HadithEntity> res = hadithEntryDao.getHadithsBySection(bookId, sectionId);
        return res != null ? res : new ArrayList<>();
    }

    public List<HadithEntity> searchHadiths(String bookId, String query) { 
        if (bookId == null || query == null) return new ArrayList<>();
        return hadithEntryDao.searchInBook(bookId, query); 
    }

    public void downloadBook(BookEntity book) {
        if (book == null) return;
        executorService.execute(() -> {
            boolean success = false;
            try {
                bookDao.updateDownloadStatus(book.bookId, false, true);
                notifyWebUI();
                bookDao.deleteHadithsByBook(book.bookId);
                bookDao.deleteSectionsByBook(book.bookId);
                Response resp = client.newCall(new Request.Builder().url(book.downloadUrl).build()).execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    try (JsonReader reader = new JsonReader(new InputStreamReader(resp.body().byteStream(), "UTF-8"))) {
                        reader.setLenient(true);
                        int count = 0;
                        reader.beginObject();
                        while (reader.hasNext()) {
                            String name = reader.nextName();
                            if ("metadata".equalsIgnoreCase(name)) parseMetadata(book.bookId, reader);
                            else if ("hadiths".equalsIgnoreCase(name)) count = parseHadithsStream(book.bookId, reader);
                            else reader.skipValue();
                        }
                        reader.endObject();
                        bookDao.updateHadithCount(book.bookId, count);
                        success = count > 0;
                    }
                }
            } catch (Exception e) { Log.e("HadithRepo", "Download Error", e); }
            finally { 
                bookDao.updateDownloadStatus(book.bookId, success, false); 
                notifyWebUI(); 
            }
        });
    }

    private void parseMetadata(String bookId, JsonReader r) throws IOException {
        if (r.peek() != JsonToken.BEGIN_OBJECT) { r.skipValue(); return; }
        r.beginObject();
        while (r.hasNext()) {
            if (r.nextName().equalsIgnoreCase("sections")) {
                if (r.peek() != JsonToken.BEGIN_OBJECT) { r.skipValue(); continue; }
                r.beginObject();
                List<SectionEntity> list = new ArrayList<>();
                while (r.hasNext()) {
                    String key = r.nextName();
                    String title = safeReadString(r);
                    int num = 0; try { num = (int) Double.parseDouble(key); } catch(Exception ignored) {}
                    list.add(new SectionEntity(bookId, num, getArabicTitle(bookId, num, title)));
                }
                sectionDao.insertSections(list);
                r.endObject();
            } else r.skipValue();
        }
        r.endObject();
    }

    private int parseHadithsStream(String bookId, JsonReader r) throws IOException {
        if (r.peek() != JsonToken.BEGIN_ARRAY) { r.skipValue(); return 0; }
        int count = 0; List<HadithEntity> chunk = new ArrayList<>();
        r.beginArray();
        while (r.hasNext()) {
            HadithEntity h = readSingleHadith(bookId, r);
            if (h != null) {
                chunk.add(h); count++;
                if (chunk.size() >= 500) { hadithEntryDao.insertHadiths(chunk); chunk.clear(); }
            }
        }
        if (!chunk.isEmpty()) hadithEntryDao.insertHadiths(chunk);
        r.endArray();
        return count;
    }

    private HadithEntity readSingleHadith(String bookId, JsonReader r) throws IOException {
        if (r.peek() != JsonToken.BEGIN_OBJECT) { r.skipValue(); return null; }
        int hNum = 0, sNum = 0; String aNum = "", text = "";
        r.beginObject();
        while (r.hasNext()) {
            String name = r.nextName();
            if (name.equalsIgnoreCase("hadithnumber")) hNum = safeReadInt(r);
            else if (name.equalsIgnoreCase("arabicnumber")) aNum = safeReadString(r);
            else if (name.equalsIgnoreCase("text")) text = safeReadString(r);
            else if (name.equalsIgnoreCase("reference")) {
                if (r.peek() == JsonToken.BEGIN_OBJECT) {
                    r.beginObject();
                    while(r.hasNext()) {
                        String ref = r.nextName();
                        if (ref.equalsIgnoreCase("book") || ref.equalsIgnoreCase("chapter")) sNum = safeReadInt(r);
                        else r.skipValue();
                    }
                    r.endObject();
                } else r.skipValue();
            } else r.skipValue();
        }
        r.endObject();
        return new HadithEntity(bookId, hNum, aNum, text, HadithEntity.removeDiacritics(text), sNum);
    }

    private int safeReadInt(JsonReader r) throws IOException {
        JsonToken t = r.peek();
        if (t == JsonToken.NUMBER) return (int) r.nextDouble();
        if (t == JsonToken.STRING) { try { return Integer.parseInt(r.nextString()); } catch (Exception e) { return 0; } }
        if (t == JsonToken.NULL) { r.nextNull(); return 0; }
        r.skipValue(); return 0;
    }

    private String safeReadString(JsonReader r) throws IOException {
        JsonToken t = r.peek();
        if (t == JsonToken.STRING) return r.nextString();
        if (t == JsonToken.NUMBER) return String.valueOf(r.nextDouble()).replace(".0", "");
        if (t == JsonToken.NULL) { r.nextNull(); return ""; }
        r.skipValue(); return "";
    }

    private void notifyWebUI() {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (context instanceof MainActivity) ((MainActivity) context).runJS("if(window.checkDownloads) window.checkDownloads();");
        });
    }

    public void initializeDefaultBooks() {
        executorService.execute(() -> {
            String[] ids = {"ara-bukhari", "ara-muslim", "ara-abudawud", "ara-tirmidhi", "ara-riyadussalihin", "ara-nawawi"};
            String[] titles = {"صحيح البخاري", "صحيح مسلم", "سنن أبي داود", "جامع الترمذي", "رياض الصالحين", "الأربعون النووية"};
            String[] urls = {
                "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ara-bukhari.min.json",
                "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ara-muslim.min.json",
                "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ara-abudawud.min.json",
                "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ara-tirmidhi.min.json",
                "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ara-riyadussalihin.min.json",
                "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ara-nawawi.min.json"
            };
            for (int i = 0; i < ids.length; i++) {
                if (bookDao.getBookById(ids[i]) == null) bookDao.insertBook(new BookEntity(ids[i], titles[i], urls[i], "10MB"));
            }
        });
    }

    public void deleteBookData(String bookId) {
        if (bookId == null) return;
        executorService.execute(() -> { 
            bookDao.deleteHadithsByBook(bookId); 
            bookDao.deleteSectionsByBook(bookId); 
            bookDao.updateDownloadStatus(bookId, false, false); 
            notifyWebUI(); 
        });
    }

    /**
     * تعليق جراحي: توحيد آلية جلب العناوين العربية لجميع الكتب لضمان عدم ظهور نصوص إنجليزية في الفهرس.
     * تم إضافة معالجة خاصة للأربعين النووية وتغيير المقارنة لتكون أكثر مرونة مع المعرفات.
     */
    private String getArabicTitle(String bookId, int s, String d) {
        if (bookId == null) return d;
        if (bookId.contains("bukhari")) return getBukhariTitle(s, d);
        if (bookId.contains("muslim")) return getMuslimTitle(s, d);
        if (bookId.contains("tirmidhi")) return getTirmidhiTitle(s, d);
        if (bookId.contains("abudawud")) return getAbudawudTitle(s, d);
        if (bookId.contains("riyadussalihin")) return getRiyadhTitle(s, d);
        if (bookId.contains("nawawi")) return (s == 0) ? "مقدمة الكتاب" : "متن الأربعين النووية";
        return d;
    }

    private String getBukhariTitle(int s, String d) {
        switch(s) {
            case 1: return "كتاب بدء الوحي"; case 2: return "كتاب الإيمان"; case 3: return "كتاب العلم";
            case 4: return "كتاب الوضوء"; case 5: return "كتاب الغسل"; case 6: return "كتاب الحيض";
            case 7: return "كتاب التيمم"; case 8: return "كتاب الصلاة"; case 9: return "كتاب مواقيت الصلاة";
            case 10: return "كتاب الأذان"; case 11: return "كتاب الجمعة"; case 12: return "كتاب صلاة الخوف";
            case 13: return "كتاب العيدين"; case 14: return "كتاب الوتر"; case 15: return "كتاب الاستسقاء";
            case 16: return "كتاب الكسوف"; case 17: return "كتاب سجود القرآن"; case 18: return "كتاب تقصير الصلاة";
            case 19: return "كتاب التهجد"; case 20: return "كتاب فضل الصلاة في مسجد مكة والمدنية";
            case 21: return "كتاب العمل في الصلاة"; case 22: return "كتاب السهو"; case 23: return "كتاب الجنائز";
            case 24: return "كتاب الزكاة"; case 25: return "كتاب الحج"; case 26: return "كتاب العمرة";
            case 27: return "كتاب المحصر"; case 28: return "كتاب جزاء الصيد"; case 29: return "كتاب فضائل المدينة";
            case 30: return "كتاب الصوم"; case 31: return "كتاب صلاة التراويح"; case 32: return "كتاب فضل ليلة القدر";
            case 33: return "كتاب الاعتكاف"; case 34: return "كتاب البيوع"; case 35: return "كتاب السلم";
            case 36: return "كتاب الشفعة"; case 37: return "كتاب الإجارة"; case 38: return "كتاب الحوالة";
            case 39: return "كتاب الكفالة"; case 40: return "كتاب الوكالة"; case 41: return "كتاب المزارعة";
            case 42: return "كتاب المساقاة"; case 43: return "كتاب في الاستقراض"; case 44: return "كتاب الخصومات";
            case 45: return "كتاب في اللقطة"; case 46: return "كتاب المظالم"; case 47: return "كتاب الشركة";
            case 48: return "كتاب الرهن"; case 49: return "كتاب العتق"; case 50: return "كتاب المكاتب";
            case 51: return "كتاب الهبة"; case 52: return "كتاب الشهادات"; case 53: return "كتاب الصلح";
            case 54: return "كتاب الشروط"; case 55: return "كتاب الوصايا"; case 56: return "كتاب الجهاد والسير";
            case 57: return "كتاب فرض الخمس"; case 58: return "كتاب الجزية والموادعة"; case 59: return "كتاب بدء الخلق";
            case 60: return "كتاب أحاديث الأنبياء"; case 61: return "كتاب المناقب"; case 62: return "كتاب فضائل أصحاب النبي ﷺ";
            case 63: return "كتاب مناقب الأنصار"; case 64: return "كتاب المغازي"; case 65: return "كتاب التفسير";
            case 66: return "كتاب فضائل القرآن"; case 67: return "كتاب النكاح"; case 68: return "كتاب الطلاق";
            case 69: return "كتاب النفقات"; case 70: return "كتاب الأطعمة"; case 71: return "كتاب العقيقة";
            case 72: return "كتاب الذبائح والصيد"; case 73: return "كتاب الأضاحي"; case 74: return "كتاب الأشربة";
            case 75: return "كتاب المرضى"; case 76: return "كتاب الطب"; case 77: return "كتاب اللباس";
            case 78: return "كتاب الأدب"; case 79: return "كتاب الاستئذان"; case 80: return "كتاب الدعوات";
            case 81: return "كتاب الرقاق"; case 82: return "كتاب القدر"; case 83: return "كتاب الأيمان والنذور";
            case 84: return "كتاب كفارات الأيمان"; case 85: return "كتاب الفرائض"; case 86: return "كتاب الحدود";
            case 87: return "كتاب الديات"; case 88: return "كتاب استتابة المرتدين"; case 89: return "كتاب الإكراه";
            case 90: return "كتاب الحيل"; case 91: return "كتاب التعبير"; case 92: return "كتاب الفتن";
            case 93: return "كتاب الأحكام"; case 94: return "كتاب التمني"; case 95: return "كتاب أخبار الآحاد";
            case 96: return "كتاب الاعتصام بالكتاب والسنة"; case 97: return "كتاب التوحيد";
            default: return d;
        }
    }

    private String getMuslimTitle(int s, String d) {
        switch(s) {
            case 1: return "كتاب الإيمان"; case 2: return "كتاب الطهارة"; case 3: return "كتاب الحيض";
            case 4: return "كتاب الصلاة"; case 5: return "كتاب المساجد"; case 6: return "كتاب صلاة المسافرين";
            case 7: return "كتاب الجمعة"; case 8: return "كتاب صلاة العيدين"; case 9: return "كتاب الاستسقاء";
            case 10: return "كتاب الكسوف"; case 11: return "كتاب الجنائز"; case 12: return "كتاب الزكاة";
            case 13: return "كتاب الصيام"; case 14: return "كتاب الاعتكاف"; case 15: return "كتاب الحج";
            case 16: return "كتاب النكاح"; case 17: return "كتاب الرضاع"; case 18: return "كتاب الطلاق";
            case 19: return "كتاب اللعان"; case 20: return "كتاب العتق"; case 21: return "كتاب البيوع";
            case 22: return "كتاب المساقاة"; case 23: return "كتاب الفرائض"; case 24: return "كتاب الهبات";
            case 25: return "كتاب الوصية"; case 26: return "كتاب النذر"; case 27: return "كتاب الأيمان";
            case 28: return "كتاب القسامة"; case 29: return "كتاب الحدود"; case 30: return "كتاب الأقضية";
            case 31: return "كتاب اللقطة"; case 32: return "كتاب الجهاد"; case 33: return "كتاب الإمارة";
            case 34: return "كتاب الصيد"; case 35: return "كتاب الأضاحي"; case 36: return "كتاب الأشربة";
            case 37: return "كتاب اللباس"; case 38: return "كتاب الآداب"; case 39: return "كتاب السلام";
            case 40: return "كتاب الألفاظ"; case 41: return "كتاب الشعر"; case 42: return "كتاب الرؤيا";
            case 43: return "كتاب الفضائل"; case 44: return "كتاب فضائل الصحابة"; case 45: return "كتاب البر والصلة";
            case 46: return "كتاب القدر"; case 47: return "كتاب العلم"; case 48: return "كتاب الذكر والدعاء";
            case 49: return "كتاب الرقاق"; case 50: return "كتاب التوبة"; case 51: return "كتاب المنافقين";
            case 52: return "كتاب القيامة والجنة والنار"; case 53: return "كتاب الجنة ونعيمها";
            case 54: return "كتاب الفتن"; case 55: return "كتاب الزهد"; case 56: return "كتاب التفسير";
            default: return d;
        }
    }

    private String getTirmidhiTitle(int s, String d) {
        switch(s) {
            case 1: return "كتاب الطهارة"; case 2: return "كتاب الصلاة"; case 3: return "كتاب الزكاة";
            case 4: return "كتاب الصوم"; case 5: return "كتاب الحج"; case 6: return "كتاب الجنائز";
            case 7: return "كتاب النكاح"; case 8: return "كتاب الرضاع"; case 9: return "كتاب الطلاق واللعان";
            case 10: return "كتاب البيوع"; case 11: return "كتاب الأحكام"; case 12: return "كتاب الديات";
            case 13: return "كتاب الأضاحي"; case 14: return "كتاب الصيد"; case 15: return "كتاب الجهاد";
            case 16: return "كتاب فضائل الجهاد"; case 17: return "كتاب اللباس"; case 18: return "كتاب الأطعمة";
            case 19: return "كتاب الأشربة"; case 20: return "كتاب البر والصلة"; case 21: return "كتاب الطب";
            case 22: return "كتاب الفرائض"; case 23: return "كتاب الوصايا"; case 24: return "كتاب الولاء والهبة";
            case 25: return "كتاب القدر"; case 26: return "كتاب الفتن"; case 27: return "كتاب الرؤيا";
            case 28: return "كتاب الشهادات"; case 29: return "كتاب الزهد"; case 30: return "كتاب صفة القيامة";
            case 31: return "كتاب صفة الجنة"; case 32: return "كتاب صفة جهنم"; case 33: return "كتاب الإيمان";
            case 34: return "كتاب العلم"; case 35: return "كتاب الاستئذان"; case 36: return "كتاب الأدب";
            case 37: return "كتاب ثواب القرآن"; case 38: return "كتاب القراءات"; case 39: return "كتاب التفسير";
            case 40: return "كتاب الدعوات"; case 41: return "كتاب المناقب"; case 42: return "كتاب الفتن";
            case 43: return "كتاب الأدب"; case 44: return "كتاب المناقب";
            default: return d;
        }
    }

    private String getAbudawudTitle(int s, String d) {
        switch(s) {
            case 1: return "كتاب الطهارة"; case 2: return "كتاب الصلاة"; case 3: return "كتاب صلاة الاستسقاء";
            case 4: return "كتاب صلاة السفر"; case 5: return "كتاب صلاة التطوع"; case 6: return "كتاب شهر رمضان";
            case 7: return "كتاب سجود القرآن"; case 8: return "كتاب الوتر"; case 9: return "كتاب الزكاة";
            case 10: return "كتاب اللقطة"; case 11: return "كتاب المناسك"; case 12: return "كتاب النكاح";
            case 13: return "كتاب الطلاق"; case 14: return "كتاب الصوم"; case 15: return "كتاب الجنائز";
            case 16: return "كتاب الجهاد"; case 17: return "كتاب الضحايا"; case 18: return "كتاب الصيد";
            case 19: return "كتاب الوصايا"; case 20: return "كتاب الفرائض"; case 21: return "كتاب البيوع";
            case 22: return "كتاب الإجارة"; case 23: return "كتاب الأقضية"; case 24: return "كتاب العلم";
            case 25: return "كتاب الأشربة"; case 26: return "كتاب الأطعمة"; case 27: return "كتاب الطب";
            case 28: return "كتاب الكهانة"; case 29: return "كتاب العتق"; case 30: return "كتاب القراءات";
            case 31: return "كتاب الحمام"; case 32: return "كتاب اللباس"; case 33: return "كتاب الترجل";
            case 34: return "كتاب الخاتم"; case 35: return "كتاب الفتن"; case 36: return "كتاب المهدي";
            case 37: return "كتاب الملاحم"; case 38: return "كتاب الحدود"; case 39: return "كتاب الديات";
            case 40: return "كتاب السنة"; case 41: return "كتاب الأدب";
            default: return d;
        }
    }

    private String getRiyadhTitle(int s, String d) {
        switch(s) {
            case 1: return "كتاب المقدمات"; case 2: return "كتاب الأدب"; case 3: return "كتاب أدب الطعام";
            case 4: return "كتاب اللباس"; case 5: return "كتاب أدب النوم"; case 6: return "كتاب السلام";
            case 7: return "كتاب عيادة المريض"; case 8: return "كتاب أدب السفر"; case 9: return "كتاب الفضائل";
            case 10: return "كتاب الاعتكاف"; case 11: return "كتاب الحج"; case 12: return "كتاب الجهاد";
            case 13: return "كتاب العلم"; case 14: return "كتاب حمد الله"; case 15: return "كتاب الصلاة على النبي";
            case 16: return "كتاب الأذكار"; case 17: return "كتاب الدعوات"; case 18: return "كتاب الأمور المنهي عنها";
            case 19: return "كتاب المنثورات والملح"; case 20: return "كتاب الاستغفار";
            default: return d;
        }
    }
}
