package com.example.fixitfinderapp;

import android.util.Log;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProviderJobListActivity extends BaseSwipeActivity {

    private static final String TAG = "ProviderJobList";

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
                        startReschedule(item);
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
                        String bookedBy = doc.getString("userFirstName");
                        if (TextUtils.isEmpty(bookedBy)) {
                            bookedBy = doc.getString("userName");
                        }
                        if (TextUtils.isEmpty(bookedBy)) {
                            bookedBy = doc.getString("bookedBy");
                        }
                        String location = doc.getString("userAddress");
                        if (TextUtils.isEmpty(location)) {
                            location = doc.getString("address");
                        }
                        if (TextUtils.isEmpty(location)) {
                            location = doc.getString("providerAddress");
                        }
                        String bookedAt = formatMillis(doc.getLong("createdAt"));
                        String status = doc.getString("status");
                        String jobDescription = doc.getString("serviceDescription");
                        if (TextUtils.isEmpty(jobDescription)) {
                            jobDescription = doc.getString("serviceName");
                        }
                        String priceText = formatPrice(doc.get("servicePrice"));

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
                                status,
                                "Job: " + valueOrUnknown(jobDescription),
                                "Price: " + valueOrUnknown(priceText)
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

    private String formatPrice(Object priceObj) {
        if (priceObj instanceof Number) {
            double price = ((Number) priceObj).doubleValue();
            if (price > 0) {
                return String.format(Locale.US, "\u20b1%.2f", price);
            }
        }
        return null;
    }

    private void updateBookingStatus(ProviderBookingItem item, String status) {
        if (item == null || TextUtils.isEmpty(item.bookingId)) {
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
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
                                .set(data, com.google.firebase.firestore.SetOptions.merge());
                    });
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
                .addOnSuccessListener(unused -> {
                    loadBookings(mode);
                    notifyBookingUser(item.bookingId, status);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update booking.",
                                Toast.LENGTH_SHORT).show());
    }

    private void startReschedule(ProviderBookingItem item) {
        if (item == null || TextUtils.isEmpty(item.bookingId)) {
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .document(item.bookingId)
                .get()
                .addOnSuccessListener(doc -> {
                    String providerId = doc.getString("providerId");
                    String providerName = doc.getString("providerName");
                    String serviceCategory = doc.getString("serviceCategory");
                    String address = doc.getString("providerAddress");
                    String logoUri = doc.getString("providerLogoUri");
                    String serviceId = doc.getString("serviceId");
                    String serviceName = doc.getString("serviceName");
                    double servicePrice = 0d;
                    Object priceObj = doc.get("servicePrice");
                    if (priceObj instanceof Number) {
                        servicePrice = ((Number) priceObj).doubleValue();
                    }
                    Intent intent = new Intent(this, BookingScheduleActivity.class);
                    intent.putExtra("providerId", providerId);
                    intent.putExtra("providerName", providerName);
                    intent.putExtra("serviceCategory", serviceCategory);
                    intent.putExtra("address", address);
                    intent.putExtra("logoUri", logoUri);
                    intent.putExtra("serviceId", serviceId);
                    intent.putExtra("serviceName", serviceName);
                    intent.putExtra("servicePrice", servicePrice);
                    intent.putExtra("bookingId", item.bookingId);
                    intent.putExtra("reschedule", true);
                    startActivity(intent);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Unable to load booking for reschedule.",
                                Toast.LENGTH_SHORT).show());
    }

    private void notifyBookingUser(String bookingId, String status) {
        if (TextUtils.isEmpty(bookingId)) {
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .document(bookingId)
                .get()
                .addOnSuccessListener(doc -> createBookingNotification(doc, status))
                .addOnFailureListener(e -> { });
    }

    private void createBookingNotification(DocumentSnapshot doc, String status) {
        if (doc == null || !doc.exists()) {
            return;
        }
        String userId = doc.getString("userId");
        if (TextUtils.isEmpty(userId)) {
            return;
        }
        String providerName = doc.getString("providerName");
        String serviceName = doc.getString("serviceName");
        String bookingNumber = doc.getString("bookingNumber");
        if (TextUtils.isEmpty(bookingNumber)) {
            bookingNumber = doc.getId();
        }
        String title = "Booking update";
        String message = buildStatusMessage(status, providerName, serviceName, bookingNumber);

        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("userId", userId);
        payload.put("bookingId", doc.getId());
        payload.put("status", status);
        payload.put("title", title);
        payload.put("message", message);
        payload.put("createdAt", FieldValue.serverTimestamp());
        payload.put("delivered", false);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("user_notifications")
                .add(payload)
                .addOnFailureListener(e ->
                        Log.w(TAG, "user_notifications write failed", e));
        postProviderStatusChatMessage(doc, status);
    }

    /**
     * Sends an in-chat line to the customer (conversation id = booking id) so job updates appear in Messages.
     */
    private void postProviderStatusChatMessage(DocumentSnapshot bookingDoc, String status) {
        FirebaseUser provider = FirebaseAuth.getInstance().getCurrentUser();
        if (provider == null || bookingDoc == null || !bookingDoc.exists()) {
            return;
        }
        String bookingId = bookingDoc.getId();
        String providerId = bookingDoc.getString("providerId");
        if (TextUtils.isEmpty(providerId) || !providerId.equals(provider.getUid())) {
            return;
        }
        String providerName = bookingDoc.getString("providerName");
        String serviceName = bookingDoc.getString("serviceName");
        String bookingNumber = bookingDoc.getString("bookingNumber");
        if (TextUtils.isEmpty(bookingNumber)) {
            bookingNumber = bookingId;
        }
        String statusLine = buildStatusMessage(status, providerName, serviceName, bookingNumber);
        String chatText = !TextUtils.isEmpty(providerName)
                ? providerName + ": " + statusLine
                : statusLine;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("conversations")
                .document(bookingId)
                .get()
                .addOnSuccessListener(convoSnap -> {
                    if (!convoSnap.exists()) {
                        return;
                    }
                    java.util.Map<String, Object> message = new java.util.HashMap<>();
                    message.put("senderId", provider.getUid());
                    message.put("senderRole", "provider");
                    message.put("text", chatText);
                    message.put("type", "booking_status");
                    message.put("bookingId", bookingId);
                    message.put("createdAt", FieldValue.serverTimestamp());
                    db.collection("conversations")
                            .document(bookingId)
                            .collection("messages")
                            .add(message)
                            .addOnSuccessListener(ref -> {
                                String preview = chatText.length() > 200
                                        ? chatText.substring(0, 197) + "…"
                                        : chatText;
                                java.util.Map<String, Object> convoUpdates = new java.util.HashMap<>();
                                convoUpdates.put("lastMessage", preview);
                                convoUpdates.put("lastMessageAt", FieldValue.serverTimestamp());
                                convoUpdates.put("unreadUserCount", FieldValue.increment(1));
                                convoUpdates.put("unreadProviderCount", 0);
                                db.collection("conversations")
                                        .document(bookingId)
                                        .update(convoUpdates)
                                        .addOnFailureListener(e ->
                                                Log.w(TAG, "conversation preview update failed", e));
                            })
                            .addOnFailureListener(e ->
                                    Log.w(TAG, "status chat message failed", e));
                })
                .addOnFailureListener(e -> Log.w(TAG, "conversation lookup failed", e));
    }

    private String buildStatusMessage(String status,
                                      String providerName,
                                      String serviceName,
                                      String bookingNumber) {
        String readableStatus = status == null ? "updated" : status.trim();
        if ("on process".equalsIgnoreCase(readableStatus)
                || "on-process".equalsIgnoreCase(readableStatus)
                || "ongoing".equalsIgnoreCase(readableStatus)) {
            readableStatus = "on process";
        }
        if ("accepted".equalsIgnoreCase(readableStatus)) {
            readableStatus = "accepted";
        } else if ("declined".equalsIgnoreCase(readableStatus)) {
            readableStatus = "declined";
        } else if ("finished".equalsIgnoreCase(readableStatus)) {
            readableStatus = "finished";
        } else if ("cancelled".equalsIgnoreCase(readableStatus)) {
            readableStatus = "cancelled";
        } else if ("rescheduled".equalsIgnoreCase(readableStatus)) {
            readableStatus = "rescheduled";
        }

        StringBuilder builder = new StringBuilder("Your booking");
        if (!TextUtils.isEmpty(bookingNumber)) {
            builder.append(" #").append(bookingNumber);
        }
        builder.append(" is ").append(readableStatus).append(".");
        if (!TextUtils.isEmpty(serviceName)) {
            builder.append(" ").append(serviceName);
        }
        if (!TextUtils.isEmpty(providerName)) {
            builder.append(" with ").append(providerName);
        }
        return builder.toString().trim();
    }
}
