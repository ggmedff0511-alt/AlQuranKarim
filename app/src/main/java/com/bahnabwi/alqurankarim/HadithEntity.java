package com.bahnabwi.alqurankarim;

import androidx.annotation.NonNull;
import androidx.room.Entity;

/**
 * كيان يمثل الحديث الشريف.
 * تعليق جراحي: تم تغيير المفتاح الأساسي ليكون مركب (bookId + hadithNumber) 
 * لضمان عدم تكرار الأحاديث عند إعادة التحميل وللسماح بتحديث النصوص بشكل سليم.
 */
@Entity(tableName = "hadith_entries", primaryKeys = {"bookId", "hadithNumber"})
public class HadithEntity {
    @NonNull
    public String bookId;
    public int hadithNumber;
    public String arabicNumber;
    public String textArabic; // النص بالتشكيل
    public String textPlain;  // النص بدون تشكيل للبحث
    public int sectionNumber;

    public HadithEntity(@NonNull String bookId, int hadithNumber, String arabicNumber, String textArabic, String textPlain, int sectionNumber) {
        this.bookId = bookId;
        this.hadithNumber = hadithNumber;
        this.arabicNumber = arabicNumber;
        this.textArabic = textArabic;
        this.textPlain = textPlain;
        this.sectionNumber = sectionNumber;
    }

    public static String removeDiacritics(String text) {
        if (text == null) return null;
        return text.replaceAll("[\u064B-\u0652\u0640]", "");
    }
}
