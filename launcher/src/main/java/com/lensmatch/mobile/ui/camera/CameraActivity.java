package com.lensmatch.mobile.ui.camera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
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
import com.google.android.material.button.MaterialButton;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.AppState;
import com.lensmatch.mobile.ui.FaceMeshOverlayView;
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
    private TextView tvInstruction;
    private MaterialButton btnCapture;
    private ProgressBar progressCapture;

    private ImageCapture imageCapture;
    private FaceDetector faceDetector;
    private TFLiteFaceDetector tfliteDetector;
    private ExecutorService cameraExecutor;

    private boolean isAligned = false;
    private int alignedFrameCount = 0;
    private int unalignedFrameCount = 0;
    private Face latestFace = null;
    private boolean isCapturing = false;

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
        tvInstruction = findViewById(R.id.tv_instruction);
        btnCapture = findViewById(R.id.btn_capture);
        progressCapture = findViewById(R.id.progress_capture);

        ImageView btnClose = findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> finish());

        btnCapture.setOnClickListener(v -> captureAndProcessImage());

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

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
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

            faceDetector.process(inputImage)
                    .addOnSuccessListener(faces -> {
                        evaluateFaces(faces, inputImage.getWidth(), inputImage.getHeight(), luminance);
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

    private void evaluateFaces(List<Face> faces, int imageWidth, int imageHeight, float luminance) {
        if (faces.isEmpty()) {
            latestFace = null;
            runOnUiThread(() -> {
                faceMeshOverlay.updateState(null, FaceMeshOverlayView.GuideState.SEARCHING, imageWidth, imageHeight);
                setInstruction("Position face within the scan dots", FaceMeshOverlayView.GuideState.SEARCHING);
            });
            return;
        }

        Face face = faces.get(0);
        latestFace = face;
        Rect bounds = face.getBoundingBox();

        // 1. Luminance Quality Gate (50 <= Y <= 215)
        if (luminance < 50f) {
            runOnUiThread(() -> {
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.MISALIGNED, imageWidth, imageHeight);
                setInstruction("Lighting too dark - move to better lighting", FaceMeshOverlayView.GuideState.MISALIGNED);
            });
            return;
        }
        if (luminance > 215f) {
            runOnUiThread(() -> {
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.MISALIGNED, imageWidth, imageHeight);
                setInstruction("Lighting too harsh - avoid direct glare", FaceMeshOverlayView.GuideState.MISALIGNED);
            });
            return;
        }

        // 2. Size Quality Gate: Face bounding box must occupy 35%–65% of screen height
        float faceHeight = bounds.height();
        if (faceHeight < imageHeight * 0.35f) {
            runOnUiThread(() -> {
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.MISALIGNED, imageWidth, imageHeight);
                setInstruction("Move closer to align with scan dots", FaceMeshOverlayView.GuideState.MISALIGNED);
            });
            return;
        }
        if (faceHeight > imageHeight * 0.65f) {
            runOnUiThread(() -> {
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.MISALIGNED, imageWidth, imageHeight);
                setInstruction("Move further away", FaceMeshOverlayView.GuideState.MISALIGNED);
            });
            return;
        }

        // 3. Center Quality Gate: Face must be centered within 10% of frame center
        float centerX = bounds.centerX();
        float centerY = bounds.centerY();
        if (Math.abs(centerX - (imageWidth / 2f)) > imageWidth * 0.10f ||
            Math.abs(centerY - (imageHeight / 2f)) > imageHeight * 0.12f) {
            runOnUiThread(() -> {
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.MISALIGNED, imageWidth, imageHeight);
                setInstruction("Center your face with the scan dots", FaceMeshOverlayView.GuideState.MISALIGNED);
            });
            return;
        }

        // 4. 3D Head Pose Orientation Check (Yaw <= 12°, Pitch <= 10°, Roll <= 8°)
        float yaw = face.getHeadEulerAngleY();   // Left / Right turn
        float pitch = face.getHeadEulerAngleX(); // Up / Down tilt
        float roll = face.getHeadEulerAngleZ();  // Sideways ear-to-shoulder tilt

        if (Math.abs(yaw) > 12.0f) {
            runOnUiThread(() -> {
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.TILTED, imageWidth, imageHeight);
                setInstruction("Look straight at the camera", FaceMeshOverlayView.GuideState.TILTED);
            });
            return;
        }
        if (Math.abs(pitch) > 10.0f) {
            runOnUiThread(() -> {
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.TILTED, imageWidth, imageHeight);
                setInstruction("Level your head", FaceMeshOverlayView.GuideState.TILTED);
            });
            return;
        }
        if (Math.abs(roll) > 8.0f) {
            runOnUiThread(() -> {
                faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.TILTED, imageWidth, imageHeight);
                setInstruction("Keep head upright", FaceMeshOverlayView.GuideState.TILTED);
            });
            return;
        }

        runOnUiThread(() -> {
            faceMeshOverlay.updateState(faces, FaceMeshOverlayView.GuideState.ALIGNED, imageWidth, imageHeight);
            setInstruction("Face Aligned! Tap to scan.", FaceMeshOverlayView.GuideState.ALIGNED);
        });
    }

    private void setInstruction(String text, FaceMeshOverlayView.GuideState state) {
        boolean aligned = (state == FaceMeshOverlayView.GuideState.ALIGNED);
        if (aligned) {
            alignedFrameCount++;
            unalignedFrameCount = 0;
        } else {
            unalignedFrameCount++;
            alignedFrameCount = 0;
        }

        if (alignedFrameCount >= 3) {
            isAligned = true;
        } else if (unalignedFrameCount >= 4) {
            isAligned = false;
        }

        tvInstruction.setText(text);
        if (state == FaceMeshOverlayView.GuideState.ALIGNED) {
            tvInstruction.setTextColor(ContextCompat.getColor(this, R.color.greenSuccess));
            btnCapture.setText("Capture & Scan");
            btnCapture.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.accentGold));
            btnCapture.setTextColor(ContextCompat.getColor(this, R.color.bgCard));
        } else if (state == FaceMeshOverlayView.GuideState.TILTED) {
            tvInstruction.setTextColor(Color.parseColor("#FFB300")); // Warning amber
            btnCapture.setText("Position Face in Frame");
        } else if (state == FaceMeshOverlayView.GuideState.MISALIGNED) {
            tvInstruction.setTextColor(Color.parseColor("#FF5252")); // Alert red
            btnCapture.setText("Position Face in Frame");
        } else {
            tvInstruction.setTextColor(ContextCompat.getColor(this, R.color.white));
            btnCapture.setText("Position Face in Frame");
        }
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
