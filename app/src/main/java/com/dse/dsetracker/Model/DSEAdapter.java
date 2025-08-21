package com.dse.dsetracker.Model;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.dse.dsetracker.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DSEAdapter extends RecyclerView.Adapter<DSEAdapter.ViewHolder> {

    private final Context context;
    private final List<DSEModel> dseList;
    private final SharedPreferences favPrefs;
    private final SharedPreferences alertPrefs;
    private final Handler alertHandler = new Handler();
    private boolean alertCheckerStarted = false;

    public DSEAdapter(Context context, List<DSEModel> dseList) {
        this.context = context;
        this.dseList = dseList;
        this.favPrefs = context.getSharedPreferences("favorites", MODE_PRIVATE);
        this.alertPrefs = context.getSharedPreferences("PriceAlerts", MODE_PRIVATE);

        createNotificationChannel();
        startPriceAlertChecker();
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

        int color = model.getChange() >= 0 ? R.color.green : R.color.red;
        holder.tvSharePrice.setTextColor(ContextCompat.getColor(context, color));
        holder.tvChangeValue.setTextColor(ContextCompat.getColor(context, color));
        holder.tvChangePercent.setTextColor(ContextCompat.getColor(context, color));

        boolean isFavorite = getFavorites().contains(model.getCompanyName());
        updateFavoriteIcon(holder.ivStar, isFavorite);

        // Bell color depends on alert existence
        if (alertPrefs.contains(model.getCompanyName() + "_price")) {
            holder.ivBell.setImageResource(R.drawable.notification_running);
        } else {
            holder.ivBell.setImageResource(R.drawable.notification);
        }

        // --- Star click ---
        holder.ivStar.setOnClickListener(v -> {
            String companyName = model.getCompanyName();
            Set<String> favorites = new HashSet<>(getFavorites());

            if (favorites.contains(companyName)) {
                favorites.remove(companyName);
                Toast.makeText(context, companyName + " removed from favorites", Toast.LENGTH_SHORT).show();
                updateFavoriteIcon(holder.ivStar, false);
            } else {
                favorites.add(companyName);
                Toast.makeText(context, companyName + " added to favorites", Toast.LENGTH_SHORT).show();
                updateFavoriteIcon(holder.ivStar, true);
            }

            favPrefs.edit().putStringSet("favorite_stocks", favorites).apply();
        });

        // --- Bell click ---
        holder.ivBell.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_price_alert, null);
            EditText etPrice = dialogView.findViewById(R.id.etTargetPrice);
            TextView btnSet = dialogView.findViewById(R.id.btnSetAlert);
            TextView btnCancel = dialogView.findViewById(R.id.btnCancel);
            TextView btnRemove = dialogView.findViewById(R.id.btnRemoveAlert);

            // Spinner for Above / Below
            Spinner spType = dialogView.findViewById(R.id.spType);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                    android.R.layout.simple_spinner_item,
                    new String[]{"Above", "Below"});
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spType.setAdapter(adapter);

            // Load previous alert if exists
            if (alertPrefs.contains(model.getCompanyName() + "_price")) {
                double prevPrice = Double.longBitsToDouble(alertPrefs.getLong(model.getCompanyName() + "_price", 0));
                String prevType = alertPrefs.getString(model.getCompanyName() + "_type", "above");
                etPrice.setText(String.valueOf(prevPrice));
                spType.setSelection("above".equals(prevType) ? 0 : 1);
            }

            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .create();

            btnCancel.setOnClickListener(view -> dialog.dismiss());

            btnSet.setOnClickListener(view -> {
                String priceText = etPrice.getText().toString().trim();
                String type = spType.getSelectedItem().toString().toLowerCase();

                if (!priceText.isEmpty()) {
                    try {
                        double targetPrice = Double.parseDouble(priceText);

                        alertPrefs.edit()
                                .putLong(model.getCompanyName() + "_price", Double.doubleToLongBits(targetPrice))
                                .putString(model.getCompanyName() + "_type", type)
                                .apply();

                        holder.ivBell.setImageResource(R.drawable.notification_running);
                        Toast.makeText(context, "Alert set at " + targetPrice + " (" + type + ")", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();

                    } catch (NumberFormatException e) {
                        Toast.makeText(context, "Invalid number", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            // Remove alert
            btnRemove.setOnClickListener(view -> {
                alertPrefs.edit().remove(model.getCompanyName() + "_price")
                        .remove(model.getCompanyName() + "_type")
                        .apply();
                holder.ivBell.setImageResource(R.drawable.notification);
                Toast.makeText(context, "Alert removed", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });

            dialog.show();
        });
    }

    @Override
    public int getItemCount() {
        return dseList.size();
    }

    private Set<String> getFavorites() {
        return favPrefs.getStringSet("favorite_stocks", new HashSet<>());
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

    // ---------------- Price Alert Checker ----------------
    private void startPriceAlertChecker() {
        if (alertCheckerStarted) return;
        alertCheckerStarted = true;

        alertHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkPriceAlerts();
                alertHandler.postDelayed(this, 5000);
            }
        }, 5000);
    }

    private void checkPriceAlerts() {
        for (DSEModel stock : dseList) {
            if (alertPrefs.contains(stock.getCompanyName() + "_price")) {
                double target = Double.longBitsToDouble(
                        alertPrefs.getLong(stock.getCompanyName() + "_price", Double.doubleToLongBits(-1))
                );
                String type = alertPrefs.getString(stock.getCompanyName() + "_type", "above");

                boolean alertTriggered = false;
                String message = "";

                if ("above".equals(type) && stock.getSharePrice() >= target) {
                    alertTriggered = true;
                    message = "Price went above " + target;
                } else if ("below".equals(type) && stock.getSharePrice() <= target) {
                    alertTriggered = true;
                    message = "Price went below " + target;
                }

                if (alertTriggered) {
                    showInAppAlert(stock, message);

                    // Remove alert after notifying
                    alertPrefs.edit().remove(stock.getCompanyName() + "_price")
                            .remove(stock.getCompanyName() + "_type")
                            .apply();
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void showInAppAlert(DSEModel stock, String message) {
        Toast.makeText(context,
                "Price Alert: " + stock.getCompanyName() + " - " + message,
                Toast.LENGTH_LONG).show();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "price_alert_channel")
                .setSmallIcon(R.drawable.notification)
                .setContentTitle("Price Alert: " + stock.getCompanyName())
                .setContentText(message + " (Current: ৳" + stock.getSharePrice() + ")")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("The stock " + stock.getCompanyName() + " has " + message + " (Current: ৳" + stock.getSharePrice() + ")"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = manager.getNotificationChannel("price_alert_channel");
            if (channel == null) {
                channel = new NotificationChannel("price_alert_channel", "Price Alerts",
                        NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Notifications for stock price alerts");
                channel.setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                        new android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build());
                manager.createNotificationChannel(channel);
            }
        } else {
            builder.setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
        }

        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            return;
        managerCompat.notify((int) System.currentTimeMillis(), builder.build());

        notifyDataSetChanged();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("price_alert_channel", "Price Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for stock price alerts");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}
