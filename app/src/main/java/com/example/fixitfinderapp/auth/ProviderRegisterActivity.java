package com.example.fixitfinderapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.graphics.Color;
import com.example.fixitfinderapp.BaseSwipeActivity;

import com.example.fixitfinderapp.R;
public class ProviderRegisterActivity extends BaseSwipeActivity {

    private boolean isPasswordVisible = false;
    private Spinner spinnerServiceCategory;
    private Spinner spinnerCountryCode;
    private boolean isFormattingPhone = false;
    private EditText edtConfirmPassword;
    private android.widget.TextView tvPasswordRuleLength;
    private android.widget.TextView tvPasswordRuleLetter;
    private android.widget.TextView tvPasswordRuleNumber;
    private android.widget.TextView tvPasswordRuleMatch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_register);

        EditText edtFullName = findViewById(R.id.edtFullName);
        EditText edtEmail = findViewById(R.id.edtEmail);
        EditText edtPhone = findViewById(R.id.edtPhone);
        EditText edtAddress = findViewById(R.id.edtAddress);
        EditText edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        tvPasswordRuleLength = findViewById(R.id.tvPasswordRuleLength);
        tvPasswordRuleLetter = findViewById(R.id.tvPasswordRuleLetter);
        tvPasswordRuleNumber = findViewById(R.id.tvPasswordRuleNumber);
        tvPasswordRuleMatch = findViewById(R.id.tvPasswordRuleMatch);
        spinnerServiceCategory = findViewById(R.id.spinnerServiceCategory);
        spinnerCountryCode = findViewById(R.id.spinnerCountryCode);
        Button btnRegister = findViewById(R.id.btnRegister);
        ImageView btnPasswordToggle = findViewById(R.id.btnPasswordToggle);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        setupPhoneFormatting(edtPhone);
        setupPasswordWatchers(edtPassword);

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

        btnRegister.setOnClickListener(v -> {
            String fullName = edtFullName.getText().toString().trim();
            String email = edtEmail.getText().toString().trim();
            String phone = buildPhoneNumber(edtPhone);
            String address = edtAddress.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            String confirmPassword = edtConfirmPassword != null
                    ? edtConfirmPassword.getText().toString().trim()
                    : "";
            String category = getSelectedCategory();

            if (TextUtils.isEmpty(fullName)) {
                edtFullName.setError("Full name is required");
                edtFullName.requestFocus();
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

            if (TextUtils.isEmpty(phone)) {
                edtPhone.setError("Phone number is required");
                edtPhone.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(address)) {
                edtAddress.setError("Address is required");
                edtAddress.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(category)) {
                Toast.makeText(this, "Please select a service category", Toast.LENGTH_SHORT).show();
                return;
            }

            btnRegister.setEnabled(false);
            btnRegister.setText("Submitting...");

            Intent intent = new Intent(this, OtpActivity.class);
            intent.putExtra("phone", phone);
            intent.putExtra("role", "provider");
            intent.putExtra("email", email);
            intent.putExtra("password", password);
            intent.putExtra("fullName", fullName);
            intent.putExtra("address", address);
            intent.putExtra("serviceCategory", category);
            startActivity(intent);
            finish();
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

    private void setupPasswordWatchers(EditText edtPassword) {
        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                updatePasswordStatus(edtPassword);
            }
        };
        if (edtPassword != null) {
            edtPassword.addTextChangedListener(watcher);
        }
        if (edtConfirmPassword != null) {
            edtConfirmPassword.addTextChangedListener(watcher);
        }
        updatePasswordStatus(edtPassword);
    }

    private void updatePasswordStatus(EditText edtPassword) {
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

    private String getSelectedCategory() {
        if (spinnerServiceCategory == null || spinnerServiceCategory.getSelectedItem() == null) {
            return "";
        }
        String selected = spinnerServiceCategory.getSelectedItem().toString().trim();
        if ("Select service category".equalsIgnoreCase(selected)) {
            return "";
        }
        return selected;
    }

    private String buildPhoneNumber(EditText edtPhone) {
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

    private void setupPhoneFormatting(EditText edtPhone) {
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
