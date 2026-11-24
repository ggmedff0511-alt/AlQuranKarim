package com.bahnabwi.alqurankarim;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * يمثل المجموعة الرئيسية (مثل: صحيح البخاري، صحيح مسلم)
 * تعليق جراحي: تم تصميم الجدول لدعم الكتب الستة بشكل منفصل.
 */
@Entity(tableName = "hadith_collections")
public class HadithCollection {
    @PrimaryKey
    public int id;
    public String nameAr;     // الاسم بالعربي (صحيح البخاري)
    public String nameEn;     // الاسم بالإنجليزي (اختياري)
    public int totalHadiths;  // عدد الأحاديث الكلي للعرض في البطاقة
    public String author;      // اسم المؤلف (الإمام البخاري)
}
