package com.example.onserve

import android.app.DatePickerDialog
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

class VolunteerManageScheduleActivity : AppCompatActivity() {

    private lateinit var tvSelectedDate: TextView
    private lateinit var rvSchedule: RecyclerView
    private lateinit var tvNoAvailability: TextView
    private val db = FirebaseFirestore.getInstance()
    private val calendar = Calendar.getInstance()
    private val scheduleItems = mutableListOf<ScheduleItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_volunteer_manage_schedule)

        tvSelectedDate = findViewById(R.id.tv_manage_selected_date)
        rvSchedule = findViewById(R.id.rv_manage_schedule)
        tvNoAvailability = findViewById(R.id.tv_no_availability)

        rvSchedule.layoutManager = LinearLayoutManager(this)
        rvSchedule.adapter = ManageScheduleAdapter(scheduleItems)

        tvSelectedDate.setOnClickListener { showDatePicker() }

        // Set default date to today
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        tvSelectedDate.text = sdf.format(Date())
        fetchSchedule(tvSelectedDate.text.toString())

        setupBottomNavigation()
    }

    private fun showDatePicker() {
        DatePickerDialog(this, { _, year, month, day ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.format(calendar.time)
            tvSelectedDate.text = date
            fetchSchedule(date)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun fetchSchedule(date: String) {
        val email = getSharedPreferences("OnServePrefs", MODE_PRIVATE).getString("USER_EMAIL", "") ?: ""
        if (email.isEmpty()) return

        // Fetch availability
        db.collection("volunteer_availability").document("${email}_$date").get()
            .addOnSuccessListener { availDoc ->
                val slots = availDoc.get("slots") as? List<Long> ?: emptyList()
                
                // Fetch tasks
                db.collection("requests")
                    .whereEqualTo("volunteerEmail", email)
                    .whereEqualTo("scheduledDate", date)
                    .get()
                    .addOnSuccessListener { taskDocs ->
                        val tasksByHour = taskDocs.documents.associateBy { it.getLong("slotHour")?.toInt() ?: -1 }
                        
                        scheduleItems.clear()
                        slots.forEach { slotLong ->
                            val hour = slotLong.toInt()
                            val taskDoc = tasksByHour[hour]
                            
                            val item = if (taskDoc != null) {
                                ScheduleItem(
                                    hour = hour,
                                    task = taskDoc.getString("type") ?: "Task",
                                    location = taskDoc.getString("location") ?: "N/A",
                                    userName = taskDoc.getString("userName") ?: "N/A",
                                    description = taskDoc.getString("description") ?: "N/A",
                                    hasTask = true
                                )
                            } else {
                                ScheduleItem(hour = hour, task = "None", hasTask = false)
                            }
                            
                            scheduleItems.add(item)
                        }
                        
                        if (scheduleItems.isEmpty()) {
                            tvNoAvailability.visibility = View.VISIBLE
                            rvSchedule.visibility = View.GONE
                        } else {
                            tvNoAvailability.visibility = View.GONE
                            rvSchedule.visibility = View.VISIBLE
                            scheduleItems.sortBy { it.hour }
                        }
                        rvSchedule.adapter?.notifyDataSetChanged()
                    }
            }
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.nav_availability).setOnClickListener {
            val intent = Intent(this, VolunteerHomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_assignment_schedule).setOnClickListener {
            startActivity(Intent(this, AssignmentScheduleActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_manage_schedule).setOnClickListener {
            // Already here
        }
        findViewById<LinearLayout>(R.id.nav_volunteer_history).setOnClickListener {
            startActivity(Intent(this, VolunteerHistoryActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.nav_notifications_volunteer).setOnClickListener {
            startActivity(Intent(this, NotificationCenterActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.nav_profile_volunteer).setOnClickListener {
            startActivity(Intent(this, VolunteerProfileActivity::class.java))
            finish()
        }
    }

    data class ScheduleItem(
        val hour: Int,
        val task: String,
        val location: String = "",
        val userName: String = "",
        val description: String = "",
        val hasTask: Boolean = false
    )

    class ManageScheduleAdapter(private val items: List<ScheduleItem>) : RecyclerView.Adapter<ManageScheduleAdapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvTime: TextView = v.findViewById(R.id.tv_manage_time)
            val tvStatus: TextView = v.findViewById(R.id.tv_manage_status)
            val tvTask: TextView = v.findViewById(R.id.tv_manage_task)
            val btnReadMore: TextView = v.findViewById(R.id.btn_manage_read_more)
            val layoutDetails: View = v.findViewById(R.id.layout_manage_details)
            val tvLocation: TextView = v.findViewById(R.id.tv_manage_location)
            val tvUserName: TextView = v.findViewById(R.id.tv_manage_user_name)
            val tvDesc: TextView = v.findViewById(R.id.tv_manage_desc)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_manage_schedule, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            val cal = Calendar.getInstance().apply { 
                set(Calendar.HOUR_OF_DAY, item.hour)
                set(Calendar.MINUTE, 0) 
            }
            val start = SimpleDateFormat("hh:00 a", Locale.getDefault()).format(cal.time)
            cal.add(Calendar.HOUR_OF_DAY, 1)
            val end = SimpleDateFormat("hh:00 a", Locale.getDefault()).format(cal.time)
            
            holder.tvTime.text = "$start - $end"
            holder.tvStatus.text = if (!item.hasTask) "Available" else "Assigned"
            holder.tvTask.text = "Task: ${item.task}"

            if (item.hasTask) {
                holder.btnReadMore.visibility = View.VISIBLE
                holder.tvLocation.text = item.location
                holder.tvUserName.text = item.userName
                holder.tvDesc.text = item.description

                holder.btnReadMore.setOnClickListener {
                    val isVisible = holder.layoutDetails.visibility == View.VISIBLE
                    holder.layoutDetails.visibility = if (isVisible) View.GONE else View.VISIBLE
                    holder.btnReadMore.text = if (isVisible) "Read more" else "Read less"
                }
            } else {
                holder.btnReadMore.visibility = View.GONE
                holder.layoutDetails.visibility = View.GONE
            }
        }

        override fun getItemCount() = items.size
    }
}
