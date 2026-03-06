package com.example.fixitfinderapp.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.fixitfinderapp.NotificationsActivity;
import com.example.fixitfinderapp.R;

public class ReminderReceiver extends BroadcastReceiver {

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_TIMESTAMP = "timestamp";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";
    private static final String CHANNEL_ID = "reminders_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }
        String title = intent.getStringExtra(EXTRA_TITLE);
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        long timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis());
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, (int) System.currentTimeMillis());

        NotificationStore.add(context, title, message, timestamp);
        showNotification(context, title, message, notificationId);
    }

    private void showNotification(Context context, String title, String message, int notificationId) {
        createChannel(context);
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
                .setContentTitle(title != null ? title : "Upcoming appointment")
                .setContentText(message != null ? message : "You have a reminder")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(notificationId, builder.build());
    }

    private void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Booking reminders");
        manager.createNotificationChannel(channel);
    }
}
