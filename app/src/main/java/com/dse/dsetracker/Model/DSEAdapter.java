package com.dse.dsetracker.Model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.dse.dsetracker.R;

import java.util.List;

public class DSEAdapter extends RecyclerView.Adapter<DSEAdapter.ViewHolder> {

    private Context context;
    private List<DSEModel> dseList;

    public DSEAdapter(Context context, List<DSEModel> dseList) {
        this.context = context;
        this.dseList = dseList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.dse_item, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DSEModel model = dseList.get(position);

        holder.tvRank.setText(String.valueOf(position + 1));
        holder.tvCompanyName.setText(model.getCompanyName());
        holder.tvSharePrice.setText("৳ " + String.format("%.2f", model.getSharePrice()));

        holder.tvChangeValue.setText(String.format("%.2f", model.getChange()));
        holder.tvChangePercent.setText(model.getChangeRate());

        // Color logic for gain/loss
        if (model.getChange() >= 0) {
            holder.tvSharePrice.setTextColor(ContextCompat.getColor(context, R.color.green));
            holder.tvChangeValue.setTextColor(ContextCompat.getColor(context, R.color.green));
            holder.tvChangePercent.setTextColor(ContextCompat.getColor(context, R.color.green));
        } else {
            holder.tvSharePrice.setTextColor(ContextCompat.getColor(context, R.color.red));
            holder.tvChangeValue.setTextColor(ContextCompat.getColor(context, R.color.red));
            holder.tvChangePercent.setTextColor(ContextCompat.getColor(context, R.color.red));
        }
    }

    @Override
    public int getItemCount() {
        return dseList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvCompanyName, tvSharePrice, tvChangeValue, tvChangePercent;
        ImageView ivStar, ivBell;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvCompanyName = itemView.findViewById(R.id.tvCompanyName);
            tvSharePrice = itemView.findViewById(R.id.tvSharePrice);
            tvChangeValue = itemView.findViewById(R.id.tvChangeValue);
            tvChangePercent = itemView.findViewById(R.id.tvChangePercent);
            ivStar = itemView.findViewById(R.id.ivStar);
            ivBell = itemView.findViewById(R.id.ivBell);
        }
    }
}

