package com.bahnabwi.alqurankarim;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

/**
 * تعليق: محول RecyclerView لعرض الأحاديث مع ميزات النسخ والمشاركة
 * من برمت روابط الكتب المهمة: تطوير مكتبة الحديث الشاملة
 */
public class HadithAdapter extends ListAdapter<HadithEntity, HadithAdapter.HadithViewHolder> {

    public HadithAdapter() {
        super(new DiffUtil.ItemCallback<HadithEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull HadithEntity oldItem, @NonNull HadithEntity newItem) {
                return oldItem.hadithNumber == newItem.hadithNumber && oldItem.bookId.equals(newItem.bookId);
            }

            @Override
            public boolean areContentsTheSame(@NonNull HadithEntity oldItem, @NonNull HadithEntity newItem) {
                return oldItem.textArabic.equals(newItem.textArabic);
            }
        });
    }

    @NonNull
    @Override
    public HadithViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hadith, parent, false);
        return new HadithViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HadithViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class HadithViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumber, tvText;
        ImageButton btnShare, btnCopy;

        public HadithViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumber = itemView.findViewById(R.id.tvHadithNumber);
            tvText = itemView.findViewById(R.id.tvHadithText);
            btnShare = itemView.findViewById(R.id.btnShare);
            btnCopy = itemView.findViewById(R.id.btnCopy);
        }

        public void bind(HadithEntity hadith) {
            tvNumber.setText("حديث رقم: " + hadith.arabicNumber);
            tvText.setText(hadith.textArabic);

            btnCopy.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("hadith", hadith.textArabic);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(v.getContext(), "تم نسخ الحديث", Toast.LENGTH_SHORT).show();
            });

            btnShare.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, hadith.textArabic);
                v.getContext().startActivity(Intent.createChooser(intent, "مشاركة الحديث عبر:"));
            });
        }
    }
}
