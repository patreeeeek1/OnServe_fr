package com.example.onserve

import com.google.firebase.firestore.FirebaseFirestore

object NotificationUtils {
    private val db = FirebaseFirestore.getInstance()

    fun sendNotification(targetEmail: String, title: String, message: String) {
        val notification = hashMapOf(
            "targetEmail" to targetEmail,
            "title" to title,
            "message" to message,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false
        )
        db.collection("notifications").add(notification)
    }
}
