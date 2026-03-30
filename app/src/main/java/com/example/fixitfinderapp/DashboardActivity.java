package com.example.fixitfinderapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.graphics.Typeface;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.adapters.ProviderServiceAdapter;
import com.example.fixitfinderapp.models.ProviderServiceItem;
import com.example.fixitfinderapp.notifications.ReminderScheduler;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.firestore.Query;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import android.location.Address;
import android.location.Geocoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends BaseSwipeActivity implements OnMapReadyCallback {

    private static final int REQ_NOTIFICATIONS = 1102;
    private TextView tvNewServicesTitle;
    private TextView tvNewServicesCount;
    private TextView tvUpcomingServicesTitle;
    private TextView tvUpcomingServicesCount;
    private final List<ProviderServiceItem> services = new ArrayList<>();
    private ProviderServiceAdapter serviceAdapter;
    private int pendingCount = 0;
    private int upcomingCount = 0;
    private GoogleMap providerMap;
    private Double providerLat;
    private Double providerLng;
    private String providerAddress;
    private String providerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_homepage);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        ImageView ivCompanyLogo = findViewById(R.id.ivCompanyLogo);
        TextView tvCompanyName = findViewById(R.id.tvCompanyName);
        TextView tvVerifiedStatus = findViewById(R.id.tvVerifiedStatus);
        tvVerifiedStatus.setOnClickListener(v ->
                startActivity(new Intent(this, ProviderDocumentsActivity.class)));
        TextView tvWhyReason = findViewById(R.id.tvWhyReason);
        TextView tvLocation = findViewById(R.id.tvLocation);
        TextView tvTotalBookings = findViewById(R.id.tvTotalBookings);
        TextView tvAvgResponseTime = findViewById(R.id.tvAvgResponseTime);
        Button btnServiceStatus = findViewById(R.id.btnServiceStatus);
        Button btnEditServices = findViewById(R.id.btnEditServices);
        RecyclerView recyclerViewServices = findViewById(R.id.recyclerViewServices);
        tvNewServicesTitle = findViewById(R.id.tvNewServicesTitle);
        tvNewServicesCount = findViewById(R.id.tvNewServicesCount);
        tvUpcomingServicesTitle = findViewById(R.id.tvUpcomingServicesTitle);
        tvUpcomingServicesCount = findViewById(R.id.tvUpcomingServicesCount);

        setupServicesRecycler(recyclerViewServices);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapProvider);
        if (mapFragment != null) {
            MapsInitializer.initialize(getApplicationContext());
            mapFragment.getMapAsync(this);
        }

        if (user != null) {
            FirebaseFirestore.getInstance()
                    .collection("providers")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        String companyName = doc.getString("fullName");
                        String logoUri = doc.getString("logoUri");
                        String reason = doc.getString("whyChooseUs");
                        String address = doc.getString("address");
                        providerAddress = address;
                        providerName = companyName;
                        providerLat = doc.getDouble("lat");
                        providerLng = doc.getDouble("lng");
                        updateProviderMap();
                        Boolean verified = doc.getBoolean("verified");
                        Boolean serviceActive = doc.getBoolean("serviceActive");

                        tvCompanyName.setText(!TextUtils.isEmpty(companyName)
                                ? companyName
                                : "Company Name");
                        tvWhyReason.setText(!TextUtils.isEmpty(reason)
                                ? reason
                                : "Provide quality service at an affordable price.");
                        if (verified != null && verified) {
                            tvVerifiedStatus.setText("Fully Verified");
                            tvVerifiedStatus.setTextColor(0xFF4CAF50);
                        } else {
                            tvVerifiedStatus.setText("Not Verified");
                            tvVerifiedStatus.setTextColor(0xFFF44336);
                        }
                        if (!TextUtils.isEmpty(address)) {
                            tvLocation.setText(address);
                        }
                        updateServiceStatusButton(btnServiceStatus, serviceActive != null && serviceActive);
                        if (!TextUtils.isEmpty(logoUri)) {
                            ImageLoader.loadProfile(ivCompanyLogo, logoUri, android.R.drawable.ic_menu_myplaces);
                        }
                    });
        }

        findViewById(R.id.cardNewServices).setOnClickListener(v -> {
            markSeen("pending");
            openJobs("pending", "New Service Offerings");
        });
        findViewById(R.id.cardUpcomingServices).setOnClickListener(v -> {
            markSeen("upcoming");
            openJobs("upcoming", "Upcoming Services");
        });
        findViewById(R.id.cardTodayEarnings).setOnClickListener(v ->
                openEarnings("today", "Today's Earnings"));
        findViewById(R.id.cardMonthlyEarnings).setOnClickListener(v ->
                openEarnings("month", "Monthly Earnings"));

        btnEditServices.setOnClickListener(v ->
                startActivity(new Intent(this, ProviderServicesActivity.class)));

        btnServiceStatus.setOnClickListener(v -> toggleServiceStatus(btnServiceStatus));

        if (user != null) {
            loadProviderStats(user.getUid(), tvTotalBookings, tvAvgResponseTime);
            loadDashboardCounts(user.getUid());
            loadProviderServices(user.getUid());
        }

        NavigationHelper.setupBottomNav(this, R.id.nav_home);
    }

    @Override
    protected void onStart() {
        super.onStart();
        NavigationHelper.ensureLoggedIn(this);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            ReminderScheduler.scheduleProviderAcceptedReminders(this, user.getUid());
            requestNotificationPermission();
            com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigation =
                    findViewById(R.id.bottomNavigation);
            NavigationHelper.updateMessageBadge(this, bottomNavigation);
        }
    }

    private void openJobs(String mode, String title) {
        Intent intent = new Intent(this, ProviderJobListActivity.class);
        intent.putExtra("mode", mode);
        intent.putExtra("title", title);
        startActivity(intent);
    }

    private void openEarnings(String mode, String title) {
        Intent intent = new Intent(this, ProviderEarningsListActivity.class);
        intent.putExtra("mode", mode);
        intent.putExtra("title", title);
        startActivity(intent);
    }

    private void updateServiceStatusButton(Button button, boolean active) {
        if (active) {
            button.setText("ACTIVE");
            button.setTextColor(0xFFFFFFFF);
            button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.color_primary));
        } else {
            button.setText("INACTIVE");
            button.setTextColor(0xFFFFFFFF);
            button.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));
        }
        button.setTag(active);
    }

    private void toggleServiceStatus(Button button) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean current = button.getTag() instanceof Boolean && (Boolean) button.getTag();
        boolean next = !current;
        updateServiceStatusButton(button, next);
        FirebaseFirestore.getInstance()
                .collection("providers")
                .document(user.getUid())
                .update("serviceActive", next);
    }

    private void loadProviderStats(String providerId, TextView tvTotal, TextView tvAvg) {
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereEqualTo("providerId", providerId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    tvTotal.setText(String.valueOf(snapshot.size()));

                    long totalMillis = 0L;
                    int count = 0;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        Long createdAt = doc.getLong("createdAt");
                        Long respondedAt = doc.getLong("respondedAt");
                        com.google.firebase.Timestamp acceptedAt = doc.getTimestamp("acceptedAt");
                        if (respondedAt == null && acceptedAt != null) {
                            respondedAt = acceptedAt.toDate().getTime();
                        }
                        if (createdAt != null && respondedAt != null && respondedAt >= createdAt) {
                            totalMillis += (respondedAt - createdAt);
                            count++;
                        }
                    }
                    if (count == 0) {
                        tvAvg.setText("N/A");
                        return;
                    }
                    double avgHours = totalMillis / (double) count / 3600000d;
                    tvAvg.setText(String.format(java.util.Locale.US, "%.2f hrs", avgHours));
                })
                .addOnFailureListener(e -> {
                    tvTotal.setText("0");
                    tvAvg.setText("N/A");
                });
    }

    private void loadDashboardCounts(String providerId) {
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereEqualTo("providerId", providerId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snapshot -> {
                    pendingCount = snapshot.size();
                    updateDashboardCountUi("pending");
                });

        FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereEqualTo("providerId", providerId)
                .whereIn("status",
                        java.util.Arrays.asList("accepted", "on process", "on-process", "ongoing"))
                .get()
                .addOnSuccessListener(snapshot -> {
                    upcomingCount = snapshot.size();
                    updateDashboardCountUi("upcoming");
                });
    }

    private void updateDashboardCountUi(String mode) {
        android.content.SharedPreferences prefs =
                getSharedPreferences("dashboard_prefs", MODE_PRIVATE);
        int lastSeenPending = prefs.getInt("last_seen_pending", 0);
        int lastSeenUpcoming = prefs.getInt("last_seen_upcoming", 0);

        if ("pending".equals(mode) && tvNewServicesCount != null && tvNewServicesTitle != null) {
            tvNewServicesCount.setText(String.valueOf(pendingCount));
            boolean unread = pendingCount > lastSeenPending;
            tvNewServicesTitle.setTypeface(null, unread ? Typeface.BOLD : Typeface.NORMAL);
            tvNewServicesCount.setTypeface(null, unread ? Typeface.BOLD : Typeface.NORMAL);
        } else if ("upcoming".equals(mode)
                && tvUpcomingServicesCount != null
                && tvUpcomingServicesTitle != null) {
            tvUpcomingServicesCount.setText(String.valueOf(upcomingCount));
            boolean unread = upcomingCount > lastSeenUpcoming;
            tvUpcomingServicesTitle.setTypeface(null, unread ? Typeface.BOLD : Typeface.NORMAL);
            tvUpcomingServicesCount.setTypeface(null, unread ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private void markSeen(String mode) {
        android.content.SharedPreferences prefs =
                getSharedPreferences("dashboard_prefs", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        if ("pending".equals(mode)) {
            editor.putInt("last_seen_pending", pendingCount);
            if (tvNewServicesTitle != null && tvNewServicesCount != null) {
                tvNewServicesTitle.setTypeface(null, Typeface.NORMAL);
                tvNewServicesCount.setTypeface(null, Typeface.NORMAL);
            }
        } else if ("upcoming".equals(mode)) {
            editor.putInt("last_seen_upcoming", upcomingCount);
            if (tvUpcomingServicesTitle != null && tvUpcomingServicesCount != null) {
                tvUpcomingServicesTitle.setTypeface(null, Typeface.NORMAL);
                tvUpcomingServicesCount.setTypeface(null, Typeface.NORMAL);
            }
        }
        editor.apply();
    }

    private void setupServicesRecycler(RecyclerView recyclerViewServices) {
        if (recyclerViewServices == null) {
            return;
        }
        recyclerViewServices.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        serviceAdapter = new ProviderServiceAdapter(services, false, false, null, null);
        recyclerViewServices.setAdapter(serviceAdapter);
    }

    private void loadProviderServices(String providerId) {
        FirebaseFirestore.getInstance()
                .collection("providers")
                .document(providerId)
                .collection("services")
                .get()
                .addOnSuccessListener(snapshot -> {
                    services.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        String imageUri = doc.getString("imageUri");
                        String description = doc.getString("description");
                        double price = 0d;
                        Object priceObj = doc.get("price");
                        if (priceObj instanceof Number) {
                            price = ((Number) priceObj).doubleValue();
                        }
                        services.add(new ProviderServiceItem(id, name, price, imageUri, description));
                    }
                    if (serviceAdapter != null) {
                        serviceAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load services: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    @Override
    public void onMapReady(GoogleMap map) {
        providerMap = map;
        updateProviderMap();
    }

    private void updateProviderMap() {
        if (providerMap == null) {
            return;
        }
        LatLng fallbackCenter = new LatLng(14.5995, 120.9842);
        providerMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fallbackCenter, 12f));
        if (providerLat != null && providerLng != null) {
            LatLng pos = new LatLng(providerLat, providerLng);
            providerMap.clear();
            providerMap.addMarker(new MarkerOptions().position(pos).title(
                    !TextUtils.isEmpty(providerName) ? providerName : "Provider"));
            providerMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 14f));
            return;
        }
        if (!TextUtils.isEmpty(providerAddress)) {
            geocodeAddress(providerAddress);
        }
    }

    private void geocodeAddress(String address) {
        new Thread(() -> {
            LatLng result = null;
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> results = geocoder.getFromLocationName(address, 1);
                if (results != null && !results.isEmpty()) {
                    Address addr = results.get(0);
                    result = new LatLng(addr.getLatitude(), addr.getLongitude());
                }
            } catch (Exception ignored) { }
            LatLng finalResult = result;
            runOnUiThread(() -> {
                if (providerMap == null || finalResult == null) {
                    return;
                }
                providerMap.clear();
                providerMap.addMarker(new MarkerOptions().position(finalResult).title(
                        !TextUtils.isEmpty(providerName) ? providerName : "Provider"));
                providerMap.animateCamera(CameraUpdateFactory.newLatLngZoom(finalResult, 14f));
            });
        }).start();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            refreshFcmToken();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            refreshFcmToken();
            return;
        }
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                REQ_NOTIFICATIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIFICATIONS
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            refreshFcmToken();
        }
    }

    private void refreshFcmToken() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (TextUtils.isEmpty(token)) {
                        return;
                    }
                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                    data.put("fcmToken", token);
                    FirebaseFirestore.getInstance()
                            .collection("providers")
                            .document(user.getUid())
                            .set(data, SetOptions.merge());
                });
    }
}
