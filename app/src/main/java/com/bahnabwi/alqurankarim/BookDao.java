package com.bahnabwi.alqurankarim;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

/**
 * تعليق جراحي: تم تغيير نوع القيمة العائدة لإجمالي الأحاديث ليكون Integer 
 * لضمان عدم انهيار التطبيق (Crash) في حال كانت قاعدة البيانات فارغة.
 */
@Dao
public interface BookDao {
    @Query("SELECT * FROM books")
    LiveData<List<BookEntity>> getAllBooks();

    @Query("SELECT * FROM books")
    List<BookEntity> getAllBooksSync();

    @Query("SELECT * FROM books WHERE bookId = :bookId LIMIT 1")
    BookEntity getBookById(String bookId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBooks(List<BookEntity> books);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBook(BookEntity book);

    @Update
    void updateBook(BookEntity book);

    @Query("UPDATE books SET isDownloaded = :isDownloaded, isDownloading = :isDownloading WHERE bookId = :bookId")
    void updateDownloadStatus(String bookId, boolean isDownloaded, boolean isDownloading);

    @Query("UPDATE books SET hadithCount = :count WHERE bookId = :bookId")
    void updateHadithCount(String bookId, int count);

    @Query("DELETE FROM hadith_entries WHERE bookId = :bookId")
    void deleteHadithsByBook(String bookId);

    @Query("DELETE FROM sections WHERE bookId = :bookId")
    void deleteSectionsByBook(String bookId);

    @Query("SELECT SUM(hadithCount) FROM books WHERE isDownloaded = 1")
    Integer getTotalDownloadedHadiths(); // تم التغيير من int إلى Integer لمنع الـ Crash
}
