package com.example.onserve;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class RequestsActivity extends AppCompatActivity {

    private RecyclerView rvActive;
    private RequestsAdapter activeAdapter;
    private List<HelpRequest> activeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requests);

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        rvActive = findViewById(R.id.rv_active_requests);

        rvActive.setLayoutManager(new LinearLayoutManager(this));

        activeAdapter = new RequestsAdapter(activeList, this::showDeleteConfirmation);

        rvActive.setAdapter(activeAdapter);

        fetchRequestsFromFirestore();

        setupNavigation();
    }

    private void setupNavigation() {
        findViewById(R.id.nav_home).setOnClickListener(v -> {
            startActivity(new Intent(this, UserHomeActivity.class));
            finish();
        });

        findViewById(R.id.nav_history).setOnClickListener(v -> {
            startActivity(new Intent(this, RequestHistoryActivity.class));
            finish();
        });

        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });
    }

    private void fetchRequestsFromFirestore() {
        String userEmail = getSharedPreferences("OnServePrefs", MODE_PRIVATE).getString("USER_EMAIL", null);
        
        if (userEmail == null) {
            DialogUtils.INSTANCE.showErrorDialog(this, "Session Error", "Please login again.");
            return;
        }

        if (!NetworkUtils.INSTANCE.isNetworkAvailable(this)) {
            DialogUtils.INSTANCE.showErrorDialog(this, "Network Status", "No internet connection. Showing offline data if available.");
        }

        FirebaseFirestore.getInstance().collection("requests")
                .whereEqualTo("userEmail", userEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    activeList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        HelpRequest req = document.toObject(HelpRequest.class);
                        req.setDocId(document.getId());
                        // Only add if NOT done (History is now a separate page)
                        if (!req.getStatus().equals("Done")) {
                            activeList.add(req);
                        }
                    }
                    // Sort: Emergency first, then by timestamp
                    activeList.sort((r1, r2) -> {
                        if (r1.isEmergency() != r2.isEmergency()) {
                            return r1.isEmergency() ? -1 : 1;
                        }
                        return Long.compare(r2.getTimestamp(), r1.getTimestamp()); // Newest first
                    });
                    activeAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    DialogUtils.INSTANCE.showErrorDialog(this, "Fetch Error", e.getMessage() != null ? e.getMessage() : "Failed to load requests.");
                });
    }

    private void showDeleteConfirmation(HelpRequest request) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Request")
                .setMessage("Are you sure you want to remove this request?")
                .setPositiveButton("Yes", (dialog, which) -> deleteRequest(request))
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteRequest(HelpRequest request) {
        if (request.getDocId() == null) return;

        FirebaseFirestore.getInstance().collection("requests")
                .document(request.getDocId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    showSuccessDialog("Request deleted successfully");
                    fetchRequestsFromFirestore();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showSuccessDialog(String message) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_success);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        dialog.setCancelable(false);

        TextView tvMessage = dialog.findViewById(R.id.tv_success_message);
        tvMessage.setText(message);

        android.widget.Button btnOk = dialog.findViewById(R.id.btn_success_ok);
        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
    }
}
