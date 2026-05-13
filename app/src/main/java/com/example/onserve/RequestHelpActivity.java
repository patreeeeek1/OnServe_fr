package com.example.onserve;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RequestHelpActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 300;

    private int currentStep = 1;
    private String selectedType = "";
    private boolean isEmergency = false;

    private TextView tvStepIndicator, tvStepTitle, tvCharCount, tvEmergencyLabel, tvLocationText;
    private TextView tvSelectedDate, tvSelectedTime, tvEditLocation;
    private EditText etPhone;
    private ScrollView layoutStep1, layoutStep2, layoutStep3;
    private RelativeLayout layoutFullMapPicker;
    private Button btnBackCancel, btnNextSubmit, btnConfirmLocation;
    private ImageButton btnCurrentLocation, btnFullCurrentLocation, btnZoomIn, btnZoomOut;
    private ImageView ivFullMapBack;
    private LinearLayout btnSelectDate, btnSelectTime;
    private EditText etOtherProblem, etDescription;
    private AutoCompleteTextView etMapSearch;
    private CheckBox cbAsap;
    private android.widget.RadioButton rbDangerYes;

    private LinearLayout optionElectrical, optionPlumbing, optionStructural, optionFood, optionMedical, optionWelfare, optionTransport, optionOther, optionFire, optionRescue, optionWater, optionAnimal, optionInfo, optionWaste;

    private GoogleMap mMap, mFullMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Marker userMarker, fullUserMarker;
    private LatLng currentSelectedLatLng;
    private final Calendar selectedCalendar = Calendar.getInstance();

    private final LatLngBounds BAGUIO_BOUNDS = new LatLngBounds(
            new LatLng(16.3700, 120.5500), // Southwest corner
            new LatLng(16.4500, 120.6500)  // Northeast corner
    );

    private PlacesClient placesClient;
    private AutocompleteSessionToken autocompleteSessionToken;
    private PlaceAutocompleteAdapter placeAutocompleteAdapter;
    private List<AutocompletePrediction> currentPredictions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_help);

        // Initialize Places
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(getApplicationContext(), "AIzaSyCYTs1IOxXsRVzBEdgFFtcPdc-NGMoZQcw");
        }
        placesClient = Places.createClient(this);
        autocompleteSessionToken = AutocompleteSessionToken.newInstance();
        placeAutocompleteAdapter = new PlaceAutocompleteAdapter(this);

        // Initialize Views
        tvStepIndicator = findViewById(R.id.tv_step_indicator);
        tvStepTitle = findViewById(R.id.tv_step_title);
        tvCharCount = findViewById(R.id.tv_char_count);
        tvEmergencyLabel = findViewById(R.id.tv_emergency_label);
        tvLocationText = findViewById(R.id.tv_location_text);
        tvEditLocation = findViewById(R.id.tv_edit_location);
        
        tvSelectedDate = findViewById(R.id.tv_selected_date);
        tvSelectedTime = findViewById(R.id.tv_selected_time);

        layoutStep1 = findViewById(R.id.layout_step1);
        layoutStep2 = findViewById(R.id.layout_step2);
        layoutStep3 = findViewById(R.id.layout_step3);
        layoutFullMapPicker = findViewById(R.id.layout_full_map_picker);
        
        btnBackCancel = findViewById(R.id.btn_back_cancel);
        btnNextSubmit = findViewById(R.id.btn_next_submit);
        btnConfirmLocation = findViewById(R.id.btn_confirm_location);
        ivFullMapBack = findViewById(R.id.iv_full_map_back);
        
        btnCurrentLocation = findViewById(R.id.btn_current_location);
        btnFullCurrentLocation = findViewById(R.id.btn_full_current_location);
        btnZoomIn = findViewById(R.id.btn_zoom_in);
        btnZoomOut = findViewById(R.id.btn_zoom_out);
        
        btnSelectDate = findViewById(R.id.btn_select_date);
        btnSelectTime = findViewById(R.id.btn_select_time);
        
        etPhone = findViewById(R.id.et_phone_number);
        etOtherProblem = findViewById(R.id.et_other_problem_step1);
        etDescription = findViewById(R.id.et_description);
        etMapSearch = findViewById(R.id.et_map_search);
        cbAsap = findViewById(R.id.cb_asap);
        rbDangerYes = findViewById(R.id.rb_danger_yes);
        
        optionElectrical = findViewById(R.id.option_electrical);
        optionPlumbing = findViewById(R.id.option_plumbing);
        optionStructural = findViewById(R.id.option_structural);
        optionFood = findViewById(R.id.option_food);
        optionMedical = findViewById(R.id.option_medical);
        optionWelfare = findViewById(R.id.option_welfare);
        optionTransport = findViewById(R.id.option_transport);
        optionOther = findViewById(R.id.option_other);
        optionFire = findViewById(R.id.option_fire);
        optionRescue = findViewById(R.id.option_rescue);
        optionWater = findViewById(R.id.option_water);
        optionAnimal = findViewById(R.id.option_animal);
        optionInfo = findViewById(R.id.option_info);
        optionWaste = findViewById(R.id.option_waste);

        // Initialize Location Services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Handle Intent extras
        isEmergency = getIntent().getBooleanExtra("IS_EMERGENCY", false);
        String typeFromHome = getIntent().getStringExtra("SELECTED_TYPE");
        
        if (isEmergency) {
            tvEmergencyLabel.setVisibility(View.VISIBLE);
            cbAsap.setChecked(true);
            cbAsap.setEnabled(false); // Always ASAP for emergencies
            tvSelectedDate.setText("ASAP");
            // tvSelectedTime.setText("ASAP");
            // Stay at step 1 for classification
        } else if (typeFromHome != null) {
            selectOption(typeFromHome);
        }

        setupListeners();
        setupSearchAutocomplete();
    }

    private void setupSearchAutocomplete() {
        etMapSearch.setAdapter(placeAutocompleteAdapter);
        etMapSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 2) {
                    fetchAutocompletePredictions(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etMapSearch.setOnItemClickListener((parent, view, position, id) -> {
            AutocompletePrediction prediction = (AutocompletePrediction) parent.getItemAtPosition(position);
            if (prediction != null) {
                fetchPlaceDetails(prediction.getPlaceId());
            }
        });
    }

    private void fetchAutocompletePredictions(String query) {
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(autocompleteSessionToken)
                .setQuery(query)
                .setLocationBias(RectangularBounds.newInstance(BAGUIO_BOUNDS))
                .setCountries("PH")
                .build();

        placesClient.findAutocompletePredictions(request).addOnSuccessListener(response -> {
            currentPredictions = response.getAutocompletePredictions();
            placeAutocompleteAdapter.updateData(currentPredictions);
            etMapSearch.showDropDown();
        }).addOnFailureListener(exception -> {
            // Silently fail or log
        });
    }

    private void fetchPlaceDetails(String placeId) {
        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.LOCATION);
        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();

        placesClient.fetchPlace(request).addOnSuccessListener(response -> {
            Place place = response.getPlace();
            if (place.getLocation() != null) {
                setManualLocation(place.getLocation(), true);
                if (mFullMap != null) {
                    mFullMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLocation(), 15f));
                }
            }
        }).addOnFailureListener(exception -> {
            DialogUtils.INSTANCE.showErrorDialog(this, "Place Details", "Failed to get place details.");
        });
    }

    private void setupListeners() {
        View.OnClickListener optionListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.option_electrical) selectOption("ELECTRICAL");
                else if (v.getId() == R.id.option_plumbing) selectOption("PLUMBING");
                else if (v.getId() == R.id.option_structural) selectOption("STRUCTURAL");
                else if (v.getId() == R.id.option_food) selectOption("FOOD");
                else if (v.getId() == R.id.option_medical) selectOption("MEDICAL");
                else if (v.getId() == R.id.option_welfare) selectOption("WELFARE");
                else if (v.getId() == R.id.option_transport) selectOption("TRANSPORT");
                else if (v.getId() == R.id.option_other) selectOption("OTHER");
                else if (v.getId() == R.id.option_fire) selectOption("FIRE");
                else if (v.getId() == R.id.option_rescue) selectOption("RESCUE");
                else if (v.getId() == R.id.option_water) selectOption("WATER");
                else if (v.getId() == R.id.option_animal) selectOption("ANIMAL");
                else if (v.getId() == R.id.option_info) selectOption("INFO");
                else if (v.getId() == R.id.option_waste) selectOption("WASTE");
            }
        };

        optionElectrical.setOnClickListener(optionListener);
        optionPlumbing.setOnClickListener(optionListener);
        optionStructural.setOnClickListener(optionListener);
        optionFood.setOnClickListener(optionListener);
        optionMedical.setOnClickListener(optionListener);
        optionWelfare.setOnClickListener(optionListener);
        optionTransport.setOnClickListener(optionListener);
        optionOther.setOnClickListener(optionListener);
        optionFire.setOnClickListener(optionListener);
        optionRescue.setOnClickListener(optionListener);
        optionWater.setOnClickListener(optionListener);
        optionAnimal.setOnClickListener(optionListener);
        optionInfo.setOnClickListener(optionListener);
        optionWaste.setOnClickListener(optionListener);

        tvEditLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFullScreenMap();
            }
        });

        ivFullMapBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutFullMapPicker.setVisibility(View.GONE);
            }
        });

        btnConfirmLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutFullMapPicker.setVisibility(View.GONE);
                if (currentSelectedLatLng != null) {
                    syncMaps(currentSelectedLatLng);
                }
            }
        });

        btnCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    getCurrentLocation(false);
                } else {
                    requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_PERMISSION_REQUEST_CODE);
                }
            }
        });

        btnFullCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentLocation(true);
            }
        });

        btnZoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFullMap != null) {
                    mFullMap.animateCamera(CameraUpdateFactory.zoomIn());
                }
            }
        });

        btnZoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFullMap != null) {
                    mFullMap.animateCamera(CameraUpdateFactory.zoomOut());
                }
            }
        });

        etMapSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    searchLocationOnMap(v.getText().toString());
                    return true;
                }
                return false;
            }
        });

        btnSelectDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isEmergency) showDatePicker();
            }
        });

        btnSelectTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isEmergency) showTimePicker();
            }
        });

        cbAsap.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                tvSelectedDate.setText("ASAP");
                tvSelectedTime.setText("ASAP");
                btnSelectTime.setVisibility(View.GONE);
                findViewById(R.id.btn_select_time_divider).setVisibility(View.GONE);
            } else {
                tvSelectedDate.setText("Select Date");
                tvSelectedTime.setText("Select Time");
                btnSelectTime.setVisibility(View.VISIBLE);
                findViewById(R.id.btn_select_time_divider).setVisibility(View.VISIBLE);
            }
        });

        btnNextSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentStep == 1) {
                    if (selectedType.isEmpty()) {
                        showInfoDialog("Select Type", "Please select the type of assistance you need.");
                        return;
                    }
                    goToStep(2);
                } else if (currentStep == 2) {
                    if (etDescription.getText().toString().trim().isEmpty()) {
                        showInfoDialog("Missing Description", "Please describe the problem so we can better assist you.");
                        return;
                    }
                    goToStep(3);
                } else if (currentStep == 3) {
                    if (!isEmergency && !cbAsap.isChecked()) {
                        if (tvSelectedDate.getText().toString().equals("Select Date")) {
                            showInfoDialog("Select Date", "Please select a preferred date for the service.");
                            return;
                        }
                        if (selectedMinutes == -1) {
                            showInfoDialog("Select Time", "Please select an available time slot from the list.");
                            return;
                        }
                        checkAvailabilityAndProceed();
                    } else {
                        saveRequestToFirestore();
                    }
                }
            }
        });

        btnBackCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentStep == 1 || (isEmergency && currentStep == 2)) {
                    finish();
                } else {
                    goToStep(currentStep - 1);
                }
            }
        });

        etDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvCharCount.setText(s.length() + "/200");
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void searchLocationOnMap(String locationName) {
        if (locationName == null || locationName.isEmpty()) return;

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addressList = geocoder.getFromLocationName(locationName, 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                setManualLocation(latLng, true);
                if (mFullMap != null) {
                    mFullMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
                }
            } else {
                DialogUtils.INSTANCE.showErrorDialog(this, "Search", "Location not found.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            DialogUtils.INSTANCE.showErrorDialog(this, "Search", "Search error, try again.");
        }
    }

    private void openFullScreenMap() {
        layoutFullMapPicker.setVisibility(View.VISIBLE);
        SupportMapFragment fullMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.full_map);
        if (fullMapFragment != null) {
            fullMapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(@NonNull GoogleMap googleMap) {
                    mFullMap = googleMap;
                    mFullMap.setLatLngBoundsForCameraTarget(BAGUIO_BOUNDS);
                    mFullMap.setOnMapClickListener(latLng -> setManualLocation(latLng, true));
                    if (currentSelectedLatLng != null) {
                        setManualLocation(currentSelectedLatLng, true);
                    } else {
                        getCurrentLocation(true);
                    }
                }
            });
        }
    }

    private void syncMaps(LatLng latLng) {
        currentSelectedLatLng = latLng;
        if (mMap != null) {
            if (userMarker != null) userMarker.setPosition(latLng);
            else userMarker = mMap.addMarker(new MarkerOptions().position(latLng));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
        }
        updateAddressText(latLng.latitude, latLng.longitude, false);
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    selectedCalendar.set(Calendar.YEAR, year1);
                    selectedCalendar.set(Calendar.MONTH, monthOfYear);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    
                    selectedMinutes = -1;
                    tvSelectedTime.setText("Select Time");
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    tvSelectedDate.setText(sdf.format(selectedCalendar.getTime()));
                }, year, month, day);
        
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private int selectedMinutes = -1;

    private void showTimePicker() {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> {
                    Calendar tempCal = (Calendar) selectedCalendar.clone();
                    tempCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    tempCal.set(Calendar.MINUTE, minute1);

                    if (tempCal.before(Calendar.getInstance())) {
                        DialogUtils.INSTANCE.showErrorDialog(this, "Invalid Time", "Cannot select a time in the past.");
                    } else {
                        selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selectedCalendar.set(Calendar.MINUTE, minute1);
                        selectedMinutes = hourOfDay * 60 + minute1;
                        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                        tvSelectedTime.setText(sdf.format(selectedCalendar.getTime()));
                    }
                }, hour, minute, false);
        timePickerDialog.show();
    }

    private boolean checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission(String permission, int code) {
        ActivityCompat.requestPermissions(this, new String[]{permission}, code);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation(false);
            }
        }
    }

    private void selectOption(String type) {
        selectedType = type;
        
        // Reset backgrounds
        optionElectrical.setBackgroundResource(R.drawable.card_white_rounded);
        optionPlumbing.setBackgroundResource(R.drawable.card_white_rounded);
        optionStructural.setBackgroundResource(R.drawable.card_white_rounded);
        optionFood.setBackgroundResource(R.drawable.card_white_rounded);
        optionMedical.setBackgroundResource(R.drawable.card_white_rounded);
        optionWelfare.setBackgroundResource(R.drawable.card_white_rounded);
        optionTransport.setBackgroundResource(R.drawable.card_white_rounded);
        optionOther.setBackgroundResource(R.drawable.card_white_rounded);
        optionFire.setBackgroundResource(R.drawable.card_white_rounded);
        optionRescue.setBackgroundResource(R.drawable.card_white_rounded);
        optionWater.setBackgroundResource(R.drawable.card_white_rounded);
        optionAnimal.setBackgroundResource(R.drawable.card_white_rounded);
        optionInfo.setBackgroundResource(R.drawable.card_white_rounded);
        optionWaste.setBackgroundResource(R.drawable.card_white_rounded);
        
        etOtherProblem.setVisibility(View.GONE);

        switch (type) {
            case "ELECTRICAL":
                optionElectrical.setBackgroundResource(R.drawable.card_selected_highlight);
                break;
            case "PLUMBING":
                optionPlumbing.setBackgroundResource(R.drawable.card_selected_highlight);
                break;
            case "STRUCTURAL":
                optionStructural.setBackgroundResource(R.drawable.card_selected_highlight);
                break;
            case "FOOD":
                optionFood.setBackgroundResource(R.drawable.card_selected_highlight);
                break;
            case "MEDICAL":
                optionMedical.setBackgroundResource(R.drawable.card_selected_highlight);
                break;
            case "WELFARE":
                optionWelfare.setBackgroundResource(R.drawable.card_selected_highlight);
                break;
            case "TRANSPORT":
                optionTransport.setBackgroundResource(R.drawable.card_selected_highlight);
                break;
            case "FIRE":
                optionFire.setBackgroundResource(R.drawable.card_selected_highlight);
                break;
            case "RESCUE":
                optionRescue.setBackgroundResource(R.drawable.card_selected_highlight);
                break;
            case "WATER":
                optionWater.setBackgroundResource(R.drawable.card_selected_highlight);
                break;
            case "ANIMAL":
                optionAnimal.setBackgroundResource(R.drawable.card_selected_highlight);
                break;
            case "INFO":
                optionInfo.setBackgroundResource(R.drawable.card_selected_highlight);
                break;
            case "WASTE":
                optionWaste.setBackgroundResource(R.drawable.card_selected_highlight);
                break;
            case "OTHER":
                optionOther.setBackgroundResource(R.drawable.card_selected_highlight);
                etOtherProblem.setVisibility(View.VISIBLE);
                etOtherProblem.requestFocus();
                break;
        }
    }

    private void goToStep(int step) {
        currentStep = step;
        
        layoutStep1.setVisibility(View.GONE);
        layoutStep2.setVisibility(View.GONE);
        layoutStep3.setVisibility(View.GONE);
        
        switch (step) {
            case 1:
                layoutStep1.setVisibility(View.VISIBLE);
                tvStepIndicator.setText("Step 1 of 3");
                tvStepTitle.setText("Choose Issue Type");
                btnBackCancel.setText("Cancel");
                btnNextSubmit.setText("Next");
                break;
            case 2:
                layoutStep2.setVisibility(View.VISIBLE);
                if (isEmergency) {
                    tvStepIndicator.setText("Emergency Step");
                    findViewById(R.id.rb_danger_yes).performClick(); // Pre-select Yes for emergency
                } else {
                    tvStepIndicator.setText("Step 2 of 3");
                }
                tvStepTitle.setText("Describe the Problem");
                btnBackCancel.setText(isEmergency ? "Cancel" : "Back");
                btnNextSubmit.setText("Next");
                break;
            case 3:
                layoutStep3.setVisibility(View.VISIBLE);
                if (isEmergency) {
                    tvStepIndicator.setText("Emergency Step");
                    btnSelectTime.setVisibility(View.GONE);
                    findViewById(R.id.btn_select_time_divider).setVisibility(View.GONE);
                } else {
                    tvStepIndicator.setText("Step 3 of 3");
                    if (cbAsap.isChecked()) {
                        btnSelectTime.setVisibility(View.GONE);
                        findViewById(R.id.btn_select_time_divider).setVisibility(View.GONE);
                    } else {
                        btnSelectTime.setVisibility(View.VISIBLE);
                        findViewById(R.id.btn_select_time_divider).setVisibility(View.VISIBLE);
                    }
                }
                tvStepTitle.setText("Review & Submit");
                btnBackCancel.setText("Back");
                btnNextSubmit.setText("SEND REQUEST");

                // Pre-fill phone number from profile
                String userEmailForPhone = getSharedPreferences("OnServePrefs", MODE_PRIVATE).getString("USER_EMAIL", "");
                if (!userEmailForPhone.isEmpty()) {
                    FirebaseFirestore.getInstance().collection("users").document(userEmailForPhone).get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists() && etPhone.getText().toString().isEmpty()) {
                                    etPhone.setText(doc.getString("phone"));
                                }
                            });
                }
                
                // Load Map
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
                if (mapFragment != null) {
                    mapFragment.getMapAsync(this);
                }
                break;
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        
        mMap.setLatLngBoundsForCameraTarget(BAGUIO_BOUNDS);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(16.4125, 120.5921), 13f));
        
        // Enable click to set location
        mMap.setOnMapClickListener(latLng -> setManualLocation(latLng, false));

        if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            getCurrentLocation(false);
        } else {
            requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void setManualLocation(LatLng latLng, boolean isFullMap) {
        currentSelectedLatLng = latLng;
        if (isFullMap && mFullMap != null) {
            if (fullUserMarker != null) fullUserMarker.setPosition(latLng);
            else fullUserMarker = mFullMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
            mFullMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        } else if (mMap != null) {
            if (userMarker != null) userMarker.setPosition(latLng);
            else userMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            updateAddressText(latLng.latitude, latLng.longitude, false);
        }
    }

    private void getCurrentLocation(boolean isFullMap) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        GoogleMap targetMap = isFullMap ? mFullMap : mMap;
        if (targetMap == null) return;

        targetMap.setMyLocationEnabled(true);
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                
                // Only center if user is within Baguio
                if (BAGUIO_BOUNDS.contains(currentLatLng)) {
                    currentSelectedLatLng = currentLatLng;
                    
                    if (isFullMap) {
                        if (fullUserMarker != null) fullUserMarker.setPosition(currentLatLng);
                        else fullUserMarker = mFullMap.addMarker(new MarkerOptions().position(currentLatLng).title("You are here"));
                        mFullMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
                    } else {
                        if (userMarker != null) userMarker.setPosition(currentLatLng);
                        else userMarker = mMap.addMarker(new MarkerOptions().position(currentLatLng).title("You are here"));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
                        updateAddressText(location.getLatitude(), location.getLongitude(), true);
                    }
                } else {
                    // Default to Baguio City center if user is outside
                    LatLng baguioCenter = new LatLng(16.4125, 120.5921);
                    targetMap.moveCamera(CameraUpdateFactory.newLatLngZoom(baguioCenter, 13f));
                }
            }
        });
    }

    private void checkAvailabilityAndProceed() {
        String date = tvSelectedDate.getText().toString();
        // Convert "MMM dd, yyyy" to "yyyy-MM-dd" for querying availability
        String searchDate = date;
        try {
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            SimpleDateFormat searchFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            java.util.Date d = displayFormat.parse(date);
            if (d != null) searchDate = searchFormat.format(d);
        } catch (Exception e) {
            searchDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
        }

        final String finalSearchDate = searchDate;

        FirebaseFirestore.getInstance().collection("users")
                .whereEqualTo("type", "Volunteer")
                .get()
                .addOnSuccessListener(volunteers -> {
                    List<com.google.firebase.firestore.DocumentSnapshot> qualified = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : volunteers.getDocuments()) {
                        String expertise = doc.getString("expertise");
                        if (expertise != null && expertise.toUpperCase().contains(selectedType.toUpperCase())) {
                            qualified.add(doc);
                        }
                    }

                    if (qualified.isEmpty()) {
                        showAvailabilityDialog();
                    } else {
                        checkVolunteerSlots(qualified, finalSearchDate, 0);
                    }
                });
    }

    private void checkVolunteerSlots(List<com.google.firebase.firestore.DocumentSnapshot> volunteers, String date, int index) {
        if (index >= volunteers.size()) {
            showAvailabilityDialog();
            return;
        }

        String email = volunteers.get(index).getId();
        FirebaseFirestore.getInstance().collection("volunteer_availability").document(email + "_" + date).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Object slotsObj = doc.get("slots");
                        if (slotsObj instanceof List) {
                            List<?> slotsList = (List<?>) slotsObj;
                            for (Object hourObj : slotsList) {
                                if (hourObj instanceof Long) {
                                    int hour = ((Long) hourObj).intValue();
                                    int slotStart = hour * 60;
                                    int slotEnd = (hour + 1) * 60;
                                    if (Math.abs(selectedMinutes - slotStart) <= 45 || Math.abs(selectedMinutes - slotEnd) <= 45 || (selectedMinutes >= slotStart && selectedMinutes <= slotEnd)) {
                                        saveRequestToFirestore();
                                        return;
                                    }
                                }
                            }
                        }
                    }
                    checkVolunteerSlots(volunteers, date, index + 1);
                });
    }

    private void showAvailabilityDialog() {
        new AlertDialog.Builder(this)
                .setTitle("No Direct Match Found")
                .setMessage("There are no available volunteers within 45 minutes of your selected time. Would you like to reschedule or stay on the waiting list?")
                .setPositiveButton("Reschedule", (dialog, which) -> {
                    // Stay on Step 3 and let user change time
                })
                .setNegativeButton("Waitlist Me", (dialog, which) -> {
                    saveRequestToFirestore();
                })
                .show();
    }

    private void saveRequestToFirestore() {
        final String finalType;
        if (selectedType.equals("OTHER")) {
            String otherText = etOtherProblem.getText().toString().trim();
            finalType = !otherText.isEmpty() ? otherText : "OTHER";
        } else {
            finalType = selectedType;
        }

        String desc = etDescription.getText().toString().trim();
        String loc = tvLocationText.getText().toString().replace("\n", " ");
        String date = tvSelectedDate.getText().toString();
        String phone = etPhone.getText().toString().trim();
        boolean isDanger = rbDangerYes.isChecked();
        boolean asap = cbAsap.isChecked() || isEmergency || isDanger;
        
        if (phone.isEmpty()) {
            showInfoDialog("Contact Number", "Please provide a contact number.");
            return;
        }

        performSave(finalType, desc, loc, date, phone, isDanger, asap);
    }

    private void performSave(String type, String desc, String loc, String date, String phone, boolean isDanger, boolean asap) {
        long timestamp = System.currentTimeMillis();
        String userEmail = getSharedPreferences("OnServePrefs", MODE_PRIVATE).getString("USER_EMAIL", "");
        if (userEmail.isEmpty()) return;

        FirebaseFirestore.getInstance().collection("users").document(userEmail).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String fullName = "User";
                    if (documentSnapshot.exists()) {
                        fullName = documentSnapshot.getString("name");
                    }

                    java.util.Map<String, Object> request = new java.util.HashMap<>();
                    request.put("userEmail", userEmail);
                    request.put("userName", fullName);
                    request.put("type", type);
                    request.put("status", "Pending Approval");
                    request.put("description", desc);
                    request.put("location", loc);
                    request.put("date", date);
                    request.put("time", tvSelectedTime.getText().toString());
                    request.put("timeMinutes", selectedMinutes);
                    request.put("slotHour", selectedMinutes != -1 ? selectedMinutes / 60 : -1);
                    request.put("phoneNumber", phone);
                    request.put("asap", asap);
                    request.put("emergency", isEmergency || isDanger);
                    request.put("timestamp", timestamp);

                    FirebaseFirestore.getInstance().collection("requests").add(request)
                            .addOnSuccessListener(documentReference -> {
                                NotificationUtils.INSTANCE.sendNotification(userEmail, "Request Submitted", "Your request for " + type + " has been submitted and is pending approval.");
                                showSuccessDialog();
                            })
                            .addOnFailureListener(e -> DialogUtils.INSTANCE.showErrorDialog(this, "Submission Failed", e.getMessage()));
                });
    }

    private void showSuccessDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_success);
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCancelable(false);

        android.widget.Button btnOk = dialog.findViewById(R.id.btn_success_ok);
        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }

    private void showInfoDialog(String title, String message) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_info);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        dialog.setCancelable(true);

        TextView tvTitle = dialog.findViewById(R.id.tv_info_title);
        TextView tvMessage = dialog.findViewById(R.id.tv_info_message);
        tvTitle.setText(title);
        tvMessage.setText(message);

        android.widget.Button btnOk = dialog.findViewById(R.id.btn_info_ok);
        btnOk.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void updateAddressText(double lat, double lon, boolean isGps) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressStr = address.getAddressLine(0);
                if (tvLocationText != null) {
                    tvLocationText.setText(addressStr);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
