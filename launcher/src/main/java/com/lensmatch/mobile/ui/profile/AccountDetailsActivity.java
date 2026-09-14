package com.lensmatch.mobile.ui.profile;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
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

    // Password Linking Section Views
    private TextView tvHeaderSetPassword;
    private TextView tvSubSetPassword;
    private TextInputLayout tilNewPassword;
    private TextInputLayout tilConfirmPassword;
    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmPassword;
    private MaterialButton btnLinkPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_details);

        View root = findViewById(R.id.account_details_root);
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

        // Password Linking Views
        tvHeaderSetPassword = findViewById(R.id.tv_header_set_password);
        tvSubSetPassword = findViewById(R.id.tv_sub_set_password);
        tilNewPassword = findViewById(R.id.til_account_new_password);
        tilConfirmPassword = findViewById(R.id.til_account_confirm_password);
        etNewPassword = findViewById(R.id.et_account_new_password);
        etConfirmPassword = findViewById(R.id.et_account_confirm_password);
        btnLinkPassword = findViewById(R.id.btn_link_password);

        loadUserData();
        setupRealtimeHeaderUpdates();
        setupPasswordLinking();

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

    private void setupPasswordLinking() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            hidePasswordSection();
            return;
        }

        boolean hasPasswordProvider = false;
        for (UserInfo info : user.getProviderData()) {
            if (info != null && "password".equalsIgnoreCase(info.getProviderId())) {
                hasPasswordProvider = true;
                break;
            }
        }

        if (hasPasswordProvider) {
            if (tvHeaderSetPassword != null) tvHeaderSetPassword.setText(R.string.title_email_signin);
            if (tvSubSetPassword != null) tvSubSetPassword.setText(R.string.sub_email_signin);
            if (tilNewPassword != null) tilNewPassword.setVisibility(View.GONE);
            if (tilConfirmPassword != null) tilConfirmPassword.setVisibility(View.GONE);
            if (btnLinkPassword != null) btnLinkPassword.setVisibility(View.GONE);
        } else {
            if (tvHeaderSetPassword != null) tvHeaderSetPassword.setText(R.string.title_set_password);
            if (tvSubSetPassword != null) tvSubSetPassword.setText(R.string.sub_set_password);
            if (tilNewPassword != null) tilNewPassword.setVisibility(View.VISIBLE);
            if (tilConfirmPassword != null) tilConfirmPassword.setVisibility(View.VISIBLE);
            if (btnLinkPassword != null) {
                btnLinkPassword.setVisibility(View.VISIBLE);
                btnLinkPassword.setOnClickListener(v -> linkNewPassword(user));
            }
        }
    }

    private void hidePasswordSection() {
        if (tvHeaderSetPassword != null) tvHeaderSetPassword.setVisibility(View.GONE);
        if (tvSubSetPassword != null) tvSubSetPassword.setVisibility(View.GONE);
        if (tilNewPassword != null) tilNewPassword.setVisibility(View.GONE);
        if (tilConfirmPassword != null) tilConfirmPassword.setVisibility(View.GONE);
        if (btnLinkPassword != null) btnLinkPassword.setVisibility(View.GONE);
    }

    private void linkNewPassword(FirebaseUser user) {
        String newPassword = etNewPassword != null && etNewPassword.getText() != null ? etNewPassword.getText().toString() : "";
        String confirmPassword = etConfirmPassword != null && etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";

        if (tilNewPassword != null) tilNewPassword.setError(null);
        if (tilConfirmPassword != null) tilConfirmPassword.setError(null);

        if (newPassword.isEmpty()) {
            if (tilNewPassword != null) tilNewPassword.setError(getString(R.string.err_new_password_required));
            return;
        }

        if (newPassword.length() < 6) {
            if (tilNewPassword != null) tilNewPassword.setError(getString(R.string.err_password_min_length));
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            if (tilConfirmPassword != null) tilConfirmPassword.setError(getString(R.string.err_passwords_do_not_match));
            return;
        }

        String email = user.getEmail();
        if (email == null || email.isEmpty()) {
            email = etEmail != null && etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        }

        if (email.isEmpty()) {
            Toast.makeText(this, R.string.err_email_required_for_password, Toast.LENGTH_SHORT).show();
            return;
        }

        if (btnLinkPassword != null) {
            btnLinkPassword.setEnabled(false);
            btnLinkPassword.setText(R.string.btn_adding_password);
        }

        AuthCredential credential = EmailAuthProvider.getCredential(email, newPassword);
        user.linkWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (btnLinkPassword != null) {
                        btnLinkPassword.setEnabled(true);
                        btnLinkPassword.setText(R.string.btn_add_password);
                    }

                    if (task.isSuccessful()) {
                        Toast.makeText(AccountDetailsActivity.this, R.string.msg_password_added_success, Toast.LENGTH_LONG).show();
                        setupPasswordLinking();
                    } else {
                        Exception ex = task.getException();
                        if (ex instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(AccountDetailsActivity.this, R.string.err_account_collision, Toast.LENGTH_LONG).show();
                        } else {
                            String errorMsg = ex != null ? ex.getMessage() : getString(R.string.err_failed_to_add_password);
                            Toast.makeText(AccountDetailsActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }
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
