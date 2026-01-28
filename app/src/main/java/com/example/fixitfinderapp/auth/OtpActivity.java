package com.example.fixitfinderapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
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
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendingToken;
    private String role = "user";

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

        Intent intent = getIntent();
        if (intent != null) {
            String phone = intent.getStringExtra("phone");
            if (!TextUtils.isEmpty(phone)) {
                edtPhone.setText(phone);
            }
            String roleExtra = intent.getStringExtra("role");
            if (!TextUtils.isEmpty(roleExtra)) {
                role = roleExtra;
            }
        }

        btnSendOtp.setOnClickListener(v -> {
            String phone = edtPhone.getText().toString().trim();
            if (TextUtils.isEmpty(phone)) {
                edtPhone.setError("Phone number is required");
                edtPhone.requestFocus();
                return;
            }
            if (!phone.startsWith("+")) {
                edtPhone.setError("Use international format, e.g. +63 9xx xxx xxxx");
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
        db.collection("users")
                .document(uid)
                .update("phoneVerified", true);
    }

    private void goToDashboard() {
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
}
