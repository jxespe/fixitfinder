package com.example.fixitfinderapp.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;
import java.util.Set;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

public final class ReminderScheduler {

    private static final String PREFS_NAME = "reminder_prefs";
    private static final String KEY_SCHEDULED = "scheduled_ids";
    private static final String ROLE_USER = "user";
    private static final String ROLE_PROVIDER = "provider";
    private static final boolean USE_LOCAL_REMINDERS = false;

    private ReminderScheduler() {
    }

    public static void scheduleAcceptedReminders(Context context, String userId) {
        if (!USE_LOCAL_REMINDERS) {
            return;
        }
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
                        boolean immediate = false;
                        if (triggerAt < now) {
                            // If within 1 hour already, fire soon.
                            triggerAt = now + 5_000L;
                            immediate = true;
                        }
                        String providerName = doc.getString("providerName");
                        String serviceName = doc.getString("serviceName");
                        String title = "Upcoming appointment";
                        String message = buildMessage(providerName, serviceName, scheduledAt, immediate);
                        String alarmKey = buildAlarmKey(ROLE_USER, bookingId, scheduledAt);
                        scheduleAlarm(context, bookingId, alarmKey, triggerAt, title, message, ROLE_USER, userId);
                        scheduled.add(key);
                    });
                    saveScheduled(context, scheduled);
                });
    }

    public static void scheduleProviderAcceptedReminders(Context context, String providerId) {
        if (!USE_LOCAL_REMINDERS) {
            return;
        }
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
                        boolean immediate = false;
                        if (triggerAt < now) {
                            triggerAt = now + 5_000L;
                            immediate = true;
                        }
                        String customerName = doc.getString("customerName");
                        String serviceName = doc.getString("serviceName");
                        String title = "Upcoming job";
                        String message = buildProviderMessage(customerName, serviceName, scheduledAt, immediate);
                        String alarmKey = buildAlarmKey(ROLE_PROVIDER, bookingId, scheduledAt);
                        scheduleAlarm(context, bookingId, alarmKey, triggerAt, title, message, ROLE_PROVIDER, providerId);
                        scheduled.add(key);
                    });
                    saveScheduled(context, scheduled);
                });
    }

    private static String buildMessage(String providerName,
                                       String serviceName,
                                       long scheduledAt,
                                       boolean immediate) {
        String timeLabel = formatTime(scheduledAt);
        if (!immediate) {
            if (!TextUtils.isEmpty(serviceName)) {
                return "Your " + serviceName + " appointment starts in 1 hour at " + timeLabel + " today.";
            }
            if (!TextUtils.isEmpty(providerName)) {
                return "Your appointment with " + providerName + " starts in 1 hour at " + timeLabel + " today.";
            }
            return "Your appointment starts in 1 hour at " + timeLabel + " today.";
        }
        if (!TextUtils.isEmpty(serviceName)) {
            return "Your " + serviceName + " appointment will start at " + timeLabel + " today.";
        }
        if (!TextUtils.isEmpty(providerName)) {
            return "Your appointment with " + providerName + " will start at " + timeLabel + " today.";
        }
        return "Your appointment will start at " + timeLabel + " today.";
    }

    private static String buildProviderMessage(String customerName,
                                               String serviceName,
                                               long scheduledAt,
                                               boolean immediate) {
        String timeLabel = formatTime(scheduledAt);
        if (!immediate) {
            if (!TextUtils.isEmpty(serviceName)) {
                return "Your " + serviceName + " job starts in 1 hour at " + timeLabel + " today.";
            }
            if (!TextUtils.isEmpty(customerName)) {
                return "Your job for " + customerName + " starts in 1 hour at " + timeLabel + " today.";
            }
            return "Your job starts in 1 hour at " + timeLabel + " today.";
        }
        if (!TextUtils.isEmpty(serviceName)) {
            return "Your " + serviceName + " job will start at " + timeLabel + " today.";
        }
        if (!TextUtils.isEmpty(customerName)) {
            return "Your job for " + customerName + " will start at " + timeLabel + " today.";
        }
        return "Your job will start at " + timeLabel + " today.";
    }

    private static String formatTime(long scheduledAt) {
        SimpleDateFormat formatter = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return formatter.format(new Date(scheduledAt));
    }

    private static void scheduleAlarm(Context context,
                                      String bookingId,
                                      String alarmKey,
                                      long triggerAt,
                                      String title,
                                      String message,
                                      String role,
                                      String userId) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        if (!TextUtils.isEmpty(alarmKey)) {
            intent.setData(Uri.parse("fixitfinder://reminder/" + alarmKey));
        }
        intent.putExtra(ReminderReceiver.EXTRA_ROLE, role);
        intent.putExtra(ReminderReceiver.EXTRA_USER_ID, userId);
        intent.putExtra(ReminderReceiver.EXTRA_TITLE, title);
        intent.putExtra(ReminderReceiver.EXTRA_MESSAGE, message);
        intent.putExtra(ReminderReceiver.EXTRA_TIMESTAMP, System.currentTimeMillis());
        int requestCode = !TextUtils.isEmpty(alarmKey)
                ? alarmKey.hashCode()
                : bookingId.hashCode();
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

    private static String buildAlarmKey(String role, String bookingId, long scheduledAt) {
        return role + ":" + bookingId + ":" + scheduledAt;
    }
}
