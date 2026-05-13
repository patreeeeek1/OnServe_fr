package com.example.onserve;

import android.util.Log;
import androidx.lifecycle.ViewModel;

public class ProcessLifecycleViewModel extends ViewModel {
    private String savedData = "";

    public ProcessLifecycleViewModel() {
        // PROOF OF CONCEPT: ViewModel constructor log
        Log.d("OnServe_OS", "VIEWMODEL STATE: PERSISTENT - Constructor called. I will survive Activity destruction.");
    }

    public String getSavedData() {
        return savedData;
    }

    public void setSavedData(String savedData) {
        this.savedData = savedData;
    }
}
