package com.lensmatch.mobile.ui.home;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
    private ImageView ivHomeLogo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        MaterialButton btnScanFace = root.findViewById(R.id.btn_scan_face);
        MaterialCardView cardLastResult = root.findViewById(R.id.card_last_result);
        MaterialCardView cardReservations = root.findViewById(R.id.card_my_reservations);
        tvLastResultSubtitle = root.findViewById(R.id.tv_last_result_subtitle);
        tvReservationsCount = root.findViewById(R.id.tv_reservations_count);
        ivHomeLogo = root.findViewById(R.id.iv_home_logo);

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

        updateLogoForTheme();
        updateQuickLinks();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateLogoForTheme();
        updateQuickLinks();
    }

    private void updateLogoForTheme() {
        if (ivHomeLogo == null || getContext() == null) return;
        boolean isNight = "dark".equalsIgnoreCase(AppState.getInstance().getThemeMode()) ||
                (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        // If a separate logo_lensmatch_dark drawable is provided, use it for dark mode.
        // Otherwise, Android automatically resolves R.drawable.logo_lensmatch from res/drawable or res/drawable-night.
        int darkLogoRes = getResources().getIdentifier("logo_lensmatch_dark", "drawable", requireContext().getPackageName());
        if (isNight && darkLogoRes != 0) {
            ivHomeLogo.setImageResource(darkLogoRes);
        } else {
            ivHomeLogo.setImageResource(R.drawable.logo_lensmatch);
        }
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
