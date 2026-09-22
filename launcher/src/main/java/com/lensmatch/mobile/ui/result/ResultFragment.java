package com.lensmatch.mobile.ui.result;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.AppState;
import com.lensmatch.mobile.service.GeminiService;
import com.lensmatch.mobile.utils.FaceShapeDetector;

import java.io.File;
import java.util.ArrayList;

public class ResultFragment extends Fragment {
    private ImageView ivFaceCrop;
    private TextView tvDetectedShape;
    private TextView tvConfidenceMatch;
    private TextView tvAiDescription;
    private TextView tvRecommendedReason;
    private ChipGroup chipGroupRecommended;
    private ChipGroup chipGroupAvoided;
    private MaterialButton btnTryFramesOn;
    private View cardBorderlineNotice;
    private TextView tvBorderlineNotes;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;

    private String currentShape = "Oval";
    private static Bitmap cachedFaceBitmap = null;
    private static String cachedImagePath = null;
    private static FaceShapeDetector.ShapeRecommendation cachedRecommendation = null;
    private static String cachedRecommendationShape = null;
    private final ArrayList<String> recommendedList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_result, container, false);

        swipeRefresh = root.findViewById(R.id.swipe_refresh_result);
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeColors(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_orange),
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_orange_dark)
            );
            swipeRefresh.setOnRefreshListener(this::refreshResultData);
        }

        ivFaceCrop = root.findViewById(R.id.iv_face_crop);
        tvDetectedShape = root.findViewById(R.id.tv_detected_shape);
        tvConfidenceMatch = root.findViewById(R.id.tv_confidence_match);
        cardBorderlineNotice = root.findViewById(R.id.card_borderline_notice);
        tvBorderlineNotes = root.findViewById(R.id.tv_borderline_notes);
        tvAiDescription = root.findViewById(R.id.tv_ai_description);
        tvRecommendedReason = root.findViewById(R.id.tv_recommended_reason);
        chipGroupRecommended = root.findViewById(R.id.chip_group_recommended);
        chipGroupAvoided = root.findViewById(R.id.chip_group_avoided);
        btnTryFramesOn = root.findViewById(R.id.btn_try_frames_on);

        btnTryFramesOn.setOnClickListener(v -> {
            startUnityAR(recommendedList.isEmpty() ? "Wayfarer" : recommendedList.get(0), recommendedList);
        });

        updateUI();

        return root;
    }

    private void refreshResultData() {
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(true);
        }
        com.lensmatch.mobile.service.FirestoreService.getUserScanHistory(new com.lensmatch.mobile.service.FirestoreService.Callback<java.util.List<com.lensmatch.mobile.data.ScanModel>>() {
            @Override
            public void onSuccess(java.util.List<com.lensmatch.mobile.data.ScanModel> scans) {
                if (scans != null && !scans.isEmpty()) {
                    AppState.getInstance().syncScanHistory(scans);
                }
                if (isAdded()) {
                    updateUI();
                }
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onError(String errorMessage) {
                if (isAdded()) {
                    updateUI();
                }
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }
        });
    }

    /** Launches the Unity AR activity for frame try-on. */
    private void startUnityAR(String frameStyle, ArrayList<String> frames) {
        if (getContext() == null) return;
        AppState.getInstance().setLastActiveTab(R.id.nav_result);
        if (btnTryFramesOn != null) {
            btnTryFramesOn.setEnabled(false);
            btnTryFramesOn.setText("Opening Frame Preview...");
        }
        Intent intent = new Intent(requireContext(), com.unity3d.player.UnityPlayerGameActivity.class);
        intent.putExtra("frameStyle", frameStyle);
        intent.putStringArrayListExtra("recommendedFrames", frames);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (btnTryFramesOn != null) {
            btnTryFramesOn.setEnabled(true);
            btnTryFramesOn.setText("Preview Frame");
        }
        updateUI();
    }

    public void updateUI() {
        if (getContext() == null || tvDetectedShape == null) return;

        AppState state = AppState.getInstance();
        currentShape = state.getLastDetectedShape();
        int confidencePct = (int) (state.getLastConfidence() * 100);

        tvDetectedShape.setText(currentShape);
        tvConfidenceMatch.setText(confidencePct + "% Match");

        boolean isBorderline = state.getLastIsBorderline();
        String notes = state.getLastNotes();
        if (cardBorderlineNotice != null && tvBorderlineNotes != null) {
            if (notes != null && !notes.trim().isEmpty()) {
                tvBorderlineNotes.setText(notes);
                cardBorderlineNotice.setVisibility(View.VISIBLE);
            } else if (isBorderline) {
                String runnerUp = state.getLastRunnerUpShape();
                tvBorderlineNotes.setText("Borderline Result: Your facial features sit between " + currentShape + " and " + (runnerUp != null ? runnerUp : "Round") + ".");
                cardBorderlineNotice.setVisibility(View.VISIBLE);
            } else {
                cardBorderlineNotice.setVisibility(View.GONE);
            }
        }

        String imagePath = state.getLastImagePath();
        if (imagePath != null) {
            if (cachedFaceBitmap != null && imagePath.equals(cachedImagePath)) {
                if (ivFaceCrop != null) {
                    ivFaceCrop.setImageBitmap(cachedFaceBitmap);
                }
            } else {
                loadFaceImageAsync(imagePath);
            }
        }

        if (cachedRecommendation != null && currentShape != null && currentShape.equalsIgnoreCase(cachedRecommendationShape)) {
            displayRecommendations(cachedRecommendation);
        } else if (currentShape != null) {
            fetchRecommendations(currentShape);
        }
    }

    private void loadFaceImageAsync(String imagePath) {
        if (imagePath == null || !new File(imagePath).exists() || ivFaceCrop == null) return;
        new Thread(() -> {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(imagePath, options);

                int targetSize = 400;
                int scale = 1;
                if (options.outHeight > targetSize || options.outWidth > targetSize) {
                    int halfH = options.outHeight / 2;
                    int halfW = options.outWidth / 2;
                    while ((halfH / scale) >= targetSize && (halfW / scale) >= targetSize) {
                        scale *= 2;
                    }
                }

                options.inJustDecodeBounds = false;
                options.inSampleSize = Math.max(1, scale);
                Bitmap bmp = BitmapFactory.decodeFile(imagePath, options);
                if (bmp != null) {
                    cachedFaceBitmap = bmp;
                    cachedImagePath = imagePath;
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (isAdded() && ivFaceCrop != null) {
                                ivFaceCrop.setImageBitmap(bmp);
                            }
                        });
                    }
                }
            } catch (Throwable ignored) {}
        }).start();
    }

    private void displayRecommendations(FaceShapeDetector.ShapeRecommendation rec) {
        if (rec == null || !isAdded() || getContext() == null) return;
        if (tvAiDescription != null) {
            tvAiDescription.setText(rec.explanation);
        }

        if (tvRecommendedReason != null) {
            tvRecommendedReason.setText("Flattering frame silhouettes that balance your unique facial geometry.");
        }

        // tvAvoidedReason has been removed from the layout, so we no longer try to set it.

        chipGroupRecommended.removeAllViews();
        chipGroupAvoided.removeAllViews();
        recommendedList.clear();

        for (String item : rec.primary) {
            recommendedList.add(item);
            Chip chip = new Chip(requireContext());
            chip.setText(item);
            chip.setChipIconResource(R.drawable.ic_check);
            chip.setChipIconTint(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_orange)));
            chip.setChipIconSize(36f);
            chip.setIconStartPadding(12f);
            chip.setChipBackgroundColorResource(R.color.primary_orange_tint);
            chip.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_primary));
            chip.setTextSize(13f);
            chip.setTypeface(null, Typeface.BOLD);
            chip.setChipStrokeColor(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_orange)));
            chip.setChipStrokeWidth(3f);
            chip.setChipCornerRadius(32f);
            chip.setOnClickListener(v -> startUnityAR(item, recommendedList));
            chipGroupRecommended.addView(chip);
        }

        for (String item : rec.secondary) {
            Chip chip = new Chip(requireContext());
            chip.setText(item);
            chip.setChipIconResource(R.drawable.ic_close);
            chip.setChipIconTint(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary)));
            chip.setChipIconSize(32f);
            chip.setIconStartPadding(12f);
            chip.setChipBackgroundColor(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.surface_white)));
            chip.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary));
            chip.setTextSize(13f);
            chip.setChipStrokeColor(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.border_light)));
            chip.setChipStrokeWidth(2f);
            chip.setChipCornerRadius(32f);
            chipGroupAvoided.addView(chip);
        }
    }

    private void fetchRecommendations(String shape) {
        // Immediately display built-in optical recommendations so screen is NEVER empty
        FaceShapeDetector.ShapeRecommendation immediateFallback = FaceShapeDetector.getRecommendation(shape);
        if (immediateFallback != null) {
            displayRecommendations(immediateFallback);
        }

        GeminiService.fetchRecommendations(shape, new GeminiService.Callback() {
            @Override
            public void onSuccess(FaceShapeDetector.ShapeRecommendation rec) {
                if (!isAdded() || getContext() == null) return;
                cachedRecommendation = rec;
                cachedRecommendationShape = shape;
                displayRecommendations(rec);
            }

            @Override
            public void onError(String message) {
                // If Gemini encounters network failure or rate limit, keep the reliable built-in recommendation
                if (!isAdded() || getContext() == null) return;
                if (cachedRecommendation == null && immediateFallback != null) {
                    displayRecommendations(immediateFallback);
                }
            }
        });
    }
}
