package com.example.fixitfinderapp.maps;

import android.os.Bundle;
import android.location.Address;
import android.location.Geocoder;
import com.example.fixitfinderapp.BaseSwipeActivity;
import com.example.fixitfinderapp.R;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.firestore.FirebaseFirestore;
import android.widget.Toast;
import android.text.TextUtils;

public class MapsActivity extends BaseSwipeActivity implements OnMapReadyCallback {

    private static final java.util.concurrent.ExecutorService GEO_EXECUTOR =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private interface GeocodeCallback {
        void onResult(LatLng result);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        if (mapFragment == null) {
            Toast.makeText(this, "Map fragment not found.", Toast.LENGTH_SHORT).show();
            return;
        }
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        LatLng fallbackCenter = new LatLng(14.5995, 120.9842);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(fallbackCenter, 12));

        FirebaseFirestore.getInstance()
                .collection("providers")
                .get()
                .addOnSuccessListener(snapshot -> {
                    LatLngBounds.Builder bounds = new LatLngBounds.Builder();
                    final boolean[] hasMarker = {false};
                    final java.util.concurrent.atomic.AtomicInteger pending =
                            new java.util.concurrent.atomic.AtomicInteger(snapshot.size());
                    snapshot.forEach(doc -> {
                        Boolean hideProfile = doc.getBoolean("hideProfile");
                        if (hideProfile != null && hideProfile) {
                            if (pending.decrementAndGet() == 0 && hasMarker[0]) {
                                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 80));
                            }
                            return;
                        }
                        Boolean shareLocation = doc.getBoolean("shareLocation");
                        if (shareLocation != null && !shareLocation) {
                            if (pending.decrementAndGet() == 0 && hasMarker[0]) {
                                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 80));
                            }
                            return;
                        }
                        Double lat = doc.getDouble("lat");
                        Double lng = doc.getDouble("lng");
                        if (lat != null && lng != null) {
                            LatLng pos = new LatLng(lat, lng);
                            String title = providerTitle(doc);
                            map.addMarker(new MarkerOptions().position(pos).title(title));
                            bounds.include(pos);
                            hasMarker[0] = true;
                            if (pending.decrementAndGet() == 0 && hasMarker[0]) {
                                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 80));
                            }
                            return;
                        }
                        String address = doc.getString("address");
                        if (TextUtils.isEmpty(address)) {
                            if (pending.decrementAndGet() == 0 && hasMarker[0]) {
                                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 80));
                            }
                            return;
                        }
                        geocodeAddress(address, result -> {
                            if (result != null) {
                                String title = providerTitle(doc);
                                map.addMarker(new MarkerOptions().position(result).title(title));
                                bounds.include(result);
                                hasMarker[0] = true;
                                saveLatLng(doc.getId(), result);
                            }
                            if (pending.decrementAndGet() == 0 && hasMarker[0]) {
                                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 80));
                            }
                        });
                    });
                    if (snapshot.isEmpty() && hasMarker[0]) {
                        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 80));
                    }
                });
    }

    private void geocodeAddress(String address, GeocodeCallback callback) {
        GEO_EXECUTOR.execute(() -> {
            LatLng result = null;
            try {
                Geocoder geocoder = new Geocoder(this, java.util.Locale.getDefault());
                java.util.List<Address> results = geocoder.getFromLocationName(address, 1);
                if (results != null && !results.isEmpty()) {
                    Address addr = results.get(0);
                    result = new LatLng(addr.getLatitude(), addr.getLongitude());
                }
            } catch (Exception ignored) { }
            LatLng finalResult = result;
            runOnUiThread(() -> callback.onResult(finalResult));
        });
    }

    private void saveLatLng(String providerId, LatLng pos) {
        if (providerId == null || pos == null) {
            return;
        }
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("lat", pos.latitude);
        updates.put("lng", pos.longitude);
        FirebaseFirestore.getInstance()
                .collection("providers")
                .document(providerId)
                .set(updates, com.google.firebase.firestore.SetOptions.merge());
    }

    private String providerTitle(com.google.firebase.firestore.DocumentSnapshot doc) {
        String title = doc.getString("fullName");
        if (title == null) {
            title = doc.getString("name");
        }
        if (title == null) {
            title = "Service Provider";
        }
        return title;
    }
}
