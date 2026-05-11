package com.example.onserve

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.Button
import android.widget.TextView

object DialogUtils {

    fun showSuccessDialog(activity: Activity, message: String, onDismiss: (() -> Unit)? = null) {
        Dialog(activity).apply {
            setContentView(R.layout.dialog_success)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCancelable(false)
            findViewById<TextView>(R.id.tv_success_message).text = message
            findViewById<Button>(R.id.btn_success_ok).setOnClickListener {
                dismiss()
                onDismiss?.invoke()
            }
            show()
        }
    }

    fun showErrorDialog(activity: Activity, title: String, message: String) {
        Dialog(activity).apply {
            setContentView(R.layout.dialog_info)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCancelable(true)
            findViewById<TextView>(R.id.tv_info_title).text = title
            findViewById<TextView>(R.id.tv_info_message).text = message
            findViewById<Button>(R.id.btn_info_ok).setOnClickListener { dismiss() }
            show()
        }
    }
}
