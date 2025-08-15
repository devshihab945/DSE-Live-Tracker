package com.dse.dsetracker.Model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.dse.dsetracker.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DSEAdapter extends RecyclerView.Adapter<DSEAdapter.ViewHolder> {

    private final Context context;
    private final List<DSEModel> dseList;
    private final SharedPreferences prefs;

    public DSEAdapter(Context context, List<DSEModel> dseList) {
        this.context = context;
        this.dseList = dseList;
        this.prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE);
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

        // Gain/Loss color
        int color = model.getChange() >= 0 ? R.color.green : R.color.red;
        holder.tvSharePrice.setTextColor(ContextCompat.getColor(context, color));
        holder.tvChangeValue.setTextColor(ContextCompat.getColor(context, color));
        holder.tvChangePercent.setTextColor(ContextCompat.getColor(context, color));

        // Favorites check (highlight on load)
        boolean isFavorite = getFavorites().contains(model.getCompanyName());
        updateFavoriteIcon(holder.ivStar, isFavorite);

        // Star click listener
        holder.ivStar.setOnClickListener(v -> {
            String companyName = model.getCompanyName();
            Set<String> favorites = new HashSet<>(getFavorites()); // Copy to avoid mutation issues

            if (favorites.contains(companyName)) {
                favorites.remove(companyName);
                Toast.makeText(context, companyName + " removed from favorites", Toast.LENGTH_SHORT).show();
                updateFavoriteIcon(holder.ivStar, false);
            } else {
                favorites.add(companyName);
                Toast.makeText(context, companyName + " added to favorites", Toast.LENGTH_SHORT).show();
                updateFavoriteIcon(holder.ivStar, true);
            }

            prefs.edit().putStringSet("favorite_stocks", favorites).apply();
        });

        // Bell click
        holder.ivBell.setOnClickListener(v -> Toast.makeText(context, "Notification feature coming soon!", Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return dseList.size();
    }

    private Set<String> getFavorites() {
        return prefs.getStringSet("favorite_stocks", new HashSet<>());
    }

    private void updateFavoriteIcon(ImageView ivStar, boolean isFavorite) {
        ivStar.setImageResource(isFavorite ? R.drawable.favorite : R.drawable.favorite_grey);
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
