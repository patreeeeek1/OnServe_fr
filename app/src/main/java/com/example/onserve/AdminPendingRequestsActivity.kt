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
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class AdminPendingRequestsActivity : AppCompatActivity() {

    private lateinit var rvPending: RecyclerView
    private lateinit var adapter: AdminPendingAdapter
    private val db = FirebaseFirestore.getInstance()
    private val pendingList = mutableListOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_pending_requests)

        findViewById<FrameLayout>(R.id.btn_back_pending).setOnClickListener { finish() }

        rvPending = findViewById(R.id.rv_admin_pending)
        rvPending.layoutManager = LinearLayoutManager(this)
        adapter = AdminPendingAdapter(pendingList, { docId, priority, expertise ->
            confirmBooking(docId, priority, expertise)
        }, { docId ->
            showDeleteConfirmation(docId)
        })
        rvPending.adapter = adapter

        fetchPendingRequests()
    }

    private fun showDeleteConfirmation(docId: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Request")
            .setMessage("Are you sure you want to remove this request?")
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
                DialogUtils.showSuccessDialog(this, "Request deleted successfully.")
            }
            .addOnFailureListener { e ->
                DialogUtils.showErrorDialog(this, "Delete Error", e.message ?: "Failed to delete request.")
            }
    }

    private fun fetchPendingRequests() {
        db.collection("requests")
            .whereEqualTo("status", "Pending Approval")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                pendingList.clear()
                val docs = snapshot?.documents?.map { doc ->
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["docId"] = doc.id
                    data
                }?.sortedWith(compareByDescending<MutableMap<String, Any>> { it["emergency"] as? Boolean ?: false }
                    .thenBy { it["timestamp"] as? Long ?: 0L })

                docs?.forEach { pendingList.add(it) }
                adapter.notifyDataSetChanged()
            }
    }

    private fun confirmBooking(docId: String, priority: String, expertiseFilter: String) {
        db.collection("requests").document(docId).get().addOnSuccessListener { snapshot ->
            val requestType = snapshot.getString("type") ?: ""
            val requestDate = snapshot.getString("date") ?: ""
            val requestTimeStr = snapshot.getString("time") ?: ""
            val requestTimeMinutes = snapshot.getLong("timeMinutes")?.toInt() ?: -1

            db.collection("users")
                .whereEqualTo("type", "Volunteer")
                .get()
                .addOnSuccessListener { volunteers ->
                    val matchingVolunteers = volunteers.documents.filter { doc ->
                        val expertise = doc.getString("expertise") ?: ""
                        if (expertiseFilter == "Default") {
                            isQualified(expertise, requestType)
                        } else {
                            expertise.uppercase(Locale.getDefault()).contains(expertiseFilter.uppercase(Locale.getDefault()))
                        }
                    }

                    if (matchingVolunteers.isEmpty()) {
                        updateRequestStatus(docId, priority, "In Waiting Queue", null, null, null, -1)
                    } else {
                        var searchDate = requestDate
                        try {
                            val displayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            val searchFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val d = displayFormat.parse(requestDate)
                            if (d != null) searchDate = searchFormat.format(d)
                        } catch (e: Exception) {
                            searchDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        }
                        findAvailableVolunteer(matchingVolunteers, docId, priority, searchDate, requestTimeMinutes, requestTimeStr, 0)
                    }
                }
        }
    }

    private fun isQualified(volunteerExpertise: String, requestType: String): Boolean {
        val vExp = volunteerExpertise.uppercase(Locale.getDefault())
        val rType = requestType.uppercase(Locale.getDefault())
        
        if (vExp.contains(rType)) return true
        
        return when (rType) {
            "FIRE" -> vExp.contains("FIRE SAFETY") || vExp.contains("HAZMAT")
            "RESCUE" -> vExp.contains("SEARCH & RESCUE")
            "MEDICAL" -> vExp.contains("MEDICAL PROFESSIONAL")
            "ELECTRICAL" -> vExp.contains("ELECTRICAL")
            "PLUMBING" -> vExp.contains("PLUMBING")
            "STRUCTURAL" -> vExp.contains("STRUCTURAL ENGINEERING")
            "ANIMAL" -> vExp.contains("ANIMAL")
            "WELFARE" -> vExp.contains("FIRST AID")
            "FOOD" -> vExp.contains("DELIVERY & DISTRIBUTION")
            "TRANSPORT" -> vExp.contains("VEHICLE TRANSPORT")
            "WASTE" -> vExp.contains("DEBRIS CLEANING")
            "INFO" -> vExp.contains("COMMUNICATIONS")
            else -> vExp.contains(rType) || rType == "OTHER"
        }
    }

    private fun findAvailableVolunteer(
        volunteers: List<com.google.firebase.firestore.DocumentSnapshot>,
        docId: String,
        priority: String,
        date: String,
        reqMinutes: Int,
        reqTimeStr: String,
        index: Int
    ) {
        val isEmergency = priority.startsWith("P1")

        if (isEmergency) {
            // For Emergencies: Find the earliest possible slot (empty or pre-emptible) across ALL volunteers
            findEarliestSlotAcrossAll(volunteers, docId, priority, date, reqMinutes)
        } else {
            // For Routine/Urgent: Traditional +/- 45 mins logic
            if (index >= volunteers.size) {
                updateRequestStatus(docId, priority, "In Waiting Queue", null, null, null, -1)
                return
            }

            val volunteerDoc = volunteers[index]
            val volunteerEmail = volunteerDoc.id
            val expertise = volunteerDoc.getString("expertise") ?: ""
            
            db.collection("requests").document(docId).get().addOnSuccessListener { reqSnap ->
                val reqType = reqSnap.getString("type") ?: ""
                if (!isQualified(expertise, reqType)) {
                    findAvailableVolunteer(volunteers, docId, priority, date, reqMinutes, reqTimeStr, index + 1)
                    return@addOnSuccessListener
                }

                db.collection("volunteer_availability").document("${volunteerEmail}_$date").get()
                    .addOnSuccessListener { avail ->
                        if (avail.exists()) {
                            val slots = (avail.get("slots") as? List<Long> ?: emptyList()).map { it.toInt() }.sorted()
                            if (slots.isEmpty()) {
                                findAvailableVolunteer(volunteers, docId, priority, date, reqMinutes, reqTimeStr, index + 1)
                                return@addOnSuccessListener
                            }

                            val matchedSlot = slots.find { hour ->
                                val slotStart = hour * 60
                                val slotEnd = (hour + 1) * 60
                                if (reqMinutes == -1) true
                                else {
                                    val isInside = reqMinutes in slotStart..slotEnd
                                    val nearStart = Math.abs(reqMinutes - slotStart) <= 45
                                    val nearEnd = Math.abs(reqMinutes - slotEnd) <= 45
                                    isInside || nearStart || nearEnd
                                }
                            }

                            if (matchedSlot != null) {
                                val targetedHour = matchedSlot
                                db.collection("requests")
                                    .whereEqualTo("volunteerEmail", volunteerEmail)
                                    .whereEqualTo("scheduledDate", date)
                                    .whereEqualTo("slotHour", targetedHour)
                                    .get()
                                    .addOnSuccessListener { tasks ->
                                        if (tasks.isEmpty) {
                                            assignToVolunteer(docId, priority, volunteerEmail, date, targetedHour, "")
                                        } else {
                                            findAvailableVolunteer(volunteers, docId, priority, date, reqMinutes, reqTimeStr, index + 1)
                                        }
                                    }
                            } else {
                                findAvailableVolunteer(volunteers, docId, priority, date, reqMinutes, reqTimeStr, index + 1)
                            }
                        } else {
                            findAvailableVolunteer(volunteers, docId, priority, date, reqMinutes, reqTimeStr, index + 1)
                        }
                    }
            }
        }
    }

    private fun findEarliestSlotAcrossAll(
        volunteers: List<com.google.firebase.firestore.DocumentSnapshot>,
        docId: String,
        priority: String,
        baseDate: String,
        reqMinutes: Int
    ) {
        val allOptions = mutableListOf<PotentialSlot>()
        val datesToCheck = mutableListOf<String>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        try {
            val startD = sdf.parse(baseDate) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = startD
            // Look ahead up to 3 days for emergencies
            for (i in 0..2) {
                datesToCheck.add(sdf.format(cal.time))
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        } catch (e: Exception) {
            datesToCheck.add(baseDate)
        }

        var totalExpected = volunteers.size * datesToCheck.size
        var processedCount = 0

        if (volunteers.isEmpty()) {
            updateRequestStatus(docId, priority, "In Waiting Queue", null, null, null, -1)
            return
        }

        volunteers.forEach { vDoc ->
            val email = vDoc.id
            datesToCheck.forEach { date ->
                db.collection("volunteer_availability").document("${email}_$date").get()
                    .addOnSuccessListener { avail ->
                        if (avail.exists()) {
                            val slots = (avail.get("slots") as? List<Long> ?: emptyList()).map { it.toInt() }
                            
                            db.collection("requests")
                                .whereEqualTo("volunteerEmail", email)
                                .whereEqualTo("scheduledDate", date)
                                .get()
                                .addOnSuccessListener { tasks ->
                                    val taskMap = tasks.documents.associateBy { it.getLong("slotHour")?.toInt() ?: -1 }
                                    
                                    slots.forEach { hour ->
                                        val task = taskMap[hour]
                                        if (task == null) {
                                            allOptions.add(PotentialSlot(hour, email, null, date))
                                        } else {
                                            val existingP = task.getString("priority") ?: "P3"
                                            if (!existingP.contains("P1")) {
                                                allOptions.add(PotentialSlot(hour, email, task.id, date))
                                            }
                                        }
                                    }
                                    
                                    processedCount++
                                    if (processedCount == totalExpected) {
                                        finalizeEarliestAssignment(allOptions, docId, priority, reqMinutes)
                                    }
                                }
                                .addOnFailureListener {
                                    processedCount++
                                    if (processedCount == totalExpected) finalizeEarliestAssignment(allOptions, docId, priority, reqMinutes)
                                }
                        } else {
                            processedCount++
                            if (processedCount == totalExpected) {
                                finalizeEarliestAssignment(allOptions, docId, priority, reqMinutes)
                            }
                        }
                    }
                    .addOnFailureListener {
                        processedCount++
                        if (processedCount == totalExpected) finalizeEarliestAssignment(allOptions, docId, priority, reqMinutes)
                    }
            }
        }
    }

    private data class PotentialSlot(val hour: Int, val email: String, val preEmptDocId: String?, val date: String)

    private fun finalizeEarliestAssignment(options: List<PotentialSlot>, docId: String, priority: String, reqMinutes: Int) {
        if (options.isEmpty()) {
            updateRequestStatus(docId, priority, "In Waiting Queue", null, null, null, -1)
            return
        }

        // Sort by date then hour (earliest first), then prefer empty slots over pre-emptible ones
        val sorted = options.sortedWith(compareBy({ it.date }, { it.hour }, { it.preEmptDocId != null }))
        val best = sorted[0]

        if (best.preEmptDocId != null) {
            preemptTask(best.preEmptDocId, docId, priority, best.email, best.date, best.hour, "")
        } else {
            assignToVolunteer(docId, priority, best.email, best.date, best.hour, "")
        }
    }

    private fun preemptTask(replacedDocId: String, newDocId: String, newPriority: String, volunteerEmail: String, date: String, hour: Int, reqTimeStr: String) {
        val reQueueUpdates = mapOf(
            "status" to "In Waiting Queue",
            "volunteerEmail" to com.google.firebase.firestore.FieldValue.delete(),
            "volunteerName" to com.google.firebase.firestore.FieldValue.delete(),
            "scheduledDate" to com.google.firebase.firestore.FieldValue.delete(),
            "scheduledTime" to com.google.firebase.firestore.FieldValue.delete(),
            "slotHour" to -1
        )
        db.collection("requests").document(replacedDocId).update(reQueueUpdates).addOnSuccessListener {
            db.collection("requests").document(replacedDocId).get().addOnSuccessListener { snapshot ->
                val userEmail = snapshot.getString("userEmail") ?: ""
                if (userEmail.isNotEmpty()) NotificationUtils.sendNotification(userEmail, "Request Rescheduled", "An emergency task has taken priority. Your request has been re-queued.")
            }
            assignToVolunteer(newDocId, newPriority, volunteerEmail, date, hour, reqTimeStr)
        }
    }

    private fun assignToVolunteer(docId: String, priority: String, volunteerEmail: String, date: String, hour: Int, reqTimeStr: String) {
        db.collection("users").document(volunteerEmail).get().addOnSuccessListener { userDoc ->
            val volunteerName = userDoc.getString("name") ?: "a Volunteer"
            val slotRange = getDefaultSlotRange(hour)
            updateRequestStatus(docId, priority, "Assigned", volunteerEmail, date, slotRange, hour, volunteerName)
        }
    }

    private fun getDefaultSlotRange(hour: Int): String {
        val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 0) }
        val sdf = SimpleDateFormat("hh:00 a", Locale.getDefault())
        val start = sdf.format(cal.time)
        cal.add(Calendar.HOUR_OF_DAY, 1)
        val end = sdf.format(cal.time)
        return "$start - $end"
    }

    private fun updateRequestStatus(docId: String, priority: String, status: String, volunteerEmail: String?, date: String?, time: String?, slotHour: Int, volunteerName: String? = null) {
        val updates = mutableMapOf<String, Any>("status" to status, "priority" to priority)
        if (volunteerEmail != null) updates["volunteerEmail"] = volunteerEmail
        if (volunteerName != null) updates["volunteerName"] = volunteerName
        if (date != null) updates["scheduledDate"] = date
        if (time != null) updates["scheduledTime"] = time
        if (slotHour != -1) updates["slotHour"] = slotHour

        db.collection("requests").document(docId).update(updates).addOnSuccessListener {
            val msg = if (volunteerName != null) "Assigned to $volunteerName" else "Confirmed as $priority"
            db.collection("requests").document(docId).get().addOnSuccessListener { snapshot ->
                val userEmail = snapshot.getString("userEmail") ?: ""
                if (userEmail.isNotEmpty()) {
                    val title = if (status == "Assigned") "Volunteer Assigned" else "Request Status Updated"
                    val body = if (status == "Assigned") "$volunteerName has been assigned to your request for $time." else "Your request status is now: $status"
                    NotificationUtils.sendNotification(userEmail, title, body)
                }
                if (volunteerEmail != null) NotificationUtils.sendNotification(volunteerEmail, "New Task Assigned", "You have been assigned a new task: ${snapshot.getString("type")}")
            }
            showSuccessDialog(msg)
        }
    }

    private fun showSuccessDialog(message: String) {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_success)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.setCancelable(false)
        dialog.findViewById<TextView>(R.id.tv_success_message).text = message
        dialog.findViewById<Button>(R.id.btn_success_ok).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}

class AdminPendingAdapter(
    private val items: List<Map<String, Any>>,
    private val onConfirm: (String, String, String) -> Unit,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<AdminPendingAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvType: TextView = v.findViewById(R.id.tv_admin_req_type)
        val tvUser: TextView = v.findViewById(R.id.tv_admin_req_user)
        val tvDesc: TextView = v.findViewById(R.id.tv_admin_req_desc)
        val tvBookedTime: TextView = v.findViewById(R.id.tv_admin_req_booked_time)
        val labelEmergency: TextView = v.findViewById(R.id.tv_admin_emergency_label)
        val rgPriority: RadioGroup = v.findViewById(R.id.rg_priority)
        val btnConfirm: Button = v.findViewById(R.id.btn_confirm_booking)
        val btnDelete: ImageView = v.findViewById(R.id.btn_delete_request)
        val layoutSubPriority: View = v.findViewById(R.id.layout_sub_priority)
        val spinnerSubPriority: Spinner = v.findViewById(R.id.spinner_sub_priority)
        val spinnerExpertise: Spinner = v.findViewById(R.id.spinner_expertise)
        
        val tvSubmittedDate: TextView = v.findViewById(R.id.tv_admin_submitted_date)
        val tvName: TextView = v.findViewById(R.id.tv_admin_req_name)
        val tvEmail: TextView = v.findViewById(R.id.tv_admin_req_email)
        val tvPhone: TextView = v.findViewById(R.id.tv_admin_req_phone)
        val btnShowMore: TextView = v.findViewById(R.id.btn_admin_show_more)
        val layoutDetails: View = v.findViewById(R.id.layout_admin_details)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_pending_request, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvType.text = item["type"] as? String
        holder.tvUser.text = "User: ${item["userEmail"]}"
        holder.tvDesc.text = item["description"] as? String
        
        val bookedDate = item["date"] as? String ?: "ASAP"
        val bookedTime = item["time"] as? String ?: ""
        holder.tvBookedTime.text = "Booked: $bookedDate $bookedTime"

        val isEmergency = item["emergency"] as? Boolean ?: false
        holder.labelEmergency.visibility = if (isEmergency) View.VISIBLE else View.GONE

        // Setup Expertise Spinner
        val expertises = listOf("Default", "Electrical", "Plumbing", "IT Support", "First Aid", "Medical Professional", "Manual Labor", "Vehicle Transport", "Delivery & Distribution", "Debris Cleaning", "Communications", "Fire Safety", "Search & Rescue", "Counseling", "Animal Handling", "Security")
        val expAdapter = ArrayAdapter(holder.itemView.context, android.R.layout.simple_spinner_dropdown_item, expertises)
        holder.spinnerExpertise.adapter = expAdapter

        val p1Subs = listOf("Life-Threatening", "Active Environmental Hazard", "Immediate Health Crisis")
        val p2Subs = listOf("Major Structural Damage", "Resource/Utility Depletion", "Access & Extraction")
        val p3Subs = listOf("Sanitation & Biohazard", "General Repair & Restoration", "Supply & Info Request")

        holder.rgPriority.setOnCheckedChangeListener { group, checkedId ->
            holder.layoutSubPriority.visibility = View.VISIBLE
            for (i in 0 until group.childCount) {
                val child = group.getChildAt(i)
                if (child is RadioButton) {
                    child.setTextColor(android.graphics.Color.BLACK)
                }
            }

            val subs = when (checkedId) {
                R.id.rb_p1 -> p1Subs
                R.id.rb_p2 -> p2Subs
                R.id.rb_p3 -> p3Subs
                else -> emptyList()
            }
            holder.spinnerSubPriority.adapter = ArrayAdapter(holder.itemView.context, android.R.layout.simple_spinner_dropdown_item, subs)
        }

        val ts = item["timestamp"] as? Long ?: 0L
        holder.tvSubmittedDate.text = if (ts > 0) SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date(ts)) else "N/A"
        holder.tvName.text = item["userName"] as? String ?: "N/A"
        holder.tvEmail.text = item["userEmail"] as? String ?: "N/A"
        holder.tvPhone.text = item["phoneNumber"] as? String ?: "N/A"

        holder.btnShowMore.setOnClickListener {
            val isVisible = holder.layoutDetails.visibility == View.VISIBLE
            holder.layoutDetails.visibility = if (isVisible) View.GONE else View.VISIBLE
            holder.btnShowMore.text = if (isVisible) "Show more" else "Show less"
        }

        holder.btnConfirm.setOnClickListener {
            val selectedId = holder.rgPriority.checkedRadioButtonId
            if (selectedId == -1) {
                DialogUtils.showErrorDialog(holder.itemView.context as AppCompatActivity, "Priority Required", "Please select a priority level.")
                return@setOnClickListener
            }

            val priority = when (selectedId) {
                R.id.rb_p1 -> "P1: Emergency"
                R.id.rb_p2 -> "P2: Urgent"
                else -> "P3: Routine"
            }
            val subPriority = holder.spinnerSubPriority.selectedItem as? String ?: ""
            val finalPriority = if (subPriority.isNotEmpty()) "$priority ($subPriority)" else priority
            val selectedExpertise = holder.spinnerExpertise.selectedItem as? String ?: "Default"

            onConfirm(item["docId"] as String, finalPriority, selectedExpertise)
        }

        holder.btnDelete.setOnClickListener { onDelete(item["docId"] as String) }
    }

    override fun getItemCount() = items.size
}
