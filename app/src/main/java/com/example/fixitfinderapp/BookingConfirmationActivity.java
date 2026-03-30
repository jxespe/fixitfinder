package com.example.fixitfinderapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BookingConfirmationActivity extends BaseSwipeActivity {

    public static final String EXTRA_BOOKING_ID = "bookingId";

    private String bookingId;
    private Timestamp scheduledAt;
    private TextView tvScheduleDate;
    private TextView tvScheduleTime;
    private TextView tvServiceName;
    private TextView tvAddressLine;
    private ImageView ivProviderPhoto;
    private TextView tvProviderName;
    private TextView tvTotalCost;
    private TextView tvBookingIdDisplay;
    private TextView tvProviderContact;
    private TextView tvPaymentMethod;
    private MaterialButton btnCancelBooking;
    private String cachedShareSummary = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_confirmation);

        ImageButton btnBack = findViewById(R.id.btnBack);
        MaterialButton btnBackHome = findViewById(R.id.btnBackHome);
        Button btnShare = findViewById(R.id.btnShare);
        Button btnViewDetails = findViewById(R.id.btnViewDetails);
        btnCancelBooking = findViewById(R.id.btnCancelBooking);
        tvScheduleDate = findViewById(R.id.tvScheduleDate);
        tvScheduleTime = findViewById(R.id.tvScheduleTime);
        tvServiceName = findViewById(R.id.tvServiceName);
        tvAddressLine = findViewById(R.id.tvAddressLine);
        ivProviderPhoto = findViewById(R.id.ivProviderPhoto);
        tvProviderName = findViewById(R.id.tvProviderName);
        tvTotalCost = findViewById(R.id.tvTotalCost);
        tvBookingIdDisplay = findViewById(R.id.tvBookingIdDisplay);
        tvProviderContact = findViewById(R.id.tvProviderContact);
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod);

        bookingId = getIntent().getStringExtra(EXTRA_BOOKING_ID);
        if (TextUtils.isEmpty(bookingId)) {
            Toast.makeText(this, "Missing booking info.", Toast.LENGTH_SHORT).show();
            goHome();
            return;
        }

        btnBack.setOnClickListener(v -> goHome());
        btnBackHome.setOnClickListener(v -> goHome());
        btnShare.setOnClickListener(v -> shareBooking());
        btnViewDetails.setOnClickListener(v -> {
            Intent i = new Intent(this, BookingTimelineActivity.class);
            i.putExtra("bookingId", bookingId);
            startActivity(i);
        });
        btnCancelBooking.setOnClickListener(v -> confirmCancel());

        loadBooking();
    }

    private void loadBooking() {
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .document(bookingId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Booking not found.", Toast.LENGTH_SHORT).show();
                        goHome();
                        return;
                    }
                    populateJobDetails(doc);
                    applyPaymentMethodLine(doc);
                    scheduledAt = doc.getTimestamp("scheduledAt");
                    updateCancelVisibility(doc);
                    BookingDisplayEnricher.enrichFromRelatedProfiles(this, doc, tvAddressLine,
                            ivProviderPhoto, tvProviderName, tvProviderContact);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Unable to load booking.", Toast.LENGTH_SHORT).show());
    }

    private void populateJobDetails(com.google.firebase.firestore.DocumentSnapshot doc) {
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
        tvScheduleDate.setText(TextUtils.isEmpty(dateStr) ? "—" : dateStr);
        tvScheduleTime.setText(TextUtils.isEmpty(timeStr) ? "—" : timeStr);

        String service = doc.getString("serviceName");
        if (TextUtils.isEmpty(service)) {
            service = doc.getString("serviceCategory");
        }
        tvServiceName.setText(TextUtils.isEmpty(service)
                ? getString(R.string.job_details_service_fallback) : service);

        String addr = doc.getString("userAddress");
        if (TextUtils.isEmpty(addr)) {
            addr = doc.getString("providerAddress");
        }
        tvAddressLine.setText(TextUtils.isEmpty(addr)
                ? getString(R.string.job_details_address_fallback) : addr);

        String provName = doc.getString("providerName");
        tvProviderName.setText(TextUtils.isEmpty(provName)
                ? getString(R.string.job_details_provider_fallback) : provName);

        String logo = doc.getString("providerLogoUri");
        ImageLoader.loadProfile(ivProviderPhoto, logo, android.R.drawable.ic_menu_myplaces);

        tvTotalCost.setText(formatPesoFromField(doc.get("servicePrice")));

        String bn = doc.getString("bookingNumber");
        if (TextUtils.isEmpty(bn) && bookingId != null && bookingId.length() >= 6) {
            bn = bookingId.length() >= 8
                    ? bookingId.substring(0, 8).toUpperCase(Locale.US)
                    : bookingId.toUpperCase(Locale.US);
        }
        String idDisplay = TextUtils.isEmpty(bn) ? "—" : formatBookingIdDisplay(bn);
        tvBookingIdDisplay.setText(idDisplay);

        cachedShareSummary = buildShareText(dateStr, timeStr, service, provName, idDisplay);
    }

    private void applyPaymentMethodLine(com.google.firebase.firestore.DocumentSnapshot bookingDoc) {
        if (tvPaymentMethod == null) {
            return;
        }
        String key = bookingDoc.getString("paymentMethodPreference");
        if (TextUtils.isEmpty(key)) {
            tvPaymentMethod.setVisibility(View.GONE);
            return;
        }
        tvPaymentMethod.setVisibility(View.VISIBLE);
        tvPaymentMethod.setText(getString(R.string.confirmation_payment_line,
                BookingDisplayEnricher.formatPaymentMethodLabel(key.trim())));
    }

    private String buildShareText(String dateStr, String timeStr, String service, String provName,
                                  String idDisplay) {
        String s = service != null ? service : "";
        String p = provName != null ? provName : "";
        String d = !TextUtils.isEmpty(dateStr) ? dateStr : "—";
        String t = !TextUtils.isEmpty(timeStr) ? timeStr : "—";
        return getString(R.string.share_booking_body_template, s, d, t, p, idDisplay);
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

    private void updateCancelVisibility(com.google.firebase.firestore.DocumentSnapshot doc) {
        String status = doc.getString("status");
        boolean isFinished = status != null && status.toLowerCase(Locale.US).contains("finish");
        boolean isCancelled = status != null && status.toLowerCase(Locale.US).contains("cancel");
        boolean isDeclined = status != null && status.toLowerCase(Locale.US).contains("declin");
        boolean canCancel = !(isFinished || isCancelled || isDeclined);
        if (isWithin24Hours(scheduledAt)) {
            canCancel = false;
        }
        btnCancelBooking.setVisibility(canCancel ? View.VISIBLE : View.GONE);
        btnCancelBooking.setEnabled(canCancel);
    }

    private boolean isWithin24Hours(Timestamp at) {
        if (at == null) {
            return false;
        }
        long scheduledMs = at.toDate().getTime();
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
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "cancelled");
        updates.put("cancelledAt", FieldValue.serverTimestamp());
        updates.put("cancelledBy", "user");
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .document(bookingId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Booking cancelled", Toast.LENGTH_SHORT).show();
                    loadBooking();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to cancel booking.", Toast.LENGTH_SHORT).show());
    }

    private void shareBooking() {
        if (TextUtils.isEmpty(cachedShareSummary)) {
            cachedShareSummary = "FixIt Finder booking #" + bookingId;
        }
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_booking_subject));
        share.putExtra(Intent.EXTRA_TEXT, cachedShareSummary);
        startActivity(Intent.createChooser(share, getString(R.string.share_booking)));
    }

    private void goHome() {
        Intent intent = new Intent(this, MainTabsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
