package com.example.fixitfinderapp.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;
import java.util.Set;

public final class ReminderScheduler {

    private static final String PREFS_NAME = "reminder_prefs";
    private static final String KEY_SCHEDULED = "scheduled_ids";
    private static final String ROLE_USER = "user";
    private static final String ROLE_PROVIDER = "provider";

    private ReminderScheduler() {
    }

    public static void scheduleAcceptedReminders(Context context, String userId) {
        if (context == null || TextUtils.isEmpty(userId)) {
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "accepted")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Set<String> scheduled = loadScheduled(context);
                    snapshot.getDocuments().forEach(doc -> {
                        String bookingId = doc.getId();
                        String key = buildKey(ROLE_USER, bookingId);
                        if (TextUtils.isEmpty(bookingId) || scheduled.contains(key)) {
                            return;
                        }
                        com.google.firebase.Timestamp ts = doc.getTimestamp("scheduledAt");
                        if (ts == null) {
                            return;
                        }
                        long scheduledAt = ts.toDate().getTime();
                        long triggerAt = scheduledAt - 60 * 60 * 1000L;
                        long now = System.currentTimeMillis();
                        if (triggerAt < now) {
                            // If within 1 hour already, fire soon.
                            triggerAt = now + 5_000L;
                        }
                        String providerName = doc.getString("providerName");
                        String serviceName = doc.getString("serviceName");
                        String title = "Upcoming appointment";
                        String message = buildMessage(providerName, serviceName);
                        scheduleAlarm(context, bookingId, triggerAt, title, message);
                        scheduled.add(key);
                    });
                    saveScheduled(context, scheduled);
                });
    }

    public static void scheduleProviderAcceptedReminders(Context context, String providerId) {
        if (context == null || TextUtils.isEmpty(providerId)) {
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereEqualTo("providerId", providerId)
                .whereEqualTo("status", "accepted")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Set<String> scheduled = loadScheduled(context);
                    snapshot.getDocuments().forEach(doc -> {
                        String bookingId = doc.getId();
                        String key = buildKey(ROLE_PROVIDER, bookingId);
                        if (TextUtils.isEmpty(bookingId) || scheduled.contains(key)) {
                            return;
                        }
                        com.google.firebase.Timestamp ts = doc.getTimestamp("scheduledAt");
                        if (ts == null) {
                            return;
                        }
                        long scheduledAt = ts.toDate().getTime();
                        long triggerAt = scheduledAt - 60 * 60 * 1000L;
                        long now = System.currentTimeMillis();
                        if (triggerAt < now) {
                            triggerAt = now + 5_000L;
                        }
                        String customerName = doc.getString("customerName");
                        String serviceName = doc.getString("serviceName");
                        String title = "Upcoming job";
                        String message = buildProviderMessage(customerName, serviceName);
                        scheduleAlarm(context, bookingId, triggerAt, title, message);
                        scheduled.add(key);
                    });
                    saveScheduled(context, scheduled);
                });
    }

    private static String buildMessage(String providerName, String serviceName) {
        if (!TextUtils.isEmpty(serviceName)) {
            return "Your " + serviceName + " appointment starts in 1 hour.";
        }
        if (!TextUtils.isEmpty(providerName)) {
            return "Your appointment with " + providerName + " starts in 1 hour.";
        }
        return "Your appointment starts in 1 hour.";
    }

    private static String buildProviderMessage(String customerName, String serviceName) {
        if (!TextUtils.isEmpty(serviceName)) {
            return "Your " + serviceName + " job starts in 1 hour.";
        }
        if (!TextUtils.isEmpty(customerName)) {
            return "Your job for " + customerName + " starts in 1 hour.";
        }
        return "Your job starts in 1 hour.";
    }

    private static void scheduleAlarm(Context context,
                                      String bookingId,
                                      long triggerAt,
                                      String title,
                                      String message) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_TITLE, title);
        intent.putExtra(ReminderReceiver.EXTRA_MESSAGE, message);
        intent.putExtra(ReminderReceiver.EXTRA_TIMESTAMP, System.currentTimeMillis());
        int requestCode = bookingId.hashCode();
        intent.putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, requestCode);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager == null) {
            return;
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // Use inexact alarm to avoid SCHEDULE_EXACT_ALARM permission.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        }
    }

    private static Set<String> loadScheduled(Context context) {
        Set<String> stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet(KEY_SCHEDULED, new HashSet<>());
        return stored == null ? new HashSet<>() : new HashSet<>(stored);
    }

    private static void saveScheduled(Context context, Set<String> values) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_SCHEDULED, values)
                .apply();
    }

    private static String buildKey(String role, String bookingId) {
        return role + ":" + bookingId;
    }
}
