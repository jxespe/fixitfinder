package com.example.fixitfinderapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.adapters.ProviderBookingAdapter;
import com.example.fixitfinderapp.models.ProviderBookingItem;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.FieldValue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProviderJobListActivity extends AppCompatActivity {

    private final List<ProviderBookingItem> items = new ArrayList<>();
    private ProviderBookingAdapter adapter;
    private String mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_job_list);

        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView tvTitle = findViewById(R.id.tvTitle);
        RecyclerView recyclerView = findViewById(R.id.recyclerBookings);

        mode = getIntent().getStringExtra("mode");
        adapter = new ProviderBookingAdapter(
                items,
                mode,
                new ProviderBookingAdapter.BookingActionListener() {
                    @Override
                    public void onAccept(ProviderBookingItem item) {
                        updateBookingStatus(item, "accepted");
                    }

                    @Override
                    public void onDecline(ProviderBookingItem item) {
                        updateBookingStatus(item, "declined");
                    }

                    @Override
                    public void onProcess(ProviderBookingItem item) {
                        updateBookingStatus(item, "on process");
                    }

                    @Override
                    public void onFinish(ProviderBookingItem item) {
                        updateBookingStatus(item, "finished");
                    }

                    @Override
                    public void onCancel(ProviderBookingItem item) {
                        updateBookingStatus(item, "cancelled");
                    }

                    @Override
                    public void onReschedule(ProviderBookingItem item) {
                        updateBookingStatus(item, "rescheduled");
                    }
                });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        String title = getIntent().getStringExtra("title");
        tvTitle.setText(!TextUtils.isEmpty(title) ? title : "Bookings");

        btnBack.setOnClickListener(v -> finish());
        loadBookings(mode);
    }

    private void loadBookings(String mode) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Query query = FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereEqualTo("providerId", user.getUid());

        if ("pending".equals(mode)) {
            query = query.whereEqualTo("status", "pending");
        } else if ("upcoming".equals(mode)) {
            query = query.whereIn("status",
                    Arrays.asList("accepted", "on process", "on-process", "ongoing"));
        }

        query.get()
                .addOnSuccessListener(snapshot -> {
                    items.clear();
                    snapshot.getDocuments().forEach(doc -> {
                        String bookingNumber = doc.getString("bookingNumber");
                        if (TextUtils.isEmpty(bookingNumber)) {
                            bookingNumber = doc.getId();
                        }
                        String bookedBy = doc.getString("bookedBy");
                        String location = doc.getString("providerAddress");
                        if (TextUtils.isEmpty(location)) {
                            location = doc.getString("address");
                        }
                        String bookedAt = formatMillis(doc.getLong("createdAt"));
                        String status = doc.getString("status");

                        Timestamp scheduledAt = doc.getTimestamp("scheduledAt");
                        String requiredAt = scheduledAt != null
                                ? formatDate(scheduledAt.toDate())
                                : doc.getString("dateKey");
                        String timeSlot = doc.getString("timeSlot");
                        if (!TextUtils.isEmpty(timeSlot)) {
                            requiredAt = requiredAt + " " + timeSlot;
                        }

                        items.add(new ProviderBookingItem(
                                doc.getId(),
                                "Booking #: " + bookingNumber,
                                "Booked by: " + valueOrUnknown(bookedBy),
                                "Booked at: " + valueOrUnknown(bookedAt),
                                "Required: " + valueOrUnknown(requiredAt),
                                "Location: " + valueOrUnknown(location),
                                status
                        ));
                    });
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load bookings.", Toast.LENGTH_SHORT).show());
    }

    private String formatMillis(Long millis) {
        if (millis == null) {
            return null;
        }
        return formatDate(new Date(millis));
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

    private void updateBookingStatus(ProviderBookingItem item, String status) {
        if (item == null || TextUtils.isEmpty(item.bookingId)) {
            return;
        }
        java.util.HashMap<String, Object> updates = new java.util.HashMap<>();
        updates.put("status", status);
        updates.put("respondedAt", System.currentTimeMillis());
        if ("accepted".equals(status)) {
            updates.put("acceptedAt", FieldValue.serverTimestamp());
        } else if ("on process".equals(status) || "on-process".equals(status) || "ongoing".equals(status)) {
            updates.put("startedAt", FieldValue.serverTimestamp());
        } else if ("finished".equals(status)) {
            updates.put("finishedAt", FieldValue.serverTimestamp());
        } else if ("cancelled".equals(status)) {
            updates.put("cancelledAt", FieldValue.serverTimestamp());
        } else if ("rescheduled".equals(status)) {
            updates.put("rescheduledAt", FieldValue.serverTimestamp());
        }
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .document(item.bookingId)
                .update(updates)
                .addOnSuccessListener(unused -> loadBookings(mode))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update booking.",
                                Toast.LENGTH_SHORT).show());
    }
}
