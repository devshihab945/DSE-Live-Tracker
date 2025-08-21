package com.dse.dsetracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.dse.dsetracker.Model.DSEAdapter;
import com.dse.dsetracker.Model.DSEModel;
import com.dse.dsetracker.Service.PriceAlertService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    RecyclerView dseRecyclerView;
    private LottieAnimationView loadingAnimation;
    private List<DSEModel> dseList;
    private DSEAdapter adapter;

    private static final String URL = "https://shihab.technetia.xyz/GUB/8thSem/CSE426/DSEBOT/show_dse_data.php";

    ImageView btnRefresh, btnSearch;
    TextView tvMarketStatus;
    CardView btnTopGaining, btnTopLosing, btnFavorite;

    @SuppressLint({"MissingInflatedId", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-edge
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- Views ---
        dseRecyclerView = findViewById(R.id.dseRecyclerView);
        loadingAnimation = findViewById(R.id.loadingAnimation);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnSearch = findViewById(R.id.btnSearch);
        tvMarketStatus = findViewById(R.id.tvMarketStatus);

        btnTopGaining = findViewById(R.id.btnTopGaining);
        btnTopLosing = findViewById(R.id.btnTopLosing);
        btnFavorite = findViewById(R.id.btnFavorite);

        // --- RecyclerView ---
        dseList = new ArrayList<>();
        adapter = new DSEAdapter(this, dseList);
        dseRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dseRecyclerView.setAdapter(adapter);

        // --- Notification permission ---
        requestNotificationPermission();


        // --- Fetch DSE Data ---
        fetchDSEData();

        // --- Click listeners ---
        btnRefresh.setOnClickListener(v -> fetchDSEData());
        btnSearch.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));

        btnTopGaining.setOnClickListener(v -> openDSEActivity("top_gaining"));
        btnTopLosing.setOnClickListener(v -> openDSEActivity("top_losing"));
        btnFavorite.setOnClickListener(v -> openDSEActivity("favorite"));

        // --- Market Status ---
        updateMarketStatus(tvMarketStatus);
    }

    @SuppressLint("SetTextI18n")
    private void updateMarketStatus(TextView tvMarketStatus) {
        Calendar calendar = Calendar.getInstance(); // local time
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        boolean isTradingDay = (dayOfWeek >= Calendar.MONDAY && dayOfWeek <= Calendar.FRIDAY);

        // DSE Trading Hours: 10:00 - 14:30
        boolean isMarketOpen = false;
        if (isTradingDay) {
            if (hour > 10 && hour < 14 || hour == 10 || hour == 14 && minute <= 30) {
                isMarketOpen = true;
            }
        }

        if (!isTradingDay) {
            tvMarketStatus.setText("Closed (Weekend)");
            tvMarketStatus.setTextColor(ContextCompat.getColor(this, R.color.red));
        } else if (isMarketOpen) {
            tvMarketStatus.setText("Open");
            tvMarketStatus.setTextColor(ContextCompat.getColor(this, R.color.green));
        } else {
            tvMarketStatus.setText("Closed");
            tvMarketStatus.setTextColor(ContextCompat.getColor(this, R.color.red));
        }
    }


    private void openDSEActivity(String type) {
        Intent intent = new Intent(MainActivity.this, DSEActivity.class);
        intent.putExtra("type", type);
        startActivity(intent);
    }

    private void fetchDSEData() {
        loadingAnimation.setVisibility(View.VISIBLE);

        JSONObject jsonRequest = new JSONObject();
        try {
            jsonRequest.put("key", "123456");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        @SuppressLint("NotifyDataSetChanged") JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                URL,
                jsonRequest,
                response -> {
                    try {
                        String status = response.getString("status");
                        String message = response.getString("message");

                        if ("success".equalsIgnoreCase(status)) {
                            JSONArray dataArray = response.getJSONArray("data");
                            dseList.clear();
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject obj = dataArray.getJSONObject(i);
                                dseList.add(new DSEModel(
                                        obj.getString("companyName"),
                                        obj.getDouble("sharePrice"),
                                        obj.getDouble("change"),
                                        obj.getString("changeRate")
                                ));
                            }
                            adapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "JSON Parse Error", Toast.LENGTH_SHORT).show();
                    }
                    loadingAnimation.setVisibility(View.GONE);
                },
                error -> {
                    loadingAnimation.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        Volley.newRequestQueue(this).add(request);
    }

    // ---------------- Notification Permission ----------------
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent serviceIntent = new Intent(this, PriceAlertService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }


}
