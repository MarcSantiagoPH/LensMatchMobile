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

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private TextView tvReservationsCount;
    private ImageView ivHomeLogo;
    private LinearLayout layoutAnnouncementsContainer;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;

    // Reservation Status UI components
    private MaterialCardView cardNoReservation;
    private com.lensmatch.mobile.utils.MaxHeightNestedScrollView scrollReservationsContainer;
    private LinearLayout layoutReservationsContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        swipeRefresh = root.findViewById(R.id.swipe_refresh_home);
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeColors(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_orange),
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_orange_dark)
            );
            swipeRefresh.setOnRefreshListener(this::refreshHomeData);
        }

        MaterialButton btnScanFace = root.findViewById(R.id.btn_scan_face);
        cardNoReservation = root.findViewById(R.id.card_no_reservation);
        scrollReservationsContainer = root.findViewById(R.id.scroll_reservations_container);
        layoutReservationsContainer = root.findViewById(R.id.layout_reservations_container);
        tvReservationsCount = root.findViewById(R.id.tv_reservations_count);

        ivHomeLogo = root.findViewById(R.id.iv_home_logo);

        btnScanFace.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openCameraScan();
            }
        });

        if (cardNoReservation != null) {
            cardNoReservation.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ReservationsActivity.class);
                startActivity(intent);
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

        View btnHomeAppGuidelines = root.findViewById(R.id.btn_home_app_guidelines);
        if (btnHomeAppGuidelines != null) {
            btnHomeAppGuidelines.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), com.lensmatch.mobile.ui.guidelines.AppGuidelinesActivity.class);
                intent.putExtra(com.lensmatch.mobile.ui.guidelines.AppGuidelinesActivity.EXTRA_FROM_SUPPORT, true);
                startActivity(intent);
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

    private void refreshHomeData() {
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(true);
        }
        updateLogoForTheme();
        com.lensmatch.mobile.service.FirestoreService.loadCustomerProfile(null);
        updateQuickLinks();
        loadAnnouncements();

        if (getView() != null) {
            getView().postDelayed(() -> {
                if (swipeRefresh != null && isAdded()) {
                    swipeRefresh.setRefreshing(false);
                }
            }, 1200);
        }
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

                List<ReservationModel> allReservations = new ArrayList<>();
                if (list != null) {
                    allReservations.addAll(list);
                }

                if (tvReservationsCount != null) {
                    tvReservationsCount.setText(allReservations.isEmpty()
                            ? "Reserve a frame to schedule an in-store fitting"
                            : allReservations.size() + (allReservations.size() == 1 ? " reservation transaction" : " reservation transactions"));
                }

                if (allReservations.isEmpty()) {
                    if (cardNoReservation != null) cardNoReservation.setVisibility(View.VISIBLE);
                    if (scrollReservationsContainer != null) scrollReservationsContainer.setVisibility(View.GONE);
                } else {
                    if (cardNoReservation != null) cardNoReservation.setVisibility(View.GONE);
                    if (scrollReservationsContainer != null) scrollReservationsContainer.setVisibility(View.VISIBLE);
                    if (layoutReservationsContainer != null) {
                        layoutReservationsContainer.removeAllViews();
                        LayoutInflater inflater = LayoutInflater.from(requireContext());

                        for (ReservationModel item : allReservations) {
                            View card = inflater.inflate(R.layout.item_home_reservation_card, layoutReservationsContainer, false);
                            TextView tvFrameName = card.findViewById(R.id.tv_home_res_frame_name);
                            TextView tvBadge = card.findViewById(R.id.tv_home_res_badge);
                            TextView tvStyle = card.findViewById(R.id.tv_home_res_style);
                            TextView tvPrice = card.findViewById(R.id.tv_home_res_price);
                            TextView tvDate = card.findViewById(R.id.tv_home_res_date);
                            TextView tvHint = card.findViewById(R.id.tv_home_res_hint);
                            ImageView ivImage = card.findViewById(R.id.iv_home_res_image);

                            if (tvFrameName != null) tvFrameName.setText(item.getFrameName());
                            String status = item.getStatus() != null && !item.getStatus().trim().isEmpty() ? item.getStatus() : "Pending";
                            if (tvBadge != null) {
                                tvBadge.setText(status);
                                if ("Confirmed".equalsIgnoreCase(status) || "Approved".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
                                    tvBadge.setTextColor(Color.parseColor("#10B981"));
                                } else if ("Cancelled".equalsIgnoreCase(status) || "Declined".equalsIgnoreCase(status) || "Rejected".equalsIgnoreCase(status)) {
                                    tvBadge.setTextColor(Color.parseColor("#EF4444"));
                                } else {
                                    tvBadge.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_orange));
                                }
                            }

                            if (tvStyle != null) {
                                String brand = item.getBrand() != null && !item.getBrand().isEmpty() ? item.getBrand() : "LensMatch";
                                String style = item.getFrameStyle() != null && !item.getFrameStyle().isEmpty() ? item.getFrameStyle() : "Eyewear";
                                tvStyle.setText(brand + " · " + style);
                            }

                            if (tvPrice != null) {
                                tvPrice.setText(item.getFormattedPrice());
                            }

                            if (tvDate != null) {
                                tvDate.setText("Requested: " + item.getFormattedDate());
                            }

                            if (tvHint != null) {
                                if ("Confirmed".equalsIgnoreCase(status) || "Approved".equalsIgnoreCase(status)) {
                                    tvHint.setText("✓ Confirmed! Held at Franselle Optical Clinic for your visit.");
                                } else if ("Completed".equalsIgnoreCase(status)) {
                                    tvHint.setText("✓ Fitting completed at Franselle Optical Clinic.");
                                } else if ("Cancelled".equalsIgnoreCase(status) || "Declined".equalsIgnoreCase(status) || "Rejected".equalsIgnoreCase(status)) {
                                    tvHint.setText("This reservation was cancelled.");
                                } else {
                                    tvHint.setText("Clinic staff is reviewing your reservation for in-store fitting.");
                                }
                            }

                            if (ivImage != null) {
                                if (item.getImageUrl() != null && !item.getImageUrl().trim().isEmpty()) {
                                    ivImage.setImageTintList(null);
                                    Glide.with(requireContext())
                                            .load(item.getImageUrl())
                                            .placeholder(R.drawable.ic_eyeglasses)
                                            .error(R.drawable.ic_eyeglasses)
                                            .into(ivImage);
                                } else {
                                    ivImage.setImageResource(R.drawable.ic_eyeglasses);
                                }
                            }

                            card.setOnClickListener(v -> {
                                Intent intent = new Intent(requireContext(), ReservationDetailActivity.class);
                                intent.putExtra("reservation", item);
                                startActivity(intent);
                            });

                            layoutReservationsContainer.addView(card);
                        }
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded() || getContext() == null) return;
                if (cardNoReservation != null) cardNoReservation.setVisibility(View.VISIBLE);
                if (scrollReservationsContainer != null) scrollReservationsContainer.setVisibility(View.GONE);
                if (tvReservationsCount != null) {
                    tvReservationsCount.setText("Check your reservations history");
                }
            }
        });
    }

    private void loadAnnouncements() {
        if (layoutAnnouncementsContainer == null || !isAdded() || getContext() == null) return;

        layoutAnnouncementsContainer.removeAllViews();

        ProgressBar pb = new ProgressBar(requireContext());
        pb.setIndeterminateTintList(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_orange)));
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
                            tvBadge.setBackgroundTintList(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_orange)));
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
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onError(String errorMessage) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
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
