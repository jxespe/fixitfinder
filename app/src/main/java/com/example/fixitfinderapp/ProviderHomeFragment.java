package com.example.fixitfinderapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.adapters.ProviderServiceAdapter;
import com.example.fixitfinderapp.models.ProviderServiceItem;
import com.example.fixitfinderapp.notifications.ReminderScheduler;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProviderHomeFragment extends Fragment implements OnMapReadyCallback {

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_provider_homepage, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        ImageView ivCompanyLogo = view.findViewById(R.id.ivCompanyLogo);
        TextView tvCompanyName = view.findViewById(R.id.tvCompanyName);
        TextView tvVerifiedStatus = view.findViewById(R.id.tvVerifiedStatus);
        tvVerifiedStatus.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProviderDocumentsActivity.class)));
        TextView tvWhyReason = view.findViewById(R.id.tvWhyReason);
        TextView tvLocation = view.findViewById(R.id.tvLocation);
        TextView tvTotalBookings = view.findViewById(R.id.tvTotalBookings);
        TextView tvAvgResponseTime = view.findViewById(R.id.tvAvgResponseTime);
        Button btnServiceStatus = view.findViewById(R.id.btnServiceStatus);
        Button btnEditServices = view.findViewById(R.id.btnEditServices);
        RecyclerView recyclerViewServices = view.findViewById(R.id.recyclerViewServices);
        tvNewServicesTitle = view.findViewById(R.id.tvNewServicesTitle);
        tvNewServicesCount = view.findViewById(R.id.tvNewServicesCount);
        tvUpcomingServicesTitle = view.findViewById(R.id.tvUpcomingServicesTitle);
        tvUpcomingServicesCount = view.findViewById(R.id.tvUpcomingServicesCount);

        setupServicesRecycler(recyclerViewServices);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapProvider);
        if (mapFragment != null) {
            MapsInitializer.initialize(requireContext().getApplicationContext());
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

        view.findViewById(R.id.cardNewServices).setOnClickListener(v -> {
            markSeen("pending");
            openJobs("pending", "New Service Offerings");
        });
        view.findViewById(R.id.cardUpcomingServices).setOnClickListener(v -> {
            markSeen("upcoming");
            openJobs("upcoming", "Upcoming Services");
        });
        view.findViewById(R.id.cardTodayEarnings).setOnClickListener(v ->
                openEarnings("today", "Today's Earnings"));
        view.findViewById(R.id.cardMonthlyEarnings).setOnClickListener(v ->
                openEarnings("month", "Monthly Earnings"));

        btnEditServices.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProviderServicesActivity.class)));

        btnServiceStatus.setOnClickListener(v -> toggleServiceStatus(btnServiceStatus));

        if (user != null) {
            loadProviderStats(user.getUid(), tvTotalBookings, tvAvgResponseTime);
            loadDashboardCounts(user.getUid());
            loadProviderServices(user.getUid());
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            ReminderScheduler.scheduleProviderAcceptedReminders(requireContext(), user.getUid());
            requestNotificationPermission();
        }
    }

    private void openJobs(String mode, String title) {
        Intent intent = new Intent(requireContext(), ProviderJobListActivity.class);
        intent.putExtra("mode", mode);
        intent.putExtra("title", title);
        startActivity(intent);
    }

    private void openEarnings(String mode, String title) {
        Intent intent = new Intent(requireContext(), ProviderEarningsListActivity.class);
        intent.putExtra("mode", mode);
        intent.putExtra("title", title);
        startActivity(intent);
    }

    private void updateServiceStatusButton(Button button, boolean active) {
        if (active) {
            button.setText("ACTIVE");
            button.setTextColor(0xFFFFFFFF);
            button.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.color_primary));
        } else {
            button.setText("INACTIVE");
            button.setTextColor(0xFFFFFFFF);
            button.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
        }
        button.setTag(active);
    }

    private void toggleServiceStatus(Button button) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        boolean isActive = button.getTag() instanceof Boolean && (Boolean) button.getTag();
        boolean newStatus = !isActive;
        button.setEnabled(false);
        FirebaseFirestore.getInstance()
                .collection("providers")
                .document(user.getUid())
                .set(java.util.Collections.singletonMap("serviceActive", newStatus), SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    button.setEnabled(true);
                    updateServiceStatusButton(button, newStatus);
                })
                .addOnFailureListener(e -> {
                    button.setEnabled(true);
                    Toast.makeText(requireContext(), "Failed to update status", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupServicesRecycler(RecyclerView recyclerView) {
        serviceAdapter = new ProviderServiceAdapter(services, false, false, null, null);
        recyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(serviceAdapter);
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
                });
    }

    private void loadProviderStats(String providerId, TextView tvTotalBookings, TextView tvAvgResponseTime) {
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereEqualTo("providerId", providerId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int total = snapshot.size();
                    tvTotalBookings.setText(String.valueOf(total));
                });

        FirebaseFirestore.getInstance()
                .collection("providers")
                .document(providerId)
                .get()
                .addOnSuccessListener(doc -> {
                    Double avg = doc.getDouble("avgResponseTime");
                    if (avg == null) {
                        tvAvgResponseTime.setText("0 min");
                        return;
                    }
                    tvAvgResponseTime.setText(String.format(Locale.US, "%.0f min", avg));
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
                    updateCounts();
                });
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereEqualTo("providerId", providerId)
                .whereIn("status", java.util.Arrays.asList("accepted", "on process", "on-process", "ongoing"))
                .get()
                .addOnSuccessListener(snapshot -> {
                    upcomingCount = snapshot.size();
                    updateCounts();
                });
    }

    private void updateCounts() {
        if (tvNewServicesCount != null) {
            tvNewServicesCount.setText(String.valueOf(pendingCount));
            tvNewServicesCount.setTypeface(null, pendingCount > 0 ? Typeface.BOLD : Typeface.NORMAL);
        }
        if (tvUpcomingServicesCount != null) {
            tvUpcomingServicesCount.setText(String.valueOf(upcomingCount));
            tvUpcomingServicesCount.setTypeface(null, upcomingCount > 0 ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private void markSeen(String key) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("providers")
                .document(user.getUid())
                .set(java.util.Collections.singletonMap("seen_" + key, true), SetOptions.merge());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
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
                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                List<Address> results = geocoder.getFromLocationName(address, 1);
                if (results != null && !results.isEmpty()) {
                    Address addr = results.get(0);
                    result = new LatLng(addr.getLatitude(), addr.getLongitude());
                }
            } catch (Exception ignored) { }
            LatLng finalResult = result;
            requireActivity().runOnUiThread(() -> {
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
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            refreshFcmToken();
            return;
        }
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
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
