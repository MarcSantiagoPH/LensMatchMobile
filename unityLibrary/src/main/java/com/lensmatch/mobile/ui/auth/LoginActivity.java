package com.lensmatch.mobile.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.ui.MainActivity;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText etEmail, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.et_login_email);
        etPassword = findViewById(R.id.et_login_password);
        TextView btnForgotPassword = findViewById(R.id.btn_forgot_password);
        MaterialButton btnLogin = findViewById(R.id.btn_login);
        TextView btnGotoSignup = findViewById(R.id.btn_goto_signup);

        btnForgotPassword.setOnClickListener(v -> 
            Toast.makeText(this, "Password reset link sent!", Toast.LENGTH_SHORT).show()
        );

        btnLogin.setOnClickListener(v -> handleLogin());

        btnGotoSignup.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignupActivity.class);
            startActivity(intent);
        });
    }

    private void handleLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
