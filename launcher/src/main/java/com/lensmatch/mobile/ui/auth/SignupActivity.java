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

public class SignupActivity extends AppCompatActivity {
    private GoogleAuthHelper googleAuthHelper;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        TextInputEditText etName = findViewById(R.id.et_signup_name);
        TextInputEditText etEmail = findViewById(R.id.et_signup_email);
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
            if (name.isEmpty()) name = "User";
            if (email.isEmpty()) email = "user@lensmatch.com";

            AppState.getInstance().setUserProfile(name, email, null);
            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
            proceedToMain();
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
