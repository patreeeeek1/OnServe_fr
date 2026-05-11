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
import java.text.SimpleDateFormat
import java.util.*

class AssignmentScheduleActivity : AppCompatActivity() {

    private lateinit var rvSchedule: RecyclerView
    private lateinit var tvTasksCount: TextView
    private lateinit var adapter: ScheduleAdapter
    private val db = FirebaseFirestore.getInstance()
    private val taskList = mutableListOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assignment_schedule)

        findViewById<FrameLayout>(R.id.btn_back_schedule).setOnClickListener { finish() }
        tvTasksCount = findViewById(R.id.tv_tasks_count)

        rvSchedule = findViewById(R.id.rv_assignment_schedule)
        rvSchedule.layoutManager = LinearLayoutManager(this)
        adapter = ScheduleAdapter(taskList) { docId ->
            // Mark as complete logic (optional here, but good for UX)
            completeTask(docId)
        }
        rvSchedule.adapter = adapter

        setupBottomNavigation()
        fetchAssignedTasks()
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.nav_availability).setOnClickListener {
            val intent = Intent(this, VolunteerHomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        findViewById<LinearLayout>(R.id.nav_assignment_schedule).setOnClickListener {
            // Already here
        }

        findViewById<LinearLayout>(R.id.nav_profile_volunteer).setOnClickListener {
            startActivity(Intent(this, VolunteerProfileActivity::class.java))
            finish()
        }
    }

    private fun fetchAssignedTasks() {
        val email = getSharedPreferences("OnServePrefs", MODE_PRIVATE).getString("USER_EMAIL", "") ?: ""
        if (email.isEmpty()) return

        // Fetching tasks assigned to this volunteer that are NOT done yet
        db.collection("requests")
            .whereEqualTo("volunteerEmail", email)
            .whereIn("status", listOf("Assigned", "In Progress"))
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                taskList.clear()
                var todayCount = 0
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                snapshot?.documents?.forEach { doc ->
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["docId"] = doc.id
                    taskList.add(data)

                    val scheduledDate = data["scheduledDate"] as? String
                    if (scheduledDate == todayStr) {
                        todayCount++
                    }
                }
                
                tvTasksCount.text = "Tasks assigned today: $todayCount / 5"
                
                // Sort by scheduled date and time if available
                taskList.sortBy { it["scheduledDate"] as? String ?: "" }
                
                adapter.notifyDataSetChanged()
            }
    }

    private fun completeTask(docId: String) {
        db.collection("requests").document(docId).update("status", "Done")
            .addOnSuccessListener {
                Toast.makeText(this, "Task completed!", Toast.LENGTH_SHORT).show()
            }
    }
}

class ScheduleAdapter(
    private val items: List<Map<String, Any>>,
    private val onCompleteClick: (String) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvPriority: TextView = v.findViewById(R.id.tv_task_priority)
        val tvType: TextView = v.findViewById(R.id.tv_task_type)
        val tvDesc: TextView = v.findViewById(R.id.tv_task_desc)
        val tvScheduledDate: TextView = v.findViewById(R.id.tv_task_submitted_date) // Reusing ID for simplicity
        val tvName: TextView = v.findViewById(R.id.tv_task_user_name)
        val tvLocation: TextView = v.findViewById(R.id.tv_task_location)
        val btnComplete: Button = v.findViewById(R.id.btn_accept_task) // Reusing ID, changing text
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_available_task, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvType.text = item["type"] as? String
        holder.tvDesc.text = item["description"] as? String
        holder.tvPriority.text = item["priority"] as? String ?: "No Priority"
        
        val date = item["scheduledDate"] as? String ?: "No date"
        val time = item["scheduledTime"] as? String ?: ""
        holder.tvScheduledDate.text = "Scheduled: $date $time"
        
        holder.tvName.text = item["userName"] as? String ?: "N/A"
        holder.tvLocation.text = item["location"] as? String ?: "N/A"
        
        holder.btnComplete.text = "Mark Done"
        holder.btnComplete.setOnClickListener {
            val docId = item["docId"] as? String
            if (docId != null) onCompleteClick(docId)
        }

        // Color coding priority
        val priority = item["priority"] as? String ?: ""
        when {
            priority.contains("P1") -> holder.tvPriority.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFC62828.toInt())
            priority.contains("P2") -> holder.tvPriority.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFFBF00.toInt())
            priority.contains("P3") -> holder.tvPriority.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2E7D32.toInt())
        }
    }

    override fun getItemCount() = items.size
}
