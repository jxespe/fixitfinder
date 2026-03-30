package com.example.fixitfinderapp;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fixitfinderapp.adapters.ProviderServiceAdapter;
import com.example.fixitfinderapp.models.ProviderServiceItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProviderServicesActivity extends BaseSwipeActivity {

    private final List<ProviderServiceItem> services = new ArrayList<>();
    private ProviderServiceAdapter adapter;
    private TextView tvEmpty;
    private FirebaseUser user;

    private String pendingImageUri;
    private ImageView pendingImageView;

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onImagePicked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_services);

        user = FirebaseAuth.getInstance().getCurrentUser();

        ImageButton btnBack = findViewById(R.id.btnBack);
        Button btnAddService = findViewById(R.id.btnAddService);
        RecyclerView recycler = findViewById(R.id.recyclerServices);
        tvEmpty = findViewById(R.id.tvEmptyServices);

        btnBack.setOnClickListener(v -> finish());
        btnAddService.setOnClickListener(v -> showServiceDialog(null));

        setupRecycler(recycler);
        attachSwipeToDelete(recycler);
        loadServices();
    }

    private void setupRecycler(RecyclerView recycler) {
        recycler.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapter = new ProviderServiceAdapter(
                services,
                true,
                false,
                this::showServiceDialog,
                (item, position) -> confirmSwipeDelete(item, position)
        );
        recycler.setAdapter(adapter);
    }

    private void attachSwipeToDelete(RecyclerView recycler) {
        ItemTouchHelper.SimpleCallback callback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(RecyclerView recyclerView,
                                          RecyclerView.ViewHolder viewHolder,
                                          RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getAdapterPosition();
                        if (position == RecyclerView.NO_POSITION || position >= services.size()) {
                            return;
                        }
                        ProviderServiceItem item = services.get(position);
                        confirmSwipeDelete(item, position);
                    }
                };
        new ItemTouchHelper(callback).attachToRecyclerView(recycler);
    }

    private void loadServices() {
        if (user == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("providers")
                .document(user.getUid())
                .collection("services")
                .get()
                .addOnSuccessListener(snapshot -> {
                    services.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        String imageUri = doc.getString("imageUri");
                        String description = doc.getString("description");
                        double price = 0d;
                        Object priceObj = doc.get("price");
                        if (priceObj instanceof Number) {
                            price = ((Number) priceObj).doubleValue();
                        }
                        services.add(new ProviderServiceItem(id, name, price, imageUri, description));
                    }
                    adapter.notifyDataSetChanged();
                    toggleEmptyState();
                })
                .addOnFailureListener(e -> {
                    String message = "Failed to load services: " + e.getMessage();
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                });
    }

    private void toggleEmptyState() {
        if (tvEmpty == null) {
            return;
        }
        tvEmpty.setVisibility(services.isEmpty()
                ? android.view.View.VISIBLE
                : android.view.View.GONE);
    }

    private void showServiceDialog(ProviderServiceItem existing) {
        if (user == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }
        pendingImageUri = existing != null ? existing.imageUri : null;
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_service_form, null);
        pendingImageView = view.findViewById(R.id.ivServicePreview);
        Button btnPickImage = view.findViewById(R.id.btnPickServiceImage);
        EditText edtName = view.findViewById(R.id.edtServiceName);
        EditText edtDescription = view.findViewById(R.id.edtServiceDescription);
        EditText edtPrice = view.findViewById(R.id.edtServicePrice);

        if (existing != null) {
            edtName.setText(existing.name);
            if (!TextUtils.isEmpty(existing.description)) {
                edtDescription.setText(existing.description);
            }
            if (existing.price > 0) {
                edtPrice.setText(String.valueOf(existing.price));
            }
        }
        if (!TextUtils.isEmpty(pendingImageUri)) {
            ImageLoader.load(pendingImageView, pendingImageUri, android.R.drawable.ic_menu_gallery);
        }

        btnPickImage.setOnClickListener(v -> imagePicker.launch("image/*"));

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Add Service" : "Edit Service")
                .setView(view)
                .setPositiveButton(existing == null ? "Add" : "Save", null)
                .setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = edtName.getText().toString().trim();
                String priceText = edtPrice.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    edtName.setError("Required");
                    return;
                }
                if (TextUtils.isEmpty(pendingImageUri)) {
                    Toast.makeText(this, "Please choose an image.", Toast.LENGTH_SHORT).show();
                    return;
                }
                double price;
                try {
                    price = Double.parseDouble(priceText);
                } catch (NumberFormatException ex) {
                    edtPrice.setError("Enter a valid price");
                    return;
                }
                if (price <= 0) {
                    edtPrice.setError("Price must be greater than 0");
                    return;
                }
                String description = edtDescription.getText().toString().trim();
                saveService(existing, name, description, price);
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private void confirmSwipeDelete(ProviderServiceItem existing, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete service?")
                .setMessage("This will remove the service from your list.")
                .setPositiveButton("Delete", (d, which) -> deleteService(existing, position))
                .setNegativeButton("Cancel", (d, which) -> adapter.notifyItemChanged(position))
                .show();
    }

    private void deleteService(ProviderServiceItem existing, int position) {
        FirebaseFirestore.getInstance()
                .collection("providers")
                .document(user.getUid())
                .collection("services")
                .document(existing.id)
                .delete()
                .addOnSuccessListener(unused -> {
                    if (position >= 0 && position < services.size()) {
                        services.remove(position);
                        adapter.notifyItemRemoved(position);
                    }
                    toggleEmptyState();
                    Toast.makeText(this, "Service deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete service", Toast.LENGTH_SHORT).show());
    }

    private void saveService(ProviderServiceItem existing, String name, String description, double price) {
        if (user == null) {
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        com.google.firebase.firestore.CollectionReference servicesRef =
                db.collection("providers")
                        .document(user.getUid())
                        .collection("services");
        com.google.firebase.firestore.DocumentReference docRef =
                existing != null ? servicesRef.document(existing.id) : servicesRef.document();

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        if (!TextUtils.isEmpty(description)) {
            data.put("description", description);
        } else {
            data.put("description", "");
        }
        data.put("price", price);
        data.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        if (existing == null) {
            data.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        }

        if (!TextUtils.isEmpty(pendingImageUri) && shouldUploadImage(pendingImageUri)) {
            uploadServiceImage(docRef, Uri.parse(pendingImageUri), data);
        } else {
            if (!TextUtils.isEmpty(pendingImageUri)) {
                data.put("imageUri", pendingImageUri);
            }
            docRef.set(data, SetOptions.merge())
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Service saved", Toast.LENGTH_SHORT).show();
                        loadServices();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to save service", Toast.LENGTH_SHORT).show());
        }
    }

    private void uploadServiceImage(com.google.firebase.firestore.DocumentReference docRef,
                                    Uri imageUri,
                                    Map<String, Object> data) {
        if (imageUri == null) {
            return;
        }
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference()
                .child("providers")
                .child(user.getUid())
                .child("services")
                .child(docRef.getId() + ".jpg");
        ref.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    data.put("imageUri", downloadUri.toString());
                    docRef.set(data, SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Service saved", Toast.LENGTH_SHORT).show();
                                loadServices();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to save service",
                                            Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show());
    }

    private boolean shouldUploadImage(String uriString) {
        if (TextUtils.isEmpty(uriString)) {
            return false;
        }
        Uri uri = Uri.parse(uriString);
        String scheme = uri.getScheme();
        return scheme != null && !scheme.toLowerCase(java.util.Locale.US).startsWith("http");
    }

    private void onImagePicked(Uri uri) {
        if (uri == null) {
            return;
        }
        pendingImageUri = uri.toString();
        if (pendingImageView != null) {
            ImageLoader.load(pendingImageView, pendingImageUri, android.R.drawable.ic_menu_gallery);
        }
    }
}
