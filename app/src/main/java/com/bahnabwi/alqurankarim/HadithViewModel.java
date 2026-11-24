package com.bahnabwi.alqurankarim;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * تعليق: ViewModel لربط واجهة المستخدم بمستودع بيانات الأحاديث
 * من برمت روابط الكتب المهمة: تطوير مكتبة الحديث الشاملة
 */
public class HadithViewModel extends AndroidViewModel {
    private final HadithRepository repository;
    private final LiveData<List<BookEntity>> allBooks;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public HadithViewModel(@NonNull Application application) {
        super(application);
        repository = new HadithRepository(application);
        repository.initializeDefaultBooks();
        allBooks = repository.getAllBooks();
    }

    public LiveData<List<BookEntity>> getAllBooks() {
        return allBooks;
    }

    public void downloadBook(BookEntity book) {
        repository.downloadBook(book);
    }

    public void deleteBook(BookEntity book) {
        // تعليق جراحي: نقل عملية الحذف إلى خيط خلفي لمنع توقف التطبيق (Crash) وضمان سلاسة استجابة الواجهة عند حذف بيانات الكتب
        executor.execute(() -> repository.deleteBookData(book.bookId));
    }

    // جلب الأبواب
    public void getSections(String bookId, OnDataLoadedListener<List<SectionEntity>> callback) {
        executor.execute(() -> {
            List<SectionEntity> sections = repository.getSections(bookId);
            callback.onLoaded(sections);
        });
    }

    // جلب الأحاديث لباب معين
    public void getHadiths(String bookId, int sectionId, OnDataLoadedListener<List<HadithEntity>> callback) {
        executor.execute(() -> {
            List<HadithEntity> hadiths = repository.getHadiths(bookId, sectionId);
            callback.onLoaded(hadiths);
        });
    }

    // البحث في الكتاب
    public void search(String bookId, String query, OnDataLoadedListener<List<HadithEntity>> callback) {
        executor.execute(() -> {
            List<HadithEntity> results = repository.searchHadiths(bookId, query);
            callback.onLoaded(results);
        });
    }

    public interface OnDataLoadedListener<T> {
        void onLoaded(T data);
    }
}
