package com.example.fixitfinderapp;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.adapters.NotificationAdapter;
import com.example.fixitfinderapp.models.AppNotificationItem;
import com.example.fixitfinderapp.notifications.NotificationStore;

import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        RecyclerView recycler = findViewById(R.id.recyclerNotifications);
        TextView tvEmpty = findViewById(R.id.tvEmptyNotifications);
        List<AppNotificationItem> items = NotificationStore.load(this);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(new NotificationAdapter(items));

        tvEmpty.setVisibility(items.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }
}
