package com.unity3d.player;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.view.ViewCompat;

import com.google.androidgamesdk.GameActivity;
import com.lensmatch.bridge.UnityBridge;
import com.lensmatch.mobile.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UnityPlayerGameActivity extends GameActivity
        implements IUnityPlayerLifecycleEvents, IUnityPermissionRequestSupport, IUnityPlayerSupport {

    class GameActivitySurfaceView extends InputEnabledSurfaceView {
        GameActivity mGameActivity;
        public GameActivitySurfaceView(GameActivity activity) {
            super(activity);
            mGameActivity = activity;
        }

        @Override
        public boolean onCapturedPointerEvent(MotionEvent event) {
            return mGameActivity.onTouchEvent(event);
        }
    }

    protected UnityPlayerForGameActivity mUnityPlayer;

    private String mCurrentShape = "wayfarer";
    private String mCurrentColor = "black";
    private ArrayList<String> recommendedFrames = new ArrayList<>();
    private int selectedTabIndex = 0;

    private LinearLayout layoutFramesUnity;
    private LinearLayout layoutColorsUnity;
    private TextView tvTabRecommended;
    private TextView tvTabAllFrames;
    private View indicatorRecommended;
    private View indicatorAllFrames;

    // Background sync to ensure the initial frame is attached as soon as ARCore detects the face
    private final Handler mFrameSyncHandler = new Handler(Looper.getMainLooper());
    private final Runnable mFrameSyncRunnable = new Runnable() {
        @Override
        public void run() {
            if (!"none".equals(mCurrentShape)) {
                updateFrame();
            }
            mFrameSyncHandler.postDelayed(this, 1200);
        }
    };

    private final List<String> allFrames = Arrays.asList(
            "None", "Wayfarer", "Rectangle", "Square", "Cat Eye",
            "Round", "Aviator", "Geometric", "Browline", "Oval"
    );

    private static class ColorOption {
        final String label;
        final String code; // "black", "gold", "silver"
        ColorOption(String label, String code) {
            this.label = label;
            this.code = code;
        }
    }

    private final List<ColorOption> colorOptions = Arrays.asList(
            new ColorOption("Original", "black"),
            new ColorOption("Gold",     "gold"),
            new ColorOption("Silver",   "silver")
    );

    protected String updateUnityCommandLineArguments(String cmdLine) {
        return cmdLine;
    }

    static {
        System.loadLibrary("game");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(true);
        }

        if (getIntent().hasExtra("recommendedFrames")) {
            recommendedFrames = getIntent().getStringArrayListExtra("recommendedFrames");
        }
        if (getIntent().hasExtra("frameStyle")) {
            mCurrentShape = normalizeShapeName(getIntent().getStringExtra("frameStyle"));
        } else if (recommendedFrames != null && !recommendedFrames.isEmpty()) {
            mCurrentShape = normalizeShapeName(recommendedFrames.get(0));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 101);
            }
        }

        // Inflate and overlay custom UI layout matching LensMatch Try 6 reference
        LayoutInflater inflater = getLayoutInflater();
        View overlayView = inflater.inflate(R.layout.activity_ar_overlay, null);
        addContentView(overlayView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        overlayView.bringToFront();

        setupNativeControls(overlayView);
    }

    private String normalizeShapeName(String raw) {
        if (raw == null) return "wayfarer";
        String lower = raw.trim().toLowerCase();
        if (lower.contains("none")) return "none";
        if (lower.contains("aviator")) return "aviator";
        if (lower.contains("bowline") || lower.contains("browline")) return "bowline";
        if (lower.contains("cat")) return "cateye";
        if (lower.contains("geometric")) return "geometric";
        if (lower.contains("oval")) return "oval";
        if (lower.contains("rect")) return "rectangle";
        if (lower.contains("round")) return "round";
        if (lower.contains("square")) return "square";
        return "wayfarer";
    }

    private void setupNativeControls(View overlay) {
        ImageView btnBack = overlay.findViewById(R.id.btn_back_unity_ar);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
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
    }

    private void updateTabs() {
        int gold = Color.parseColor("#D4AF37");
        int grey = Color.parseColor("#AFAFAF");
        if (tvTabRecommended == null || tvTabAllFrames == null) return;
        if (selectedTabIndex == 0) {
            tvTabRecommended.setTextColor(gold);
            tvTabAllFrames.setTextColor(grey);
            if (indicatorRecommended != null) indicatorRecommended.setBackgroundColor(gold);
            if (indicatorAllFrames   != null) indicatorAllFrames.setBackgroundColor(Color.TRANSPARENT);
        } else {
            tvTabRecommended.setTextColor(grey);
            tvTabAllFrames.setTextColor(gold);
            if (indicatorRecommended != null) indicatorRecommended.setBackgroundColor(Color.TRANSPARENT);
            if (indicatorAllFrames   != null) indicatorAllFrames.setBackgroundColor(gold);
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

            boolean sel = normalizeShapeName(frame).equalsIgnoreCase(mCurrentShape);
            circleBg.setBackgroundResource(sel ? R.drawable.bg_ar_circle_selected : R.drawable.bg_ar_circle_unselected);
            label.setTextColor(sel ? Color.parseColor("#D4AF37") : Color.WHITE);

            item.setOnClickListener(v -> {
                mCurrentShape = normalizeShapeName(frame);
                setupFrameCircles();
                updateFrame();
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

            boolean sel = opt.code.equalsIgnoreCase(mCurrentColor);
            chip.setBackgroundResource(sel ? R.drawable.bg_confidence_chip : 0);
            if (!sel) chip.setBackgroundColor(Color.parseColor("#141414"));
            chip.setTextColor(sel ? Color.parseColor("#D4AF37") : Color.parseColor("#AFAFAF"));

            chip.setOnClickListener(v -> {
                mCurrentColor = opt.code;
                setupColorChips();
                updateFrame();
            });

            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p.setMargins(0, 0, 16, 0);
            layoutColorsUnity.addView(chip, p);
        }
    }

    private void updateFrame() {
        try {
            if ("none".equals(mCurrentShape)) {
                UnityBridge.clearFrame();
            } else {
                String message;
                if ("black".equals(mCurrentColor)) {
                    message = mCurrentShape;
                } else {
                    message = mCurrentColor + "_" + mCurrentShape;
                }
                android.util.Log.d("LensMatchDebug", "Sending showFrame: " + message);
                UnityBridge.showFrame(message);
            }
        } catch (Throwable t) {
            android.util.Log.w("UnityActivity", "Error in updateFrame: " + t.getMessage());
        }
    }

    @Override
    public UnityPlayerForGameActivity getUnityPlayerConnection() {
        return mUnityPlayer;
    }

    private void applyInsetListener(SurfaceView surfaceView) {
        surfaceView.getViewTreeObserver().addOnGlobalLayoutListener(
                () -> onApplyWindowInsets(surfaceView, ViewCompat.getRootWindowInsets(getWindow().getDecorView())));
    }

    @Override
    protected InputEnabledSurfaceView createSurfaceView() {
        return new GameActivitySurfaceView(this);
    }

    @Override
    protected void onCreateSurfaceView() {
        super.onCreateSurfaceView();
        FrameLayout frameLayout = findViewById(contentViewId);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q)
            applyInsetListener(mSurfaceView);

        mSurfaceView.setId(UnityPlayerForGameActivity.getUnityViewIdentifier(this));

        String cmdLine = updateUnityCommandLineArguments(getIntent().getStringExtra("unity"));
        getIntent().putExtra("unity", cmdLine);
        mUnityPlayer = new UnityPlayerForGameActivity(this, frameLayout, mSurfaceView, this);
    }

    @Override
    public void onUnityPlayerUnloaded() {
        moveTaskToBack(true);
    }

    @Override
    public void onUnityPlayerQuitted() {
    }

    // Quit Unity - process Java callback before native
    @Override
    protected void onDestroy() {
        mFrameSyncHandler.removeCallbacksAndMessages(null);
        if (mUnityPlayer != null) {
            mUnityPlayer.destroy();
        }
        super.onDestroy();
    }

    // Stop Unity - process Java callback before native
    @Override
    protected void onStop() {
        mFrameSyncHandler.removeCallbacksAndMessages(null);
        if (mUnityPlayer != null) {
            mUnityPlayer.onStop();
        }
        super.onStop();
    }

    // Start Unity - process Java callback before native
    @Override
    protected void onStart() {
        if (mUnityPlayer != null) {
            mUnityPlayer.onStart();
        }
        super.onStart();
    }

    // Pause Unity - process Java callback before native to immediately release camera and GPU resources
    @Override
    protected void onPause() {
        mFrameSyncHandler.removeCallbacksAndMessages(null);
        if (mUnityPlayer != null) {
            mUnityPlayer.onPause();
        }
        super.onPause();
    }

    // Resume Unity - process Java callback before native
    @Override
    protected void onResume() {
        if (mUnityPlayer != null) {
            mUnityPlayer.onResume();
        }
        super.onResume();

        // Trigger immediate frame updates as ARCore acquires face tracking
        mFrameSyncHandler.removeCallbacksAndMessages(null);
        mFrameSyncHandler.postDelayed(this::updateFrame, 300);
        mFrameSyncHandler.postDelayed(this::updateFrame, 800);
        mFrameSyncHandler.postDelayed(this::updateFrame, 1500);
        mFrameSyncHandler.postDelayed(mFrameSyncRunnable, 2200);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (mUnityPlayer != null) {
            mUnityPlayer.configurationChanged(newConfig);
        }
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (mUnityPlayer != null) {
            mUnityPlayer.windowFocusChanged(hasFocus);
        }
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (mUnityPlayer != null) {
            mUnityPlayer.newIntent(intent);
        }

        if (intent.hasExtra("frameStyle")) {
            mCurrentShape = normalizeShapeName(intent.getStringExtra("frameStyle"));
        }
        if (intent.hasExtra("recommendedFrames")) {
            recommendedFrames = intent.getStringArrayListExtra("recommendedFrames");
        }
        setupFrameCircles();
        updateFrame();
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void requestPermissions(PermissionRequest request) {
        if (mUnityPlayer != null) {
            mUnityPlayer.addPermissionRequest(request);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (mUnityPlayer != null) {
            mUnityPlayer.permissionResponse(this, requestCode, permissions, grantResults);
        }
    }
}
