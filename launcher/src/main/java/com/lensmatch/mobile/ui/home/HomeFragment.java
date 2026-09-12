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
import com.lensmatch.mobile.data.ReservationModel;
import com.lensmatch.mobile.service.FirestoreService;
import com.lensmatch.mobile.ui.MainActivity;
import com.lensmatch.mobile.ui.profile.ReservationsActivity;

import java.util.List;

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
        if (tvLastResultSubtitle != null) {
            tvLastResultSubtitle.setText(state.getLastDetectedShape() + " shape · 3 recommendations");
        }

        FirestoreService.getUserReservations(new FirestoreService.Callback<List<ReservationModel>>() {
            @Override
            public void onSuccess(List<ReservationModel> list) {
                if (!isAdded() || getContext() == null || tvReservationsCount == null) return;
                int count = 0;
                if (list != null) {
                    for (ReservationModel item : list) {
                        if (FirestoreService.isActiveStatus(item.getStatus())) {
                            count++;
                        }
                    }
                }
                String text = count + (count == 1 ? " item reserved" : " items reserved");
                tvReservationsCount.setText(text);
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded() || getContext() == null || tvReservationsCount == null) return;
                tvReservationsCount.setText("Unable to load reservations");
            }
        });
    }
}
