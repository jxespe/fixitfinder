package com.example.fixitfinderapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitfinderapp.R;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class ProviderRegisterActivity extends AppCompatActivity {

    private static final String TAG = "ProviderRegisterActivity";
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private boolean isPasswordVisible = false;
    private Spinner spinnerServiceCategory;
    private Spinner spinnerCountryCode;
    private boolean isFormattingPhone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        EditText edtFullName = findViewById(R.id.edtFullName);
        EditText edtEmail = findViewById(R.id.edtEmail);
        EditText edtPhone = findViewById(R.id.edtPhone);
        EditText edtAddress = findViewById(R.id.edtAddress);
        EditText edtPassword = findViewById(R.id.edtPassword);
        spinnerServiceCategory = findViewById(R.id.spinnerServiceCategory);
        spinnerCountryCode = findViewById(R.id.spinnerCountryCode);
        Button btnRegister = findViewById(R.id.btnRegister);
        ImageView btnPasswordToggle = findViewById(R.id.btnPasswordToggle);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        setupPhoneFormatting(edtPhone);

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

        btnRegister.setOnClickListener(v -> {
            String fullName = edtFullName.getText().toString().trim();
            String email = edtEmail.getText().toString().trim();
            String phone = buildPhoneNumber(edtPhone);
            String address = edtAddress.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            String category = getSelectedCategory();

            if (TextUtils.isEmpty(fullName)) {
                edtFullName.setError("Full name is required");
                edtFullName.requestFocus();
                return;
            }

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

            if (!isStrongPassword(password)) {
                edtPassword.setError("Use 8+ characters with letters and numbers");
                edtPassword.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(phone)) {
                edtPhone.setError("Phone number is required");
                edtPhone.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(address)) {
                edtAddress.setError("Address is required");
                edtAddress.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(category)) {
                Toast.makeText(this, "Please select a service category", Toast.LENGTH_SHORT).show();
                return;
            }

            btnRegister.setEnabled(false);
            btnRegister.setText("Submitting...");

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                saveProviderToFirestore(user.getUid(), fullName, email, phone, address, category);
                            }
                        } else {
                            btnRegister.setEnabled(true);
                            btnRegister.setText("Submit");

                            String errorMessage = "Registration failed";
                            if (task.getException() != null) {
                                errorMessage = task.getException().getMessage();
                            }
                            Toast.makeText(ProviderRegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    private void saveProviderToFirestore(String userId, String fullName, String email, String phone, String address, String category) {
        String firstName = fullName;
        if (!TextUtils.isEmpty(fullName)) {
            String[] parts = fullName.trim().split("\\s+");
            if (parts.length > 0) {
                firstName = parts[0];
            }
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", userId);
        userData.put("fullName", fullName);
        userData.put("firstName", firstName);
        userData.put("email", email);
        userData.put("phone", phone);
        userData.put("address", address);
        userData.put("serviceCategory", category);
        userData.put("serviceCategoryLower", category.toLowerCase(Locale.US).trim());
        userData.put("role", "provider");
        userData.put("phoneVerified", false);
        userData.put("createdAt", System.currentTimeMillis());

        db.collection("providers")
                .document(userId)
                .set(userData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, OtpActivity.class);
                    intent.putExtra("phone", phone);
                    intent.putExtra("role", "provider");
                    intent.putExtra("email", email);
                    intent.putExtra("serviceCategory", category);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save provider profile", e);
                    Toast.makeText(this, "Account created but failed to save profile: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(this, OtpActivity.class);
                    intent.putExtra("phone", phone);
                    intent.putExtra("role", "provider");
                    intent.putExtra("email", email);
                    intent.putExtra("serviceCategory", category);
                    startActivity(intent);
                    finish();
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

    private String getSelectedCategory() {
        if (spinnerServiceCategory == null || spinnerServiceCategory.getSelectedItem() == null) {
            return "";
        }
        String selected = spinnerServiceCategory.getSelectedItem().toString().trim();
        if ("Select service category".equalsIgnoreCase(selected)) {
            return "";
        }
        return selected;
    }

    private String buildPhoneNumber(EditText edtPhone) {
        String local = edtPhone != null ? edtPhone.getText().toString().trim() : "";
        if (TextUtils.isEmpty(local)) {
            if (edtPhone != null) {
                edtPhone.setError("Phone number is required");
                edtPhone.requestFocus();
            }
            return "";
        }
        String stripped = local.replaceAll("\\D", "");
        if (stripped.startsWith("0")) {
            stripped = stripped.substring(1);
        }
        if (stripped.startsWith("+")) {
            stripped = stripped.substring(1);
        }
        String code = "+63";
        if (spinnerCountryCode != null && spinnerCountryCode.getSelectedItem() != null) {
            String selected = spinnerCountryCode.getSelectedItem().toString().trim();
            if (!TextUtils.isEmpty(selected)) {
                code = selected;
            }
        }
        return code + stripped;
    }

    private void setupPhoneFormatting(EditText edtPhone) {
        if (edtPhone == null) {
            return;
        }
        edtPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormattingPhone) {
                    return;
                }
                isFormattingPhone = true;
                String digits = s.toString().replaceAll("\\D", "");
                if (digits.startsWith("0")) {
                    digits = digits.substring(1);
                }
                if (digits.length() > 10) {
                    digits = digits.substring(0, 10);
                }
                String formatted = formatLocalPhone(digits);
                if (!formatted.equals(s.toString())) {
                    edtPhone.setText(formatted);
                    edtPhone.setSelection(formatted.length());
                }
                isFormattingPhone = false;
            }
        });
    }

    private String formatLocalPhone(String digits) {
        if (TextUtils.isEmpty(digits)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int len = digits.length();
        sb.append(digits, 0, Math.min(3, len));
        if (len > 3) {
            sb.append(" ");
            sb.append(digits, 3, Math.min(6, len));
        }
        if (len > 6) {
            sb.append(" ");
            sb.append(digits, 6, len);
        }
        return sb.toString();
    }
}
