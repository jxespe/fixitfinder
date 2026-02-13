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

        NavigationHelper.setupBottomNav(this, R.id.nav_messages);
        loadConversations();
    }

    @Override
    protected void onStart() {
        super.onStart();
        NavigationHelper.ensureLoggedIn(this);
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
                        String providerId = doc.getString("providerId");
                        String userId = doc.getString("userId");
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

                        // Refresh avatar from the other user's profile to keep it current.
                        if ("provider".equals(role)) {
                            refreshAvatar(conversationId, "users", userId, avatar);
                        } else {
                            refreshAvatar(conversationId, "providers", providerId, avatar);
                        }
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

    private void refreshAvatar(String conversationId, String collection, String docId, String fallback) {
        if (TextUtils.isEmpty(conversationId) || TextUtils.isEmpty(collection) || TextUtils.isEmpty(docId)) {
            return;
        }
        FirebaseFirestore.getInstance()
                .collection(collection)
                .document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    String avatar = null;
                    if ("users".equals(collection)) {
                        avatar = doc.getString("photoUrl");
                        if (TextUtils.isEmpty(avatar)) {
                            avatar = doc.getString("profilePhotoUri");
                        }
                    } else if ("providers".equals(collection)) {
                        avatar = doc.getString("logoUri");
                    }
                    if (TextUtils.isEmpty(avatar)) {
                        avatar = fallback;
                    }
                    if (TextUtils.isEmpty(avatar)) {
                        return;
                    }
                    updateItemAvatar(conversationId, avatar);
                });
    }

    private void updateItemAvatar(String conversationId, String avatarUri) {
        for (int i = 0; i < items.size(); i++) {
            ConversationItem current = items.get(i);
            if (conversationId.equals(current.conversationId)) {
                if (avatarUri.equals(current.avatarUri)) {
                    return;
                }
                ConversationItem updated = new ConversationItem(
                        current.conversationId,
                        current.name,
                        current.preview,
                        avatarUri,
                        current.lastMessageAt,
                        current.unreadCount);
                items.set(i, updated);
                adapter.notifyItemChanged(i);
                return;
            }
        }
    }
}
