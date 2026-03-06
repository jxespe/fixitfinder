package com.example.fixitfinderapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.graphics.Typeface;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.adapters.ProviderServiceAdapter;
import com.example.fixitfinderapp.models.ProviderServiceItem;
import com.example.fixitfinderapp.notifications.ReminderScheduler;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvNewServicesTitle;
    private TextView tvNewServicesCount;
    private TextView tvUpcomingServicesTitle;
    private TextView tvUpcomingServicesCount;
    private final List<ProviderServiceItem> services = new ArrayList<>();
    private ProviderServiceAdapter serviceAdapter;
    private int pendingCount = 0;
    private int upcomingCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_homepage);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        ImageView ivCompanyLogo = findViewById(R.id.ivCompanyLogo);
        TextView tvCompanyName = findViewById(R.id.tvCompanyName);
        TextView tvVerifiedStatus = findViewById(R.id.tvVerifiedStatus);
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
                            ImageLoader.load(ivCompanyLogo, logoUri, android.R.drawable.ic_menu_myplaces);
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
        serviceAdapter = new ProviderServiceAdapter(services, false, false, null);
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
}
