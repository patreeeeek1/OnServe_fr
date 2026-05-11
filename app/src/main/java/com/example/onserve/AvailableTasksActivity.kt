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

class AvailableTasksActivity : AppCompatActivity() {

    private lateinit var rvTasks: RecyclerView
    private lateinit var adapter: AvailableTasksAdapter
    private val db = FirebaseFirestore.getInstance()
    private val taskList = mutableListOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_available_tasks)

        findViewById<FrameLayout>(R.id.btn_back_available).setOnClickListener { finish() }

        rvTasks = findViewById(R.id.rv_available_tasks)
        rvTasks.layoutManager = LinearLayoutManager(this)
        adapter = AvailableTasksAdapter(taskList) { docId ->
            acceptTask(docId)
        }
        rvTasks.adapter = adapter

        setupBottomNavigation()
        fetchAvailableTasks()
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.nav_home_volunteer).setOnClickListener {
            val intent = Intent(this, VolunteerHomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        findViewById<LinearLayout>(R.id.nav_available_tasks).setOnClickListener {
            // Already here
        }

        findViewById<LinearLayout>(R.id.nav_active_tasks).setOnClickListener {
            startActivity(Intent(this, ActiveTasksActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.nav_profile_volunteer).setOnClickListener {
            startActivity(Intent(this, VolunteerProfileActivity::class.java))
            finish()
        }
    }

    private fun fetchAvailableTasks() {
        db.collection("requests")
            .whereEqualTo("status", "In Waiting Queue")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    DialogUtils.showErrorDialog(this, "Fetch Error", e.message ?: "Failed to load tasks.")
                    return@addSnapshotListener
                }

                taskList.clear()
                val docs = snapshot?.documents?.map { doc ->
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["docId"] = doc.id
                    data
                }?.sortedWith(Comparator { r1, r2 ->
                    val p1 = r1["priority"] as? String ?: ""
                    val p2 = r2["priority"] as? String ?: ""

                    val pLevel1 = if (p1.contains("P1")) 1 else if (p1.contains("P2")) 2 else if (p1.contains("P3")) 3 else 4
                    val pLevel2 = if (p2.contains("P1")) 1 else if (p2.contains("P2")) 2 else if (p2.contains("P3")) 3 else 4

                    if (pLevel1 != pLevel2) return@Comparator pLevel1 - pLevel2

                    val p1Subs = listOf("Life-Threatening", "Active Environmental Hazard", "Immediate Health Crisis",
                        "Major Structural Damage", "Resource/Utility Depletion", "Access & Extraction",
                        "Sanitation & Biohazard", "General Repair & Restoration", "Supply & Info Request")
                    
                    val sub1 = p1Subs.indexOfFirst { p1.contains(it) }.let { if (it == -1) 99 else it }
                    val sub2 = p1Subs.indexOfFirst { p2.contains(it) }.let { if (it == -1) 99 else it }

                    if (sub1 != sub2) return@Comparator sub1 - sub2

                    val t1 = r1["timestamp"] as? Long ?: 0L
                    val t2 = r2["timestamp"] as? Long ?: 0L
                    t1.compareTo(t2)
                })

                docs?.forEach { taskList.add(it) }
                adapter.notifyDataSetChanged()
            }
    }

    private fun acceptTask(docId: String) {
        val userEmail = getSharedPreferences("OnServePrefs", MODE_PRIVATE).getString("USER_EMAIL", "") ?: ""
        if (userEmail.isEmpty()) return

        val updates = mapOf(
            "status" to "In Progress",
            "volunteerEmail" to userEmail
        )

        db.collection("requests").document(docId).update(updates)
            .addOnSuccessListener {
                showSuccessDialog("Task accepted! You can now view it in Active Tasks.")
            }
            .addOnFailureListener { e ->
                DialogUtils.showErrorDialog(this, "Accept Error", e.message ?: "Failed to accept task.")
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

class AvailableTasksAdapter(
    private val items: List<Map<String, Any>>,
    private val onAcceptClick: (String) -> Unit
) : RecyclerView.Adapter<AvailableTasksAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvPriority: TextView = v.findViewById(R.id.tv_task_priority)
        val tvType: TextView = v.findViewById(R.id.tv_task_type)
        val tvDesc: TextView = v.findViewById(R.id.tv_task_desc)
        val tvSubmittedDate: TextView = v.findViewById(R.id.tv_task_submitted_date)
        val tvName: TextView = v.findViewById(R.id.tv_task_user_name)
        val tvLocation: TextView = v.findViewById(R.id.tv_task_location)
        val btnShowMore: TextView = v.findViewById(R.id.btn_task_show_more)
        val layoutDetails: View = v.findViewById(R.id.layout_task_details)
        val btnAccept: Button = v.findViewById(R.id.btn_accept_task)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_available_task, parent, false)
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

        holder.btnAccept.setOnClickListener {
            val docId = item["docId"] as? String
            if (docId != null) onAcceptClick(docId)
        }
    }

    override fun getItemCount() = items.size
}
