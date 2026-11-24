package com.bahnabwi.alqurankarim;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * تحديث قاعدة البيانات - نسخة الإصلاح الشامل للمزامنة
 * تعليق جراحي: تم رفع الإصدار إلى 3 لضمان تفعيل المفاتيح الأساسية المركبة (Composite Keys) 
 * ومسح البيانات القديمة التي كانت تسبب تكرار الأبواب وظهور رسائل "لم يتم العثور على أحاديث".
 */
@Database(entities = {Hadith.class, BookEntity.class, SectionEntity.class, HadithEntity.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public abstract HadithDao hadithDao();
    public abstract BookDao bookDao();
    public abstract SectionDao sectionDao();
    public abstract HadithEntryDao hadithEntryDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "quran_karim_db")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
