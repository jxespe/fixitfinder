package com.example.fixitfinderapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitfinderapp.DashboardActivity;
import com.example.fixitfinderapp.R;
import com.example.fixitfinderapp.UserDashboardActivity;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

public class OtpActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText edtPhone;
    private EditText edtOtp;
    private Button btnSendOtp;
    private Button btnVerifyOtp;
    private Spinner spinnerCountryCode;
    private boolean isFormattingPhone = false;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendingToken;
    private String role = "user";
    private boolean isOauthFlow = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edtPhone = findViewById(R.id.edtPhone);
        edtOtp = findViewById(R.id.edtOtp);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        spinnerCountryCode = findViewById(R.id.spinnerCountryCode);
        setupPhoneFormatting();

        Intent intent = getIntent();
        if (intent != null) {
            String phone = intent.getStringExtra("phone");
            if (!TextUtils.isEmpty(phone)) {
                setLocalPhoneFromFull(phone);
            }
            String roleExtra = intent.getStringExtra("role");
            if (!TextUtils.isEmpty(roleExtra)) {
                role = roleExtra;
            }
            isOauthFlow = intent.getBooleanExtra("oauth", false);
        }

        btnSendOtp.setOnClickListener(v -> {
            String phone = buildPhoneNumber();
            if (TextUtils.isEmpty(phone)) {
                edtPhone.setError("Phone number is required");
                edtPhone.requestFocus();
                return;
            }
            startPhoneVerification(phone);
        });

        btnVerifyOtp.setOnClickListener(v -> {
            String code = edtOtp.getText().toString().trim();
            if (TextUtils.isEmpty(code)) {
                edtOtp.setError("OTP is required");
                edtOtp.requestFocus();
                return;
            }
            if (verificationId == null) {
                Toast.makeText(this, "Please request an OTP first", Toast.LENGTH_SHORT).show();
                return;
            }
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
            linkPhoneCredential(credential);
        });
    }

    private void startPhoneVerification(String phone) {
        btnSendOtp.setEnabled(false);
        btnSendOtp.setText("Sending...");

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
                    btnSendOtp.setEnabled(true);
                    btnSendOtp.setText("Send OTP");
                    Toast.makeText(OtpActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCodeSent(@NonNull String verificationId,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    OtpActivity.this.verificationId = verificationId;
                    resendingToken = token;
                    btnSendOtp.setEnabled(true);
                    btnSendOtp.setText("Resend OTP");
                    Toast.makeText(OtpActivity.this, "OTP sent", Toast.LENGTH_SHORT).show();
                }
            };

    private void linkPhoneCredential(PhoneAuthCredential credential) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please register or login again", Toast.LENGTH_LONG).show();
            return;
        }

        btnVerifyOtp.setEnabled(false);
        btnVerifyOtp.setText("Verifying...");

        user.linkWithCredential(credential)
                .addOnCompleteListener(task -> {
                    btnVerifyOtp.setEnabled(true);
                    btnVerifyOtp.setText("Confirm OTP");

                    if (task.isSuccessful()) {
                        markPhoneVerified(user.getUid());
                        Toast.makeText(this, "OTP verified!", Toast.LENGTH_SHORT).show();
                        goToDashboard();
                    } else {
                        String message = "OTP verification failed";
                        if (task.getException() != null) {
                            message = task.getException().getMessage();
                        }
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void markPhoneVerified(String uid) {
        String collection = "user".equalsIgnoreCase(role) ? "users" : "providers";
        db.collection(collection)
                .document(uid)
                .update("phoneVerified", true);
    }

    private void goToDashboard() {
        if (isOauthFlow) {
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
            intent = new Intent(this, DashboardActivity.class);
        } else {
            intent = new Intent(this, UserDashboardActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String buildPhoneNumber() {
        String local = edtPhone != null ? edtPhone.getText().toString().trim() : "";
        if (TextUtils.isEmpty(local)) {
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

    private void setLocalPhoneFromFull(String fullPhone) {
        if (edtPhone == null) {
            return;
        }
        String trimmed = fullPhone == null ? "" : fullPhone.trim();
        if (TextUtils.isEmpty(trimmed)) {
            return;
        }
        String withoutSpaces = trimmed.replaceAll("\\s+", "");
        String code = "+63";
        if (spinnerCountryCode != null && spinnerCountryCode.getSelectedItem() != null) {
            String selected = spinnerCountryCode.getSelectedItem().toString().trim();
            if (!TextUtils.isEmpty(selected)) {
                code = selected;
            }
        }
        if (withoutSpaces.startsWith(code)) {
            withoutSpaces = withoutSpaces.substring(code.length());
        }
        if (withoutSpaces.startsWith("+")) {
            withoutSpaces = withoutSpaces.substring(1);
        }
        if (withoutSpaces.startsWith("0")) {
            withoutSpaces = withoutSpaces.substring(1);
        }
        String digits = withoutSpaces.replaceAll("\\D", "");
        if (digits.length() > 10) {
            digits = digits.substring(0, 10);
        }
        edtPhone.setText(formatLocalPhone(digits));
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
}
