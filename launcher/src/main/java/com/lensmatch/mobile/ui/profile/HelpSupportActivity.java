package com.lensmatch.mobile.ui.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.utils.StatusBarUtils;

public class HelpSupportActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_support);

        View root = findViewById(R.id.help_support_root);
        StatusBarUtils.applyTopWindowInsets(root);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_help_support);
        toolbar.setNavigationOnClickListener(v -> finish());

        MaterialButton btnContact = findViewById(R.id.btn_contact_support);
        btnContact.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:support@lensmatch.com"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "LensMatch App Support Request");
            try {
                startActivity(Intent.createChooser(emailIntent, "Send Email Support"));
            } catch (Exception e) {
                Toast.makeText(this, "Email client not found. Reach us at support@lensmatch.com", Toast.LENGTH_LONG).show();
            }
        });
    }
}
