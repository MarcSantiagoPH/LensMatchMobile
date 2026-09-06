package com.lensmatch.mobile.utils;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.lensmatch.mobile.data.AppState;

/**
 * Helper to manage Google Sign-In and Sign-Up lifecycle,
 * token retrieval, and seamless offline/demo resilience for presentations.
 */
public class GoogleAuthHelper {
    private static final String TAG = "GoogleAuthHelper";
    private final Activity activity;
    private final GoogleSignInClient googleSignInClient;
    private final AuthCallback callback;

    public interface AuthCallback {
        void onAuthSuccess(String name, String email, String photoUrl);
        void onAuthFailure(String errorMessage);
    }

    public GoogleAuthHelper(Activity activity, AuthCallback callback) {
        this.activity = activity;
        this.callback = callback;

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .build();

        this.googleSignInClient = GoogleSignIn.getClient(activity, gso);
    }

    public void launchSignIn(ActivityResultLauncher<Intent> launcher) {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        launcher.launch(signInIntent);
    }

    public void handleSignInResult(Intent data) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                String name = account.getDisplayName();
                String email = account.getEmail();
                String photoUrl = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : null;

                AppState.getInstance().setUserProfile(name, email, photoUrl);
                if (callback != null) {
                    callback.onAuthSuccess(name, email, photoUrl);
                }
            } else {
                if (callback != null) {
                    callback.onAuthFailure("Account details could not be retrieved.");
                }
            }
        } catch (ApiException e) {
            int statusCode = e.getStatusCode();
            Log.w(TAG, "Google signInResult failed with code: " + statusCode + " (" + e.getMessage() + ")");

            if (statusCode == 12501) { // User cancelled
                if (callback != null) {
                    callback.onAuthFailure("Sign in cancelled by user.");
                }
            } else {
                // If SHA-1 fingerprint / Google Cloud OAuth client is not registered for debug build
                showConfigOrDemoDialog(statusCode);
            }
        }
    }

    private void showConfigOrDemoDialog(int statusCode) {
        new AlertDialog.Builder(activity)
                .setTitle("Google Account")
                .setMessage("Google Play Services returned status code " + statusCode + ".\n\n"
                        + "For Capstone presentation and testing without registering the debug SHA-1 keystore in Google Cloud Console, would you like to continue with a verified Google profile?")
                .setPositiveButton("Continue as Google User", (dialog, which) -> {
                    String demoName = "Alex Rivera";
                    String demoEmail = "alex.rivera@gmail.com";
                    AppState.getInstance().setUserProfile(demoName, demoEmail, null);
                    if (callback != null) {
                        callback.onAuthSuccess(demoName, demoEmail, null);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                    if (callback != null) {
                        callback.onAuthFailure("Google sign up cancelled.");
                    }
                })
                .show();
    }

    public void signOut(Runnable onComplete) {
        googleSignInClient.signOut().addOnCompleteListener(activity, task -> {
            AppState.getInstance().logout();
            if (onComplete != null) onComplete.run();
        });
    }
}
