package com.bahnabwi.alqurankarim;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

/**
 * تعليق: محول RecyclerView لعرض الكتب وحالات التحميل الثلاث
 * من برمت روابط الكتب المهمة: تطوير مكتبة الحديث الشاملة
 */
public class BookAdapter extends ListAdapter<BookEntity, BookAdapter.BookViewHolder> {

    private final OnBookClickListener listener;

    public interface OnBookClickListener {
        void onDownloadClick(BookEntity book);
        void onDeleteClick(BookEntity book);
        void onOpenBook(BookEntity book);
    }

    public BookAdapter(OnBookClickListener listener) {
        super(new DiffUtil.ItemCallback<BookEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull BookEntity oldItem, @NonNull BookEntity newItem) {
                return oldItem.bookId.equals(newItem.bookId);
            }

            @Override
            public boolean areContentsTheSame(@NonNull BookEntity oldItem, @NonNull BookEntity newItem) {
                return oldItem.isDownloaded == newItem.isDownloaded &&
                        oldItem.isDownloading == newItem.isDownloading;
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_book, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSize;
        ImageButton btnDownload, btnOptions;
        ProgressBar pbDownloading;
        ImageView ivDownloaded;

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvBookTitle);
            tvSize = itemView.findViewById(R.id.tvBookSize);
            btnDownload = itemView.findViewById(R.id.btnDownload);
            btnOptions = itemView.findViewById(R.id.btnOptions);
            pbDownloading = itemView.findViewById(R.id.pbDownloading);
            ivDownloaded = itemView.findViewById(R.id.ivDownloaded);
        }

        public void bind(BookEntity book, OnBookClickListener listener) {
            tvTitle.setText(book.arabicTitle);
            
            // استخدام String Resource المحدث لعرض الحجم
            String sizeText = itemView.getContext().getString(R.string.book_size_label, book.approxSize);
            tvSize.setText(sizeText);

            if (book.isDownloading) {
                btnDownload.setVisibility(View.GONE);
                pbDownloading.setVisibility(View.VISIBLE);
                ivDownloaded.setVisibility(View.GONE);
                btnOptions.setVisibility(View.GONE);
            } else if (book.isDownloaded) {
                btnDownload.setVisibility(View.GONE);
                pbDownloading.setVisibility(View.GONE);
                ivDownloaded.setVisibility(View.VISIBLE);
                btnOptions.setVisibility(View.VISIBLE);
            } else {
                btnDownload.setVisibility(View.VISIBLE);
                pbDownloading.setVisibility(View.GONE);
                ivDownloaded.setVisibility(View.GONE);
                btnOptions.setVisibility(View.GONE);
            }

            btnDownload.setOnClickListener(v -> listener.onDownloadClick(book));
            btnOptions.setOnClickListener(v -> listener.onDeleteClick(book));
            itemView.setOnClickListener(v -> {
                if (book.isDownloaded) {
                    listener.onOpenBook(book);
                } else if (!book.isDownloading) {
                    listener.onDownloadClick(book);
                }
            });
        }
    }
}
