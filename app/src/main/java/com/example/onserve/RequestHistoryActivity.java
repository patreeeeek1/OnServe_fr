package com.example.onserve;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class RequestHistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private RequestsAdapter historyAdapter;
    private List<HelpRequest> historyList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_history);

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        rvHistory = findViewById(R.id.rv_history);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        historyAdapter = new RequestsAdapter(historyList, this::showDeleteConfirmation);
        rvHistory.setAdapter(historyAdapter);

        fetchHistoryFromFirestore();

        setupNavigation();
    }

    private void setupNavigation() {
        findViewById(R.id.nav_home).setOnClickListener(v -> {
            startActivity(new Intent(this, UserHomeActivity.class));
            finish();
        });

        findViewById(R.id.nav_requests).setOnClickListener(v -> {
            startActivity(new Intent(this, RequestsActivity.class));
            finish();
        });

        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });
    }

    private void fetchHistoryFromFirestore() {
        String userEmail = getSharedPreferences("OnServePrefs", MODE_PRIVATE).getString("USER_EMAIL", null);
        if (userEmail == null) return;

        FirebaseFirestore.getInstance().collection("requests")
                .whereEqualTo("userEmail", userEmail)
                .whereEqualTo("status", "Done")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    historyList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        HelpRequest req = document.toObject(HelpRequest.class);
                        req.setDocId(document.getId());
                        historyList.add(req);
                    }
                    historyList.sort((r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));
                    historyAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteConfirmation(HelpRequest request) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Request")
                .setMessage("Are you sure you want to remove this request from your history?")
                .setPositiveButton("Yes", (dialog, which) -> deleteRequest(request))
                .setNegativeButton("No", null)
                .show()
                .getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE));
    }

    private void deleteRequest(HelpRequest request) {
        if (request.getDocId() == null) return;
        FirebaseFirestore.getInstance().collection("requests").document(request.getDocId()).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "History entry deleted", Toast.LENGTH_SHORT).show();
                    fetchHistoryFromFirestore();
                });
    }
}
