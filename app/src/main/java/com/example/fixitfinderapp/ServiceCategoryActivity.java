package com.example.fixitfinderapp;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.adapters.ServiceProviderAdapter;
import com.example.fixitfinderapp.models.ServiceProviderProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import android.util.Log;
import android.location.Address;
import android.location.Geocoder;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

public class ServiceCategoryActivity extends BaseSwipeActivity {

    public static final String EXTRA_CATEGORY_NAME = "category_name";
    private static final String TAG = "ServiceCategoryActivity";
    private final List<ServiceProviderProfile> providers = new ArrayList<>();
    private ServiceProviderAdapter adapter;
    private TextView tvEmptyProviders;
    private Double userLat;
    private Double userLng;
    private FusedLocationProviderClient locationClient;
    private static final int REQ_LOCATION = 301;
    private static final String PREF_PRIVACY = "privacy_settings";
    private static final String KEY_SHARE_LOCATION = "share_location";
    private static final java.util.concurrent.ExecutorService GEO_EXECUTOR =
            java.util.concurrent.Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_category);

        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView tvCategoryTitle = findViewById(R.id.tvCategoryTitle);
        TextView tvCategorySubtitle = findViewById(R.id.tvCategorySubtitle);
        tvEmptyProviders = findViewById(R.id.tvEmptyProviders);
        RecyclerView recyclerProviders = findViewById(R.id.recyclerProviders);

        String name = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);
        if (name == null || name.trim().isEmpty()) {
            name = "Service Category";
        }

        tvCategoryTitle.setText(name);
        tvCategorySubtitle.setText("Explore " + name + " services");
        setupProviderList(recyclerProviders);
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        loadProviders(name);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupProviderList(RecyclerView recyclerProviders) {
        adapter = new ServiceProviderAdapter(providers);
        recyclerProviders.setLayoutManager(new LinearLayoutManager(this));
        recyclerProviders.setAdapter(adapter);
    }

    private void loadProviders(String categoryName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            tvEmptyProviders.setVisibility(android.view.View.VISIBLE);
            tvEmptyProviders.setText("Please log in to view providers.");
            return;
        }
        Log.d(TAG, "Loading providers for uid=" + user.getUid());
        String normalized = categoryName == null ? "" : categoryName.toLowerCase(java.util.Locale.US).trim();
        boolean shareLocation =
                getSharedPreferences(PREF_PRIVACY, MODE_PRIVATE)
                        .getBoolean(KEY_SHARE_LOCATION, true);
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    userLat = doc.getDouble("lat");
                    userLng = doc.getDouble("lng");
                    if (adapter != null) {
                        adapter.setUserLocation(userLat, userLng);
                    }
                    if (shareLocation) {
                        requestDeviceLocation(user);
                    }
                    loadFromCollections(normalized, categoryName);
                })
                .addOnFailureListener(e -> {
                    if (shareLocation) {
                        requestDeviceLocation(user);
                    }
                    loadFromCollections(normalized, categoryName);
                });
    }

    private void requestDeviceLocation(FirebaseUser user) {
        if (user == null) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
            return;
        }
        fetchDeviceLocation(user);
    }

    private void fetchDeviceLocation(FirebaseUser user) {
        if (locationClient == null || user == null) {
            return;
        }
        locationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        return;
                    }
                    userLat = location.getLatitude();
                    userLng = location.getLongitude();
                    if (adapter != null) {
                        adapter.setUserLocation(userLat, userLng);
                    }
                    java.util.Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("lat", userLat);
                    updates.put("lng", userLng);
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(user.getUid())
                            .set(updates, com.google.firebase.firestore.SetOptions.merge());
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            fetchDeviceLocation(user);
        }
    }

    private void loadFromCollections(String normalized, String originalCategory) {
        java.util.Map<String, com.google.firebase.firestore.DocumentSnapshot> merged =
                new java.util.LinkedHashMap<>();
        FirebaseFirestore.getInstance()
                .collection("providers")
                .whereEqualTo("serviceCategoryLower", normalized)
                .get()
                .addOnSuccessListener(providerSnap -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : providerSnap.getDocuments()) {
                        merged.put(doc.getId(), doc);
                    }
                    if (merged.isEmpty()) {
                        fallbackCaseSensitive(originalCategory, merged);
                        return;
                    }
                    applyProviders(new java.util.ArrayList<>(merged.values()));
                })
                .addOnFailureListener(this::showProviderLoadError);
    }

    private void fallbackCaseSensitive(String originalCategory,
                                       java.util.Map<String, com.google.firebase.firestore.DocumentSnapshot> merged) {
        FirebaseFirestore.getInstance()
                .collection("providers")
                .whereEqualTo("serviceCategory", originalCategory)
                .get()
                .addOnSuccessListener(providerSnap -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : providerSnap.getDocuments()) {
                        merged.put(doc.getId(), doc);
                    }
                    if (merged.isEmpty()) {
                        applyProviders(new java.util.ArrayList<>(merged.values()));
                        return;
                    }
                    applyProviders(new java.util.ArrayList<>(merged.values()));
                })
                .addOnFailureListener(this::showProviderLoadError);
    }

    private void applyProviders(java.util.List<com.google.firebase.firestore.DocumentSnapshot> docs) {
        providers.clear();
        for (com.google.firebase.firestore.DocumentSnapshot doc : docs) {
            Boolean hideProfile = doc.getBoolean("hideProfile");
            if (hideProfile != null && hideProfile) {
                continue;
            }
            String providerId = doc.getId();
            String fullName = doc.getString("fullName");
            String address = doc.getString("address");
            Double lat = doc.getDouble("lat");
            Double lng = doc.getDouble("lng");
            Boolean shareLocation = doc.getBoolean("shareLocation");
            boolean isLocationVisible = shareLocation == null || shareLocation;
            if (!isLocationVisible) {
                address = "Location hidden";
                lat = null;
                lng = null;
            }
            String category = doc.getString("serviceCategory");
            String logoUri = doc.getString("logoUri");
            ServiceProviderProfile profile =
                    new ServiceProviderProfile(providerId, fullName, category, address, logoUri, lat, lng);
            providers.add(profile);
            if (lat == null && lng == null && isLocationVisible
                    && address != null && !address.isEmpty()) {
                geocodeProviderAddress(profile, address);
            }
        }
        adapter.notifyDataSetChanged();
        boolean isEmpty = providers.isEmpty();
        if (isEmpty) {
            tvEmptyProviders.setText("No providers found for this category.");
            tvEmptyProviders.setVisibility(android.view.View.VISIBLE);
        } else {
            tvEmptyProviders.setVisibility(android.view.View.GONE);
        }
    }

    private void showProviderLoadError(Exception e) {
        Log.e(TAG, "Failed to load providers", e);
        tvEmptyProviders.setVisibility(android.view.View.VISIBLE);
        String message = "Unable to load providers: " + e.getMessage();
        if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
            com.google.firebase.firestore.FirebaseFirestoreException fe =
                    (com.google.firebase.firestore.FirebaseFirestoreException) e;
            if (fe.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                String uid = user != null ? user.getUid() : "null";
                message = "Permission denied. Auth uid=" + uid
                        + ". Check Firestore rules and App Check.";
            }
        }
        tvEmptyProviders.setText(message);
    }

    private void geocodeProviderAddress(ServiceProviderProfile profile, String address) {
        GEO_EXECUTOR.execute(() -> {
            Double lat = null;
            Double lng = null;
            try {
                Geocoder geocoder = new Geocoder(this, java.util.Locale.getDefault());
                java.util.List<Address> results = geocoder.getFromLocationName(address, 1);
                if (results != null && !results.isEmpty()) {
                    Address addr = results.get(0);
                    lat = addr.getLatitude();
                    lng = addr.getLongitude();
                }
            } catch (Exception ignored) { }
            Double finalLat = lat;
            Double finalLng = lng;
            runOnUiThread(() -> {
                if (finalLat == null || finalLng == null) {
                    return;
                }
                profile.setLatLng(finalLat, finalLng);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                java.util.Map<String, Object> updates = new java.util.HashMap<>();
                updates.put("lat", finalLat);
                updates.put("lng", finalLng);
                FirebaseFirestore.getInstance()
                        .collection("providers")
                        .document(profile.providerId)
                        .set(updates, com.google.firebase.firestore.SetOptions.merge());
            });
        });
    }
}
