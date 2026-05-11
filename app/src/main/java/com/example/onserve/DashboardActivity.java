package com.example.onserve;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Get data from SignupActivity
        final String name = getIntent().getStringExtra("NAME");
        final String email = getIntent().getStringExtra("EMAIL");
        final String password = getIntent().getStringExtra("PASSWORD");
        final String phone = getIntent().getStringExtra("PHONE");
        final String address = getIntent().getStringExtra("ADDRESS");

        ImageView backArrow = findViewById(R.id.iv_back_arrow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        final CheckBox cbElectrical = findViewById(R.id.cb_electrical);
        final CheckBox cbPlumbing = findViewById(R.id.cb_plumbing);
        final CheckBox cbITSupport = findViewById(R.id.cb_it_support);
        final CheckBox cbFirstAid = findViewById(R.id.cb_first_aid);
        final CheckBox cbMedical = findViewById(R.id.cb_medical);
        final CheckBox cbManualLabor = findViewById(R.id.cb_manual_labor);
        final CheckBox cbVehicleTransport = findViewById(R.id.cb_vehicle_transport);
        final CheckBox cbDelivery = findViewById(R.id.cb_delivery);
        final CheckBox cbDebris = findViewById(R.id.cb_debris);
        final CheckBox cbCommunications = findViewById(R.id.cb_communications);
        final CheckBox cbOthers = findViewById(R.id.cb_others);

        final EditText etOtherExpertise = findViewById(R.id.et_other_expertise);
        final LinearLayout layoutOthersContainer = findViewById(R.id.layout_others_container);
        final LinearLayout layoutDynamicOthers = findViewById(R.id.layout_dynamic_others);
        ImageButton btnAddOther = findViewById(R.id.btn_add_other);

        final List<EditText> extraExpertiseFields = new ArrayList<>();

        // Optional: Change background when checked
        CompoundButton.OnCheckedChangeListener backgroundUpdater = (buttonView, isChecked) -> {
            View parent = (View) buttonView.getParent();
            if (isChecked) {
                parent.setBackgroundResource(R.drawable.card_selected_highlight);
            } else {
                parent.setBackgroundResource(R.drawable.card_selection_bg);
            }
        };

        cbElectrical.setOnCheckedChangeListener(backgroundUpdater);
        cbPlumbing.setOnCheckedChangeListener(backgroundUpdater);
        cbITSupport.setOnCheckedChangeListener(backgroundUpdater);
        cbFirstAid.setOnCheckedChangeListener(backgroundUpdater);
        cbMedical.setOnCheckedChangeListener(backgroundUpdater);
        cbManualLabor.setOnCheckedChangeListener(backgroundUpdater);
        cbVehicleTransport.setOnCheckedChangeListener(backgroundUpdater);
        cbDelivery.setOnCheckedChangeListener(backgroundUpdater);
        cbDebris.setOnCheckedChangeListener(backgroundUpdater);
        cbCommunications.setOnCheckedChangeListener(backgroundUpdater);

        cbOthers.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                backgroundUpdater.onCheckedChanged(buttonView, isChecked);
                layoutOthersContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        btnAddOther.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText newField = addNewExpertiseField(layoutDynamicOthers);
                extraExpertiseFields.add(newField);
            }
        });

        Button btnCreateAccount = findViewById(R.id.btn_create_account);
        btnCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Collect all expertise
                StringBuilder expertise = new StringBuilder();
                if (cbElectrical.isChecked()) expertise.append("Electrical;");
                if (cbPlumbing.isChecked()) expertise.append("Plumbing;");
                if (cbITSupport.isChecked()) expertise.append("IT Support;");
                if (cbFirstAid.isChecked()) expertise.append("First Aid;");
                if (cbMedical.isChecked()) expertise.append("Medical Professional;");
                if (cbManualLabor.isChecked()) expertise.append("Manual Labor;");
                if (cbVehicleTransport.isChecked()) expertise.append("Vehicle Transport;");
                if (cbDelivery.isChecked()) expertise.append("Delivery & Distribution;");
                if (cbDebris.isChecked()) expertise.append("Debris Cleaning;");
                if (cbCommunications.isChecked()) expertise.append("Communications;");
                
                if (cbOthers.isChecked()) {
                    String other = etOtherExpertise.getText().toString().trim();
                    if (!other.isEmpty()) expertise.append(other).append(";");
                    
                    for (EditText field : extraExpertiseFields) {
                        String val = field.getText().toString().trim();
                        if (!val.isEmpty()) expertise.append(val).append(";");
                    }
                }

                if (!NetworkUtils.INSTANCE.isNetworkAvailable(DashboardActivity.this)) {
                    DialogUtils.INSTANCE.showErrorDialog(DashboardActivity.this, "Network Error", "No internet connection detected.");
                    return;
                }

                User volunteer = new User(name, email, password, "Volunteer", phone, address, expertise.toString());
                saveVolunteerToFirestore(email, volunteer);
            }
        });
    }

    private void saveVolunteerToFirestore(String email, User volunteer) {
        FirebaseFirestore.getInstance().collection("users").document(email).set(volunteer)
                .addOnSuccessListener(aVoid -> {
                    // Remember email for session
                    getSharedPreferences("OnServePrefs", MODE_PRIVATE).edit().putString("USER_EMAIL", email).apply();
                    
                    Intent intent = new Intent(DashboardActivity.this, SuccessActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    DialogUtils.INSTANCE.showErrorDialog(this, "Save Failed", e.getMessage() != null ? e.getMessage() : "Failed to save volunteer details.");
                });
    }

    private EditText addNewExpertiseField(LinearLayout container) {
        EditText editText = new EditText(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(50)
        );
        params.setMargins(0, 0, 0, dpToPx(10));
        editText.setLayoutParams(params);
        editText.setHint("Specify expertise");
        editText.setBackgroundResource(R.drawable.input_rounded);
        editText.setPadding(dpToPx(15), 0, dpToPx(15), 0);
        editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT);

        container.addView(editText);
        return editText;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}
