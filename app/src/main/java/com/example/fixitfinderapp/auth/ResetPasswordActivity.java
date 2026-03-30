package com.example.fixitfinderapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class ResetPasswordActivity extends BaseSwipeActivity {

    public static final String EXTRA_ROLE = "role";
    public static final String EXTRA_RESET_LOGIN_EMAIL = "resetLoginEmail";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText edtPassword;
    private EditText edtConfirmPassword;
    private ImageView btnPasswordToggle;
    private boolean isPasswordVisible;
    private String role = "user";
    private String resetLoginEmail = "";
    private android.widget.TextView tvPasswordRuleLength;
    private android.widget.TextView tvPasswordRuleLetter;
    private android.widget.TextView tvPasswordRuleNumber;
    private android.widget.TextView tvPasswordRuleMatch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        if (intent != null) {
            String r = intent.getStringExtra(EXTRA_ROLE);
            if (!TextUtils.isEmpty(r)) {
                role = r;
            }
            resetLoginEmail = intent.getStringExtra(EXTRA_RESET_LOGIN_EMAIL);
        }

        if (TextUtils.isEmpty(resetLoginEmail) || auth.getCurrentUser() == null) {
            Toast.makeText(this, "Session expired. Start again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnPasswordToggle = findViewById(R.id.btnPasswordToggle);
        Button btnSave = findViewById(R.id.btnSavePassword);
        ImageButton btnBack = findViewById(R.id.btnBack);
        tvPasswordRuleLength = findViewById(R.id.tvPasswordRuleLength);
        tvPasswordRuleLetter = findViewById(R.id.tvPasswordRuleLetter);
        tvPasswordRuleNumber = findViewById(R.id.tvPasswordRuleNumber);
        tvPasswordRuleMatch = findViewById(R.id.tvPasswordRuleMatch);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
        if (btnPasswordToggle != null) {
            btnPasswordToggle.setOnClickListener(v -> {
                if (isPasswordVisible) {
                    edtPassword.setTransformationMethod(new PasswordTransformationMethod());
                    edtConfirmPassword.setTransformationMethod(new PasswordTransformationMethod());
                    btnPasswordToggle.setImageResource(android.R.drawable.ic_menu_view);
                    isPasswordVisible = false;
                } else {
                    edtPassword.setTransformationMethod(null);
                    edtConfirmPassword.setTransformationMethod(null);
                    btnPasswordToggle.setImageResource(android.R.drawable.ic_menu_revert);
                    isPasswordVisible = true;
                }
                edtPassword.setSelection(edtPassword.getText().length());
            });
        }

        setupPasswordWatchers();

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> submitNewPassword());
        }
    }

    private void submitNewPassword() {
        String password = edtPassword != null ? edtPassword.getText().toString().trim() : "";
        String confirm = edtConfirmPassword != null ? edtConfirmPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Password is required");
            edtPassword.requestFocus();
            return;
        }
        if (!isStrongPassword(password)) {
            edtPassword.setError("Use 8+ characters with letters and numbers");
            edtPassword.requestFocus();
            return;
        }
        if (!password.equals(confirm)) {
            edtConfirmPassword.setError("Passwords do not match");
            edtConfirmPassword.requestFocus();
            return;
        }

        Button btn = findViewById(R.id.btnSavePassword);
        if (btn != null) {
            btn.setEnabled(false);
            btn.setText("Updating…");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("password", password);

        PasswordResetFunctions.getInstance()
                .getHttpsCallable("completePasswordReset")
                .call(data)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        if (btn != null) {
                            btn.setEnabled(true);
                            btn.setText("Update password");
                        }
                        Exception e = task.getException();
                        String msg = "Could not update password.";
                        if (e instanceof FirebaseFunctionsException) {
                            FirebaseFunctionsException fe = (FirebaseFunctionsException) e;
                            if (fe.getMessage() != null) {
                                msg = fe.getMessage();
                            }
                        } else if (e != null && e.getMessage() != null) {
                            msg = e.getMessage();
                        }
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        return;
                    }

                    final String pwd = password;
                    auth.signOut();
                    auth.signInWithEmailAndPassword(resetLoginEmail, pwd)
                            .addOnCompleteListener(signInTask -> {
                                if (btn != null) {
                                    btn.setEnabled(true);
                                    btn.setText("Update password");
                                }
                                if (!signInTask.isSuccessful()) {
                                    Toast.makeText(this,
                                            "Password updated. Sign in with your email and new password.",
                                            Toast.LENGTH_LONG).show();
                                    Class<?> dest = "provider".equalsIgnoreCase(role)
                                            ? ProviderLoginActivity.class
                                            : LoginActivity.class;
                                    Intent i = new Intent(this, dest);
                                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(i);
                                    finish();
                                    return;
                                }
                                routeAfterPasswordReset();
                            });
                });
    }

    private void routeAfterPasswordReset() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return;
        }
        String collection = "provider".equalsIgnoreCase(role) ? "providers" : "users";
        db.collection(collection)
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        auth.signOut();
                        Toast.makeText(this, "Account not found.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    Boolean phoneVerified = doc.getBoolean("phoneVerified");
                    if (phoneVerified == null || !phoneVerified) {
                        auth.signOut();
                        Toast.makeText(this, "Please complete account verification.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    SessionManager.saveRole(this, role);
                    updateFcmToken(collection);
                    Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, MainTabsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Could not load profile.", Toast.LENGTH_LONG).show();
                });
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
                    Map<String, Object> data = new HashMap<>();
                    data.put("fcmToken", token);
                    db.collection(collection)
                            .document(user.getUid())
                            .set(data, com.google.firebase.firestore.SetOptions.merge());
                });
    }

    private boolean isStrongPassword(String password) {
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        return password.length() >= 8 && hasLetter && hasDigit;
    }

    private void setupPasswordWatchers() {
        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                updatePasswordStatus();
            }
        };
        if (edtPassword != null) {
            edtPassword.addTextChangedListener(watcher);
        }
        if (edtConfirmPassword != null) {
            edtConfirmPassword.addTextChangedListener(watcher);
        }
        updatePasswordStatus();
    }

    private void updatePasswordStatus() {
        String password = edtPassword != null ? edtPassword.getText().toString() : "";
        String confirm = edtConfirmPassword != null ? edtConfirmPassword.getText().toString() : "";
        boolean hasLength = password.length() >= 8;
        boolean hasLetter = password.matches(".*[A-Za-z].*");
        boolean hasNumber = password.matches(".*\\d.*");
        boolean match = !TextUtils.isEmpty(confirm) && password.equals(confirm);

        setRuleColor(tvPasswordRuleLength, hasLength);
        setRuleColor(tvPasswordRuleLetter, hasLetter);
        setRuleColor(tvPasswordRuleNumber, hasNumber);
        if (tvPasswordRuleMatch != null) {
            tvPasswordRuleMatch.setText(match ? "• Passwords match" : "• Passwords do not match");
            setRuleColor(tvPasswordRuleMatch, match);
        }
    }

    private void setRuleColor(android.widget.TextView view, boolean ok) {
        if (view == null) {
            return;
        }
        view.setTextColor(ok ? android.graphics.Color.parseColor("#1B9C85")
                : android.graphics.Color.parseColor("#F44336"));
    }
}
