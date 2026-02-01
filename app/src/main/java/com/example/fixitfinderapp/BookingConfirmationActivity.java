package com.example.fixitfinderapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class BookingConfirmationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_confirmation);

        ImageButton btnBack = findViewById(R.id.btnBack);
        Button btnBackHome = findViewById(R.id.btnBackHome);

        btnBack.setOnClickListener(v -> goHome());
        btnBackHome.setOnClickListener(v -> goHome());
    }

    private void goHome() {
        Intent intent = new Intent(this, UserDashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
