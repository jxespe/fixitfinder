package com.example.fixitfinderapp;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.adapters.ChatMessageAdapter;
import com.example.fixitfinderapp.models.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private final List<ChatMessage> messages = new ArrayList<>();
    private ChatMessageAdapter adapter;
    private String conversationId;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        conversationId = getIntent().getStringExtra("conversationId");
        String title = getIntent().getStringExtra("title");
        String avatarUri = getIntent().getStringExtra("avatarUri");

        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageView ivAvatar = findViewById(R.id.ivChatAvatar);
        TextView tvTitle = findViewById(R.id.tvChatTitle);
        RecyclerView recyclerView = findViewById(R.id.recyclerChat);
        EditText edtMessage = findViewById(R.id.edtMessage);
        ImageButton btnSend = findViewById(R.id.btnSend);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvTitle.setText(!TextUtils.isEmpty(title) ? title : "Chat");
        if (!TextUtils.isEmpty(avatarUri)) {
            ivAvatar.setImageURI(Uri.parse(avatarUri));
        } else {
            ivAvatar.setImageResource(android.R.drawable.ic_menu_myplaces);
        }

        adapter = new ChatMessageAdapter(messages, currentUser.getUid());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> {
            String text = edtMessage.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                return;
            }
            sendMessage(text);
            edtMessage.setText("");
        });

        if (TextUtils.isEmpty(conversationId)) {
            Toast.makeText(this, "Missing conversation.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        markConversationRead();
        listenForMessages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        markConversationRead();
    }

    private void listenForMessages() {
        FirebaseFirestore.getInstance()
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) {
                        return;
                    }
                    messages.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String senderId = doc.getString("senderId");
                        String senderRole = doc.getString("senderRole");
                        String text = doc.getString("text");
                        Timestamp createdAt = doc.getTimestamp("createdAt");
                        long time = createdAt != null ? createdAt.toDate().getTime() : 0L;
                        messages.add(new ChatMessage(doc.getId(), senderId, senderRole, text, time));
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void sendMessage(String text) {
        String role = SessionManager.getRole(this);
        if (TextUtils.isEmpty(role)) {
            role = "user";
        }
        final String roleFinal = role;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        java.util.Map<String, Object> message = new java.util.HashMap<>();
        message.put("senderId", currentUser.getUid());
        message.put("senderRole", role);
        message.put("text", text);
        message.put("createdAt", FieldValue.serverTimestamp());

        db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(ref -> db.collection("conversations")
                        .document(conversationId)
                        .update(buildConversationUpdate(roleFinal, text)))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send message.", Toast.LENGTH_SHORT).show());
    }

    private java.util.Map<String, Object> buildConversationUpdate(String role, String text) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("lastMessage", text);
        updates.put("lastMessageAt", FieldValue.serverTimestamp());
        if ("provider".equalsIgnoreCase(role)) {
            updates.put("unreadUserCount", FieldValue.increment(1));
            updates.put("unreadProviderCount", 0);
        } else {
            updates.put("unreadProviderCount", FieldValue.increment(1));
            updates.put("unreadUserCount", 0);
        }
        return updates;
    }

    private void markConversationRead() {
        if (TextUtils.isEmpty(conversationId)) {
            return;
        }
        String role = SessionManager.getRole(this);
        String field = "provider".equalsIgnoreCase(role)
                ? "unreadProviderCount"
                : "unreadUserCount";
        FirebaseFirestore.getInstance()
                .collection("conversations")
                .document(conversationId)
                .update(field, 0);
    }
}
