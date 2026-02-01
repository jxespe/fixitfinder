package com.example.fixitfinderapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_homepage);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        ImageView ivCompanyLogo = findViewById(R.id.ivCompanyLogo);
        ImageView ivWhyLogo = findViewById(R.id.ivWhyLogo);
        TextView tvCompanyName = findViewById(R.id.tvCompanyName);
        TextView tvVerifiedStatus = findViewById(R.id.tvVerifiedStatus);
        TextView tvWhyReason = findViewById(R.id.tvWhyReason);
        TextView tvLocation = findViewById(R.id.tvLocation);
        TextView tvTotalBookings = findViewById(R.id.tvTotalBookings);
        TextView tvAvgResponseTime = findViewById(R.id.tvAvgResponseTime);
        Button btnServiceStatus = findViewById(R.id.btnServiceStatus);
        Button btnEditServices = findViewById(R.id.btnEditServices);

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
                            Uri uri = Uri.parse(logoUri);
                            ivCompanyLogo.setImageURI(uri);
                            ivWhyLogo.setImageURI(uri);
                        }
                    });
        }

        findViewById(R.id.cardNewServices).setOnClickListener(v ->
                openJobs("pending", "New Service Offerings"));
        findViewById(R.id.cardUpcomingServices).setOnClickListener(v ->
                openJobs("upcoming", "Upcoming Services"));
        findViewById(R.id.cardTodayEarnings).setOnClickListener(v ->
                openEarnings("today", "Today's Earnings"));
        findViewById(R.id.cardMonthlyEarnings).setOnClickListener(v ->
                openEarnings("month", "Monthly Earnings"));

        btnEditServices.setOnClickListener(v ->
                Toast.makeText(this, "Service management coming soon", Toast.LENGTH_SHORT).show());

        btnServiceStatus.setOnClickListener(v -> toggleServiceStatus(btnServiceStatus));

        if (user != null) {
            loadProviderStats(user.getUid(), tvTotalBookings, tvAvgResponseTime);
        }

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    return true;
                } else if (id == R.id.nav_history) {
                    startActivity(new Intent(this, ProviderHistoryActivity.class));
                    return true;
                } else if (id == R.id.nav_messages) {
                    Toast.makeText(this, "Messages coming soon", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.nav_settings) {
                    startActivity(new Intent(this, ProviderSettingsActivity.class));
                    return true;
                }
                return false;
            });
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
}
