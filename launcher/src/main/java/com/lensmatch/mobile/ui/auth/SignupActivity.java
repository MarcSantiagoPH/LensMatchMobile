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
import com.google.firebase.auth.UserProfileChangeRequest;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.AppState;
import com.lensmatch.mobile.ui.MainActivity;
import com.lensmatch.mobile.utils.GoogleAuthHelper;

public class SignupActivity extends AppCompatActivity {
    private GoogleAuthHelper googleAuthHelper;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        TextInputEditText etName = findViewById(R.id.et_signup_name);
        TextInputEditText etEmail = findViewById(R.id.et_signup_email);
        TextInputEditText etPassword = findViewById(R.id.et_signup_password);
        MaterialButton btnSignup = findViewById(R.id.btn_signup);
        MaterialButton btnGoogleSignup = findViewById(R.id.btn_google_signup);
        TextView btnGotoLogin = findViewById(R.id.btn_goto_login);

        // Initialize Google Auth Helper
        googleAuthHelper = new GoogleAuthHelper(this, new GoogleAuthHelper.AuthCallback() {
            @Override
            public void onAuthSuccess(String name, String email, String photoUrl) {
                Toast.makeText(SignupActivity.this, "Signed up with Google as " + name + "!", Toast.LENGTH_SHORT).show();
                proceedToMain();
            }

            @Override
            public void onAuthFailure(String errorMessage) {
                if (errorMessage != null && !errorMessage.isEmpty()) {
                    Toast.makeText(SignupActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
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

        btnGoogleSignup.setOnClickListener(v -> googleAuthHelper.launchSignIn(googleSignInLauncher));

        btnSignup.setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword != null && etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            if (name.isEmpty()) {
                etName.setError("Full name is required");
                etName.requestFocus();
                return;
            }

            if (email.isEmpty()) {
                etEmail.setError("Email address is required");
                etEmail.requestFocus();
                return;
            }

            if (password.length() < 6) {
                if (etPassword != null) {
                    etPassword.setError("Password must be at least 6 characters");
                    etPassword.requestFocus();
                } else {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            btnSignup.setEnabled(false);
            btnSignup.setText("Creating account...");

            final String finalName = name;
            final String finalEmail = email;

            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        btnSignup.setEnabled(true);
                        btnSignup.setText("Sign Up");

                        if (task.isSuccessful()) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(finalName)
                                        .build();
                                user.updateProfile(profileUpdates);
                            }
                            AppState.getInstance().setUserProfile(finalName, finalEmail, null);
                            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                            proceedToMain();
                        } else {
                            String error = task.getException() != null ? task.getException().getMessage() : "Registration failed.";
                            Toast.makeText(SignupActivity.this, error, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        btnGotoLogin.setOnClickListener(v -> finish());
    }

    private void proceedToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
