package com.example.onserve

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {
    private var isVolunteer = false
    private lateinit var db: FirebaseFirestore

    private val termsResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val cbAgree: CheckBox = findViewById(R.id.cb_agree_terms)
            cbAgree.isChecked = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        db = FirebaseFirestore.getInstance()

        val backArrow: ImageView = findViewById(R.id.iv_back_arrow)
        backArrow.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val tvUser: TextView = findViewById(R.id.tv_user_toggle)
        val tvVolunteer: TextView = findViewById(R.id.tv_volunteer_toggle)

        tvUser.setOnClickListener {
            isVolunteer = false
            tvUser.setBackgroundResource(R.drawable.input_rounded)
            tvUser.setTypeface(null, Typeface.BOLD)
            tvVolunteer.background = null
            tvVolunteer.setTypeface(null, Typeface.NORMAL)
        }

        tvVolunteer.setOnClickListener {
            isVolunteer = true
            tvVolunteer.setBackgroundResource(R.drawable.input_rounded)
            tvVolunteer.setTypeface(null, Typeface.BOLD)
            tvUser.background = null
            tvUser.setTypeface(null, Typeface.NORMAL)
        }

        val etFirstName: EditText = findViewById(R.id.et_signup_first_name)
        val etLastName: EditText = findViewById(R.id.et_signup_last_name)
        val etEmail: EditText = findViewById(R.id.et_signup_email)
        val etPhone: EditText = findViewById(R.id.et_signup_phone)
        val etAddress: EditText = findViewById(R.id.et_signup_address)
        val etPassword: EditText = findViewById(R.id.et_signup_password)
        val btnSubmit: Button = findViewById(R.id.btn_signup_submit)
        val cbAgree: CheckBox = findViewById(R.id.cb_agree_terms)
        val tvTermsLink: TextView = findViewById(R.id.tv_terms_link)

        tvTermsLink.setOnClickListener {
            val intent = Intent(this, TermsConditionsActivity::class.java)
            termsResultLauncher.launch(intent)
        }

        btnSubmit.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val address = etAddress.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (!cbAgree.isChecked) {
                DialogUtils.showErrorDialog(this, "Terms & Conditions", "You must agree to the terms and conditions to proceed.")
                return@setOnClickListener
            }

            if (firstName.isNotEmpty() && lastName.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && phone.isNotEmpty() && address.isNotEmpty()) {
                
                if (!NetworkUtils.isNetworkAvailable(this)) {
                    DialogUtils.showErrorDialog(this, "Network Error", "No internet connection detected. Please check your signal and try again.")
                    return@setOnClickListener
                }

                val fullName = "$firstName $lastName"
                val hashedPassword = HashUtils.sha256(password)
                val user = User(fullName, email, hashedPassword, if (isVolunteer) "Volunteer" else "User", phone, address)
                
                // Save email for session consistency
                getSharedPreferences("OnServePrefs", MODE_PRIVATE).edit().putString("USER_EMAIL", email).apply()

                // Use email as the unique ID for simplicity without Auth UID
                if (isVolunteer) {
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.putExtra("NAME", fullName)
                    intent.putExtra("EMAIL", email)
                    intent.putExtra("PASSWORD", password)
                    intent.putExtra("PHONE", phone)
                    intent.putExtra("ADDRESS", address)
                    startActivity(intent)
                } else {
                    saveUserToFirestore(email, user)
                }
            } else {
                DialogUtils.showErrorDialog(this, "Incomplete Form", "Please fill in all the required fields to create your account.")
            }
        }
    }

    private fun saveUserToFirestore(email: String, user: User) {
        db.collection("users").document(email).set(user)
            .addOnSuccessListener {
                startActivity(Intent(this, SuccessActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                DialogUtils.showErrorDialog(this, "Registration Failed", e.message ?: "Could not complete registration.")
            }
    }
}
