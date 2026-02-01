package com.example.fixitfinderapp;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.adapters.ServiceProviderAdapter;
import com.example.fixitfinderapp.models.ServiceProviderProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ServiceCategoryActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_NAME = "category_name";
    private static final String TAG = "ServiceCategoryActivity";
    private final List<ServiceProviderProfile> providers = new ArrayList<>();
    private ServiceProviderAdapter adapter;
    private TextView tvEmptyProviders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_category);

        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView tvCategoryTitle = findViewById(R.id.tvCategoryTitle);
        TextView tvCategorySubtitle = findViewById(R.id.tvCategorySubtitle);
        tvEmptyProviders = findViewById(R.id.tvEmptyProviders);
        RecyclerView recyclerProviders = findViewById(R.id.recyclerProviders);

        String name = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);
        if (name == null || name.trim().isEmpty()) {
            name = "Service Category";
        }

        tvCategoryTitle.setText(name);
        tvCategorySubtitle.setText("Explore " + name + " services");
        setupProviderList(recyclerProviders);
        loadProviders(name);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupProviderList(RecyclerView recyclerProviders) {
        adapter = new ServiceProviderAdapter(providers);
        recyclerProviders.setLayoutManager(new LinearLayoutManager(this));
        recyclerProviders.setAdapter(adapter);
    }

    private void loadProviders(String categoryName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            tvEmptyProviders.setVisibility(android.view.View.VISIBLE);
            tvEmptyProviders.setText("Please log in to view providers.");
            return;
        }
        Log.d(TAG, "Loading providers for uid=" + user.getUid());
        String normalized = categoryName == null ? "" : categoryName.toLowerCase(java.util.Locale.US).trim();
        loadFromCollections(normalized, categoryName);
    }

    private void loadFromCollections(String normalized, String originalCategory) {
        java.util.Map<String, com.google.firebase.firestore.DocumentSnapshot> merged =
                new java.util.LinkedHashMap<>();
        FirebaseFirestore.getInstance()
                .collection("providers")
                .whereEqualTo("serviceCategoryLower", normalized)
                .get()
                .addOnSuccessListener(providerSnap -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : providerSnap.getDocuments()) {
                        merged.put(doc.getId(), doc);
                    }
                    if (merged.isEmpty()) {
                        fallbackCaseSensitive(originalCategory, merged);
                        return;
                    }
                    applyProviders(new java.util.ArrayList<>(merged.values()));
                })
                .addOnFailureListener(this::showProviderLoadError);
    }

    private void fallbackCaseSensitive(String originalCategory,
                                       java.util.Map<String, com.google.firebase.firestore.DocumentSnapshot> merged) {
        FirebaseFirestore.getInstance()
                .collection("providers")
                .whereEqualTo("serviceCategory", originalCategory)
                .get()
                .addOnSuccessListener(providerSnap -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : providerSnap.getDocuments()) {
                        merged.put(doc.getId(), doc);
                    }
                    if (merged.isEmpty()) {
                        applyProviders(new java.util.ArrayList<>(merged.values()));
                        return;
                    }
                    applyProviders(new java.util.ArrayList<>(merged.values()));
                })
                .addOnFailureListener(this::showProviderLoadError);
    }

    private void applyProviders(java.util.List<com.google.firebase.firestore.DocumentSnapshot> docs) {
        providers.clear();
        for (com.google.firebase.firestore.DocumentSnapshot doc : docs) {
            String providerId = doc.getId();
            String fullName = doc.getString("fullName");
            String address = doc.getString("address");
            String category = doc.getString("serviceCategory");
            String logoUri = doc.getString("logoUri");
            providers.add(new ServiceProviderProfile(providerId, fullName, category, address, logoUri));
        }
        adapter.notifyDataSetChanged();
        boolean isEmpty = providers.isEmpty();
        if (isEmpty) {
            tvEmptyProviders.setText("No providers found for this category.");
            tvEmptyProviders.setVisibility(android.view.View.VISIBLE);
        } else {
            tvEmptyProviders.setVisibility(android.view.View.GONE);
        }
    }

    private void showProviderLoadError(Exception e) {
        Log.e(TAG, "Failed to load providers", e);
        tvEmptyProviders.setVisibility(android.view.View.VISIBLE);
        String message = "Unable to load providers: " + e.getMessage();
        if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
            com.google.firebase.firestore.FirebaseFirestoreException fe =
                    (com.google.firebase.firestore.FirebaseFirestoreException) e;
            if (fe.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                String uid = user != null ? user.getUid() : "null";
                message = "Permission denied. Auth uid=" + uid
                        + ". Check Firestore rules and App Check.";
            }
        }
        tvEmptyProviders.setText(message);
    }
}
