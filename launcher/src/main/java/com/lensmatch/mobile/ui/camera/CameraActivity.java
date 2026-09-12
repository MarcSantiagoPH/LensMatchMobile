package com.lensmatch.mobile.ui.camera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.content.Context;
import android.content.res.ColorStateList;
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

import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
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
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.android.material.button.MaterialButton;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.AppState;
import com.lensmatch.mobile.ui.FaceMeshOverlayView;
import com.lensmatch.mobile.utils.StatusBarUtils;
import com.lensmatch.mobile.utils.TFLiteFaceDetector;

import java.io.File;
import java.util.ArrayList;
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
    private MaterialButton btnCapture;
    private ProgressBar progressCapture;

    private ImageCapture imageCapture;
    private FaceDetector faceDetector;
    private TFLiteFaceDetector tfliteDetector;
    private ExecutorService cameraExecutor;

    private Face latestFace = null;
    private boolean isCapturing = false;

    // Auto-capture progress tracking
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
        btnCapture = findViewById(R.id.btn_capture);
        progressCapture = findViewById(R.id.progress_capture);

        ImageView btnClose = findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> finish());
        StatusBarUtils.applyTopMargin(btnClose);
        StatusBarUtils.applyTopMargin(layoutInstructionPill);

        btnCapture.setOnClickListener(v -> {
            triggerHapticFeedback();
            captureAndProcessImage();
        });

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
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

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

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @androidx.camera.core.ExperimentalGetImage
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
                            runOnUiThread(() -> tvInstruction.setText("Scanning face contours (" + collected + "/" + TARGET_TEMPORAL_SAMPLES + " frames)..."));
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
                setInstruction("Position face in camera", FaceMeshOverlayView.GuideState.SEARCHING);
            });
            return;
        }

        Face face = faces.get(0);
        latestFace = face;
        Rect bounds = face.getBoundingBox();

        // 1. Luminance Quality Gate (Forgiving: 30 <= Y <= 235)
        if (luminance < 30f) {
            scanProgress = Math.max(0f, scanProgress - SCAN_DECAY);
            runOnUiThread(() -> {
                Matrix matrix = getScreenTransformMatrix(sourceTransform);
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.MISALIGNED, scanProgress, matrix, imageWidth, imageHeight);
                setInstruction("Lighting too dark — move to light", FaceMeshOverlayView.GuideState.MISALIGNED);
            });
            return;
        }
        if (luminance > 235f) {
            scanProgress = Math.max(0f, scanProgress - SCAN_DECAY);
            runOnUiThread(() -> {
                Matrix matrix = getScreenTransformMatrix(sourceTransform);
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.MISALIGNED, scanProgress, matrix, imageWidth, imageHeight);
                setInstruction("Lighting too harsh — avoid direct glare", FaceMeshOverlayView.GuideState.MISALIGNED);
            });
            return;
        }

        // 2. Size Quality Gate: Face occupies 20%–85% of screen height
        float faceHeight = bounds.height();
        if (faceHeight < imageHeight * 0.20f) {
            scanProgress = Math.max(0f, scanProgress - SCAN_DECAY);
            runOnUiThread(() -> {
                Matrix matrix = getScreenTransformMatrix(sourceTransform);
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.MISALIGNED, scanProgress, matrix, imageWidth, imageHeight);
                setInstruction("Move slightly closer", FaceMeshOverlayView.GuideState.MISALIGNED);
            });
            return;
        }
        if (faceHeight > imageHeight * 0.85f) {
            scanProgress = Math.max(0f, scanProgress - SCAN_DECAY);
            runOnUiThread(() -> {
                Matrix matrix = getScreenTransformMatrix(sourceTransform);
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.MISALIGNED, scanProgress, matrix, imageWidth, imageHeight);
                setInstruction("Move a little further back", FaceMeshOverlayView.GuideState.MISALIGNED);
            });
            return;
        }

        // 3. Center Quality Gate: Relaxed 28% tolerance
        float centerX = bounds.centerX();
        float centerY = bounds.centerY();
        if (Math.abs(centerX - (imageWidth / 2f)) > imageWidth * 0.28f ||
            Math.abs(centerY - (imageHeight / 2f)) > imageHeight * 0.28f) {
            scanProgress = Math.max(0f, scanProgress - SCAN_DECAY);
            runOnUiThread(() -> {
                Matrix matrix = getScreenTransformMatrix(sourceTransform);
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.MISALIGNED, scanProgress, matrix, imageWidth, imageHeight);
                setInstruction("Center face in camera", FaceMeshOverlayView.GuideState.MISALIGNED);
            });
            return;
        }

        // 4. 3D Head Pose Orientation Check (Yaw <= 18°, Pitch <= 16°, Roll <= 16°)
        float yaw = face.getHeadEulerAngleY();   // Left / Right turn
        float pitch = face.getHeadEulerAngleX(); // Up / Down tilt
        float roll = face.getHeadEulerAngleZ();  // Sideways ear-to-shoulder tilt

        if (Math.abs(yaw) > 18.0f) {
            scanProgress = Math.max(0f, scanProgress - SCAN_DECAY);
            runOnUiThread(() -> {
                Matrix matrix = getScreenTransformMatrix(sourceTransform);
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.TILTED, scanProgress, matrix, imageWidth, imageHeight);
                setInstruction("Look straight at the camera", FaceMeshOverlayView.GuideState.TILTED);
            });
            return;
        }
        if (Math.abs(pitch) > 16.0f) {
            scanProgress = Math.max(0f, scanProgress - SCAN_DECAY);
            runOnUiThread(() -> {
                Matrix matrix = getScreenTransformMatrix(sourceTransform);
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.TILTED, scanProgress, matrix, imageWidth, imageHeight);
                setInstruction("Level your head", FaceMeshOverlayView.GuideState.TILTED);
            });
            return;
        }
        if (Math.abs(roll) > 16.0f) {
            scanProgress = Math.max(0f, scanProgress - SCAN_DECAY);
            runOnUiThread(() -> {
                Matrix matrix = getScreenTransformMatrix(sourceTransform);
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.TILTED, scanProgress, matrix, imageWidth, imageHeight);
                setInstruction("Keep head upright", FaceMeshOverlayView.GuideState.TILTED);
            });
            return;
        }

        // Face is in great scanning position!
        scanProgress = Math.min(1.0f, scanProgress + SCAN_INCREMENT);

        if (scanProgress >= 1.0f) {
            runOnUiThread(() -> {
                Matrix matrix = getScreenTransformMatrix(sourceTransform);
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.SUCCESS, 1.0f, matrix, imageWidth, imageHeight);
                setInstruction("Face Aligned! Ready to scan.", FaceMeshOverlayView.GuideState.SUCCESS);
                triggerHapticFeedback();
                captureAndProcessImage();
            });
        } else {
            final float currentProgress = scanProgress;
            runOnUiThread(() -> {
                FaceMeshOverlayView.GuideState state = FaceMeshOverlayView.GuideState.ALIGNED;
                Matrix matrix = getScreenTransformMatrix(sourceTransform);
                faceMeshOverlay.updateState(faces, state, currentProgress, matrix, imageWidth, imageHeight);
                setInstruction("Face Aligned! Ready to scan.", state);
            });
        }
    }

    private void setInstruction(String text, FaceMeshOverlayView.GuideState state) {
        tvInstruction.setText(text);
        if (state == FaceMeshOverlayView.GuideState.SUCCESS) {
            if (layoutInstructionPill != null) {
                layoutInstructionPill.setBackgroundResource(R.drawable.bg_instruction_pill_green);
            }
            tvInstruction.setTextColor(ContextCompat.getColor(this, R.color.white));
            btnCapture.setText("Processing...");
            btnCapture.setEnabled(false);
        } else if (state == FaceMeshOverlayView.GuideState.SCANNING || state == FaceMeshOverlayView.GuideState.ALIGNED) {
            if (layoutInstructionPill != null) {
                layoutInstructionPill.setBackgroundResource(R.drawable.bg_instruction_pill_green);
            }
            tvInstruction.setText("Face Aligned! Ready to scan.");
            tvInstruction.setTextColor(ContextCompat.getColor(this, R.color.white));
            btnCapture.setText("Capture & Scan");
            btnCapture.setEnabled(true);
            btnCapture.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.accentGold));
            btnCapture.setTextColor(Color.BLACK);
        } else if (state == FaceMeshOverlayView.GuideState.TILTED) {
            if (layoutInstructionPill != null) {
                layoutInstructionPill.setBackgroundResource(R.drawable.bg_instruction_pill_amber);
            }
            tvInstruction.setTextColor(ContextCompat.getColor(this, R.color.white));
            btnCapture.setText("Capture & Scan");
            btnCapture.setEnabled(latestFace != null);
            btnCapture.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.accentGold));
            btnCapture.setTextColor(Color.BLACK);
        } else if (state == FaceMeshOverlayView.GuideState.MISALIGNED) {
            if (layoutInstructionPill != null) {
                layoutInstructionPill.setBackgroundResource(R.drawable.bg_instruction_pill);
            }
            tvInstruction.setTextColor(ContextCompat.getColor(this, R.color.white));
            btnCapture.setText("Capture & Scan");
            btnCapture.setEnabled(latestFace != null);
            if (latestFace != null) {
                btnCapture.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.accentGold));
                btnCapture.setTextColor(Color.BLACK);
            } else {
                btnCapture.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#40E0A96D")));
                btnCapture.setTextColor(Color.parseColor("#80000000"));
            }
        } else {
            if (layoutInstructionPill != null) {
                layoutInstructionPill.setBackgroundResource(R.drawable.bg_instruction_pill);
            }
            tvInstruction.setTextColor(ContextCompat.getColor(this, R.color.white));
            btnCapture.setText("Capture & Scan");
            btnCapture.setEnabled(false);
            btnCapture.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#40E0A96D")));
            btnCapture.setTextColor(Color.parseColor("#80000000"));
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
        btnCapture.setVisibility(View.INVISIBLE);
        progressCapture.setVisibility(View.VISIBLE);
        tvInstruction.setText("Analyzing facial structure (multi-frame sampling)...");

        File photoFile = new File(getCacheDir(), "captured_face_" + System.currentTimeMillis() + ".jpg");
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

                            processFinalResult(photoFile.getAbsolutePath(), allFrames, bounds);
                        })
                        .addOnFailureListener(e -> {
                            Rect bounds = new Rect(0, 0, finalBitmap.getWidth(), finalBitmap.getHeight());
                            List<Bitmap> allFrames = new ArrayList<>();
                            synchronized (temporalFrames) {
                                allFrames.addAll(temporalFrames);
                            }
                            allFrames.add(finalBitmap);

                            processFinalResult(photoFile.getAbsolutePath(), allFrames, bounds);
                        });
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                isCapturing = false;
                btnCapture.setVisibility(View.VISIBLE);
                progressCapture.setVisibility(View.GONE);
                Toast.makeText(CameraActivity.this, "Capture failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processFinalResult(String photoPath, List<Bitmap> allFrames, Rect bounds) {
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

        Intent intent = new Intent();
        intent.putExtra("imagePath", photoPath);
        intent.putExtra("shape", result.getShape());
        intent.putExtra("confidence", result.getConfidence());
        intent.putExtra("isBorderline", result.isBorderline());
        intent.putExtra("runnerUpShape", result.getRunnerUpShape());
        intent.putExtra("notes", result.getNotes());
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

            try (java.io.FileOutputStream out = new java.io.FileOutputStream(photoFile)) {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tfliteDetector != null) tfliteDetector.close();
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }
}
