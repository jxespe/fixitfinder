package com.example.fixitfinderapp.maps;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.fixitfinderapp.R;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.firestore.FirebaseFirestore;
import android.widget.Toast;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

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
                    snapshot.forEach(doc -> {
                        Double lat = doc.getDouble("lat");
                        Double lng = doc.getDouble("lng");
                        if (lat == null || lng == null) {
                            return;
                        }
                        LatLng pos = new LatLng(lat, lng);
                        String title = doc.getString("fullName");
                        if (title == null) {
                            title = doc.getString("name");
                        }
                        if (title == null) {
                            title = "Service Provider";
                        }
                        map.addMarker(new MarkerOptions().position(pos).title(title));
                        bounds.include(pos);
                        hasMarker[0] = true;
                    });
                    if (hasMarker[0]) {
                        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 80));
                    }
                });
    }
}
