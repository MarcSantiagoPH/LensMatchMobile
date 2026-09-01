package com.lensmatch.mobile.ui.camera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

        tfliteDetector = new TFLiteFaceDetector(this);
        cameraExecutor = Executors.newSingleThreadExecutor();

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .build();
        faceDetector = FaceDetection.getClient(options);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        }

        btnCapture.setOnClickListener(v -> captureAndProcessImage());
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
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
            imageProxy.close();
            return;
        }

        if (imageProxy.getImage() != null) {
            InputImage inputImage = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

            faceDetector.process(inputImage)
                    .addOnSuccessListener(faces -> {
                        evaluateFaces(faces, inputImage.getWidth(), inputImage.getHeight());
                        imageProxy.close();
                    })
                    .addOnFailureListener(e -> imageProxy.close());
        } else {
            imageProxy.close();
        }
    }

    private void evaluateFaces(List<Face> faces, int imageWidth, int imageHeight) {
        if (faces.isEmpty()) {
            latestFace = null;
            runOnUiThread(() -> {
                faceMeshOverlay.updateFaces(null, false, imageWidth, imageHeight);
                setInstruction("Position face in frame", false);
            });
            return;
        }

        Face face = faces.get(0);
        latestFace = face;
        Rect bounds = face.getBoundingBox();

        // 1. Size check
        float faceWidth = bounds.width();
        if (faceWidth < imageHeight * 0.32f) {
            runOnUiThread(() -> setInstruction("Move closer", false));
            return;
        }
        if (faceWidth > imageHeight * 0.78f) {
            runOnUiThread(() -> setInstruction("Move further away", false));
            return;
        }

        // 2. Center check
        float centerX = bounds.centerX();
        if (Math.abs(centerX - imageHeight / 2f) > imageHeight * 0.18f) {
            runOnUiThread(() -> setInstruction("Center your face", false));
            return;
        }

        runOnUiThread(() -> {
            faceMeshOverlay.updateFaces(faces, true, imageWidth, imageHeight);
            setInstruction("Face Aligned! Tap to scan.", true);
        });
    }

    private void setInstruction(String text, boolean aligned) {
        if (aligned) {
            alignedFrameCount++;
            unalignedFrameCount = 0;
        } else {
            unalignedFrameCount++;
            alignedFrameCount = 0;
        }

        boolean newAlignedState = isAligned;
        if (alignedFrameCount >= 3) {
            newAlignedState = true;
        } else if (unalignedFrameCount >= 4) {
            newAlignedState = false;
        }

        isAligned = newAlignedState;
        tvInstruction.setText(text);
        if (isAligned) {
            tvInstruction.setTextColor(ContextCompat.getColor(this, R.color.greenSuccess));
            btnCapture.setText("Capture & Scan");
            btnCapture.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.accentGold));
            btnCapture.setTextColor(ContextCompat.getColor(this, R.color.bgCard));
        } else {
            tvInstruction.setTextColor(ContextCompat.getColor(this, R.color.white));
            btnCapture.setText(latestFace != null ? "Tap to Scan" : "Position Face in Frame");
        }
    }

    private void captureAndProcessImage() {
        if (imageCapture == null || isCapturing) return;

        isCapturing = true;
        btnCapture.setVisibility(View.INVISIBLE);
        progressCapture.setVisibility(View.VISIBLE);

        File photoFile = new File(getCacheDir(), "captured_face_" + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Bitmap bitmap = getCorrectlyOrientedBitmap(photoFile);
                if (bitmap == null) {
                    bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                }
                Rect bounds = latestFace != null ? latestFace.getBoundingBox() : new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

                TFLiteFaceDetector.FaceShapeResult result = tfliteDetector.processImage(bitmap, bounds);

                AppState.getInstance().setLastImagePath(photoFile.getAbsolutePath());
                AppState.getInstance().setLastDetectedShape(result.getShape());
                AppState.getInstance().setLastConfidence(result.getConfidence());

                Intent intent = new Intent();
                intent.putExtra("imagePath", photoFile.getAbsolutePath());
                intent.putExtra("shape", result.getShape());
                intent.putExtra("confidence", result.getConfidence());
                setResult(RESULT_OK, intent);
                finish();
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

            // Mirror for natural selfie orientation if front camera
            matrix.postScale(-1, 1, bitmap.getWidth() / 2f, bitmap.getHeight() / 2f);

            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            try (java.io.FileOutputStream out = new java.io.FileOutputStream(photoFile)) {
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out);
            }

            return rotatedBitmap;
        } catch (Exception e) {
            return BitmapFactory.decodeFile(photoFile.getAbsolutePath());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to scan your face", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tfliteDetector != null) tfliteDetector.close();
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }
}
