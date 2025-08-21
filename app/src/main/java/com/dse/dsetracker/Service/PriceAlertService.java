package com.dse.dsetracker.Service;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.dse.dsetracker.Model.DSEModel;
import com.dse.dsetracker.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PriceAlertService extends Service {

    private static final String CHANNEL_ID = "price_alert_channel";
    Handler handler = new Handler();
    private Runnable fetchRunnable;
    private static final int INTERVAL = 5000; // 5 seconds

    List<DSEModel> stockList = new ArrayList<>();
    private SharedPreferences alertPrefs;

    private static final String URL = "https://shihab.technetia.xyz/GUB/8thSem/CSE426/DSEBOT/show_dse_data.php";

    @Override
    public void onCreate() {
        super.onCreate();
        alertPrefs = getSharedPreferences("PriceAlerts", MODE_PRIVATE);

        createNotificationChannel();

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Price Alerts Running")
                .setContentText("Monitoring stock prices in background…")
                .setSmallIcon(R.drawable.notification)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);

        startForeground(1, notification.build());

        startFetching();
    }

    private void startFetching() {
        fetchRunnable = new Runnable() {
            @Override
            public void run() {
                fetchDSEData();
                handler.postDelayed(this, INTERVAL);
            }
        };
        handler.post(fetchRunnable);
    }

    private void fetchDSEData() {
        JSONObject jsonRequest = new JSONObject();
        try {
            jsonRequest.put("key", "123456");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                URL,
                jsonRequest,
                response -> {
                    try {
                        String status = response.getString("status");
                        if ("success".equalsIgnoreCase(status)) {
                            JSONArray dataArray = response.getJSONArray("data");
                            stockList.clear();
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject obj = dataArray.getJSONObject(i);
                                stockList.add(new DSEModel(
                                        obj.getString("companyName"),
                                        obj.getDouble("sharePrice"),
                                        obj.getDouble("change"),
                                        obj.getString("changeRate")
                                ));
                            }
                            checkPriceAlerts();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    // handle Volley error if needed
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        Volley.newRequestQueue(this).add(request);
    }

    private void checkPriceAlerts() {
        for (DSEModel stock : stockList) {
            String priceKey = stock.getCompanyName() + "_price";
            String typeKey = stock.getCompanyName() + "_type";

            if (alertPrefs.contains(priceKey) && alertPrefs.contains(typeKey)) {
                double target = Double.longBitsToDouble(
                        alertPrefs.getLong(priceKey, Double.doubleToLongBits(-1))
                );
                String type = alertPrefs.getString(typeKey, "above");

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
                    showNotification(stock, message);
                    // remove alert
                    alertPrefs.edit().remove(priceKey).remove(typeKey).apply();
                }
            }
        }
    }

    private void showNotification(DSEModel stock, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle("Price Alert: " + stock.getCompanyName())
                .setContentText(message + " (Current: ৳" + stock.getSharePrice() + ")")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("The stock " + stock.getCompanyName() + " has " + message +
                                " (Current: ৳" + stock.getSharePrice() + ")"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        }

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(fetchRunnable);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Price Alerts", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for stock price alerts");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}
