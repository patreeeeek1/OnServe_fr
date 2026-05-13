package com.example.onserve;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "OnServe_OS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "[PROCESS STATE: NEW] -> [STATE: READY] - MainActivity created and resources allocated.");

        Button loginBtn = findViewById(R.id.loginButton);
        Button signupBtn = findViewById(R.id.signupButton);
        TextView adminLoginBtn = findViewById(R.id.tv_admin_login_main);

        // 2. Action for the Login Button
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate only to LoginActivity
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        // 3. Action for the Signup Button
        signupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate only to SignupActivity
                Intent intent = new Intent(MainActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });

        adminLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAdminPasswordDialog();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "[PROCESS STATE: READY] - MainActivity is now visible.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "[PROCESS STATE: RUNNING] - MainActivity is executing in CPU foreground.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "[PROCESS STATE: RUNNING] -> [STATE: WAITING] - MainActivity losing focus.");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "[PROCESS STATE: WAITING] -> [STATE: READY] - MainActivity returning from background.");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "[PROCESS STATE: WAITING] - MainActivity is backgrounded.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "[PROCESS STATE: TERMINATED] - MainActivity resources released.");
    }

    private void showAdminPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Admin Login");
        builder.setMessage("Enter Admin Password");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Login", (dialog, which) -> {
            String password = input.getText().toString();
            if (password.equals("12345")) {
                startActivity(new Intent(MainActivity.this, AdminDashboardActivity.class));
                finish();
            } else {
                Toast.makeText(MainActivity.this, "Incorrect Password", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}