package com.example.fixitfinderapp.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.example.fixitfinderapp.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.fixitfinderapp.models.AppNotificationItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class NotificationStore {

    private static final String PREFS_NAME = "app_notifications";
    private static final String KEY_LIST = "items";
    private static final String ROLE_USER = "user";
    private static final String ROLE_PROVIDER = "provider";

    private NotificationStore() {
    }

    public static void add(Context context, String title, String message, long timestamp) {
        String role = SessionManager.getRole(context);
        add(context, title, message, timestamp, role);
    }

    public static void add(Context context,
                           String title,
                           String message,
                           long timestamp,
                           String role) {
        String userKey = resolveUserKey();
        if (TextUtils.isEmpty(userKey)) {
            return;
        }
        add(context, title, message, timestamp, role, userKey, "device");
    }

    /**
     * Resolves the current user and tags the Firestore row (e.g. {@code "fcm"}, {@code "booking_update"}).
     */
    public static void addWithSource(Context context,
                                     String title,
                                     String message,
                                     long timestamp,
                                     String role,
                                     String firestoreSource) {
        String userKey = resolveUserKey();
        if (TextUtils.isEmpty(userKey)) {
            return;
        }
        add(context, title, message, timestamp, role, userKey, firestoreSource);
    }

    public static void add(Context context,
                           String title,
                           String message,
                           long timestamp,
                           String role,
                           String userId) {
        add(context, title, message, timestamp, role, userId, "device");
    }

    public static void add(Context context,
                           String title,
                           String message,
                           long timestamp,
                           String role,
                           String userId,
                           String firestoreSource) {
        if (context == null) {
            return;
        }
        try {
            JSONArray array = loadArray(context, role, userId);
            JSONObject obj = new JSONObject();
            obj.put("title", title);
            obj.put("message", message);
            obj.put("timestamp", timestamp);
            array.put(obj);
            saveArray(context, array, role, userId);
            NotificationFirestoreSync.persistForMatchingUser(context, title, message, role, userId,
                    TextUtils.isEmpty(firestoreSource) ? "device" : firestoreSource);
        } catch (Exception ignored) {
            // Best-effort storage.
        }
    }

    public static List<AppNotificationItem> load(Context context) {
        String role = SessionManager.getRole(context);
        return load(context, role);
    }

    public static List<AppNotificationItem> load(Context context, String role) {
        String userId = resolveUserId();
        String email = resolveUserEmail();
        if (TextUtils.isEmpty(userId) && TextUtils.isEmpty(email)) {
            return new ArrayList<>();
        }
        List<AppNotificationItem> items = new ArrayList<>();
        if (!TextUtils.isEmpty(userId)) {
            items.addAll(load(context, role, userId));
        }
        if (!TextUtils.isEmpty(email) && (TextUtils.isEmpty(userId)
                || !email.equalsIgnoreCase(userId))) {
            // Merge legacy email-based items if they exist.
            items.addAll(load(context, role, email));
        }
        if (items.isEmpty() && !TextUtils.isEmpty(userId)) {
            // One-time fallback: claim legacy role-only notifications.
            JSONArray legacy = loadArrayByKey(context, buildKey(role, null));
            if (legacy.length() > 0) {
                saveArray(context, legacy, role, userId);
                clearByKey(context, buildKey(role, null));
                items.addAll(load(context, role, userId));
            }
        }
        return items;
    }

    public static List<AppNotificationItem> load(Context context, String role, String userId) {
        List<AppNotificationItem> items = new ArrayList<>();
        if (context == null) {
            return items;
        }
        try {
            JSONArray array = loadArray(context, role, userId);
            for (int i = array.length() - 1; i >= 0; i--) {
                JSONObject obj = array.optJSONObject(i);
                if (obj == null) {
                    continue;
                }
                String title = obj.optString("title", "Notification");
                String message = obj.optString("message", "");
                long timestamp = obj.optLong("timestamp", 0L);
                items.add(new AppNotificationItem(title, message, timestamp));
            }
        } catch (Exception ignored) {
            // Best-effort parsing.
        }
        return items;
    }

    private static JSONArray loadArray(Context context, String role, String userId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(buildKey(role, userId), "[]");
        try {
            return new JSONArray(raw);
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    private static JSONArray loadArrayByKey(Context context, String key) {
        if (context == null) {
            return new JSONArray();
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(key, "[]");
        try {
            return new JSONArray(raw);
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    private static void saveArray(Context context, JSONArray array, String role, String userId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(buildKey(role, userId), array.toString()).apply();
    }

    private static void clearByKey(Context context, String key) {
        if (context == null) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(key).apply();
    }

    private static String buildKey(String role, String userId) {
        if (TextUtils.isEmpty(role)) {
            return KEY_LIST + "_" + ROLE_USER + suffixUser(userId);
        }
        String normalized = role.trim().toLowerCase(java.util.Locale.US);
        if (ROLE_PROVIDER.equals(normalized)) {
            return KEY_LIST + "_" + ROLE_PROVIDER + suffixUser(userId);
        }
        return KEY_LIST + "_" + ROLE_USER + suffixUser(userId);
    }

    private static String suffixUser(String userId) {
        if (TextUtils.isEmpty(userId)) {
            return "";
        }
        return "_" + userId;
    }

    private static String resolveUserId() {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            return user != null ? user.getUid() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String resolveUserEmail() {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String email = user != null ? user.getEmail() : null;
            return TextUtils.isEmpty(email) ? null : email.trim().toLowerCase(java.util.Locale.US);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String resolveUserKey() {
        String uid = resolveUserId();
        if (!TextUtils.isEmpty(uid)) {
            return uid;
        }
        return resolveUserEmail();
    }
}
