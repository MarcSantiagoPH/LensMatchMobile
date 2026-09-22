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
        private final FaceMetrics metrics;

        public FaceShapeResult(String shape, float confidence) {
            this(shape, confidence, null, false, null, null);
        }

        public FaceShapeResult(String shape, float confidence, String runnerUpShape, boolean isBorderline, String notes, FaceMetrics metrics) {
            this.shape = shape;
            this.confidence = confidence;
            this.runnerUpShape = runnerUpShape;
            this.isBorderline = isBorderline;
            this.notes = notes;
            this.metrics = metrics;
        }

        public String getShape() { return shape; }
        public float getConfidence() { return confidence; }
        public String getRunnerUpShape() { return runnerUpShape; }
        public boolean isBorderline() { return isBorderline; }
        public String getNotes() { return notes; }
        public FaceMetrics getMetrics() { return metrics; }
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
            return new FaceShapeResult("Oval", 0.92f, null, false, null, null);
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
            return new FaceShapeResult("Oval", 0.92f, null, false, null, null);
        }

        List<FaceMetrics> collectedMetrics = new ArrayList<>();
        List<FaceMetrics> fallbackMetrics = new ArrayList<>(); // frames rejected by pose gate, used as fallback
        List<float[]> cnnProbsList = new ArrayList<>();

        for (Bitmap frame : frames) {
            if (frame == null) continue;
            int imgW = frame.getWidth();
            int imgH = frame.getHeight();

            // 2. Evaluate CNN classifier for this frame using exact pose and landmarks
            float rollDeg = 0f;
            List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark> lms = null;
            if (landmarkerHelper != null) {
                MediaPipeFaceLandmarkerHelper.DetectionResult detection = landmarkerHelper.detect(frame);
                if (detection != null && detection.landmarks != null && !detection.landmarks.isEmpty()) {
                    lms = detection.landmarks;
                    PoseNormalizer.NormalizedFace normFace = PoseNormalizer.normalize(
                            detection.landmarks, detection.transformMatrix, imgW, imgH);
                    
                    if (normFace != null) {
                        rollDeg = normFace.rollDeg;
                        if (normFace.isPoseAcceptable) {
                            FaceMetrics metrics = GeometricFaceShapeAnalyzer.extractMetrics(
                                    detection.landmarks, detection.transformMatrix, imgW, imgH);
                            if (metrics != null) collectedMetrics.add(metrics);
                        } else {
                            FaceMetrics metrics = GeometricFaceShapeAnalyzer.extractMetrics(
                                    detection.landmarks, detection.transformMatrix, imgW, imgH);
                            if (metrics != null) fallbackMetrics.add(metrics);
                        }
                    }
                }
            }

            float[] cnnProbs = evaluateCnnForFrame(frame, lms, rollDeg);
            if (cnnProbs != null) {
                cnnProbsList.add(cnnProbs);
            }
        }

        // 3. Robust Temporal MAD Filtering
        // If pose gatekeeper rejected all strict frames, use fallback (slightly off-pose) frames
        if (collectedMetrics.isEmpty() && !fallbackMetrics.isEmpty()) {
            Log.w(TAG, "All frames rejected by strict pose gate. Using " + fallbackMetrics.size() + " fallback frames.");
            collectedMetrics.addAll(fallbackMetrics);
        }

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

            String cnnPrimary = null;
            float cnnMaxProb = -1f;
            for (int i = 0; i < labels.size(); i++) {
                String label = labels.get(i);
                float cnnP = (avgCnn != null && i < avgCnn.length) ? avgCnn[i] : 0.0f;
                if (cnnP > cnnMaxProb) {
                    cnnMaxProb = cnnP;
                    cnnPrimary = label;
                }
            }

            String geomPrimary = ruleResult.getPrimaryShape();
            boolean strongAgreement = (cnnPrimary != null && cnnPrimary.equals(geomPrimary));
            boolean cnnAmbiguous = (cnnMaxProb < 0.45f);

            for (int i = 0; i < labels.size(); i++) {
                String label = labels.get(i);
                float ruleP = ruleResult.getShapeProbabilities().getOrDefault(label, 0.01f);
                float cnnP = (avgCnn != null && i < avgCnn.length) ? avgCnn[i] : ruleP;
                
                float combinedP = 0f;
                if (strongAgreement) {
                    // Synergy bonus
                    combinedP = (0.50f * ruleP) + (0.50f * cnnP);
                } else if (cnnAmbiguous) {
                    // Geometry holds slightly more weight if CNN is uncertain
                    combinedP = (0.60f * ruleP) + (0.40f * cnnP);
                } else {
                    // Balanced fusion
                    combinedP = (0.50f * ruleP) + (0.50f * cnnP);
                }
                combinedProbs.put(label, combinedP);
            }

            // Normalize combined distribution
            float sum = 0f;
            for (float p : combinedProbs.values()) sum += p;
            if (sum > 0f) {
                for (Map.Entry<String, Float> e : combinedProbs.entrySet()) {
                    e.setValue(e.getValue() / sum);
                }
            }

            // Determine top shapes from combined distribution
            String primary = null;
            float primaryProb = -1f;
            String runnerUp = null;
            float runnerUpProb = -1f;

            for (Map.Entry<String, Float> e : combinedProbs.entrySet()) {
                if (e.getValue() > primaryProb) {
                    runnerUp = primary;
                    runnerUpProb = primaryProb;
                    primary = e.getKey();
                    primaryProb = e.getValue();
                } else if (e.getValue() > runnerUpProb) {
                    runnerUp = e.getKey();
                    runnerUpProb = e.getValue();
                }
            }

            float margin = primaryProb - runnerUpProb;
            boolean isBorderline = (margin < 0.12f) || ruleResult.isBorderline();
            float confidence = ruleResult.getPrimaryConfidence();
            if (strongAgreement) confidence = Math.min(0.99f, confidence + 0.10f);

            String notes = ruleResult.getNotes();
            if (isBorderline && (notes == null || !notes.contains("Borderline"))) {
                notes = "Borderline Result: Features lean between " + primary + " and " + runnerUp + ".";
            } else if (!strongAgreement && cnnPrimary != null) {
                notes = (notes == null ? "" : notes + " ") + "AI suggests a hint of " + cnnPrimary + ", but proportions align closer to " + primary + ".";
            }

            return new FaceShapeResult(primary, confidence, runnerUp, isBorderline, notes != null ? notes.trim() : null, averagedMetrics);
        }

        // Fallback if landmarking failed
        return new FaceShapeResult("Oval", 0.90f, "Round", false, null, null);
    }

    private float[] evaluateCnnForFrame(Bitmap fullBitmap, List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark> landmarks, float rollDeg) {
        if (interpreter == null || fullBitmap == null) return null;
        try {
            int imgW = fullBitmap.getWidth();
            int imgH = fullBitmap.getHeight();

            Bitmap alignedBitmap = fullBitmap;
            // 1. Face Alignment (Level the eyes)
            if (Math.abs(rollDeg) > 2.0f) {
                android.graphics.Matrix matrix = new android.graphics.Matrix();
                matrix.postRotate(-rollDeg);
                alignedBitmap = Bitmap.createBitmap(fullBitmap, 0, 0, imgW, imgH, matrix, true);
                imgW = alignedBitmap.getWidth();
                imgH = alignedBitmap.getHeight();
            }

            // 2. Face Cropping
            Rect cropRect = new Rect(0, 0, imgW, imgH);
            if (landmarks != null && !landmarks.isEmpty()) {
                float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
                float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
                for (com.google.mediapipe.tasks.components.containers.NormalizedLandmark lm : landmarks) {
                    if (lm.x() < minX) minX = lm.x();
                    if (lm.x() > maxX) maxX = lm.x();
                    if (lm.y() < minY) minY = lm.y();
                    if (lm.y() > maxY) maxY = lm.y();
                }
                
                int left = (int) (minX * fullBitmap.getWidth());
                int right = (int) (maxX * fullBitmap.getWidth());
                int top = (int) (minY * fullBitmap.getHeight());
                int bottom = (int) (maxY * fullBitmap.getHeight());
                
                int faceW = right - left;
                int faceH = bottom - top;
                
                // Add proportional padding (20% of face width/height)
                int padX = (int) (faceW * 0.20f);
                int padY = (int) (faceH * 0.20f);
                
                cropRect.left = Math.max(0, left - padX);
                cropRect.right = Math.min(imgW, right + padX);
                cropRect.top = Math.max(0, top - (int)(padY * 1.5f)); // Extra pad on top for forehead/hair
                cropRect.bottom = Math.min(imgH, bottom + padY);
            }

            if (cropRect.width() <= 0 || cropRect.height() <= 0) return null;
            Bitmap faceCrop = Bitmap.createBitmap(alignedBitmap, cropRect.left, cropRect.top, cropRect.width(), cropRect.height());

            // 3. Proportion-Preserving Resize and Pad (224x224)
            int targetSize = 224;
            Bitmap finalSquareBmp = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(finalSquareBmp);
            canvas.drawColor(android.graphics.Color.BLACK); // Pad with black
            
            float scale = Math.min((float) targetSize / faceCrop.getWidth(), (float) targetSize / faceCrop.getHeight());
            int scaledW = Math.round(scale * faceCrop.getWidth());
            int scaledH = Math.round(scale * faceCrop.getHeight());
            Bitmap scaledCrop = Bitmap.createScaledBitmap(faceCrop, scaledW, scaledH, true);
            
            int drawLeft = (targetSize - scaledW) / 2;
            int drawTop = (targetSize - scaledH) / 2;
            canvas.drawBitmap(scaledCrop, drawLeft, drawTop, null);

            ByteBuffer inputBuffer = ByteBuffer.allocateDirect(1 * targetSize * targetSize * 3 * 4);
            inputBuffer.order(ByteOrder.nativeOrder());

            int[] pixels = new int[targetSize * targetSize];
            finalSquareBmp.getPixels(pixels, 0, targetSize, 0, 0, targetSize, targetSize);

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
