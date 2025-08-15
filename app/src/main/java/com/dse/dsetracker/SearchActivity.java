package com.dse.dsetracker;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
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
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    RecyclerView dseRecyclerView;
    private LottieAnimationView loadingAnimation;
    private List<DSEModel> dseList;
    private DSEAdapter adapter;

    private static final String URL = "https://shihab.technetia.xyz/GUB/8thSem/CSE426/DSEBOT/dse_query.php";

    ImageView ivBack, btnRefresh, btnSearch;
    EditText edSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false); // white icons
        setContentView(R.layout.activity_search);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dseRecyclerView = findViewById(R.id.dseRecyclerView);
        loadingAnimation = findViewById(R.id.loadingAnimation);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnSearch = findViewById(R.id.btnSearch);
        ivBack = findViewById(R.id.ivBack);
        edSearch = findViewById(R.id.edSearch);

        ivBack.setOnClickListener(v -> finish());

        dseList = new ArrayList<>();
        adapter = new DSEAdapter(this, dseList);
        dseRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dseRecyclerView.setAdapter(adapter);

        btnRefresh.setOnClickListener(v -> {
            String companyName = edSearch.getText().toString().trim();
            if (!companyName.isEmpty()) {
                fetchDSEData(companyName);
            } else {
                Toast.makeText(SearchActivity.this, "Please enter a company name", Toast.LENGTH_SHORT).show();
            }
        });

        btnSearch.setOnClickListener(v -> {
            String companyName = edSearch.getText().toString().trim();
            if (!companyName.isEmpty()) {
                fetchDSEData(companyName);
            } else {
                Toast.makeText(SearchActivity.this, "Please enter a company name", Toast.LENGTH_SHORT).show();
            }
        });

    }


    private void fetchDSEData(String companyName) {
        loadingAnimation.setVisibility(View.VISIBLE);

        JSONObject jsonRequest = new JSONObject();
        try {
            jsonRequest.put("key", "123456");
            jsonRequest.put("companyName", companyName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        @SuppressLint("NotifyDataSetChanged") JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, URL, jsonRequest, response -> {
            try {
                String status = response.getString("status");
                String message = response.getString("message");
                int total = response.getInt("total");

                if ("success".equalsIgnoreCase(status)) {
                    JSONArray dataArray = response.getJSONArray("data");
                    dseList.clear();
                    for (int i = 0; i < dataArray.length(); i++) {
                        JSONObject obj = dataArray.getJSONObject(i);
//                                String companyName = obj.getString("companyName");
                        double sharePrice = obj.getDouble("sharePrice");
                        double change = obj.getDouble("change");
                        String changeRate = obj.getString("changeRate");

                        dseList.add(new DSEModel(companyName, sharePrice, change, changeRate));
                    }
                    adapter.notifyDataSetChanged();

                } else {
                    Toast.makeText(SearchActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(SearchActivity.this, "JSON Parse Error", Toast.LENGTH_SHORT).show();
            }
            loadingAnimation.setVisibility(View.GONE);
        }, error -> {
            loadingAnimation.setVisibility(View.GONE);
            Toast.makeText(SearchActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
        });

        // Retry policy
        request.setRetryPolicy(new DefaultRetryPolicy(0, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(this).add(request);
    }

}