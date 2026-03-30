package com.example.fixitfinderapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.fixitfinderapp.maps.AddressGeocoder;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends BaseSwipeActivity {

    private EditText edtFullName;
    private EditText edtEmail;
    private EditText edtPhone;
    private EditText edtAddress;
    private Button btnSave;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        user = FirebaseAuth.getInstance().getCurrentUser();

        edtFullName = findViewById(R.id.edtFullName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        edtAddress = findViewById(R.id.edtAddress);
        btnSave = findViewById(R.id.btnSave);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        loadProfile();

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void loadProfile() {
        if (user == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (edtEmail != null) {
            edtEmail.setText(user.getEmail());
        }
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String fullName = doc.getString("fullName");
                    String phone = doc.getString("phone");
                    String address = doc.getString("address");
                    if (edtFullName != null) {
                        edtFullName.setText(fullName);
                    }
                    if (edtPhone != null) {
                        edtPhone.setText(phone);
                    }
                    if (edtAddress != null) {
                        edtAddress.setText(address);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    private void saveProfile() {
        if (user == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullName = edtFullName != null ? edtFullName.getText().toString().trim() : "";
        String phone = edtPhone != null ? edtPhone.getText().toString().trim() : "";
        String address = edtAddress != null ? edtAddress.getText().toString().trim() : "";

        if (TextUtils.isEmpty(fullName)) {
            if (edtFullName != null) {
                edtFullName.setError("Name is required");
                edtFullName.requestFocus();
            }
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("firstName", extractFirstName(fullName));
        updates.put("phone", phone);
        updates.put("address", address);

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build();
                    user.updateProfile(request);
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                    AddressGeocoder.updateLatLng(this, "users", user.getUid(), address);
                    btnSave.setEnabled(true);
                    btnSave.setText("Save Changes");
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("Save Changes");
                });
    }

    private String extractFirstName(String fullName) {
        if (TextUtils.isEmpty(fullName)) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : fullName;
    }
}
