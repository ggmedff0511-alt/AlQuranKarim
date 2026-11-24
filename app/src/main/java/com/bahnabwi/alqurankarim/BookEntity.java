package com.bahnabwi.alqurankarim;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * تعليق: هذا الكيان يمثل الكتاب وحالة تحميله
 * من برمت روابط الكتب المهمة: تطوير مكتبة الحديث الشاملة
 */
@Entity(tableName = "books")
public class BookEntity {
    @PrimaryKey(autoGenerate = false)
    @NonNull
    public String bookId; // مثل "ara-bukhari"
    public String arabicTitle;
    public String downloadUrl;
    public boolean isDownloaded;
    public int hadithCount;
    public String approxSize;
    public boolean isDownloading; // حالة جاري التحميل في الواجهة

    public BookEntity(@NonNull String bookId, String arabicTitle, String downloadUrl, String approxSize) {
        this.bookId = bookId;
        this.arabicTitle = arabicTitle;
        this.downloadUrl = downloadUrl;
        this.approxSize = approxSize;
        this.isDownloaded = false;
        this.isDownloading = false;
    }
}
