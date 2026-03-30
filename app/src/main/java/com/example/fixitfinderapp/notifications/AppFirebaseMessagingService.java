package com.example.fixitfinderapp.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;

import com.example.fixitfinderapp.ChatActivity;
import com.example.fixitfinderapp.MainTabsActivity;
import com.example.fixitfinderapp.R;
import com.example.fixitfinderapp.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

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
        Map<String, String> data = remoteMessage.getData();

        String title = firstNonEmpty(
                data.get("title"),
                remoteMessage.getNotification() != null
                        ? remoteMessage.getNotification().getTitle()
                        : null);
        String body = firstNonEmpty(
                data.get("body"),
                remoteMessage.getNotification() != null
                        ? remoteMessage.getNotification().getBody()
                        : null);
        if (TextUtils.isEmpty(title)) {
            title = "New message";
        }
        if (TextUtils.isEmpty(body)) {
            body = "";
        }

        String targetUserId = data.get("targetUserId");
        String conversationId = data.get("conversationId");
        String chatTitle = firstNonEmpty(data.get("chatTitle"), title);
        String avatarUri = data.get("avatarUri");
        String roleFromPayload = data.get("role");

        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();

        // Data-only FCM from backend includes targetUserId. Legacy messages may omit it.
        if (TextUtils.isEmpty(targetUserId)) {
            if (current == null) {
                // Logged out and cannot attribute — do not show tray or queue.
                return;
            }
            targetUserId = current.getUid();
        }

        boolean sameAccount = current != null && targetUserId.equals(current.getUid());
        if (!sameAccount) {
            PendingFcmNotificationQueue.enqueue(this, targetUserId, title, body,
                    !TextUtils.isEmpty(roleFromPayload) ? roleFromPayload : "user",
                    conversationId, chatTitle, avatarUri);
            return;
        }

        String role = !TextUtils.isEmpty(roleFromPayload)
                ? roleFromPayload
                : SessionManager.getRole(this);
        showNotification(title, body, conversationId, chatTitle, avatarUri);
        NotificationStore.addWithSource(this,
                title,
                body,
                System.currentTimeMillis(),
                role,
                "fcm");
    }

    private static String firstNonEmpty(String a, String b) {
        if (!TextUtils.isEmpty(a)) {
            return a;
        }
        return b != null ? b : "";
    }

    private void showNotification(String title, String body,
                                  String conversationId, String chatTitle, String avatarUri) {
        createChannel();
        Intent intent;
        if (conversationId != null && !conversationId.isEmpty()) {
            intent = new Intent(this, ChatActivity.class);
            intent.putExtra("conversationId", conversationId);
            intent.putExtra("title", chatTitle != null ? chatTitle : "Chat");
            intent.putExtra("avatarUri", avatarUri != null ? avatarUri : "");
        } else {
            intent = new Intent(this, MainTabsActivity.class);
            intent.putExtra(MainTabsActivity.EXTRA_INITIAL_TAB_ID, R.id.nav_messages);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) (System.currentTimeMillis() % Integer.MAX_VALUE),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), builder.build());
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
