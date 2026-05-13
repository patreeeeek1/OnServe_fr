package com.example.onserve;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvName, tvAddress, tvPhone, tvEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvName = findViewById(R.id.tv_profile_name);
        tvAddress = findViewById(R.id.tv_profile_address);
        tvPhone = findViewById(R.id.tv_profile_phone);
        tvEmail = findViewById(R.id.tv_profile_email);

        ImageView btnBack = findViewById(R.id.btn_back_profile);
        LinearLayout btnMyRequests = findViewById(R.id.btn_my_requests);
        LinearLayout btnEditProfile = findViewById(R.id.btn_edit_profile);
        Button btnLogout = findViewById(R.id.btn_logout);

        loadUserData();

        btnBack.setOnClickListener(v -> finish());

        btnMyRequests.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, RequestsActivity.class));
        });

        btnEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class));
        });

        btnLogout.setOnClickListener(v -> {
            // Clear current session memory
            getSharedPreferences("OnServePrefs", MODE_PRIVATE).edit().remove("USER_EMAIL").apply();
            
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        LinearLayout navHome = findViewById(R.id.nav_home);
        navHome.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, UserHomeActivity.class));
            finish();
        });

        LinearLayout navRequests = findViewById(R.id.nav_requests);
        navRequests.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, RequestsActivity.class));
            finish();
        });

        findViewById(R.id.nav_history).setOnClickListener(v -> {
            startActivity(new Intent(this, RequestHistoryActivity.class));
            finish();
        });

        findViewById(R.id.nav_notifications).setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationCenterActivity.class));
            finish();
        });
    }

    private void loadUserData() {
        String email = getSharedPreferences("OnServePrefs", MODE_PRIVATE).getString("USER_EMAIL", null);
        if (email == null) return;

        FirebaseFirestore.getInstance().collection("users").document(email).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            tvName.setText(user.getName());
                            tvEmail.setText(user.getEmail());
                            tvPhone.setText(user.getPhone());
                            tvAddress.setText(user.getAddress());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
    }
}
