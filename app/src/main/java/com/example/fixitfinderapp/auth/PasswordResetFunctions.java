package com.example.fixitfinderapp.auth;

import com.google.firebase.FirebaseApp;
import com.google.firebase.functions.FirebaseFunctions;

/**
 * Region must match {@code onCall({ region: ... })} in {@code functions/index.js} after deploy.
 */
public final class PasswordResetFunctions {

    /** Default for 2nd gen callables when region is set explicitly in code. */
    public static final String REGION = "us-central1";

    private PasswordResetFunctions() {
    }

    public static FirebaseFunctions getInstance() {
        return FirebaseFunctions.getInstance(FirebaseApp.getInstance(), REGION);
    }
}
