package com.example.onserve

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.FileOutputStream
import java.io.OutputStreamWriter

import android.graphics.Typeface
import android.widget.TextView

class SignupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)


        val backArrow: ImageView = findViewById(R.id.iv_back_arrow)
        backArrow.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val tvUser: TextView = findViewById(R.id.tv_user_toggle)
        val tvVolunteer: TextView = findViewById(R.id.tv_volunteer_toggle)

        tvUser.setOnClickListener {
            tvUser.setBackgroundResource(R.drawable.input_rounded)
            tvUser.setTypeface(null, Typeface.BOLD)
            tvVolunteer.background = null
            tvVolunteer.setTypeface(null, Typeface.NORMAL)
        }

        tvVolunteer.setOnClickListener {
            tvVolunteer.setBackgroundResource(R.drawable.input_rounded)
            tvVolunteer.setTypeface(null, Typeface.BOLD)
            tvUser.background = null
            tvUser.setTypeface(null, Typeface.NORMAL)
        }

        val etName: EditText = findViewById(R.id.et_signup_name)
        val etEmail: EditText = findViewById(R.id.et_signup_email)
        val etPassword: EditText = findViewById(R.id.et_signup_password)
        val btnSubmit: Button = findViewById(R.id.btn_signup_submit)

        btnSubmit.setOnClickListener {
            val name = etName.text.toString()
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                saveToCSV(name, email, password)
            } else {
                Toast.makeText(
                    this@SignupActivity,
                    "Please fill all fields",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun saveToCSV(name: String?, email: String?, password: String?) {
        try {

            val fOut: FileOutputStream = openFileOutput("students.csv", MODE_APPEND)
            val osw: OutputStreamWriter = OutputStreamWriter(fOut)


            osw.write(name + "," + email + "," + password + "\n")
            osw.flush()
            osw.close()

            Toast.makeText(this, "Account Created!", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error saving data", Toast.LENGTH_SHORT).show()
        }
    }
}