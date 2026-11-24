package com.bahnabwi.alqurankarim;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface HadithDao {
    @Query("SELECT * FROM hadiths")
    List<Hadith> getAll();

    @Insert
    void insertAll(Hadith... hadiths);

    /**
     * تعليق جراحي: وظيفة البحث في محتوى الأحاديث وعناوين الأبواب
     * تستخدم للبحث النصي الجزئي في قاعدة بيانات Room
     */
    @Query("SELECT * FROM hadiths WHERE content LIKE '%' || :query || '%' OR chapter LIKE '%' || :query || '%'")
    List<Hadith> search(String query);
}
