package com.example.fixitfinderapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitfinderapp.maps.MapsActivity;
import com.example.fixitfinderapp.payment.PaymentActivity;
import com.google.firebase.auth.FirebaseAuth;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        Button btnServices = findViewById(R.id.btnServices);
        Button btnMap = findViewById(R.id.btnMap);
        Button btnPayment = findViewById(R.id.btnPayment);
        Button btnLogout = findViewById(R.id.btnLogout);

        btnServices.setOnClickListener(v ->
                startActivity(new Intent(this, ServiceListActivity.class)));

        btnMap.setOnClickListener(v ->
                startActivity(new Intent(this, MapsActivity.class)));

        btnPayment.setOnClickListener(v ->
                startActivity(new Intent(this, PaymentActivity.class)));

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}
