package com.example.fixitfinderapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.adapters.ConversationAdapter;
import com.example.fixitfinderapp.models.ConversationItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MessagesActivity extends AppCompatActivity {

    private static final String TAG = "MessagesActivity";
    private final List<ConversationItem> items = new ArrayList<>();
    private ConversationAdapter adapter;
    private String role = "user";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        ImageView btnBack = findViewById(R.id.btnBack);
        RecyclerView recyclerView = findViewById(R.id.recyclerViewMessages);

        adapter = new ConversationAdapter(items, this::openChat);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        String savedRole = SessionManager.getRole(this);
        if (!TextUtils.isEmpty(savedRole)) {
            role = savedRole;
        }

        setupBottomNavigation();
        loadConversations();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation == null) {
            return;
        }
        bottomNavigation.setSelectedItemId(R.id.nav_messages);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this,
                        "provider".equals(role) ? DashboardActivity.class : UserDashboardActivity.class));
                return true;
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this,
                        "provider".equals(role) ? ProviderHistoryActivity.class : HistoryActivity.class));
                return true;
            } else if (id == R.id.nav_messages) {
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this,
                        "provider".equals(role) ? ProviderSettingsActivity.class : UserSettingsActivity.class));
                return true;
            }
            return false;
        });
    }

    private void loadConversations() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String field = "provider".equals(role) ? "providerId" : "userId";
        FirebaseFirestore.getInstance()
                .collection("conversations")
                .whereEqualTo(field, user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    items.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String conversationId = doc.getId();
                        String preview = doc.getString("lastMessage");
                        String providerName = doc.getString("providerName");
                        String userName = doc.getString("userName");
                        String providerLogo = doc.getString("providerLogoUri");
                        String userLogo = doc.getString("userLogoUri");
                        Long unreadUser = doc.getLong("unreadUserCount");
                        Long unreadProvider = doc.getLong("unreadProviderCount");
                        Timestamp last = doc.getTimestamp("lastMessageAt");
                        long lastMillis = last != null ? last.toDate().getTime() : 0L;

                        String displayName = "provider".equals(role)
                                ? valueOrFallback(userName, "Customer")
                                : valueOrFallback(providerName, "Service Provider");
                        String avatar = "provider".equals(role) ? userLogo : providerLogo;

                        int unreadCount = "provider".equals(role)
                                ? safeLongToInt(unreadProvider)
                                : safeLongToInt(unreadUser);

                        items.add(new ConversationItem(conversationId,
                                displayName,
                                valueOrFallback(preview, "Tap to open chat"),
                                avatar,
                                lastMillis,
                                unreadCount));
                    }
                    Collections.sort(items, Comparator.comparingLong(o -> -o.lastMessageAt));
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load messages", e);
                    String message = "Failed to load messages.";
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
                        com.google.firebase.firestore.FirebaseFirestoreException fe =
                                (com.google.firebase.firestore.FirebaseFirestoreException) e;
                        if (fe.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            message = "Permission denied. Check Firestore rules for conversations.";
                        } else {
                            message = "Failed to load messages: " + fe.getMessage();
                        }
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                });
    }

    private void openChat(ConversationItem item) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("conversationId", item.conversationId);
        intent.putExtra("title", item.name);
        intent.putExtra("avatarUri", item.avatarUri);
        startActivity(intent);
    }

    private String valueOrFallback(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private int safeLongToInt(Long value) {
        if (value == null) {
            return 0;
        }
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return value.intValue();
    }
}
