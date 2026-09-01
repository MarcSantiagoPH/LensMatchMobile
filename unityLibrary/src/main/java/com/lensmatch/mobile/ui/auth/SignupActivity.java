package com.lensmatch.mobile.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.ui.MainActivity;

public class SignupActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        MaterialButton btnSignup = findViewById(R.id.btn_signup);
        TextView btnGotoLogin = findViewById(R.id.btn_goto_login);

        btnSignup.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        btnGotoLogin.setOnClickListener(v -> finish());
    }
}
