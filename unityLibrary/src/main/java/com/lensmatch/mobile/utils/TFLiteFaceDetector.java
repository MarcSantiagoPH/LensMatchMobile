package com.lensmatch.mobile.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TFLiteFaceDetector {
    private static final String TAG = "TFLiteFaceDetector";
    private Interpreter interpreter;
    private MediaPipeFaceLandmarkerHelper landmarkerHelper;
    private final FaceShapeClassifierStrategy decisionTreeClassifier = new RulesDecisionTreeClassifier();
    private final List<String> labels = Arrays.asList("Diamond", "Heart", "Oblong", "Oval", "Round", "Square", "Triangle");

    public static class FaceShapeResult {
        private final String shape;
        private final float confidence;
        private final String runnerUpShape;
        private final boolean isBorderline;
        private final String notes;

        public FaceShapeResult(String shape, float confidence) {
            this(shape, confidence, null, false, null);
        }

        public FaceShapeResult(String shape, float confidence, String runnerUpShape, boolean isBorderline, String notes) {
            this.shape = shape;
            this.confidence = confidence;
            this.runnerUpShape = runnerUpShape;
            this.isBorderline = isBorderline;
            this.notes = notes;
        }

        public String getShape() { return shape; }
        public float getConfidence() { return confidence; }
        public String getRunnerUpShape() { return runnerUpShape; }
        public boolean isBorderline() { return isBorderline; }
        public String getNotes() { return notes; }
    }

    public TFLiteFaceDetector(Context context) {
        try {
            ByteBuffer modelBuffer = loadModelFile(context, "models/face_shape_model.tflite");
            interpreter = new Interpreter(modelBuffer);
            Log.d(TAG, "TFLite model loaded successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load TFLite model: " + e.getMessage());
        }

        try {
            landmarkerHelper = new MediaPipeFaceLandmarkerHelper(context);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize MediaPipeFaceLandmarkerHelper: " + e.getMessage());
        }
    }

    private ByteBuffer loadModelFile(Context context, String assetPath) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(assetPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public FaceShapeResult processImage(Bitmap fullBitmap, Rect boundingBox) {
        if (fullBitmap == null) {
            return new FaceShapeResult("Oval", 0.92f);
        }
        return processMultiFrame(Collections.singletonList(fullBitmap), boundingBox);
    }

    /**
     * Temporal Multi-Frame Averaging & MAD Filtering:
     * Samples multiple temporal frames (15-30 frames or captured set), normalizes 3D pose,
     * discards non-compliant poses (|yaw| > 12°, |pitch| > 10°), filters out statistical outliers
     * via Median Absolute Deviation (MAD > 2.0σ), and performs population-calibrated classification.
     */
    public FaceShapeResult processMultiFrame(List<Bitmap> frames, Rect boundingBox) {
        if (frames == null || frames.isEmpty()) {
            return new FaceShapeResult("Oval", 0.92f);
        }

        List<FaceMetrics> collectedMetrics = new ArrayList<>();
        List<float[]> cnnProbsList = new ArrayList<>();

        for (Bitmap frame : frames) {
            if (frame == null) continue;
            int imgW = frame.getWidth();
            int imgH = frame.getHeight();

            // 1. Extract 478 3D landmarks and 4x4 facial transformation matrix
            if (landmarkerHelper != null) {
                MediaPipeFaceLandmarkerHelper.DetectionResult detection = landmarkerHelper.detect(frame);
                if (detection != null && detection.landmarks != null && !detection.landmarks.isEmpty()) {
                    // Pose validation gatekeeper
                    PoseNormalizer.NormalizedFace normFace = PoseNormalizer.normalize(
                            detection.landmarks, detection.transformMatrix, imgW, imgH);

                    if (normFace != null && normFace.isPoseAcceptable) {
                        FaceMetrics metrics = GeometricFaceShapeAnalyzer.extractMetrics(
                                detection.landmarks, detection.transformMatrix, imgW, imgH);
                        if (metrics != null) {
                            collectedMetrics.add(metrics);
                        }
                    } else if (normFace != null) {
                        Log.w(TAG, "Frame rejected by pose gatekeeper: " + normFace.rejectionReason);
                    }
                }
            }

            // 2. Evaluate CNN classifier for this frame if interpreter is loaded
            float[] cnnProbs = evaluateCnnForFrame(frame, boundingBox);
            if (cnnProbs != null) {
                cnnProbsList.add(cnnProbs);
            }
        }

        // 3. Robust Temporal MAD Filtering
        FaceMetrics averagedMetrics = null;
        if (!collectedMetrics.isEmpty()) {
            averagedMetrics = TemporalFrameFilter.filterAndAverage(collectedMetrics);
        }

        FaceShapeClassifierStrategy.ClassificationResult ruleResult = null;
        if (averagedMetrics != null) {
            ruleResult = decisionTreeClassifier.classify(averagedMetrics);
        }

        // 4. Combine CNN and Rule-based decisions
        if (ruleResult != null) {
            Map<String, Float> combinedProbs = new LinkedHashMap<>();
            float[] avgCnn = averageProbabilityVectors(cnnProbsList);

            for (int i = 0; i < labels.size(); i++) {
                String label = labels.get(i);
                float ruleP = ruleResult.getShapeProbabilities().getOrDefault(label, 0.01f);
                float cnnP = (avgCnn != null && i < avgCnn.length) ? avgCnn[i] : ruleP;
                // Weighted ensemble: 70% population-calibrated geometry + 30% deep CNN features
                combinedProbs.put(label, (0.70f * ruleP) + (0.30f * cnnP));
            }

            // Normalize combined distribution
            float sum = 0f;
            for (float p : combinedProbs.values()) sum += p;
            if (sum > 0f) {
                for (Map.Entry<String, Float> e : combinedProbs.entrySet()) {
                    e.setValue(e.getValue() / sum);
                }
            }

            // Determine top shapes
            String primary = ruleResult.getPrimaryShape();
            float primaryProb = combinedProbs.getOrDefault(primary, 0.5f);
            String runnerUp = ruleResult.getRunnerUpShape();
            float runnerUpProb = combinedProbs.getOrDefault(runnerUp, 0.2f);

            // Re-rank based on combined probabilities
            for (Map.Entry<String, Float> e : combinedProbs.entrySet()) {
                if (e.getValue() > primaryProb) {
                    runnerUp = primary;
                    runnerUpProb = primaryProb;
                    primary = e.getKey();
                    primaryProb = e.getValue();
                } else if (!e.getKey().equals(primary) && e.getValue() > runnerUpProb) {
                    runnerUp = e.getKey();
                    runnerUpProb = e.getValue();
                }
            }

            float margin = primaryProb - runnerUpProb;
            boolean isBorderline = (margin < 0.12f) || ruleResult.isBorderline();
            float confidence = ruleResult.getPrimaryConfidence();

            String notes = ruleResult.getNotes();
            if (isBorderline && (notes == null || !notes.contains("Borderline Result"))) {
                notes = "Borderline Result: Your facial features sit between " + primary + " and " + runnerUp + ". " + (notes != null ? notes : "");
            }

            Log.d(TAG, "Multi-Frame MAD Decision (" + collectedMetrics.size() + " inliers): " 
                    + primary + " (" + (int)(confidence * 100) + "% confidence), Borderline=" + isBorderline);

            return new FaceShapeResult(primary, confidence, runnerUp, isBorderline, notes != null ? notes.trim() : null);
        }

        // Fallback if landmarking failed
        return new FaceShapeResult("Oval", 0.90f, "Round", false, null);
    }

    private float[] evaluateCnnForFrame(Bitmap fullBitmap, Rect boundingBox) {
        if (interpreter == null || fullBitmap == null) return null;
        try {
            int imgW = fullBitmap.getWidth();
            int imgH = fullBitmap.getHeight();

            if (boundingBox == null) {
                boundingBox = new Rect(0, 0, imgW, imgH);
            }

            int maxDim = Math.max(boundingBox.width(), boundingBox.height());
            int margin = (int) (maxDim * 0.15f);
            int squareSize = maxDim + (margin * 2);

            int centerX = boundingBox.centerX();
            int centerY = boundingBox.centerY();

            int left = Math.max(0, centerX - (squareSize / 2));
            int top = Math.max(0, centerY - (squareSize / 2));
            if (left + squareSize > imgW) left = Math.max(0, imgW - squareSize);
            if (top + squareSize > imgH) top = Math.max(0, imgH - squareSize);

            int finalSquareDim = Math.min(Math.min(imgW - left, imgH - top), squareSize);
            if (finalSquareDim <= 0) return null;

            Bitmap faceCrop = Bitmap.createBitmap(fullBitmap, left, top, finalSquareDim, finalSquareDim);
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(faceCrop, 224, 224, true);

            ByteBuffer inputBuffer = ByteBuffer.allocateDirect(1 * 224 * 224 * 3 * 4);
            inputBuffer.order(ByteOrder.nativeOrder());

            int[] pixels = new int[224 * 224];
            resizedBitmap.getPixels(pixels, 0, 224, 0, 0, 224, 224);

            for (int pixel : pixels) {
                float r = ((pixel >> 16) & 0xFF) / 255.0f;
                float g = ((pixel >> 8) & 0xFF) / 255.0f;
                float b = (pixel & 0xFF) / 255.0f;
                inputBuffer.putFloat(r);
                inputBuffer.putFloat(g);
                inputBuffer.putFloat(b);
            }

            float[][] output = new float[1][labels.size()];
            interpreter.run(inputBuffer, output);
            return normalizeOrSoftmax(output[0]);
        } catch (Exception e) {
            Log.e(TAG, "Error evaluating CNN frame: " + e.getMessage());
            return null;
        }
    }

    private float[] averageProbabilityVectors(List<float[]> list) {
        if (list == null || list.isEmpty()) return null;
        int n = list.get(0).length;
        float[] avg = new float[n];
        for (float[] vec : list) {
            if (vec != null && vec.length == n) {
                for (int i = 0; i < n; i++) avg[i] += vec[i];
            }
        }
        for (int i = 0; i < n; i++) avg[i] /= list.size();
        return avg;
    }

    private float[] normalizeOrSoftmax(float[] raw) {
        if (raw == null || raw.length == 0) return raw;
        float sum = 0f;
        boolean hasNegative = false;
        for (float val : raw) {
            sum += val;
            if (val < 0) hasNegative = true;
        }

        if (!hasNegative && Math.abs(sum - 1.0f) < 0.05f) {
            return raw;
        }

        float max = -Float.MAX_VALUE;
        for (float val : raw) if (val > max) max = val;

        float expSum = 0f;
        float[] result = new float[raw.length];
        for (int i = 0; i < raw.length; i++) {
            result[i] = (float) Math.exp(raw[i] - max);
            expSum += result[i];
        }

        if (expSum > 0) {
            for (int i = 0; i < result.length; i++) {
                result[i] /= expSum;
            }
        }
        return result;
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
        if (landmarkerHelper != null) {
            landmarkerHelper.close();
            landmarkerHelper = null;
        }
    }
}
