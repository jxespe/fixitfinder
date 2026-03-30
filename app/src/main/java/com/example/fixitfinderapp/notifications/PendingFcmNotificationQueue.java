package com.example.fixitfinderapp.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Holds FCM payloads when the intended recipient is not signed in on this device, so they can be
 * applied to {@link NotificationStore} (and Firestore sync) after login. Prevents system tray
 * notifications while logged out by pairing with data-only FCM from the backend.
 */
public final class PendingFcmNotificationQueue {

    private static final String TAG = "PendingFcmQueue";
    private static final String PREFS = "pending_fcm_notifications";
    private static final String KEY_PREFIX = "q_";
    private static final int MAX_PER_USER = 150;

    private PendingFcmNotificationQueue() {
    }

    public static void enqueue(Context context,
                               String targetUserId,
                               String title,
                               String body,
                               String role,
                               String conversationId,
                               String chatTitle,
                               String avatarUri) {
        if (context == null || TextUtils.isEmpty(targetUserId)) {
            return;
        }
        try {
            JSONObject o = new JSONObject();
            o.put("title", title != null ? title : "");
            o.put("body", body != null ? body : "");
            o.put("ts", System.currentTimeMillis());
            o.put("role", !TextUtils.isEmpty(role) ? role : "user");
            if (!TextUtils.isEmpty(conversationId)) {
                o.put("conversationId", conversationId);
            }
            if (!TextUtils.isEmpty(chatTitle)) {
                o.put("chatTitle", chatTitle);
            }
            if (!TextUtils.isEmpty(avatarUri)) {
                o.put("avatarUri", avatarUri);
            }

            SharedPreferences prefs = context.getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String key = KEY_PREFIX + targetUserId;
            JSONArray arr = readArray(prefs, key);
            arr.put(o);
            while (arr.length() > MAX_PER_USER) {
                JSONArray trimmed = new JSONArray();
                for (int i = arr.length() - MAX_PER_USER; i < arr.length(); i++) {
                    trimmed.put(arr.get(i));
                }
                arr = trimmed;
            }
            prefs.edit().putString(key, arr.toString()).apply();
        } catch (Exception e) {
            Log.w(TAG, "enqueue failed", e);
        }
    }

    /**
     * Applies queued items for {@code uid} to notification storage (and cloud sync via
     * {@link NotificationStore}). Call on the main thread after auth is ready.
     */
    public static void drainForUser(Context context, String uid) {
        if (context == null || TextUtils.isEmpty(uid)) {
            return;
        }
        Context app = context.getApplicationContext();
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String key = KEY_PREFIX + uid;
        String raw = prefs.getString(key, null);
        if (TextUtils.isEmpty(raw)) {
            return;
        }
        prefs.edit().remove(key).apply();

        JSONArray arr;
        try {
            arr = new JSONArray(raw);
        } catch (Exception e) {
            Log.w(TAG, "drain parse failed", e);
            return;
        }

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) {
                continue;
            }
            String title = o.optString("title", "Notification");
            String body = o.optString("body", "");
            long ts = o.optLong("ts", System.currentTimeMillis());
            String role = o.optString("role", "user");
            try {
                NotificationStore.add(app, title, body, ts, role, uid, "fcm_pending");
            } catch (Exception e) {
                Log.w(TAG, "drain item failed", e);
            }
        }
    }

    private static JSONArray readArray(SharedPreferences prefs, String key) {
        String raw = prefs.getString(key, "[]");
        try {
            return new JSONArray(raw);
        } catch (Exception e) {
            return new JSONArray();
        }
    }
}
