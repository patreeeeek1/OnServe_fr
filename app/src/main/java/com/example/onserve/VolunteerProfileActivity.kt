package com.example.onserve

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class VolunteerProfileActivity : AppCompatActivity() {

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvAddress: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_volunteer_profile)

        tvName = findViewById(R.id.tv_profile_name)
        tvEmail = findViewById(R.id.tv_profile_email)
        tvPhone = findViewById(R.id.tv_profile_phone)
        tvAddress = findViewById(R.id.tv_profile_address)

        findViewById<ImageView>(R.id.btn_back_profile).setOnClickListener { finish() }

        findViewById<LinearLayout>(R.id.btn_edit_profile).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        findViewById<Button>(R.id.btn_logout).setOnClickListener {
            getSharedPreferences("OnServePrefs", MODE_PRIVATE).edit().remove("USER_EMAIL").apply()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Bottom Navigation
        findViewById<LinearLayout>(R.id.nav_availability).setOnClickListener {
            val intent = Intent(this, VolunteerHomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        findViewById<LinearLayout>(R.id.nav_assignment_schedule).setOnClickListener {
            startActivity(Intent(this, AssignmentScheduleActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.nav_profile_volunteer).setOnClickListener {
            // Already here
        }

        loadUserData()
    }

    private fun loadUserData() {
        val email = getSharedPreferences("OnServePrefs", MODE_PRIVATE).getString("USER_EMAIL", null)
        if (email == null) return

        FirebaseFirestore.getInstance().collection("users").document(email).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val user = documentSnapshot.toObject(User::class.java)
                    if (user != null) {
                        tvName.text = user.name
                        tvEmail.text = user.email
                        tvPhone.text = user.phone
                        tvAddress.text = user.address
                    }
                }
            }
            .addOnFailureListener { e ->
                DialogUtils.showErrorDialog(this, "Profile Error", e.message ?: "Failed to load user data.")
            }
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }
}
