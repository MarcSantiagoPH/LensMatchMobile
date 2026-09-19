package com.lensmatch.mobile.ui.home;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.AppState;
import com.lensmatch.mobile.data.ReservationModel;
import com.lensmatch.mobile.service.FirestoreService;
import com.lensmatch.mobile.ui.MainActivity;
import com.lensmatch.mobile.ui.profile.HelpSupportActivity;
import com.lensmatch.mobile.ui.profile.ReservationDetailActivity;
import com.lensmatch.mobile.ui.profile.ReservationsActivity;

import java.util.List;

public class HomeFragment extends Fragment {
    private TextView tvReservationsCount;
    private ImageView ivHomeLogo;

    // Reservation Status UI components
    private MaterialCardView cardReservationStatus;
    private LinearLayout layoutActiveReservation;
    private LinearLayout layoutNoReservation;
    private TextView tvResStatusFrameName;
    private TextView tvResStatusBadge;
    private TextView tvResStatusStyle;
    private TextView tvResStatusPrice;
    private TextView tvResStatusDate;
    private TextView tvResStatusHint;
    private ImageView ivResStatusImage;

    private ReservationModel activeReservation = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        MaterialButton btnScanFace = root.findViewById(R.id.btn_scan_face);
        MaterialCardView cardAnnouncement = root.findViewById(R.id.card_announcement);
        cardReservationStatus = root.findViewById(R.id.card_reservation_status);
        layoutActiveReservation = root.findViewById(R.id.layout_active_reservation);
        layoutNoReservation = root.findViewById(R.id.layout_no_reservation);
        tvResStatusFrameName = root.findViewById(R.id.tv_res_status_frame_name);
        tvResStatusBadge = root.findViewById(R.id.tv_res_status_badge);
        tvResStatusStyle = root.findViewById(R.id.tv_res_status_style);
        tvResStatusPrice = root.findViewById(R.id.tv_res_status_price);
        tvResStatusDate = root.findViewById(R.id.tv_res_status_date);
        tvResStatusHint = root.findViewById(R.id.tv_res_status_hint);
        ivResStatusImage = root.findViewById(R.id.iv_res_status_image);

        ivHomeLogo = root.findViewById(R.id.iv_home_logo);

        btnScanFace.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openCameraScan();
            }
        });

        if (cardAnnouncement != null) {
            cardAnnouncement.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), HelpSupportActivity.class);
                startActivity(intent);
            });
        }

        if (cardReservationStatus != null) {
            cardReservationStatus.setOnClickListener(v -> {
                if (activeReservation != null) {
                    Intent intent = new Intent(requireContext(), ReservationDetailActivity.class);
                    intent.putExtra("reservation", activeReservation);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(requireContext(), ReservationsActivity.class);
                    startActivity(intent);
                }
            });
        }

        MaterialCardView cardFaceShapeGuide = root.findViewById(R.id.card_face_shape_guide);
        MaterialCardView cardClinicInfo = root.findViewById(R.id.card_clinic_info);

        if (cardFaceShapeGuide != null) {
            cardFaceShapeGuide.setOnClickListener(v -> {
                if (getActivity() != null) {
                    com.google.android.material.bottomnavigation.BottomNavigationView bNav =
                            getActivity().findViewById(R.id.bottom_navigation);
                    if (bNav != null) {
                        bNav.setSelectedItemId(R.id.nav_frame);
                    }
                }
            });
        }

        if (cardClinicInfo != null) {
            cardClinicInfo.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), HelpSupportActivity.class);
                startActivity(intent);
            });
        }

        View actionQuickCatalog = root.findViewById(R.id.action_quick_catalog);
        View actionQuickResult = root.findViewById(R.id.action_quick_result);
        View actionQuickReservations = root.findViewById(R.id.action_quick_reservations);
        View actionQuickClinic = root.findViewById(R.id.action_quick_clinic);
        MaterialCardView cardAnnouncementNewFrames = root.findViewById(R.id.card_announcement_new_frames);
        View btnSeeMoreGuide = root.findViewById(R.id.btn_see_more_guide);

        if (btnSeeMoreGuide != null) {
            btnSeeMoreGuide.setOnClickListener(v -> {
                FullGuideBottomSheet bottomSheet = new FullGuideBottomSheet();
                bottomSheet.show(getChildFragmentManager(), "FullGuideBottomSheet");
            });
        }



        if (actionQuickCatalog != null) {
            actionQuickCatalog.setOnClickListener(v -> {
                if (getActivity() != null) {
                    com.google.android.material.bottomnavigation.BottomNavigationView bNav =
                            getActivity().findViewById(R.id.bottom_navigation);
                    if (bNav != null) {
                        bNav.setSelectedItemId(R.id.nav_frame);
                    }
                }
            });
        }

        if (actionQuickResult != null) {
            actionQuickResult.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), com.lensmatch.mobile.ui.profile.ScanHistoryActivity.class);
                startActivity(intent);
            });
        }

        if (actionQuickReservations != null) {
            actionQuickReservations.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ReservationsActivity.class);
                startActivity(intent);
            });
        }

        if (actionQuickClinic != null) {
            actionQuickClinic.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), HelpSupportActivity.class);
                startActivity(intent);
            });
        }

        if (cardAnnouncementNewFrames != null) {
            cardAnnouncementNewFrames.setOnClickListener(v -> {
                if (getActivity() != null) {
                    com.google.android.material.bottomnavigation.BottomNavigationView bNav =
                            getActivity().findViewById(R.id.bottom_navigation);
                    if (bNav != null) {
                        bNav.setSelectedItemId(R.id.nav_frame);
                    }
                }
            });
        }

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

        int darkLogoRes = getResources().getIdentifier("logo_lensmatch_dark", "drawable", requireContext().getPackageName());
        if (isNight && darkLogoRes != 0) {
            ivHomeLogo.setImageResource(darkLogoRes);
        } else {
            ivHomeLogo.setImageResource(R.drawable.logo_lensmatch);
        }
    }

    private void updateQuickLinks() {
        FirestoreService.getUserReservations(new FirestoreService.Callback<List<ReservationModel>>() {
            @Override
            public void onSuccess(List<ReservationModel> list) {
                if (!isAdded() || getContext() == null) return;

                int activeCount = 0;
                ReservationModel latestActive = null;

                if (list != null) {
                    for (ReservationModel item : list) {
                        if (FirestoreService.isActiveStatus(item.getStatus())) {
                            activeCount++;
                            if (latestActive == null) {
                                latestActive = item;
                            }
                        }
                    }
                }

                activeReservation = latestActive;

                if (tvReservationsCount != null) {
                    tvReservationsCount.setText(activeCount == 0 ? "Reserve a frame to schedule an in-store fitting"
                            : activeCount + (activeCount == 1 ? " active frame reserved" : " active frames reserved"));
                }

                // Update Live Reservation Status Card
                if (latestActive != null && layoutActiveReservation != null && layoutNoReservation != null) {
                    layoutActiveReservation.setVisibility(View.VISIBLE);
                    layoutNoReservation.setVisibility(View.GONE);

                    if (tvResStatusFrameName != null) {
                        tvResStatusFrameName.setText(latestActive.getFrameName());
                    }

                    if (tvResStatusBadge != null) {
                        String status = latestActive.getStatus();
                        tvResStatusBadge.setText(status);
                        if ("Confirmed".equalsIgnoreCase(status) || "Approved".equalsIgnoreCase(status)) {
                            tvResStatusBadge.setTextColor(Color.parseColor("#10B981"));
                        } else if ("Cancelled".equalsIgnoreCase(status)) {
                            tvResStatusBadge.setTextColor(Color.parseColor("#EF4444"));
                        } else {
                            tvResStatusBadge.setTextColor(Color.parseColor("#D97745"));
                        }
                    }

                    if (tvResStatusStyle != null) {
                        String brand = latestActive.getBrand() != null && !latestActive.getBrand().isEmpty() ? latestActive.getBrand() : "LensMatch";
                        String style = latestActive.getFrameStyle() != null && !latestActive.getFrameStyle().isEmpty() ? latestActive.getFrameStyle() : "Eyewear";
                        tvResStatusStyle.setText(brand + " · " + style);
                    }

                    if (tvResStatusPrice != null) {
                        tvResStatusPrice.setText(latestActive.getFormattedPrice());
                    }

                    if (tvResStatusDate != null) {
                        tvResStatusDate.setText("Requested: " + latestActive.getFormattedDate());
                    }

                    if (tvResStatusHint != null) {
                        String status = latestActive.getStatus();
                        if ("Confirmed".equalsIgnoreCase(status)) {
                            tvResStatusHint.setText("✓ Confirmed! Held at Franselle Optical Clinic for your visit.");
                        } else {
                            tvResStatusHint.setText("Clinic staff is reviewing your reservation for in-store fitting.");
                        }
                    }

                    if (ivResStatusImage != null) {
                        if (latestActive.getImageUrl() != null && !latestActive.getImageUrl().trim().isEmpty()) {
                            ivResStatusImage.setImageTintList(null);
                            Glide.with(requireContext())
                                    .load(latestActive.getImageUrl())
                                    .placeholder(R.drawable.ic_eyeglasses)
                                    .error(R.drawable.ic_eyeglasses)
                                    .into(ivResStatusImage);
                        } else {
                            ivResStatusImage.setImageResource(R.drawable.ic_eyeglasses);
                        }
                    }
                } else if (layoutActiveReservation != null && layoutNoReservation != null) {
                    layoutActiveReservation.setVisibility(View.GONE);
                    layoutNoReservation.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded() || getContext() == null) return;
                if (tvReservationsCount != null) {
                    tvReservationsCount.setText("Check your reservations history");
                }
                if (layoutActiveReservation != null && layoutNoReservation != null) {
                    layoutActiveReservation.setVisibility(View.GONE);
                    layoutNoReservation.setVisibility(View.VISIBLE);
                }
            }
        });
    }
}
