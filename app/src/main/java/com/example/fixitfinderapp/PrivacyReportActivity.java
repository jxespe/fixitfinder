package com.example.fixitfinderapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrivacyReportActivity extends BaseSwipeActivity {

    private Spinner spinnerCategory;
    private Spinner spinnerSubCategory;
    private EditText edtDescription;
    private Button btnSubmit;
    private android.view.View layoutSuccess;
    private android.widget.TextView tvCategoryLabel;
    private android.widget.TextView tvSubCategoryLabel;
    private android.widget.TextView tvDescriptionLabel;

    private final Map<String, List<String>> subCategoryMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_report);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerSubCategory = findViewById(R.id.spinnerSubCategory);
        edtDescription = findViewById(R.id.edtReportDescription);
        btnSubmit = findViewById(R.id.btnSubmitReport);
        layoutSuccess = findViewById(R.id.layoutReportSuccess);
        tvCategoryLabel = findViewById(R.id.tvCategoryLabel);
        tvSubCategoryLabel = findViewById(R.id.tvSubCategoryLabel);
        tvDescriptionLabel = findViewById(R.id.tvDescriptionLabel);

        setupCategories();

        btnSubmit.setOnClickListener(v -> submitReport());

        NavigationHelper.setupBottomNav(this, R.id.nav_settings);
    }

    @Override
    protected void onStart() {
        super.onStart();
        NavigationHelper.ensureLoggedIn(this);
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigation =
                findViewById(R.id.bottomNavigation);
        NavigationHelper.updateMessageBadge(this, bottomNavigation);
    }

    private void setupCategories() {
        List<String> categories = new ArrayList<>();
        categories.add("Account");
        categories.add("Booking");
        categories.add("Payment");
        categories.add("Provider");
        categories.add("App issue");

        subCategoryMap.put("Account", listOf("Login problem", "Profile update", "Verification"));
        subCategoryMap.put("Booking", listOf("Schedule issue", "Cancellation", "Status update"));
        subCategoryMap.put("Payment", listOf("Payment failed", "Refund request", "Incorrect charge"));
        subCategoryMap.put("Provider", listOf("Provider behavior", "Service quality", "No show"));
        subCategoryMap.put("App issue", listOf("Crash", "Slow performance", "UI bug"));

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(categoryAdapter);

        spinnerCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent,
                                       android.view.View view,
                                       int position,
                                       long id) {
                String selected = categories.get(position);
                List<String> subs = subCategoryMap.get(selected);
                if (subs == null || subs.isEmpty()) {
                    subs = listOf("Other");
                }
                ArrayAdapter<String> subAdapter = new ArrayAdapter<>(
                        PrivacyReportActivity.this,
                        android.R.layout.simple_spinner_dropdown_item,
                        subs);
                spinnerSubCategory.setAdapter(subAdapter);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                ArrayAdapter<String> subAdapter = new ArrayAdapter<>(
                        PrivacyReportActivity.this,
                        android.R.layout.simple_spinner_dropdown_item,
                        listOf("Other"));
                spinnerSubCategory.setAdapter(subAdapter);
            }
        });
    }

    private void submitReport() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }
        String category = spinnerCategory.getSelectedItem() != null
                ? spinnerCategory.getSelectedItem().toString()
                : "";
        String subCategory = spinnerSubCategory.getSelectedItem() != null
                ? spinnerSubCategory.getSelectedItem().toString()
                : "";
        String description = edtDescription.getText().toString().trim();

        if (TextUtils.isEmpty(description)) {
            edtDescription.setError("Please describe the issue");
            edtDescription.requestFocus();
            return;
        }

        Map<String, Object> report = new HashMap<>();
        report.put("userId", user.getUid());
        report.put("role", SessionManager.getRole(this));
        report.put("category", category);
        report.put("subCategory", subCategory);
        report.put("description", description);
        report.put("status", "open");
        report.put("createdAt", FieldValue.serverTimestamp());

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting...");

        FirebaseFirestore.getInstance()
                .collection("reports")
                .add(report)
                .addOnSuccessListener(doc -> {
                    showSuccess();
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Submit Report");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit report", Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Submit Report");
                });
    }

    private void showSuccess() {
        edtDescription.setText("");
        spinnerCategory.setSelection(0);
        spinnerSubCategory.setSelection(0);
        setFormVisibility(false);
        if (layoutSuccess != null) {
            layoutSuccess.setVisibility(android.view.View.VISIBLE);
        }
    }

    private void setFormVisibility(boolean visible) {
        int vis = visible ? android.view.View.VISIBLE : android.view.View.GONE;
        if (spinnerCategory != null) spinnerCategory.setVisibility(vis);
        if (spinnerSubCategory != null) spinnerSubCategory.setVisibility(vis);
        if (edtDescription != null) edtDescription.setVisibility(vis);
        if (btnSubmit != null) btnSubmit.setVisibility(vis);
        if (tvCategoryLabel != null) tvCategoryLabel.setVisibility(vis);
        if (tvSubCategoryLabel != null) tvSubCategoryLabel.setVisibility(vis);
        if (tvDescriptionLabel != null) tvDescriptionLabel.setVisibility(vis);
    }

    private List<String> listOf(String... values) {
        List<String> list = new ArrayList<>();
        if (values == null) {
            return list;
        }
        java.util.Collections.addAll(list, values);
        return list;
    }
}
