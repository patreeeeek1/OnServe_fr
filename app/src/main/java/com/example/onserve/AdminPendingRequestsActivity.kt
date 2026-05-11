package com.example.onserve

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        adapter = AdminPendingAdapter(pendingList, { docId, priority ->
            confirmBooking(docId, priority)
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
            .setPositiveButton("Yes") { dialog, which ->
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
        // Fetch all pending then sort manually to avoid index requirement
        db.collection("requests")
            .whereEqualTo("status", "Pending Approval")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    DialogUtils.showErrorDialog(this, "Fetch Error", e.message ?: "Failed to load pending requests.")
                    return@addSnapshotListener
                }

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

    private fun confirmBooking(docId: String, priority: String) {
        db.collection("requests").document(docId).get().addOnSuccessListener { snapshot ->
            val requestType = snapshot.getString("type") ?: ""
            val requestDate = snapshot.getString("date") ?: ""
            val requestTimeStr = snapshot.getString("time") ?: ""
            val requestTimeMinutes = snapshot.getLong("timeMinutes")?.toInt() ?: -1

            // Convert "MMM dd, yyyy" to "yyyy-MM-dd"
            var searchDate = requestDate
            try {
                val displayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val searchFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val d = displayFormat.parse(requestDate)
                if (d != null) searchDate = searchFormat.format(d)
            } catch (e: Exception) {
                // fallback to today if parsing fails
                searchDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            }

            val finalSearchDate = searchDate
            
            // 1. Find volunteers with matching expertise
            db.collection("users")
                .whereEqualTo("type", "Volunteer")
                .get()
                .addOnSuccessListener { volunteers ->
                    val matchingVolunteers = volunteers.documents.filter { doc ->
                        val expertise = doc.getString("expertise")?.uppercase(Locale.getDefault()) ?: ""
                        expertise.contains(requestType.uppercase(Locale.getDefault()))
                    }

                    if (matchingVolunteers.isEmpty()) {
                        updateRequestStatus(docId, priority, "In Waiting Queue", null, null, null, -1)
                    } else {
                        findAvailableVolunteer(matchingVolunteers, docId, priority, finalSearchDate, requestTimeMinutes, requestTimeStr, 0)
                    }
                }
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
        if (index >= volunteers.size) {
            updateRequestStatus(docId, priority, "In Waiting Queue", null, null, null, -1)
            return
        }

        val volunteerEmail = volunteers[index].id
        val reqHour = if (reqMinutes != -1) reqMinutes / 60 else -1
        
        db.collection("volunteer_availability").document("${volunteerEmail}_$date").get()
            .addOnSuccessListener { avail ->
                if (avail.exists()) {
                    val slots = avail.get("slots") as? List<Long> ?: emptyList()
                    
                    val isTimeOk = if (reqHour != -1) {
                        slots.contains(reqHour.toLong())
                    } else slots.isNotEmpty()

                    if (isTimeOk) {
                        val targetedHour = if (reqHour != -1) reqHour else slots[0].toInt()
                        
                        db.collection("requests")
                            .whereEqualTo("volunteerEmail", volunteerEmail)
                            .whereEqualTo("scheduledDate", date)
                            .whereEqualTo("slotHour", targetedHour)
                            .get()
                            .addOnSuccessListener { tasks ->
                                if (tasks.isEmpty) {
                                    val slotRange = if (reqTimeStr.isNotEmpty()) {
                                        try {
                                            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                            val d = sdf.parse(reqTimeStr)
                                            val cal = Calendar.getInstance().apply { time = d }
                                            val start = sdf.format(cal.time)
                                            cal.add(Calendar.HOUR_OF_DAY, 1)
                                            val end = sdf.format(cal.time)
                                            "$start - $end"
                                        } catch (e: Exception) {
                                            getDefaultSlotRange(targetedHour)
                                        }
                                    } else {
                                        getDefaultSlotRange(targetedHour)
                                    }
                                    
                                    updateRequestStatus(docId, priority, "Assigned", volunteerEmail, date, slotRange, targetedHour)
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

    private fun getDefaultSlotRange(hour: Int): String {
        val cal = Calendar.getInstance().apply { 
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0) 
        }
        val sdf = SimpleDateFormat("hh:00 a", Locale.getDefault())
        val start = sdf.format(cal.time)
        cal.add(Calendar.HOUR_OF_DAY, 1)
        val end = sdf.format(cal.time)
        return "$start - $end"
    }

    private fun updateRequestStatus(
        docId: String,
        priority: String,
        status: String,
        volunteerEmail: String?,
        date: String?,
        time: String?,
        slotHour: Int
    ) {
        val updates = mutableMapOf<String, Any>(
            "status" to status,
            "priority" to priority
        )
        if (volunteerEmail != null) updates["volunteerEmail"] = volunteerEmail
        if (date != null) updates["scheduledDate"] = date
        if (time != null) updates["scheduledTime"] = time
        if (slotHour != -1) updates["slotHour"] = slotHour

        db.collection("requests").document(docId).update(updates)
            .addOnSuccessListener {
                val msg = if (volunteerEmail != null) "Assigned to $volunteerEmail" else "Confirmed as $priority"
                showSuccessDialog(msg)
            }
            .addOnFailureListener { e ->
                DialogUtils.showErrorDialog(this, "Update Error", e.message ?: "Failed to update request.")
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

class AdminPendingAdapter(
    private val items: List<Map<String, Any>>,
    private val onConfirm: (String, String) -> Unit,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<AdminPendingAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvType: TextView = v.findViewById(R.id.tv_admin_req_type)
        val tvUser: TextView = v.findViewById(R.id.tv_admin_req_user)
        val tvDesc: TextView = v.findViewById(R.id.tv_admin_req_desc)
        val labelEmergency: TextView = v.findViewById(R.id.tv_admin_emergency_label)
        val rgPriority: RadioGroup = v.findViewById(R.id.rg_priority)
        val btnConfirm: Button = v.findViewById(R.id.btn_confirm_booking)
        val btnDelete: ImageView = v.findViewById(R.id.btn_delete_request)
        val layoutSubPriority: View = v.findViewById(R.id.layout_sub_priority)
        val spinnerSubPriority: Spinner = v.findViewById(R.id.spinner_sub_priority)
        
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
        
        val isEmergency = item["emergency"] as? Boolean ?: false
        holder.labelEmergency.visibility = if (isEmergency) View.VISIBLE else View.GONE

        val p1Subs = listOf("Life-Threatening", "Active Environmental Hazard", "Immediate Health Crisis")
        val p2Subs = listOf("Major Structural Damage", "Resource/Utility Depletion", "Access & Extraction")
        val p3Subs = listOf("Sanitation & Biohazard", "General Repair & Restoration", "Supply & Info Request")

        holder.rgPriority.setOnCheckedChangeListener { _, checkedId ->
            holder.layoutSubPriority.visibility = View.VISIBLE
            val subs = when (checkedId) {
                R.id.rb_p1 -> p1Subs
                R.id.rb_p2 -> p2Subs
                R.id.rb_p3 -> p3Subs
                else -> emptyList()
            }
            val adapter = ArrayAdapter(holder.itemView.context, android.R.layout.simple_spinner_dropdown_item, subs)
            holder.spinnerSubPriority.adapter = adapter
        }

        // Fill expandable details
        val ts = item["timestamp"] as? Long ?: 0L
        if (ts > 0) {
            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault())
            holder.tvSubmittedDate.text = sdf.format(java.util.Date(ts))
        } else {
            holder.tvSubmittedDate.text = "N/A"
        }

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
                R.id.rb_p3 -> "P3: Routine"
                else -> "P3: Routine"
            }
            
            val subPriority = holder.spinnerSubPriority.selectedItem as? String ?: ""
            val finalPriority = if (subPriority.isNotEmpty()) "$priority ($subPriority)" else priority

            onConfirm(item["docId"] as String, finalPriority)
        }

        holder.btnDelete.setOnClickListener {
            onDelete(item["docId"] as String)
        }
    }

    override fun getItemCount() = items.size
}
