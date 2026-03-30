package com.example.fixitfinderapp.notifications;

import android.content.Context;
import android.text.TextUtils;

import com.example.fixitfinderapp.SessionManager;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Persists in-app notification rows to Firestore so history and badges stay in sync across devices.
 */
public final class NotificationFirestoreSync {

    private NotificationFirestoreSync() {
    }

    public static void persist(Context context, String title, String message, String source) {
        if (context == null) {
            return;
        }
        String role = SessionManager.getRole(context);
        persistForRole(context, title, message, role, source);
    }

    public static void persistForRole(Context context,
                                      String title,
                                      String message,
                                      String role,
                                      String source) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || context == null) {
            return;
        }
        String normalizedRole = normalizeRole(role);
        Map<String, Object> data = baseFields(user.getUid(), normalizedRole, title, message, source);
        data.put("seen", false);
        data.put("createdAt", FieldValue.serverTimestamp());
        FirebaseFirestore.getInstance().collection("notifications").add(data);
    }

    /**
     * Uploads a legacy local notification with a fixed time (e.g. one-time device migration).
     */
    public static void persistLegacy(Context context,
                                     String title,
                                     String message,
                                     String role,
                                     long tsMillis,
                                     boolean seen) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || context == null) {
            return;
        }
        String normalizedRole = normalizeRole(role);
        Map<String, Object> data = baseFields(user.getUid(), normalizedRole, title, message, "migrated_local");
        data.put("seen", seen);
        if (tsMillis > 0L) {
            data.put("createdAt", new Timestamp(new Date(tsMillis)));
        } else {
            data.put("createdAt", FieldValue.serverTimestamp());
        }
        FirebaseFirestore.getInstance().collection("notifications").add(data);
    }

    /**
     * Writes to Firestore only if the signed-in account matches the local prefs user key (uid or email).
     */
    public static void persistForMatchingUser(Context context,
                                              String title,
                                              String message,
                                              String role,
                                              String storageUserKey,
                                              String source) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || context == null) {
            return;
        }
        if (!TextUtils.isEmpty(storageUserKey)) {
            if (!storageUserKey.equals(user.getUid())) {
                String email = user.getEmail();
                if (email == null
                        || !storageUserKey.equalsIgnoreCase(email.trim().toLowerCase(Locale.US))) {
                    return;
                }
            }
        }
        persistForRole(context, title, message, role, source);
    }

    private static Map<String, Object> baseFields(String userId,
                                                   String normalizedRole,
                                                   String title,
                                                   String message,
                                                   String source) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("role", normalizedRole);
        data.put("title", TextUtils.isEmpty(title) ? "Notification" : title);
        data.put("message", message != null ? message : "");
        data.put("source", TextUtils.isEmpty(source) ? "client" : source);
        return data;
    }

    private static String normalizeRole(String role) {
        if (TextUtils.isEmpty(role)) {
            return "user";
        }
        return "provider".equalsIgnoreCase(role.trim()) ? "provider" : "user";
    }
}
