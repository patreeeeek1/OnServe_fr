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

class AdminActiveRequestsActivity : AppCompatActivity() {

    private lateinit var rvActive: RecyclerView
    private lateinit var adapter: AdminActiveAdapter
    private val db = FirebaseFirestore.getInstance()
    private val activeList = mutableListOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_active_requests)

        findViewById<FrameLayout>(R.id.btn_back_active).setOnClickListener { finish() }

        rvActive = findViewById(R.id.rv_admin_active)
        rvActive.layoutManager = LinearLayoutManager(this)
        adapter = AdminActiveAdapter(activeList, { docId ->
            showDeleteConfirmation(docId)
        }, { item ->
            showReassignDialog(item)
        })
        rvActive.adapter = adapter

        fetchActiveRequests()
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

    private fun showReassignDialog(item: Map<String, Any>) {
        val docId = item["docId"] as? String ?: return
        val type = item["type"] as? String ?: ""
        
        db.collection("users").whereEqualTo("type", "Volunteer").get()
            .addOnSuccessListener { volunteers ->
                val matchingVolunteers = volunteers.documents.filter { doc ->
                    val expertise = doc.getString("expertise")?.uppercase() ?: ""
                    expertise.contains(type.uppercase())
                }

                if (matchingVolunteers.isEmpty()) {
                    Toast.makeText(this, "No volunteers with matching expertise found.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val volunteerNames = matchingVolunteers.map { it.getString("name") ?: it.id }.toTypedArray()
                val volunteerEmails = matchingVolunteers.map { it.id }.toTypedArray()

                AlertDialog.Builder(this)
                    .setTitle("Reassign to Volunteer")
                    .setItems(volunteerNames) { _, which ->
                        val selectedEmail = volunteerEmails[which]
                        val selectedName = volunteerNames[which]
                        reassignRequest(docId, selectedEmail, selectedName)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
    }

    private fun reassignRequest(docId: String, email: String, name: String) {
        db.collection("requests").document(docId).get().addOnSuccessListener { reqDoc ->
            val rawDate = reqDoc.getString("date") ?: ""
            val reqMinutes = reqDoc.getLong("timeMinutes")?.toInt() ?: -1
            
            var searchDate = rawDate
            try {
                val displayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val searchFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val d = displayFormat.parse(rawDate)
                if (d != null) searchDate = searchFormat.format(d)
            } catch (e: Exception) {}

            db.collection("volunteer_availability").document("${email}_$searchDate").get()
                .addOnSuccessListener { avail ->
                    if (avail.exists()) {
                        val slots = (avail.get("slots") as? List<Long> ?: emptyList()).map { it.toInt() }.sorted()
                        if (slots.isEmpty()) {
                            Toast.makeText(this, "Volunteer has no slots.", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        val reqHour = if (reqMinutes != -1) reqMinutes / 60 else -1
                        val targetHour = if (reqHour != -1 && slots.contains(reqHour)) reqHour 
                                         else slots.minByOrNull { Math.abs(it - (if(reqHour == -1) slots[0] else reqHour)) } ?: slots[0]

                        val cal = Calendar.getInstance().apply { 
                            set(Calendar.HOUR_OF_DAY, targetHour)
                            set(Calendar.MINUTE, 0) 
                        }
                        val start = SimpleDateFormat("hh:00 a", Locale.getDefault()).format(cal.time)
                        cal.add(Calendar.HOUR_OF_DAY, 1)
                        val end = SimpleDateFormat("hh:00 a", Locale.getDefault()).format(cal.time)
                        val slotRange = "$start - $end"

                        db.collection("requests").document(docId)
                            .update(mapOf(
                                "volunteerEmail" to email,
                                "volunteerName" to name,
                                "status" to "Assigned",
                                "scheduledDate" to searchDate,
                                "scheduledTime" to slotRange,
                                "slotHour" to targetHour
                            ))
                            .addOnSuccessListener {
                                DialogUtils.showSuccessDialog(this, "Reassigned to $name for $slotRange")
                            }
                    } else {
                        Toast.makeText(this, "Volunteer not available on this date.", Toast.LENGTH_LONG).show()
                    }
                }
        }
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

    private fun fetchActiveRequests() {
        db.collection("requests")
            .whereIn("status", listOf("Assigned", "In Progress"))
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                activeList.clear()
                val docs = snapshot?.documents?.map { doc ->
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["docId"] = doc.id
                    data
                }?.sortedWith(Comparator { r1, r2 ->
                    val p1 = r1["priority"] as? String ?: ""
                    val p2 = r2["priority"] as? String ?: ""
                    val pLevel1 = if (p1.contains("P1")) 1 else if (p1.contains("P2")) 2 else 3
                    val pLevel2 = if (p2.contains("P1")) 1 else if (p2.contains("P2")) 2 else 3
                    if (pLevel1 != pLevel2) pLevel1 - pLevel2
                    else (r1["timestamp"] as? Long ?: 0L).compareTo(r2["timestamp"] as? Long ?: 0L)
                })

                docs?.forEach { activeList.add(it) }
                adapter.notifyDataSetChanged()
            }
    }
}

class AdminActiveAdapter(
    private val items: List<Map<String, Any>>,
    private val onDeleteClick: (String) -> Unit,
    private val onReassignClick: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<AdminActiveAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvType: TextView = v.findViewById(R.id.tv_admin_req_type)
        val tvUser: TextView = v.findViewById(R.id.tv_admin_req_user)
        val tvDesc: TextView = v.findViewById(R.id.tv_admin_req_desc)
        val tvPriority: TextView = v.findViewById(R.id.tv_admin_priority_label)
        val btnDelete: ImageView = v.findViewById(R.id.btn_delete_request)
        val tvStatus: TextView = v.findViewById(R.id.tv_admin_req_status)
        val btnReassign: Button = v.findViewById(R.id.btn_reassign)
        val tvSubmittedDate: TextView = v.findViewById(R.id.tv_admin_submitted_date)
        val tvName: TextView = v.findViewById(R.id.tv_admin_req_name)
        val tvEmail: TextView = v.findViewById(R.id.tv_admin_req_email)
        val tvPhone: TextView = v.findViewById(R.id.tv_admin_req_phone)
        val btnShowMore: TextView = v.findViewById(R.id.btn_admin_show_more)
        val layoutDetails: View = v.findViewById(R.id.layout_admin_details)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
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
            else -> {
                holder.tvPriority.setTextColor(0xFF18263A.toInt())
                holder.tvPriority.backgroundTintList = null
            }
        }

        val ts = item["timestamp"] as? Long ?: 0L
        holder.tvSubmittedDate.text = if (ts > 0) SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date(ts)) else "N/A"
        holder.tvName.text = item["userName"] as? String ?: "N/A"
        holder.tvEmail.text = item["userEmail"] as? String ?: "N/A"
        holder.tvPhone.text = item["phoneNumber"] as? String ?: "N/A"

        val status = item["status"] as? String ?: "Searching"
        val vName = item["volunteerName"] as? String
        val sTime = item["scheduledTime"] as? String
        
        var statusText = "Status: $status"
        if (vName != null) statusText += " ($vName)"
        if (sTime != null) statusText += "\nTime: $sTime"
        holder.tvStatus.text = statusText

        holder.btnShowMore.setOnClickListener {
            val isVisible = holder.layoutDetails.visibility == View.VISIBLE
            holder.layoutDetails.visibility = if (isVisible) View.GONE else View.VISIBLE
            holder.btnShowMore.text = if (isVisible) "Show more" else "Show less"
        }

        holder.btnDelete.setOnClickListener {
            val docId = item["docId"] as? String
            if (docId != null) onDeleteClick(docId)
        }

        holder.btnReassign.setOnClickListener {
            onReassignClick(item)
        }
    }

    override fun getItemCount() = items.size
}
