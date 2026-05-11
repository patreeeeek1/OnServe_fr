package com.example.onserve

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class AdminHistoryActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var adapter: AdminHistoryAdapter
    private val db = FirebaseFirestore.getInstance()
    private val historyList = mutableListOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_history)

        findViewById<FrameLayout>(R.id.btn_back_history).setOnClickListener { finish() }

        rvHistory = findViewById(R.id.rv_admin_history)
        rvHistory.layoutManager = LinearLayoutManager(this)
        adapter = AdminHistoryAdapter(historyList)
        rvHistory.adapter = adapter

        fetchHistory()
    }

    private fun fetchHistory() {
        db.collection("requests")
            .whereEqualTo("status", "Done")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    DialogUtils.showErrorDialog(this, "Fetch Error", e.message ?: "Failed to load history.")
                    return@addSnapshotListener
                }

                historyList.clear()
                snapshot?.documents?.forEach { doc ->
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["docId"] = doc.id
                    historyList.add(data)
                }
                historyList.sortByDescending { it["timestamp"] as? Long ?: 0L }
                adapter.notifyDataSetChanged()
            }
    }
}

class AdminHistoryAdapter(private val items: List<Map<String, Any>>) : RecyclerView.Adapter<AdminHistoryAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvType: TextView = v.findViewById(R.id.tv_admin_req_type)
        val tvUser: TextView = v.findViewById(R.id.tv_admin_req_user)
        val tvDesc: TextView = v.findViewById(R.id.tv_admin_req_desc)
        val tvPriority: TextView = v.findViewById(R.id.tv_admin_priority_label)
        val tvSubmittedDate: TextView = v.findViewById(R.id.tv_admin_submitted_date)
        val tvName: TextView = v.findViewById(R.id.tv_admin_req_name)
        val tvEmail: TextView = v.findViewById(R.id.tv_admin_req_email)
        val tvPhone: TextView = v.findViewById(R.id.tv_admin_req_phone)
        val btnShowMore: TextView = v.findViewById(R.id.btn_admin_show_more)
        val layoutDetails: View = v.findViewById(R.id.layout_admin_details)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_active_request, parent, false)
        // Hide delete button in history if it exists
        v.findViewById<View>(R.id.btn_delete_request)?.visibility = View.GONE
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvType.text = item["type"] as? String
        holder.tvUser.text = "User: ${item["userEmail"]}"
        holder.tvDesc.text = item["description"] as? String
        holder.tvPriority.text = item["priority"] as? String ?: "Done"
        holder.tvPriority.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF4CAF50.toInt()) // Green for done

        val ts = item["timestamp"] as? Long ?: 0L
        if (ts > 0) {
            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault())
            holder.tvSubmittedDate.text = sdf.format(java.util.Date(ts))
        }

        holder.tvName.text = item["userName"] as? String ?: "N/A"
        holder.tvEmail.text = item["userEmail"] as? String ?: "N/A"
        holder.tvPhone.text = item["phoneNumber"] as? String ?: "N/A"

        holder.btnShowMore.setOnClickListener {
            val isVisible = holder.layoutDetails.visibility == View.VISIBLE
            holder.layoutDetails.visibility = if (isVisible) View.GONE else View.VISIBLE
            holder.btnShowMore.text = if (isVisible) "Show more" else "Show less"
        }
    }

    override fun getItemCount() = items.size
}
