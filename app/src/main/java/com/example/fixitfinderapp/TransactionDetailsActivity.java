package com.example.fixitfinderapp;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.google.firebase.firestore.SetOptions;

public class TransactionDetailsActivity extends BaseSwipeActivity {

    public static final String EXTRA_SCHEDULED_AT_MILLIS = "scheduled_at_millis";
    public static final String EXTRA_NORMALIZED_SLOT = "normalized_slot";
    public static final String EXTRA_TIME_SLOT_KEY = "time_slot_key";
    public static final String EXTRA_PROVIDER_ID = "provider_id";
    public static final String EXTRA_PROVIDER_NAME = "provider_name";
    public static final String EXTRA_LOGO_URI = "logo_uri";
    public static final String EXTRA_SERVICE_CATEGORY = "service_category";
    public static final String EXTRA_PROVIDER_ADDRESS = "provider_address";
    public static final String EXTRA_SERVICE_ID = "service_id";
    public static final String EXTRA_SERVICE_NAME = "service_name";
    public static final String EXTRA_SERVICE_DESCRIPTION = "service_description";
    public static final String EXTRA_SERVICE_PRICE = "service_price";
    public static final String EXTRA_SERVICE_IMAGE_URI = "service_image_uri";
    public static final String EXTRA_DATE_KEY = "date_key";

    private long scheduledAtMillis;
    private String normalizedSlot;
    private int timeSlotKey;
    private String providerId;
    private String providerName;
    private String logoUri;
    private String serviceCategory;
    private String providerAddress;
    private String serviceId;
    private String serviceName;
    private String serviceDescription;
    private double servicePrice;
    private String serviceImageUri;
    private String dateKey;

    private TextView chipGcash;
    private TextView chipBank;
    private TextView chipCash;
    private TextView chipApplePay;
    private String selectedPaymentKey;

    private MaterialButton btnBookNow;
    private CharSequence bookButtonDefaultLabel;
    private boolean bookingInProgress;
    private final Runnable slowBookingHint = () -> {
        if (bookingInProgress) {
            Toast.makeText(TransactionDetailsActivity.this, R.string.booking_still_connecting,
                    Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_details);

        Intent in = getIntent();
        scheduledAtMillis = in.getLongExtra(EXTRA_SCHEDULED_AT_MILLIS, 0L);
        normalizedSlot = in.getStringExtra(EXTRA_NORMALIZED_SLOT);
        timeSlotKey = in.getIntExtra(EXTRA_TIME_SLOT_KEY, -1);
        providerId = in.getStringExtra(EXTRA_PROVIDER_ID);
        providerName = in.getStringExtra(EXTRA_PROVIDER_NAME);
        logoUri = in.getStringExtra(EXTRA_LOGO_URI);
        serviceCategory = in.getStringExtra(EXTRA_SERVICE_CATEGORY);
        providerAddress = in.getStringExtra(EXTRA_PROVIDER_ADDRESS);
        serviceId = in.getStringExtra(EXTRA_SERVICE_ID);
        serviceName = in.getStringExtra(EXTRA_SERVICE_NAME);
        serviceDescription = in.getStringExtra(EXTRA_SERVICE_DESCRIPTION);
        servicePrice = in.getDoubleExtra(EXTRA_SERVICE_PRICE, 0d);
        serviceImageUri = in.getStringExtra(EXTRA_SERVICE_IMAGE_URI);
        dateKey = in.getStringExtra(EXTRA_DATE_KEY);

        if (TextUtils.isEmpty(dateKey) || TextUtils.isEmpty(normalizedSlot) || scheduledAtMillis <= 0) {
            Toast.makeText(this, "Missing booking details.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        TextView tvProviderName = findViewById(R.id.tvProviderName);
        ImageView ivProviderPhoto = findViewById(R.id.ivProviderPhoto);
        TextView tvScheduleDate = findViewById(R.id.tvScheduleDate);
        TextView tvScheduleTime = findViewById(R.id.tvScheduleTime);
        TextView tvServiceNameField = findViewById(R.id.tvServiceName);

        if (tvProviderName != null) {
            tvProviderName.setText(!TextUtils.isEmpty(providerName) ? providerName : "—");
        }
        if (ivProviderPhoto != null) {
            ImageLoader.loadProfile(ivProviderPhoto, logoUri, android.R.drawable.ic_menu_myplaces);
        }

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(scheduledAtMillis);
        String dateStr = new SimpleDateFormat("MMMM d, yyyy", Locale.US).format(cal.getTime());
        if (tvScheduleDate != null) {
            tvScheduleDate.setText(dateStr);
        }
        if (tvScheduleTime != null) {
            tvScheduleTime.setText(!TextUtils.isEmpty(normalizedSlot) ? normalizedSlot : "—");
        }
        String svc = !TextUtils.isEmpty(serviceName) ? serviceName : serviceCategory;
        if (tvServiceNameField != null) {
            tvServiceNameField.setText(!TextUtils.isEmpty(svc) ? svc : getString(R.string.job_details_service_fallback));
        }

        TextView tvAddressLine = findViewById(R.id.tvAddressLine);
        loadUserAddress(tvAddressLine);

        TextView tvTotalCost = findViewById(R.id.tvTotalCost);
        TextView tvBookingIdPreview = findViewById(R.id.tvBookingIdPreview);
        TextView tvJobLineTitle = findViewById(R.id.tvJobLineTitle);
        TextView tvJobLinePrice = findViewById(R.id.tvJobLinePrice);
        ImageView ivServiceImage = findViewById(R.id.ivServiceImage);

        if (servicePrice > 0) {
            String peso = String.format(Locale.US, "%,d", (int) Math.round(servicePrice));
            if (tvTotalCost != null) {
                tvTotalCost.setText(peso);
            }
            if (tvJobLinePrice != null) {
                tvJobLinePrice.setText(String.format(Locale.US, "₱%,d", (int) Math.round(servicePrice)));
            }
        } else {
            if (tvTotalCost != null) {
                tvTotalCost.setText("—");
            }
            if (tvJobLinePrice != null) {
                tvJobLinePrice.setText("—");
            }
        }
        String previewId = BookingCompletionHelper.createBookingNumber(dateKey);
        if (tvBookingIdPreview != null) {
            tvBookingIdPreview.setText(formatBookingIdDisplay(previewId));
        }
        if (tvJobLineTitle != null) {
            tvJobLineTitle.setText(!TextUtils.isEmpty(serviceName) ? serviceName : "Service");
        }
        if (ivServiceImage != null) {
            if (!TextUtils.isEmpty(serviceImageUri)) {
                ImageLoader.load(ivServiceImage, serviceImageUri, 0);
            } else {
                ivServiceImage.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }

        chipGcash = findViewById(R.id.chipGcash);
        chipBank = findViewById(R.id.chipBank);
        chipCash = findViewById(R.id.chipCash);
        chipApplePay = findViewById(R.id.chipApplePay);
        wireChip(chipGcash, "gcash");
        wireChip(chipBank, "bank_transfer");
        wireChip(chipCash, "cash");
        wireChip(chipApplePay, "apple_pay");

        btnBookNow = findViewById(R.id.btnBookNow);
        if (btnBookNow != null) {
            bookButtonDefaultLabel = btnBookNow.getText();
            btnBookNow.setOnClickListener(v -> onBookNow());
        }
        findViewById(R.id.btnBackToServices).setOnClickListener(v -> finish());
    }

    private void setBookingUiBusy(boolean busy) {
        if (btnBookNow == null) {
            return;
        }
        btnBookNow.setEnabled(!busy);
        btnBookNow.setClickable(!busy);
        btnBookNow.setAlpha(busy ? 0.6f : 1f);
        if (busy) {
            btnBookNow.setText(R.string.booking_in_progress);
            btnBookNow.removeCallbacks(slowBookingHint);
            btnBookNow.postDelayed(slowBookingHint, 12_000);
        } else {
            btnBookNow.removeCallbacks(slowBookingHint);
            btnBookNow.setText(bookButtonDefaultLabel != null ? bookButtonDefaultLabel
                    : getString(R.string.book_now));
        }
    }

    private void resetBookingUiAfterFailure() {
        if (isFinishing()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed()) {
            return;
        }
        bookingInProgress = false;
        setBookingUiBusy(false);
    }

    private String formatBookingIdDisplay(String raw) {
        if (TextUtils.isEmpty(raw) || raw.length() < 6) {
            return raw;
        }
        return raw.substring(0, 3) + " - " + raw.substring(Math.max(0, raw.length() - 3));
    }

    private void loadUserAddress(TextView tvAddressLine) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || tvAddressLine == null) {
            if (tvAddressLine != null) {
                tvAddressLine.setText(!TextUtils.isEmpty(providerAddress) ? providerAddress : "—");
            }
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String addr = doc.getString("address");
                    if (TextUtils.isEmpty(addr)) {
                        addr = providerAddress;
                    }
                    tvAddressLine.setText(!TextUtils.isEmpty(addr) ? addr : "—");
                })
                .addOnFailureListener(e ->
                        tvAddressLine.setText(!TextUtils.isEmpty(providerAddress) ? providerAddress : "—"));
    }

    private void wireChip(TextView chip, String key) {
        if (chip == null) {
            return;
        }
        chip.setOnClickListener(v -> selectPayment(key));
    }

    private void selectPayment(String key) {
        selectedPaymentKey = key;
        applyChipStyle(chipGcash, "gcash".equals(key));
        applyChipStyle(chipBank, "bank_transfer".equals(key));
        applyChipStyle(chipCash, "cash".equals(key));
        applyChipStyle(chipApplePay, "apple_pay".equals(key));
    }

    private void applyChipStyle(TextView chip, boolean selected) {
        if (chip == null) {
            return;
        }
        chip.setBackgroundResource(selected
                ? R.drawable.bg_payment_chip_selected
                : R.drawable.bg_payment_chip_unselected);
    }

    private void onBookNow() {
        if (bookingInProgress) {
            return;
        }
        if (TextUtils.isEmpty(selectedPaymentKey)) {
            Toast.makeText(this, R.string.select_payment_first, Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }
        bookingInProgress = true;
        setBookingUiBusy(true);
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (!TextUtils.isEmpty(token)) {
                        Map<String, Object> tokenUpdate = new HashMap<>();
                        tokenUpdate.put("fcmToken", token);
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(user.getUid())
                                .set(tokenUpdate, SetOptions.merge());
                    }
                });
        checkSlotStillAvailable(() ->
                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(user.getUid())
                        .get()
                        .addOnCompleteListener(task -> {
                            com.google.firebase.firestore.DocumentSnapshot userDoc = null;
                            if (task.isSuccessful() && task.getResult() != null
                                    && task.getResult().exists()) {
                                userDoc = task.getResult();
                            }
                            submitBooking(user, userDoc);
                        }));
    }

    private void checkSlotStillAvailable(Runnable onOk) {
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereEqualTo("providerId", providerId)
                .whereEqualTo("dateKey", dateKey)
                .whereEqualTo("timeSlotKey", timeSlotKey)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && !snapshot.isEmpty()) {
                        resetBookingUiAfterFailure();
                        Toast.makeText(this, "This time slot was just booked. Pick another time.",
                                Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    FirebaseFirestore.getInstance()
                            .collection("bookings")
                            .whereEqualTo("providerId", providerId)
                            .whereEqualTo("dateKey", dateKey)
                            .whereEqualTo("timeSlot", normalizedSlot)
                            .get()
                            .addOnSuccessListener(s2 -> {
                                if (s2 != null && !s2.isEmpty()) {
                                    resetBookingUiAfterFailure();
                                    Toast.makeText(this, "This time slot was just booked. Pick another time.",
                                            Toast.LENGTH_LONG).show();
                                    finish();
                                    return;
                                }
                                onOk.run();
                            })
                            .addOnFailureListener(e -> onOk.run());
                })
                .addOnFailureListener(e -> onOk.run());
    }

    private void submitBooking(FirebaseUser user, com.google.firebase.firestore.DocumentSnapshot userDoc) {
        Calendar scheduled = Calendar.getInstance();
        scheduled.setTimeInMillis(scheduledAtMillis);

        Map<String, Object> booking = new HashMap<>();
        booking.put("userId", user.getUid());
        booking.put("bookedBy", !TextUtils.isEmpty(user.getEmail())
                ? user.getEmail()
                : user.getUid());
        booking.put("providerId", providerId);
        booking.put("providerName", providerName);
        booking.put("providerLogoUri", logoUri);
        booking.put("serviceCategory", serviceCategory);
        if (!TextUtils.isEmpty(serviceId)) {
            booking.put("serviceId", serviceId);
        }
        if (!TextUtils.isEmpty(serviceName)) {
            booking.put("serviceName", serviceName);
        }
        if (!TextUtils.isEmpty(serviceDescription)) {
            booking.put("serviceDescription", serviceDescription);
        }
        if (servicePrice > 0) {
            booking.put("servicePrice", servicePrice);
        }
        if (!TextUtils.isEmpty(serviceImageUri)) {
            booking.put("serviceImageUri", serviceImageUri);
        }
        booking.put("providerAddress", providerAddress);
        booking.put("status", "pending");
        booking.put("paymentStatus", "On-hold");
        booking.put("dateKey", dateKey);
        booking.put("timeSlot", normalizedSlot);
        booking.put("timeSlotKey", timeSlotKey);
        booking.put("scheduledAt", new Timestamp(scheduled.getTime()));
        booking.put("createdAt", System.currentTimeMillis());
        booking.put("bookingNumber", BookingCompletionHelper.createBookingNumber(dateKey));
        booking.put("paymentMethodPreference", selectedPaymentKey);

        if (userDoc != null) {
            String fullName = userDoc.getString("fullName");
            String firstName = userDoc.getString("firstName");
            if (TextUtils.isEmpty(firstName) && !TextUtils.isEmpty(fullName)) {
                String[] nameParts = fullName.trim().split("\\s+");
                if (nameParts.length > 0) {
                    firstName = nameParts[0];
                }
            }
            if (!TextUtils.isEmpty(fullName)) {
                booking.put("userName", fullName);
            }
            if (!TextUtils.isEmpty(firstName)) {
                booking.put("userFirstName", firstName);
            }
            String userAddress = userDoc.getString("address");
            if (!TextUtils.isEmpty(userAddress)) {
                booking.put("userAddress", userAddress);
            }
            Double lat = userDoc.getDouble("lat");
            Double lng = userDoc.getDouble("lng");
            if (lat != null && lng != null) {
                booking.put("userLat", lat);
                booking.put("userLng", lng);
            }
        }

        BookingCompletionHelper.addBookingAndFinish(this, booking, normalizedSlot,
                providerId, providerName, logoUri, dateKey, this::resetBookingUiAfterFailure);
    }

    @Override
    protected void onDestroy() {
        if (btnBookNow != null) {
            btnBookNow.removeCallbacks(slowBookingHint);
        }
        super.onDestroy();
    }
}
