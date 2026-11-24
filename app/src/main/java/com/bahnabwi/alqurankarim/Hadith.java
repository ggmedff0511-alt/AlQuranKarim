package com.bahnabwi.alqurankarim;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "hadiths")
public class Hadith {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String bookId;
    public String content;
    public String chapter;
}
