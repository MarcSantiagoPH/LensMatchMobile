package com.lensmatch.mobile.utils;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.lensmatch.mobile.data.AppState;

/**
 * Helper to manage Google Sign-In and Firebase Authentication lifecycle.
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

        int webClientIdRes = activity.getResources().getIdentifier("default_web_client_id", "string", activity.getPackageName());
        if (webClientIdRes != 0) {
            String webClientId = activity.getString(webClientIdRes);
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .requestProfile()
                    .build();
            this.googleSignInClient = GoogleSignIn.getClient(activity, gso);
        } else {
            this.googleSignInClient = null;
            Log.e(TAG, "default_web_client_id resource not found in google-services.json.");
        }
    }

    public void launchSignIn(ActivityResultLauncher<Intent> launcher) {
        if (googleSignInClient == null) {
            if (callback != null) {
                callback.onAuthFailure("Google Sign-In requires default_web_client_id in google-services.json.");
            }
            return;
        }
        Intent signInIntent = googleSignInClient.getSignInIntent();
        launcher.launch(signInIntent);
    }

    public void handleSignInResult(Intent data) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                String idToken = account.getIdToken();
                if (idToken != null && !idToken.isEmpty()) {
                    AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
                    FirebaseAuth.getInstance().signInWithCredential(credential)
                            .addOnCompleteListener(activity, authTask -> {
                                if (authTask.isSuccessful()) {
                                    FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
                                    String finalName = (fbUser != null && fbUser.getDisplayName() != null && !fbUser.getDisplayName().isEmpty())
                                            ? fbUser.getDisplayName() : account.getDisplayName();
                                    String finalEmail = (fbUser != null && fbUser.getEmail() != null && !fbUser.getEmail().isEmpty())
                                            ? fbUser.getEmail() : account.getEmail();
                                    String photoUrl = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : null;

                                    AppState.getInstance().setUserProfile(finalName, finalEmail, photoUrl);
                                    if (callback != null) {
                                        callback.onAuthSuccess(finalName, finalEmail, photoUrl);
                                    }
                                } else {
                                    String msg = authTask.getException() != null ? authTask.getException().getMessage() : "Firebase sign-in failed.";
                                    if (callback != null) callback.onAuthFailure(msg);
                                }
                            });
                } else {
                    if (callback != null) {
                        callback.onAuthFailure("Google ID token unavailable for Firebase Authentication.");
                    }
                }
            } else {
                if (callback != null) {
                    callback.onAuthFailure("Account details could not be retrieved.");
                }
            }
        } catch (ApiException e) {
            int statusCode = e.getStatusCode();
            Log.w(TAG, "Google signInResult failed with code: " + statusCode + " (" + e.getMessage() + ")");
            String errorMsg = (statusCode == 12501) ? "Sign in cancelled by user." : "Google Sign-In failed (error code " + statusCode + ").";
            if (callback != null) {
                callback.onAuthFailure(errorMsg);
            }
        }
    }

    public void signOut(Runnable onComplete) {
        FirebaseAuth.getInstance().signOut();
        if (googleSignInClient != null) {
            googleSignInClient.signOut().addOnCompleteListener(activity, task -> {
                AppState.getInstance().logout();
                if (onComplete != null) onComplete.run();
            });
        } else {
            AppState.getInstance().logout();
            if (onComplete != null) onComplete.run();
        }
    }
}
