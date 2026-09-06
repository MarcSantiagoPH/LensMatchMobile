package com.lensmatch.mobile.utils;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * High-precision anthropometric facial analyzer.
 * Uses MediaPipe 478 3D landmarks, normalized for eye-level roll and scale (IPD),
 * to calculate true physical facial proportions and calibrated probability distributions
 * across 7 canonical face shapes (Oval, Round, Square, Oblong, Heart, Diamond, Triangle).
 */
public class GeometricFaceShapeAnalyzer {

    // =========================================================================
    // MediaPipe 478 Face Mesh Landmarks (Calibrated for Asian facial features)
    // =========================================================================
    public static final int FOREHEAD_TOP = 10;           // Trichion / top of forehead apex
    public static final int CHIN_BOTTOM = 152;           // Gnathion / chin tip

    // Forehead width: 54 & 284 (fallback: 21 & 251)
    public static final int FOREHEAD_LEFT = 54;
    public static final int FOREHEAD_RIGHT = 284;
    public static final int FOREHEAD_FALLBACK_LEFT = 21;
    public static final int FOREHEAD_FALLBACK_RIGHT = 251;

    // Cheekbone width: 137 & 366 (zygomatic arch prominence)
    public static final int CHEEK_LEFT = 137;
    public static final int CHEEK_RIGHT = 366;

    // Jaw width: 172 & 397 (gonial angle of mandible)
    public static final int JAW_LEFT = 172;
    public static final int JAW_RIGHT = 397;

    // Jaw angle & Chin curvature landmarks: 148, 149, 150, 152, 377, 378
    public static final int JAW_ANGLE_LEFT = 148;
    public static final int JAW_ANGLE_RIGHT = 377;
    public static final int CHIN_BASE_LEFT_OUTER = 149;
    public static final int CHIN_BASE_LEFT_INNER = 150;
    public static final int CHIN_BASE_RIGHT_INNER = 378;
    public static final int CHIN_BASE_RIGHT_OUTER = 377;

    private static final FaceShapeClassifierStrategy classifier = new RulesDecisionTreeClassifier();

    /**
     * Extracts normalized FaceMetrics with Pose Normalization and scale invariance.
     */
    public static FaceMetrics extractMetrics(List<NormalizedLandmark> rawLandmarks, float[] transformMatrix, int imgW, int imgH) {
        if (rawLandmarks == null || rawLandmarks.size() < 468) {
            return null;
        }

        // 1. Pose Normalization (Roll leveling + IPD scaling + Yaw/Pitch validation)
        PoseNormalizer.NormalizedFace normFace = PoseNormalizer.normalize(rawLandmarks, transformMatrix, imgW, imgH);
        if (normFace == null) {
            return null;
        }

        PoseNormalizer.Point3D[] pts = normFace.points;

        // 2. Forehead Width (Primary: 54/284, Fallback: 21/251)
        float foreheadWidthIpd = pts[FOREHEAD_LEFT].dist(pts[FOREHEAD_RIGHT]);
        if (foreheadWidthIpd < 0.25f) {
            foreheadWidthIpd = pts[FOREHEAD_FALLBACK_LEFT].dist(pts[FOREHEAD_FALLBACK_RIGHT]);
        }

        // 3. Cheekbone Width (137/366)
        float cheekWidthIpd = pts[CHEEK_LEFT].dist(pts[CHEEK_RIGHT]);

        // 4. Jaw Width (172/397)
        float jawWidthIpd = pts[JAW_LEFT].dist(pts[JAW_RIGHT]);

        // 5. Vertical Face Length (10 to 152)
        float faceLengthIpd = pts[FOREHEAD_TOP].dist(pts[CHIN_BOTTOM]);

        if (cheekWidthIpd <= 0.05f || faceLengthIpd <= 0.05f || jawWidthIpd <= 0.05f) {
            return null;
        }

        // 6. Jaw Angle Sharpness (148 - 152 - 377: vertex at 152)
        float jawAngleDeg = calculateAngleDeg(pts[JAW_ANGLE_LEFT], pts[CHIN_BOTTOM], pts[JAW_ANGLE_RIGHT]);

        // 7. Chin Curvature Score (across 149, 150, 152, 377, 378)
        // Inner chin apex angle (150 - 152 - 378)
        float angleInner = calculateAngleDeg(pts[CHIN_BASE_LEFT_INNER], pts[CHIN_BOTTOM], pts[CHIN_BASE_RIGHT_INNER]);
        // Outer chin apex angle (149 - 152 - 377)
        float angleOuter = calculateAngleDeg(pts[CHIN_BASE_LEFT_OUTER], pts[CHIN_BOTTOM], pts[CHIN_BASE_RIGHT_OUTER]);
        float compositeChinAngle = (angleInner * 0.60f) + (angleOuter * 0.40f);

        // Normalize chin curvature: 0.0 = sharp V-line (~105°), 1.0 = broad/square (~160°)
        float chinCurvatureScore = Math.max(0.0f, Math.min(1.0f, (compositeChinAngle - 105.0f) / (160.0f - 105.0f)));

        // 8. Forehead Occlusion Confidence Check (54, 284, 21, 251, 10)
        float conf54 = getConfidence(rawLandmarks.get(FOREHEAD_LEFT));
        float conf284 = getConfidence(rawLandmarks.get(FOREHEAD_RIGHT));
        float conf21 = getConfidence(rawLandmarks.get(FOREHEAD_FALLBACK_LEFT));
        float conf251 = getConfidence(rawLandmarks.get(FOREHEAD_FALLBACK_RIGHT));
        float conf10 = getConfidence(rawLandmarks.get(FOREHEAD_TOP));
        float foreheadConf = (conf54 + conf284 + conf21 + conf251 + conf10) / 5.0f;

        boolean isForeheadOccluded = foreheadConf < FaceShapeConfig.MIN_LANDMARK_CONFIDENCE;

        // Jaw Confidence
        float confJawL = getConfidence(rawLandmarks.get(JAW_LEFT));
        float confJawR = getConfidence(rawLandmarks.get(JAW_RIGHT));
        float confChin = getConfidence(rawLandmarks.get(CHIN_BOTTOM));
        float jawConf = (confJawL + confJawR + confChin) / 3.0f;

        // Key Ratios
        float widthToHeightRatio = cheekWidthIpd / faceLengthIpd;
        float jawToCheekboneRatio = jawWidthIpd / cheekWidthIpd;
        float foreheadToJawRatio = foreheadWidthIpd / jawWidthIpd;
        float foreheadToCheekRatio = foreheadWidthIpd / cheekWidthIpd;

        return new FaceMetrics(
                widthToHeightRatio,
                jawToCheekboneRatio,
                foreheadToJawRatio,
                foreheadToCheekRatio,
                jawAngleDeg,
                chinCurvatureScore,
                foreheadConf,
                isForeheadOccluded,
                jawConf,
                faceLengthIpd,
                cheekWidthIpd,
                foreheadWidthIpd,
                jawWidthIpd
        );
    }

    public static FaceMetrics extractMetrics(List<NormalizedLandmark> rawLandmarks, int imgW, int imgH) {
        return extractMetrics(rawLandmarks, null, imgW, imgH);
    }

    /**
     * Legacy adapter method returning array of shape probabilities corresponding to shapeLabels.
     */
    public static float[] calculateShapeProbabilities(List<NormalizedLandmark> landmarks, float[] transformMatrix, int width, int height, List<String> shapeLabels) {
        float[] probs = new float[shapeLabels.size()];
        FaceMetrics metrics = extractMetrics(landmarks, transformMatrix, width, height);

        if (metrics == null) {
            for (int i = 0; i < probs.length; i++) probs[i] = 1.0f / probs.length;
            return probs;
        }

        FaceShapeClassifierStrategy.ClassificationResult result = classifier.classify(metrics);
        Map<String, Float> shapeMap = result.getShapeProbabilities();

        float sum = 0f;
        for (int i = 0; i < shapeLabels.size(); i++) {
            String label = shapeLabels.get(i);
            probs[i] = (shapeMap != null && shapeMap.containsKey(label)) ? shapeMap.get(label) : 0.01f;
            sum += probs[i];
        }

        if (sum > 0) {
            for (int i = 0; i < probs.length; i++) {
                probs[i] /= sum;
            }
        }

        return probs;
    }

    public static float[] calculateShapeProbabilities(List<NormalizedLandmark> landmarks, int width, int height, List<String> shapeLabels) {
        return calculateShapeProbabilities(landmarks, null, width, height, shapeLabels);
    }

    public static float[] calculateShapeProbabilities(List<NormalizedLandmark> landmarks, List<String> shapeLabels) {
        return calculateShapeProbabilities(landmarks, null, 1000, 1000, shapeLabels);
    }

    public static FaceShapeClassifierStrategy.ClassificationResult analyze(List<NormalizedLandmark> landmarks, float[] transformMatrix, int width, int height) {
        FaceMetrics metrics = extractMetrics(landmarks, transformMatrix, width, height);
        return classifier.classify(metrics);
    }

    private static float getConfidence(NormalizedLandmark lm) {
        if (lm == null) return 0f;
        float p = lm.presence().isPresent() ? lm.presence().get() : 1.0f;
        float v = lm.visibility().isPresent() ? lm.visibility().get() : 1.0f;
        return Math.min(p, v);
    }

    private static float calculateAngleDeg(PoseNormalizer.Point3D p1, PoseNormalizer.Point3D vertex, PoseNormalizer.Point3D p2) {
        float v1x = p1.x - vertex.x;
        float v1y = p1.y - vertex.y;
        float v2x = p2.x - vertex.x;
        float v2y = p2.y - vertex.y;

        float dot = (v1x * v2x) + (v1y * v2y);
        float mag1 = (float) Math.sqrt(v1x * v1x + v1y * v1y);
        float mag2 = (float) Math.sqrt(v2x * v2x + v2y * v2y);

        if (mag1 <= 0.0001f || mag2 <= 0.0001f) return 90f;
        float cos = Math.max(-1f, Math.min(1f, dot / (mag1 * mag2)));
        return (float) Math.toDegrees(Math.acos(cos));
    }
}
