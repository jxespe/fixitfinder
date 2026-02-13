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
            Intent intent = new Intent(this, ProviderLoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finishAffinity();
        });

        NavigationHelper.setupBottomNav(this, R.id.nav_settings);
    }

    @Override
    protected void onStart() {
        super.onStart();
        NavigationHelper.ensureLoggedIn(this);
    }
}
