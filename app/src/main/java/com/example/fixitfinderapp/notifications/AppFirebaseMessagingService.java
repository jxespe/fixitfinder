package com.example.fixitfinderapp.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.fixitfinderapp.ChatActivity;
import com.example.fixitfinderapp.MessagesActivity;
import com.example.fixitfinderapp.R;
import com.example.fixitfinderapp.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class AppFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "messages_channel";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        saveToken(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        String title = remoteMessage.getNotification() != null
                ? remoteMessage.getNotification().getTitle()
                : remoteMessage.getData().get("title");
        String body = remoteMessage.getNotification() != null
                ? remoteMessage.getNotification().getBody()
                : remoteMessage.getData().get("body");
        String conversationId = remoteMessage.getData().get("conversationId");
        String chatTitle = remoteMessage.getData().get("chatTitle");
        String avatarUri = remoteMessage.getData().get("avatarUri");

        showNotification(title, body, conversationId, chatTitle, avatarUri);
        NotificationStore.add(this,
                title != null ? title : "New message",
                body != null ? body : "You have a new message",
                System.currentTimeMillis());
    }

    private void showNotification(String title, String body,
                                  String conversationId, String chatTitle, String avatarUri) {
        createChannel();
        Intent intent;
        if (conversationId != null && !conversationId.isEmpty()) {
            intent = new Intent(this, ChatActivity.class);
            intent.putExtra("conversationId", conversationId);
            intent.putExtra("title", chatTitle != null ? chatTitle : "Chat");
            intent.putExtra("avatarUri", avatarUri);
        } else {
            intent = new Intent(this, MessagesActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title != null ? title : "New message")
                .setContentText(body != null ? body : "You have a new message")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Message notifications");
        manager.createNotificationChannel(channel);
    }

    private void saveToken(String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        String role = SessionManager.getRole(this);
        String collection = "provider".equalsIgnoreCase(role) ? "providers" : "users";
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("fcmToken", token);
        FirebaseFirestore.getInstance()
                .collection(collection)
                .document(user.getUid())
                .set(data, SetOptions.merge());
    }
}
