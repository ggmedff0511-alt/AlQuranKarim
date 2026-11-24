package com.bahnabwi.alqurankarim;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;

/**
 * تعليق: نشاط عرض الأحاديث مع ميزة البحث الذكي
 * من برمت روابط الكتب المهمة: تطوير مكتبة الحديث الشاملة
 */
public class HadithDisplayActivity extends AppCompatActivity {

    private HadithViewModel viewModel;
    private HadithAdapter adapter;
    private String bookId;
    private int sectionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hadith_display);

        bookId = getIntent().getStringExtra("book_id");
        sectionId = getIntent().getIntExtra("section_id", 0);
        String sectionTitle = getIntent().getStringExtra("section_title");

        MaterialToolbar toolbar = findViewById(R.id.toolbarHadiths);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(sectionTitle);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rvHadiths = findViewById(R.id.rvHadiths);
        rvHadiths.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HadithAdapter();
        rvHadiths.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(HadithViewModel.class);

        // تحميل الأحاديث التابعة لهذا الباب
        viewModel.getHadiths(bookId, sectionId, hadiths -> {
            runOnUiThread(() -> adapter.submitList(hadiths));
        });

        // إعداد البحث
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    viewModel.getHadiths(bookId, sectionId, hadiths -> {
                        runOnUiThread(() -> adapter.submitList(hadiths));
                    });
                } else {
                    performSearch(newText);
                }
                return true;
            }
        });
    }

    private void performSearch(String query) {
        // تعليق جراحي: تنظيف نص البحث من التشكيل لضمان مطابقة النتائج مع النص المخزن في قاعدة البيانات (textPlain)
        String cleanQuery = HadithEntity.removeDiacritics(query);
        viewModel.search(bookId, cleanQuery, results -> {
            runOnUiThread(() -> adapter.submitList(results));
        });
    }
}
