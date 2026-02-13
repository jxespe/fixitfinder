package com.example.fixitfinderapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yalantis.ucrop.UCrop;

public class UserProfileActivity extends AppCompatActivity {

    private static final String TAG = "UserProfileActivity";
    private ImageView ivProfilePhoto;
    private TextView tvFullName;
    private TextView tvEmail;
    private TextView tvPhone;
    private TextView tvAddress;
    private FirebaseUser user;

    private Uri pendingCropOutput;
    private Uri pendingInput;
    private Uri pendingSource;

    private final ActivityResultLauncher<String> photoPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onPhotoPicked);

    private final ActivityResultLauncher<Intent> cropLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    this::onCropResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        user = FirebaseAuth.getInstance().getCurrentUser();

        ImageButton btnBack = findViewById(R.id.btnBack);
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        tvFullName = findViewById(R.id.tvFullName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvAddress = findViewById(R.id.tvAddress);
        ImageButton btnChangePhoto = findViewById(R.id.btnChangePhoto);

        btnBack.setOnClickListener(v -> finish());
        btnChangePhoto.setOnClickListener(v -> photoPicker.launch("image/*"));

        loadProfile();
    }

    private void loadProfile() {
        if (user == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String fullName = doc.getString("fullName");
                    String email = doc.getString("email");
                    String phone = doc.getString("phone");
                    String address = doc.getString("address");
                    String photo = doc.getString("photoUrl");
                    if (TextUtils.isEmpty(photo)) {
                        photo = doc.getString("profilePhotoUri");
                    }
                    if (TextUtils.isEmpty(photo) && user.getPhotoUrl() != null) {
                        photo = user.getPhotoUrl().toString();
                    }

                    tvFullName.setText(!TextUtils.isEmpty(fullName) ? fullName : "Name");
                    tvEmail.setText(!TextUtils.isEmpty(email) ? email : "Email");
                    tvPhone.setText(!TextUtils.isEmpty(phone) ? phone : "Phone");
                    tvAddress.setText(!TextUtils.isEmpty(address) ? address : "Address");
                    ImageLoader.load(ivProfilePhoto, photo, android.R.drawable.ic_menu_myplaces);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    private void onPhotoPicked(Uri uri) {
        if (uri == null || user == null) {
            return;
        }
        pendingInput = uri;
        Uri source = copyToCacheUri(uri);
        if (source == null) {
            applyPhoto(uri);
            return;
        }
        pendingSource = source;
        startCrop(source);
    }

    private void startCrop(Uri source) {
        Uri outputUri = createTempImageUri();
        if (outputUri == null) {
            applyPhoto(source);
            return;
        }
        pendingCropOutput = outputUri;
        UCrop.Options options = new UCrop.Options();
        options.setToolbarTitle("Crop Photo");
        options.setHideBottomControls(false);
        options.setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(90);
        options.setFreeStyleCropEnabled(true);
        options.setCircleDimmedLayer(true);

        Intent cropIntent = UCrop.of(source, outputUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(512, 512)
                .withOptions(options)
                .getIntent(this);
        cropLauncher.launch(cropIntent);
    }

    private void onCropResult(ActivityResult result) {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri output = UCrop.getOutput(result.getData());
            if (output != null) {
                applyPhoto(output);
                return;
            }
        }
        if (result.getResultCode() == UCrop.RESULT_ERROR && result.getData() != null) {
            Throwable error = UCrop.getError(result.getData());
            if (error != null) {
                Toast.makeText(this, "Crop failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        if (pendingSource != null) {
            applyPhoto(pendingSource);
        } else if (pendingInput != null) {
            applyPhoto(pendingInput);
        }
    }

    private void applyPhoto(Uri uri) {
        if (uri == null || user == null) {
            return;
        }
        ImageLoader.load(ivProfilePhoto, uri.toString(), android.R.drawable.ic_menu_myplaces);
        uploadPhotoToStorage(uri);
    }

    private void uploadPhotoToStorage(Uri uri) {
        if (uri == null || user == null) {
            return;
        }
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference()
                .child("users")
                .child(user.getUid())
                .child("profile.jpg");
        ref.putFile(uri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> savePhotoUrl(downloadUri.toString()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Photo upload failed", e);
                    Toast.makeText(this, "Failed to upload photo: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void savePhotoUrl(String url) {
        if (TextUtils.isEmpty(url) || user == null) {
            return;
        }
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("profilePhotoUri", url);
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Photo updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save photo URL", e);
                    Toast.makeText(this, "Failed to save photo: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });

        updateConversationAvatars(url);
    }

    private void updateConversationAvatars(String url) {
        if (TextUtils.isEmpty(url) || user == null) {
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("conversations")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        doc.getReference().update("userLogoUri", url);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update conversation avatars", e));
    }

    private Uri createTempImageUri() {
        try {
            java.io.File tempFile = java.io.File.createTempFile(
                    "user_crop_", ".jpg", getCacheDir());
            return FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", tempFile);
        } catch (Exception e) {
            return null;
        }
    }

    private Uri copyToCacheUri(Uri source) {
        if (source == null) {
            return null;
        }
        Uri dest = createTempImageUri();
        if (dest == null) {
            return null;
        }
        try (java.io.InputStream input = getContentResolver().openInputStream(source);
             java.io.OutputStream output = getContentResolver().openOutputStream(dest)) {
            if (input == null || output == null) {
                return null;
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();
            return dest;
        } catch (Exception e) {
            return null;
        }
    }
}
