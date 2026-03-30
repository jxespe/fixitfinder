package com.example.fixitfinderapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.graphics.Color;

import com.example.fixitfinderapp.BaseSwipeActivity;

import com.example.fixitfinderapp.MainTabsActivity;
import com.example.fixitfinderapp.NavigationHelper;
import com.example.fixitfinderapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class CreatePasswordActivity extends BaseSwipeActivity {

    private EditText edtPassword;
    private EditText edtConfirmPassword;
    private ImageView btnPasswordToggle;
    private boolean isPasswordVisible = false;
    private String role = "user";
    private android.widget.TextView tvPasswordRuleLength;
    private android.widget.TextView tvPasswordRuleLetter;
    private android.widget.TextView tvPasswordRuleNumber;
    private android.widget.TextView tvPasswordRuleMatch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_password);

        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnPasswordToggle = findViewById(R.id.btnPasswordToggle);
        Button btnSavePassword = findViewById(R.id.btnSavePassword);
        tvPasswordRuleLength = findViewById(R.id.tvPasswordRuleLength);
        tvPasswordRuleLetter = findViewById(R.id.tvPasswordRuleLetter);
        tvPasswordRuleNumber = findViewById(R.id.tvPasswordRuleNumber);
        tvPasswordRuleMatch = findViewById(R.id.tvPasswordRuleMatch);

        Intent intent = getIntent();
        if (intent != null) {
            String roleExtra = intent.getStringExtra("role");
            if (!TextUtils.isEmpty(roleExtra)) {
                role = roleExtra;
            }
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

        btnSavePassword.setOnClickListener(v -> savePassword());
    }

    private void savePassword() {
        String password = edtPassword.getText().toString().trim();
        String confirm = edtConfirmPassword.getText().toString().trim();

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

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        Button btnSavePassword = findViewById(R.id.btnSavePassword);
        btnSavePassword.setEnabled(false);
        btnSavePassword.setText("Saving...");

        user.updatePassword(password)
                .addOnSuccessListener(unused -> {
                    markPasswordCreated(user.getUid());
                    Toast.makeText(this, "Password created", Toast.LENGTH_SHORT).show();
                    com.example.fixitfinderapp.SessionManager.saveRole(this, role);
                    goToDashboard();
                })
                .addOnFailureListener(e -> {
                    btnSavePassword.setEnabled(true);
                    btnSavePassword.setText("Save Password");
                    Toast.makeText(this, "Failed to save password: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void markPasswordCreated(String uid) {
        String collection = "provider".equalsIgnoreCase(role) ? "providers" : "users";
        FirebaseFirestore.getInstance()
                .collection(collection)
                .document(uid)
                .update("passwordCreated", true);
    }

    private void goToDashboard() {
        Intent intent;
        if ("provider".equalsIgnoreCase(role)) {
            intent = new Intent(this, MainTabsActivity.class);
        } else {
            intent = new Intent(this, MainTabsActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
        view.setTextColor(ok ? Color.parseColor("#1B9C85") : Color.parseColor("#F44336"));
    }
}
