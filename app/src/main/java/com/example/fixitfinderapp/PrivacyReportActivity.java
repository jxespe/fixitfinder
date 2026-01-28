package com.example.fixitfinderapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

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
                startActivity(new Intent(this, UserSettingsActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }
}
