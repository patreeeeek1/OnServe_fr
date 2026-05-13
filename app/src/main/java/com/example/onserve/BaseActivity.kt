package com.example.onserve

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

/**
 * Base activity to showcase Process State Management by mapping 
 * the Android Activity Lifecycle to OS Process States.
 */
abstract class BaseActivity : AppCompatActivity() {

    protected val lifecycleTag = "OnServe_OS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(lifecycleTag, "[PROCESS STATE: NEW] -> [STATE: READY] - ${this.javaClass.simpleName} created and resources allocated.")
    }

    override fun onStart() {
        super.onStart()
        Log.d(lifecycleTag, "[PROCESS STATE: READY] - ${this.javaClass.simpleName} is now visible and ready to run.")
    }

    override fun onResume() {
        super.onResume()
        Log.d(lifecycleTag, "[PROCESS STATE: RUNNING] - ${this.javaClass.simpleName} is executing in the CPU foreground.")
    }

    override fun onPause() {
        super.onPause()
        Log.d(lifecycleTag, "[PROCESS STATE: RUNNING] -> [STATE: WAITING] - ${this.javaClass.simpleName} interrupted/losing focus.")
    }

    override fun onStop() {
        super.onStop()
        Log.d(lifecycleTag, "[PROCESS STATE: WAITING] - ${this.javaClass.simpleName} is backgrounded and waiting for CPU slot.")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(lifecycleTag, "[PROCESS STATE: WAITING] -> [STATE: READY] - ${this.javaClass.simpleName} returning from background.")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(lifecycleTag, "[PROCESS STATE: TERMINATED] - ${this.javaClass.simpleName} resources released from memory.")
    }
}
