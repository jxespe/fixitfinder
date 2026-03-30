package com.example.fixitfinderapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.adapters.NotificationAdapter;
import com.example.fixitfinderapp.models.AppNotificationItem;
import com.example.fixitfinderapp.notifications.NotificationFirestoreSync;
import com.example.fixitfinderapp.notifications.NotificationStore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NotificationsActivity extends BaseSwipeActivity {

    private static final String TAG = "NotificationsActivity";
    private static final int NOTIFICATION_PAGE_LIMIT = 200;
    private static final String LEGACY_BOOKING_OK_PREFIX = "Your booking was successful for ";
    private static final String META_PREFS = "notification_sync_meta";

    private final List<AppNotificationItem> items = new ArrayList<>();
    private final List<DocumentSnapshot> lastNotificationDocs = new ArrayList<>();
    private NotificationAdapter adapter;
    private TextView tvEmpty;
    private RecyclerView recycler;
    private ListenerRegistration notificationRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        recycler = findViewById(R.id.recyclerNotifications);
        tvEmpty = findViewById(R.id.tvEmptyNotifications);

        adapter = new NotificationAdapter(items, this::openNotificationDestination);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        attachNotificationsIfPossible();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!lastNotificationDocs.isEmpty()) {
            markNotificationsSeen(lastNotificationDocs);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detachFirestoreListener();
    }

    private void detachFirestoreListener() {
        if (notificationRegistration != null) {
            notificationRegistration.remove();
            notificationRegistration = null;
        }
    }

    /**
     * Load every in-app notification row for this account from the cloud (Facebook-style inbox),
     * regardless of {@code role} on each document, so a new device always matches the account.
     */
    private void attachNotificationsIfPossible() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showLocalFallback(null);
            return;
        }
        attachFirestoreListener(user.getUid());
    }

    private void attachFirestoreListener(String uid) {
        detachFirestoreListener();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !uid.equals(user.getUid())) {
            showLocalFallback(null);
            return;
        }

        notificationRegistration = FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("userId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(NOTIFICATION_PAGE_LIMIT)
                .addSnapshotListener((QuerySnapshot snapshot, FirebaseFirestoreException e) -> {
                    if (e != null) {
                        Log.w(TAG, "notifications query listener", e);
                        // Do not clear a list that already rendered; showLocalFallback detaches and wipes UI.
                        if (items.isEmpty() && lastNotificationDocs.isEmpty()) {
                            showLocalFallback(uid);
                        }
                        return;
                    }
                    if (snapshot == null) {
                        return;
                    }

                    // Pending sync: empty cache snapshot can follow real data and flash the list away.
                    if (snapshot.isEmpty()
                            && snapshot.getMetadata().isFromCache()
                            && !items.isEmpty()) {
                        return;
                    }

                    maybeMigrateLocalHistory(uid, snapshot);

                    items.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        long ts = 0L;
                        com.google.firebase.Timestamp createdAt = doc.getTimestamp("createdAt");
                        if (createdAt != null) {
                            ts = createdAt.toDate().getTime();
                        }
                        items.add(toDisplayNotification(
                                doc.getString("title"),
                                doc.getString("message"),
                                ts,
                                doc.getString("source"),
                                doc.getString("bookingId"),
                                doc.getString("conversationId")));
                    }

                    lastNotificationDocs.clear();
                    lastNotificationDocs.addAll(snapshot.getDocuments());

                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);

                    if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                        markNotificationsSeen(lastNotificationDocs);
                    }
                });
    }

    /**
     * One-time upload of device-only history so a new phone can still see past rows after the user
     * opens this screen once on the old device (or if cloud was empty).
     */
    private void maybeMigrateLocalHistory(String uid, QuerySnapshot snapshot) {
        if (!snapshot.isEmpty()) {
            return;
        }
        // Avoid migrating on a stale empty cache snapshot while the server may still have rows.
        if (snapshot.getMetadata().isFromCache()) {
            return;
        }
        SharedPreferences prefs = getSharedPreferences(META_PREFS, MODE_PRIVATE);
        String key = "legacy_migrated_v2_all_roles_" + uid;
        if (prefs.getBoolean(key, false)) {
            return;
        }
        boolean any = false;
        for (String role : new String[]{"user", "provider"}) {
            List<AppNotificationItem> local = NotificationStore.load(this, role, uid);
            for (AppNotificationItem item : local) {
                NotificationFirestoreSync.persistLegacy(this, item.title, item.message, role,
                        item.timestamp, true);
                any = true;
            }
        }
        if (!any) {
            prefs.edit().putBoolean(key, true).apply();
            return;
        }
        prefs.edit().putBoolean(key, true).apply();
    }

    private void markNotificationsSeen(List<DocumentSnapshot> docs) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || docs == null || docs.isEmpty()) {
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        com.google.firebase.firestore.WriteBatch batch = db.batch();
        int updates = 0;
        for (DocumentSnapshot doc : docs) {
            Boolean seen = doc.getBoolean("seen");
            if (Boolean.TRUE.equals(seen)) {
                continue;
            }
            batch.update(doc.getReference(), "seen", true);
            batch.update(doc.getReference(), "seenAt",
                    com.google.firebase.firestore.FieldValue.serverTimestamp());
            updates++;
        }
        if (updates > 0) {
            batch.commit();
        }
    }

    private static AppNotificationItem toDisplayNotification(String title,
                                                             String message,
                                                             long ts,
                                                             String source,
                                                             String bookingId,
                                                             String conversationId) {
        if (TextUtils.isEmpty(title)) {
            title = "Notification";
        }
        if (message == null) {
            message = "";
        }
        if ("Booking successful".equals(title)) {
            title = "Booking update";
        }
        message = normalizeLegacyBookingCreatedBody(message);
        return new AppNotificationItem(title, message, ts, source, bookingId, conversationId);
    }

    /**
     * Older Cloud Function copy used a different title/body shape; map to the same wording as
     * {@code buildStatusMessage}-style updates when the English legacy pattern matches.
     */
    private static String normalizeLegacyBookingCreatedBody(String message) {
        if (TextUtils.isEmpty(message)) {
            return message;
        }
        if ("Your booking was successful.".equals(message.trim())) {
            return "Your booking is pending.";
        }
        if (message.startsWith(LEGACY_BOOKING_OK_PREFIX) && message.endsWith(".")) {
            String schedule = message.substring(
                    LEGACY_BOOKING_OK_PREFIX.length(), message.length() - 1);
            return "Your booking is pending. Scheduled for " + schedule + ".";
        }
        return message;
    }

    /**
     * @param uid signed-in account; when non-null, merges user + provider local buckets for that id.
     */
    private void showLocalFallback(String uid) {
        detachFirestoreListener();
        items.clear();
        lastNotificationDocs.clear();
        List<AppNotificationItem> merged = new ArrayList<>();
        if (!TextUtils.isEmpty(uid)) {
            merged.addAll(NotificationStore.load(this, "user", uid));
            merged.addAll(NotificationStore.load(this, "provider", uid));
        } else {
            merged.addAll(NotificationStore.load(this, SessionManager.getRole(this)));
        }
        Collections.sort(merged, (a, b) -> Long.compare(b.timestamp, a.timestamp));
        Set<String> dedupe = new LinkedHashSet<>();
        for (AppNotificationItem item : merged) {
            String key = item.timestamp + "\n" + safeKeyPart(item.title) + "\n" + safeKeyPart(item.message);
            if (!dedupe.add(key)) {
                continue;
            }
            items.add(toDisplayNotification(
                    item.title,
                    item.message,
                    item.timestamp,
                    item.source,
                    item.bookingId,
                    item.conversationId));
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openNotificationDestination(AppNotificationItem item) {
        if (item == null) {
            return;
        }
        if (!TextUtils.isEmpty(item.conversationId)) {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("conversationId", item.conversationId);
            intent.putExtra("title", "Chat");
            startActivity(intent);
            return;
        }
        if (!TextUtils.isEmpty(item.bookingId)) {
            Intent intent = new Intent(this, BookingTimelineActivity.class);
            intent.putExtra("bookingId", item.bookingId);
            startActivity(intent);
            return;
        }
        String source = item.source != null ? item.source.toLowerCase(Locale.US) : "";
        String title = item.title != null ? item.title.toLowerCase(Locale.US) : "";
        if (source.contains("chat") || title.contains("message")) {
            startActivity(new Intent(this, MessagesActivity.class));
            return;
        }
        if (source.contains("booking")) {
            startActivity(new Intent(this, HistoryActivity.class));
            return;
        }
        if (source.contains("reminder")) {
            startActivity(new Intent(this, BookingActivity.class));
        }
    }

    private static String safeKeyPart(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.US);
    }
}
