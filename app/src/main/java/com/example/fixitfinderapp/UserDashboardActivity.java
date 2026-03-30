package com.example.fixitfinderapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.messaging.FirebaseMessaging;

import android.text.TextUtils;

import com.example.fixitfinderapp.notifications.PendingFcmNotificationQueue;
import com.example.fixitfinderapp.notifications.ReminderScheduler;


/**
 * User home/dashboard screen that shows the service categories grid.
 */
public class UserDashboardActivity extends BaseSwipeActivity {

    private static final int REQ_NOTIFICATIONS = 1101;
    private TextView tvNotificationBadge;
    private ListenerRegistration notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_homepage);

        Button btnSubscribe = findViewById(R.id.btnSubscribe);
        btnSubscribe.setOnClickListener(v ->
                startActivity(new Intent(this, SubscriptionActivity.class)));

        TextView tvGreeting = findViewById(R.id.tvGreeting);
        setGreeting(tvGreeting);
        ImageView ivUserProfile = findViewById(R.id.ivUserProfile);
        loadProfilePhoto(ivUserProfile);
        wireProfileTaps(ivUserProfile);
        wireNotificationsIcon();
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);

        wireCategoryCards();

        NavigationHelper.setupBottomNav(this, R.id.nav_home);
    }

    @Override
    protected void onStart() {
        super.onStart();
        NavigationHelper.ensureLoggedIn(this);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            PendingFcmNotificationQueue.drainForUser(this, user.getUid());
            ReminderScheduler.scheduleAcceptedReminders(this, user.getUid());
            com.example.fixitfinderapp.notifications.BookingUpdateListener.getInstance()
                    .start(this);
            requestNotificationPermission();
            updateNotificationBadge();
            com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigation =
                    findViewById(R.id.bottomNavigation);
            NavigationHelper.updateMessageBadge(this, bottomNavigation);
            startNotificationBadgeListener(user.getUid());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNotificationBadge();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
    }

    private void wireCategoryCards() {
        findViewById(R.id.cardCarpentry)
                .setOnClickListener(v -> openCategory("Carpentry"));
        findViewById(R.id.cardPlumbing)
                .setOnClickListener(v -> openCategory("Plumbing"));
        findViewById(R.id.cardAirconRepairs)
                .setOnClickListener(v -> openCategory("Aircon Repairs"));
        findViewById(R.id.cardCarMechanic)
                .setOnClickListener(v -> openCategory("Car Mechanic"));
        findViewById(R.id.cardApplianceRepair)
                .setOnClickListener(v -> openCategory("Appliance Repair"));
        findViewById(R.id.cardElectricalRepair)
                .setOnClickListener(v -> openCategory("Electrical Repair"));
        findViewById(R.id.cardElectronicsRepair)
                .setOnClickListener(v -> openCategory("Electronics Repair"));
        findViewById(R.id.cardInternetTechnician)
                .setOnClickListener(v -> openCategory("Internet Technician"));
    }

    private void wireProfileTaps(ImageView... views) {
        if (views == null) {
            return;
        }
        View.OnClickListener listener =
                v -> startActivity(new Intent(this, UserProfileActivity.class));
        for (ImageView view : views) {
            if (view != null) {
                view.setOnClickListener(listener);
            }
        }
    }

    private void wireNotificationsIcon() {
        ImageView bell = findViewById(R.id.ivNotification);
        if (bell == null) {
            return;
        }
        bell.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));
    }

    private void openCategory(String name) {
        Intent intent = new Intent(this, ServiceCategoryActivity.class);
        intent.putExtra(ServiceCategoryActivity.EXTRA_CATEGORY_NAME, name);
        startActivity(intent);
    }

    private void setGreeting(TextView tvGreeting) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            tvGreeting.setText("Hello!");
            return;
        }

        String fallbackEmail = user.getEmail();
        String displayName = user.getDisplayName();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String firstName = doc.getString("firstName");
                    String fullName = doc.getString("fullName");
                    String preferredName = pickFirstName(firstName, fullName, displayName, fallbackEmail);
                    tvGreeting.setText("Hello, " + preferredName + "!");
                })
                .addOnFailureListener(e -> {
                    String preferredName = pickFirstName(null, null, displayName, fallbackEmail);
                    tvGreeting.setText("Hello, " + preferredName + "!");
                });
    }

    private void loadProfilePhoto(ImageView... views) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || views == null || views.length == 0) {
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String photo = doc.getString("photoUrl");
                    if (TextUtils.isEmpty(photo)) {
                        photo = doc.getString("profilePhotoUri");
                    }
                    if (TextUtils.isEmpty(photo) && user.getPhotoUrl() != null) {
                        photo = user.getPhotoUrl().toString();
                    }
                    if (!TextUtils.isEmpty(photo)) {
                        setProfileImage(views, photo);
                    }
                });
    }

    private void setProfileImage(ImageView[] views, String uriString) {
        if (views == null || views.length == 0 || TextUtils.isEmpty(uriString)) {
            return;
        }
        for (ImageView view : views) {
            if (view != null) {
                ImageLoader.loadProfile(view, uriString, 0);
            }
        }
    }

    private String pickFirstName(String firstName, String fullName, String displayName, String email) {
        if (!TextUtils.isEmpty(firstName)) {
            return firstName;
        }
        String fromFull = firstFromDisplay(fullName);
        if (!TextUtils.isEmpty(fromFull)) {
            return fromFull;
        }
        String fromDisplay = firstFromDisplay(displayName);
        if (!TextUtils.isEmpty(fromDisplay)) {
            return fromDisplay;
        }
        return !TextUtils.isEmpty(email) ? email : "there";
    }

    private String firstFromDisplay(String name) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        String[] parts = name.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : name;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            refreshFcmToken();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            refreshFcmToken();
            return;
        }
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                REQ_NOTIFICATIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIFICATIONS
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            refreshFcmToken();
        }
    }

    private void refreshFcmToken() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (TextUtils.isEmpty(token)) {
                        return;
                    }
                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                    data.put("fcmToken", token);
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(user.getUid())
                            .set(data, SetOptions.merge());
                });
    }

    private void updateNotificationBadge() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || tvNotificationBadge == null) {
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("userId", user.getUid())
                .whereEqualTo("seen", false)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(99)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int count = snapshot.size();
                    if (count <= 0) {
                        tvNotificationBadge.setVisibility(View.GONE);
                        return;
                    }
                    tvNotificationBadge.setVisibility(View.VISIBLE);
                    tvNotificationBadge.setText(count >= 99 ? "99+" : String.valueOf(count));
                })
                .addOnFailureListener(e -> {
                    // Keep last badge state on transient or index errors.
                });
    }

    private void startNotificationBadgeListener(String userId) {
        if (tvNotificationBadge == null || TextUtils.isEmpty(userId)) {
            return;
        }
        if (notificationListener != null) {
            notificationListener.remove();
        }
        notificationListener = FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("seen", false)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(99)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) {
                        return;
                    }
                    int count = snapshot.size();
                    if (count <= 0) {
                        tvNotificationBadge.setVisibility(View.GONE);
                        return;
                    }
                    tvNotificationBadge.setVisibility(View.VISIBLE);
                    tvNotificationBadge.setText(count >= 99 ? "99+" : String.valueOf(count));
                });
    }

}

