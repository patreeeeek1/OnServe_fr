package com.example.onserve

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class VolunteerHomeActivity : AppCompatActivity() {

    private var connectionListener: ListenerRegistration? = null
    private lateinit var connectionDot: View
    private lateinit var tvConnectionStatus: TextView
    
    private lateinit var tvSelectedDate: TextView
    private lateinit var btnSave: Button
    private lateinit var rvSlots: RecyclerView
    private lateinit var tvSlotsCount: TextView
    
    private val calendar = Calendar.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    private val selectedSlots = mutableSetOf<Int>() // Hour of day (8-19)
    private val lockedSlots = mutableSetOf<Int>() // Slots that have assigned tasks
    private val allSlots = (8..19).toList() // 8 AM to 7 PM start times

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_volunteer_home)

        connectionDot = findViewById(R.id.view_connection_dot)
        tvConnectionStatus = findViewById(R.id.tv_connection_status)
        
        tvSelectedDate = findViewById(R.id.tv_selected_date)
        tvSlotsCount = findViewById(R.id.tv_slots_count)
        btnSave = findViewById(R.id.btn_save_availability)

        rvSlots = findViewById(R.id.rv_time_slots)
        rvSlots.layoutManager = GridLayoutManager(this, 3)
        setupSlotsAdapter()

        startMonitoringConnection()

        tvSelectedDate.setOnClickListener { showDatePicker() }
        btnSave.setOnClickListener { saveAvailability() }

        setupBottomNavigation()
    }

    private fun setupSlotsAdapter() {
        rvSlots.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_time_slot, parent, false)
                return object : RecyclerView.ViewHolder(v) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val hour = allSlots[position]
                val tv = holder.itemView.findViewById<TextView>(R.id.tv_slot_time)
                
                val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 0) }
                val start = SimpleDateFormat("hh:00 a", Locale.getDefault()).format(cal.time)
                cal.add(Calendar.HOUR_OF_DAY, 1)
                val end = SimpleDateFormat("hh:00 a", Locale.getDefault()).format(cal.time)
                
                tv.text = "$start - $end"

                val isLocked = lockedSlots.contains(hour)
                
                if (isLocked) {
                    tv.setBackgroundResource(R.drawable.card_selection_bg) // Or a different "locked" drawable if available
                    tv.alpha = 0.5f
                    tv.text = "$start - $end (Assigned)"
                } else if (selectedSlots.contains(hour)) {
                    tv.setBackgroundResource(R.drawable.card_selected_highlight)
                    tv.alpha = 1.0f
                } else {
                    tv.setBackgroundResource(R.drawable.card_selection_bg)
                    tv.alpha = 1.0f
                }

                tv.setOnClickListener {
                    if (isLocked) {
                        Toast.makeText(this@VolunteerHomeActivity, "This slot is locked due to an assigned task.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    if (selectedSlots.contains(hour)) {
                        selectedSlots.remove(hour)
                    } else {
                        if (selectedSlots.size >= 5) {
                            Toast.makeText(this@VolunteerHomeActivity, "Maximum 5 slots allowed", Toast.LENGTH_SHORT).show()
                        } else {
                            selectedSlots.add(hour)
                        }
                    }
                    tvSlotsCount.text = "Selected: ${selectedSlots.size}/5 slots"
                    notifyItemChanged(position)
                }
            }

            override fun getItemCount() = allSlots.size
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(this, { _, year, month, day ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.format(calendar.time)
            tvSelectedDate.text = date
            fetchExistingAvailability(date)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun fetchExistingAvailability(date: String) {
        val email = getSharedPreferences("OnServePrefs", MODE_PRIVATE).getString("USER_EMAIL", "") ?: ""
        if (email.isEmpty()) return

        // 1. Fetch assigned tasks to lock those slots
        db.collection("requests")
            .whereEqualTo("volunteerEmail", email)
            .whereEqualTo("scheduledDate", date)
            .whereIn("status", listOf("Assigned", "In Progress"))
            .get()
            .addOnSuccessListener { tasks ->
                lockedSlots.clear()
                tasks.documents.forEach { doc ->
                    val hour = doc.getLong("slotHour")?.toInt()
                    if (hour != null) lockedSlots.add(hour)
                }
                
                // 2. Fetch volunteer's intended availability
                db.collection("volunteer_availability").document("${email}_${date}").get()
                    .addOnSuccessListener { doc ->
                        selectedSlots.clear()
                        if (doc.exists()) {
                            val slots = doc.get("slots") as? List<Long>
                            slots?.forEach { selectedSlots.add(it.toInt()) }
                        }
                        // Ensure locked slots are also shown as selected
                        selectedSlots.addAll(lockedSlots)
                        
                        tvSlotsCount.text = "Selected: ${selectedSlots.size}/5 slots"
                        rvSlots.adapter?.notifyDataSetChanged()
                    }
            }
    }

    private fun saveAvailability() {
        val date = tvSelectedDate.text.toString()
        val email = getSharedPreferences("OnServePrefs", MODE_PRIVATE).getString("USER_EMAIL", "") ?: ""

        if (date == "Choose a date" || email.isEmpty()) {
            Toast.makeText(this, "Please select a valid date", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedSlots.isEmpty()) {
            Toast.makeText(this, "Please select at least one time slot", Toast.LENGTH_SHORT).show()
            return
        }

        val data = hashMapOf(
            "email" to email,
            "date" to date,
            "slots" to selectedSlots.toList().sorted(),
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("volunteer_availability").document("${email}_${date}")
            .set(data)
            .addOnSuccessListener {
                DialogUtils.showSuccessDialog(this, "Availability updated for $date!")
                // Trigger queue check for each slot
                selectedSlots.forEach { hour ->
                    checkWaitingQueueForSlot(email, date, hour)
                }
            }
            .addOnFailureListener { e ->
                DialogUtils.showErrorDialog(this, "Save Error", e.message ?: "Failed to save.")
            }
    }

    private fun checkWaitingQueueForSlot(email: String, date: String, hour: Int) {
        // Check current workload for this specific slot (max 1 task per 1-hour slot)
        db.collection("requests")
            .whereEqualTo("volunteerEmail", email)
            .whereEqualTo("scheduledDate", date)
            .whereEqualTo("slotHour", hour)
            .get()
            .addOnSuccessListener { existing ->
                if (existing.isEmpty) {
                    // Try to find a task from waiting queue that fits this slot
                    db.collection("users").document(email).get().addOnSuccessListener { userDoc ->
                        val expertise = userDoc.getString("expertise")?.uppercase() ?: ""
                        
                        db.collection("requests")
                            .whereEqualTo("status", "In Waiting Queue")
                            .get()
                            .addOnSuccessListener { waitingDocs ->
                                val potentialTasks = waitingDocs.documents.filter { doc ->
                                    val reqType = doc.getString("type")?.uppercase() ?: ""
                                    val reqDate = doc.getString("date") ?: ""
                                    val reqMinutes = doc.getLong("timeMinutes")?.toInt() ?: -1
                                    
                                    var formattedReqDate = reqDate
                                    try {
                                        val displayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                        val searchFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                        val d = displayFormat.parse(reqDate)
                                        if (d != null) formattedReqDate = searchFormat.format(d)
                                    } catch (e: Exception) {}

                                    val dateMatch = formattedReqDate == date
                                    val expertiseMatch = expertise.contains(reqType)
                                    // Time match: if reqMinutes falls into this 1-hour window
                                    val timeMatch = if (reqMinutes != -1) (reqMinutes / 60) == hour else true
                                    
                                    dateMatch && expertiseMatch && timeMatch
                                }.sortedWith(compareByDescending<com.google.firebase.firestore.DocumentSnapshot> { it.getBoolean("emergency") ?: false }
                                    .thenBy { it.getString("priority") ?: "" }
                                    .thenBy { it.getLong("timestamp") ?: 0L })

                                if (potentialTasks.isNotEmpty()) {
                                    val task = potentialTasks[0]
                                    val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 0) }
                                    val startTime = SimpleDateFormat("hh:00 a", Locale.getDefault()).format(cal.time)
                                    cal.add(Calendar.HOUR_OF_DAY, 1)
                                    val endTime = SimpleDateFormat("hh:00 a", Locale.getDefault()).format(cal.time)
                                    val slotRange = "$startTime - $endTime"

                                    val updates = mapOf(
                                        "volunteerEmail" to email,
                                        "status" to "Assigned",
                                        "scheduledDate" to date,
                                        "scheduledTime" to slotRange,
                                        "slotHour" to hour
                                    )
                                    db.collection("requests").document(task.id).update(updates)
                                }
                            }
                    }
                }
            }
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.nav_availability).setOnClickListener {
            // Already here
        }

        findViewById<LinearLayout>(R.id.nav_assignment_schedule).setOnClickListener {
            startActivity(Intent(this, AssignmentScheduleActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.nav_profile_volunteer).setOnClickListener {
            startActivity(Intent(this, VolunteerProfileActivity::class.java))
        }
    }

    private fun startMonitoringConnection() {
        val email = getSharedPreferences("OnServePrefs", MODE_PRIVATE).getString("USER_EMAIL", "")
        if (email.isNullOrEmpty()) return

        val docRef = db.collection("users").document(email)
        connectionListener = docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                updateConnectionUI(false)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val isConnected = !snapshot.metadata.isFromCache
                updateConnectionUI(isConnected)
            }
        }
    }

    private fun updateConnectionUI(isOnline: Boolean) {
        if (isOnline) {
            connectionDot.setBackgroundResource(R.drawable.dot_online)
            tvConnectionStatus.text = "Connected"
        } else {
            connectionDot.setBackgroundResource(R.drawable.dot_offline)
            tvConnectionStatus.text = "Offline"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        connectionListener?.remove()
    }
}
