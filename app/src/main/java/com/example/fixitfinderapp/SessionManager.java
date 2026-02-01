package com.example.fixitfinderapp;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREFS = "session_prefs";
    private static final String KEY_ROLE = "session_role";

    private SessionManager() { }

    public static void saveRole(Context context, String role) {
        if (context == null || role == null) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ROLE, role).apply();
    }

    public static String getRole(Context context) {
        if (context == null) {
            return "";
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ROLE, "");
    }

    public static void clear(Context context) {
        if (context == null) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
}
