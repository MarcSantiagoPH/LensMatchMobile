package com.lensmatch.mobile.ui.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.ClinicModel;
import com.lensmatch.mobile.service.FirestoreService;
import com.lensmatch.mobile.utils.StatusBarUtils;

public class HelpSupportActivity extends AppCompatActivity {
    private String supportEmail = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_support);

        View root = findViewById(R.id.help_support_root);
        StatusBarUtils.applyTopWindowInsets(root);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_help_support);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvClinicName = findViewById(R.id.tv_support_clinic_name);
        TextView tvSubtitle = findViewById(R.id.tv_support_subtitle);
        MaterialButton btnContact = findViewById(R.id.btn_contact_support);

        FirestoreService.getClinicInformation(new FirestoreService.Callback<ClinicModel>() {
            @Override
            public void onSuccess(ClinicModel clinic) {
                if (isFinishing() || isDestroyed()) return;

                if (clinic != null) {
                    if (tvClinicName != null && clinic.getClinicName() != null && !clinic.getClinicName().isEmpty()) {
                        tvClinicName.setText(clinic.getClinicName());
                    }

                    String email = clinic.getEmail();
                    String hours = clinic.getBusinessHours();
                    if (email != null && !email.isEmpty()) {
                        supportEmail = email;
                        if (tvSubtitle != null) {
                            String sub = hours != null && !hours.isEmpty() ? (email + " • " + hours) : email;
                            tvSubtitle.setText(sub);
                        }
                    } else if (hours != null && !hours.isEmpty() && tvSubtitle != null) {
                        tvSubtitle.setText(hours);
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                // Keep default layout state without failing
            }
        });

        btnContact.setOnClickListener(v -> {
            if (supportEmail == null || supportEmail.trim().isEmpty()) {
                Toast.makeText(this, R.string.err_no_email_app, Toast.LENGTH_LONG).show();
                return;
            }

            String subject = "LensMatch App Support Request";
            String mailtoUri = "mailto:" + supportEmail.trim() + "?subject=" + Uri.encode(subject);

            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse(mailtoUri));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            try {
                startActivity(Intent.createChooser(emailIntent, "Send Email Support"));
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.err_no_email_app), Toast.LENGTH_LONG).show();
            }
        });
    }
}
