package com.example.fixitfinderapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitfinderapp.DashboardActivity;
import com.example.fixitfinderapp.R;
import com.example.fixitfinderapp.UserDashboardActivity;
import com.example.fixitfinderapp.auth.OtpActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProviderLoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        EditText edtEmail = findViewById(R.id.edtEmail);
        EditText edtPassword = findViewById(R.id.edtPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnSwitchToUser = findViewById(R.id.btnSwitchToUser);
        ImageView btnPasswordToggle = findViewById(R.id.btnPasswordToggle);

        if (btnPasswordToggle != null) {
            btnPasswordToggle.setOnClickListener(v -> {
                if (isPasswordVisible) {
                    edtPassword.setTransformationMethod(new PasswordTransformationMethod());
                    btnPasswordToggle.setImageResource(android.R.drawable.ic_menu_view);
                    isPasswordVisible = false;
                } else {
                    edtPassword.setTransformationMethod(null);
                    btnPasswordToggle.setImageResource(android.R.drawable.ic_menu_revert);
                    isPasswordVisible = true;
                }
                edtPassword.setSelection(edtPassword.getText().length());
            });
        }

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
                            routeAfterLogin();
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
                startActivity(new Intent(this, ProviderRegisterActivity.class)));

        btnSwitchToUser.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
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
        routeAfterLogin();
    }

    private void routeAfterLogin() {
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
                    String resolvedRole = role != null ? role : "provider";
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

    private void goToProviderDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void goToUserDashboard() {
        Intent intent = new Intent(this, UserDashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
