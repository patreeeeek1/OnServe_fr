package com.example.onserve

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class PriorityOverviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_priority_overview)

        findViewById<FrameLayout>(R.id.btn_back_priority).setOnClickListener { finish() }
    }
}
