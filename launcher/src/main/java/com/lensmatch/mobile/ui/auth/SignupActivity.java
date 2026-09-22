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
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.CheckBox;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.AppState;
import com.lensmatch.mobile.service.FirestoreService;
import com.lensmatch.mobile.ui.MainActivity;
import com.lensmatch.mobile.utils.GoogleAuthHelper;
import com.lensmatch.mobile.utils.StatusBarUtils;

public class SignupActivity extends AppCompatActivity {
    private GoogleAuthHelper googleAuthHelper;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().setLocalNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        androidx.core.view.WindowInsetsControllerCompat controller = 
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        getWindow().setNavigationBarColor(android.graphics.Color.WHITE);
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
            controller.setAppearanceLightNavigationBars(true);
        }

        android.view.View root = findViewById(R.id.signup_root);
        StatusBarUtils.applyWindowInsets(root);

        TextInputEditText etName = findViewById(R.id.et_signup_name);
        TextInputEditText etEmail = findViewById(R.id.et_signup_email);
        TextInputEditText etPhone = findViewById(R.id.et_signup_phone);
        TextInputEditText etPassword = findViewById(R.id.et_signup_password);
        MaterialButton btnSignup = findViewById(R.id.btn_signup);
        MaterialButton btnGoogleSignup = findViewById(R.id.btn_google_signup);
        TextView btnGotoLogin = findViewById(R.id.btn_goto_login);

        CheckBox cbTermsPrivacy = findViewById(R.id.cb_terms_privacy);
        TextView tvTermsPrivacy = findViewById(R.id.tv_terms_privacy);

        String agreementText = "I have read and agree to the Terms of Use and Privacy Policy.";
        SpannableString ss = new SpannableString(agreementText);

        ClickableSpan termsSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent intent = new Intent(SignupActivity.this, LegalActivity.class);
                intent.putExtra("type", "terms");
                startActivity(intent);
            }
        };

        ClickableSpan privacySpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent intent = new Intent(SignupActivity.this, LegalActivity.class);
                intent.putExtra("type", "privacy");
                startActivity(intent);
            }
        };

        ss.setSpan(termsSpan, 29, 41, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(privacySpan, 46, 60, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvTermsPrivacy.setText(ss);
        tvTermsPrivacy.setMovementMethod(LinkMovementMethod.getInstance());

        // Initialize Google Auth Helper
        googleAuthHelper = new GoogleAuthHelper(this, new GoogleAuthHelper.AuthCallback() {
            @Override
            public void onAuthSuccess(String name, String email, String photoUrl) {
                AppState.getInstance().setEulaAccepted(true);
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

        btnGoogleSignup.setOnClickListener(v -> {
            if (!cbTermsPrivacy.isChecked()) {
                Toast.makeText(SignupActivity.this, "You must agree to the Terms of Use and Privacy Policy before creating an account.", Toast.LENGTH_LONG).show();
                return;
            }
            googleAuthHelper.launchSignIn(googleSignInLauncher);
        });

        btnSignup.setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
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

            if (phone.isEmpty()) {
                etPhone.setError("Phone number is required");
                etPhone.requestFocus();
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

            if (!cbTermsPrivacy.isChecked()) {
                Toast.makeText(this, "You must agree to the Terms of Use and Privacy Policy before creating an account.", Toast.LENGTH_LONG).show();
                return;
            }

            btnSignup.setEnabled(false);
            btnSignup.setText("Creating account...");

            final String finalName = name;
            final String finalEmail = email;
            final String finalPhone = phone;

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
                            AppState.getInstance().setEulaAccepted(true);
                            AppState.getInstance().updateAccountDetails(finalName, finalPhone, "", finalEmail);
                            AppState.getInstance().setUserProfile(finalName, finalEmail, null);

                            btnSignup.setEnabled(false);
                            btnSignup.setText("Creating profile...");

                            FirestoreService.createInitialCustomerProfile(finalName, finalPhone, finalEmail, new FirestoreService.Callback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    if (isFinishing() || isDestroyed()) return;
                                    btnSignup.setEnabled(true);
                                    btnSignup.setText("Sign Up");
                                    Toast.makeText(SignupActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                                    proceedToMain();
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    if (isFinishing() || isDestroyed()) return;
                                    btnSignup.setEnabled(true);
                                    btnSignup.setText("Sign Up");
                                    Toast.makeText(SignupActivity.this, errorMessage != null ? errorMessage : "Failed to create customer profile.", Toast.LENGTH_LONG).show();
                                }
                            });
                        } else {
                            btnSignup.setEnabled(true);
                            btnSignup.setText("Sign Up");
                            String error = task.getException() != null ? task.getException().getMessage() : "Registration failed.";
                            Toast.makeText(SignupActivity.this, error, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        btnGotoLogin.setOnClickListener(v -> finish());
    }

    private void proceedToMain() {
        Intent intent = new Intent(this, com.lensmatch.mobile.ui.guidelines.AppGuidelinesActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
