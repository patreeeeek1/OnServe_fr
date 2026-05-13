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

class VolunteerListActivity : AppCompatActivity() {

    private lateinit var rvVolunteers: RecyclerView
    private lateinit var adapter: VolunteerAdapter
    private val db = FirebaseFirestore.getInstance()
    private val volunteerList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_volunteer_list)

        findViewById<FrameLayout>(R.id.btn_back_volunteer_list).setOnClickListener { finish() }

        rvVolunteers = findViewById(R.id.rv_volunteer_list)
        rvVolunteers.layoutManager = LinearLayoutManager(this)
        adapter = VolunteerAdapter(volunteerList)
        rvVolunteers.adapter = adapter

        fetchVolunteers()
    }

    private fun fetchVolunteers() {
        db.collection("users")
            .whereEqualTo("type", "Volunteer")
            .get()
            .addOnSuccessListener { snapshot ->
                volunteerList.clear()
                for (doc in snapshot.documents) {
                    val user = doc.toObject(User::class.java)
                    if (user != null) volunteerList.add(user)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                DialogUtils.showErrorDialog(this, "Fetch Error", e.message ?: "Failed to load volunteers.")
            }
    }
}

class VolunteerAdapter(private val items: List<User>) : RecyclerView.Adapter<VolunteerAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tv_volunteer_name)
        val tvEmail: TextView = v.findViewById(R.id.tv_volunteer_email)
        val tvExpertisePreview: TextView = v.findViewById(R.id.tv_volunteer_expertise_preview)
        val tvExpertise: TextView = v.findViewById(R.id.tv_volunteer_expertise)
        val tvPhone: TextView = v.findViewById(R.id.tv_volunteer_phone)
        val tvAddress: TextView = v.findViewById(R.id.tv_volunteer_address)
        val btnReadMore: TextView = v.findViewById(R.id.btn_volunteer_read_more)
        val layoutDetails: View = v.findViewById(R.id.layout_volunteer_details)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_volunteer, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = items[position]
        holder.tvName.text = user.name
        holder.tvEmail.text = user.email
        
        val expText = if (user.expertise.isEmpty()) "Not specified" else user.expertise
        holder.tvExpertisePreview.text = "Expertise: ${expText.split(";")[0]}"
        holder.tvExpertise.text = expText

        holder.tvPhone.text = user.phone
        holder.tvAddress.text = user.address

        holder.btnReadMore.setOnClickListener {
            val isVisible = holder.layoutDetails.visibility == View.VISIBLE
            holder.layoutDetails.visibility = if (isVisible) View.GONE else View.VISIBLE
            holder.btnReadMore.text = if (isVisible) "Read more" else "Read less"
        }
    }

    override fun getItemCount() = items.size
}
