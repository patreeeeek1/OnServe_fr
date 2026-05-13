package com.example.onserve

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class AdminDashboardActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        findViewById<LinearLayout>(R.id.card_pending_requests).setOnClickListener {
            startActivity(Intent(this, AdminPendingRequestsActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.card_active_requests).setOnClickListener {
            startActivity(Intent(this, AdminActiveRequestsActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.card_volunteer_list).setOnClickListener {
            startActivity(Intent(this, VolunteerListActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.card_priority_overview).setOnClickListener {
            startActivity(Intent(this, PriorityOverviewActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.card_waiting_queue).setOnClickListener {
            startActivity(Intent(this, AdminWaitingQueueActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.card_admin_history).setOnClickListener {
            startActivity(Intent(this, AdminHistoryActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.card_admin_logout).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
