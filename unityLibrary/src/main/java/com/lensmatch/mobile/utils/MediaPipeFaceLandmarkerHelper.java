package com.lensmatch.mobile.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult;

import java.util.List;

/**
 * Helper class that wraps Google MediaPipe Face Landmarker to extract 478 3D facial landmarks
 * and 4x4 facial transformation matrixes for precise pose estimation.
 */
public class MediaPipeFaceLandmarkerHelper {
    private static final String TAG = "MediaPipeLandmarker";
    private static final String MODEL_ASSET = "models/face_landmarker.task";

    private FaceLandmarker faceLandmarker;

    public static class DetectionResult {
        public final List<NormalizedLandmark> landmarks;
        public final float[] transformMatrix;

        public DetectionResult(List<NormalizedLandmark> landmarks, float[] transformMatrix) {
            this.landmarks = landmarks;
            this.transformMatrix = transformMatrix;
        }
    }

    public MediaPipeFaceLandmarkerHelper(Context context) {
        try {
            BaseOptions baseOptions = BaseOptions.builder()
                    .setModelAssetPath(MODEL_ASSET)
                    .build();

            FaceLandmarker.FaceLandmarkerOptions options = FaceLandmarker.FaceLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(RunningMode.IMAGE)
                    .setNumFaces(1)
                    .setMinFaceDetectionConfidence(0.5f)
                    .setMinFacePresenceConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .setOutputFaceBlendshapes(false)
                    .setOutputFacialTransformationMatrixes(true)
                    .build();

            faceLandmarker = FaceLandmarker.createFromOptions(context, options);
            Log.d(TAG, "MediaPipe FaceLandmarker successfully initialized with transformation matrix output.");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing MediaPipe FaceLandmarker: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts full detection result including landmarks and 4x4 facial transformation matrix.
     */
    public DetectionResult detect(Bitmap bitmap) {
        if (faceLandmarker == null || bitmap == null) return null;

        try {
            MPImage mpImage = new BitmapImageBuilder(bitmap).build();
            FaceLandmarkerResult result = faceLandmarker.detect(mpImage);

            if (result != null && result.faceLandmarks() != null && !result.faceLandmarks().isEmpty()) {
                List<NormalizedLandmark> landmarks = result.faceLandmarks().get(0);
                float[] matrix = null;
                if (result.facialTransformationMatrixes() != null && result.facialTransformationMatrixes().isPresent()) {
                    List<float[]> matrices = result.facialTransformationMatrixes().get();
                    if (matrices != null && !matrices.isEmpty()) {
                        matrix = matrices.get(0);
                    }
                }
                return new DetectionResult(landmarks, matrix);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error detecting landmarks on bitmap: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Extracts 478 normalized 3D landmarks for the primary face in the given bitmap.
     * Returns null if no face is detected or if initialization failed.
     */
    public List<NormalizedLandmark> extractLandmarks(Bitmap bitmap) {
        DetectionResult res = detect(bitmap);
        return res != null ? res.landmarks : null;
    }

    public void close() {
        if (faceLandmarker != null) {
            try {
                faceLandmarker.close();
            } catch (Exception ignored) {}
            faceLandmarker = null;
        }
    }
}
