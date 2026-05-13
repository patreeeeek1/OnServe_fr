package com.example.onserve

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : BaseActivity() {
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        db = FirebaseFirestore.getInstance()

        val backArrow: ImageView = findViewById(R.id.iv_back_arrow)
        backArrow.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val etEmail: EditText = findViewById(R.id.et_login_email)
        val etPassword: EditText = findViewById(R.id.et_login_password)
        val btnSubmit: Button = findViewById(R.id.btn_login_submit)
        val tvAdminLogin: android.widget.TextView = findViewById(R.id.tv_admin_login)

        tvAdminLogin.setOnClickListener {
            showAdminPasswordDialog()
        }

        findViewById<TextView>(R.id.tv_admin_login).setOnLongClickListener {
            startActivity(Intent(this, ProcessLifecycleActivity::class.java))
            true
        }

        btnSubmit.setOnClickListener {
            val inputEmail = etEmail.text.toString().trim()
            val inputPassword = etPassword.text.toString().trim()

            if (inputEmail.isEmpty() || inputPassword.isEmpty()) {
                DialogUtils.showErrorDialog(this, "Incomplete Form", "Please fill in all fields.")
                return@setOnClickListener
            }

            if (!NetworkUtils.isNetworkAvailable(this)) {
                DialogUtils.showErrorDialog(this, "Network Error", "No internet connection detected.")
                return@setOnClickListener
            }

                // Manually check central database for matching email and password
                db.collection("users").document(inputEmail).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val savedPassword = document.getString("password")
                            val inputHash = HashUtils.sha256(inputPassword)
                            
                            // Check for both hashed and plain text for backward compatibility
                            if (savedPassword == inputPassword || savedPassword == inputHash) {
                                // If plain text matched, migrate to hash automatically
                                if (savedPassword == inputPassword && savedPassword != inputHash) {
                                    db.collection("users").document(inputEmail).update("password", inputHash)
                                }
                                
                                val user = document.toObject(User::class.java)
                            if (user != null) {
                                // Remember current user email for this session
                                val prefs = getSharedPreferences("OnServePrefs", MODE_PRIVATE)
                                prefs.edit().putString("USER_EMAIL", inputEmail).apply()

                                if (user.type == "User") {
                                    startActivity(Intent(this, UserHomeActivity::class.java))
                                } else if (user.type == "Volunteer") {
                                    startActivity(Intent(this, VolunteerHomeActivity::class.java))
                                } else {
                                    DialogUtils.showErrorDialog(this, "Login Info", "Logged in as ${user.type}")
                                    startActivity(Intent(this, UserHomeActivity::class.java))
                                }
                                finish()
                            }
                        } else {
                            DialogUtils.showErrorDialog(this, "Invalid Credentials", "The email or password you entered is incorrect.")
                        }
                    } else {
                        DialogUtils.showErrorDialog(this, "Account Not Found", "The email address you entered is not registered.")
                    }
                }
                .addOnFailureListener { e ->
                    DialogUtils.showErrorDialog(this, "System Error", e.message ?: "An unexpected error occurred.")
                }
        }
    }

    private fun showAdminPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Admin Login")
        builder.setMessage("Enter Admin Password")

        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder.setView(input)

        builder.setPositiveButton("Login") { dialog, _ ->
            val password = input.text.toString()
            if (password == "12345") {
                startActivity(Intent(this, AdminDashboardActivity::class.java))
                finish()
            } else {
                DialogUtils.showErrorDialog(this, "Incorrect Password", "The password you entered is incorrect.")
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }
}
