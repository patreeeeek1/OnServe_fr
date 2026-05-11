package com.example.onserve

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TermsConditionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms_conditions)

        val btnBack: ImageView = findViewById(R.id.btn_back)
        val btnAgree: Button = findViewById(R.id.btn_agree)
        val cbTerms: CheckBox = findViewById(R.id.cb_terms_inner)

        btnBack.setOnClickListener {
            finish()
        }

        btnAgree.setOnClickListener {
            if (cbTerms.isChecked) {
                setResult(RESULT_OK)
                finish()
            } else {
                DialogUtils.showErrorDialog(this, "Agreement Required", "Please check the box to agree to the terms and conditions.")
            }
        }
    }
}
