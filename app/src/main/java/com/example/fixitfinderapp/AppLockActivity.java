package com.example.fixitfinderapp;

import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AppLockActivity extends AppCompatActivity {

    private static final int REQ_LOCK = 2001;
    private static final String PREF_PRIVACY = "privacy_settings";
    private static final String KEY_APP_LOCK = "app_lock";
    private static final String KEY_LAST_UNLOCK = "last_unlock";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean enabled = getSharedPreferences(PREF_PRIVACY, MODE_PRIVATE)
                .getBoolean(KEY_APP_LOCK, false);
        if (!enabled) {
            finish();
            return;
        }

        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (km == null || !km.isKeyguardSecure()) {
            Toast.makeText(this, "Device lock not set up.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Intent intent = km.createConfirmDeviceCredentialIntent(
                "Unlock FixIt Finder",
                "Verify to continue");
        if (intent == null) {
            finish();
            return;
        }
        startActivityForResult(intent, REQ_LOCK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_LOCK) {
            if (resultCode == RESULT_OK) {
                getSharedPreferences(PREF_PRIVACY, MODE_PRIVATE)
                        .edit()
                        .putLong(KEY_LAST_UNLOCK, System.currentTimeMillis())
                        .apply();
                finish();
            } else {
                finishAffinity();
            }
        }
    }
}
