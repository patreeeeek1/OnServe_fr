package com.example.onserve

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private var currentUserEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        db = FirebaseFirestore.getInstance()
        currentUserEmail = getSharedPreferences("OnServePrefs", Context.MODE_PRIVATE).getString("USER_EMAIL", null)

        if (currentUserEmail == null) {
            DialogUtils.showErrorDialog(this, "Session Error", "Please login again.")
            finish()
            return
        }

        val btnBack: FrameLayout = findViewById(R.id.btn_back_container)
        btnBack.setOnClickListener { finish() }

        findViewById<Button>(R.id.btn_change_name).setOnClickListener {
            showEditDialog("Change Name", "name", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        }

        findViewById<Button>(R.id.btn_change_phone).setOnClickListener {
            showEditDialog("Change Phone Number", "phone", InputType.TYPE_CLASS_PHONE)
        }

        findViewById<Button>(R.id.btn_change_email).setOnClickListener {
            showEditDialog("Change Email Address", "email", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        }

        findViewById<Button>(R.id.btn_change_address).setOnClickListener {
            showEditDialog("Change Address", "address", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
        }

        findViewById<Button>(R.id.btn_change_password).setOnClickListener {
            showEditDialog("Change Password", "password", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        }
    }

    private fun showEditDialog(title: String, fieldKey: String, inputType: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)

        val input = EditText(this)
        input.inputType = inputType
        
        // Add some padding to the EditText in the dialog
        val padding = (20 * resources.displayMetrics.density).toInt()
        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(padding, padding / 2, padding, padding / 2)
        input.layoutParams = params
        container.addView(input)
        
        // Fetch current value to pre-fill
        db.collection("users").document(currentUserEmail!!).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val currentVal = document.getString(fieldKey) ?: ""
                    input.setText(currentVal)
                    input.setSelection(input.text.length)
                }
            }

        builder.setView(container)

        builder.setPositiveButton("Update") { dialog, _ ->
            val newValue = input.text.toString().trim()
            if (newValue.isNotEmpty()) {
                if (fieldKey == "email") {
                    migrateUserEmail(newValue)
                } else {
                    updateField(fieldKey, newValue)
                }
            } else {
                DialogUtils.showErrorDialog(this, "Input Error", "Field cannot be empty.")
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun updateField(key: String, value: String) {
        db.collection("users").document(currentUserEmail!!).update(key, value)
            .addOnSuccessListener {
                DialogUtils.showSuccessDialog(this, "Profile updated successfully.")
            }
            .addOnFailureListener { e ->
                DialogUtils.showErrorDialog(this, "Update Failed", e.message ?: "Could not update profile.")
            }
    }

    private fun migrateUserEmail(newEmail: String) {
        if (newEmail == currentUserEmail) return

        val oldEmail = currentUserEmail!!
        
        // 1. Fetch current data
        db.collection("users").document(oldEmail).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userData = document.toObject(User::class.java)
                    if (userData != null) {
                        // Create a modified copy
                        val newUser = userData.copy(email = newEmail)
                        
                        // 2. Create new document
                        db.collection("users").document(newEmail).set(newUser)
                            .addOnSuccessListener {
                                // 3. Delete old document
                                db.collection("users").document(oldEmail).delete()
                                
                                // 4. Update session
                                getSharedPreferences("OnServePrefs", Context.MODE_PRIVATE)
                                    .edit()
                                    .putString("USER_EMAIL", newEmail)
                                    .apply()
                                
                                currentUserEmail = newEmail
                                DialogUtils.showSuccessDialog(this, "Email updated successfully.")
                            }
                            .addOnFailureListener { e ->
                                DialogUtils.showErrorDialog(this, "Migrate Error", e.message ?: "Failed to create new record.")
                            }
                    }
                }
            }
    }
}
