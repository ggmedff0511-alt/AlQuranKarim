package com.bahnabwi.alqurankarim;

import androidx.annotation.NonNull;
import androidx.room.Entity;

/**
 * كيان يمثل الأبواب (Sections) داخل الكتاب.
 * تعليق جراحي: تم تغيير المفتاح الأساسي ليكون مركب (bookId + sectionNumber) 
 * لضمان عدم تكرار الأبواب عند إعادة التحميل وللسماح بتحديث البيانات بشكل سليم.
 */
@Entity(tableName = "sections", primaryKeys = {"bookId", "sectionNumber"})
public class SectionEntity {
    @NonNull
    public String bookId;
    public int sectionNumber;
    public String sectionTitle;

    public SectionEntity(@NonNull String bookId, int sectionNumber, String sectionTitle) {
        this.bookId = bookId;
        this.sectionNumber = sectionNumber;
        this.sectionTitle = sectionTitle;
    }
}
