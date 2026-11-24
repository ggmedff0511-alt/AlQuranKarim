package com.bahnabwi.alqurankarim;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * تعليق: نشاط عرض أبواب الكتاب - نسخة الاستقرار الفائق
 * تم حل مشكلة الانهيار (Crash) عبر حماية الخيوط البرمجية والتحقق من البيانات.
 */
public class HadithSectionsActivity extends AppCompatActivity {

    private String bookId;
    private String bookTitle;
    private static final String TAG = "HadithSections";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_hadith_sections);

            // جلب البيانات مع فحص الأمان لتجنب NPE
            if (getIntent() != null) {
                bookId = getIntent().getStringExtra("book_id");
                bookTitle = getIntent().getStringExtra("book_title");
            }

            if (bookId == null || bookId.isEmpty()) {
                Toast.makeText(this, "عذراً، معرف الكتاب غير صحيح", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // إعداد شريط العنوان بتصميم متوافق
            MaterialToolbar toolbar = findViewById(R.id.toolbarSections);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(bookTitle != null ? bookTitle : "الأبواب");
                }
                toolbar.setNavigationOnClickListener(v -> finish());
            }

            // إعداد القائمة RecyclerView
            RecyclerView rvSections = findViewById(R.id.rvSections);
            if (rvSections != null) {
                rvSections.setLayoutManager(new LinearLayoutManager(this));

                SectionAdapter adapter = new SectionAdapter(section -> {
                    if (section != null) {
                        Intent intent = new Intent(HadithSectionsActivity.this, HadithDisplayActivity.class);
                        intent.putExtra("book_id", bookId);
                        intent.putExtra("section_id", section.sectionNumber);
                        intent.putExtra("section_title", section.sectionTitle);
                        startActivity(intent);
                    }
                });

                rvSections.setAdapter(adapter);

                // جلب البيانات من ViewModel بشكل آمن
                HadithViewModel viewModel = new ViewModelProvider(this).get(HadithViewModel.class);
                viewModel.getSections(bookId, sections -> {
                    runOnUiThread(() -> {
                        if (!isFinishing() && sections != null) {
                            adapter.submitList(sections);
                        }
                    });
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Critical Error prevented", e);
            Toast.makeText(this, "حدث خطأ غير متوقع، يرجى المحاولة مرة أخرى", Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
