package com.example.fixitfinderapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class BookingScheduleActivity extends BaseSwipeActivity {

    private static final String TAG = "BookingSchedule";
    private static final int START_HOUR = 5;
    private static final int END_HOUR = 22;

    private LinearLayout layoutAmSlots;
    private LinearLayout layoutPmSlots;
    private TextView tvSelectedDate;
    private TextView tvDateStatus;
    private Button btnConfirm;
    private CalendarView calendarView;
    private String providerId;
    private String providerName;
    private String serviceCategory;
    private String providerAddress;
    private String logoUri;
    private String serviceId;
    private String serviceName;
    private double servicePrice;
    private String serviceImageUri;
    private String serviceDescription;
    private String selectedDateKey;
    private String selectedTime;
    private boolean isReschedule = false;
    private String rescheduleBookingId;
    private final Set<String> bookedTimes = new HashSet<>();
    private final Map<String, TextView> slotButtons = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_schedule);

        providerId = getIntent().getStringExtra("providerId");
        providerName = getIntent().getStringExtra("providerName");
        serviceCategory = getIntent().getStringExtra("serviceCategory");
        providerAddress = getIntent().getStringExtra("address");
        logoUri = getIntent().getStringExtra("logoUri");
        serviceId = getIntent().getStringExtra("serviceId");
        serviceName = getIntent().getStringExtra("serviceName");
        servicePrice = getIntent().getDoubleExtra("servicePrice", 0d);
        serviceImageUri = getIntent().getStringExtra("serviceImageUri");
        serviceDescription = getIntent().getStringExtra("serviceDescription");
        rescheduleBookingId = getIntent().getStringExtra("bookingId");
        isReschedule = getIntent().getBooleanExtra("reschedule", false)
                && !TextUtils.isEmpty(rescheduleBookingId);

        ImageButton btnBack = findViewById(R.id.btnBack);
        calendarView = findViewById(R.id.calendarView);
        layoutAmSlots = findViewById(R.id.layoutAmSlots);
        layoutPmSlots = findViewById(R.id.layoutPmSlots);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvDateStatus = findViewById(R.id.tvDateStatus);
        btnConfirm = findViewById(R.id.btnConfirmBooking);
        Button btnBackToServices = findViewById(R.id.btnBackToServices);

        btnBack.setOnClickListener(v -> finish());
        btnConfirm.setOnClickListener(v -> submitBooking());
        if (isReschedule) {
            btnConfirm.setText(R.string.confirm_booking_reschedule);
        }
        if (btnBackToServices != null) {
            btnBackToServices.setOnClickListener(v -> finish());
        }
        Calendar today = Calendar.getInstance();
        calendarView.setMinDate(today.getTimeInMillis());
        selectedDateKey = formatDateKey(today);
        updateSelectedDateLabel(today);
        if (isReschedule) {
            loadRescheduleBooking();
        } else {
            loadBookedSlots();
        }

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth, 0, 0, 0);
            selected.set(Calendar.MILLISECOND, 0);
            selectedDateKey = formatDateKey(selected);
            selectedTime = null;
            updateSelectedDateLabel(selected);
            loadBookedSlots();
        });
    }

    private void updateSelectedDateLabel(Calendar calendar) {
        String dateText = android.text.format.DateFormat.format("MMMM dd, yyyy", calendar).toString();
        tvSelectedDate.setText(dateText);
    }

    private void loadBookedSlots() {
        bookedTimes.clear();
        slotButtons.clear();
        layoutAmSlots.removeAllViews();
        layoutPmSlots.removeAllViews();
        if (TextUtils.isEmpty(providerId)) {
            tvDateStatus.setText("Missing provider.");
            tvDateStatus.setTextColor(0xFFF44336);
            return;
        }
        Query query = FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereEqualTo("providerId", providerId)
                .whereEqualTo("dateKey", selectedDateKey);
        query.get()
                .addOnSuccessListener(snapshot -> {
                    snapshot.getDocuments().forEach(doc -> {
                        if (isReschedule && rescheduleBookingId != null
                                && rescheduleBookingId.equals(doc.getId())) {
                            return;
                        }
                        String timeSlot = normalizeSlot(doc.getString("timeSlot"));
                        if (TextUtils.isEmpty(timeSlot)) {
                            Long slotKey = doc.getLong("timeSlotKey");
                            if (slotKey != null) {
                                timeSlot = formatHour(slotKey.intValue());
                            }
                        }
                        if (!TextUtils.isEmpty(timeSlot)) {
                            bookedTimes.add(timeSlot);
                        }
                    });
                    renderSlots();
                })
                .addOnFailureListener(e -> {
                    tvDateStatus.setText("Unable to load availability.");
                    tvDateStatus.setTextColor(0xFFF44336);
                    renderSlots();
                });
    }

    private void renderSlots() {
        List<String> allSlots = buildTimeSlots();
        boolean allBooked = !allSlots.isEmpty() && bookedTimes.containsAll(allSlots);
        if (!TextUtils.isEmpty(selectedTime) && bookedTimes.contains(selectedTime)) {
            selectedTime = null;
        }
        if (allBooked) {
            tvDateStatus.setText("All time slots are booked for this date.");
            tvDateStatus.setTextColor(0xFFF44336);
        } else {
            tvDateStatus.setText("Select an available time.");
            tvDateStatus.setTextColor(0xFF757575);
        }
        updateCalendarIndicator(allBooked);

        for (String slot : allSlots) {
            boolean isPast = isSlotInPast(slot);
            TextView view = createSlotView(slot, bookedTimes.contains(slot), isPast);
            LinearLayout target = slot.toUpperCase(Locale.US).contains("AM")
                    ? layoutAmSlots
                    : layoutPmSlots;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 6, 8, 6);
            view.setLayoutParams(params);
            target.addView(view);
            slotButtons.put(slot, view);
        }
    }

    private TextView createSlotView(String slot, boolean booked, boolean isPast) {
        TextView view = new TextView(this);
        view.setText(slot);
        view.setTextSize(14f);
        view.setPadding(24, 12, 24, 12);
        if (booked || isPast) {
            view.setEnabled(false);
            view.setTextColor(0xFF9E9E9E);
            view.setBackground(null);
        } else {
            view.setTextColor(0xFF000000);
            view.setBackground(null);
            view.setOnClickListener(v -> selectSlot(slot));
        }
        return view;
    }

    private void selectSlot(String slot) {
        selectedTime = slot;
        for (Map.Entry<String, TextView> entry : slotButtons.entrySet()) {
            TextView button = entry.getValue();
            if (!button.isEnabled()) {
                continue;
            }
            if (entry.getKey().equals(slot)) {
                button.setBackgroundResource(R.drawable.button_rounded_primary);
                button.setTextColor(0xFFFFFFFF);
            } else {
                button.setBackground(null);
                button.setTextColor(0xFF000000);
            }
        }
    }

    private void updateCalendarIndicator(boolean allBooked) {
        if (calendarView == null) {
            return;
        }
        int indicator = allBooked
                ? R.drawable.calendar_selected_full
                : R.drawable.calendar_selected_available;
        calendarView.setSelectedDateVerticalBar(indicator);
    }

    private List<String> buildTimeSlots() {
        List<String> slots = new ArrayList<>();
        for (int hour = START_HOUR; hour <= END_HOUR; hour++) {
            slots.add(formatHour(hour));
        }
        return slots;
    }

    private String formatHour(int hour) {
        int displayHour = hour % 12;
        if (displayHour == 0) {
            displayHour = 12;
        }
        String meridiem = hour < 12 ? "AM" : "PM";
        return String.format(Locale.US, "%d:00 %s", displayHour, meridiem);
    }

    private String normalizeSlot(String slot) {
        if (TextUtils.isEmpty(slot)) {
            return null;
        }
        String cleaned = slot.trim().toUpperCase(Locale.US);
        if (cleaned.contains(":")) {
            return cleaned.replace("  ", " ");
        }
        // Accept legacy formats like "5 AM" or "5PM".
        String digits = cleaned.replaceAll("[^0-9]", "");
        boolean isPm = cleaned.contains("PM");
        boolean isAm = cleaned.contains("AM");
        if (!TextUtils.isEmpty(digits) && (isAm || isPm)) {
            int hour = Integer.parseInt(digits);
            if (hour >= 1 && hour <= 12) {
                int normalizedHour = hour % 12;
                if (normalizedHour == 0) {
                    normalizedHour = 12;
                }
                return String.format(Locale.US, "%d:00 %s", normalizedHour, isPm ? "PM" : "AM");
            }
        }
        return cleaned;
    }

    private String formatDateKey(Calendar calendar) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));
    }

    private void loadRescheduleBooking() {
        if (TextUtils.isEmpty(rescheduleBookingId)) {
            loadBookedSlots();
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .document(rescheduleBookingId)
                .get()
                .addOnSuccessListener(doc -> {
                    String dateKey = doc.getString("dateKey");
                    String timeSlot = doc.getString("timeSlot");
                    Timestamp scheduledAt = doc.getTimestamp("scheduledAt");
                    if (scheduledAt != null) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(scheduledAt.toDate());
                        selectedDateKey = formatDateKey(cal);
                        updateSelectedDateLabel(cal);
                        if (calendarView != null) {
                            calendarView.setDate(cal.getTimeInMillis(), false, true);
                        }
                    } else if (!TextUtils.isEmpty(dateKey)) {
                        selectedDateKey = dateKey;
                    }
                    if (!TextUtils.isEmpty(timeSlot)) {
                        selectedTime = normalizeSlot(timeSlot);
                    }
                    loadBookedSlots();
                })
                .addOnFailureListener(e -> loadBookedSlots());
    }

    private void submitBooking() {
        if (TextUtils.isEmpty(selectedTime)) {
            Toast.makeText(this, "Please select a time slot.", Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Ensure the latest FCM token is stored for push delivery.
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (TextUtils.isEmpty(token)) {
                        return;
                    }
                    java.util.Map<String, Object> tokenUpdate = new java.util.HashMap<>();
                    tokenUpdate.put("fcmToken", token);
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(user.getUid())
                            .set(tokenUpdate, SetOptions.merge());
                });

        Calendar scheduled = Calendar.getInstance();
        String[] parts = selectedDateKey.split("-");
        if (parts.length == 3) {
            scheduled.set(Calendar.YEAR, Integer.parseInt(parts[0]));
            scheduled.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
            scheduled.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
        }
        int hour = parseHour(selectedTime);
        String normalizedSlot = formatHour(hour);
        scheduled.set(Calendar.HOUR_OF_DAY, hour);
        scheduled.set(Calendar.MINUTE, 0);
        scheduled.set(Calendar.SECOND, 0);
        scheduled.set(Calendar.MILLISECOND, 0);

        btnConfirm.setEnabled(false);
        btnConfirm.setText("Checking...");
        checkExistingBooking(hour, normalizedSlot, rescheduleBookingId, () -> {
            if (!isReschedule) {
                btnConfirm.setEnabled(true);
                btnConfirm.setText(R.string.continue_to_payment);
                openTransactionDetails(scheduled.getTimeInMillis(), normalizedSlot, hour);
                return;
            }
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
            booking.put("dateKey", selectedDateKey);
            booking.put("timeSlot", normalizedSlot);
            booking.put("timeSlotKey", hour);
            booking.put("scheduledAt", new Timestamp(scheduled.getTime()));
            booking.put("createdAt", System.currentTimeMillis());
            booking.put("bookingNumber", BookingCompletionHelper.createBookingNumber(selectedDateKey));
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        mergeUserProfileIntoBooking(booking, doc, user);
                        submitRescheduleRecord(booking);
                    })
                    .addOnFailureListener(e -> mergeUserProfileIntoBooking(booking, null, user));
        });
    }

    private void mergeUserProfileIntoBooking(Map<String, Object> booking,
                                             com.google.firebase.firestore.DocumentSnapshot doc,
                                             FirebaseUser user) {
        if (doc != null) {
            String fullName = doc.getString("fullName");
            String firstName = doc.getString("firstName");
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
            String userAddress = doc.getString("address");
            if (!TextUtils.isEmpty(userAddress)) {
                booking.put("userAddress", userAddress);
            }
            Double lat = doc.getDouble("lat");
            Double lng = doc.getDouble("lng");
            if (lat != null && lng != null) {
                booking.put("userLat", lat);
                booking.put("userLng", lng);
            }
        }
        submitRescheduleRecord(booking);
    }

    private void openTransactionDetails(long scheduledAtMillis,
                                        String normalizedSlot,
                                        int hour) {
        Intent intent = new Intent(this, TransactionDetailsActivity.class);
        intent.putExtra(TransactionDetailsActivity.EXTRA_SCHEDULED_AT_MILLIS, scheduledAtMillis);
        intent.putExtra(TransactionDetailsActivity.EXTRA_NORMALIZED_SLOT, normalizedSlot);
        intent.putExtra(TransactionDetailsActivity.EXTRA_TIME_SLOT_KEY, hour);
        intent.putExtra(TransactionDetailsActivity.EXTRA_PROVIDER_ID, providerId);
        intent.putExtra(TransactionDetailsActivity.EXTRA_PROVIDER_NAME, providerName);
        intent.putExtra(TransactionDetailsActivity.EXTRA_LOGO_URI, logoUri);
        intent.putExtra(TransactionDetailsActivity.EXTRA_SERVICE_CATEGORY, serviceCategory);
        intent.putExtra(TransactionDetailsActivity.EXTRA_PROVIDER_ADDRESS, providerAddress);
        intent.putExtra(TransactionDetailsActivity.EXTRA_SERVICE_ID, serviceId);
        intent.putExtra(TransactionDetailsActivity.EXTRA_SERVICE_NAME, serviceName);
        intent.putExtra(TransactionDetailsActivity.EXTRA_SERVICE_DESCRIPTION, serviceDescription);
        intent.putExtra(TransactionDetailsActivity.EXTRA_SERVICE_PRICE, servicePrice);
        intent.putExtra(TransactionDetailsActivity.EXTRA_SERVICE_IMAGE_URI, serviceImageUri);
        intent.putExtra(TransactionDetailsActivity.EXTRA_DATE_KEY, selectedDateKey);
        startActivity(intent);
    }

    private void submitRescheduleRecord(Map<String, Object> booking) {
        if (TextUtils.isEmpty(rescheduleBookingId)) {
            return;
        }
        btnConfirm.setText("Rescheduling...");
        Map<String, Object> updates = new HashMap<>();
        updates.put("dateKey", booking.get("dateKey"));
        updates.put("timeSlot", booking.get("timeSlot"));
        updates.put("timeSlotKey", booking.get("timeSlotKey"));
        updates.put("scheduledAt", booking.get("scheduledAt"));
        updates.put("status", "rescheduled");
        updates.put("rescheduledAt", FieldValue.serverTimestamp());
        updates.put("rescheduledBy", "provider");
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .document(rescheduleBookingId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    sendRescheduleMessage();
                    Toast.makeText(this, "Booking rescheduled.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnConfirm.setEnabled(true);
                    btnConfirm.setText(R.string.confirm_booking_reschedule);
                    Toast.makeText(this, "Failed to reschedule: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void sendRescheduleMessage() {
        if (TextUtils.isEmpty(rescheduleBookingId)) {
            return;
        }
        String dateLabel = selectedDateKey + " " + valueOrEmpty(selectedTime);
        String messageText = "Booking rescheduled to " + dateLabel + ". Tap to review.";
        final String role = TextUtils.isEmpty(SessionManager.getRole(this))
                ? "provider"
                : SessionManager.getRole(this);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> message = new HashMap<>();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        message.put("senderId", user != null ? user.getUid() : "");
        message.put("senderRole", role);
        message.put("type", "system_reschedule");
        message.put("bookingId", rescheduleBookingId);
        message.put("text", messageText);
        message.put("createdAt", FieldValue.serverTimestamp());

        db.collection("conversations")
                .document(rescheduleBookingId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(ref -> {
                    Map<String, Object> convoUpdates = new HashMap<>();
                    convoUpdates.put("lastMessage", "Booking rescheduled.");
                    convoUpdates.put("lastMessageAt", FieldValue.serverTimestamp());
                    if ("provider".equalsIgnoreCase(role)) {
                        convoUpdates.put("unreadUserCount", FieldValue.increment(1));
                        convoUpdates.put("unreadProviderCount", 0);
                    } else {
                        convoUpdates.put("unreadProviderCount", FieldValue.increment(1));
                        convoUpdates.put("unreadUserCount", 0);
                    }
                    db.collection("conversations")
                            .document(rescheduleBookingId)
                            .update(convoUpdates);
                });
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private void checkExistingBooking(int hour,
                                      String normalizedSlot,
                                      String excludeBookingId,
                                      Runnable onAvailable) {
        Query baseQuery = FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereEqualTo("providerId", providerId)
                .whereEqualTo("dateKey", selectedDateKey);

        baseQuery.whereEqualTo("timeSlotKey", hour)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (hasOtherBooking(snapshot.getDocuments(), excludeBookingId)) {
                        handleSlotAlreadyBooked();
                        return;
                    }
                    baseQuery.whereEqualTo("timeSlot", normalizedSlot)
                            .get()
                            .addOnSuccessListener(fallback -> {
                                if (hasOtherBooking(fallback.getDocuments(), excludeBookingId)) {
                                    handleSlotAlreadyBooked();
                                    return;
                                }
                                onAvailable.run();
                            })
                            .addOnFailureListener(this::handleAvailabilityCheckError);
                })
                .addOnFailureListener(this::handleAvailabilityCheckError);
    }

    private boolean hasOtherBooking(List<com.google.firebase.firestore.DocumentSnapshot> docs,
                                    String excludeBookingId) {
        if (docs == null || docs.isEmpty()) {
            return false;
        }
        for (com.google.firebase.firestore.DocumentSnapshot doc : docs) {
            if (excludeBookingId != null && excludeBookingId.equals(doc.getId())) {
                continue;
            }
            return true;
        }
        return false;
    }

    private void handleSlotAlreadyBooked() {
        Toast.makeText(this, "This time slot is already booked.",
                Toast.LENGTH_LONG).show();
        btnConfirm.setEnabled(true);
        btnConfirm.setText(isReschedule ? R.string.confirm_booking_reschedule
                : R.string.continue_to_payment);
        loadBookedSlots();
    }

    private void handleAvailabilityCheckError(Exception e) {
        btnConfirm.setEnabled(true);
        btnConfirm.setText(isReschedule ? R.string.confirm_booking_reschedule
                : R.string.continue_to_payment);
        Toast.makeText(this, "Failed to check availability: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
    }

    private int parseHour(String slot) {
        if (TextUtils.isEmpty(slot)) {
            return START_HOUR;
        }
        String[] parts = slot.split(":");
        int hour = Integer.parseInt(parts[0].trim());
        boolean isPm = slot.toUpperCase(Locale.US).contains("PM");
        if (hour == 12) {
            hour = isPm ? 12 : 0;
        } else if (isPm) {
            hour += 12;
        }
        return hour;
    }

    private boolean isSlotInPast(String slot) {
        if (TextUtils.isEmpty(selectedDateKey) || TextUtils.isEmpty(slot)) {
            return false;
        }
        Calendar now = Calendar.getInstance();
        Calendar slotTime = Calendar.getInstance();
        String[] parts = selectedDateKey.split("-");
        if (parts.length == 3) {
            slotTime.set(Calendar.YEAR, Integer.parseInt(parts[0]));
            slotTime.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
            slotTime.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
        }
        int hour = parseHour(slot);
        slotTime.set(Calendar.HOUR_OF_DAY, hour);
        slotTime.set(Calendar.MINUTE, 0);
        slotTime.set(Calendar.SECOND, 0);
        slotTime.set(Calendar.MILLISECOND, 0);
        return now.after(slotTime);
    }

}
