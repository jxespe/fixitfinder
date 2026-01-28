package com.example.fixitfinderapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitfinderapp.DashboardActivity;
import com.example.fixitfinderapp.R;
import com.example.fixitfinderapp.UserDashboardActivity;
import com.example.fixitfinderapp.auth.OtpActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        EditText edtEmail = findViewById(R.id.edtEmail);
        EditText edtPassword = findViewById(R.id.edtPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnSwitchToProvider = findViewById(R.id.btnSwitchToProvider);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("email")) {
            String email = intent.getStringExtra("email");
            if (email != null) {
                edtEmail.setText(email);
            }
        }

        final String role = intent != null && intent.hasExtra("role")
                ? intent.getStringExtra("role")
                : "user";

        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                edtEmail.setError("Email is required");
                edtEmail.requestFocus();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edtEmail.setError("Please enter a valid email address");
                edtEmail.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(password)) {
                edtPassword.setError("Password is required");
                edtPassword.requestFocus();
                return;
            }

            btnLogin.setEnabled(false);
            btnLogin.setText("Logging in...");

            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Login");

                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                            routeAfterLogin(role);
                        } else {
                            String errorMessage = "Login failed. Please check your credentials.";
                            if (task.getException() != null) {
                                errorMessage = task.getException().getMessage();
                            }
                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        btnSwitchToProvider.setOnClickListener(v ->
                startActivity(new Intent(this, ProviderLoginActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        redirectIfLoggedIn();
    }

    private void redirectIfLoggedIn() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return;
        }
        routeAfterLogin("user");
    }

    private void routeAfterLogin(String fallbackRole) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return;
        }
        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                    Boolean phoneVerified = doc.getBoolean("phoneVerified");
                    String phone = doc.getString("phone");
                    boolean verified = phoneVerified != null && phoneVerified;
                    String resolvedRole = role != null ? role : fallbackRole;
                    if (!verified) {
                        goToOtp(phone, resolvedRole);
                        return;
                    }
                    if ("provider".equalsIgnoreCase(resolvedRole)) {
                        goToProviderDashboard();
                    } else {
                        goToUserDashboard();
                    }
                })
                .addOnFailureListener(e -> goToOtp(null, fallbackRole));
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

    private void goToUserDashboard() {
        Intent intent = new Intent(this, UserDashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void goToProviderDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
