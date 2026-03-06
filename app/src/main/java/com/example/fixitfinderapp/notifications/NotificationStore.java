package com.example.fixitfinderapp.notifications;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.fixitfinderapp.models.AppNotificationItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class NotificationStore {

    private static final String PREFS_NAME = "app_notifications";
    private static final String KEY_LIST = "items";

    private NotificationStore() {
    }

    public static void add(Context context, String title, String message, long timestamp) {
        if (context == null) {
            return;
        }
        try {
            JSONArray array = loadArray(context);
            JSONObject obj = new JSONObject();
            obj.put("title", title);
            obj.put("message", message);
            obj.put("timestamp", timestamp);
            array.put(obj);
            saveArray(context, array);
        } catch (Exception ignored) {
            // Best-effort storage.
        }
    }

    public static List<AppNotificationItem> load(Context context) {
        List<AppNotificationItem> items = new ArrayList<>();
        if (context == null) {
            return items;
        }
        try {
            JSONArray array = loadArray(context);
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

    private static JSONArray loadArray(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_LIST, "[]");
        try {
            return new JSONArray(raw);
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    private static void saveArray(Context context, JSONArray array) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LIST, array.toString()).apply();
    }
}
