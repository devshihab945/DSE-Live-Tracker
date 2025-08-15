package com.dse.dsetracker;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DSEActivity extends AppCompatActivity {

    private String type, url;
    RecyclerView dseRecyclerView;
    private LottieAnimationView loadingAnimation;
    private List<DSEModel> dseList;
    private DSEAdapter adapter;

    @SuppressLint({"MissingInflatedId", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        setContentView(R.layout.activity_dse);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        ImageView ivBack = findViewById(R.id.ivBack);
        TextView tvTitle = findViewById(R.id.tvTittle);
        ImageView btnRefresh = findViewById(R.id.btnRefresh);
        dseRecyclerView = findViewById(R.id.dseRecyclerView);
        loadingAnimation = findViewById(R.id.loadingAnimation);

        ivBack.setOnClickListener(v -> finish());

        // Get data from intent
        String titleFromIntent = getIntent().getStringExtra("title");
        type = getIntent().getStringExtra("type");
        url = getIntent().getStringExtra("url");

        if (url == null) {
            url = "https://shihab.technetia.xyz/GUB/8thSem/CSE426/DSEBOT/show_dse_data.php";
        }

        // Set title & URL based on type
        switch (type != null ? type : "") {
            case "top_gaining":
                tvTitle.setText("Top Gaining Companies");
                url = "https://shihab.technetia.xyz/GUB/8thSem/CSE426/DSEBOT/top_gaining.php";
                break;
            case "top_losing":
                tvTitle.setText("Top Losing Companies");
                url = "https://shihab.technetia.xyz/GUB/8thSem/CSE426/DSEBOT/top_losing.php";
                break;
            case "favorite":
                tvTitle.setText("Favorite Companies");
                break;
            default:
                tvTitle.setText(titleFromIntent != null ? titleFromIntent : "DSE Tracker");
                break;
        }

        // Setup RecyclerView
        dseList = new ArrayList<>();
        adapter = new DSEAdapter(this, dseList);
        dseRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dseRecyclerView.setAdapter(adapter);

        // Load data
        if ("favorite".equals(type)) {
            fetchAndFilterFavorites();
        } else {
            fetchDSEData(url, null);
        }

        btnRefresh.setOnClickListener(v -> {
            if ("favorite".equals(type)) {
                fetchAndFilterFavorites();
            } else {
                fetchDSEData(url, null);
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchAndFilterFavorites() {
        Set<String> favorites = getFavorites();
        if (favorites.isEmpty()) {
            dseList.clear();
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "No favorite companies found", Toast.LENGTH_SHORT).show();
            return;
        }
        // Fetch all data then filter
        fetchDSEData("https://shihab.technetia.xyz/GUB/8thSem/CSE426/DSEBOT/show_dse_data.php", favorites);
    }

    private void fetchDSEData(String requestUrl, Set<String> filterFavorites) {
        loadingAnimation.setVisibility(View.VISIBLE);

        JSONObject jsonRequest = new JSONObject();
        try {
            jsonRequest.put("key", "123456");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        @SuppressLint("NotifyDataSetChanged") JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                requestUrl,
                jsonRequest,
                response -> {
                    try {
                        if ("success".equalsIgnoreCase(response.getString("status"))) {
                            JSONArray dataArray = response.getJSONArray("data");
                            dseList.clear();
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject obj = dataArray.getJSONObject(i);
                                String companyName = obj.getString("companyName");
                                double sharePrice = obj.getDouble("sharePrice");
                                double change = obj.getDouble("change");
                                String changeRate = obj.getString("changeRate");

                                if (filterFavorites == null || filterFavorites.contains(companyName)) {
                                    dseList.add(new DSEModel(companyName, sharePrice, change, changeRate));
                                }
                            }
                            adapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(this, response.getString("message"), Toast.LENGTH_SHORT).show();
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

    private Set<String> getFavorites() {
        SharedPreferences prefs = getSharedPreferences("favorites", MODE_PRIVATE);
        return new HashSet<>(prefs.getStringSet("favorite_stocks", new HashSet<>()));
    }
}
