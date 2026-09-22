package com.lensmatch.mobile.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.AppState;
import com.lensmatch.mobile.ui.auth.LoginActivity;

public class ProfileFragment extends Fragment {
    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvAvatarInitials;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        swipeRefresh = root.findViewById(R.id.swipe_refresh_profile);
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeColors(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_orange),
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_orange_dark)
            );
            swipeRefresh.setOnRefreshListener(this::refreshProfileData);
        }

        tvProfileName = root.findViewById(R.id.tv_profile_name);
        tvProfileEmail = root.findViewById(R.id.tv_profile_email);
        tvAvatarInitials = root.findViewById(R.id.tv_avatar_initials);

        LinearLayout itemScanHistory = root.findViewById(R.id.item_scan_history);
        LinearLayout itemMyReservations = root.findViewById(R.id.item_my_reservations);
        LinearLayout itemAccountDetails = root.findViewById(R.id.item_account_details);
        LinearLayout itemNotifications = root.findViewById(R.id.item_notifications);
        LinearLayout itemHelpSupport = root.findViewById(R.id.item_help_support);
        MaterialButton btnLogout = root.findViewById(R.id.btn_logout);

        updateProfileUI();

        itemMyReservations.setOnClickListener(v -> startActivity(new Intent(requireContext(), ReservationsActivity.class)));
        itemScanHistory.setOnClickListener(v -> startActivity(new Intent(requireContext(), ScanHistoryActivity.class)));
        itemAccountDetails.setOnClickListener(v -> startActivity(new Intent(requireContext(), AccountDetailsActivity.class)));
        itemNotifications.setOnClickListener(v -> startActivity(new Intent(requireContext(), NotificationsActivity.class)));
        itemHelpSupport.setOnClickListener(v -> startActivity(new Intent(requireContext(), HelpSupportActivity.class)));



        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            try {
                int webClientIdRes = getResources().getIdentifier("default_web_client_id", "string", requireContext().getPackageName());
                if (webClientIdRes != 0) {
                    com.google.android.gms.auth.api.signin.GoogleSignInOptions gso =
                            new com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestEmail()
                                    .build();
                    com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(requireContext(), gso).signOut();
                }
            } catch (Exception ignored) {}
            AppState.getInstance().logout();
            Intent intent = new Intent(requireContext(), com.lensmatch.mobile.ui.auth.LandingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateProfileUI();
        com.lensmatch.mobile.service.FirestoreService.loadCustomerProfile(new com.lensmatch.mobile.service.FirestoreService.Callback<java.util.Map<String, Object>>() {
            @Override
            public void onSuccess(java.util.Map<String, Object> result) {
                if (isAdded()) {
                    updateProfileUI();
                }
            }

            @Override
            public void onError(String errorMessage) {}
        });
    }

    private void refreshProfileData() {
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(true);
        }
        com.lensmatch.mobile.service.FirestoreService.loadCustomerProfile(new com.lensmatch.mobile.service.FirestoreService.Callback<java.util.Map<String, Object>>() {
            @Override
            public void onSuccess(java.util.Map<String, Object> result) {
                if (isAdded()) {
                    updateProfileUI();
                }
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onError(String errorMessage) {
                if (isAdded()) {
                    updateProfileUI();
                }
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void updateProfileUI() {
        AppState state = AppState.getInstance();
        String name = state.getUserName();
        String email = state.getUserEmail();

        if (tvProfileName != null) tvProfileName.setText(name);
        if (tvProfileEmail != null) tvProfileEmail.setText(email);

        if (tvAvatarInitials != null) {
            String initials = "JD";
            if (name != null && !name.trim().isEmpty()) {
                String[] parts = name.trim().split("\\s+");
                if (parts.length >= 2) {
                    initials = "" + Character.toUpperCase(parts[0].charAt(0)) + Character.toUpperCase(parts[1].charAt(0));
                } else if (parts.length == 1 && parts[0].length() > 0) {
                    initials = "" + Character.toUpperCase(parts[0].charAt(0));
                }
            }
            tvAvatarInitials.setText(initials);
        }
    }
}
