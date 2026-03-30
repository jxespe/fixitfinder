package com.example.fixitfinderapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.adapters.ProviderEarningsAdapter;
import com.example.fixitfinderapp.models.ProviderEarningItem;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProviderEarningsListActivity extends BaseSwipeActivity {

    private final List<ProviderEarningItem> items = new ArrayList<>();
    private ProviderEarningsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_earnings_list);

        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView tvTitle = findViewById(R.id.tvTitle);
        RecyclerView recyclerView = findViewById(R.id.recyclerEarnings);

        adapter = new ProviderEarningsAdapter(items);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        String title = getIntent().getStringExtra("title");
        String mode = getIntent().getStringExtra("mode");
        tvTitle.setText(!TextUtils.isEmpty(title) ? title : "Earnings");

        btnBack.setOnClickListener(v -> finish());
        loadEarnings(mode);
    }

    private void loadEarnings(String mode) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Query query = FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereEqualTo("providerId", user.getUid())
                .whereIn("status", Arrays.asList("finished", "completed"));

        query.get()
                .addOnSuccessListener(snapshot -> {
                    items.clear();
                    snapshot.getDocuments().forEach(doc -> {
                        Timestamp paymentAt = doc.getTimestamp("paymentAt");
                        long createdAt = doc.getLong("createdAt") != null
                                ? doc.getLong("createdAt")
                                : 0L;
                        Date paymentDate = paymentAt != null ? paymentAt.toDate() : new Date(createdAt);
                        if (!isInRange(paymentDate, mode)) {
                            return;
                        }

                        String bookingNumber = doc.getString("bookingNumber");
                        if (TextUtils.isEmpty(bookingNumber)) {
                            bookingNumber = doc.getId();
                        }
                        String paidBy = doc.getString("paidBy");
                        if (TextUtils.isEmpty(paidBy)) {
                            paidBy = doc.getString("bookedBy");
                        }
                        String jobDone = doc.getString("serviceCategory");
                        String paymentMethod = doc.getString("paymentMethod");

                        items.add(new ProviderEarningItem(
                                "Booking #: " + bookingNumber,
                                "Paid by: " + valueOrUnknown(paidBy),
                                "Payment date: " + valueOrUnknown(formatDate(paymentDate)),
                                "Job: " + valueOrUnknown(jobDone),
                                "Payment Method: " + valueOrUnknown(paymentMethod)
                        ));
                    });
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load earnings.", Toast.LENGTH_SHORT).show());
    }

    private boolean isInRange(Date date, String mode) {
        if (date == null || TextUtils.isEmpty(mode)) {
            return true;
        }
        Calendar calendar = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTime(date);

        if ("today".equals(mode)) {
            return calendar.get(Calendar.YEAR) == target.get(Calendar.YEAR)
                    && calendar.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR);
        }
        if ("month".equals(mode)) {
            return calendar.get(Calendar.YEAR) == target.get(Calendar.YEAR)
                    && calendar.get(Calendar.MONTH) == target.get(Calendar.MONTH);
        }
        return true;
    }

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US).format(date);
    }

    private String valueOrUnknown(String value) {
        return TextUtils.isEmpty(value) ? "N/A" : value;
    }
}
