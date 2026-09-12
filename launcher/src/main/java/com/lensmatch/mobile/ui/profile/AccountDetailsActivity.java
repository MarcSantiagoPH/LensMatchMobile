package com.lensmatch.mobile.ui.profile;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.AppState;
import com.lensmatch.mobile.utils.StatusBarUtils;

public class AccountDetailsActivity extends AppCompatActivity {

    private TextView tvAvatarInitials;
    private TextView tvHeaderName;
    private TextView tvHeaderEmail;

    private TextInputLayout tilName;
    private TextInputLayout tilPhone;
    private TextInputLayout tilAddress;
    private TextInputLayout tilEmail;

    private TextInputEditText etName;
    private TextInputEditText etPhone;
    private TextInputEditText etAddress;
    private TextInputEditText etEmail;

    private MaterialButton btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_details);

        android.view.View root = findViewById(R.id.account_details_root);
        StatusBarUtils.applyTopWindowInsets(root);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_account_details);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvAvatarInitials = findViewById(R.id.tv_account_avatar_initials);
        tvHeaderName = findViewById(R.id.tv_account_header_name);
        tvHeaderEmail = findViewById(R.id.tv_account_header_email);

        tilName = findViewById(R.id.til_account_name);
        tilPhone = findViewById(R.id.til_account_phone);
        tilAddress = findViewById(R.id.til_account_address);
        tilEmail = findViewById(R.id.til_account_email);

        etName = findViewById(R.id.et_account_name);
        etPhone = findViewById(R.id.et_account_phone);
        etAddress = findViewById(R.id.et_account_address);
        etEmail = findViewById(R.id.et_account_email);

        btnSave = findViewById(R.id.btn_save_account);

        loadUserData();
        setupRealtimeHeaderUpdates();

        btnSave.setOnClickListener(v -> saveAccountDetails());
    }

    private void loadUserData() {
        AppState state = AppState.getInstance();
        String name = state.getUserName();
        String phone = state.getUserPhone();
        String address = state.getUserAddress();
        String email = state.getUserEmail();

        etName.setText(name);
        etPhone.setText(phone);
        etAddress.setText(address);
        etEmail.setText(email);

        updateHeader(name, email);
    }

    private void updateHeader(String name, String email) {
        if (tvHeaderName != null) {
            tvHeaderName.setText(name != null && !name.trim().isEmpty() ? name : "User");
        }
        if (tvHeaderEmail != null) {
            tvHeaderEmail.setText(email != null && !email.trim().isEmpty() ? email : "");
        }
        if (tvAvatarInitials != null) {
            tvAvatarInitials.setText(calculateInitials(name));
        }
    }

    private String calculateInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "LM";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2 && parts[0].length() > 0 && parts[1].length() > 0) {
            return ("" + Character.toUpperCase(parts[0].charAt(0)) + Character.toUpperCase(parts[1].charAt(0)));
        } else if (parts.length == 1 && parts[0].length() > 0) {
            return ("" + Character.toUpperCase(parts[0].charAt(0)));
        }
        return "LM";
    }

    private void setupRealtimeHeaderUpdates() {
        etName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String name = s != null ? s.toString() : "";
                if (tvHeaderName != null) tvHeaderName.setText(!name.trim().isEmpty() ? name : "User");
                if (tvAvatarInitials != null) tvAvatarInitials.setText(calculateInitials(name));
                if (tilName != null) tilName.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String email = s != null ? s.toString() : "";
                if (tvHeaderEmail != null) tvHeaderEmail.setText(email);
                if (tilEmail != null) tilEmail.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void saveAccountDetails() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        boolean hasError = false;

        if (name.isEmpty()) {
            tilName.setError("Full name is required");
            hasError = true;
        }

        if (email.isEmpty() || !email.contains("@") || !email.contains(".")) {
            tilEmail.setError("Enter a valid email address");
            hasError = true;
        }

        if (hasError) return;

        AppState.getInstance().updateAccountDetails(name, phone, address, email);
        Toast.makeText(this, "Account details updated successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
