package com.example.fixitfinderapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;

import java.util.Locale;
import java.util.Map;

/**
 * Persists a new booking document and seeds the related conversation.
 */
public final class BookingCompletionHelper {

    private static final String TAG = "BookingCompletion";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private BookingCompletionHelper() {
    }

    /**
     * @param onBookingFailedUi optional; run on main thread when Firestore {@code add} fails (re-enable UI).
     */
    public static void addBookingAndFinish(Activity activity,
                                           Map<String, Object> booking,
                                           String normalizedSlot,
                                           String providerId,
                                           String providerName,
                                           String logoUri,
                                           String selectedDateKey,
                                           Runnable onBookingFailedUi) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(activity, "Please log in again.", Toast.LENGTH_SHORT).show();
            if (onBookingFailedUi != null) {
                MAIN.post(onBookingFailedUi);
            }
            return;
        }
        final Context appCtx = activity.getApplicationContext();
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .add(booking)
                .addOnSuccessListener(doc -> {
                    createConversation(appCtx, doc.getId(), user, normalizedSlot,
                            providerId, providerName, logoUri, selectedDateKey);
                    MAIN.post(() -> openBookingConfirmation(appCtx, doc.getId()));
                })
                .addOnFailureListener(e -> {
                    if (onBookingFailedUi != null) {
                        MAIN.post(onBookingFailedUi);
                    }
                    if (e instanceof FirebaseFirestoreException
                            && ((FirebaseFirestoreException) e).getCode()
                            == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Toast.makeText(appCtx,
                                "Permission denied. Check Firestore rules for bookings.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    Toast.makeText(appCtx,
                            "Failed to book: " + (e.getMessage() != null ? e.getMessage() : "network error"),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Clears the task and opens confirmation so navigation works even if {@link Activity} was
     * destroyed while Firestore was slow (e.g. SSL timeouts).
     */
    private static void openBookingConfirmation(Context appContext, String bookingId) {
        Intent intent = new Intent(appContext, BookingConfirmationActivity.class);
        intent.putExtra(BookingConfirmationActivity.EXTRA_BOOKING_ID, bookingId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        appContext.startActivity(intent);
    }

    private static void createConversation(Context context,
                                           String bookingId,
                                           FirebaseUser user,
                                           String slot,
                                           String providerId,
                                           String providerName,
                                           String logoUri,
                                           String selectedDateKey) {
        if (TextUtils.isEmpty(bookingId) || user == null) {
            Log.w(TAG, "Skip conversation: bookingId or user missing");
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String fallbackName = !TextUtils.isEmpty(user.getEmail()) ? user.getEmail() : user.getUid();
        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String userName = doc.getString("fullName");
                    if (TextUtils.isEmpty(userName)) {
                        userName = doc.getString("firstName");
                    }
                    if (TextUtils.isEmpty(userName)) {
                        userName = fallbackName;
                    }
                    String userLogoUri = doc.getString("photoUrl");
                    if (TextUtils.isEmpty(userLogoUri)) {
                        userLogoUri = doc.getString("profilePhotoUri");
                    }
                    if (TextUtils.isEmpty(userLogoUri) && user.getPhotoUrl() != null) {
                        userLogoUri = user.getPhotoUrl().toString();
                    }
                    writeConversation(context, db, bookingId, user, userName, userLogoUri, slot,
                            providerId, providerName, logoUri, selectedDateKey);
                })
                .addOnFailureListener(e -> {
                    String fallbackLogo = user.getPhotoUrl() != null
                            ? user.getPhotoUrl().toString()
                            : null;
                    writeConversation(context, db, bookingId, user, fallbackName, fallbackLogo, slot,
                            providerId, providerName, logoUri, selectedDateKey);
                });
    }

    private static void writeConversation(Context context,
                                          FirebaseFirestore db,
                                          String bookingId,
                                          FirebaseUser user,
                                          String userName,
                                          String userLogoUri,
                                          String slot,
                                          String providerId,
                                          String providerName,
                                          String logoUri,
                                          String selectedDateKey) {
        String initialMessage = "Hi! I just booked an appointment for "
                + selectedDateKey + " " + slot + ".";

        Map<String, Object> convo = new java.util.HashMap<>();
        convo.put("bookingId", bookingId);
        convo.put("providerId", providerId);
        convo.put("providerName", providerName);
        convo.put("providerLogoUri", logoUri);
        convo.put("userId", user.getUid());
        convo.put("userName", userName);
        convo.put("userLogoUri", userLogoUri);
        convo.put("unreadUserCount", 0);
        convo.put("unreadProviderCount", 1);
        convo.put("createdAt", FieldValue.serverTimestamp());
        convo.put("lastMessage", initialMessage);
        convo.put("lastMessageAt", FieldValue.serverTimestamp());

        Map<String, Object> message = new java.util.HashMap<>();
        message.put("senderId", user.getUid());
        message.put("senderRole", "user");
        message.put("text", initialMessage);
        message.put("createdAt", FieldValue.serverTimestamp());

        com.google.firebase.firestore.DocumentReference convoRef =
                db.collection("conversations").document(bookingId);
        convoRef.set(convo, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    convoRef.collection("messages").document().set(message)
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to create seed message", e);
                                Toast.makeText(context,
                                        "Booked, but initial chat message failed. Check Firestore rules.",
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create conversation", e);
                    Toast.makeText(context,
                            "Booked, but chat thread was not created. Check Firestore rules.",
                            Toast.LENGTH_LONG).show();
                });
    }

    public static String createBookingNumber(String dateKey) {
        String datePart = dateKey != null ? dateKey.replace("-", "") : "00000000";
        long suffix = System.currentTimeMillis() % 10000;
        return String.format(Locale.US, "%s%04d", datePart, suffix);
    }
}
