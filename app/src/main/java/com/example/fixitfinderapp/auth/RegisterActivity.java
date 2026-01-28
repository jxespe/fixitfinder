package com.example.fixitfinderapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitfinderapp.R;
import com.example.fixitfinderapp.auth.OtpActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.OAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final int RC_GOOGLE_SIGN_IN = 9001;
    private static final String TAG = "RegisterActivity";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText edtEmail;
    private EditText edtPhone;
    private EditText edtPassword;
    private EditText edtAddress;
    private Button btnRegister;
    private Button btnGoogle;
    private Button btnFacebook;
    private Button btnAppleId;
    private ImageView btnPasswordToggle;
    private boolean isPasswordVisible = false;
    private GoogleSignInClient googleSignInClient;
    private Spinner spinnerCountryCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        edtAddress = findViewById(R.id.edtAddress);
        edtPassword = findViewById(R.id.edtPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnFacebook = findViewById(R.id.btnFacebook);
        btnAppleId = findViewById(R.id.btnAppleId);
        spinnerCountryCode = findViewById(R.id.spinnerCountryCode);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // Password toggle functionality
        btnPasswordToggle = findViewById(R.id.btnPasswordToggle);
        if (btnPasswordToggle != null) {
            btnPasswordToggle.setOnClickListener(v -> {
                if (isPasswordVisible) {
                    edtPassword.setTransformationMethod(new PasswordTransformationMethod());
                    btnPasswordToggle.setImageResource(android.R.drawable.ic_menu_view);
                    isPasswordVisible = false;
                } else {
                    edtPassword.setTransformationMethod(null);
                    btnPasswordToggle.setImageResource(android.R.drawable.ic_menu_revert);
                    isPasswordVisible = true;
                }
                edtPassword.setSelection(edtPassword.getText().length());
            });
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        if (btnGoogle != null) {
            btnGoogle.setOnClickListener(v -> startGoogleSignIn());
        }
        if (btnFacebook != null) {
            btnFacebook.setOnClickListener(v -> startOAuthProviderSignIn("facebook.com"));
        }
        if (btnAppleId != null) {
            btnAppleId.setOnClickListener(v -> startOAuthProviderSignIn("apple.com"));
        }

        btnRegister.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String phone = buildPhoneNumber();
            String address = edtAddress.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            // Validation
            if (TextUtils.isEmpty(email)) {
                edtEmail.setError("Email is required");
                edtEmail.requestFocus();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edtEmail.setError("Please enter a valid email address");
                edtEmail.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(phone)) {
                edtPhone.setError("Phone number is required");
                edtPhone.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(password)) {
                edtPassword.setError("Password is required");
                edtPassword.requestFocus();
                return;
            }

            if (!isStrongPassword(password)) {
                edtPassword.setError("Use 8+ characters with letters and numbers");
                edtPassword.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(address)) {
                edtAddress.setError("Address is required");
                edtAddress.requestFocus();
                return;
            }

            // Disable button during registration
            btnRegister.setEnabled(false);
            btnRegister.setText("Submitting...");

            // Create user with Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // User created successfully, now save to Firestore
                                FirebaseUser user = auth.getCurrentUser();
                                if (user != null) {
                                    saveUserToFirestore(user.getUid(), email, phone, address);
                                }
                            } else {
                                // Registration failed
                                btnRegister.setEnabled(true);
                                btnRegister.setText("Submit");
                                
                                String errorMessage = "Registration failed";
                                if (task.getException() != null) {
                                    errorMessage = task.getException().getMessage();
                                }
                                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });
    }

    private void saveUserToFirestore(String userId, String email, String phone, String address) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", userId);
        userData.put("email", email);
        userData.put("phone", phone);
        userData.put("address", address);
        userData.put("role", "user");
        userData.put("phoneVerified", false);
        userData.put("createdAt", System.currentTimeMillis());

        // Save to Firestore
        db.collection("users")
                .document(userId)
                .set(userData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Successfully saved to Firestore
                        Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();

                        // Proceed to OTP verification
                        Intent intent = new Intent(RegisterActivity.this, OtpActivity.class);
                        intent.putExtra("phone", phone);
                        intent.putExtra("role", "user");
                        intent.putExtra("email", email);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        // Failed to save to Firestore, but user is created in Auth
                        // We should still show success, but log the error
                        Toast.makeText(RegisterActivity.this, "Account created but failed to save profile. Please try logging in.", Toast.LENGTH_LONG).show();
                        
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Submit");
                        
                        // Proceed to OTP verification anyway
                        Intent intent = new Intent(RegisterActivity.this, OtpActivity.class);
                        intent.putExtra("phone", phone);
                        intent.putExtra("role", "user");
                        intent.putExtra("email", email);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    private boolean isStrongPassword(String password) {
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        return password.length() >= 8 && hasLetter && hasDigit;
    }

    private String buildPhoneNumber() {
        String local = edtPhone != null ? edtPhone.getText().toString().trim() : "";
        if (TextUtils.isEmpty(local)) {
            if (edtPhone != null) {
                edtPhone.setError("Phone number is required");
                edtPhone.requestFocus();
            }
            return "";
        }
        String stripped = local.replaceAll("\\s+", "");
        if (stripped.startsWith("0")) {
            stripped = stripped.substring(1);
        }
        if (stripped.startsWith("+")) {
            stripped = stripped.substring(1);
        }
        String code = "+63";
        if (spinnerCountryCode != null && spinnerCountryCode.getSelectedItem() != null) {
            String selected = spinnerCountryCode.getSelectedItem().toString().trim();
            if (!TextUtils.isEmpty(selected)) {
                code = selected;
            }
        }
        return code + stripped;
    }

    private void startGoogleSignIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account);
                } else {
                    Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_LONG).show();
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign-in failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            saveGoogleUserProfile(user, account);
                        }
                    } else {
                        String errorMessage = "Google sign-in failed";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveGoogleUserProfile(FirebaseUser user, GoogleSignInAccount account) {
        String email = user.getEmail();
        String displayName = user.getDisplayName();
        String firstName = account.getGivenName();
        if (TextUtils.isEmpty(firstName) && !TextUtils.isEmpty(displayName)) {
            String[] parts = displayName.trim().split("\\s+");
            firstName = parts.length > 0 ? parts[0] : displayName;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("email", !TextUtils.isEmpty(email) ? email : "");
        userData.put("fullName", !TextUtils.isEmpty(displayName) ? displayName : "");
        userData.put("firstName", !TextUtils.isEmpty(firstName) ? firstName : "");
        userData.put("role", "user");
        userData.put("phoneVerified", false);
        userData.put("createdAt", System.currentTimeMillis());
        userData.put("authProvider", "google.com");
        userData.put("phone", "");
        userData.put("address", "");

        db.collection("users")
                .document(user.getUid())
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Signed in with Google!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, OtpActivity.class);
                    intent.putExtra("role", "user");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Google profile save failed", e);
                    Toast.makeText(this, "Signed in, but profile save failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void startOAuthProviderSignIn(String providerId) {
        OAuthProvider.Builder provider = OAuthProvider.newBuilder(providerId);
        if ("apple.com".equals(providerId)) {
            provider.setScopes(Arrays.asList("email", "name"));
        }

        Task<AuthResult> pendingResultTask = auth.getPendingAuthResult();
        if (pendingResultTask != null) {
            pendingResultTask
                    .addOnSuccessListener(result -> handleOAuthResult(result, providerId))
                    .addOnFailureListener(e -> showOAuthError(providerId, e));
            return;
        }

        auth.startActivityForSignInWithProvider(this, provider.build())
                .addOnSuccessListener(result -> handleOAuthResult(result, providerId))
                .addOnFailureListener(e -> showOAuthError(providerId, e));
    }

    private void handleOAuthResult(AuthResult result, String providerId) {
        FirebaseUser user = result.getUser();
        if (user == null) {
            user = auth.getCurrentUser();
        }
        if (user == null) {
            Toast.makeText(this, "OAuth sign-in failed", Toast.LENGTH_LONG).show();
            return;
        }
        saveOAuthUserProfile(user, providerId);
    }

    private void showOAuthError(String providerId, Exception e) {
        String providerLabel = providerId.replace(".com", "").replace(".", " ");
        String errorMessage = "Sign-in failed for " + providerLabel;
        if (e != null && e.getMessage() != null) {
            errorMessage = errorMessage + ": " + e.getMessage();
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void saveOAuthUserProfile(FirebaseUser user, String providerId) {
        String email = user.getEmail();
        String displayName = user.getDisplayName();

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("email", !TextUtils.isEmpty(email) ? email : "");
        userData.put("fullName", !TextUtils.isEmpty(displayName) ? displayName : "");
        String[] parts = !TextUtils.isEmpty(displayName) ? displayName.trim().split("\\s+") : new String[0];
        userData.put("firstName", parts.length > 0 ? parts[0] : "");
        userData.put("role", "user");
        userData.put("phoneVerified", false);
        userData.put("createdAt", System.currentTimeMillis());
        userData.put("authProvider", providerId);
        userData.put("phone", "");
        userData.put("address", "");

        db.collection("users")
                .document(user.getUid())
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, OtpActivity.class);
                    intent.putExtra("role", "user");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "OAuth profile save failed: " + providerId, e);
                    Toast.makeText(this, "Signed in, but profile save failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
