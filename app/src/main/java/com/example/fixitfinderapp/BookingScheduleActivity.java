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

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class BookingScheduleActivity extends AppCompatActivity {

    private static final int START_HOUR = 5;
    private static final int END_HOUR = 22;

    private LinearLayout layoutAmSlots;
    private LinearLayout layoutPmSlots;
    private TextView tvSelectedDate;
    private TextView tvDateStatus;
    private Button btnConfirm;
    private String providerId;
    private String providerName;
    private String serviceCategory;
    private String providerAddress;
    private String logoUri;
    private String selectedDateKey;
    private String selectedTime;
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

        ImageButton btnBack = findViewById(R.id.btnBack);
        CalendarView calendarView = findViewById(R.id.calendarView);
        layoutAmSlots = findViewById(R.id.layoutAmSlots);
        layoutPmSlots = findViewById(R.id.layoutPmSlots);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvDateStatus = findViewById(R.id.tvDateStatus);
        btnConfirm = findViewById(R.id.btnConfirmBooking);

        btnBack.setOnClickListener(v -> finish());
        btnConfirm.setOnClickListener(v -> submitBooking());

        Calendar today = Calendar.getInstance();
        calendarView.setMinDate(today.getTimeInMillis());
        selectedDateKey = formatDateKey(today);
        updateSelectedDateLabel(today);
        loadBookedSlots();

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
                        String timeSlot = doc.getString("timeSlot");
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
        if (allBooked) {
            tvDateStatus.setText("All time slots are booked for this date.");
            tvDateStatus.setTextColor(0xFFF44336);
        } else {
            tvDateStatus.setText("Select an available time.");
            tvDateStatus.setTextColor(0xFF757575);
        }

        for (String slot : allSlots) {
            TextView view = createSlotView(slot, bookedTimes.contains(slot));
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

    private TextView createSlotView(String slot, boolean booked) {
        TextView view = new TextView(this);
        view.setText(slot);
        view.setTextSize(12f);
        view.setPadding(24, 12, 24, 12);
        if (booked) {
            view.setEnabled(false);
            view.setTextColor(0xFF9E9E9E);
            view.setBackgroundResource(R.drawable.button_outline_gray);
        } else {
            view.setTextColor(0xFF7B1113);
            view.setBackgroundResource(R.drawable.button_outline_red);
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
                button.setBackgroundResource(R.drawable.button_outline_red);
                button.setTextColor(0xFF7B1113);
            }
        }
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

    private String formatDateKey(Calendar calendar) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));
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

        Calendar scheduled = Calendar.getInstance();
        String[] parts = selectedDateKey.split("-");
        if (parts.length == 3) {
            scheduled.set(Calendar.YEAR, Integer.parseInt(parts[0]));
            scheduled.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
            scheduled.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
        }
        int hour = parseHour(selectedTime);
        scheduled.set(Calendar.HOUR_OF_DAY, hour);
        scheduled.set(Calendar.MINUTE, 0);
        scheduled.set(Calendar.SECOND, 0);
        scheduled.set(Calendar.MILLISECOND, 0);

        Map<String, Object> booking = new HashMap<>();
        booking.put("userId", user.getUid());
        booking.put("bookedBy", !TextUtils.isEmpty(user.getEmail())
                ? user.getEmail()
                : user.getUid());
        booking.put("providerId", providerId);
        booking.put("providerName", providerName);
        booking.put("providerLogoUri", logoUri);
        booking.put("serviceCategory", serviceCategory);
        booking.put("providerAddress", providerAddress);
        booking.put("status", "pending");
        booking.put("paymentStatus", "On-hold");
        booking.put("dateKey", selectedDateKey);
        booking.put("timeSlot", selectedTime);
        booking.put("scheduledAt", new Timestamp(scheduled.getTime()));
        booking.put("createdAt", System.currentTimeMillis());
        booking.put("bookingNumber", createBookingNumber());

        btnConfirm.setEnabled(false);
        btnConfirm.setText("Booking...");
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .add(booking)
                .addOnSuccessListener(doc -> {
                    Intent intent = new Intent(this, BookingConfirmationActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    btnConfirm.setEnabled(true);
                    btnConfirm.setText("Confirm Booking");
                    if (e instanceof FirebaseFirestoreException
                            && ((FirebaseFirestoreException) e).getCode()
                            == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Toast.makeText(this,
                                "Permission denied. Check Firestore rules for bookings.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    Toast.makeText(this, "Failed to book: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
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

    private String createBookingNumber() {
        String datePart = selectedDateKey != null ? selectedDateKey.replace("-", "") : "00000000";
        long suffix = System.currentTimeMillis() % 10000;
        return String.format(Locale.US, "%s%04d", datePart, suffix);
    }
}
