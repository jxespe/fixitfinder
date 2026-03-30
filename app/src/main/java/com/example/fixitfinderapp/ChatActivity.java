package com.example.fixitfinderapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.Timestamp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends BaseSwipeActivity {

    private final List<ChatMessage> messages = new ArrayList<>();
    private ChatMessageAdapter adapter;
    private String conversationId;
    private FirebaseUser currentUser;
    private ImageView ivAvatar;
    private ListenerRegistration headerListener;
    private String headerAvatarUri;
    private ActivityResultLauncher<String> pickImageLauncher;
    private EditText edtMessage;
    private ImageButton btnAttach;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        conversationId = getIntent().getStringExtra("conversationId");
        String title = getIntent().getStringExtra("title");
        String avatarUri = getIntent().getStringExtra("avatarUri");

        ImageButton btnBack = findViewById(R.id.btnBack);
        ivAvatar = findViewById(R.id.ivChatAvatar);
        TextView tvTitle = findViewById(R.id.tvChatTitle);
        RecyclerView recyclerView = findViewById(R.id.recyclerChat);
        edtMessage = findViewById(R.id.edtMessage);
        ImageButton btnSend = findViewById(R.id.btnSend);
        btnAttach = findViewById(R.id.btnAttach);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvTitle.setText(!TextUtils.isEmpty(title) ? title : "Chat");
        ImageLoader.loadProfile(ivAvatar, avatarUri, android.R.drawable.ic_menu_myplaces);

        String role = SessionManager.getRole(this);
        if (TextUtils.isEmpty(role)) {
            role = "user";
        }
        adapter = new ChatMessageAdapter(messages, currentUser.getUid(), role);
        if (!TextUtils.isEmpty(avatarUri)) {
            adapter.setPeerAvatarUrl(avatarUri);
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        loadSelfAvatarForMessages();

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> {
            String text = edtMessage.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                return;
            }
            sendMessage(text, null);
            edtMessage.setText("");
        });

        pickImageLauncher =
                registerForActivityResult(new ActivityResultContracts.GetContent(),
                        uri -> {
                            if (uri == null) {
                                return;
                            }
                            String caption = edtMessage.getText().toString().trim();
                            edtMessage.setText("");
                            uploadImageMessage(uri, caption);
                        });
        if (btnAttach != null) {
            btnAttach.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        }

        if (TextUtils.isEmpty(conversationId)) {
            Toast.makeText(this, "Missing conversation.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        listenForHeaderAvatar();
        markConversationRead();
        listenForMessages();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (headerListener != null) {
            headerListener.remove();
            headerListener = null;
        }
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
                        String imageUrl = doc.getString("imageUrl");
                        String type = doc.getString("type");
                        String bookingId = doc.getString("bookingId");
                        Timestamp createdAt = doc.getTimestamp("createdAt");
                        long time = createdAt != null ? createdAt.toDate().getTime() : 0L;
                        messages.add(new ChatMessage(doc.getId(), senderId, senderRole, text, imageUrl,
                                type, bookingId, time));
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void sendMessage(String text, String imageUrl) {
        if (TextUtils.isEmpty(text) && TextUtils.isEmpty(imageUrl)) {
            return;
        }
        String role = SessionManager.getRole(this);
        if (TextUtils.isEmpty(role)) {
            role = "user";
        }
        final String roleFinal = role;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        java.util.Map<String, Object> message = new java.util.HashMap<>();
        message.put("senderId", currentUser.getUid());
        message.put("senderRole", role);
        if (!TextUtils.isEmpty(text)) {
            message.put("text", text);
        }
        if (!TextUtils.isEmpty(imageUrl)) {
            message.put("imageUrl", imageUrl);
            message.put("type", "image");
        } else {
            message.put("type", "text");
        }
        message.put("createdAt", FieldValue.serverTimestamp());

        db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(ref -> db.collection("conversations")
                        .document(conversationId)
                        .update(buildConversationUpdate(roleFinal, text, imageUrl)))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send message.", Toast.LENGTH_SHORT).show());
    }

    private java.util.Map<String, Object> buildConversationUpdate(String role,
                                                                  String text,
                                                                  String imageUrl) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        String preview = !TextUtils.isEmpty(text)
                ? text
                : (!TextUtils.isEmpty(imageUrl) ? "Photo" : "");
        updates.put("lastMessage", preview);
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

    private void uploadImageMessage(Uri uri, String caption) {
        if (uri == null || TextUtils.isEmpty(conversationId)) {
            return;
        }
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference()
                .child("chat_images")
                .child(conversationId)
                .child(java.util.UUID.randomUUID().toString() + ".jpg");
        ref.putFile(uri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    String imageUrl = downloadUri != null ? downloadUri.toString() : null;
                    sendMessage(caption, imageUrl);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Image upload failed.", Toast.LENGTH_SHORT).show());
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

    private void listenForHeaderAvatar() {
        if (TextUtils.isEmpty(conversationId) || ivAvatar == null) {
            return;
        }
        String role = SessionManager.getRole(this);
        headerListener = FirebaseFirestore.getInstance()
                .collection("conversations")
                .document(conversationId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) {
                        return;
                    }
                    String userId = snapshot.getString("userId");
                    String providerId = snapshot.getString("providerId");
                    String avatar = "provider".equalsIgnoreCase(role)
                            ? snapshot.getString("userLogoUri")
                            : snapshot.getString("providerLogoUri");
                    if (TextUtils.isEmpty(avatar)) {
                        if ("provider".equalsIgnoreCase(role)) {
                            loadAvatarFromProfile("users", userId);
                        } else {
                            loadAvatarFromProfile("providers", providerId);
                        }
                        return;
                    }
                    if (!avatar.equals(headerAvatarUri)) {
                        headerAvatarUri = avatar;
                        ImageLoader.loadProfile(ivAvatar, avatar, android.R.drawable.ic_menu_myplaces);
                        if (adapter != null) {
                            adapter.setPeerAvatarUrl(avatar);
                        }
                    }
                });
    }

    private void loadAvatarFromProfile(String collection, String docId) {
        if (TextUtils.isEmpty(collection) || TextUtils.isEmpty(docId)) {
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
                        return;
                    }
                    if (!avatar.equals(headerAvatarUri)) {
                        headerAvatarUri = avatar;
                        ImageLoader.loadProfile(ivAvatar, avatar, android.R.drawable.ic_menu_myplaces);
                        if (adapter != null) {
                            adapter.setPeerAvatarUrl(avatar);
                        }
                    }
                });
    }

    private void loadSelfAvatarForMessages() {
        if (currentUser == null || adapter == null) {
            return;
        }
        String role = SessionManager.getRole(this);
        if (TextUtils.isEmpty(role)) {
            role = "user";
        }
        String collection = "provider".equalsIgnoreCase(role) ? "providers" : "users";
        FirebaseFirestore.getInstance()
                .collection(collection)
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        return;
                    }
                    String uri = null;
                    if ("users".equals(collection)) {
                        uri = doc.getString("photoUrl");
                        if (TextUtils.isEmpty(uri)) {
                            uri = doc.getString("profilePhotoUri");
                        }
                    } else {
                        uri = doc.getString("logoUri");
                    }
                    if (TextUtils.isEmpty(uri) && currentUser.getPhotoUrl() != null) {
                        uri = currentUser.getPhotoUrl().toString();
                    }
                    if (adapter != null) {
                        adapter.setSelfAvatarUrl(uri);
                    }
                });
    }
}
