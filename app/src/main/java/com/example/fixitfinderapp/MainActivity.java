package com.example.fixitfinderapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitfinderapp.auth.LoginActivity;
import com.example.fixitfinderapp.auth.ProviderLoginActivity;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();

        Button btnLoginUser = findViewById(R.id.btnLoginUser);
        Button btnLoginService = findViewById(R.id.btnLoginService);
        android.widget.TextView tvTitle = findViewById(R.id.tvTitle);

        btnLoginUser.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));

        btnLoginService.setOnClickListener(v ->
                startActivity(new Intent(this, ProviderLoginActivity.class)));

        tvTitle.setOnLongClickListener(v -> {
            startActivity(new Intent(this, TestMenuActivity.class));
            return true;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(this, UserDashboardActivity.class));
            finish();
        }
    }
}