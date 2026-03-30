package com.example.fixitfinderapp.auth;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Allocates human-readable account IDs via Firestore transactions.
 * User: U{year}{5-digit sequence}. Provider: {categoryPrefix}{5-digit sequence}.
 */
public final class AccountPublicIdHelper {

    private static final String SEQ_COLLECTION = "account_id_sequences";
    private static final int SEQ_WIDTH = 5;

    public interface IdCallback {
        void onAllocated(@NonNull String accountId);

        void onError(@NonNull Exception e);
    }

    private AccountPublicIdHelper() {
    }

    /**
     * Maps provider registration category labels (see {@code strings.xml} {@code service_categories})
     * to ID prefixes. Electrical Repair is not in the product spec; uses ELR.
     */
    @NonNull
    public static String providerPrefixForCategory(String serviceCategory) {
        if (TextUtils.isEmpty(serviceCategory)) {
            return "GEN";
        }
        String n = serviceCategory.trim().toLowerCase(Locale.US);
        if (n.contains("carpentry")) {
            return "CRP";
        }
        if (n.contains("plumbing")) {
            return "PLS";
        }
        if (n.contains("aircon")) {
            return "ACS";
        }
        if (n.contains("car") && n.contains("mechanic")) {
            return "CRM";
        }
        if (n.contains("appliance")) {
            return "APR";
        }
        if (n.contains("electronics")) {
            return "DVR";
        }
        if (n.contains("electrical")) {
            return "ELR";
        }
        if (n.contains("internet")) {
            return "INR";
        }
        return "GEN";
    }

    public static void allocateNextUserId(@NonNull FirebaseFirestore db, @NonNull IdCallback callback) {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        String docId = "user_" + year;
        DocumentReference ref = db.collection(SEQ_COLLECTION).document(docId);
        db.runTransaction((Transaction.Function<Long>) transaction -> {
            DocumentSnapshot snap = transaction.get(ref);
            long next = 1L;
            if (snap.exists()) {
                Long cur = snap.getLong("seq");
                if (cur != null) {
                    next = cur + 1;
                }
            }
            Map<String, Object> data = new HashMap<>();
            data.put("seq", next);
            transaction.set(ref, data, SetOptions.merge());
            return next;
        }).addOnSuccessListener(seq -> {
            String id = "U" + year + String.format(Locale.US, "%0" + SEQ_WIDTH + "d", seq);
            callback.onAllocated(id);
        }).addOnFailureListener(callback::onError);
    }

    public static void allocateNextProviderId(@NonNull FirebaseFirestore db,
                                              @NonNull String serviceCategory,
                                              @NonNull IdCallback callback) {
        String prefix = providerPrefixForCategory(serviceCategory);
        String docId = "provider_" + prefix;
        DocumentReference ref = db.collection(SEQ_COLLECTION).document(docId);
        db.runTransaction((Transaction.Function<Long>) transaction -> {
            DocumentSnapshot snap = transaction.get(ref);
            long next = 1L;
            if (snap.exists()) {
                Long cur = snap.getLong("seq");
                if (cur != null) {
                    next = cur + 1;
                }
            }
            Map<String, Object> data = new HashMap<>();
            data.put("seq", next);
            transaction.set(ref, data, SetOptions.merge());
            return next;
        }).addOnSuccessListener(seq -> {
            String id = prefix + String.format(Locale.US, "%0" + SEQ_WIDTH + "d", seq);
            callback.onAllocated(id);
        }).addOnFailureListener(callback::onError);
    }
}
