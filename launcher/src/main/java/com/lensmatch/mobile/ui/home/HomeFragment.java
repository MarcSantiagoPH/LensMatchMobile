package com.lensmatch.mobile.ui.home;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.AnnouncementModel;
import com.lensmatch.mobile.data.AppState;
import com.lensmatch.mobile.data.ReservationModel;
import com.lensmatch.mobile.service.FirestoreService;
import com.lensmatch.mobile.ui.MainActivity;
import com.lensmatch.mobile.ui.profile.HelpSupportActivity;
import com.lensmatch.mobile.ui.profile.ReservationDetailActivity;
import com.lensmatch.mobile.ui.profile.ReservationsActivity;
import com.lensmatch.mobile.ui.profile.ScanHistoryActivity;

import java.util.List;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private TextView tvReservationsCount;
    private ImageView ivHomeLogo;
    private LinearLayout layoutAnnouncementsContainer;

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
                    BottomNavigationView bNav =
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
                    BottomNavigationView bNav =
                            getActivity().findViewById(R.id.bottom_navigation);
                    if (bNav != null) {
                        bNav.setSelectedItemId(R.id.nav_frame);
                    }
                }
            });
        }

        if (actionQuickResult != null) {
            actionQuickResult.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ScanHistoryActivity.class);
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

        layoutAnnouncementsContainer = root.findViewById(R.id.layout_announcements_container);

        updateLogoForTheme();
        updateQuickLinks();
        loadAnnouncements();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateLogoForTheme();
        updateQuickLinks();
        loadAnnouncements();
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

    private void loadAnnouncements() {
        if (layoutAnnouncementsContainer == null || !isAdded() || getContext() == null) return;

        layoutAnnouncementsContainer.removeAllViews();

        ProgressBar pb = new ProgressBar(requireContext());
        pb.setIndeterminateTintList(ColorStateList.valueOf(Color.parseColor("#D97745")));
        LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pbParams.gravity = Gravity.CENTER_HORIZONTAL;
        pbParams.setMargins(0, 24, 0, 24);
        layoutAnnouncementsContainer.addView(pb, pbParams);

        FirestoreService.getAnnouncements(new FirestoreService.Callback<List<AnnouncementModel>>() {
            @Override
            public void onSuccess(List<AnnouncementModel> list) {
                if (!isAdded() || getContext() == null || layoutAnnouncementsContainer == null) return;
                layoutAnnouncementsContainer.removeAllViews();

                if (list == null || list.isEmpty()) {
                    showFallbackAnnouncementCard();
                    return;
                }

                LayoutInflater inflater = LayoutInflater.from(requireContext());
                for (AnnouncementModel item : list) {
                    View card = inflater.inflate(R.layout.item_announcement_card, layoutAnnouncementsContainer, false);
                    TextView tvBadge = card.findViewById(R.id.tv_announcement_badge);
                    TextView tvSubtitle = card.findViewById(R.id.tv_announcement_subtitle);
                    TextView tvTitle = card.findViewById(R.id.tv_announcement_title);
                    TextView tvDesc = card.findViewById(R.id.tv_announcement_desc);
                    ImageView ivFooterIcon = card.findViewById(R.id.iv_announcement_footer_icon);
                    TextView tvFooterLabel = card.findViewById(R.id.tv_announcement_footer_label);
                    TextView tvActionText = card.findViewById(R.id.tv_announcement_action_text);

                    String category = item.getCategory() != null && !item.getCategory().isEmpty() ? item.getCategory().toUpperCase() : "GENERAL";
                    if (tvBadge != null) tvBadge.setText(category);

                    if ("NEW ARRIVALS".equals(category)) {
                        if (tvBadge != null) {
                            tvBadge.setBackgroundResource(R.drawable.bg_new_badge);
                            tvBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D97745")));
                            tvBadge.setTextColor(Color.parseColor("#FFFFFF"));
                        }
                        if (ivFooterIcon != null) ivFooterIcon.setImageResource(R.drawable.ic_eyeglasses);
                        if (tvFooterLabel != null) tvFooterLabel.setText("Browse New Arrivals in Catalog");
                        if (tvActionText != null) tvActionText.setText("View Catalog →");

                        card.setOnClickListener(v -> {
                            if (getActivity() != null) {
                                BottomNavigationView bNav = getActivity().findViewById(R.id.bottom_navigation);
                                if (bNav != null) {
                                    bNav.setSelectedItemId(R.id.nav_frame);
                                }
                            }
                        });
                    } else {
                        if (tvBadge != null) {
                            tvBadge.setBackgroundResource(R.drawable.bg_confidence_chip);
                            tvBadge.setBackgroundTintList(null);
                            tvBadge.setTextColor(Color.parseColor("#1A1A1A"));
                        }
                        if (ivFooterIcon != null) ivFooterIcon.setImageResource(R.drawable.ic_location);
                        if (tvFooterLabel != null) tvFooterLabel.setText("Franselle Optical Clinic");
                        if (tvActionText != null) tvActionText.setText("View Details →");

                        card.setOnClickListener(v -> {
                            Intent intent = new Intent(requireContext(), HelpSupportActivity.class);
                            startActivity(intent);
                        });
                    }

                    if (tvSubtitle != null) tvSubtitle.setText("Franselle Optical");
                    if (tvTitle != null) tvTitle.setText(item.getTitle());
                    if (tvDesc != null) tvDesc.setText(item.getDescription());

                    layoutAnnouncementsContainer.addView(card);
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded() || getContext() == null || layoutAnnouncementsContainer == null) return;
                Log.e(TAG, "Error loading announcements: " + errorMessage);
                showFallbackAnnouncementCard();
            }
        });
    }

    private void showFallbackAnnouncementCard() {
        if (layoutAnnouncementsContainer == null || !isAdded() || getContext() == null) return;
        layoutAnnouncementsContainer.removeAllViews();

        View card = LayoutInflater.from(requireContext()).inflate(R.layout.item_announcement_card, layoutAnnouncementsContainer, false);
        TextView tvBadge = card.findViewById(R.id.tv_announcement_badge);
        TextView tvTitle = card.findViewById(R.id.tv_announcement_title);
        TextView tvDesc = card.findViewById(R.id.tv_announcement_desc);
        TextView tvAction = card.findViewById(R.id.tv_announcement_action_text);

        if (tvBadge != null) {
            tvBadge.setText("CLINIC NOTICE");
            tvBadge.setBackgroundResource(R.drawable.bg_confidence_chip);
            tvBadge.setBackgroundTintList(null);
            tvBadge.setTextColor(Color.parseColor("#1A1A1A"));
        }
        if (tvTitle != null) tvTitle.setText("No Active Announcements");
        if (tvDesc != null) tvDesc.setText("Check back soon for upcoming frame releases, promotions, and optical clinic updates.");
        if (tvAction != null) tvAction.setText("Help & Support →");

        card.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), HelpSupportActivity.class);
            startActivity(intent);
        });

        layoutAnnouncementsContainer.addView(card);
    }
}
