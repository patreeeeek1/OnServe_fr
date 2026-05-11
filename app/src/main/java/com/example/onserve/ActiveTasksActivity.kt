package com.example.onserve

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class ActiveTasksActivity : AppCompatActivity() {

    private lateinit var rvTasks: RecyclerView
    private lateinit var adapter: ActiveTasksAdapter
    private val db = FirebaseFirestore.getInstance()
    private val taskList = mutableListOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_active_tasks)

        findViewById<FrameLayout>(R.id.btn_back_active_tasks).setOnClickListener { finish() }

        rvTasks = findViewById(R.id.rv_active_tasks)
        rvTasks.layoutManager = LinearLayoutManager(this)
        adapter = ActiveTasksAdapter(taskList) { docId ->
            completeTask(docId)
        }
        rvTasks.adapter = adapter

        setupBottomNavigation()
        fetchActiveTasks()
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.nav_home_volunteer).setOnClickListener {
            val intent = Intent(this, VolunteerHomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        findViewById<LinearLayout>(R.id.nav_available_tasks).setOnClickListener {
            startActivity(Intent(this, AvailableTasksActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.nav_active_tasks).setOnClickListener {
            // Already here
        }

        findViewById<LinearLayout>(R.id.nav_profile_volunteer).setOnClickListener {
            startActivity(Intent(this, VolunteerProfileActivity::class.java))
            finish()
        }
    }

    private fun fetchActiveTasks() {
        val userEmail = getSharedPreferences("OnServePrefs", MODE_PRIVATE).getString("USER_EMAIL", "") ?: ""
        if (userEmail.isEmpty()) return

        db.collection("requests")
            .whereEqualTo("status", "In Progress")
            .whereEqualTo("volunteerEmail", userEmail)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    DialogUtils.showErrorDialog(this, "Fetch Error", e.message ?: "Failed to load tasks.")
                    return@addSnapshotListener
                }

                taskList.clear()
                snapshot?.documents?.forEach { doc ->
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["docId"] = doc.id
                    taskList.add(data)
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun completeTask(docId: String) {
        db.collection("requests").document(docId).update("status", "Done")
            .addOnSuccessListener {
                showSuccessDialog("Great job! Task marked as completed.")
            }
            .addOnFailureListener { e ->
                DialogUtils.showErrorDialog(this, "Update Error", e.message ?: "Failed to complete task.")
            }
    }

    private fun showSuccessDialog(message: String) {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_success)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.setCancelable(false)
        dialog.findViewById<TextView>(R.id.tv_success_message).text = message
        dialog.findViewById<Button>(R.id.btn_success_ok).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
}

class ActiveTasksAdapter(
    private val items: List<Map<String, Any>>,
    private val onCompleteClick: (String) -> Unit
) : RecyclerView.Adapter<ActiveTasksAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvPriority: TextView = v.findViewById(R.id.tv_active_priority)
        val tvType: TextView = v.findViewById(R.id.tv_active_type)
        val tvDesc: TextView = v.findViewById(R.id.tv_active_desc)
        val tvSubmittedDate: TextView = v.findViewById(R.id.tv_active_submitted_date)
        val tvName: TextView = v.findViewById(R.id.tv_active_user_name)
        val tvLocation: TextView = v.findViewById(R.id.tv_active_location)
        val btnShowMore: TextView = v.findViewById(R.id.btn_active_show_more)
        val layoutDetails: View = v.findViewById(R.id.layout_active_details)
        val btnComplete: Button = v.findViewById(R.id.btn_complete_task)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_active_task, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvType.text = item["type"] as? String
        holder.tvDesc.text = item["description"] as? String
        
        val priority = item["priority"] as? String ?: "No Priority"
        holder.tvPriority.text = priority

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
            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault())
            holder.tvSubmittedDate.text = sdf.format(java.util.Date(ts))
        }

        holder.tvName.text = item["userName"] as? String ?: "N/A"
        holder.tvLocation.text = item["location"] as? String ?: "N/A"

        holder.btnShowMore.setOnClickListener {
            val isVisible = holder.layoutDetails.visibility == View.VISIBLE
            holder.layoutDetails.visibility = if (isVisible) View.GONE else View.VISIBLE
            holder.btnShowMore.text = if (isVisible) "Show more" else "Show less"
        }

        holder.btnComplete.setOnClickListener {
            val docId = item["docId"] as? String
            if (docId != null) onCompleteClick(docId)
        }
    }

    override fun getItemCount() = items.size
}
