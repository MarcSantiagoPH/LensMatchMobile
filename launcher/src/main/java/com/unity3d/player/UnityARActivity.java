package com.unity3d.player;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.lensmatch.mobile.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UnityARActivity extends AppCompatActivity implements IUnityPlayerLifecycleEvents {
    private static final String TAG = "UnityARActivity";

    private UnityPlayerForActivityOrService mUnityPlayer;

    private String selectedFrameStyle = "Wayfarer";
    private String selectedColorVariant = "Black"; // "Black", "Gold", "Silver"
    private ArrayList<String> recommendedFrames = new ArrayList<>();
    private int selectedTabIndex = 0;

    private LinearLayout layoutFramesUnity;
    private LinearLayout layoutColorsUnity;
    private TextView tvTabRecommended;
    private TextView tvTabAllFrames;
    private View indicatorRecommended;
    private View indicatorAllFrames;

    private final List<String> allFrames = Arrays.asList(
            "None", "Wayfarer", "Rectangle", "Square", "Cat Eye",
            "Round", "Aviator", "Geometric", "Browline", "Oval"
    );

    private static class ColorOption {
        final String label;
        final String prefix;
        ColorOption(String label, String prefix) {
            this.label = label;
            this.prefix = prefix;
        }
    }

    private final List<ColorOption> colorOptions = Arrays.asList(
            new ColorOption("Black",  ""),
            new ColorOption("Gold",   "Gold "),
            new ColorOption("Silver", "Silver ")
    );

    /**
     * Builds the exact Unity FrameManager prefab name:
     * "Wayfarer" + "Gold " -> "Gold Wayfarer Frame"
     * "Cat Eye"  + ""      -> "CatEye Frame"
     */
    private static String toUnityFrameName(String shape, String prefix) {
        if ("None".equalsIgnoreCase(shape)) return "None";
        String base;
        switch (shape) {
            case "Cat Eye":   base = "CatEye Frame";    break;
            case "Browline":  base = "Browline Frame";  break;
            case "Geometric": base = "Geometric Frame"; break;
            case "Aviator":   base = "Aviator Frame";   break;
            case "Oval":      base = "Oval Frame";      break;
            case "Round":     base = "Round Frame";     break;
            case "Square":    base = "Square Frame";    break;
            case "Rectangle": base = "Rectangle Frame"; break;
            case "Wayfarer":
            default:          base = "Wayfarer Frame";  break;
        }
        return prefix + base;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().hasExtra("recommendedFrames")) {
            recommendedFrames = getIntent().getStringArrayListExtra("recommendedFrames");
        }
        if (getIntent().hasExtra("frameStyle")) {
            selectedFrameStyle = getIntent().getStringExtra("frameStyle");
        } else if (recommendedFrames != null && !recommendedFrames.isEmpty()) {
            selectedFrameStyle = recommendedFrames.get(0);
        }

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);
        }

        // Initialize Unity Embedded Player
        mUnityPlayer = new UnityPlayerForActivityOrService(this, this);

        FrameLayout rootLayout = mUnityPlayer.getFrameLayout();
        if (rootLayout == null) {
            rootLayout = new FrameLayout(this);
            View unityView = mUnityPlayer.getView();
            if (unityView != null) {
                rootLayout.addView(unityView, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));
            }
        }

        // Inflate Native Overlay UI on top of Unity AR surface
        View overlay = LayoutInflater.from(this).inflate(R.layout.activity_ar_overlay, rootLayout, false);
        rootLayout.addView(overlay);

        setContentView(rootLayout);

        setupNativeControls(overlay, rootLayout);
    }

    private void setupNativeControls(View overlay, FrameLayout rootLayout) {
        ImageView btnBack = overlay.findViewById(R.id.btn_back_unity_ar);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnContinue = overlay.findViewById(R.id.btn_continue_unity_ar);
        if (btnContinue != null) {
            btnContinue.setOnClickListener(v -> {
                if (selectedFrameStyle == null || "none".equalsIgnoreCase(selectedFrameStyle.trim())) {
                    android.widget.Toast.makeText(this, "Please select a frame shape first", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                String targetStyle = toCatalogStyleName(selectedFrameStyle);
                com.lensmatch.mobile.data.AppState.getInstance().setPendingCatalogStyle(targetStyle);
                Intent intent = new Intent(this, com.lensmatch.mobile.ui.catalog.FrameReservationNoticeActivity.class);
                intent.putExtra("frameStyle", targetStyle);
                intent.putExtra("colorVariant", selectedColorVariant);
                if (recommendedFrames != null) {
                    intent.putStringArrayListExtra("recommendedFrames", recommendedFrames);
                }
                startActivity(intent);
            });
        }

        LinearLayout tabRec = overlay.findViewById(R.id.tab_recommended_unity);
        LinearLayout tabAll = overlay.findViewById(R.id.tab_all_frames_unity);
        tvTabRecommended    = overlay.findViewById(R.id.tv_tab_recommended_unity);
        tvTabAllFrames      = overlay.findViewById(R.id.tv_tab_all_frames_unity);
        indicatorRecommended = overlay.findViewById(R.id.indicator_recommended_unity);
        indicatorAllFrames  = overlay.findViewById(R.id.indicator_all_frames_unity);
        layoutFramesUnity   = overlay.findViewById(R.id.layout_frames_unity);
        layoutColorsUnity   = overlay.findViewById(R.id.layout_colors_unity);

        if (tabRec != null) {
            tabRec.setOnClickListener(v -> {
                selectedTabIndex = 0;
                updateTabs();
                setupFrameCircles();
            });
        }

        if (tabAll != null) {
            tabAll.setOnClickListener(v -> {
                selectedTabIndex = 1;
                updateTabs();
                setupFrameCircles();
            });
        }

        updateTabs();
        setupFrameCircles();
        setupColorChips();

        // Send initial frame commands to Unity
        sendFrameToUnity(selectedFrameStyle, selectedColorVariant);
        if (rootLayout != null) {
            rootLayout.postDelayed(() -> sendFrameToUnity(selectedFrameStyle, selectedColorVariant), 600);
            rootLayout.postDelayed(() -> sendFrameToUnity(selectedFrameStyle, selectedColorVariant), 1500);
            rootLayout.postDelayed(() -> sendFrameToUnity(selectedFrameStyle, selectedColorVariant), 3000);
        }
    }

    private void updateTabs() {
        int orange = Color.parseColor("#F4A261");
        int darkText = Color.BLACK;
        int mutedText = Color.parseColor("#444444");
        if (tvTabRecommended == null || tvTabAllFrames == null) return;
        if (selectedTabIndex == 0) {
            tvTabRecommended.setTextColor(darkText);
            tvTabRecommended.setTypeface(null, Typeface.BOLD);
            tvTabAllFrames.setTextColor(mutedText);
            tvTabAllFrames.setTypeface(null, Typeface.NORMAL);
            if (indicatorRecommended != null) indicatorRecommended.setBackgroundColor(orange);
            if (indicatorAllFrames   != null) indicatorAllFrames.setBackgroundColor(Color.TRANSPARENT);
        } else {
            tvTabRecommended.setTextColor(mutedText);
            tvTabRecommended.setTypeface(null, Typeface.NORMAL);
            tvTabAllFrames.setTextColor(darkText);
            tvTabAllFrames.setTypeface(null, Typeface.BOLD);
            if (indicatorRecommended != null) indicatorRecommended.setBackgroundColor(Color.TRANSPARENT);
            if (indicatorAllFrames   != null) indicatorAllFrames.setBackgroundColor(orange);
        }
    }

    private void setupFrameCircles() {
        if (layoutFramesUnity == null) return;
        layoutFramesUnity.removeAllViews();

        List<String> display;
        if (selectedTabIndex == 0 && recommendedFrames != null && !recommendedFrames.isEmpty()) {
            display = new ArrayList<>();
            display.add("None");
            display.addAll(recommendedFrames);
        } else {
            display = allFrames;
        }

        LayoutInflater inf = LayoutInflater.from(this);
        for (String frame : display) {
            View item = inf.inflate(R.layout.item_ar_frame_circle, layoutFramesUnity, false);
            FrameLayout circleBg = item.findViewById(R.id.frame_circle_bg);
            ImageView icon       = item.findViewById(R.id.iv_frame_icon);
            TextView label       = item.findViewById(R.id.tv_frame_label);

            label.setText(frame);
            icon.setImageResource("None".equalsIgnoreCase(frame) ? R.drawable.ic_none : R.drawable.ic_eyeglasses);
            icon.setColorFilter(Color.BLACK);

            boolean sel = frame.equalsIgnoreCase(selectedFrameStyle);
            circleBg.setBackgroundResource(sel ? R.drawable.bg_ar_circle_selected : R.drawable.bg_ar_circle_unselected);
            label.setTextColor(Color.BLACK);
            label.setTypeface(null, sel ? Typeface.BOLD : Typeface.NORMAL);

            item.setOnClickListener(v -> {
                selectedFrameStyle = frame;
                setupFrameCircles();
                sendFrameToUnity(selectedFrameStyle, selectedColorVariant);
            });
            layoutFramesUnity.addView(item);
        }
    }

    private void setupColorChips() {
        if (layoutColorsUnity == null) return;
        layoutColorsUnity.removeAllViews();

        for (ColorOption opt : colorOptions) {
            TextView chip = new TextView(this);
            chip.setText(opt.label);
            chip.setPadding(32, 16, 32, 16);
            chip.setTextSize(13);

            boolean sel = opt.label.equalsIgnoreCase(selectedColorVariant);
            chip.setBackgroundResource(sel ? R.drawable.bg_confidence_chip : R.drawable.bg_color_chip_unselected);
            chip.setTextColor(Color.BLACK);
            chip.setTypeface(null, sel ? Typeface.BOLD : Typeface.NORMAL);

            chip.setOnClickListener(v -> {
                selectedColorVariant = opt.label;
                setupColorChips();
                sendFrameToUnity(selectedFrameStyle, selectedColorVariant);
            });

            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p.setMargins(0, 0, 16, 0);
            layoutColorsUnity.addView(chip, p);
        }
    }

    private void sendFrameToUnity(String shape, String colorVariant) {
        if ("None".equalsIgnoreCase(shape)) {
            safeUnitySend("FrameManager", "HideAllFrames", "");
            safeUnitySend("ARManager",    "SwitchFrame",   "None");
            return;
        }
        String prefix = "";
        for (ColorOption c : colorOptions) {
            if (c.label.equalsIgnoreCase(colorVariant)) { prefix = c.prefix; break; }
        }
        String name = toUnityFrameName(shape, prefix);
        safeUnitySend("FrameManager", "ShowFrame",   name);
        safeUnitySend("ARManager",    "SwitchFrame", name);
    }

    private void safeUnitySend(String obj, String method, String param) {
        try { UnityPlayer.UnitySendMessage(obj, method, param); } catch (Throwable ignored) {}
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mUnityPlayer != null) {
            mUnityPlayer.onStart();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mUnityPlayer != null) {
            mUnityPlayer.onStop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mUnityPlayer != null) {
            mUnityPlayer.onResume();
            mUnityPlayer.resume();
            mUnityPlayer.windowFocusChanged(true);
        }
        sendFrameToUnity(selectedFrameStyle, selectedColorVariant);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mUnityPlayer != null) {
            mUnityPlayer.onPause();
            mUnityPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUnityPlayer != null) {
            mUnityPlayer.destroy();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mUnityPlayer != null) {
            mUnityPlayer.configurationChanged(newConfig);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (mUnityPlayer != null) {
            mUnityPlayer.windowFocusChanged(hasFocus);
        }
    }

    @Override
    public void onUnityPlayerUnloaded() {
        finish();
    }

    @Override
    public void onUnityPlayerQuitted() {
        finish();
    }

    private String toCatalogStyleName(String raw) {
        if (raw == null) return "Wayfarer";
        String lower = raw.trim().toLowerCase();
        if (lower.contains("aviator")) return "Aviator";
        if (lower.contains("bowline") || lower.contains("browline")) return "Browline";
        if (lower.contains("cat")) return "Cat-Eye";
        if (lower.contains("geometric")) return "Geometric";
        if (lower.contains("oval")) return "Oval";
        if (lower.contains("rect")) return "Rectangle";
        if (lower.contains("round")) return "Round";
        if (lower.contains("square")) return "Square";
        return "Wayfarer";
    }
}
