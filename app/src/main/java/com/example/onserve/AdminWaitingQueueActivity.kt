package com.example.onserve

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminWaitingQueueActivity : AppCompatActivity() {

    private lateinit var rvWaiting: RecyclerView
    private lateinit var adapter: AdminWaitingAdapter
    private val db = FirebaseFirestore.getInstance()
    private val waitingList = mutableListOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_waiting_queue)

        findViewById<FrameLayout>(R.id.btn_back_waiting).setOnClickListener { finish() }

        rvWaiting = findViewById(R.id.rv_admin_waiting)
        rvWaiting.layoutManager = LinearLayoutManager(this)
        adapter = AdminWaitingAdapter(waitingList) { docId ->
            showDeleteConfirmation(docId)
        }
        rvWaiting.adapter = adapter

        fetchWaitingRequests()
    }

    private fun showDeleteConfirmation(docId: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Request")
            .setMessage("Are you sure you want to remove this request from the waiting queue?")
            .setPositiveButton("Yes") { _, _ ->
                deleteRequest(docId)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteRequest(docId: String) {
        db.collection("requests").document(docId)
            .delete()
            .addOnSuccessListener {
                DialogUtils.showSuccessDialog(this, "Request removed from queue.")
            }
            .addOnFailureListener { e ->
                DialogUtils.showErrorDialog(this, "Delete Error", e.message ?: "Failed to remove.")
            }
    }

    private fun fetchWaitingRequests() {
        db.collection("requests")
            .whereEqualTo("status", "In Waiting Queue")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    DialogUtils.showErrorDialog(this, "Fetch Error", e.message ?: "Failed to load waiting queue.")
                    return@addSnapshotListener
                }

                waitingList.clear()
                val docs = snapshot?.documents?.map { doc ->
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["docId"] = doc.id
                    data
                }?.sortedWith(compareByDescending<MutableMap<String, Any>> { it["emergency"] as? Boolean ?: false }
                    .thenBy { it["priority"] as? String ?: "" } // Priority established P1 > P2 > P3
                    .thenBy { it["timestamp"] as? Long ?: 0L })

                docs?.forEach { waitingList.add(it) }
                adapter.notifyDataSetChanged()
            }
    }
}

class AdminWaitingAdapter(
    private val items: List<Map<String, Any>>,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<AdminWaitingAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvType: TextView = v.findViewById(R.id.tv_admin_req_type)
        val tvUser: TextView = v.findViewById(R.id.tv_admin_req_user)
        val tvDesc: TextView = v.findViewById(R.id.tv_admin_req_desc)
        val tvPriority: TextView = v.findViewById(R.id.tv_admin_priority_label)
        val btnDelete: ImageView = v.findViewById(R.id.btn_delete_request)
        
        val tvSubmittedDate: TextView = v.findViewById(R.id.tv_admin_submitted_date)
        val tvName: TextView = v.findViewById(R.id.tv_admin_req_name)
        val tvEmail: TextView = v.findViewById(R.id.tv_admin_req_email)
        val tvPhone: TextView = v.findViewById(R.id.tv_admin_req_phone)
        val btnShowMore: TextView = v.findViewById(R.id.btn_admin_show_more)
        val layoutDetails: View = v.findViewById(R.id.layout_admin_details)
        val tvWaitingStatus: TextView = v.findViewById(R.id.tv_admin_req_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        // Reuse item_admin_active_request layout since it has the status and delete button
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_active_request, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvType.text = item["type"] as? String
        holder.tvUser.text = "User: ${item["userEmail"]}"
        holder.tvDesc.text = item["description"] as? String
        
        val priority = item["priority"] as? String ?: "No Priority"
        holder.tvPriority.text = priority

        // Color labeling for priorities
        when {
            priority.contains("P1") -> {
                holder.tvPriority.setTextColor(0xFFFFFFFF.toInt())
                holder.tvPriority.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFC62828.toInt())
            }
            priority.contains("P2") -> {
                holder.tvPriority.setTextColor(0xFF000000.toInt())
                holder.tvPriority.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFFBF00.toInt())
            }
            priority.contains("P3") -> {
                holder.tvPriority.setTextColor(0xFFFFFFFF.toInt())
                holder.tvPriority.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2E7D32.toInt())
            }
        }

        val ts = item["timestamp"] as? Long ?: 0L
        if (ts > 0) {
            val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
            holder.tvSubmittedDate.text = sdf.format(Date(ts))
        } else {
            holder.tvSubmittedDate.text = "N/A"
        }

        holder.tvName.text = item["userName"] as? String ?: "N/A"
        holder.tvEmail.text = item["userEmail"] as? String ?: "N/A"
        holder.tvPhone.text = item["phoneNumber"] as? String ?: "N/A"
        
        val sTime = item["scheduledTime"] as? String
        holder.tvWaitingStatus.text = if (sTime != null) "Status: In Waiting Queue\nTime: $sTime" else "Status: In Waiting Queue"
        
        // Hide reassign button in waiting queue
        val btnReassign: View? = holder.itemView.findViewById(R.id.btn_reassign)
        btnReassign?.visibility = View.GONE

        holder.btnShowMore.setOnClickListener {
            val isVisible = holder.layoutDetails.visibility == View.VISIBLE
            holder.layoutDetails.visibility = if (isVisible) View.GONE else View.VISIBLE
            holder.btnShowMore.text = if (isVisible) "Show more" else "Show less"
        }

        holder.btnDelete.setOnClickListener {
            val docId = item["docId"] as? String
            if (docId != null) onDeleteClick(docId)
        }
    }

    override fun getItemCount() = items.size
}
