package com.example.fixitfinderapp.maps;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.fixitfinderapp.R;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.firestore.FirebaseFirestore;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        FirebaseFirestore.getInstance()
                .collection("providers")
                .get()
                .addOnSuccessListener(snapshot -> {
                    snapshot.forEach(doc -> {
                        LatLng pos = new LatLng(
                                doc.getDouble("lat"),
                                doc.getDouble("lng")
                        );
                        map.addMarker(new MarkerOptions()
                                .position(pos)
                                .title(doc.getString("name")));
                    });
                });

        map.moveCamera(CameraUpdateFactory
                .newLatLngZoom(new LatLng(14.5995, 120.9842), 12));
    }
}
