package com.example.onserve

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class NotificationCenterActivity : AppCompatActivity() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var tvEmpty: TextView
    private val db = FirebaseFirestore.getInstance()
    private val notificationList = mutableListOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_center)

        findViewById<ImageView>(R.id.btn_back_notifications).setOnClickListener { finish() }
        rvNotifications = findViewById(R.id.rv_notifications)
        tvEmpty = findViewById(R.id.tv_empty_notifications)

        rvNotifications.layoutManager = LinearLayoutManager(this)
        rvNotifications.adapter = NotificationAdapter(notificationList)

        fetchNotifications()
    }

    private fun fetchNotifications() {
        val email = getSharedPreferences("OnServePrefs", MODE_PRIVATE).getString("USER_EMAIL", "") ?: ""
        if (email.isEmpty()) return

        db.collection("notifications")
            .whereEqualTo("targetEmail", email)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                notificationList.clear()
                snapshot?.documents?.forEach { doc ->
                    notificationList.add(doc.data ?: emptyMap())
                }

                if (notificationList.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    rvNotifications.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    rvNotifications.visibility = View.VISIBLE
                }
                rvNotifications.adapter?.notifyDataSetChanged()
            }
    }
}

class NotificationAdapter(private val items: List<Map<String, Any>>) : RecyclerView.Adapter<NotificationAdapter.VH>() {
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tv_notif_title)
        val tvMessage: TextView = v.findViewById(R.id.tv_notif_message)
        val tvTime: TextView = v.findViewById(R.id.tv_notif_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item["title"] as? String
        holder.tvMessage.text = item["message"] as? String
        
        val ts = item["timestamp"] as? Long ?: 0L
        if (ts > 0) {
            val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
            holder.tvTime.text = sdf.format(Date(ts))
        }
    }

    override fun getItemCount() = items.size
}
