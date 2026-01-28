package com.example.fixitfinderapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitfinderapp.auth.LoginActivity;
import com.example.fixitfinderapp.auth.OtpActivity;
import com.example.fixitfinderapp.auth.ProviderLoginActivity;
import com.example.fixitfinderapp.auth.ProviderRegisterActivity;
import com.example.fixitfinderapp.auth.RegisterActivity;
import com.example.fixitfinderapp.maps.MapsActivity;
import com.example.fixitfinderapp.payment.PaymentActivity;

public class TestMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_menu);

        LinearLayout container = findViewById(R.id.layoutTestContainer);

        addActivityButton(container, "User Login", new Intent(this, LoginActivity.class));
        addActivityButton(container, "User Register", new Intent(this, RegisterActivity.class));
        addActivityButton(container, "Provider Login", new Intent(this, ProviderLoginActivity.class));
        addActivityButton(container, "Provider Register", new Intent(this, ProviderRegisterActivity.class));
        addActivityButton(container, "OTP", new Intent(this, OtpActivity.class));
        addActivityButton(container, "User Home", new Intent(this, UserDashboardActivity.class));
        addActivityButton(container, "Provider Home", new Intent(this, DashboardActivity.class));
        addActivityButton(container, "User Settings", new Intent(this, UserSettingsActivity.class));
        addActivityButton(container, "Privacy Report", new Intent(this, PrivacyReportActivity.class));
        addActivityButton(container, "Service List", new Intent(this, ServiceListActivity.class));
        addActivityButton(container, "Maps", new Intent(this, MapsActivity.class));
        addActivityButton(container, "Payment", new Intent(this, PaymentActivity.class));

        addPreviewButton(container, "Booking", R.layout.activity_booking);
        addPreviewButton(container, "History", R.layout.activity_history);
        addPreviewButton(container, "Messages", R.layout.activity_messages);
        addPreviewButton(container, "Provider Earnings", R.layout.activity_provider_earnings);
        addPreviewButton(container, "Provider Settings", R.layout.activity_provider_settings);
        addPreviewButton(container, "Services", R.layout.activity_services);
        addPreviewButton(container, "Subscription", R.layout.activity_subscription);
        addPreviewButton(container, "Terms Agreement", R.layout.activity_terms_agreement);
        addPreviewButton(container, "Forgot Password", R.layout.activity_forgot_password);
        addPreviewButton(container, "Service Categories Grid", R.layout.activity_user_homepage);
        addPreviewButton(container, "Provider Homepage", R.layout.activity_provider_homepage);
    }

    private void addActivityButton(LinearLayout container, String title, Intent intent) {
        Button button = buildButton(title);
        button.setOnClickListener(v -> startActivity(intent));
        container.addView(button);
    }

    private void addPreviewButton(LinearLayout container, String title, int layoutResId) {
        Button button = buildButton(title);
        button.setOnClickListener(v -> {
            Intent intent = new Intent(this, PreviewActivity.class);
            intent.putExtra(PreviewActivity.EXTRA_LAYOUT_RES_ID, layoutResId);
            startActivity(intent);
        });
        container.addView(button);
    }

    private Button buildButton(String title) {
        Button button = new Button(this);
        button.setText(title);
        button.setAllCaps(false);
        button.setBackgroundResource(R.drawable.button_rounded_primary);
        button.setTextColor(ContextCompat.getColor(this, R.color.color_background));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = getResources().getDimensionPixelSize(R.dimen.test_menu_spacing);
        button.setLayoutParams(params);
        return button;
    }
}
