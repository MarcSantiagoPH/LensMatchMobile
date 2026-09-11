package com.lensmatch.mobile.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.AppState;
import com.lensmatch.mobile.ui.MainActivity;
import com.lensmatch.mobile.utils.GoogleAuthHelper;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText etEmail, etPassword;
    private GoogleAuthHelper googleAuthHelper;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private MaterialButton btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already logged in via Firebase
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
            String name = current.getDisplayName() != null && !current.getDisplayName().isEmpty() ? current.getDisplayName() : "User";
            AppState.getInstance().setUserProfile(name, current.getEmail(), null);
            handleLogin();
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.et_login_email);
        etPassword = findViewById(R.id.et_login_password);
        TextView btnForgotPassword = findViewById(R.id.btn_forgot_password);
        btnLogin = findViewById(R.id.btn_login);
        MaterialButton btnGoogleLogin = findViewById(R.id.btn_google_login);
        TextView btnGotoSignup = findViewById(R.id.btn_goto_signup);

        // Google Sign-In Setup
        googleAuthHelper = new GoogleAuthHelper(this, new GoogleAuthHelper.AuthCallback() {
            @Override
            public void onAuthSuccess(String name, String email, String photoUrl) {
                Toast.makeText(LoginActivity.this, "Welcome back, " + name + "!", Toast.LENGTH_SHORT).show();
                handleLogin();
            }

            @Override
            public void onAuthFailure(String errorMessage) {
                if (errorMessage != null && !errorMessage.isEmpty()) {
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getData() != null) {
                        googleAuthHelper.handleSignInResult(result.getData());
                    }
                }
        );

        btnGoogleLogin.setOnClickListener(v -> googleAuthHelper.launchSignIn(googleSignInLauncher));

        btnForgotPassword.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            if (email.isEmpty()) {
                etEmail.setError("Please enter your email to reset password");
                etEmail.requestFocus();
                return;
            }

            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Password reset email sent to " + email, Toast.LENGTH_LONG).show();
                        } else {
                            String error = task.getException() != null ? task.getException().getMessage() : "Failed to send reset email.";
                            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            if (email.isEmpty()) {
                etEmail.setError("Email is required");
                etEmail.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                etPassword.setError("Password is required");
                etPassword.requestFocus();
                return;
            }

            btnLogin.setEnabled(false);
            btnLogin.setText("Logging in...");

            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Log In");

                        if (task.isSuccessful()) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            String name = (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                                    ? user.getDisplayName()
                                    : (email.contains("@") ? email.substring(0, email.indexOf("@")) : "User");
                            AppState.getInstance().setUserProfile(name, email, null);
                            Toast.makeText(LoginActivity.this, "Welcome back, " + name + "!", Toast.LENGTH_SHORT).show();
                            handleLogin();
                        } else {
                            String error = task.getException() != null ? task.getException().getMessage() : "Authentication failed.";
                            Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        btnGotoSignup.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignupActivity.class);
            startActivity(intent);
        });
    }

    private void handleLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
