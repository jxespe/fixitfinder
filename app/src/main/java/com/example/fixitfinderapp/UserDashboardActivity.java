package com.example.fixitfinderapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.text.TextUtils;

import com.example.fixitfinderapp.notifications.ReminderScheduler;


/**
 * User home/dashboard screen that shows the service categories grid.
 */
public class UserDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_homepage);

        // Subscribe button – you can later navigate to a real SubscriptionActivity here
        Button btnSubscribe = findViewById(R.id.btnSubscribe);
        btnSubscribe.setOnClickListener(v ->
                Toast.makeText(this, "Subscribe flow coming soon", Toast.LENGTH_SHORT).show());

        TextView tvGreeting = findViewById(R.id.tvGreeting);
        setGreeting(tvGreeting);
        ImageView ivUserProfile = findViewById(R.id.ivUserProfile);
        loadProfilePhoto(ivUserProfile);
        wireProfileTaps(ivUserProfile);
        wireNotificationsIcon();

        wireCategoryCards();

        NavigationHelper.setupBottomNav(this, R.id.nav_home);
    }

    @Override
    protected void onStart() {
        super.onStart();
        NavigationHelper.ensureLoggedIn(this);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            ReminderScheduler.scheduleAcceptedReminders(this, user.getUid());
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
        ImageView bell = findViewById(R.id.ivHelp);
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
                ImageLoader.load(view, uriString, 0);
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

}

