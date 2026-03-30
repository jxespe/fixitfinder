package com.example.fixitfinderapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.fixitfinderapp.auth.LoginActivity;
import com.example.fixitfinderapp.maps.AddressGeocoder;
import com.example.fixitfinderapp.payment.PaymentActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserSettingsFragment extends Fragment {

    private static final String PREF_PRIVACY = "privacy_settings";
    private static final String KEY_APP_LOCK = "app_lock";
    private static final String KEY_HIDE_PROFILE = "hide_profile";
    private static final String KEY_SHARE_LOCATION = "share_location";
    private static final String KEY_ANALYTICS = "analytics_enabled";

    private boolean isUpdatingSwitches = false;
    private String currentAddress;
    private FusedLocationProviderClient locationClient;
    private static final int REQ_LOCATION = 302;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_user_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setVisibility(View.GONE);
        }

        LinearLayout rowProfileInfo = view.findViewById(R.id.rowProfileInfo);
        LinearLayout rowEditProfile = view.findViewById(R.id.rowEditProfile);
        LinearLayout rowPaymentMethods = view.findViewById(R.id.rowPaymentMethods);
        LinearLayout rowPrivacySecurity = view.findViewById(R.id.rowPrivacySecurity);
        LinearLayout rowBugReport = view.findViewById(R.id.rowBugReport);
        LinearLayout rowFacebook = view.findViewById(R.id.rowFacebook);
        LinearLayout rowClearCache = view.findViewById(R.id.rowClearCache);
        LinearLayout rowLogout = view.findViewById(R.id.rowLogout);
        LinearLayout layoutPrivacySecurity = view.findViewById(R.id.layoutPrivacySecurity);
        ImageView ivPrivacyChevron = view.findViewById(R.id.ivPrivacyChevron);
        Switch switchAppLock = view.findViewById(R.id.switchAppLock);
        Switch switchHideProfile = view.findViewById(R.id.switchHideProfile);
        Switch switchShareLocation = view.findViewById(R.id.switchShareLocation);
        Switch switchAnalytics = view.findViewById(R.id.switchAnalytics);

        rowProfileInfo.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), UserProfileActivity.class)));

        rowEditProfile.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        rowPaymentMethods.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PaymentActivity.class)));

        rowPrivacySecurity.setOnClickListener(v -> {
            boolean willShow = layoutPrivacySecurity.getVisibility() != View.VISIBLE;
            layoutPrivacySecurity.setVisibility(
                    willShow ? View.VISIBLE : View.GONE);
            if (ivPrivacyChevron != null) {
                ivPrivacyChevron.setRotation(willShow ? 90f : 0f);
            }
        });

        rowBugReport.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PrivacyReportActivity.class)));

        rowFacebook.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.facebook.com/FixItFinder"));
            startActivity(browserIntent);
        });

        rowClearCache.setOnClickListener(v -> {
            boolean cleared = clearAppCache();
            Toast.makeText(requireContext(), cleared ? "Cache cleared" : "Unable to clear cache",
                    Toast.LENGTH_SHORT).show();
        });

        rowLogout.setOnClickListener(v -> {
            com.example.fixitfinderapp.notifications.BookingUpdateListener.getInstance().stop();
            FirebaseAuth.getInstance().signOut();
            SessionManager.clear(requireContext());
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finishAffinity();
        });

        initPrivacySwitches(switchAppLock, switchHideProfile, switchShareLocation, switchAnalytics);
        loadPrivacyFromFirestore(switchAppLock, switchHideProfile, switchShareLocation, switchAnalytics);

        locationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        View bottomNavigation = view.findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setVisibility(View.GONE);
        }
        // Layout padding was for embedded nav; MainTabs host already clears space above bar.
        View scrollContent = view.findViewById(R.id.scrollContent);
        if (scrollContent != null) {
            int endPad = (int) (16f * getResources().getDisplayMetrics().density + 0.5f);
            scrollContent.setPadding(
                    scrollContent.getPaddingLeft(),
                    scrollContent.getPaddingTop(),
                    scrollContent.getPaddingRight(),
                    endPad);
        }
    }

    private void openSupportEmail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:support@fixitfinderapp.com"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "FixIt Finder - Bug Report");
        intent.putExtra(Intent.EXTRA_TEXT, "Describe the issue here...");
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(requireContext(), "No email app found.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean clearAppCache() {
        return deleteDir(requireContext().getCacheDir());
    }

    private boolean deleteDir(java.io.File dir) {
        if (dir == null) {
            return false;
        }
        java.io.File[] files = dir.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
                if (file.isDirectory()) {
                    deleteDir(file);
                } else {
                    file.delete();
                }
            }
        }
        return dir.delete();
    }

    private void initPrivacySwitches(Switch... switches) {
        android.content.SharedPreferences prefs =
                requireContext().getSharedPreferences(PREF_PRIVACY, android.content.Context.MODE_PRIVATE);
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
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        return;
                    }
                    currentAddress = doc.getString("address");
                    boolean appLockValue = doc.getBoolean("appLock") != null && doc.getBoolean("appLock");
                    boolean hideProfileValue = doc.getBoolean("hideProfile") != null && doc.getBoolean("hideProfile");
                    boolean shareLocationValue = doc.getBoolean("shareLocation") == null
                            || doc.getBoolean("shareLocation");
                    boolean analyticsValue = doc.getBoolean("analyticsEnabled") == null
                            || doc.getBoolean("analyticsEnabled");
                    isUpdatingSwitches = true;
                    if (appLock != null) appLock.setChecked(appLockValue);
                    if (hideProfile != null) hideProfile.setChecked(hideProfileValue);
                    if (shareLocation != null) shareLocation.setChecked(shareLocationValue);
                    if (analytics != null) analytics.setChecked(analyticsValue);
                    isUpdatingSwitches = false;
                });
    }

    private void updatePrivacySetting(String key, boolean value) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        if (KEY_APP_LOCK.equals(key)) {
            updates.put("appLock", value);
        } else if (KEY_HIDE_PROFILE.equals(key)) {
            updates.put("hideProfile", value);
        } else if (KEY_SHARE_LOCATION.equals(key)) {
            updates.put("shareLocation", value);
        } else if (KEY_ANALYTICS.equals(key)) {
            updates.put("analyticsEnabled", value);
        }
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .set(updates, com.google.firebase.firestore.SetOptions.merge());

        if (KEY_SHARE_LOCATION.equals(key) && value && !TextUtils.isEmpty(currentAddress)) {
            AddressGeocoder.updateLatLng(requireContext(), "users", user.getUid(), currentAddress);
            requestPreciseLocation(user);
        }
    }

    private void requestPreciseLocation(FirebaseUser user) {
        if (user == null) {
            return;
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
            return;
        }
        fetchDeviceLocation(user);
    }

    private void fetchDeviceLocation(FirebaseUser user) {
        if (locationClient == null || user == null) {
            return;
        }
        locationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        return;
                    }
                    java.util.Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("lat", location.getLatitude());
                    updates.put("lng", location.getLongitude());
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(user.getUid())
                            .set(updates, com.google.firebase.firestore.SetOptions.merge());
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            fetchDeviceLocation(user);
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
