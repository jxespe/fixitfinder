package com.example.fixitfinderapp;

import android.content.Context;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

/**
 * Loads related Firestore user documents so booking UIs show current address, provider logo,
 * name, and contact when the booking snapshot is missing or stale.
 */
public final class BookingDisplayEnricher {

    private BookingDisplayEnricher() {
    }

    public static void enrichFromRelatedProfiles(Context context,
                                                 DocumentSnapshot bookingDoc,
                                                 TextView tvAddressLine,
                                                 ImageView ivProviderPhoto,
                                                 TextView tvProviderName,
                                                 TextView tvProviderContact) {
        if (bookingDoc == null) {
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (tvAddressLine != null && user != null
                && TextUtils.isEmpty(bookingDoc.getString("userAddress"))) {
            db.collection("users")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(userSnap -> {
                        if (!userSnap.exists()) {
                            return;
                        }
                        String addr = userSnap.getString("address");
                        if (!TextUtils.isEmpty(addr)) {
                            tvAddressLine.setText(addr);
                        }
                    });
        }

        String providerId = bookingDoc.getString("providerId");
        if (TextUtils.isEmpty(providerId) || ivProviderPhoto == null) {
            return;
        }
        db.collection("users")
                .document(providerId)
                .get()
                .addOnSuccessListener(pSnap -> {
                    if (!pSnap.exists()) {
                        return;
                    }
                    if (tvProviderName != null) {
                        String bookingName = bookingDoc.getString("providerName");
                        String fullName = pSnap.getString("fullName");
                        if (TextUtils.isEmpty(bookingName) && !TextUtils.isEmpty(fullName)) {
                            tvProviderName.setText(fullName);
                        }
                    }
                    String logo = pickLogoUri(bookingDoc.getString("providerLogoUri"),
                            pSnap.getString("logoUri"));
                    if (TextUtils.isEmpty(logo)) {
                        logo = pSnap.getString("photoUrl");
                    }
                    if (TextUtils.isEmpty(logo)) {
                        logo = pSnap.getString("profilePhotoUri");
                    }
                    ImageLoader.loadProfile(ivProviderPhoto, logo, android.R.drawable.ic_menu_myplaces);

                    if (tvProviderContact != null) {
                        String phone = pSnap.getString("phone");
                        String email = pSnap.getString("email");
                        String contact = !TextUtils.isEmpty(phone) ? phone.trim()
                                : (!TextUtils.isEmpty(email) ? email.trim() : "");
                        if (!TextUtils.isEmpty(contact)) {
                            tvProviderContact.setText(contact);
                        } else {
                            tvProviderContact.setText(R.string.contact_information);
                        }
                    }
                });
    }

    public static String formatPaymentMethodLabel(String key) {
        if (TextUtils.isEmpty(key)) {
            return "";
        }
        String k = key.trim().toLowerCase(Locale.US);
        if ("gcash".equals(k)) {
            return "GCash";
        }
        if ("bank_transfer".equals(k)) {
            return "Bank transfer";
        }
        if ("cash".equals(k)) {
            return "Cash";
        }
        if ("apple_pay".equals(k)) {
            return "Apple Pay";
        }
        return key.trim();
    }

    private static String pickLogoUri(String primary, String fallback) {
        if (!TextUtils.isEmpty(primary)) {
            return primary;
        }
        return !TextUtils.isEmpty(fallback) ? fallback : null;
    }
}
