package com.example.fixitfinderapp;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.adapters.BookingHistoryAdapter;
import com.example.fixitfinderapp.adapters.UserReviewAdapter;
import com.example.fixitfinderapp.models.BookingHistoryItem;
import com.example.fixitfinderapp.models.UserReviewItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends BaseSwipeActivity {

    private final List<BookingHistoryItem> historyItems = new ArrayList<>();
    private final List<UserReviewItem> reviewItems = new ArrayList<>();
    private BookingHistoryAdapter historyAdapter;
    private UserReviewAdapter reviewAdapter;
    private TextView tvEmpty;
    private RecyclerView recycler;
    private FirebaseUser user;
    private boolean showingHistory = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        user = FirebaseAuth.getInstance().getCurrentUser();
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        TextView tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvHeaderTitle.setText("Booking History");

        MaterialButton btnTabHistory = findViewById(R.id.btnTabHistory);
        MaterialButton btnTabReview = findViewById(R.id.btnTabReview);
        applyHistoryReviewTabStyle(true);
        if (btnTabHistory != null) {
            btnTabHistory.setOnClickListener(v -> {
                if (!showingHistory) {
                    applyHistoryReviewTabStyle(true);
                    showHistoryTab();
                }
            });
        }
        if (btnTabReview != null) {
            btnTabReview.setOnClickListener(v -> {
                if (showingHistory) {
                    applyHistoryReviewTabStyle(false);
                    showReviewTab();
                }
            });
        }

        recycler = findViewById(R.id.recyclerViewHistory);
        tvEmpty = findViewById(R.id.tvEmptyHistory);
        historyAdapter = new BookingHistoryAdapter(historyItems);
        reviewAdapter = new UserReviewAdapter(reviewItems);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(historyAdapter);

        loadHistory();

        NavigationHelper.setupBottomNav(this, R.id.nav_history);
    }

    private void applyHistoryReviewTabStyle(boolean historySelected) {
        MaterialButton btnHistory = findViewById(R.id.btnTabHistory);
        MaterialButton btnReview = findViewById(R.id.btnTabReview);
        if (btnHistory == null || btnReview == null) {
            return;
        }
        int active = ContextCompat.getColor(this, R.color.color_primary);
        int inactive = ContextCompat.getColor(this, R.color.history_tab_inactive);
        int white = ContextCompat.getColor(this, R.color.white);
        btnHistory.setBackgroundTintList(ColorStateList.valueOf(historySelected ? active : inactive));
        btnReview.setBackgroundTintList(ColorStateList.valueOf(historySelected ? inactive : active));
        btnHistory.setTextColor(white);
        btnReview.setTextColor(white);
    }

    @Override
    protected void onStart() {
        super.onStart();
        NavigationHelper.ensureLoggedIn(this);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        NavigationHelper.updateMessageBadge(this, bottomNavigation);
    }

    private void showHistoryTab() {
        showingHistory = true;
        recycler.setAdapter(historyAdapter);
        tvEmpty.setText("No bookings yet.");
        loadHistory();
    }

    private void showReviewTab() {
        showingHistory = false;
        recycler.setAdapter(reviewAdapter);
        tvEmpty.setText("You have not reviewed any providers yet.");
        loadReviews();
    }

    private void loadHistory() {
        if (user == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    historyItems.clear();
                    snapshot.getDocuments().forEach(doc -> {
                        String bookingId = doc.getId();
                        String providerName = doc.getString("providerName");
                        String serviceName = doc.getString("serviceName");
                        String serviceDescription = doc.getString("serviceDescription");
                        if (TextUtils.isEmpty(serviceDescription)) {
                            serviceDescription = serviceName;
                        }
                        String priceText = formatPrice(doc.get("servicePrice"));
                        String title = !TextUtils.isEmpty(providerName) ? providerName :
                                (!TextUtils.isEmpty(serviceName) ? serviceName : "Service Booking");

                        String status = displayStatus(doc.getString("status"));
                        String payment = displayPayment(doc.getString("paymentStatus"));
                        String logoUri = pickLogoUri(doc.getString("providerLogoUri"),
                                doc.getString("logoUri"));
                        String dateText = formatDate(doc.get("scheduledAt"), doc.get("createdAt"));
                        long sortTimestamp = pickSortTimestamp(doc.get("scheduledAt"), doc.get("createdAt"));
                        historyItems.add(new BookingHistoryItem(
                                bookingId,
                                title,
                                dateText,
                                status,
                                payment,
                                logoUri,
                                "Job: " + valueOrUnknown(serviceDescription),
                                "Price: " + valueOrUnknown(priceText),
                                sortTimestamp
                        ));
                    });
                    historyItems.sort((a, b) -> Long.compare(b.sortTimestamp, a.sortTimestamp));
                    historyAdapter.notifyDataSetChanged();
                    if (showingHistory) {
                        tvEmpty.setText("No bookings yet.");
                        tvEmpty.setVisibility(historyItems.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    tvEmpty.setText("Unable to load history: " + e.getMessage());
                    tvEmpty.setVisibility(View.VISIBLE);
                });
    }

    private void loadReviews() {
        if (user == null) {
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    reviewItems.clear();
                    snapshot.getDocuments().forEach(doc -> {
                        Integer rating = readRating(doc.get("userRating"));
                        if (rating == null || rating <= 0) {
                            return;
                        }
                        String bookingId = doc.getId();
                        String providerName = doc.getString("providerName");
                        if (TextUtils.isEmpty(providerName)) {
                            providerName = "Provider";
                        }
                        String providerId = doc.getString("providerId");
                        String providerAddress = doc.getString("providerAddress");
                        String serviceCategory = doc.getString("serviceCategory");
                        String userReviewText = doc.getString("userReviewText");
                        String logoUri = pickLogoUri(doc.getString("providerLogoUri"),
                                doc.getString("logoUri"));
                        long sortTs = pickSortTimestamp(doc.get("userRatedAt"), doc.get("createdAt"));
                        reviewItems.add(new UserReviewItem(
                                bookingId,
                                providerId,
                                providerName,
                                providerAddress,
                                serviceCategory,
                                rating,
                                userReviewText,
                                logoUri,
                                sortTs
                        ));
                    });
                    reviewItems.sort((a, b) -> Long.compare(b.sortTimestamp, a.sortTimestamp));
                    reviewAdapter.notifyDataSetChanged();
                    if (!showingHistory) {
                        tvEmpty.setText("You have not reviewed any providers yet.");
                        tvEmpty.setVisibility(reviewItems.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    tvEmpty.setText("Unable to load reviews: " + e.getMessage());
                    tvEmpty.setVisibility(View.VISIBLE);
                });
    }

    private static Integer readRating(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Long) {
            return ((Long) raw).intValue();
        }
        if (raw instanceof Integer) {
            return (Integer) raw;
        }
        if (raw instanceof Double) {
            return ((Double) raw).intValue();
        }
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        return null;
    }

    private String displayStatus(String raw) {
        if (TextUtils.isEmpty(raw)) {
            return "Pending";
        }
        String normalized = raw.replace("_", " ").trim();
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private String displayPayment(String raw) {
        return TextUtils.isEmpty(raw) ? "On-hold" : raw;
    }

    private String pickLogoUri(String primary, String fallback) {
        if (!TextUtils.isEmpty(primary)) {
            return primary;
        }
        return !TextUtils.isEmpty(fallback) ? fallback : null;
    }

    private long pickSortTimestamp(Object scheduledAt, Object createdAt) {
        Date date = toDate(scheduledAt);
        if (date == null) {
            date = toDate(createdAt);
        }
        return date != null ? date.getTime() : 0L;
    }

    private String formatPrice(Object priceObj) {
        if (priceObj instanceof Number) {
            double price = ((Number) priceObj).doubleValue();
            if (price > 0) {
                return String.format(Locale.US, "\u20b1%.2f", price);
            }
        }
        return null;
    }

    private String valueOrUnknown(String value) {
        return TextUtils.isEmpty(value) ? "N/A" : value;
    }

    private String formatDate(Object scheduledAt, Object createdAt) {
        Date date = toDate(scheduledAt);
        if (date == null) {
            date = toDate(createdAt);
        }
        if (date == null) {
            return "Date not set";
        }
        return new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(date);
    }

    private Date toDate(Object value) {
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toDate();
        }
        if (value instanceof Long) {
            return new Date((Long) value);
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        return null;
    }
}
