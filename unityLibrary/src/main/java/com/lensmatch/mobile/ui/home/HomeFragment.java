package com.lensmatch.mobile.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.AppState;
import com.lensmatch.mobile.ui.MainActivity;
import com.lensmatch.mobile.ui.profile.ReservationsActivity;

public class HomeFragment extends Fragment {
    private TextView tvLastResultSubtitle;
    private TextView tvReservationsCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        MaterialButton btnScanFace = root.findViewById(R.id.btn_scan_face);
        MaterialCardView cardLastResult = root.findViewById(R.id.card_last_result);
        MaterialCardView cardReservations = root.findViewById(R.id.card_my_reservations);
        tvLastResultSubtitle = root.findViewById(R.id.tv_last_result_subtitle);
        tvReservationsCount = root.findViewById(R.id.tv_reservations_count);

        btnScanFace.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openCameraScan();
            }
        });

        cardLastResult.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToResult();
            }
        });

        cardReservations.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ReservationsActivity.class);
            startActivity(intent);
        });

        updateQuickLinks();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateQuickLinks();
    }

    private void updateQuickLinks() {
        AppState state = AppState.getInstance();
        tvLastResultSubtitle.setText(state.getLastDetectedShape() + " shape - 3 recommendations");
        tvReservationsCount.setText(state.getReservedFrames().size() + " items reserved");
    }
}
