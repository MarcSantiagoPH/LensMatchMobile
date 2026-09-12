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
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.AppState;
import com.lensmatch.mobile.ui.MainActivity;
import com.lensmatch.mobile.utils.GoogleAuthHelper;
import com.lensmatch.mobile.utils.StatusBarUtils;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText etEmail, etPassword;
    private GoogleAuthHelper googleAuthHelper;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        android.view.View root = findViewById(R.id.login_root);
        StatusBarUtils.applyTopWindowInsets(root);

        etEmail = findViewById(R.id.et_login_email);
        etPassword = findViewById(R.id.et_login_password);
        TextView btnForgotPassword = findViewById(R.id.btn_forgot_password);
        MaterialButton btnLogin = findViewById(R.id.btn_login);
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

        btnForgotPassword.setOnClickListener(v -> 
            Toast.makeText(this, "Password reset link sent!", Toast.LENGTH_SHORT).show()
        );

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            if (!email.isEmpty()) {
                String fallbackName = email.contains("@") ? email.substring(0, email.indexOf("@")) : "User";
                fallbackName = Character.toUpperCase(fallbackName.charAt(0)) + fallbackName.substring(1);
                AppState.getInstance().setUserProfile(fallbackName, email, null);
            }
            handleLogin();
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
