package com.lensmatch.mobile.ui.camera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.content.Context;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.camera.view.TransformExperimental;
import androidx.camera.view.transform.CoordinateTransform;
import androidx.camera.view.transform.ImageProxyTransformFactory;
import androidx.camera.view.transform.OutputTransform;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.AppState;
import com.lensmatch.mobile.data.ScanModel;
import com.lensmatch.mobile.service.FirestoreService;
import com.lensmatch.mobile.ui.FaceMeshOverlayView;
import com.lensmatch.mobile.utils.FaceShapeDetector;
import com.lensmatch.mobile.utils.StatusBarUtils;
import com.lensmatch.mobile.utils.TFLiteFaceDetector;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";
    private static final int PERMISSION_REQUEST_CAMERA = 101;

    private PreviewView viewFinder;
    private FaceMeshOverlayView faceMeshOverlay;
    private View layoutInstructionPill;
    private TextView tvInstruction;
    private View layoutScanningIndicator;
    private TextView tvScanningStatus;

    private ImageCapture imageCapture;
    private ProcessCameraProvider cameraProvider;
    private FaceDetector faceDetector;
    private TFLiteFaceDetector tfliteDetector;
    private ExecutorService cameraExecutor;

    private Face latestFace = null;
    private boolean isCapturing = false;

    // Auto-capture progress tracking (~1.0s smooth biometric hold)
    private float scanProgress = 0f;
    private static final float SCAN_INCREMENT = 0.042f; // ~24 frames (~1.0s at 24-30fps)
    private static final float SCAN_DECAY = 0.02f;      // Gentle decay on slight movement

    // Multi-Frame Temporal Averaging buffer (15-30 frames over ~1.5s)
    private final List<Bitmap> temporalFrames = new ArrayList<>();
    private int remainingTemporalSamples = 0;
    private static final int TARGET_TEMPORAL_SAMPLES = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        viewFinder = findViewById(R.id.view_finder);
        faceMeshOverlay = findViewById(R.id.face_mesh_overlay);
        layoutInstructionPill = findViewById(R.id.layout_instruction_pill);
        tvInstruction = findViewById(R.id.tv_instruction);
        layoutScanningIndicator = findViewById(R.id.layout_scanning_indicator);
        tvScanningStatus = findViewById(R.id.tv_scanning_status);

        ImageView btnClose = findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> finish());
        StatusBarUtils.applyTopMargin(btnClose);
        StatusBarUtils.applyTopMargin(layoutInstructionPill);

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build();
        faceDetector = FaceDetection.getClient(options);
        tfliteDetector = new TFLiteFaceDetector(this);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        }
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required for face shape analysis.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                this.cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::processImageProxy);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                if (this.cameraProvider != null) {
                    this.cameraProvider.unbindAll();
                    this.cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);
                }

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @ExperimentalGetImage
    @TransformExperimental
    private void processImageProxy(ImageProxy imageProxy) {
        if (isCapturing) {
            if (remainingTemporalSamples > 0) {
                try {
                    Bitmap bmp = imageProxy.toBitmap();
                    if (bmp != null) {
                        Matrix matrix = new Matrix();
                        matrix.postRotate(imageProxy.getImageInfo().getRotationDegrees());
                        matrix.postScale(-1f, 1f); // front camera horizontal mirror
                        Bitmap orientedBmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                        synchronized (temporalFrames) {
                            temporalFrames.add(orientedBmp);
                            remainingTemporalSamples--;
                            int collected = TARGET_TEMPORAL_SAMPLES - remainingTemporalSamples;
                            runOnUiThread(() -> {
                                if (tvScanningStatus != null) {
                                    tvScanningStatus.setText("Scanning face contours (" + collected + "/" + TARGET_TEMPORAL_SAMPLES + " frames)...");
                                }
                            });
                        }
                    }
                } catch (Throwable t) {
                    remainingTemporalSamples = 0;
                }
            }
            imageProxy.close();
            return;
        }

        if (imageProxy.getImage() != null) {
            InputImage inputImage = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
            float luminance = calculateLuminance(imageProxy);

            OutputTransform sourceTransform = null;
            try {
                ImageProxyTransformFactory factory = new ImageProxyTransformFactory();
                factory.setUsingRotationDegrees(true);
                sourceTransform = factory.getOutputTransform(imageProxy);
            } catch (Exception e) {
                Log.w(TAG, "Failed to get source OutputTransform", e);
            }

            final OutputTransform finalSourceTransform = sourceTransform;
            faceDetector.process(inputImage)
                    .addOnSuccessListener(faces -> {
                        evaluateFaces(faces, finalSourceTransform, inputImage.getWidth(), inputImage.getHeight(), luminance);
                        imageProxy.close();
                    })
                    .addOnFailureListener(e -> imageProxy.close());
        } else {
            imageProxy.close();
        }
    }

    private float calculateLuminance(ImageProxy imageProxy) {
        try {
            ImageProxy.PlaneProxy yPlane = imageProxy.getPlanes()[0];
            ByteBuffer buffer = yPlane.getBuffer();
            int limit = buffer.limit();
            int step = Math.max(1, limit / 200); // Sample ~200 points across image
            long sum = 0;
            int count = 0;
            for (int i = 0; i < limit; i += step) {
                sum += (buffer.get(i) & 0xFF);
                count++;
            }
            return count > 0 ? (float) sum / count : 128f;
        } catch (Exception e) {
            return 128f;
        }
    }

    @TransformExperimental
    private Matrix getScreenTransformMatrix(OutputTransform sourceTransform) {
        if (sourceTransform == null || viewFinder == null) return null;
        try {
            OutputTransform targetTransform = viewFinder.getOutputTransform();
            if (targetTransform == null) return null;
            CoordinateTransform coordinateTransform = new CoordinateTransform(sourceTransform, targetTransform);
            Matrix matrix = new Matrix();
            coordinateTransform.transform(matrix);
            return matrix;
        } catch (Exception e) {
            Log.w(TAG, "Failed to compute CoordinateTransform matrix", e);
            return null;
        }
    }

    @TransformExperimental
    private void evaluateFaces(List<Face> faces, OutputTransform sourceTransform, int imageWidth, int imageHeight, float luminance) {
        if (isCapturing) return;

        if (faces.isEmpty()) {
            latestFace = null;
            scanProgress = Math.max(0f, scanProgress - 0.08f);
            runOnUiThread(() -> {
                Matrix matrix = getScreenTransformMatrix(sourceTransform);
                faceMeshOverlay.updateState(null, FaceMeshOverlayView.GuideState.SEARCHING, scanProgress, matrix, imageWidth, imageHeight);
                setInstruction("Position face in center oval", FaceMeshOverlayView.GuideState.SEARCHING);
            });
            return;
        }

        Face face = faces.get(0);
        latestFace = face;
        Rect bounds = face.getBoundingBox();

        Matrix matrix = getScreenTransformMatrix(sourceTransform);
        if (matrix == null) return; // Need matrix for accurate position gating

        // Transform ML Kit face bounds into Screen (PreviewView) coordinates
        RectF transformedBounds = new RectF(bounds);
        matrix.mapRect(transformedBounds);

        // Fetch the visual oval guide boundaries
        RectF guideBounds = faceMeshOverlay.getGuideOvalRect();
        if (guideBounds == null || guideBounds.isEmpty()) return;

        // 1. Luminance Quality Gate (Forgiving: 20 <= Y <= 245)
        if (luminance < 20f) {
            scanProgress = Math.max(0f, scanProgress - SCAN_DECAY);
            runOnUiThread(() -> {
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.MISALIGNED, scanProgress, matrix, imageWidth, imageHeight);
                setInstruction("Lighting too dark — move to light", FaceMeshOverlayView.GuideState.MISALIGNED);
            });
            return;
        }
        if (luminance > 245f) {
            scanProgress = Math.max(0f, scanProgress - SCAN_DECAY);
            runOnUiThread(() -> {
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.MISALIGNED, scanProgress, matrix, imageWidth, imageHeight);
                setInstruction("Lighting too harsh — avoid direct glare", FaceMeshOverlayView.GuideState.MISALIGNED);
            });
            return;
        }

        // 2. Position and Size Quality Gate (Strict alignment with the visible guide oval)
        float guideCenterX = guideBounds.centerX();
        float guideCenterY = guideBounds.centerY();
        float guideW = guideBounds.width();
        float guideH = guideBounds.height();

        float faceCenterX = transformedBounds.centerX();
        float faceCenterY = transformedBounds.centerY();
        float faceH = transformedBounds.height();

        float toleranceX = guideW * 0.15f;
        float toleranceY = guideH * 0.15f;

        // Size check against the oval (not the raw screen height)
        if (faceH < guideH * 0.60f) {
            scanProgress = Math.max(0f, scanProgress - SCAN_DECAY);
            runOnUiThread(() -> {
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.MISALIGNED, scanProgress, matrix, imageWidth, imageHeight);
                setInstruction("Move slightly closer", FaceMeshOverlayView.GuideState.MISALIGNED);
            });
            return;
        }
        if (faceH > guideH * 0.95f) {
            scanProgress = Math.max(0f, scanProgress - SCAN_DECAY);
            runOnUiThread(() -> {
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.MISALIGNED, scanProgress, matrix, imageWidth, imageHeight);
                setInstruction("Move a little further back", FaceMeshOverlayView.GuideState.MISALIGNED);
            });
            return;
        }

        // Center position checks (providing directional feedback relative to the screen)
        if (faceCenterY < guideCenterY - toleranceY) {
            scanProgress = Math.max(0f, scanProgress - SCAN_DECAY);
            runOnUiThread(() -> {
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.MISALIGNED, scanProgress, matrix, imageWidth, imageHeight);
                setInstruction("Move down", FaceMeshOverlayView.GuideState.MISALIGNED);
            });
            return;
        }
        if (faceCenterY > guideCenterY + toleranceY) {
            scanProgress = Math.max(0f, scanProgress - SCAN_DECAY);
            runOnUiThread(() -> {
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.MISALIGNED, scanProgress, matrix, imageWidth, imageHeight);
                setInstruction("Move up", FaceMeshOverlayView.GuideState.MISALIGNED);
            });
            return;
        }
        if (faceCenterX < guideCenterX - toleranceX) {
            scanProgress = Math.max(0f, scanProgress - SCAN_DECAY);
            runOnUiThread(() -> {
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.MISALIGNED, scanProgress, matrix, imageWidth, imageHeight);
                setInstruction("Move right", FaceMeshOverlayView.GuideState.MISALIGNED);
            });
            return;
        }
        if (faceCenterX > guideCenterX + toleranceX) {
            scanProgress = Math.max(0f, scanProgress - SCAN_DECAY);
            runOnUiThread(() -> {
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.MISALIGNED, scanProgress, matrix, imageWidth, imageHeight);
                setInstruction("Move left", FaceMeshOverlayView.GuideState.MISALIGNED);
            });
            return;
        }

        // 4. Face Contour Completeness Check
        FaceContour faceContour = face.getContour(FaceContour.FACE);
        if (faceContour == null || faceContour.getPoints() == null || faceContour.getPoints().size() < 20) {
            scanProgress = Math.max(0f, scanProgress - SCAN_DECAY);
            runOnUiThread(() -> {
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.MISALIGNED, scanProgress, matrix, imageWidth, imageHeight);
                setInstruction("Keep full face inside frame", FaceMeshOverlayView.GuideState.MISALIGNED);
            });
            return;
        }

        // 5. 3D Head Pose Orientation Check (Yaw <= 30°, Pitch <= 30°, Roll <= 30°)
        float yaw = face.getHeadEulerAngleY();   // Left / Right turn
        float pitch = face.getHeadEulerAngleX(); // Up / Down tilt
        float roll = face.getHeadEulerAngleZ();  // Sideways ear-to-shoulder tilt

        if (Math.abs(yaw) > 30.0f) {
            scanProgress = Math.max(0f, scanProgress - SCAN_DECAY);
            runOnUiThread(() -> {
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.TILTED, scanProgress, matrix, imageWidth, imageHeight);
                setInstruction("Look straight at the camera", FaceMeshOverlayView.GuideState.TILTED);
            });
            return;
        }
        if (Math.abs(pitch) > 30.0f) {
            scanProgress = Math.max(0f, scanProgress - SCAN_DECAY);
            runOnUiThread(() -> {
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.TILTED, scanProgress, matrix, imageWidth, imageHeight);
                setInstruction("Level your head", FaceMeshOverlayView.GuideState.TILTED);
            });
            return;
        }
        if (Math.abs(roll) > 30.0f) {
            scanProgress = Math.max(0f, scanProgress - SCAN_DECAY);
            runOnUiThread(() -> {
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.TILTED, scanProgress, matrix, imageWidth, imageHeight);
                setInstruction("Keep head upright", FaceMeshOverlayView.GuideState.TILTED);
            });
            return;
        }

        // All quality & orientation checks passed! Face is ready for biometric capture
        scanProgress = Math.min(1.0f, scanProgress + SCAN_INCREMENT);

        if (scanProgress >= 1.0f) {
            runOnUiThread(() -> {
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.SUCCESS, 1.0f, matrix, imageWidth, imageHeight);
                setInstruction("Face aligned! Scanning...", FaceMeshOverlayView.GuideState.SUCCESS);
                triggerHapticFeedback();
                captureAndProcessImage();
            });
        } else {
            final float currentProgress = scanProgress;
            runOnUiThread(() -> {
                FaceMeshOverlayView.GuideState state = FaceMeshOverlayView.GuideState.ALIGNED;
                faceMeshOverlay.updateState(faces, state, currentProgress, matrix, imageWidth, imageHeight);
                setInstruction("Face centered — Hold still...", state);
            });
        }
    }

    private void setInstruction(String text, FaceMeshOverlayView.GuideState state) {
        tvInstruction.setText(text);
        if (state == FaceMeshOverlayView.GuideState.SUCCESS ||
            state == FaceMeshOverlayView.GuideState.SCANNING ||
            state == FaceMeshOverlayView.GuideState.ALIGNED) {
            if (layoutInstructionPill != null) {
                layoutInstructionPill.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.success)));
            }
            tvInstruction.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else if (state == FaceMeshOverlayView.GuideState.TILTED) {
            if (layoutInstructionPill != null) {
                layoutInstructionPill.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.warning)));
            }
            tvInstruction.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else if (state == FaceMeshOverlayView.GuideState.MISALIGNED) {
            if (layoutInstructionPill != null) {
                layoutInstructionPill.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.error)));
            }
            tvInstruction.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            if (layoutInstructionPill != null) {
                layoutInstructionPill.setBackgroundTintList(null); // Revert to original background tint if any
                layoutInstructionPill.setBackgroundResource(R.drawable.bg_instruction_pill);
            }
            tvInstruction.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
    }

    private void triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                }
            } else if (viewFinder != null) {
                viewFinder.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            }
        } catch (Exception ignored) {}
    }

    private void captureAndProcessImage() {
        if (imageCapture == null || isCapturing) return;

        isCapturing = true;
        synchronized (temporalFrames) {
            temporalFrames.clear();
            remainingTemporalSamples = TARGET_TEMPORAL_SAMPLES;
        }
        if (layoutScanningIndicator != null) {
            layoutScanningIndicator.setVisibility(View.VISIBLE);
        }
        if (tvScanningStatus != null) {
            tvScanningStatus.setText("Scanning face contours (1/" + TARGET_TEMPORAL_SAMPLES + " frames)...");
        }
        tvInstruction.setText("Full face detected! Scanning...");
        if (layoutInstructionPill != null) {
            layoutInstructionPill.setBackgroundResource(R.drawable.bg_instruction_pill_green);
        }

        File scansDir = new File(getFilesDir(), "scans");
        if (!scansDir.exists()) {
            scansDir.mkdirs();
        }
        File photoFile = new File(scansDir, "captured_face_" + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Bitmap bitmap = getCorrectlyOrientedBitmap(photoFile);
                if (bitmap == null) {
                    bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                }
                
                InputImage image = InputImage.fromBitmap(bitmap, 0);
                final Bitmap finalBitmap = bitmap;
                
                final String photoBase64 = ScanModel.encodeBitmapToBase64Thumbnail(finalBitmap, 240, 70);

                faceDetector.process(image)
                        .addOnSuccessListener(faces -> {
                            Rect bounds = faces.isEmpty() ? 
                                    new Rect(0, 0, finalBitmap.getWidth(), finalBitmap.getHeight()) : 
                                    faces.get(0).getBoundingBox();
                            
                            List<Bitmap> allFrames = new ArrayList<>();
                            synchronized (temporalFrames) {
                                allFrames.addAll(temporalFrames);
                            }
                            allFrames.add(finalBitmap); // Anchor high-res photo

                            processFinalResult(photoFile.getAbsolutePath(), photoBase64, allFrames, bounds);
                        })
                        .addOnFailureListener(e -> {
                            Rect bounds = new Rect(0, 0, finalBitmap.getWidth(), finalBitmap.getHeight());
                            List<Bitmap> allFrames = new ArrayList<>();
                            synchronized (temporalFrames) {
                                allFrames.addAll(temporalFrames);
                            }
                            allFrames.add(finalBitmap);

                            processFinalResult(photoFile.getAbsolutePath(), photoBase64, allFrames, bounds);
                        });
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                isCapturing = false;
                scanProgress = 0f;
                if (layoutScanningIndicator != null) {
                    layoutScanningIndicator.setVisibility(View.GONE);
                }
                Toast.makeText(CameraActivity.this, "Capture failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processFinalResult(String photoPath, String photoBase64, List<Bitmap> allFrames, Rect bounds) {
        if (layoutScanningIndicator != null) {
            layoutScanningIndicator.setVisibility(View.GONE);
        }

        TFLiteFaceDetector.FaceShapeResult result = tfliteDetector.processMultiFrame(allFrames, bounds);

        // Recycle the intermediate preview frames to free native memory immediately
        for (int i = 0; i < allFrames.size() - 1; i++) {
            Bitmap bmp = allFrames.get(i);
            if (bmp != null && !bmp.isRecycled()) {
                bmp.recycle();
            }
        }
        synchronized (temporalFrames) {
            temporalFrames.clear();
        }

        AppState.getInstance().setLastImagePath(photoPath);
        AppState.getInstance().setLastDetectedShape(result.getShape());
        AppState.getInstance().setLastConfidence(result.getConfidence());
        AppState.getInstance().setLastIsBorderline(result.isBorderline());
        AppState.getInstance().setLastRunnerUpShape(result.getRunnerUpShape());
        AppState.getInstance().setLastNotes(result.getNotes());

        FaceShapeDetector.ShapeRecommendation rec = FaceShapeDetector.getRecommendation(result);
        List<String> recStyles = new ArrayList<>();
        List<String> avoidStyles = new ArrayList<>();
        String recReason = "";
        String avoidReason = "";
        if (rec != null) {
            if (rec.primary != null) recStyles.addAll(rec.primary);
            if (rec.secondary != null) {
                // We map secondary options to avoidedStyles just to preserve DB schema backwards compatibility,
                // or we could append them. The UI handles them appropriately.
                avoidStyles.addAll(rec.secondary);
            }
            recReason = rec.explanation != null ? rec.explanation : "";
        }

        String scanId = "scan_" + System.currentTimeMillis();
        String customerId = "local";
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getUid() != null) {
            customerId = user.getUid();
        }
        String customerName = AppState.getInstance().getUserName();

        ScanModel scan = new ScanModel(
                scanId,
                customerId,
                customerName,
                result.getShape(),
                result.getConfidence(),
                result.isBorderline(),
                result.getRunnerUpShape(),
                result.getNotes(),
                recStyles,
                avoidStyles,
                recReason,
                avoidReason,
                photoPath,
                photoBase64,
                new Date()
        );

        // ALWAYS SAVE TO LOCAL HISTORY IMMEDIATELY! EVERY SCAN ACCUMULATES ("SAVE AND SAVE SAVE SAVE")
        AppState.getInstance().addScan(scan);

        final String finalScanId = scanId;
        FirestoreService.saveScanResult(scan, new FirestoreService.Callback<String>() {
            @Override
            public void onSuccess(String resultId) {
                Log.d(TAG, "Scan record saved to Firebase Firestore: " + resultId);
                if (resultId != null) {
                    AppState.getInstance().updateScanId(finalScanId, resultId);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.w(TAG, "Failed saving scan to Firebase Firestore (saved locally): " + errorMessage);
            }
        });

        Intent intent = new Intent();
        intent.putExtra("imagePath", photoPath);
        intent.putExtra("shape", result.getShape());
        intent.putExtra("confidence", result.getConfidence());
        intent.putExtra("isBorderline", result.isBorderline());
        intent.putExtra("runnerUpShape", result.getRunnerUpShape());
        intent.putExtra("notes", result.getNotes());
        
        ArrayList<String> combinedRecommendations = new ArrayList<>(recStyles);
        combinedRecommendations.addAll(avoidStyles); // avoidStyles holds the secondary recommendations now
        intent.putStringArrayListExtra("recommendedFrames", combinedRecommendations);
        
        setResult(RESULT_OK, intent);
        finish();
    }

    private Bitmap getCorrectlyOrientedBitmap(File photoFile) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
            if (bitmap == null) return null;

            ExifInterface exif = new ExifInterface(photoFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED
            );

            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    if (bitmap.getWidth() > bitmap.getHeight()) {
                        matrix.postRotate(270);
                    }
                    break;
            }

            // Mirror horizontally after rotation for natural selfie orientation on front camera
            matrix.postScale(-1f, 1f);

            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            try (FileOutputStream out = new FileOutputStream(photoFile)) {
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out);
            }

            try {
                ExifInterface newExif = new ExifInterface(photoFile.getAbsolutePath());
                newExif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_NORMAL));
                newExif.saveAttributes();
            } catch (Exception ignored) {}

            return rotatedBitmap;
        } catch (Exception e) {
            return BitmapFactory.decodeFile(photoFile.getAbsolutePath());
        }
    }

    private void stopCamera() {
        try {
            if (cameraProvider != null) {
                cameraProvider.unbindAll();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error unbinding camera provider", e);
        }
    }

    @Override
    protected void onPause() {
        stopCamera();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopCamera();
        super.onDestroy();
        if (tfliteDetector != null) tfliteDetector.close();
        if (cameraExecutor != null) cameraExecutor.shutdown();
        if (faceDetector != null) faceDetector.close();
    }
}
