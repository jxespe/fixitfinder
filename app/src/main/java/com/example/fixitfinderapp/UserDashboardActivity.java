package com.example.fixitfinderapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.text.TextUtils;

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

        // Bottom navigation – wire basic tabs; real screens can be added later
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // Already on home; no-op
                return true;
            } else if (id == R.id.nav_history) {
                Toast.makeText(this, "History coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_messages) {
                Toast.makeText(this, "Messages coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, UserSettingsActivity.class));
                return true;
            }
            return false;
        });
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

