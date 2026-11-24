package com.bahnabwi.alqurankarim;

import android.util.Base64;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * محرك المكتبة الإسلامية الشاملة - نسخة الإصلاح النهائي للمزامنة الحقيقية
 */
public class HadithManager {
    private final MainActivity activity;
    private final HadithRepository repository;
    private final ExecutorService executor;

    public HadithManager(MainActivity activity, HadithRepository repository, ExecutorService executor) {
        this.activity = activity;
        this.repository = repository;
        this.executor = executor;
    }

    public void getHadiths(String type, String callback) {
        executor.execute(() -> {
            try {
                JSONArray result = new JSONArray();
                String bookId = determineBookId(type);

                // إذا كان الطلب فهرس الكتاب (ينتهي بـ _index أو هو اسم الكتاب مباشرة)
                if (type.endsWith("_index") || type.equals("jamia_tirmidhi") || type.equals("riyadh_salihin") || type.equals("sahih_muslim")) {
                    BookEntity book = repository.getBookStatus(bookId);
                    List<SectionEntity> kitabs = repository.getSections(bookId);
                    
                    if (book != null && book.isDownloading) {
                        JSONObject obj = new JSONObject();
                        obj.put("id", -1);
                        obj.put("title", "جاري المزامنة...");
                        obj.put("subtitle", "يتم حالياً استخراج أحاديث " + getBookFriendlyName(bookId) + ".. فضلاً انتظر ثوانٍ.");
                        result.put(obj);
                    } else if (kitabs.isEmpty()) {
                        JSONObject obj = new JSONObject();
                        obj.put("id", -1);
                        obj.put("title", "المحتوى غير متوفر");
                        obj.put("subtitle", "يرجى تحميل الكتاب من المكتبة أولاً.");
                        result.put(obj);
                    } else {
                        for (SectionEntity kitab : kitabs) {
                            JSONObject obj = new JSONObject();
                            obj.put("id", kitab.sectionNumber);
                            obj.put("title", kitab.sectionTitle);
                            obj.put("subtitle", "انقر لعرض أحاديث الكتاب");
                            // توحيد الحقول لشاشات الويب التي تتوقع text
                            obj.put("text", kitab.sectionTitle);
                            result.put(obj);
                        }
                    }
                } 
                else if (type.contains("_chapters_") || type.startsWith("hadiths_")) {
                    int sectionId = extractSectionId(type);
                    if (sectionId < 0) return;

                    List<HadithEntity> hadiths = repository.getHadiths(bookId, sectionId);
                    for (HadithEntity h : hadiths) {
                        JSONObject obj = new JSONObject();
                        obj.put("title", "حديث رقم " + h.arabicNumber);
                        obj.put("matn", h.textArabic);
                        obj.put("text", h.textArabic); // توحيد الحقل ليعمل مع كافة الشاشات
                        obj.put("grade", getBookFriendlyName(bookId));
                        result.put(obj);
                    }
                }

                final String b64 = Base64.encodeToString(result.toString().getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                activity.runOnUiThread(() -> activity.runJS("if(window." + callback + ") " + callback + "('" + b64 + "')"));
            } catch (Exception e) {
                Log.e("HadithManager", "Error in getHadiths", e);
            }
        });
    }

    public void searchHadiths(String query, String callback) {
        executor.execute(() -> {
            try {
                JSONArray result = new JSONArray();
                String[] allBookIds = {"ara-bukhari", "ara-muslim", "ara-abudawud", "ara-tirmidhi", "ara-riyadussalihin", "ara-nawawi"};
                
                for (String bookId : allBookIds) {
                    BookEntity status = repository.getBookStatus(bookId);
                    if (status != null && status.isDownloaded) {
                        List<HadithEntity> res = repository.searchHadiths(bookId, query);
                        for (HadithEntity h : res) {
                            JSONObject obj = new JSONObject();
                            obj.put("title", getBookFriendlyName(bookId) + " - " + h.arabicNumber);
                            obj.put("matn", h.textArabic);
                            obj.put("text", h.textArabic);
                            obj.put("grade", "نتيجة بحث");
                            result.put(obj);
                        }
                    }
                }
                final String b64 = Base64.encodeToString(result.toString().getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                activity.runOnUiThread(() -> activity.runJS("if(window." + callback + ") " + callback + "('" + b64 + "')"));
            } catch (Exception e) {
                Log.e("HadithManager", "Search Error", e);
            }
        });
    }

    private String determineBookId(String type) {
        if (type.contains("muslim")) return "ara-muslim";
        if (type.contains("abudawud") || type.contains("abi_dawood")) return "ara-abudawud";
        if (type.contains("tirmidhi")) return "ara-tirmidhi";
        if (type.contains("riyadh") || type.contains("salihin")) return "ara-riyadussalihin";
        if (type.contains("nawawi") || type.contains("forty")) return "ara-nawawi";
        return "ara-bukhari";
    }

    private int extractSectionId(String type) {
        try {
            String clean = type.replaceAll("[^0-9]", "");
            return clean.isEmpty() ? 0 : Integer.parseInt(clean);
        } catch (Exception e) { return -1; }
    }

    private String getBookFriendlyName(String bookId) {
        switch (bookId) {
            case "ara-bukhari": return "صحيح البخاري";
            case "ara-muslim": return "صحيح مسلم";
            case "ara-abudawud": return "سنن أبي داود";
            case "ara-tirmidhi": return "جامع الترمذي";
            case "ara-riyadussalihin": return "رياض الصالحين";
            case "ara-nawawi": return "الأربعون النووية";
            default: return "مكتبة الحديث";
        }
    }
}
