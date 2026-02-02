package com.example.fixitfinderapp.notifications;

import android.content.Context;
import android.text.TextUtils;

import com.example.fixitfinderapp.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

public class MessagingHelper {

    private MessagingHelper() { }

    public static void syncToken(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || context == null) {
            return;
        }
        String role = SessionManager.getRole(context);
        String collection = "provider".equalsIgnoreCase(role) ? "providers" : "users";

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (TextUtils.isEmpty(token)) {
                        return;
                    }
                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                    data.put("fcmToken", token);
                    FirebaseFirestore.getInstance()
                            .collection(collection)
                            .document(user.getUid())
                            .set(data, SetOptions.merge());
                });
    }
}
