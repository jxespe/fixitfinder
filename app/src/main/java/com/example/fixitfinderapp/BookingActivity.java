package com.example.fixitfinderapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitfinderapp.adapters.ProviderServiceAdapter;
import com.example.fixitfinderapp.models.ProviderServiceItem;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class BookingActivity extends AppCompatActivity {

    private final List<ProviderServiceItem> services = new ArrayList<>();
    private ProviderServiceAdapter serviceAdapter;
    private TextView tvServicesEmpty;
    private String selectedServiceId;
    private String selectedServiceName;
    private double selectedServicePrice;
    private String selectedServiceImageUri;
    private String selectedServiceDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        TextView tvProviderName = findViewById(R.id.tvProviderName);
        TextView tvProviderSubtitle = findViewById(R.id.tvProviderSubtitle);
        TextView tvProviderLocation = findViewById(R.id.tvProviderLocation);
        RecyclerView recyclerServices = findViewById(R.id.recyclerProviderServices);
        tvServicesEmpty = findViewById(R.id.tvServicesEmpty);

        String providerId = getIntent().getStringExtra("providerId");
        String providerName = getIntent().getStringExtra("providerName");
        String category = getIntent().getStringExtra("serviceCategory");
        String address = getIntent().getStringExtra("address");
        String logoUri = getIntent().getStringExtra("logoUri");

        if (!TextUtils.isEmpty(providerName)) {
            tvHeaderTitle.setText(providerName);
            tvProviderName.setText(providerName);
        }
        if (!TextUtils.isEmpty(category)) {
            tvProviderSubtitle.setText("Licensed " + category);
        }
        if (!TextUtils.isEmpty(address)) {
            tvProviderLocation.setText(address);
        }

        btnBack.setOnClickListener(v -> finish());

        setupServicesRecycler(recyclerServices);
        if (!TextUtils.isEmpty(providerId)) {
            loadProviderServices(providerId);
        } else {
            showEmptyServices();
        }

        findViewById(R.id.btnBookAppointment).setOnClickListener(v -> {
            if (TextUtils.isEmpty(selectedServiceId)) {
                Toast.makeText(this, "Please select a service.", Toast.LENGTH_SHORT).show();
                return;
            }
            android.content.Intent intent =
                    new android.content.Intent(this, BookingScheduleActivity.class);
            intent.putExtra("providerId", providerId);
            intent.putExtra("providerName", providerName);
            intent.putExtra("serviceCategory", category);
            intent.putExtra("address", address);
            intent.putExtra("logoUri", logoUri);
            intent.putExtra("serviceId", selectedServiceId);
            intent.putExtra("serviceName", selectedServiceName);
            intent.putExtra("servicePrice", selectedServicePrice);
            intent.putExtra("serviceImageUri", selectedServiceImageUri);
            intent.putExtra("serviceDescription", selectedServiceDescription);
            startActivity(intent);
        });
        findViewById(R.id.btnBackToServices).setOnClickListener(v -> finish());
    }

    private void setupServicesRecycler(RecyclerView recyclerServices) {
        if (recyclerServices == null) {
            return;
        }
        recyclerServices.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        serviceAdapter = new ProviderServiceAdapter(services, false, true, item -> {
            selectedServiceId = item.id;
            selectedServiceName = item.name;
            selectedServicePrice = item.price;
            selectedServiceImageUri = item.imageUri;
            selectedServiceDescription = item.description;
            serviceAdapter.setSelectedId(item.id);
        });
        recyclerServices.setAdapter(serviceAdapter);
    }

    private void loadProviderServices(String providerId) {
        FirebaseFirestore.getInstance()
                .collection("providers")
                .document(providerId)
                .collection("services")
                .get()
                .addOnSuccessListener(snapshot -> {
                    services.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        String imageUri = doc.getString("imageUri");
                        String description = doc.getString("description");
                        double price = 0d;
                        Object priceObj = doc.get("price");
                        if (priceObj instanceof Number) {
                            price = ((Number) priceObj).doubleValue();
                        }
                        services.add(new ProviderServiceItem(id, name, price, imageUri, description));
                    }
                    if (serviceAdapter != null) {
                        serviceAdapter.notifyDataSetChanged();
                    }
                    syncSelectedService();
                    toggleEmptyServices();
                })
                .addOnFailureListener(e -> {
                    String message = "Failed to load services: " + e.getMessage();
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    showEmptyServices();
                });
    }

    private void toggleEmptyServices() {
        if (tvServicesEmpty == null) {
            return;
        }
        tvServicesEmpty.setVisibility(services.isEmpty()
                ? android.view.View.VISIBLE
                : android.view.View.GONE);
    }

    private void showEmptyServices() {
        services.clear();
        if (serviceAdapter != null) {
            serviceAdapter.notifyDataSetChanged();
        }
        clearSelectedService();
        if (tvServicesEmpty != null) {
            tvServicesEmpty.setVisibility(android.view.View.VISIBLE);
        }
    }

    private void syncSelectedService() {
        if (TextUtils.isEmpty(selectedServiceId)) {
            return;
        }
        for (ProviderServiceItem item : services) {
            if (selectedServiceId.equals(item.id)) {
                return;
            }
        }
        clearSelectedService();
    }

    private void clearSelectedService() {
        selectedServiceId = null;
        selectedServiceName = null;
        selectedServicePrice = 0d;
        selectedServiceImageUri = null;
        selectedServiceDescription = null;
        if (serviceAdapter != null) {
            serviceAdapter.setSelectedId(null);
        }
    }
}
