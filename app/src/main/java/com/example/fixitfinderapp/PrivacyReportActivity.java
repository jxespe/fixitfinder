package com.example.fixitfinderapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class PrivacyReportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_report);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_settings);
        bottomNavigation.setOnItemSelectedListener(item -> {
            String role = SessionManager.getRole(this);
            boolean isProvider = "provider".equalsIgnoreCase(role);
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, isProvider
                        ? DashboardActivity.class
                        : UserDashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, isProvider
                        ? ProviderHistoryActivity.class
                        : HistoryActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_messages) {
                startActivity(new Intent(this, MessagesActivity.class));
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, isProvider
                        ? ProviderSettingsActivity.class
                        : UserSettingsActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }
}
