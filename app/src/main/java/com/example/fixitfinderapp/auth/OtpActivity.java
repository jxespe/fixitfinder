package com.example.fixitfinderapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.CheckBox;
import android.view.View;
import android.widget.Toast;
import android.os.CountDownTimer;
import androidx.annotation.NonNull;
import com.example.fixitfinderapp.BaseSwipeActivity;

import com.example.fixitfinderapp.MainTabsActivity;
import com.example.fixitfinderapp.NavigationHelper;
import com.example.fixitfinderapp.R;
import com.example.fixitfinderapp.auth.RegisterActivity;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.example.fixitfinderapp.maps.AddressGeocoder;

import java.util.concurrent.TimeUnit;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
public class OtpActivity extends BaseSwipeActivity {

    public static final String EXTRA_FORGOT_PASSWORD = "forgot_password";
    public static final String EXTRA_RESET_LOGIN_EMAIL = "reset_login_email";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText edtOtp;
    private Button btnVerifyOtp;
    private TextView tvMaskedPhone;
    private TextView tvResend;
    private TextView tvOtpStatus;
    private CheckBox cbSaveDevice;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendingToken;
    private String role = "user";
    private boolean isOauthFlow = false;
    private String fullPhone = "";
    private String fullName = "";
    private String email = "";
    private String address = "";
    private String pendingPassword = "";
    private String serviceCategory = "";
    private CountDownTimer otpTimer;
    private boolean otpExpired = false;
    private boolean forgotPasswordFlow = false;
    private String resetLoginEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edtOtp = findViewById(R.id.edtOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        tvMaskedPhone = findViewById(R.id.tvMaskedPhone);
        tvResend = findViewById(R.id.tvResend);
        tvOtpStatus = findViewById(R.id.tvOtpStatus);
        cbSaveDevice = findViewById(R.id.cbSaveDevice);
        android.widget.ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleBackToRegistration());
        }
        Intent intent = getIntent();
        if (intent != null) {
            String phone = intent.getStringExtra("phone");
            if (!TextUtils.isEmpty(phone)) {
                fullPhone = phone;
            }
            String roleExtra = intent.getStringExtra("role");
            if (!TextUtils.isEmpty(roleExtra)) {
                role = roleExtra;
            }
            isOauthFlow = intent.getBooleanExtra("oauth", false);
            fullName = intent.getStringExtra("fullName");
            email = intent.getStringExtra("email");
            address = intent.getStringExtra("address");
            pendingPassword = intent.getStringExtra("password");
            serviceCategory = intent.getStringExtra("serviceCategory");
            forgotPasswordFlow = intent.getBooleanExtra(EXTRA_FORGOT_PASSWORD, false);
            resetLoginEmail = intent.getStringExtra(EXTRA_RESET_LOGIN_EMAIL);
        }

        if (forgotPasswordFlow) {
            TextView tvTitle = findViewById(R.id.tvTwoStepTitle);
            if (tvTitle != null) {
                tvTitle.setText("Reset your password");
            }
            TextView tvInstr = findViewById(R.id.tvOtpInstructions);
            if (tvInstr != null) {
                tvInstr.setText("Enter the verification code we sent to your phone. "
                        + "After you verify, you can choose a new password.");
            }
            TextView tvSent = findViewById(R.id.tvOtpSentTo);
            if (tvSent != null) {
                tvSent.setText("A code has been sent to");
            }
            if (cbSaveDevice != null) {
                cbSaveDevice.setVisibility(View.GONE);
            }
        }

        if (tvMaskedPhone != null) {
            tvMaskedPhone.setText(maskPhone(fullPhone));
        }

        if (!TextUtils.isEmpty(fullPhone)) {
            startPhoneVerification(fullPhone);
        } else {
            Toast.makeText(this,
                    forgotPasswordFlow ? "Phone number missing. Go back and try again."
                            : "Phone number missing. Please register again.",
                    Toast.LENGTH_LONG).show();
        }

        btnVerifyOtp.setOnClickListener(v -> {
            String code = edtOtp.getText().toString().trim();
            if (TextUtils.isEmpty(code)) {
                edtOtp.setError("OTP is required");
                edtOtp.requestFocus();
                return;
            }
            if (otpExpired) {
                Toast.makeText(this, "OTP expired. Please resend.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (verificationId == null) {
                Toast.makeText(this, "Please request an OTP first", Toast.LENGTH_SHORT).show();
                return;
            }
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
            linkPhoneCredential(credential);
        });

        if (tvResend != null) {
            tvResend.setOnClickListener(v -> {
                if (TextUtils.isEmpty(fullPhone)) {
                    Toast.makeText(this, "Phone number missing.", Toast.LENGTH_SHORT).show();
                    return;
                }
                startPhoneVerification(fullPhone);
            });
        }
    }

    private void startPhoneVerification(String phone) {
        otpExpired = false;
        if (btnVerifyOtp != null) {
            btnVerifyOtp.setEnabled(true);
        }
        updateOtpStatus("Sending code...");
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    linkPhoneCredential(credential);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    Toast.makeText(OtpActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    updateOtpStatus("Unable to send code.");
                }

                @Override
                public void onCodeSent(@NonNull String verificationId,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    OtpActivity.this.verificationId = verificationId;
                    resendingToken = token;
                    Toast.makeText(OtpActivity.this, "OTP sent", Toast.LENGTH_SHORT).show();
                    startOtpTimer(60_000L);
                }
            };

    private void linkPhoneCredential(PhoneAuthCredential credential) {
        if (forgotPasswordFlow) {
            auth.signOut();
        }
        FirebaseUser user = auth.getCurrentUser();
        btnVerifyOtp.setEnabled(false);
        btnVerifyOtp.setText("Verifying...");
        updateOtpStatus("Verifying...");

        if (user == null) {
            auth.signInWithCredential(credential)
                    .addOnCompleteListener(task -> {
                        btnVerifyOtp.setEnabled(true);
                        btnVerifyOtp.setText("Confirm OTP");
                        if (task.isSuccessful() && task.getResult() != null) {
                            FirebaseUser signedIn = task.getResult().getUser();
                            handlePostOtp(signedIn);
                        } else {
                            String message = "OTP verification failed";
                            if (task.getException() != null) {
                                message = task.getException().getMessage();
                            }
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                            updateOtpStatus("Verification failed.");
                        }
                    });
            return;
        }

        user.linkWithCredential(credential)
                .addOnCompleteListener(task -> {
                    btnVerifyOtp.setEnabled(true);
                    btnVerifyOtp.setText("Confirm OTP");

                    if (task.isSuccessful()) {
                        handlePostOtp(user);
                    } else {
                        String message = "OTP verification failed";
                        if (task.getException() != null) {
                            message = task.getException().getMessage();
                        }
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        updateOtpStatus("Verification failed.");
                    }
                });
    }

    private void markPhoneVerified(String uid) {
        String collection = "user".equalsIgnoreCase(role) ? "users" : "providers";
        db.collection(collection)
                .document(uid)
                .update("phoneVerified", true);
    }

    private void saveUserProfileAfterOtp(FirebaseUser user) {
        if (user == null) {
            return;
        }
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        String safeEmail = !TextUtils.isEmpty(email) ? email : user.getEmail();
        if (!TextUtils.isEmpty(safeEmail)) {
            userData.put("email", safeEmail);
        }
        if (!TextUtils.isEmpty(fullName)) {
            userData.put("fullName", fullName);
            userData.put("firstName", extractFirstName(fullName));
        }
        if (!TextUtils.isEmpty(fullPhone)) {
            userData.put("phone", fullPhone);
        }
        if (!TextUtils.isEmpty(address)) {
            userData.put("address", address);
        }
        userData.put("role", "user");
        userData.put("phoneVerified", true);
        userData.put("createdAt", System.currentTimeMillis());
        userData.put("shareLocation", true);
        userData.put("hideProfile", false);
        userData.put("analyticsEnabled", true);
        userData.put("appLockEnabled", false);
        maybePutPhotoUrl(userData, user);

        AccountPublicIdHelper.allocateNextUserId(db, new AccountPublicIdHelper.IdCallback() {
            @Override
            public void onAllocated(@NonNull String accountId) {
                userData.put("accountId", accountId);
                persistUserProfileAfterOtp(user, userData);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Toast.makeText(OtpActivity.this,
                        "Could not assign user ID. Saving profile; contact support if needed.",
                        Toast.LENGTH_LONG).show();
                persistUserProfileAfterOtp(user, userData);
            }
        });
    }

    private void persistUserProfileAfterOtp(FirebaseUser user, Map<String, Object> userData) {
        db.collection("users")
                .document(user.getUid())
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    AddressGeocoder.updateLatLng(OtpActivity.this, "users", user.getUid(), address);
                    Toast.makeText(this, "OTP verified!", Toast.LENGTH_SHORT).show();
                    goToDashboard();
                })
                .addOnFailureListener(e -> {
                    markPhoneVerified(user.getUid());
                    Toast.makeText(this, "OTP verified, but profile save failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    goToDashboard();
                });
    }

    private void handlePostOtp(FirebaseUser user) {
        if (user == null) {
            Toast.makeText(this, "Please register or login again", Toast.LENGTH_LONG).show();
            return;
        }
        if (forgotPasswordFlow) {
            goToResetPasswordAfterOtp(user);
            return;
        }
        if ("user".equalsIgnoreCase(role)) {
            if (shouldLinkEmailPassword()) {
                linkEmailCredentialThen(user, () -> saveUserProfileAfterOtp(user));
            } else {
                saveUserProfileAfterOtp(user);
            }
            return;
        }
        if ("provider".equalsIgnoreCase(role)) {
            if (shouldLinkEmailPassword()) {
                linkEmailCredentialThen(user, () -> saveProviderProfileAfterOtp(user));
            } else {
                saveProviderProfileAfterOtp(user);
            }
            return;
        }
        markPhoneVerified(user.getUid());
        Toast.makeText(this, "OTP verified!", Toast.LENGTH_SHORT).show();
        goToDashboard();
    }

    private void goToResetPasswordAfterOtp(FirebaseUser user) {
        if (user == null || TextUtils.isEmpty(resetLoginEmail)) {
            Toast.makeText(this, "Could not continue password reset.", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, "Verified. Choose your new password.", Toast.LENGTH_SHORT).show();
        Intent i = new Intent(this, ResetPasswordActivity.class);
        i.putExtra(ResetPasswordActivity.EXTRA_ROLE, role);
        i.putExtra(ResetPasswordActivity.EXTRA_RESET_LOGIN_EMAIL, resetLoginEmail);
        startActivity(i);
        finish();
    }

    private boolean shouldLinkEmailPassword() {
        return !TextUtils.isEmpty(email) && !TextUtils.isEmpty(pendingPassword);
    }

    private void linkEmailCredentialThen(FirebaseUser user, Runnable onSuccess) {
        AuthCredential emailCred = EmailAuthProvider.getCredential(email, pendingPassword);
        user.linkWithCredential(emailCred)
                .addOnSuccessListener(unused -> onSuccess.run())
                .addOnFailureListener(e -> {
                    btnVerifyOtp.setEnabled(true);
                    btnVerifyOtp.setText("Confirm OTP");
                    updateOtpStatus("Email linking failed.");
                    Toast.makeText(this, "Email/password already in use. Please use a different email.",
                            Toast.LENGTH_LONG).show();
                });
    }

    private void saveProviderProfileAfterOtp(FirebaseUser user) {
        if (user == null) {
            return;
        }
        String safeCategory = serviceCategory != null ? serviceCategory : "";
        Map<String, Object> providerData = new HashMap<>();
        providerData.put("uid", user.getUid());
        if (!TextUtils.isEmpty(fullName)) {
            providerData.put("fullName", fullName);
            providerData.put("firstName", extractFirstName(fullName));
        }
        String safeEmail = !TextUtils.isEmpty(email) ? email : user.getEmail();
        if (!TextUtils.isEmpty(safeEmail)) {
            providerData.put("email", safeEmail);
        }
        if (!TextUtils.isEmpty(fullPhone)) {
            providerData.put("phone", fullPhone);
        }
        if (!TextUtils.isEmpty(address)) {
            providerData.put("address", address);
        }
        if (!TextUtils.isEmpty(safeCategory)) {
            providerData.put("serviceCategory", safeCategory);
            providerData.put("serviceCategoryLower", safeCategory.toLowerCase(Locale.US).trim());
        }
        providerData.put("role", "provider");
        providerData.put("phoneVerified", true);
        providerData.put("createdAt", System.currentTimeMillis());
        providerData.put("shareLocation", true);
        providerData.put("hideProfile", false);
        providerData.put("analyticsEnabled", true);
        providerData.put("appLockEnabled", false);
        maybePutPhotoUrl(providerData, user);

        AccountPublicIdHelper.allocateNextProviderId(db, safeCategory, new AccountPublicIdHelper.IdCallback() {
            @Override
            public void onAllocated(@NonNull String accountId) {
                providerData.put("accountId", accountId);
                persistProviderProfileAfterOtp(user, providerData);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Toast.makeText(OtpActivity.this,
                        "Could not assign provider ID. Saving profile; contact support if needed.",
                        Toast.LENGTH_LONG).show();
                persistProviderProfileAfterOtp(user, providerData);
            }
        });
    }

    private void persistProviderProfileAfterOtp(FirebaseUser user, Map<String, Object> providerData) {
        db.collection("providers")
                .document(user.getUid())
                .set(providerData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    AddressGeocoder.updateLatLng(OtpActivity.this, "providers", user.getUid(), address);
                    Toast.makeText(this, "OTP verified!", Toast.LENGTH_SHORT).show();
                    goToDashboard();
                })
                .addOnFailureListener(e -> {
                    markPhoneVerified(user.getUid());
                    Toast.makeText(this, "OTP verified, but profile save failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    goToDashboard();
                });
    }

    private String extractFirstName(String fullName) {
        if (TextUtils.isEmpty(fullName)) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : fullName;
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

    private void goToDashboard() {
        if (isOauthFlow && TextUtils.isEmpty(pendingPassword)) {
            Intent intent = new Intent(this, CreatePasswordActivity.class);
            intent.putExtra("role", role);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        com.example.fixitfinderapp.SessionManager.saveRole(this, role);
        Intent intent;
        if ("provider".equalsIgnoreCase(role)) {
            intent = new Intent(this, MainTabsActivity.class);
        } else {
            intent = new Intent(this, MainTabsActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String maskPhone(String phone) {
        if (TextUtils.isEmpty(phone)) {
            return "";
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return phone;
        }
        String lastTwo = digits.substring(digits.length() - 2);
        return "+*******" + lastTwo;
    }

    private void handleBackToRegistration() {
        if (forgotPasswordFlow) {
            finish();
            return;
        }
        if ("provider".equalsIgnoreCase(role)) {
            finish();
            return;
        }
        Intent intent = new Intent(this, RegisterActivity.class);
        if (!TextUtils.isEmpty(fullName)) {
            intent.putExtra("fullName", fullName);
        }
        if (!TextUtils.isEmpty(email)) {
            intent.putExtra("email", email);
        }
        if (!TextUtils.isEmpty(fullPhone)) {
            intent.putExtra("phone", fullPhone);
        }
        if (!TextUtils.isEmpty(address)) {
            intent.putExtra("address", address);
        }
        intent.putExtra("fromOtp", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void startOtpTimer(long totalMs) {
        if (otpTimer != null) {
            otpTimer.cancel();
        }
        otpTimer = new CountDownTimer(totalMs, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000L;
                long minutes = seconds / 60L;
                long remain = seconds % 60L;
                String status = String.format(Locale.US,
                        "Code expires in %02d:%02d", minutes, remain);
                updateOtpStatus(status);
            }

            @Override
            public void onFinish() {
                otpExpired = true;
                verificationId = null;
                updateOtpStatus("Code expired. Please resend.");
                if (btnVerifyOtp != null) {
                    btnVerifyOtp.setEnabled(false);
                }
            }
        };
        otpTimer.start();
    }

    private void updateOtpStatus(String status) {
        if (tvOtpStatus != null) {
            tvOtpStatus.setText(status);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (otpTimer != null) {
            otpTimer.cancel();
        }
    }
}
