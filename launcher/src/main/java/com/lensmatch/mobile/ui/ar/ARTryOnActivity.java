package com.lensmatch.mobile.ui.ar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.lensmatch.mobile.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ARTryOnActivity extends AppCompatActivity {
    private static final String TAG = "ARTryOnActivity";
    private static final int PERMISSION_REQUEST_CAMERA = 102;

    private PreviewView viewFinderAr;
    private ARFrameOverlayView arFrameOverlay;
    private LinearLayout layoutFramesAr;
    private LinearLayout layoutColorsAr;

    private TextView tvTabRecommended;
    private TextView tvTabAllFrames;
    private View indicatorRecommended;
    private View indicatorAllFrames;

    private FaceDetector faceDetector;
    private ExecutorService cameraExecutor;

    private String selectedFrameStyle = "Wayfarer";
    private String selectedFrameColor = "#141414";
    private ArrayList<String> recommendedFrames = new ArrayList<>();
    private int selectedTabIndex = 0; // 0: Recommended, 1: All Frames

    private final List<String> allFrames = Arrays.asList(
            "None", "Wayfarer", "Rectangle", "Square", "Cat Eye", "Round", "Aviator", "Geometric", "Browline", "Oval"
    );

    private final List<ColorOption> colorOptions = Arrays.asList(
            new ColorOption("Original", "#141414", Color.parseColor("#141414")),
            new ColorOption("Gold", "#FFD700", Color.parseColor("#FFD700")),
            new ColorOption("Silver", "#C0C0C0", Color.parseColor("#C0C0C0"))
    );

    private static class ColorOption {
        final String name;
        final String hex;
        final int colorInt;

        ColorOption(String name, String hex, int colorInt) {
            this.name = name;
            this.hex = hex;
            this.colorInt = colorInt;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar_tryon);

        if (getIntent().hasExtra("recommendedFrames")) {
            recommendedFrames = getIntent().getStringArrayListExtra("recommendedFrames");
        }

        if (getIntent().hasExtra("frameStyle")) {
            selectedFrameStyle = getIntent().getStringExtra("frameStyle");
        } else if (recommendedFrames != null && !recommendedFrames.isEmpty()) {
            selectedFrameStyle = recommendedFrames.get(0);
        }

        viewFinderAr = findViewById(R.id.view_finder_ar);
        arFrameOverlay = findViewById(R.id.ar_frame_overlay);
        layoutFramesAr = findViewById(R.id.layout_frames_ar);
        layoutColorsAr = findViewById(R.id.layout_colors_ar);

        ImageView btnBack = findViewById(R.id.btn_back_ar);
        btnBack.setOnClickListener(v -> finish());

        LinearLayout tabRecommended = findViewById(R.id.tab_recommended);
        LinearLayout tabAllFrames = findViewById(R.id.tab_all_frames);
        tvTabRecommended = findViewById(R.id.tv_tab_recommended);
        tvTabAllFrames = findViewById(R.id.tv_tab_all_frames);
        indicatorRecommended = findViewById(R.id.indicator_recommended);
        indicatorAllFrames = findViewById(R.id.indicator_all_frames);

        tabRecommended.setOnClickListener(v -> {
            selectedTabIndex = 0;
            updateTabs();
            setupFrameCircles();
        });

        tabAllFrames.setOnClickListener(v -> {
            selectedTabIndex = 1;
            updateTabs();
            setupFrameCircles();
        });

        arFrameOverlay.setFrameStyle(selectedFrameStyle);
        arFrameOverlay.setFrameColor(selectedFrameColor);

        updateTabs();
        setupFrameCircles();
        setupColorChips();

        cameraExecutor = Executors.newSingleThreadExecutor();

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .build();
        faceDetector = FaceDetection.getClient(options);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinderAr.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::processImageProxy);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Binding camera preview failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @androidx.camera.core.ExperimentalGetImage
    private void processImageProxy(ImageProxy imageProxy) {
        if (imageProxy.getImage() != null) {
            InputImage inputImage = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

            faceDetector.process(inputImage)
                    .addOnSuccessListener(faces -> {
                        runOnUiThread(() -> arFrameOverlay.updateFaces(faces, inputImage.getWidth(), inputImage.getHeight()));
                        imageProxy.close();
                    })
                    .addOnFailureListener(e -> imageProxy.close());
        } else {
            imageProxy.close();
        }
    }

    private void updateTabs() {
        if (selectedTabIndex == 0) {
            tvTabRecommended.setTextColor(getColor(R.color.accentGold));
            tvTabAllFrames.setTextColor(getColor(R.color.textSecondary));
            indicatorRecommended.setBackgroundColor(getColor(R.color.accentGold));
            indicatorAllFrames.setBackgroundColor(getColor(R.color.transparent));
        } else {
            tvTabRecommended.setTextColor(getColor(R.color.textSecondary));
            tvTabAllFrames.setTextColor(getColor(R.color.accentGold));
            indicatorRecommended.setBackgroundColor(getColor(R.color.transparent));
            indicatorAllFrames.setBackgroundColor(getColor(R.color.accentGold));
        }
    }

    private void setupFrameCircles() {
        layoutFramesAr.removeAllViews();
        List<String> displayFrames;

        if (selectedTabIndex == 0 && recommendedFrames != null && !recommendedFrames.isEmpty()) {
            displayFrames = new ArrayList<>();
            displayFrames.add("None");
            displayFrames.addAll(recommendedFrames);
        } else {
            displayFrames = allFrames;
        }

        LayoutInflater inflater = LayoutInflater.from(this);

        for (String frame : displayFrames) {
            View itemView = inflater.inflate(R.layout.item_ar_frame_circle, layoutFramesAr, false);
            FrameLayout circleBg = itemView.findViewById(R.id.frame_circle_bg);
            ImageView ivIcon = itemView.findViewById(R.id.iv_frame_icon);
            TextView tvLabel = itemView.findViewById(R.id.tv_frame_label);

            tvLabel.setText(frame);

            if ("None".equalsIgnoreCase(frame)) {
                ivIcon.setImageResource(R.drawable.ic_none);
            } else {
                ivIcon.setImageResource(R.drawable.ic_eyeglasses);
            }

            boolean isSelected = frame.equalsIgnoreCase(selectedFrameStyle);
            if (isSelected) {
                circleBg.setBackgroundResource(R.drawable.bg_ar_circle_selected);
                tvLabel.setTextColor(getColor(R.color.accentGold));
            } else {
                circleBg.setBackgroundResource(R.drawable.bg_ar_circle_unselected);
                tvLabel.setTextColor(getColor(R.color.white));
            }

            itemView.setOnClickListener(v -> {
                selectedFrameStyle = frame;
                arFrameOverlay.setFrameStyle(selectedFrameStyle);
                setupFrameCircles();
            });

            layoutFramesAr.addView(itemView);
        }
    }

    private void setupColorChips() {
        layoutColorsAr.removeAllViews();

        for (ColorOption option : colorOptions) {
            TextView chip = new TextView(this);
            chip.setText(option.name);
            chip.setPadding(32, 16, 32, 16);
            chip.setTextSize(13);

            boolean isSelected = option.hex.equalsIgnoreCase(selectedFrameColor);
            if (isSelected) {
                chip.setBackgroundResource(R.drawable.bg_confidence_chip);
                chip.setTextColor(getColor(R.color.accentGold));
            } else {
                chip.setBackgroundResource(R.color.bgCard);
                chip.setTextColor(getColor(R.color.textSecondary));
            }

            chip.setOnClickListener(v -> {
                selectedFrameColor = option.hex;
                arFrameOverlay.setFrameColor(selectedFrameColor);
                setupColorChips();
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 16, 0);
            layoutColorsAr.addView(chip, params);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission is required for AR preview", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) cameraExecutor.shutdown();
        if (faceDetector != null) faceDetector.close();
    }
}
