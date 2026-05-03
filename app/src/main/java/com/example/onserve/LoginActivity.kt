package com.example.onserve

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.FileInputStream

import android.graphics.Typeface
import android.widget.TextView

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        val backArrow: ImageView = findViewById(R.id.iv_back_arrow)
        backArrow.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val tvUser: TextView = findViewById(R.id.tv_user_toggle)
        val tvVolunteer: TextView = findViewById(R.id.tv_volunteer_toggle)

        tvUser.setOnClickListener {
            // Update UI for User selection
            tvUser.setBackgroundResource(R.drawable.input_rounded)
            tvUser.setTypeface(null, Typeface.BOLD)
            
            tvVolunteer.background = null
            tvVolunteer.setTypeface(null, Typeface.NORMAL)
        }

        tvVolunteer.setOnClickListener {
            // Update UI for Volunteer selection
            tvVolunteer.setBackgroundResource(R.drawable.input_rounded)
            tvVolunteer.setTypeface(null, Typeface.BOLD)
            
            tvUser.background = null
            tvUser.setTypeface(null, Typeface.NORMAL)
        }

        val etEmail: EditText = findViewById(R.id.et_login_email)
        val etPassword: EditText = findViewById(R.id.et_login_password)
        val btnSubmit: Button = findViewById(R.id.btn_login_submit)

        btnSubmit.setOnClickListener {
            val inputEmail = etEmail.text.toString().trim()
            val inputPassword = etPassword.text.toString().trim()

            if (checkCredentials(inputEmail, inputPassword)) {
                val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(
                    this@LoginActivity,
                    "Invalid Email or Password",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    //
    private fun checkCredentials(email: String, password: String): Boolean {
        try {
            val fIn: FileInputStream = openFileInput("students.csv")
            val reader: BufferedReader = BufferedReader(java.io.InputStreamReader(fIn))
            var line: String?


            while ((reader.readLine().also { line = it }) != null) {

                val parts: Array<String?> =
                    line!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                if (parts.size >= 3) {

                    val savedEmail = parts[1]!!.trim { it <= ' ' }
                    val savedPassword = parts[2]!!.trim { it <= ' ' }


                    if (email == savedEmail && password == savedPassword) {
                        reader.close()
                        return true
                    }
                }
            }
            reader.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return false
    }
}