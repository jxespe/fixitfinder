package com.example.fixitfinderapp.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.fixitfinderapp.NotificationsActivity;
import com.example.fixitfinderapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class BookingUpdateListener {

    private static final String TAG = "BookingUpdateListener";
    private static final String CHANNEL_ID = "booking_updates";
    private static BookingUpdateListener instance;
    private ListenerRegistration registration;

    public static synchronized BookingUpdateListener getInstance() {
        if (instance == null) {
            instance = new BookingUpdateListener();
        }
        return instance;
    }

    public void start(Context context) {
        if (context == null) {
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        stop();
        Context appContext = context.getApplicationContext();
        final String uid = user.getUid();
        registration = FirebaseFirestore.getInstance()
                .collection("user_notifications")
                .whereEqualTo("userId", uid)
                .whereEqualTo("delivered", false)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.w(TAG, "user_notifications listener failed (check Firestore rules/indexes)", error);
                        return;
                    }
                    if (snapshot == null) {
                        return;
                    }
                    if (!snapshot.getDocumentChanges().isEmpty()) {
                        for (DocumentChange change : snapshot.getDocumentChanges()) {
                            if (change.getType() != DocumentChange.Type.ADDED) {
                                continue;
                            }
                            handleUndeliveredBookingDoc(appContext, uid, change.getDocument());
                        }
                    } else if (!snapshot.isEmpty()) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            if (Boolean.TRUE.equals(doc.getBoolean("delivered"))) {
                                continue;
                            }
                            handleUndeliveredBookingDoc(appContext, uid, doc);
                        }
                    }
                });
    }

    public void stop() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    private void handleUndeliveredBookingDoc(Context appContext, String uid, DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return;
        }
        if (Boolean.TRUE.equals(doc.getBoolean("delivered"))) {
            return;
        }
        String title = doc.getString("title");
        String message = doc.getString("message");
        long timestamp = System.currentTimeMillis();
        NotificationStore.addWithSource(appContext,
                TextUtils.isEmpty(title) ? "Booking update" : title,
                TextUtils.isEmpty(message) ? "Your booking was updated." : message,
                timestamp,
                "user",
                "booking_update");
        ReminderScheduler.scheduleAcceptedReminders(appContext, uid);
        showTrayNotification(appContext,
                TextUtils.isEmpty(title) ? "Booking update" : title,
                TextUtils.isEmpty(message) ? "Your booking was updated." : message,
                (int) (timestamp % Integer.MAX_VALUE));
        markDelivered(doc.getId());
    }

    private void showTrayNotification(Context context, String title, String body, int notificationId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Booking updates",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Job status updates from your provider");
            manager.createNotificationChannel(channel);
        }
        Intent openIntent = new Intent(context, NotificationsActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(notificationId, builder.build());
    }

    private void markDelivered(String docId) {
        if (TextUtils.isEmpty(docId)) {
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("delivered", true);
        updates.put("deliveredAt", FieldValue.serverTimestamp());
        FirebaseFirestore.getInstance()
                .collection("user_notifications")
                .document(docId)
                .update(updates)
                .addOnFailureListener(e ->
                        Log.w(TAG, "markDelivered failed for " + docId, e));
    }
}
