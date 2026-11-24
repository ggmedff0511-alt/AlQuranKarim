package com.bahnabwi.alqurankarim;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

/**
 * تعليق: DAO لإدارة الأحاديث مع ميزة البحث في النص المبسط
 * من برمت روابط الكتب المهمة: تطوير مكتبة الحديث الشاملة
 */
@Dao
public interface HadithEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertHadiths(List<HadithEntity> hadiths);

    @Query("SELECT * FROM hadith_entries WHERE bookId = :bookId AND sectionNumber = :sectionId ORDER BY hadithNumber ASC")
    List<HadithEntity> getHadithsBySection(String bookId, int sectionId);

    @Query("SELECT * FROM hadith_entries WHERE bookId = :bookId AND textPlain LIKE '%' || :query || '%'")
    List<HadithEntity> searchInBook(String bookId, String query);
}
