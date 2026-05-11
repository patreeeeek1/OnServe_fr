package com.example.onserve

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

/**
 * Base activity to showcase Process State Management by mapping 
 * the Android Activity Lifecycle to OS Process States.
 */
abstract class BaseActivity : AppCompatActivity() {

    protected val lifecycleTag = "ProcessStateManagement"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(lifecycleTag, "${this.javaClass.simpleName} [STATE: NEW] - Activity created, mapping to Process 'New' state.")
    }

    override fun onStart() {
        super.onStart()
        Log.d(lifecycleTag, "${this.javaClass.simpleName} [STATE: READY] - Activity visible, mapping to Process 'Ready' state (Loaded in memory).")
    }

    override fun onResume() {
        super.onResume()
        Log.d(lifecycleTag, "${this.javaClass.simpleName} [STATE: RUNNING] - Activity focused, mapping to Process 'Running' state (Executing in CPU).")
    }

    override fun onPause() {
        super.onPause()
        Log.d(lifecycleTag, "${this.javaClass.simpleName} [STATE: WAITING] - Activity losing focus, mapping to Process 'Waiting' state (Interrupted).")
    }

    override fun onStop() {
        super.onStop()
        Log.d(lifecycleTag, "${this.javaClass.simpleName} [STATE: WAITING/READY] - Activity hidden, mapping to Process 'Waiting/Ready' (Swapped out of CPU).")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(lifecycleTag, "${this.javaClass.simpleName} [STATE: TERMINATED] - Activity destroyed, mapping to Process 'Terminated' state (Removed from memory).")
    }
}
