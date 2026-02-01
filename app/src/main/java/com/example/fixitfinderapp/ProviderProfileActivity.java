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
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

public class ProviderProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProviderProfileActivity";
    private ImageView ivCompanyLogo;
    private TextView tvCompanyName;
    private TextView tvServiceCategory;
    private TextView tvAddress;
    private FirebaseUser user;

    private Uri pendingCropOutput;
    private Uri pendingInput;
    private Uri pendingSource;

    private final ActivityResultLauncher<String> logoPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onLogoPicked);

    private final ActivityResultLauncher<Intent> cropLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    this::onCropResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_profile);

        user = FirebaseAuth.getInstance().getCurrentUser();

        ImageButton btnBack = findViewById(R.id.btnBack);
        ivCompanyLogo = findViewById(R.id.ivCompanyLogo);
        tvCompanyName = findViewById(R.id.tvCompanyName);
        tvServiceCategory = findViewById(R.id.tvServiceCategory);
        tvAddress = findViewById(R.id.tvCompanyAddress);
        ImageButton btnChangeLogo = findViewById(R.id.btnChangeLogo);

        btnBack.setOnClickListener(v -> finish());
        btnChangeLogo.setOnClickListener(v -> logoPicker.launch("image/*"));

        loadProfile();
    }

    private void loadProfile() {
        if (user == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("providers")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String companyName = doc.getString("fullName");
                    String category = doc.getString("serviceCategory");
                    String address = doc.getString("address");
                    String logoUri = doc.getString("logoUri");

                    tvCompanyName.setText(!TextUtils.isEmpty(companyName)
                            ? companyName
                            : "Company Name");
                    tvServiceCategory.setText(!TextUtils.isEmpty(category)
                            ? category
                            : "Service Category");
                    tvAddress.setText(!TextUtils.isEmpty(address)
                            ? address
                            : "Address");

                    if (!TextUtils.isEmpty(logoUri)) {
                        ivCompanyLogo.setImageURI(Uri.parse(logoUri));
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    private void onLogoPicked(Uri uri) {
        if (uri == null || user == null) {
            return;
        }
        pendingInput = uri;
        Uri source = copyToCacheUri(uri);
        if (source == null) {
            applyLogo(uri);
            return;
        }
        pendingSource = source;
        startCrop(source);
    }

    private void startCrop(Uri source) {
        Uri outputUri = createTempImageUri();
        if (outputUri == null) {
            applyLogo(source);
            return;
        }
        pendingCropOutput = outputUri;
        UCrop.Options options = new UCrop.Options();
        options.setToolbarTitle("Crop Logo");
        options.setHideBottomControls(false);
        options.setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(90);
        options.setFreeStyleCropEnabled(true);
        options.setCircleDimmedLayer(false);

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
                applyLogo(output);
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
            applyLogo(pendingSource);
        } else if (pendingInput != null) {
            applyLogo(pendingInput);
        }
    }

    private void applyLogo(Uri uri) {
        if (uri == null || user == null) {
            return;
        }
        // Persist read permission so the logo remains available.
        try {
            getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
            // Some providers don't allow persistable permissions.
        }
        ivCompanyLogo.setImageURI(uri);
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("logoUri", uri.toString());
        FirebaseFirestore.getInstance()
                .collection("providers")
                .document(user.getUid())
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Logo updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save logo", e);
                    Toast.makeText(this, "Failed to save logo: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private Uri createTempImageUri() {
        try {
            java.io.File tempFile = java.io.File.createTempFile(
                    "logo_crop_", ".jpg", getCacheDir());
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
