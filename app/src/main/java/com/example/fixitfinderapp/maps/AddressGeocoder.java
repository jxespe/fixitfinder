package com.example.fixitfinderapp.maps;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.text.TextUtils;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AddressGeocoder {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private AddressGeocoder() { }

    public static void updateLatLng(Context context,
                                    String collection,
                                    String docId,
                                    String addressText) {
        if (context == null || TextUtils.isEmpty(collection)
                || TextUtils.isEmpty(docId) || TextUtils.isEmpty(addressText)) {
            return;
        }
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(appContext, Locale.getDefault());
                List<Address> results = geocoder.getFromLocationName(addressText, 1);
                if (results == null || results.isEmpty()) {
                    return;
                }
                Address address = results.get(0);
                double lat = address.getLatitude();
                double lng = address.getLongitude();
                java.util.Map<String, Object> updates = new java.util.HashMap<>();
                updates.put("lat", lat);
                updates.put("lng", lng);
                FirebaseFirestore.getInstance()
                        .collection(collection)
                        .document(docId)
                        .set(updates, com.google.firebase.firestore.SetOptions.merge());
            } catch (Exception ignored) {
                // Ignore geocoding failures; user can retry by editing address.
            }
        });
    }
}
