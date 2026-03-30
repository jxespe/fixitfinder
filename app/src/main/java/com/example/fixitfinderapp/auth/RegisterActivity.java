package com.example.fixitfinderapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;
import android.util.Log;
import android.content.SharedPreferences;
import android.graphics.Color;
import com.example.fixitfinderapp.BaseSwipeActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.fixitfinderapp.NavigationHelper;
import com.example.fixitfinderapp.R;
import com.example.fixitfinderapp.auth.OtpActivity;
import com.example.fixitfinderapp.SessionManager;
import com.example.fixitfinderapp.MainTabsActivity;
import com.example.fixitfinderapp.maps.AddressGeocoder;
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

public class RegisterActivity extends BaseSwipeActivity {

    private static final int RC_GOOGLE_SIGN_IN = 9001;
    private static final String TAG = "RegisterActivity";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText edtEmail;
    private EditText edtFullName;
    private EditText edtPhone;
    private EditText edtPassword;
    private EditText edtConfirmPassword;
    private EditText edtAddress;
    private android.widget.CheckBox cbTerms;
    private android.widget.TextView tvViewTerms;
    private android.widget.TextView tvPasswordRuleLength;
    private android.widget.TextView tvPasswordRuleLetter;
    private android.widget.TextView tvPasswordRuleNumber;
    private android.widget.TextView tvPasswordRuleMatch;
    private Button btnRegister;
    private Button btnGoogle;
    private Button btnFacebook;
    private Button btnAppleId;
    private ImageView btnPasswordToggle;
    private boolean isPasswordVisible = false;
    private GoogleSignInClient googleSignInClient;
    private Spinner spinnerCountryCode;
    private boolean isFormattingPhone = false;
    private ActivityResultLauncher<Intent> termsLauncher;
    private boolean isOauthFlow = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edtFullName = findViewById(R.id.edtFullName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        edtAddress = findViewById(R.id.edtAddress);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        tvPasswordRuleLength = findViewById(R.id.tvPasswordRuleLength);
        tvPasswordRuleLetter = findViewById(R.id.tvPasswordRuleLetter);
        tvPasswordRuleNumber = findViewById(R.id.tvPasswordRuleNumber);
        tvPasswordRuleMatch = findViewById(R.id.tvPasswordRuleMatch);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnFacebook = findViewById(R.id.btnFacebook);
        btnAppleId = findViewById(R.id.btnAppleId);
        spinnerCountryCode = findViewById(R.id.spinnerCountryCode);
        cbTerms = findViewById(R.id.cbTerms);
        tvViewTerms = findViewById(R.id.tvViewTerms);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        prefillFromOtp(getIntent());

        termsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && cbTerms != null) {
                        cbTerms.setChecked(true);
                        cbTerms.setEnabled(true);
                    }
                }
        );

        setupPhoneFormatting();
        setupPasswordWatchers();

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

        if (cbTerms != null) {
            cbTerms.setChecked(false);
            cbTerms.setEnabled(false);
        }
        if (tvViewTerms != null) {
            tvViewTerms.setOnClickListener(v -> {
                Intent intent = new Intent(RegisterActivity.this, TermsAgreementActivity.class);
                termsLauncher.launch(intent);
            });
        }

        btnRegister.setOnClickListener(v -> {
            String fullName = edtFullName != null ? edtFullName.getText().toString().trim() : "";
            String email = edtEmail.getText().toString().trim();
            String phone = buildPhoneNumber();
            String address = edtAddress.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            String confirmPassword = edtConfirmPassword != null
                    ? edtConfirmPassword.getText().toString().trim()
                    : "";

            // Validation
            if (TextUtils.isEmpty(fullName)) {
                if (edtFullName != null) {
                    edtFullName.setError("Name is required");
                    edtFullName.requestFocus();
                }
                return;
            }

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

            if (TextUtils.isEmpty(confirmPassword)) {
                if (edtConfirmPassword != null) {
                    edtConfirmPassword.setError("Please confirm your password");
                    edtConfirmPassword.requestFocus();
                }
                return;
            }

            if (!password.equals(confirmPassword)) {
                if (edtConfirmPassword != null) {
                    edtConfirmPassword.setError("Passwords do not match");
                    edtConfirmPassword.requestFocus();
                }
                return;
            }

            if (TextUtils.isEmpty(address)) {
                edtAddress.setError("Address is required");
                edtAddress.requestFocus();
                return;
            }

            if (cbTerms == null || !cbTerms.isChecked()) {
                Toast.makeText(RegisterActivity.this, "Please accept the Terms and Conditions", Toast.LENGTH_LONG).show();
                return;
            }

            // Disable button during registration
            btnRegister.setEnabled(false);
            btnRegister.setText("Submitting...");

            if (isOauthFlow) {
                FirebaseUser user = auth.getCurrentUser();
                if (user == null) {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Submit");
                    Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
                    return;
                }
                completeOauthProfile(user, fullName, email, phone, address, password);
                return;
            }

            // Create user with Firebase Authentication
            Intent intent = new Intent(RegisterActivity.this, OtpActivity.class);
            intent.putExtra("phone", phone);
            intent.putExtra("role", "user");
            intent.putExtra("email", email);
            intent.putExtra("password", password);
            intent.putExtra("fullName", fullName);
            intent.putExtra("address", address);
            startActivity(intent);
            finish();
        });
    }

    private void saveUserToFirestore(String userId, String fullName, String email, String phone, String address) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", userId);
        userData.put("email", email);
        userData.put("fullName", fullName);
        userData.put("firstName", extractFirstName(fullName));
        userData.put("phone", phone);
        userData.put("address", address);
        userData.put("shareLocation", true);
        userData.put("hideProfile", false);
        userData.put("analyticsEnabled", true);
        userData.put("appLockEnabled", false);
        userData.put("role", "user");
        userData.put("phoneVerified", false);
        userData.put("createdAt", System.currentTimeMillis());
        maybePutPhotoUrl(userData, FirebaseAuth.getInstance().getCurrentUser());

        // Save to Firestore
        db.collection("users")
                .document(userId)
                .set(userData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Successfully saved to Firestore
                        Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                        AddressGeocoder.updateLatLng(RegisterActivity.this, "users", userId, address);

                        // Proceed to OTP verification
                        Intent intent = new Intent(RegisterActivity.this, OtpActivity.class);
                        intent.putExtra("phone", phone);
                        intent.putExtra("role", "user");
                        intent.putExtra("email", email);
                        intent.putExtra("fullName", fullName);
                        intent.putExtra("address", address);
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
                        intent.putExtra("fullName", fullName);
                        intent.putExtra("address", address);
                        AddressGeocoder.updateLatLng(RegisterActivity.this, "users", userId, address);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    private void completeOauthProfile(FirebaseUser user,
                                      String fullName,
                                      String email,
                                      String phone,
                                      String address,
                                      String password) {
        Intent intent = new Intent(RegisterActivity.this, OtpActivity.class);
        intent.putExtra("phone", phone);
        intent.putExtra("role", "user");
        intent.putExtra("email", email);
        intent.putExtra("password", password);
        intent.putExtra("fullName", fullName);
        intent.putExtra("address", address);
        intent.putExtra("oauth", true);
        startActivity(intent);
        finish();
    }

    private String extractFirstName(String fullName) {
        if (TextUtils.isEmpty(fullName)) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : fullName;
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

    private void setupPasswordWatchers() {
        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                updatePasswordStatus();
            }
        };
        if (edtPassword != null) {
            edtPassword.addTextChangedListener(watcher);
        }
        if (edtConfirmPassword != null) {
            edtConfirmPassword.addTextChangedListener(watcher);
        }
        updatePasswordStatus();
    }

    private void updatePasswordStatus() {
        String password = edtPassword != null ? edtPassword.getText().toString() : "";
        String confirm = edtConfirmPassword != null ? edtConfirmPassword.getText().toString() : "";
        boolean hasLength = password.length() >= 8;
        boolean hasLetter = password.matches(".*[A-Za-z].*");
        boolean hasNumber = password.matches(".*\\d.*");
        boolean match = !TextUtils.isEmpty(confirm) && password.equals(confirm);

        setRuleColor(tvPasswordRuleLength, hasLength);
        setRuleColor(tvPasswordRuleLetter, hasLetter);
        setRuleColor(tvPasswordRuleNumber, hasNumber);
        if (tvPasswordRuleMatch != null) {
            tvPasswordRuleMatch.setText(match ? "• Passwords match" : "• Passwords do not match");
            setRuleColor(tvPasswordRuleMatch, match);
        }
    }

    private void setRuleColor(android.widget.TextView view, boolean ok) {
        if (view == null) {
            return;
        }
        view.setTextColor(ok ? Color.parseColor("#1B9C85") : Color.parseColor("#F44336"));
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
        String stripped = local.replaceAll("\\D", "");
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

    private void setupPhoneFormatting() {
        if (edtPhone == null) {
            return;
        }
        edtPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormattingPhone) {
                    return;
                }
                isFormattingPhone = true;
                String digits = s.toString().replaceAll("\\D", "");
                if (digits.startsWith("0")) {
                    digits = digits.substring(1);
                }
                if (digits.length() > 10) {
                    digits = digits.substring(0, 10);
                }
                String formatted = formatLocalPhone(digits);
                if (!formatted.equals(s.toString())) {
                    edtPhone.setText(formatted);
                    edtPhone.setSelection(formatted.length());
                }
                isFormattingPhone = false;
            }
        });
    }

    private String formatLocalPhone(String digits) {
        if (TextUtils.isEmpty(digits)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int len = digits.length();
        sb.append(digits, 0, Math.min(3, len));
        if (len > 3) {
            sb.append(" ");
            sb.append(digits, 3, Math.min(6, len));
        }
        if (len > 6) {
            sb.append(" ");
            sb.append(digits, 6, len);
        }
        return sb.toString();
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

        saveLastOAuthEmail(email);
        prefillOauthProfile(displayName, email);
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

        saveLastOAuthEmail(email);
        attemptExistingOauthLogin(user, displayName, email);
    }

    private void attemptExistingOauthLogin(FirebaseUser user, String displayName, String email) {
        if (user == null) {
            prefillOauthProfile(displayName, email);
            return;
        }
        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean phoneVerified = doc.getBoolean("phoneVerified");
                        boolean verified = phoneVerified != null && phoneVerified;
                        if (verified) {
                            SessionManager.saveRole(this, "user");
                            Intent intent = new Intent(this, MainTabsActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                            return;
                        }
                    }
                    prefillOauthProfile(displayName, email);
                })
                .addOnFailureListener(e -> prefillOauthProfile(displayName, email));
    }

    private void saveLastOAuthEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return;
        }
        SharedPreferences prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
        prefs.edit().putString("last_oauth_email", email).apply();
    }

    private void maybePutPhotoUrl(Map<String, Object> userData, FirebaseUser user) {
        if (user == null || userData == null || user.getPhotoUrl() == null) {
            return;
        }
        String photoUrl = user.getPhotoUrl().toString();
        if (!TextUtils.isEmpty(photoUrl)) {
            userData.put("photoUrl", photoUrl);
        }
    }

    private void prefillOauthProfile(String displayName, String email) {
        isOauthFlow = true;
        if (edtFullName != null && !TextUtils.isEmpty(displayName)) {
            edtFullName.setText(displayName);
        }
        if (edtEmail != null && !TextUtils.isEmpty(email)) {
            edtEmail.setText(email);
            edtEmail.setEnabled(false);
        }
        Toast.makeText(this, "Please complete your profile details.", Toast.LENGTH_LONG).show();
    }

    private void prefillFromOtp(Intent intent) {
        if (intent == null) {
            return;
        }
        boolean fromOtp = intent.getBooleanExtra("fromOtp", false);
        if (!fromOtp) {
            return;
        }
        String fullName = intent.getStringExtra("fullName");
        String email = intent.getStringExtra("email");
        String phone = intent.getStringExtra("phone");
        String address = intent.getStringExtra("address");
        if (!TextUtils.isEmpty(fullName) && edtFullName != null) {
            edtFullName.setText(fullName);
        }
        if (!TextUtils.isEmpty(email) && edtEmail != null) {
            edtEmail.setEnabled(true);
            edtEmail.setText(email);
        }
        if (!TextUtils.isEmpty(address) && edtAddress != null) {
            edtAddress.setText(address);
        }
        if (!TextUtils.isEmpty(phone) && edtPhone != null) {
            edtPhone.setText(formatLocalPhone(stripCountryCode(phone)));
        }
    }

    private String stripCountryCode(String phone) {
        if (TextUtils.isEmpty(phone)) {
            return "";
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("63") && digits.length() > 2) {
            digits = digits.substring(2);
        }
        if (digits.startsWith("0") && digits.length() > 1) {
            digits = digits.substring(1);
        }
        return digits;
    }

    
}
