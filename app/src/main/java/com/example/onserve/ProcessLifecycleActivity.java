package com.example.onserve;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class ProcessLifecycleActivity extends AppCompatActivity {

    private static final String TAG = "OnServe_OS";
    private ProcessLifecycleViewModel viewModel;
    private EditText etData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process_lifecycle);
        
        // OS CONCEPT: NEW -> READY
        Log.d(TAG, "[PROCESS STATE: NEW] -> [STATE: READY] - Activity created and resources allocated.");

        viewModel = new ViewModelProvider(this).get(ProcessLifecycleViewModel.class);
        etData = findViewById(R.id.et_lifecycle_data);

        // Retrieve data from ViewModel (survives rotation)
        if (!viewModel.getSavedData().isEmpty()) {
            etData.setText(viewModel.getSavedData());
        }

        // Retrieve data from savedInstanceState (survives process death / 'Don't keep activities')
        if (savedInstanceState != null) {
            String bundleData = savedInstanceState.getString("persistence_key");
            if (bundleData != null && etData.getText().toString().isEmpty()) {
                etData.setText(bundleData);
                Log.d(TAG, "PERSISTENCE: Data restored from Bundle (survived WAITING state).");
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // OS CONCEPT: READY
        Log.d(TAG, "[PROCESS STATE: READY] - Activity is now visible and ready to run.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // OS CONCEPT: RUNNING
        Log.d(TAG, "[PROCESS STATE: RUNNING] - Activity is executing in CPU foreground.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        // OS CONCEPT: RUNNING -> WAITING
        Log.d(TAG, "[PROCESS STATE: RUNNING] -> [STATE: WAITING] - Activity interrupted/losing focus.");
        
        // Save current input to ViewModel
        viewModel.setSavedData(etData.getText().toString());
    }

    @Override
    protected void onStop() {
        super.onStop();
        // OS CONCEPT: WAITING
        Log.d(TAG, "[PROCESS STATE: WAITING] - Activity backgrounded.");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "[PROCESS STATE: WAITING] -> [STATE: READY] - Returning from background.");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("persistence_key", etData.getText().toString());
        Log.d(TAG, "PERSISTENCE: Data saved to Bundle for state management.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // OS CONCEPT: TERMINATED
        Log.d(TAG, "PROCESS STATE: TERMINATED - Activity is being destroyed and resources are released.");
    }
}
