package com.example.onserve

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class VolunteerHistoryActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var tvEmpty: TextView
    private val db = FirebaseFirestore.getInstance()
    private val historyList = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: ScheduleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_volunteer_history)

        findViewById<ImageView>(R.id.btn_back_history).setOnClickListener { finish() }
        tvEmpty = findViewById(R.id.tv_empty_history)
        rvHistory = findViewById(R.id.rv_volunteer_history)
        
        rvHistory.layoutManager = LinearLayoutManager(this)
        adapter = ScheduleAdapter(historyList) { _ ->
            // Already done, maybe show details or something but mark done not needed
        }
        rvHistory.adapter = adapter

        setupBottomNavigation()
        fetchHistory()
    }

    private fun fetchHistory() {
        val email = getSharedPreferences("OnServePrefs", MODE_PRIVATE).getString("USER_EMAIL", "") ?: ""
        if (email.isEmpty()) return

        db.collection("requests")
            .whereEqualTo("volunteerEmail", email)
            .whereEqualTo("status", "Done")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                historyList.clear()
                snapshot?.documents?.forEach { doc ->
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["docId"] = doc.id
                    historyList.add(data)
                }

                if (historyList.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    rvHistory.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    rvHistory.visibility = View.VISIBLE
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.nav_availability).setOnClickListener {
            startActivity(Intent(this, VolunteerHomeActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_assignment_schedule).setOnClickListener {
            startActivity(Intent(this, AssignmentScheduleActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_manage_schedule).setOnClickListener {
            startActivity(Intent(this, VolunteerManageScheduleActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_volunteer_history).setOnClickListener {
            // Already here
        }
        findViewById<LinearLayout>(R.id.nav_notifications_volunteer).setOnClickListener {
            startActivity(Intent(this, NotificationCenterActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.nav_profile_volunteer).setOnClickListener {
            startActivity(Intent(this, VolunteerProfileActivity::class.java))
            finish()
        }
    }
}
