package com.example.fixitfinderapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitfinderapp.auth.LoginActivity;
import com.example.fixitfinderapp.payment.PaymentActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class UserSettingsActivity extends AppCompatActivity {

    private LinearLayout layoutPrivacyDetails;
    private ImageView ivPrivacyChevron;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_settings);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        LinearLayout rowProfileInfo = findViewById(R.id.rowProfileInfo);
        LinearLayout rowPaymentMethods = findViewById(R.id.rowPaymentMethods);
        LinearLayout rowPrivacySecurity = findViewById(R.id.rowPrivacySecurity);
        LinearLayout rowBugReport = findViewById(R.id.rowBugReport);
        LinearLayout rowFacebook = findViewById(R.id.rowFacebook);
        LinearLayout rowClearCache = findViewById(R.id.rowClearCache);
        LinearLayout rowLogout = findViewById(R.id.rowLogout);

        layoutPrivacyDetails = findViewById(R.id.layoutPrivacyDetails);
        ivPrivacyChevron = findViewById(R.id.ivPrivacyChevron);

        rowProfileInfo.setOnClickListener(v ->
                Toast.makeText(this, "Profile info coming soon", Toast.LENGTH_SHORT).show());

        rowPaymentMethods.setOnClickListener(v ->
                startActivity(new Intent(this, PaymentActivity.class)));

        rowPrivacySecurity.setOnClickListener(v -> togglePrivacyDetails());

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
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_settings);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, UserDashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_history) {
                Toast.makeText(this, "History coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_messages) {
                Toast.makeText(this, "Messages coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_settings) {
                return true;
            }
            return false;
        });
    }

    private void togglePrivacyDetails() {
        if (layoutPrivacyDetails.getVisibility() == View.VISIBLE) {
            layoutPrivacyDetails.setVisibility(View.GONE);
            ivPrivacyChevron.setRotation(0f);
        } else {
            layoutPrivacyDetails.setVisibility(View.VISIBLE);
            ivPrivacyChevron.setRotation(180f);
        }
    }
}
