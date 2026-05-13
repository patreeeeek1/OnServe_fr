package com.example.onserve;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class SuccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);

        Button btnGoToDashboard = findViewById(R.id.btn_go_to_dashboard);
        btnGoToDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userEmail = getSharedPreferences("OnServePrefs", MODE_PRIVATE).getString("USER_EMAIL", "");
                if (userEmail.isEmpty()) {
                    finish();
                    return;
                }

                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(userEmail).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            Intent intent;
                            String type = documentSnapshot.getString("type");
                            if ("Volunteer".equals(type)) {
                                intent = new Intent(SuccessActivity.this, VolunteerHomeActivity.class);
                            } else {
                                intent = new Intent(SuccessActivity.this, UserHomeActivity.class);
                            }
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        })
                        .addOnFailureListener(e -> {
                            Intent intent = new Intent(SuccessActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        });
            }
        });
    }
}
