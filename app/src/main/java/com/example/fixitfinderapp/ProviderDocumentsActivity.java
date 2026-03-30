package com.example.fixitfinderapp;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class ProviderDocumentsActivity extends BaseSwipeActivity {

    public static final String DOC_TYPE_ID_DTI = "ID / DTI";
    public static final String DOC_TYPE_BIR = "BIR";
    public static final String DOC_TYPE_BUSINESS_PERMIT = "Business Permit";

    private final Map<String, Uri> selectedUris = new HashMap<>();
    private String pendingDocType;
    private ActivityResultLauncher<String> docPicker;
    private TextView tvStatusIdDti;
    private TextView tvStatusBir;
    private TextView tvStatusPermit;
    private Button btnPickIdDti;
    private Button btnPickBir;
    private Button btnPickPermit;
    private Button btnSubmit;
    private String providerName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_documents);

        ImageButton btnBack = findViewById(R.id.btnBack);
        tvStatusIdDti = findViewById(R.id.tvStatusIdDti);
        tvStatusBir = findViewById(R.id.tvStatusBir);
        tvStatusPermit = findViewById(R.id.tvStatusPermit);
        btnPickIdDti = findViewById(R.id.btnPickIdDti);
        btnPickBir = findViewById(R.id.btnPickBir);
        btnPickPermit = findViewById(R.id.btnPickPermit);
        btnSubmit = findViewById(R.id.btnSubmitDocuments);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        docPicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null || TextUtils.isEmpty(pendingDocType)) {
                        return;
                    }
                    selectedUris.put(pendingDocType, uri);
                    updateStatusAfterPick(pendingDocType);
                });

        btnPickIdDti.setOnClickListener(v -> openPicker(DOC_TYPE_ID_DTI));
        btnPickBir.setOnClickListener(v -> openPicker(DOC_TYPE_BIR));
        btnPickPermit.setOnClickListener(v -> openPicker(DOC_TYPE_BUSINESS_PERMIT));

        btnSubmit.setOnClickListener(v -> submitDocuments());

        loadProviderNameAndStatuses();
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadFirestoreStatusesOnly();
    }

    private void openPicker(String docType) {
        pendingDocType = docType;
        docPicker.launch("*/*");
    }

    private void updateStatusAfterPick(String docType) {
        Uri uri = selectedUris.get(docType);
        String label = uri != null ? shortFileLabel(uri) : "—";
        String line = "Selected: " + label;
        if (DOC_TYPE_ID_DTI.equals(docType)) {
            tvStatusIdDti.setText(line);
        } else if (DOC_TYPE_BIR.equals(docType)) {
            tvStatusBir.setText(line);
        } else if (DOC_TYPE_BUSINESS_PERMIT.equals(docType)) {
            tvStatusPermit.setText(line);
        }
    }

    private String shortFileLabel(Uri uri) {
        String name = getFileName(uri);
        if (TextUtils.isEmpty(name)) {
            return "file attached";
        }
        if (name.length() > 28) {
            return name.substring(0, 25) + "…";
        }
        return name;
    }

    private void loadProviderNameAndStatuses() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("providers")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    providerName = doc.getString("fullName");
                    if (TextUtils.isEmpty(providerName)) {
                        providerName = "";
                    }
                    loadFirestoreStatusesOnly();
                })
                .addOnFailureListener(e -> loadFirestoreStatusesOnly());
    }

    private void loadFirestoreStatusesOnly() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("provider_documents")
                .whereEqualTo("providerId", user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    applyLatestStatus(DOC_TYPE_ID_DTI, snapshot.getDocuments());
                    applyLatestStatus(DOC_TYPE_BIR, snapshot.getDocuments());
                    applyLatestStatus(DOC_TYPE_BUSINESS_PERMIT, snapshot.getDocuments());
                });
    }

    private void applyLatestStatus(String docType, java.util.List<DocumentSnapshot> docs) {
        if (selectedUris.containsKey(docType)) {
            return;
        }
        DocumentSnapshot latest = null;
        for (DocumentSnapshot d : docs) {
            String type = d.getString("docType");
            if (!docType.equals(type)) {
                continue;
            }
            if (latest == null || isNewer(d, latest)) {
                latest = d;
            }
        }
        TextView target = statusViewForType(docType);
        if (target == null) {
            return;
        }
        if (latest == null) {
            target.setText("No file submitted yet");
            target.setTextColor(0xFF757575);
            return;
        }
        String status = latest.getString("status");
        if (TextUtils.isEmpty(status)) {
            status = "Pending";
        }
        String line = "Latest: " + status;
        if ("Approved".equalsIgnoreCase(status)) {
            target.setTextColor(0xFF4CAF50);
        } else if ("Rejected".equalsIgnoreCase(status)) {
            target.setTextColor(0xFFF44336);
        } else {
            target.setTextColor(0xFFFF9800);
        }
        target.setText(line);
    }

    private boolean isNewer(DocumentSnapshot a, DocumentSnapshot b) {
        Object ta = a.get("submittedAt");
        Object tb = b.get("submittedAt");
        if (ta instanceof com.google.firebase.Timestamp && tb instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) ta).compareTo((com.google.firebase.Timestamp) tb) > 0;
        }
        return false;
    }

    private TextView statusViewForType(String docType) {
        if (DOC_TYPE_ID_DTI.equals(docType)) {
            return tvStatusIdDti;
        }
        if (DOC_TYPE_BIR.equals(docType)) {
            return tvStatusBir;
        }
        if (DOC_TYPE_BUSINESS_PERMIT.equals(docType)) {
            return tvStatusPermit;
        }
        return null;
    }

    private void submitDocuments() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in again.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!selectedUris.containsKey(DOC_TYPE_ID_DTI)
                || !selectedUris.containsKey(DOC_TYPE_BIR)
                || !selectedUris.containsKey(DOC_TYPE_BUSINESS_PERMIT)) {
            Toast.makeText(this, "Please choose a file for all three document types.", Toast.LENGTH_LONG).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Uploading…");

        String uid = user.getUid();
        String name = !TextUtils.isEmpty(providerName) ? providerName : (user.getDisplayName() != null
                ? user.getDisplayName()
                : "");

        AtomicInteger remaining = new AtomicInteger(3);
        AtomicBoolean failed = new AtomicBoolean(false);

        uploadOne(uid, name, DOC_TYPE_ID_DTI, selectedUris.get(DOC_TYPE_ID_DTI), remaining, failed);
        uploadOne(uid, name, DOC_TYPE_BIR, selectedUris.get(DOC_TYPE_BIR), remaining, failed);
        uploadOne(uid, name, DOC_TYPE_BUSINESS_PERMIT, selectedUris.get(DOC_TYPE_BUSINESS_PERMIT),
                remaining, failed);
    }

    private void uploadOne(String userId,
                           String providerDisplayName,
                           String docType,
                           Uri uri,
                           AtomicInteger remaining,
                           AtomicBoolean failed) {
        String fileName = getFileName(uri);
        String safeName = TextUtils.isEmpty(fileName)
                ? docType.replace(" ", "_").replace("/", "_").toLowerCase(Locale.US)
                + "_" + System.currentTimeMillis()
                : fileName;
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference()
                .child("providers")
                .child(userId)
                .child("documents")
                .child(safeName);
        ref.putFile(uri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUrl ->
                        saveDocumentMeta(userId, providerDisplayName, docType, safeName,
                                downloadUrl.toString(), remaining, failed))
                .addOnFailureListener(e -> {
                    failed.set(true);
                    if (remaining.decrementAndGet() == 0) {
                        onAllUploadsFinished(failed.get());
                    }
                });
    }

    private void saveDocumentMeta(String userId,
                                  String providerDisplayName,
                                  String docType,
                                  String fileName,
                                  String fileUrl,
                                  AtomicInteger remaining,
                                  AtomicBoolean failed) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("providerId", userId);
        doc.put("providerName", providerDisplayName);
        doc.put("docType", docType);
        doc.put("fileName", fileName);
        doc.put("fileUrl", fileUrl);
        doc.put("status", "Pending");
        doc.put("submittedAt", FieldValue.serverTimestamp());
        FirebaseFirestore.getInstance()
                .collection("provider_documents")
                .add(doc)
                .addOnSuccessListener(unused -> {
                    if (remaining.decrementAndGet() == 0) {
                        onAllUploadsFinished(failed.get());
                    }
                })
                .addOnFailureListener(e -> {
                    failed.set(true);
                    if (remaining.decrementAndGet() == 0) {
                        onAllUploadsFinished(failed.get());
                    }
                });
    }

    private void onAllUploadsFinished(boolean failed) {
        runOnUiThread(() -> {
            btnSubmit.setEnabled(true);
            btnSubmit.setText(R.string.submit_documents);
            if (failed) {
                Toast.makeText(this, "Some uploads failed. Please try again.", Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(this, "Documents submitted for review.", Toast.LENGTH_LONG).show();
            selectedUris.clear();
            loadFirestoreStatusesOnly();
        });
    }

    private String getFileName(Uri uri) {
        if (uri == null) {
            return null;
        }
        String name = null;
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    name = cursor.getString(idx);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return name;
    }
}
