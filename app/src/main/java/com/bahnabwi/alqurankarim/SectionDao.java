package com.bahnabwi.alqurankarim;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

/**
 * تعليق جراحي: DAO لإدارة الأبواب - تم إضافة insertSection لدعم عملية المزامنة التدفقية (Streaming).
 */
@Dao
public interface SectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSections(List<SectionEntity> sections);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSection(SectionEntity section);

    @Query("SELECT * FROM sections WHERE bookId = :bookId ORDER BY sectionNumber ASC")
    List<SectionEntity> getSectionsByBook(String bookId);
}
