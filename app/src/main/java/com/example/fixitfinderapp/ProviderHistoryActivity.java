package com.example.fixitfinderapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.adapters.BookingHistoryAdapter;
import com.example.fixitfinderapp.models.BookingHistoryItem;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.chip.Chip;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProviderHistoryActivity extends AppCompatActivity {

    private final List<BookingHistoryItem> items = new ArrayList<>();
    private BookingHistoryAdapter adapter;
    private TextView tvEmpty;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        user = FirebaseAuth.getInstance().getCurrentUser();
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        TextView tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvHeaderTitle.setText("Bookings Received");

        RecyclerView recycler = findViewById(R.id.recyclerViewHistory);
        tvEmpty = findViewById(R.id.tvEmptyHistory);
        adapter = new BookingHistoryAdapter(items);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        ChipGroup chipGroup = findViewById(R.id.chipGroupFilters);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            String filter = "all";
            if (!checkedIds.isEmpty()) {
                int id = checkedIds.get(0);
                filter = readFilterTag(id);
            }
            loadHistory(filter);
        });

        loadHistory("all");

        NavigationHelper.setupBottomNav(this, R.id.nav_history);
    }

    @Override
    protected void onStart() {
        super.onStart();
        NavigationHelper.ensureLoggedIn(this);
    }

    private String readFilterTag(int chipId) {
        Chip chip = findViewById(chipId);
        if (chip == null || chip.getTag() == null) {
            return "all";
        }
        return chip.getTag().toString();
    }

    private void loadHistory(String statusFilter) {
        if (user == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Query query = FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereEqualTo("providerId", user.getUid());
        query.get()
                .addOnSuccessListener(snapshot -> {
                    items.clear();
                    snapshot.getDocuments().forEach(doc -> {
                        String bookingId = doc.getId();
                        String customerName = doc.getString("customerName");
                        String serviceName = doc.getString("serviceName");
                        String serviceDescription = doc.getString("serviceDescription");
                        if (TextUtils.isEmpty(serviceDescription)) {
                            serviceDescription = serviceName;
                        }
                        String priceText = formatPrice(doc.get("servicePrice"));
                        String title = !TextUtils.isEmpty(customerName) ? customerName :
                                (!TextUtils.isEmpty(serviceName) ? serviceName : "Customer Booking");

                        String status = displayStatus(doc.getString("status"));
                        if (!matchesFilter(doc.getString("status"), statusFilter)) {
                            return;
                        }
                        String payment = displayPayment(doc.getString("paymentStatus"));
                        String logoUri = pickLogoUri(doc.getString("customerLogoUri"),
                                doc.getString("logoUri"));
                        String dateText = formatDate(doc.get("scheduledAt"), doc.get("createdAt"));
                        long sortTimestamp = pickSortTimestamp(doc.get("scheduledAt"), doc.get("createdAt"));
                        items.add(new BookingHistoryItem(
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
                    items.sort((a, b) -> Long.compare(b.sortTimestamp, a.sortTimestamp));
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(items.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                })
                .addOnFailureListener(e -> {
                    tvEmpty.setText("Unable to load history: " + e.getMessage());
                    tvEmpty.setVisibility(android.view.View.VISIBLE);
                });
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

    private boolean matchesFilter(String status, String filter) {
        if (TextUtils.isEmpty(filter) || "all".equalsIgnoreCase(filter)) {
            return true;
        }
        String normalizedStatus = status == null ? "" : status.replace("_", " ").trim().toLowerCase(Locale.US);
        String normalizedFilter = filter.replace("_", " ").trim().toLowerCase(Locale.US);
        if ("on process".equals(normalizedFilter)) {
            return normalizedStatus.equals("on process")
                    || normalizedStatus.equals("on-process")
                    || normalizedStatus.equals("ongoing");
        }
        return normalizedStatus.equals(normalizedFilter);
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
        return null;
    }
}
