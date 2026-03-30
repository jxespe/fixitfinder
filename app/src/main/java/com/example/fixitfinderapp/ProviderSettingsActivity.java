package com.example.fixitfinderapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;
import android.text.TextUtils;


import com.example.fixitfinderapp.auth.ProviderLoginActivity;
import com.example.fixitfinderapp.maps.AddressGeocoder;
import com.example.fixitfinderapp.payment.PaymentActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProviderSettingsActivity extends BaseSwipeActivity {

    private static final String PREF_PRIVACY = "privacy_settings";
    private static final String KEY_APP_LOCK = "app_lock";
    private static final String KEY_HIDE_PROFILE = "hide_profile";
    private static final String KEY_SHARE_LOCATION = "share_location";
    private static final String KEY_ANALYTICS = "analytics_enabled";

    private boolean isUpdatingSwitches = false;
    private String currentAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_settings);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        LinearLayout rowProfileInfo = findViewById(R.id.rowProfileInfo);
        LinearLayout rowBusinessDocuments = findViewById(R.id.rowBusinessDocuments);
        LinearLayout rowPaymentMethods = findViewById(R.id.rowPaymentMethods);
        LinearLayout rowPrivacySecurity = findViewById(R.id.rowPrivacySecurity);
        LinearLayout rowBugReport = findViewById(R.id.rowBugReport);
        LinearLayout rowFacebook = findViewById(R.id.rowFacebook);
        LinearLayout rowClearCache = findViewById(R.id.rowClearCache);
        LinearLayout rowLogout = findViewById(R.id.rowLogout);
        LinearLayout layoutPrivacySecurity = findViewById(R.id.layoutPrivacySecurity);
        ImageView ivPrivacyChevron = findViewById(R.id.ivPrivacyChevron);
        Switch switchAppLock = findViewById(R.id.switchAppLock);
        Switch switchHideProfile = findViewById(R.id.switchHideProfile);
        Switch switchShareLocation = findViewById(R.id.switchShareLocation);
        Switch switchAnalytics = findViewById(R.id.switchAnalytics);

        rowProfileInfo.setOnClickListener(v ->
                startActivity(new Intent(this, ProviderProfileActivity.class)));

        if (rowBusinessDocuments != null) {
            rowBusinessDocuments.setOnClickListener(v ->
                    startActivity(new Intent(this, ProviderDocumentsActivity.class)));
        }

        rowPaymentMethods.setOnClickListener(v ->
                startActivity(new Intent(this, PaymentActivity.class)));

        rowPrivacySecurity.setOnClickListener(v -> {
            boolean willShow = layoutPrivacySecurity.getVisibility() != android.view.View.VISIBLE;
            layoutPrivacySecurity.setVisibility(
                    willShow ? android.view.View.VISIBLE : android.view.View.GONE);
            if (ivPrivacyChevron != null) {
                ivPrivacyChevron.setRotation(willShow ? 90f : 0f);
            }
        });

        rowBugReport.setOnClickListener(v ->
                startActivity(new Intent(this, PrivacyReportActivity.class)));

        rowFacebook.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.facebook.com/FixItFinder"));
            startActivity(browserIntent);
        });

        rowClearCache.setOnClickListener(v ->
                Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show());

        rowLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            SessionManager.clear(this);
            Intent intent = new Intent(this, ProviderLoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finishAffinity();
        });

        initPrivacySwitches(switchAppLock, switchHideProfile, switchShareLocation, switchAnalytics);
        loadPrivacyFromFirestore(switchAppLock, switchHideProfile, switchShareLocation, switchAnalytics);

        NavigationHelper.setupBottomNav(this, R.id.nav_settings);
    }

    @Override
    protected void onStart() {
        super.onStart();
        NavigationHelper.ensureLoggedIn(this);
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigation =
                findViewById(R.id.bottomNavigation);
        NavigationHelper.updateMessageBadge(this, bottomNavigation);
    }

    private void initPrivacySwitches(Switch... switches) {
        android.content.SharedPreferences prefs =
                getSharedPreferences(PREF_PRIVACY, MODE_PRIVATE);
        setSwitchState(switches, prefs);
        for (Switch s : switches) {
            if (s == null) {
                continue;
            }
            s.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isUpdatingSwitches) {
                    return;
                }
                String key = switchKey(buttonView.getId());
                if (!TextUtils.isEmpty(key)) {
                    prefs.edit().putBoolean(key, isChecked).apply();
                    updatePrivacySetting(key, isChecked);
                }
            });
        }
    }

    private void setSwitchState(Switch[] switches, android.content.SharedPreferences prefs) {
        if (switches == null) {
            return;
        }
        isUpdatingSwitches = true;
        for (Switch s : switches) {
            if (s == null) {
                continue;
            }
            String key = switchKey(s.getId());
            boolean checked = prefs.getBoolean(key, defaultForKey(key));
            s.setChecked(checked);
        }
        isUpdatingSwitches = false;
    }

    private void loadPrivacyFromFirestore(Switch appLock,
                                          Switch hideProfile,
                                          Switch shareLocation,
                                          Switch analytics) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("providers")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    currentAddress = doc.getString("address");
                    applySwitchFromDoc(appLock, KEY_APP_LOCK, doc.getBoolean("appLockEnabled"));
                    applySwitchFromDoc(hideProfile, KEY_HIDE_PROFILE, doc.getBoolean("hideProfile"));
                    applySwitchFromDoc(shareLocation, KEY_SHARE_LOCATION, doc.getBoolean("shareLocation"));
                    applySwitchFromDoc(analytics, KEY_ANALYTICS, doc.getBoolean("analyticsEnabled"));
                });
    }

    private void applySwitchFromDoc(Switch target, String key, Boolean value) {
        if (target == null || value == null) {
            return;
        }
        android.content.SharedPreferences prefs =
                getSharedPreferences(PREF_PRIVACY, MODE_PRIVATE);
        prefs.edit().putBoolean(key, value).apply();
        isUpdatingSwitches = true;
        target.setChecked(value);
        isUpdatingSwitches = false;
    }

    private void updatePrivacySetting(String key, boolean value) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        if (KEY_APP_LOCK.equals(key)) {
            updates.put("appLockEnabled", value);
            if (value) {
                getSharedPreferences(PREF_PRIVACY, MODE_PRIVATE)
                        .edit()
                        .putLong("last_unlock", 0L)
                        .apply();
            }
        } else if (KEY_HIDE_PROFILE.equals(key)) {
            updates.put("hideProfile", value);
        } else if (KEY_SHARE_LOCATION.equals(key)) {
            updates.put("shareLocation", value);
        } else if (KEY_ANALYTICS.equals(key)) {
            updates.put("analyticsEnabled", value);
        }
        FirebaseFirestore.getInstance()
                .collection("providers")
                .document(user.getUid())
                .set(updates, com.google.firebase.firestore.SetOptions.merge());

        if (KEY_SHARE_LOCATION.equals(key) && value && !TextUtils.isEmpty(currentAddress)) {
            AddressGeocoder.updateLatLng(this, "providers", user.getUid(), currentAddress);
        }
    }

    private String switchKey(int id) {
        if (id == R.id.switchAppLock) {
            return KEY_APP_LOCK;
        }
        if (id == R.id.switchHideProfile) {
            return KEY_HIDE_PROFILE;
        }
        if (id == R.id.switchShareLocation) {
            return KEY_SHARE_LOCATION;
        }
        if (id == R.id.switchAnalytics) {
            return KEY_ANALYTICS;
        }
        return "";
    }

    private boolean defaultForKey(String key) {
        if (KEY_SHARE_LOCATION.equals(key)) {
            return true;
        }
        if (KEY_ANALYTICS.equals(key)) {
            return true;
        }
        return false;
    }
}
