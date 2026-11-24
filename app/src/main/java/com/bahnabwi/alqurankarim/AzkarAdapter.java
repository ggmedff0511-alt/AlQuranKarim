package com.bahnabwi.alqurankarim;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AzkarAdapter extends RecyclerView.Adapter<AzkarAdapter.ViewHolder> {

    private final List<AzkarData.Zekr> azkarList;

    public AzkarAdapter(List<AzkarData.Zekr> azkarList) {
        this.azkarList = azkarList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_zekr, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AzkarData.Zekr zekr = azkarList.get(position);
        holder.tvText.setText(zekr.text);
        holder.tvCount.setText(zekr.count);
        holder.tvDesc.setText(zekr.description);
        
        if (zekr.description == null || zekr.description.isEmpty()) {
            holder.tvDesc.setVisibility(View.GONE);
        } else {
            holder.tvDesc.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return azkarList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvText, tvCount, tvDesc;

        ViewHolder(View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tvZekrText);
            tvCount = itemView.findViewById(R.id.tvZekrCount);
            tvDesc = itemView.findViewById(R.id.tvZekrDesc);
        }
    }
}
