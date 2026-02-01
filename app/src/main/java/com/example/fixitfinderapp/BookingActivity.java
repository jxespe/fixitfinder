package com.example.fixitfinderapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class BookingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        TextView tvProviderName = findViewById(R.id.tvProviderName);
        TextView tvProviderSubtitle = findViewById(R.id.tvProviderSubtitle);
        TextView tvProviderLocation = findViewById(R.id.tvProviderLocation);

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

        findViewById(R.id.btnBookAppointment).setOnClickListener(v -> {
            android.content.Intent intent =
                    new android.content.Intent(this, BookingScheduleActivity.class);
            intent.putExtra("providerId", providerId);
            intent.putExtra("providerName", providerName);
            intent.putExtra("serviceCategory", category);
            intent.putExtra("address", address);
            intent.putExtra("logoUri", logoUri);
            startActivity(intent);
        });
        findViewById(R.id.btnBackToServices).setOnClickListener(v -> finish());
    }
}
