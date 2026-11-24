package com.bahnabwi.alqurankarim;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

/**
 * تعليق: محول RecyclerView لعرض أبواب الكتاب - نسخة الاستقرار العالية
 */
public class SectionAdapter extends ListAdapter<SectionEntity, SectionAdapter.SectionViewHolder> {

    private final OnSectionClickListener listener;

    public interface OnSectionClickListener {
        void onSectionClick(SectionEntity section);
    }

    public SectionAdapter(OnSectionClickListener listener) {
        super(new DiffUtil.ItemCallback<SectionEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull SectionEntity oldItem, @NonNull SectionEntity newItem) {
                return oldItem.sectionNumber == newItem.sectionNumber && 
                       safeEquals(oldItem.bookId, newItem.bookId);
            }

            @Override
            public boolean areContentsTheSame(@NonNull SectionEntity oldItem, @NonNull SectionEntity newItem) {
                return safeEquals(oldItem.sectionTitle, newItem.sectionTitle);
            }

            private boolean safeEquals(String s1, String s2) {
                if (s1 == null && s2 == null) return true;
                if (s1 == null || s2 == null) return false;
                return s1.equals(s2);
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public SectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_section, parent, false);
        return new SectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SectionViewHolder holder, int position) {
        SectionEntity section = getItem(position);
        if (section != null) {
            holder.bind(section, listener);
        }
    }

    static class SectionViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumber, tvTitle;

        public SectionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumber = itemView.findViewById(R.id.tvSectionNumber);
            tvTitle = itemView.findViewById(R.id.tvSectionTitle);
        }

        public void bind(SectionEntity section, OnSectionClickListener listener) {
            if (tvNumber != null) tvNumber.setText(String.valueOf(section.sectionNumber));
            if (tvTitle != null) tvTitle.setText(section.sectionTitle != null ? section.sectionTitle : "بدون عنوان");
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onSectionClick(section);
            });
        }
    }
}
