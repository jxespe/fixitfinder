package com.example.fixitfinderapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitfinderapp.auth.LoginActivity;
import com.example.fixitfinderapp.auth.OtpActivity;
import com.example.fixitfinderapp.auth.ProviderLoginActivity;
import com.example.fixitfinderapp.notifications.MessagingHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Button btnLoginUser = findViewById(R.id.btnLoginUser);
        Button btnLoginService = findViewById(R.id.btnLoginService);
        android.widget.TextView tvTitle = findViewById(R.id.tvTitle);

        btnLoginUser.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));

        btnLoginService.setOnClickListener(v ->
                startActivity(new Intent(this, ProviderLoginActivity.class)));

        tvTitle.setOnLongClickListener(v -> {
            startActivity(new Intent(this, TestMenuActivity.class));
            return true;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            routeAfterLogin(user);
        }
    }

    private void routeAfterLogin(FirebaseUser user) {
        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String role = doc.getString("role");
                        Boolean phoneVerified = doc.getBoolean("phoneVerified");
                        String phone = doc.getString("phone");
                        boolean verified = phoneVerified != null && phoneVerified;
                        if (!verified) {
                            goToOtp(phone, role != null ? role : "user");
                            return;
                        }
                        SessionManager.saveRole(this, role != null ? role : "user");
                        MessagingHelper.syncToken(this);
                        if ("provider".equalsIgnoreCase(role)) {
                            startActivity(new Intent(this, DashboardActivity.class));
                        } else {
                            startActivity(new Intent(this, UserDashboardActivity.class));
                        }
                        finish();
                        return;
                    }
                    routeProviderAfterLogin(user);
                })
                .addOnFailureListener(e -> {
                    String cachedRole = SessionManager.getRole(this);
                    if ("provider".equalsIgnoreCase(cachedRole)) {
                        startActivity(new Intent(this, DashboardActivity.class));
                    } else if ("user".equalsIgnoreCase(cachedRole)) {
                        startActivity(new Intent(this, UserDashboardActivity.class));
                    } else {
                        goToOtp(null, "user");
                    }
                    finish();
                });
    }

    private void routeProviderAfterLogin(FirebaseUser user) {
        db.collection("providers")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        return;
                    }
                    Boolean phoneVerified = doc.getBoolean("phoneVerified");
                    String phone = doc.getString("phone");
                    boolean verified = phoneVerified != null && phoneVerified;
                    if (!verified) {
                        goToOtp(phone, "provider");
                        return;
                    }
                    SessionManager.saveRole(this, "provider");
                    MessagingHelper.syncToken(this);
                    startActivity(new Intent(this, DashboardActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> goToOtp(null, "provider"));
    }

    private void goToOtp(String phone, String role) {
        Intent intent = new Intent(this, OtpActivity.class);
        if (phone != null) {
            intent.putExtra("phone", phone);
        }
        intent.putExtra("role", role);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}