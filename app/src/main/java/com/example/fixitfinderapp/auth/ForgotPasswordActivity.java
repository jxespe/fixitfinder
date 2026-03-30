package com.example.fixitfinderapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fixitfinderapp.BaseSwipeActivity;
import com.example.fixitfinderapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.HashMap;
import java.util.Map;

public class ForgotPasswordActivity extends BaseSwipeActivity {

    public static final String EXTRA_ROLE_FILTER = "role_filter";

    private EditText edtEmailReset;
    private EditText edtPhoneReset;
    private RadioGroup rgContactMethod;
    private View layoutEmailSection;
    private View layoutMobileSection;
    private boolean isFormattingPhone;
    private String roleFilter = "user";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        Intent in = getIntent();
        if (in != null) {
            String rf = in.getStringExtra(EXTRA_ROLE_FILTER);
            if (!TextUtils.isEmpty(rf)) {
                roleFilter = rf;
            }
        }

        FirebaseAuth.getInstance().signOut();

        edtEmailReset = findViewById(R.id.edtEmailReset);
        edtPhoneReset = findViewById(R.id.edtPhoneReset);
        rgContactMethod = findViewById(R.id.rgContactMethod);
        layoutEmailSection = findViewById(R.id.layoutEmailSection);
        layoutMobileSection = findViewById(R.id.layoutMobileSection);
        Button btnResetPassword = findViewById(R.id.btnResetPassword);
        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvDescription = findViewById(R.id.tvDescription);

        if (tvDescription != null) {
            tvDescription.setText("Choose email or mobile number. We will send a verification code "
                    + "to the phone number on your account.");
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
        TextView tvSignIn = findViewById(R.id.tvSignIn);
        if (tvSignIn != null) {
            tvSignIn.setOnClickListener(v -> {
                Class<?> dest = "provider".equalsIgnoreCase(roleFilter)
                        ? ProviderLoginActivity.class
                        : LoginActivity.class;
                startActivity(new Intent(this, dest));
                finish();
            });
        }

        if (rgContactMethod != null) {
            rgContactMethod.setOnCheckedChangeListener((group, checkedId) -> applyContactMode(checkedId));
            applyContactMode(rgContactMethod.getCheckedRadioButtonId());
        }

        setupPhoneFormatting();

        if (btnResetPassword != null) {
            btnResetPassword.setOnClickListener(v -> requestReset());
        }
    }

    private void applyContactMode(int checkedId) {
        boolean emailMode = checkedId == R.id.rbEmail;
        if (layoutEmailSection != null) {
            layoutEmailSection.setVisibility(emailMode ? View.VISIBLE : View.GONE);
        }
        if (layoutMobileSection != null) {
            layoutMobileSection.setVisibility(emailMode ? View.GONE : View.VISIBLE);
        }
        if (emailMode) {
            if (edtEmailReset != null) {
                edtEmailReset.requestFocus();
            }
        } else {
            if (edtPhoneReset != null) {
                edtPhoneReset.requestFocus();
            }
        }
    }

    private boolean isEmailMode() {
        return rgContactMethod != null && rgContactMethod.getCheckedRadioButtonId() == R.id.rbEmail;
    }

    private void setupPhoneFormatting() {
        if (edtPhoneReset == null) {
            return;
        }
        edtPhoneReset.addTextChangedListener(new TextWatcher() {
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
                    edtPhoneReset.setText(formatted);
                    edtPhoneReset.setSelection(formatted.length());
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

    private void requestReset() {
        String identifier;
        if (isEmailMode()) {
            String raw = edtEmailReset != null ? edtEmailReset.getText().toString().trim() : "";
            if (TextUtils.isEmpty(raw)) {
                if (edtEmailReset != null) {
                    edtEmailReset.setError("Email is required");
                    edtEmailReset.requestFocus();
                }
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(raw).matches()) {
                if (edtEmailReset != null) {
                    edtEmailReset.setError("Please enter a valid email");
                    edtEmailReset.requestFocus();
                }
                return;
            }
            identifier = raw.toLowerCase();
        } else {
            identifier = buildPhoneNumber();
            if (TextUtils.isEmpty(identifier)) {
                return;
            }
        }

        Button btn = findViewById(R.id.btnResetPassword);
        if (btn != null) {
            btn.setEnabled(false);
            btn.setText("Checking…");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("identifier", identifier);
        data.put("roleFilter", roleFilter);

        PasswordResetFunctions.getInstance()
                .getHttpsCallable("preparePasswordReset")
                .call(data)
                .addOnCompleteListener(task -> {
                    if (btn != null) {
                        btn.setEnabled(true);
                        btn.setText("Continue");
                    }
                    if (!task.isSuccessful()) {
                        Exception e = task.getException();
                        String msg = "Something went wrong. Try again.";
                        if (e instanceof FirebaseFunctionsException) {
                            FirebaseFunctionsException fe = (FirebaseFunctionsException) e;
                            if (fe.getCode() == FirebaseFunctionsException.Code.NOT_FOUND) {
                                msg = "No account matches that email or phone number.";
                            } else if (fe.getCode() == FirebaseFunctionsException.Code.FAILED_PRECONDITION) {
                                msg = fe.getMessage() != null ? fe.getMessage() : msg;
                            } else if (fe.getCode() == FirebaseFunctionsException.Code.INVALID_ARGUMENT) {
                                msg = fe.getMessage() != null ? fe.getMessage() : msg;
                            } else if (fe.getCode() == FirebaseFunctionsException.Code.INTERNAL) {
                                msg = fe.getMessage() != null ? fe.getMessage() : msg;
                            } else if (fe.getMessage() != null) {
                                msg = fe.getMessage();
                            }
                        } else if (e != null && e.getMessage() != null) {
                            msg = e.getMessage();
                        }
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        return;
                    }
                    HttpsCallableResult result = task.getResult();
                    if (result == null || result.getData() == null) {
                        Toast.makeText(this, "Unexpected response.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> payload = (Map<String, Object>) result.getData();
                    String phoneE164 = payload.get("phoneE164") instanceof String
                            ? (String) payload.get("phoneE164") : "";
                    String email = payload.get("email") instanceof String
                            ? (String) payload.get("email") : "";
                    if (TextUtils.isEmpty(phoneE164)) {
                        Toast.makeText(this, "Could not send code.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    Intent intent = new Intent(this, OtpActivity.class);
                    intent.putExtra("phone", phoneE164);
                    intent.putExtra("role", roleFilter);
                    intent.putExtra(OtpActivity.EXTRA_FORGOT_PASSWORD, true);
                    intent.putExtra(OtpActivity.EXTRA_RESET_LOGIN_EMAIL, email);
                    startActivity(intent);
                    finish();
                });
    }

    private String buildPhoneNumber() {
        String local = edtPhoneReset != null ? edtPhoneReset.getText().toString().trim() : "";
        if (TextUtils.isEmpty(local)) {
            if (edtPhoneReset != null) {
                edtPhoneReset.setError("Mobile number is required");
                edtPhoneReset.requestFocus();
            }
            return "";
        }
        String stripped = local.replaceAll("\\D", "");
        if (stripped.startsWith("0")) {
            stripped = stripped.substring(1);
        }
        if (stripped.length() < 10) {
            if (edtPhoneReset != null) {
                edtPhoneReset.setError("Enter a valid 10-digit mobile number");
                edtPhoneReset.requestFocus();
            }
            return "";
        }
        return "+63" + stripped;
    }
}
