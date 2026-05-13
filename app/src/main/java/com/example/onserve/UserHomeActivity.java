package com.example.onserve;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class UserHomeActivity extends AppCompatActivity {

    private ListenerRegistration connectionListener;
    private View connectionDot;
    private TextView tvConnectionStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_home);

        connectionDot = findViewById(R.id.view_connection_dot);
        tvConnectionStatus = findViewById(R.id.tv_connection_status);

        startMonitoringConnection();

        LinearLayout cardElectrical = findViewById(R.id.card_electrical);
        LinearLayout cardPlumbing = findViewById(R.id.card_plumbing);
        LinearLayout cardStructural = findViewById(R.id.card_structural);
        LinearLayout cardOther = findViewById(R.id.card_other);
        
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navRequests = findViewById(R.id.nav_requests);
        LinearLayout navProfile = findViewById(R.id.nav_profile);
        
        Button btnRequestHelp = findViewById(R.id.btn_request_help);

        View.OnClickListener cardListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String type = "";
                if (v.getId() == R.id.card_electrical) type = "ELECTRICAL";
                else if (v.getId() == R.id.card_plumbing) type = "PLUMBING";
                else if (v.getId() == R.id.card_structural) type = "STRUCTURAL";
                else if (v.getId() == R.id.card_other) type = "OTHER";

                Intent intent = new Intent(UserHomeActivity.this, RequestHelpActivity.class);
                intent.putExtra("SELECTED_TYPE", type);
                startActivity(intent);
            }
        };

        cardElectrical.setOnClickListener(cardListener);
        cardPlumbing.setOnClickListener(cardListener);
        cardStructural.setOnClickListener(cardListener);
        cardOther.setOnClickListener(cardListener);

        navHome.setOnClickListener(v -> {
            // Already home, or refresh
        });

        navRequests.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Switching fragments would be better for persistent nav, but using Activities for now as established
                startActivity(new Intent(UserHomeActivity.this, RequestsActivity.class));
            }
        });

        findViewById(R.id.nav_history).setOnClickListener(v -> {
            startActivity(new Intent(this, RequestHistoryActivity.class));
        });

        findViewById(R.id.nav_notifications).setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationCenterActivity.class));
        });

        findViewById(R.id.btn_notifications).setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationCenterActivity.class));
        });

        navProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(UserHomeActivity.this, ProfileActivity.class));
            }
        });

        btnRequestHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UserHomeActivity.this, RequestHelpActivity.class);
                intent.putExtra("IS_EMERGENCY", true);
                startActivity(intent);
            }
        });
    }

    private void startMonitoringConnection() {
        String email = getSharedPreferences("OnServePrefs", MODE_PRIVATE).getString("USER_EMAIL", "");
        if (email.isEmpty()) return;

        DocumentReference docRef = FirebaseFirestore.getInstance().collection("users").document(email);
        connectionListener = docRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                updateConnectionUI(false);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                // if snapshot.getMetadata().isFromCache() is false, it means we got data from the server
                boolean isConnected = !snapshot.getMetadata().isFromCache();
                updateConnectionUI(isConnected);
            }
        });
    }

    private void updateConnectionUI(boolean isOnline) {
        if (isOnline) {
            connectionDot.setBackgroundResource(R.drawable.dot_online);
            tvConnectionStatus.setText("Connected");
        } else {
            connectionDot.setBackgroundResource(R.drawable.dot_offline);
            tvConnectionStatus.setText("Offline");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectionListener != null) {
            connectionListener.remove();
        }
    }
}
