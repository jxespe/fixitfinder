package com.example.fixitfinderapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.adapters.TimelineAdapter;
import com.example.fixitfinderapp.models.TimelineEntry;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingTimelineActivity extends AppCompatActivity {

    private final List<TimelineEntry> entries = new ArrayList<>();
    private TimelineAdapter adapter;
    private TextView tvCurrentStatus;
    private TextView tvCurrentStatusTime;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_timeline);

        ImageButton btnBack = findViewById(R.id.btnBack);
        tvCurrentStatus = findViewById(R.id.tvCurrentStatus);
        tvCurrentStatusTime = findViewById(R.id.tvCurrentStatusTime);
        tvEmpty = findViewById(R.id.tvEmptyTimeline);
        RecyclerView recycler = findViewById(R.id.recyclerTimeline);

        btnBack.setOnClickListener(v -> finish());

        adapter = new TimelineAdapter(entries);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        String bookingId = getIntent().getStringExtra("bookingId");
        if (TextUtils.isEmpty(bookingId)) {
            Toast.makeText(this, "Missing booking info.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        loadTimeline(bookingId);
    }

    private void loadTimeline(String bookingId) {
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .document(bookingId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        showEmpty();
                        return;
                    }
                    entries.clear();
                    List<TimelinePoint> points = new ArrayList<>();
                    addPoint(points, "Booked", doc.get("createdAt"));
                    addPoint(points, "Accepted", doc.get("acceptedAt"));
                    addPoint(points, "In Process", doc.get("startedAt"));
                    addPoint(points, "Rescheduled", doc.get("rescheduledAt"));
                    addPoint(points, "Cancelled", doc.get("cancelledAt"));
                    addPoint(points, "Declined", doc.get("respondedAt"), "declined", doc.getString("status"));
                    addPoint(points, "Finished", doc.get("finishedAt"));

                    points.sort(Comparator.comparingLong(p -> p.timestamp));
                    for (TimelinePoint point : points) {
                        entries.add(new TimelineEntry(point.label, formatDate(point.timestamp)));
                    }
                    adapter.notifyDataSetChanged();
                    updateCurrentStatus(doc.getString("status"), doc.get("acceptedAt"), doc.get("startedAt"),
                            doc.get("finishedAt"), doc.get("cancelledAt"), doc.get("rescheduledAt"),
                            doc.get("respondedAt"), doc.get("createdAt"));
                    toggleEmpty();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Unable to load timeline.", Toast.LENGTH_SHORT).show();
                    showEmpty();
                });
    }

    private void addPoint(List<TimelinePoint> points, String label, Object value) {
        Date date = toDate(value);
        if (date == null) {
            return;
        }
        points.add(new TimelinePoint(label, date.getTime()));
    }

    private void addPoint(List<TimelinePoint> points, String label, Object value,
                          String expectedStatus, String actualStatus) {
        if (TextUtils.isEmpty(actualStatus)
                || !actualStatus.trim().equalsIgnoreCase(expectedStatus)) {
            return;
        }
        addPoint(points, label, value);
    }

    private void updateCurrentStatus(String status,
                                     Object acceptedAt,
                                     Object startedAt,
                                     Object finishedAt,
                                     Object cancelledAt,
                                     Object rescheduledAt,
                                     Object respondedAt,
                                     Object createdAt) {
        String displayStatus = formatStatus(status);
        long timestamp = pickStatusTime(status, acceptedAt, startedAt,
                finishedAt, cancelledAt, rescheduledAt, respondedAt, createdAt);
        tvCurrentStatus.setText("Status: " + displayStatus);
        tvCurrentStatusTime.setText(timestamp > 0 ? formatDate(timestamp) : "Date not set");
    }

    private long pickStatusTime(String status,
                                Object acceptedAt,
                                Object startedAt,
                                Object finishedAt,
                                Object cancelledAt,
                                Object rescheduledAt,
                                Object respondedAt,
                                Object createdAt) {
        if (TextUtils.isEmpty(status)) {
            Date created = toDate(createdAt);
            return created != null ? created.getTime() : 0L;
        }
        String normalized = status.trim().toLowerCase(Locale.US);
        if (normalized.contains("finish")) {
            Date d = toDate(finishedAt);
            return d != null ? d.getTime() : 0L;
        }
        if (normalized.contains("cancel")) {
            Date d = toDate(cancelledAt);
            return d != null ? d.getTime() : 0L;
        }
        if (normalized.contains("resched")) {
            Date d = toDate(rescheduledAt);
            return d != null ? d.getTime() : 0L;
        }
        if (normalized.contains("process") || normalized.contains("ongoing")) {
            Date d = toDate(startedAt);
            return d != null ? d.getTime() : 0L;
        }
        if (normalized.contains("accept")) {
            Date d = toDate(acceptedAt);
            return d != null ? d.getTime() : 0L;
        }
        if (normalized.contains("declin")) {
            Date d = toDate(respondedAt);
            return d != null ? d.getTime() : 0L;
        }
        Date created = toDate(createdAt);
        return created != null ? created.getTime() : 0L;
    }

    private String formatStatus(String status) {
        if (TextUtils.isEmpty(status)) {
            return "Pending";
        }
        String normalized = status.replace("_", " ").trim();
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0) {
            return "Date not set";
        }
        return new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US).format(new Date(timestamp));
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

    private void toggleEmpty() {
        boolean empty = entries.isEmpty();
        tvEmpty.setVisibility(empty ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void showEmpty() {
        entries.clear();
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(android.view.View.VISIBLE);
    }

    private static class TimelinePoint {
        final String label;
        final long timestamp;

        TimelinePoint(String label, long timestamp) {
            this.label = label;
            this.timestamp = timestamp;
        }
    }
}
