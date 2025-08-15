package com.dse.dsetracker;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
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
import java.util.List;

public class MainActivity extends AppCompatActivity {

    RecyclerView dseRecyclerView;
    private LottieAnimationView loadingAnimation;
    private List<DSEModel> dseList;
    private DSEAdapter adapter;

    private static final String URL = "https://shihab.technetia.xyz/GUB/8thSem/CSE426/DSEBOT/show_dse_data.php";

    ImageView btnRefresh, btnSearch;
    CardView btnTopGaining, btnTopLosing, btnFavorite;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false); // white icons
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dseRecyclerView = findViewById(R.id.dseRecyclerView);
        loadingAnimation = findViewById(R.id.loadingAnimation);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnSearch = findViewById(R.id.btnSearch);

        btnTopGaining = findViewById(R.id.btnTopGaining);
        btnTopLosing = findViewById(R.id.btnTopLosing);
        btnFavorite = findViewById(R.id.btnFavorite);

        dseList = new ArrayList<>();
        adapter = new DSEAdapter(this, dseList);
        dseRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dseRecyclerView.setAdapter(adapter);

        fetchDSEData();

        btnRefresh.setOnClickListener(v -> fetchDSEData());

        btnRefresh.setOnClickListener(v -> fetchDSEData());

        btnSearch.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SearchActivity.class)));

        btnTopGaining.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DSEActivity.class);
            intent.putExtra("type", "top_gaining");
            startActivity(intent);
        });

        btnTopLosing.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DSEActivity.class);
            intent.putExtra("type", "top_losing");
            startActivity(intent);
        });

        btnFavorite.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DSEActivity.class);
            intent.putExtra("type", "favorite");
            startActivity(intent);
        });

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
                        int total = response.getInt("total");

                        if ("success".equalsIgnoreCase(status)) {
                            JSONArray dataArray = response.getJSONArray("data");
                            dseList.clear();
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject obj = dataArray.getJSONObject(i);
                                String companyName = obj.getString("companyName");
                                double sharePrice = obj.getDouble("sharePrice");
                                double change = obj.getDouble("change");
                                String changeRate = obj.getString("changeRate");

                                dseList.add(new DSEModel(companyName, sharePrice, change, changeRate));
                            }
                            adapter.notifyDataSetChanged();

                        } else {
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "JSON Parse Error", Toast.LENGTH_SHORT).show();
                    }
                    loadingAnimation.setVisibility(View.GONE);
                },
                error -> {
                    loadingAnimation.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
        );

        // Retry policy
        request.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        Volley.newRequestQueue(this).add(request);
    }


}