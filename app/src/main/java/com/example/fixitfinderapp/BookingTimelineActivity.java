package com.example.fixitfinderapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.adapters.TimelineAdapter;
import com.example.fixitfinderapp.models.TimelineEntry;
import android.widget.ImageView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingTimelineActivity extends BaseSwipeActivity {

    private final List<TimelineEntry> entries = new ArrayList<>();
    private TimelineAdapter adapter;
    private TextView tvCurrentStatus;
    private TextView tvCurrentStatusTime;
    private TextView tvEmpty;
    private TextView tvScheduleDate;
    private TextView tvScheduleTime;
    private TextView tvServiceName;
    private TextView tvAddressLine;
    private ImageView ivProviderPhoto;
    private TextView tvProviderName;
    private TextView tvProviderContact;
    private TextView tvTotalCost;
    private TextView tvBookingIdDisplay;
    private View layoutBookingActions;
    private Button btnCancelBooking;
    private Button btnRateBooking;
    private String bookingId;
    private String providerId;
    private Timestamp scheduledAt;
    private boolean alreadyRated = false;
    private boolean promptedRating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_timeline);

        ImageButton btnBack = findViewById(R.id.btnBack);
        tvCurrentStatus = findViewById(R.id.tvCurrentStatus);
        tvCurrentStatusTime = findViewById(R.id.tvCurrentStatusTime);
        tvEmpty = findViewById(R.id.tvEmptyTimeline);
        tvScheduleDate = findViewById(R.id.tvScheduleDate);
        tvScheduleTime = findViewById(R.id.tvScheduleTime);
        tvServiceName = findViewById(R.id.tvServiceName);
        tvAddressLine = findViewById(R.id.tvAddressLine);
        ivProviderPhoto = findViewById(R.id.ivProviderPhoto);
        tvProviderName = findViewById(R.id.tvProviderName);
        tvProviderContact = findViewById(R.id.tvProviderContact);
        tvTotalCost = findViewById(R.id.tvTotalCost);
        tvBookingIdDisplay = findViewById(R.id.tvBookingIdDisplay);
        layoutBookingActions = findViewById(R.id.layoutBookingActions);
        btnCancelBooking = findViewById(R.id.btnCancelBooking);
        btnRateBooking = findViewById(R.id.btnRateBooking);
        RecyclerView recycler = findViewById(R.id.recyclerTimeline);

        btnBack.setOnClickListener(v -> finish());
        adapter = new TimelineAdapter(entries);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        bookingId = getIntent().getStringExtra("bookingId");
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
                    populateJobDetails(doc);
                    BookingDisplayEnricher.enrichFromRelatedProfiles(this, doc, tvAddressLine,
                            ivProviderPhoto, tvProviderName, tvProviderContact);
                    updateActions(doc);
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

    private void populateJobDetails(com.google.firebase.firestore.DocumentSnapshot doc) {
        if (doc == null) {
            return;
        }
        Timestamp sched = doc.getTimestamp("scheduledAt");
        String timeSlot = doc.getString("timeSlot");
        String dateStr = "";
        String timeStr = "";
        if (sched != null) {
            Date d = sched.toDate();
            dateStr = new SimpleDateFormat("MMMM d, yyyy", Locale.US).format(d);
            timeStr = new SimpleDateFormat("h:mm a", Locale.US).format(d);
        }
        if (TextUtils.isEmpty(timeStr) && !TextUtils.isEmpty(timeSlot)) {
            timeStr = timeSlot.trim();
        }
        if (tvScheduleDate != null) {
            tvScheduleDate.setText(TextUtils.isEmpty(dateStr) ? "—" : dateStr);
        }
        if (tvScheduleTime != null) {
            tvScheduleTime.setText(TextUtils.isEmpty(timeStr) ? "—" : timeStr);
        }
        String service = doc.getString("serviceName");
        if (TextUtils.isEmpty(service)) {
            service = doc.getString("serviceCategory");
        }
        if (tvServiceName != null) {
            tvServiceName.setText(TextUtils.isEmpty(service) ? getString(R.string.job_details_service_fallback)
                    : service);
        }
        String addr = doc.getString("userAddress");
        if (TextUtils.isEmpty(addr)) {
            addr = doc.getString("providerAddress");
        }
        if (tvAddressLine != null) {
            tvAddressLine.setText(TextUtils.isEmpty(addr)
                    ? getString(R.string.job_details_address_fallback) : addr);
        }
        String provName = doc.getString("providerName");
        if (tvProviderName != null) {
            tvProviderName.setText(TextUtils.isEmpty(provName)
                    ? getString(R.string.job_details_provider_fallback) : provName);
        }
        String logo = pickLogoUri(doc.getString("providerLogoUri"), doc.getString("logoUri"));
        if (ivProviderPhoto != null) {
            ImageLoader.loadProfile(ivProviderPhoto, logo, android.R.drawable.ic_menu_myplaces);
        }
        if (tvTotalCost != null) {
            tvTotalCost.setText(formatPesoFromField(doc.get("servicePrice")));
        }
        String bn = doc.getString("bookingNumber");
        if (TextUtils.isEmpty(bn) && bookingId != null && bookingId.length() >= 8) {
            bn = bookingId.substring(0, 8).toUpperCase(Locale.US);
        }
        if (tvBookingIdDisplay != null) {
            tvBookingIdDisplay.setText(TextUtils.isEmpty(bn) ? "—" : formatBookingIdDisplay(bn));
        }
    }

    private String formatBookingIdDisplay(String raw) {
        if (TextUtils.isEmpty(raw) || raw.length() < 6) {
            return raw;
        }
        return raw.substring(0, 3) + " - " + raw.substring(Math.max(0, raw.length() - 3));
    }

    private String formatPesoFromField(Object price) {
        if (price == null) {
            return "—";
        }
        if (price instanceof Number) {
            int peso = (int) Math.round(((Number) price).doubleValue());
            return String.format(Locale.US, "₱%,d", peso);
        }
        try {
            int peso = (int) Math.round(Double.parseDouble(price.toString()));
            return String.format(Locale.US, "₱%,d", peso);
        } catch (Exception e) {
            return "—";
        }
    }

    private static String pickLogoUri(String primary, String fallback) {
        if (!TextUtils.isEmpty(primary)) {
            return primary;
        }
        return !TextUtils.isEmpty(fallback) ? fallback : null;
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

    private void updateActions(com.google.firebase.firestore.DocumentSnapshot doc) {
        if (doc == null) {
            return;
        }
        providerId = doc.getString("providerId");
        scheduledAt = doc.getTimestamp("scheduledAt");
        alreadyRated = doc.getDouble("userRating") != null;
        Boolean ratePromptSent = doc.getBoolean("ratePromptSent");
        String status = doc.getString("status");
        boolean isFinished = status != null && status.toLowerCase(Locale.US).contains("finish");
        boolean isCancelled = status != null && status.toLowerCase(Locale.US).contains("cancel");
        boolean isDeclined = status != null && status.toLowerCase(Locale.US).contains("declin");

        boolean canCancel = !(isFinished || isCancelled || isDeclined);
        if (isWithin24Hours(scheduledAt)) {
            canCancel = false;
        }

        boolean canRate = isFinished && !alreadyRated;

        if (layoutBookingActions != null) {
            layoutBookingActions.setVisibility((canCancel || canRate) ? View.VISIBLE : View.GONE);
        }
        if (btnCancelBooking != null) {
            btnCancelBooking.setVisibility(canCancel ? View.VISIBLE : View.GONE);
            btnCancelBooking.setEnabled(canCancel);
            btnCancelBooking.setOnClickListener(v -> confirmCancel());
        }
        if (btnRateBooking != null) {
            btnRateBooking.setVisibility(canRate ? View.VISIBLE : View.GONE);
            btnRateBooking.setOnClickListener(v -> showRatingDialog());
        }
        if (canRate) {
            maybePromptRating();
            if (ratePromptSent == null || !ratePromptSent) {
                sendRatePromptMessage(doc);
            }
        }
    }

    private void sendRatePromptMessage(com.google.firebase.firestore.DocumentSnapshot bookingDoc) {
        if (TextUtils.isEmpty(bookingId)) {
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        java.util.Map<String, Object> message = new java.util.HashMap<>();
        message.put("senderId", providerId != null ? providerId : "system");
        message.put("senderRole", "system");
        message.put("type", "rate_prompt");
        message.put("bookingId", bookingId);
        message.put("text", "Please rate your service. Tap to rate.");
        message.put("createdAt", FieldValue.serverTimestamp());

        db.collection("conversations")
                .document(bookingId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(ref -> {
                    java.util.Map<String, Object> convoUpdates = new java.util.HashMap<>();
                    convoUpdates.put("lastMessage", "Please rate your service.");
                    convoUpdates.put("lastMessageAt", FieldValue.serverTimestamp());
                    convoUpdates.put("unreadUserCount", FieldValue.increment(1));
                    convoUpdates.put("unreadProviderCount", 0);
                    db.collection("conversations")
                            .document(bookingId)
                            .update(convoUpdates);
                    if (bookingDoc != null) {
                        bookingDoc.getReference()
                                .update("ratePromptSent", true);
                    }
                });
    }

    private void maybePromptRating() {
        if (TextUtils.isEmpty(bookingId) || promptedRating || alreadyRated) {
            return;
        }
        android.content.SharedPreferences prefs =
                getSharedPreferences("rating_prompts", MODE_PRIVATE);
        String key = "prompted_" + bookingId;
        if (prefs.getBoolean(key, false)) {
            return;
        }
        promptedRating = true;
        prefs.edit().putBoolean(key, true).apply();
        showRatingDialog();
    }

    private boolean isWithin24Hours(Timestamp scheduledAt) {
        if (scheduledAt == null) {
            return false;
        }
        long scheduledMs = scheduledAt.toDate().getTime();
        long now = System.currentTimeMillis();
        return scheduledMs - now <= 24L * 60L * 60L * 1000L;
    }

    private void confirmCancel() {
        if (TextUtils.isEmpty(bookingId)) {
            return;
        }
        if (isWithin24Hours(scheduledAt)) {
            Toast.makeText(this, "Cannot cancel within 24 hours of the schedule.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Cancel booking?")
                .setMessage("Are you sure you want to cancel this booking?")
                .setPositiveButton("Cancel Booking", (dialog, which) -> cancelBooking())
                .setNegativeButton("Keep", null)
                .show();
    }

    private void cancelBooking() {
        if (TextUtils.isEmpty(bookingId)) {
            return;
        }
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("status", "cancelled");
        updates.put("cancelledAt", FieldValue.serverTimestamp());
        updates.put("cancelledBy", "user");
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .document(bookingId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Booking cancelled", Toast.LENGTH_SHORT).show();
                    loadTimeline(bookingId);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to cancel booking.",
                                Toast.LENGTH_SHORT).show());
    }

    private void showRatingDialog() {
        if (TextUtils.isEmpty(bookingId)) {
            return;
        }
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pad, pad, pad, pad);

        RatingBar ratingBar = new RatingBar(this);
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1f);
        ratingBar.setRating(5f);
        layout.addView(ratingBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        EditText etComment = new EditText(this);
        etComment.setHint("Share your experience (optional)");
        etComment.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etComment.setMinLines(2);
        layout.addView(etComment, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
                .setTitle("Rate your service")
                .setView(layout)
                .setPositiveButton("Submit", (dialog, which) -> {
                    float rating = ratingBar.getRating();
                    if (rating <= 0f) {
                        Toast.makeText(this, "Please select a rating.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String comment = etComment.getText() != null
                            ? etComment.getText().toString().trim() : "";
                    submitRating((int) rating, comment);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitRating(int rating, String comment) {
        if (TextUtils.isEmpty(bookingId)) {
            return;
        }
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("userRating", rating);
        updates.put("userRatedAt", FieldValue.serverTimestamp());
        if (!TextUtils.isEmpty(comment)) {
            updates.put("userReviewText", comment);
        }

        FirebaseFirestore.getInstance()
                .collection("bookings")
                .document(bookingId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    if (!TextUtils.isEmpty(providerId)) {
                        java.util.Map<String, Object> providerUpdates = new java.util.HashMap<>();
                        providerUpdates.put("ratingSum", FieldValue.increment(rating));
                        providerUpdates.put("ratingCount", FieldValue.increment(1));
                        FirebaseFirestore.getInstance()
                                .collection("providers")
                                .document(providerId)
                                .set(providerUpdates, com.google.firebase.firestore.SetOptions.merge());
                    }
                    Toast.makeText(this, "Thank you for your rating!", Toast.LENGTH_SHORT).show();
                    loadTimeline(bookingId);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to submit rating.", Toast.LENGTH_SHORT).show());
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
