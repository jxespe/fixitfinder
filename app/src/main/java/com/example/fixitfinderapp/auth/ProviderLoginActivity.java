package com.example.fixitfinderapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.example.fixitfinderapp.BaseSwipeActivity;

import com.example.fixitfinderapp.MainTabsActivity;
import com.example.fixitfinderapp.NavigationHelper;
import com.example.fixitfinderapp.R;
import com.example.fixitfinderapp.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class ProviderLoginActivity extends BaseSwipeActivity {

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
                    btnPasswordToggle.setImageResource(R.drawable.ic_eye_open);
                    isPasswordVisible = false;
                } else {
                    edtPassword.setTransformationMethod(null);
                    btnPasswordToggle.setImageResource(R.drawable.ic_eye_slash);
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

        android.widget.Button btnForgot = findViewById(R.id.btnForgotPassword);
        if (btnForgot != null) {
            btnForgot.setOnClickListener(v -> {
                Intent fp = new Intent(this, ForgotPasswordActivity.class);
                fp.putExtra(ForgotPasswordActivity.EXTRA_ROLE_FILTER, "provider");
                startActivity(fp);
            });
        }

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
        db.collection("providers")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        FirebaseAuth.getInstance().signOut();
                        Toast.makeText(this, "Wrong login credentials",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    Boolean phoneVerified = doc.getBoolean("phoneVerified");
                    String phone = doc.getString("phone");
                    boolean verified = phoneVerified != null && phoneVerified;
                    String resolvedRole = "provider";
                    if (!verified) {
                        handleIncompleteRegistration();
                        return;
                    }
                    SessionManager.saveRole(this, resolvedRole);
                    updateFcmToken("providers");
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                    if ("provider".equalsIgnoreCase(resolvedRole)) {
                        goToProviderDashboard();
                    } else {
                        goToUserDashboard();
                    }
                })
                .addOnFailureListener(e -> handleIncompleteRegistration());
    }

    private void handleIncompleteRegistration() {
        SessionManager.clear(this);
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(this, "Please complete registration to continue.", Toast.LENGTH_LONG).show();
    }

    private void goToProviderDashboard() {
        Intent intent = new Intent(this, MainTabsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void goToUserDashboard() {
        Intent intent = new Intent(this, MainTabsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void updateFcmToken(String collection) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || TextUtils.isEmpty(collection)) {
            return;
        }
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (TextUtils.isEmpty(token)) {
                        return;
                    }
                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                    data.put("fcmToken", token);
                    db.collection(collection)
                            .document(user.getUid())
                            .set(data, com.google.firebase.firestore.SetOptions.merge());
                });
    }
}
