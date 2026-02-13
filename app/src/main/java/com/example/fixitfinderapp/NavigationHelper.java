package com.example.fixitfinderapp;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitfinderapp.auth.LoginActivity;
import com.example.fixitfinderapp.auth.ProviderLoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public final class NavigationHelper {

    private NavigationHelper() {}

    public static void setupBottomNav(AppCompatActivity activity, int selectedItemId) {
        BottomNavigationView bottomNavigation = activity.findViewById(R.id.bottomNavigation);
        if (bottomNavigation == null) {
            return;
        }
        bottomNavigation.setSelectedItemId(selectedItemId);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            String role = SessionManager.getRole(activity);
            boolean isProvider = "provider".equalsIgnoreCase(role);
            if (id == R.id.nav_home) {
                activity.startActivity(new Intent(activity,
                        isProvider ? DashboardActivity.class : UserDashboardActivity.class));
                activity.finish();
                return true;
            } else if (id == R.id.nav_history) {
                activity.startActivity(new Intent(activity,
                        isProvider ? ProviderHistoryActivity.class : HistoryActivity.class));
                activity.finish();
                return true;
            } else if (id == R.id.nav_messages) {
                activity.startActivity(new Intent(activity, MessagesActivity.class));
                return true;
            } else if (id == R.id.nav_settings) {
                activity.startActivity(new Intent(activity,
                        isProvider ? ProviderSettingsActivity.class : UserSettingsActivity.class));
                activity.finish();
                return true;
            }
            return false;
        });
    }

    public static void ensureLoggedIn(AppCompatActivity activity) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return;
        }
        String role = SessionManager.getRole(activity);
        SessionManager.clear(activity);
        Intent intent = new Intent(activity,
                "provider".equalsIgnoreCase(role) ? ProviderLoginActivity.class : LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
