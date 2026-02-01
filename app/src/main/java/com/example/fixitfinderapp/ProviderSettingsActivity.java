package com.example.fixitfinderapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitfinderapp.auth.ProviderLoginActivity;
import com.example.fixitfinderapp.payment.PaymentActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class ProviderSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_settings);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        LinearLayout rowProfileInfo = findViewById(R.id.rowProfileInfo);
        LinearLayout rowPaymentMethods = findViewById(R.id.rowPaymentMethods);
        LinearLayout rowPrivacySecurity = findViewById(R.id.rowPrivacySecurity);
        LinearLayout rowBugReport = findViewById(R.id.rowBugReport);
        LinearLayout rowFacebook = findViewById(R.id.rowFacebook);
        LinearLayout rowClearCache = findViewById(R.id.rowClearCache);
        LinearLayout rowLogout = findViewById(R.id.rowLogout);

        rowProfileInfo.setOnClickListener(v ->
                startActivity(new Intent(this, ProviderProfileActivity.class)));

        rowPaymentMethods.setOnClickListener(v ->
                startActivity(new Intent(this, PaymentActivity.class)));

        rowPrivacySecurity.setOnClickListener(v ->
                startActivity(new Intent(this, PrivacyReportActivity.class)));

        rowBugReport.setOnClickListener(v ->
                Toast.makeText(this, "Bug report coming soon", Toast.LENGTH_SHORT).show());

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
            startActivity(new Intent(this, ProviderLoginActivity.class));
            finish();
        });

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_settings);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, ProviderHistoryActivity.class));
                finish();
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
}
