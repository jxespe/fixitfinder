package com.example.fixitfinderapp;

import android.content.Intent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import com.example.fixitfinderapp.auth.LoginActivity;
import com.example.fixitfinderapp.auth.ProviderLoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public final class NavigationHelper {

    private NavigationHelper() {}

    public static void setupBottomNav(AppCompatActivity activity, int selectedItemId) {
        BottomNavigationView bottomNavigation = activity.findViewById(R.id.bottomNavigation);
        if (bottomNavigation == null) {
            return;
        }
        bottomNavigation.setSelectedItemId(selectedItemId);
        bottomNavigation.setOnItemSelectedListener(item -> {
            Intent intent = new Intent(activity, MainTabsActivity.class);
            intent.putExtra(MainTabsActivity.EXTRA_INITIAL_TAB_ID, item.getItemId());
            activity.startActivity(intent);
            activity.finish();
            return true;
        });
        updateMessageBadge(activity, bottomNavigation);
    }

    /**
     * Gesture detector for swipe-right / fling-to-back. Use from {@link BaseSwipeActivity}
     * via {@link #dispatchTouchEvent} so scrollable children still allow the gesture to be detected.
     */
    public static GestureDetectorCompat newSwipeBackGestureDetector(AppCompatActivity activity) {
        int minDistance = dpToPx(activity, 72);
        int minVelocity = Math.max(ViewConfiguration.get(activity).getScaledMinimumFlingVelocity(), 300);
        return new GestureDetectorCompat(activity,
                new android.view.GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2,
                                           float velocityX, float velocityY) {
                        if (e1 == null || e2 == null) {
                            return false;
                        }
                        float dx = e2.getX() - e1.getX();
                        float dy = e2.getY() - e1.getY();
                        if (Math.abs(dx) <= Math.abs(dy)) {
                            return false;
                        }
                        if (dx < minDistance || Math.abs(velocityX) < minVelocity) {
                            return false;
                        }
                        activity.getOnBackPressedDispatcher().onBackPressed();
                        return true;
                    }
                });
    }

    public static int tabIndexForMenu(int itemId) {
        if (itemId == R.id.nav_home) {
            return 0;
        }
        if (itemId == R.id.nav_history) {
            return 1;
        }
        if (itemId == R.id.nav_messages) {
            return 2;
        }
        if (itemId == R.id.nav_settings) {
            return 3;
        }
        return -1;
    }

    public static int menuForTabIndex(int index) {
        if (index == 0) {
            return R.id.nav_home;
        }
        if (index == 1) {
            return R.id.nav_history;
        }
        if (index == 2) {
            return R.id.nav_messages;
        }
        if (index == 3) {
            return R.id.nav_settings;
        }
        return 0;
    }

    private static int dpToPx(AppCompatActivity activity, int dp) {
        float density = activity.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    public static void updateMessageBadge(AppCompatActivity activity,
                                          BottomNavigationView bottomNavigation) {
        if (activity == null || bottomNavigation == null) {
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            bottomNavigation.removeBadge(R.id.nav_messages);
            return;
        }
        String role = SessionManager.getRole(activity);
        String unreadField = "provider".equalsIgnoreCase(role)
                ? "unreadProviderCount"
                : "unreadUserCount";
        String ownerField = "provider".equalsIgnoreCase(role)
                ? "providerId"
                : "userId";

        FirebaseFirestore.getInstance()
                .collection("conversations")
                .whereEqualTo(ownerField, user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    int total = 0;
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Long count = doc.getLong(unreadField);
                        if (count != null && count > 0) {
                            total += Math.min(Integer.MAX_VALUE, count.intValue());
                        }
                    }
                    if (total <= 0) {
                        bottomNavigation.removeBadge(R.id.nav_messages);
                        return;
                    }
                    com.google.android.material.badge.BadgeDrawable badge =
                            bottomNavigation.getOrCreateBadge(R.id.nav_messages);
                    badge.setVisible(true);
                    badge.setNumber(Math.min(total, 99));
                })
                .addOnFailureListener(e -> bottomNavigation.removeBadge(R.id.nav_messages));
    }

    public static void ensureLoggedIn(AppCompatActivity activity) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            maybeRequireAppLock(activity);
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

    private static void maybeRequireAppLock(AppCompatActivity activity) {
        if (activity instanceof AppLockActivity) {
            return;
        }
        android.content.SharedPreferences prefs =
                activity.getSharedPreferences("privacy_settings", AppCompatActivity.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean("app_lock", false);
        if (!enabled) {
            return;
        }
        long lastUnlock = prefs.getLong("last_unlock", 0L);
        long now = System.currentTimeMillis();
        long timeoutMs = 5 * 60 * 1000L;
        if (now - lastUnlock > timeoutMs) {
            Intent intent = new Intent(activity, AppLockActivity.class);
            activity.startActivity(intent);
        }
    }
}
