package com.bahnabwi.alqurankarim;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;

/**
 * تعليق: النشاط الرئيسي للمكتبة الإسلامية - يضمن ظهور الكتب فوراً
 * من برمت روابط الكتب المهمة: تطوير مكتبة الحديث الشاملة
 */
public class HadithLibraryActivity extends AppCompatActivity {

    private HadithViewModel viewModel;
    private BookAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hadith_library);

        // إعداد التولبار
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // إعداد القائمة (RecyclerView)
        RecyclerView rvBooks = findViewById(R.id.rvBooks);
        rvBooks.setLayoutManager(new LinearLayoutManager(this));
        rvBooks.setHasFixedSize(true); // لتحسين الأداء وضمان الظهور السريع

        adapter = new BookAdapter(new BookAdapter.OnBookClickListener() {
            @Override
            public void onDownloadClick(BookEntity book) {
                viewModel.downloadBook(book);
                Toast.makeText(HadithLibraryActivity.this, "جاري بدء تحميل " + book.arabicTitle, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteClick(BookEntity book) {
                new AlertDialog.Builder(HadithLibraryActivity.this)
                        .setTitle("حذف البيانات")
                        .setMessage("هل تريد حذف ملفات " + book.arabicTitle + "؟")
                        .setPositiveButton("حذف", (dialog, which) -> viewModel.deleteBook(book))
                        .setNegativeButton("إلغاء", null)
                        .show();
            }

            @Override
            public void onOpenBook(BookEntity book) {
                Intent intent = new Intent(HadithLibraryActivity.this, HadithSectionsActivity.class);
                intent.putExtra("book_id", book.bookId);
                intent.putExtra("book_title", book.arabicTitle);
                startActivity(intent);
            }
        });

        rvBooks.setAdapter(adapter);

        // ربط البيانات
        viewModel = new ViewModelProvider(this).get(HadithViewModel.class);
        
        // مراقبة الكتب - ستظهر الكتب العشرة هنا فور إضافتها لقاعدة البيانات
        viewModel.getAllBooks().observe(this, books -> {
            if (books != null && !books.isEmpty()) {
                adapter.submitList(new ArrayList<>(books));
            }
        });
    }
}
