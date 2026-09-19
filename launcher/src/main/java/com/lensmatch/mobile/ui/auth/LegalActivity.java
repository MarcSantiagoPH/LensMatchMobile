package com.lensmatch.mobile.ui.auth;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.lensmatch.mobile.R;

public class LegalActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legal);

        TextView tvTitle = findViewById(R.id.tv_legal_title);
        TextView tvContent = findViewById(R.id.tv_legal_content);
        
        String type = getIntent().getStringExtra("type");
        if ("terms".equals(type)) {
            tvTitle.setText("Terms of Use");
            tvContent.setText("Welcome to LensMatch.\n\n" +
                    "By creating an account, you agree to abide by our terms and conditions. " +
                    "You must provide accurate information, including your full name, email address, and phone number, during registration.\n\n" +
                    "These details are required to provide you with a personalized experience and for account recovery purposes.");
        } else if ("privacy".equals(type)) {
            tvTitle.setText("Privacy Policy");
            tvContent.setText("LensMatch values your privacy.\n\n" +
                    "Data Collection:\n" +
                    "To use our services, we collect your Full Name, Email Address, and Phone Number. " +
                    "This information is used strictly for account management, authentication, and personalized communication.\n\n" +
                    "We do not sell your personal data to third parties. By agreeing to this policy, you consent to the secure storage and processing of your personal information.");
        }
    }
}
