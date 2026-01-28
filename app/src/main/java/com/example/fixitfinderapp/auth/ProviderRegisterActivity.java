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
import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitfinderapp.R;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProviderRegisterActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private boolean isPasswordVisible = false;

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
        Button btnRegister = findViewById(R.id.btnRegister);
        ImageView btnPasswordToggle = findViewById(R.id.btnPasswordToggle);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

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
            String phone = edtPhone.getText().toString().trim();
            String address = edtAddress.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

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

            if (!phone.startsWith("+")) {
                edtPhone.setError("Use international format, e.g. +63 9xx xxx xxxx");
                edtPhone.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(address)) {
                edtAddress.setError("Address is required");
                edtAddress.requestFocus();
                return;
            }

            btnRegister.setEnabled(false);
            btnRegister.setText("Submitting...");

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                saveProviderToFirestore(user.getUid(), fullName, email, phone, address);
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

    private void saveProviderToFirestore(String userId, String fullName, String email, String phone, String address) {
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
        userData.put("role", "provider");
        userData.put("phoneVerified", false);
        userData.put("createdAt", System.currentTimeMillis());

        db.collection("users")
                .document(userId)
                .set(userData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, OtpActivity.class);
                    intent.putExtra("phone", phone);
                    intent.putExtra("role", "provider");
                    intent.putExtra("email", email);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Account created but failed to save profile. Please try logging in.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(this, OtpActivity.class);
                    intent.putExtra("phone", phone);
                    intent.putExtra("role", "provider");
                    intent.putExtra("email", email);
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
}
