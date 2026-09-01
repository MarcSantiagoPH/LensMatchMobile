package com.lensmatch.mobile.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.ui.auth.LoginActivity;

public class ProfileFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        LinearLayout itemScanHistory = root.findViewById(R.id.item_scan_history);
        LinearLayout itemMyReservations = root.findViewById(R.id.item_my_reservations);
        LinearLayout itemAccountDetails = root.findViewById(R.id.item_account_details);
        LinearLayout itemNotifications = root.findViewById(R.id.item_notifications);
        LinearLayout itemHelpSupport = root.findViewById(R.id.item_help_support);
        MaterialButton btnLogout = root.findViewById(R.id.btn_logout);

        itemMyReservations.setOnClickListener(v -> startActivity(new Intent(requireContext(), ReservationsActivity.class)));
        itemScanHistory.setOnClickListener(v -> startActivity(new Intent(requireContext(), ScanHistoryActivity.class)));
        itemAccountDetails.setOnClickListener(v -> startActivity(new Intent(requireContext(), AccountDetailsActivity.class)));
        itemNotifications.setOnClickListener(v -> startActivity(new Intent(requireContext(), NotificationsActivity.class)));
        itemHelpSupport.setOnClickListener(v -> startActivity(new Intent(requireContext(), HelpSupportActivity.class)));

        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return root;
    }
}
