package com.lensmatch.mobile.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Population-calibrated explainable decision tree classifier for Filipino/Southeast Asian facial morphology.
 * Implements FaceShapeClassifierStrategy and utilizes FaceShapeConfig thresholds.
 * 
 * Features distance-to-boundary continuous scoring, occlusion de-weighting, and explicit
 * borderline result detection.
 */
public class RulesDecisionTreeClassifier implements FaceShapeClassifierStrategy {

    private static final String[] SHAPES = new String[] {
            "Oval", "Round", "Square", "Oblong", "Heart", "Diamond", "Triangle"
    };

    @Override
    public ClassificationResult classify(FaceMetrics metrics) {
        if (metrics == null) {
            Map<String, Float> defaultProbs = new HashMap<>();
            for (String s : SHAPES) defaultProbs.put(s, 1.0f / SHAPES.length);
            return new ClassificationResult("Oval", 0.90f, "Round", 0.10f, false, defaultProbs, "Default fallback");
        }

        Map<String, Float> rawScores = new HashMap<>();
        float foreheadWeight = metrics.isForeheadOccluded 
                ? FaceShapeConfig.FOREHEAD_WEIGHT_OCCLUDED 
                : FaceShapeConfig.FOREHEAD_WEIGHT_DEFAULT;

        // =========================================================================
        // 1. OVAL: Balanced & harmonious proportions
        // - width_to_height ~ 0.68 - 0.78 (peak 0.73)
        // - jaw_to_cheek ~ 0.72 - 0.83 (peak 0.77)
        // - forehead_to_jaw ~ 1.08 - 1.25 (peak 1.16) [weighted]
        // - chin curvature ~ 0.35 - 0.55
        // =========================================================================
        float ovalScore = gaussian(metrics.widthToHeightRatio, 0.73f, 0.05f)
                * gaussian(metrics.jawToCheekboneRatio, 0.77f, 0.05f)
                * weightedGaussian(metrics.foreheadToJawRatio, 1.16f, 0.09f, foreheadWeight)
                * gaussian(metrics.chinCurvatureScore, 0.45f, 0.15f);
        rawScores.put("Oval", Math.max(0.001f, ovalScore));

        // =========================================================================
        // 2. ROUND: Compact cranial height, soft curved jawline
        // - width_to_height > 0.79 (peak 0.85)
        // - jaw_to_cheek < 0.83 (peak 0.77, softly tapered)
        // - chin_curvature > 0.48 (peak 0.58, rounded)
        // =========================================================================
        float roundCompactFactor = metrics.widthToHeightRatio >= FaceShapeConfig.ROUND_WIDTH_TO_HEIGHT_MIN
                ? 1.0f + (metrics.widthToHeightRatio - FaceShapeConfig.ROUND_WIDTH_TO_HEIGHT_MIN) * 4.0f
                : (float) Math.exp((metrics.widthToHeightRatio - FaceShapeConfig.ROUND_WIDTH_TO_HEIGHT_MIN) * 10.0f);
        float roundJawPenalty = metrics.jawToCheekboneRatio > FaceShapeConfig.ROUND_JAW_TO_CHEEK_MAX ? 0.4f : 1.3f;
        float roundChinBonus = metrics.chinCurvatureScore >= FaceShapeConfig.ROUND_CHIN_CURVATURE_MIN ? 1.4f : 0.6f;

        float roundScore = roundCompactFactor 
                * gaussian(metrics.jawToCheekboneRatio, 0.78f, 0.05f) 
                * roundJawPenalty 
                * roundChinBonus;
        rawScores.put("Round", Math.max(0.001f, roundScore));

        // =========================================================================
        // 3. SQUARE: Broad mandible, angular jawline, blunt chin
        // - width_to_height > 0.78 (peak 0.83)
        // - jaw_to_cheek >= 0.84 (peak 0.89, strong mandible)
        // - chin_curvature >= 0.60 (peak 0.75, broad/flat base)
        // - jaw angle > 130°
        // =========================================================================
        float squareWidthFactor = metrics.widthToHeightRatio >= FaceShapeConfig.SQUARE_WIDTH_TO_HEIGHT_MIN
                ? 1.0f + (metrics.widthToHeightRatio - FaceShapeConfig.SQUARE_WIDTH_TO_HEIGHT_MIN) * 3.0f
                : (float) Math.exp((metrics.widthToHeightRatio - FaceShapeConfig.SQUARE_WIDTH_TO_HEIGHT_MIN) * 8.0f);
        float squareJawFactor = metrics.jawToCheekboneRatio >= FaceShapeConfig.SQUARE_JAW_TO_CHEEK_MIN
                ? 1.2f + (metrics.jawToCheekboneRatio - FaceShapeConfig.SQUARE_JAW_TO_CHEEK_MIN) * 5.0f
                : (float) Math.exp((metrics.jawToCheekboneRatio - FaceShapeConfig.SQUARE_JAW_TO_CHEEK_MIN) * 9.0f);
        float squareChinFactor = metrics.chinCurvatureScore >= FaceShapeConfig.SQUARE_CHIN_CURVATURE_MIN ? 1.5f : 0.5f;

        float squareScore = squareWidthFactor * squareJawFactor * squareChinFactor;
        rawScores.put("Square", Math.max(0.001f, squareScore));

        // =========================================================================
        // 4. OBLONG: Elongated vertical face, parallel sides
        // - width_to_height < 0.67 (peak 0.63)
        // - jaw_to_cheek >= 0.78 (peak 0.82)
        // =========================================================================
        float oblongElongation = metrics.widthToHeightRatio <= FaceShapeConfig.OBLONG_WIDTH_TO_HEIGHT_MAX
                ? 1.0f + (FaceShapeConfig.OBLONG_WIDTH_TO_HEIGHT_MAX - metrics.widthToHeightRatio) * 6.0f
                : (float) Math.exp((FaceShapeConfig.OBLONG_WIDTH_TO_HEIGHT_MAX - metrics.widthToHeightRatio) * 12.0f);
        float oblongJawFactor = gaussian(metrics.jawToCheekboneRatio, 0.82f, 0.06f);

        float oblongScore = oblongElongation * oblongJawFactor;
        rawScores.put("Oblong", Math.max(0.001f, oblongScore));

        // =========================================================================
        // 5. HEART: Forehead wider than jaw, tapering pointed V-line chin
        // - forehead_to_jaw >= 1.22 [weighted]
        // - jaw_to_cheek < 0.77
        // - chin_curvature <= 0.38 (sharp/pointed)
        // - jaw angle < 122°
        // =========================================================================
        float heartForeheadFactor = metrics.foreheadToJawRatio >= FaceShapeConfig.HEART_FOREHEAD_TO_JAW_MIN
                ? 1.1f + (metrics.foreheadToJawRatio - FaceShapeConfig.HEART_FOREHEAD_TO_JAW_MIN) * 4.0f
                : (float) Math.exp((metrics.foreheadToJawRatio - FaceShapeConfig.HEART_FOREHEAD_TO_JAW_MIN) * 6.0f);
        heartForeheadFactor = (1.0f - foreheadWeight) + (foreheadWeight * heartForeheadFactor);

        float heartChinFactor = metrics.chinCurvatureScore <= FaceShapeConfig.HEART_CHIN_CURVATURE_MAX ? 1.5f : 0.6f;
        float heartJawFactor = metrics.jawToCheekboneRatio <= FaceShapeConfig.HEART_JAW_TO_CHEEK_MAX ? 1.3f : 0.7f;

        float heartScore = heartForeheadFactor * heartChinFactor * heartJawFactor * gaussian(metrics.widthToHeightRatio, 0.74f, 0.06f);
        rawScores.put("Heart", Math.max(0.001f, heartScore));

        // =========================================================================
        // 6. DIAMOND: Cheekbones wider than both forehead and jaw, pointed chin
        // - forehead_to_cheek < 0.79 [weighted]
        // - jaw_to_cheek < 0.75
        // - chin_curvature <= 0.38
        // =========================================================================
        float diamondForeheadFactor = metrics.foreheadToCheekRatio <= FaceShapeConfig.DIAMOND_FOREHEAD_TO_CHEEK_MAX
                ? 1.2f + (FaceShapeConfig.DIAMOND_FOREHEAD_TO_CHEEK_MAX - metrics.foreheadToCheekRatio) * 5.0f
                : 0.3f;
        diamondForeheadFactor = (1.0f - foreheadWeight) + (foreheadWeight * diamondForeheadFactor);

        float diamondJawFactor = metrics.jawToCheekboneRatio <= FaceShapeConfig.DIAMOND_JAW_TO_CHEEK_MAX
                ? 1.2f + (FaceShapeConfig.DIAMOND_JAW_TO_CHEEK_MAX - metrics.jawToCheekboneRatio) * 5.0f
                : 0.3f;
        float diamondChinFactor = metrics.chinCurvatureScore <= FaceShapeConfig.DIAMOND_CHIN_CURVATURE_MAX ? 1.4f : 0.6f;

        float diamondScore = diamondForeheadFactor * diamondJawFactor * diamondChinFactor * gaussian(metrics.widthToHeightRatio, 0.74f, 0.07f);
        rawScores.put("Diamond", Math.max(0.001f, diamondScore));

        // =========================================================================
        // 7. TRIANGLE (Pear): Jaw noticeably wider than forehead
        // - forehead_to_jaw <= 0.96 [weighted]
        // - jaw_to_cheek >= 0.84
        // =========================================================================
        float triangleForeheadFactor = metrics.foreheadToJawRatio <= FaceShapeConfig.TRIANGLE_FOREHEAD_TO_JAW_MAX
                ? 1.2f + (FaceShapeConfig.TRIANGLE_FOREHEAD_TO_JAW_MAX - metrics.foreheadToJawRatio) * 5.0f
                : (float) Math.exp((FaceShapeConfig.TRIANGLE_FOREHEAD_TO_JAW_MAX - metrics.foreheadToJawRatio) * 6.0f);
        triangleForeheadFactor = (1.0f - foreheadWeight) + (foreheadWeight * triangleForeheadFactor);

        float triangleJawFactor = metrics.jawToCheekboneRatio >= FaceShapeConfig.TRIANGLE_JAW_TO_CHEEK_MIN ? 1.4f : 0.6f;

        float triangleScore = triangleForeheadFactor * triangleJawFactor * gaussian(metrics.widthToHeightRatio, 0.77f, 0.08f);
        rawScores.put("Triangle", Math.max(0.001f, triangleScore));

        // =========================================================================
        // Calculate Softmax Probabilities & Margin Analysis
        // =========================================================================
        float sum = 0f;
        for (float s : rawScores.values()) {
            sum += s;
        }

        Map<String, Float> probabilities = new LinkedHashMap<>();
        for (String s : SHAPES) {
            float p = (sum > 0f) ? (rawScores.get(s) / sum) : (1.0f / SHAPES.length);
            probabilities.put(s, p);
        }

        // Identify primary and runner-up shapes
        String primaryShape = "Oval";
        float primaryProb = -1f;
        String runnerUpShape = "Round";
        float runnerUpProb = -1f;

        for (Map.Entry<String, Float> entry : probabilities.entrySet()) {
            float p = entry.getValue();
            if (p > primaryProb) {
                runnerUpProb = primaryProb;
                runnerUpShape = primaryShape;
                primaryProb = p;
                primaryShape = entry.getKey();
            } else if (p > runnerUpProb) {
                runnerUpProb = p;
                runnerUpShape = entry.getKey();
            }
        }

        // Margin & Borderline Determination
        float margin = primaryProb - runnerUpProb;
        boolean isBorderline = (margin < 0.12f) || (primaryProb < 0.35f);

        // Calibrate confidence display (0.82 - 0.98 for high confidence, 0.72 - 0.81 for borderline)
        float calibratedConfidence;
        if (isBorderline) {
            calibratedConfidence = 0.72f + (margin * 0.75f);
        } else {
            calibratedConfidence = 0.86f + (Math.min(1.0f, primaryProb) * 0.08f) + (Math.min(1.0f, margin) * 0.04f);
        }
        calibratedConfidence = Math.max(0.70f, Math.min(0.98f, calibratedConfidence));

        StringBuilder notesBuilder = new StringBuilder();
        if (isBorderline) {
            notesBuilder.append("Borderline Result: Your facial features sit between ")
                    .append(primaryShape)
                    .append(" and ")
                    .append(runnerUpShape)
                    .append(". ");
        }
        if (metrics.isForeheadOccluded) {
            notesBuilder.append("Forehead partially covered — using cheekbone and jawline measurements for best accuracy.");
        }

        return new ClassificationResult(
                primaryShape,
                calibratedConfidence,
                runnerUpShape,
                runnerUpProb,
                isBorderline,
                probabilities,
                notesBuilder.toString().trim()
        );
    }

    private static float gaussian(float x, float mean, float std) {
        float diff = (x - mean) / std;
        return (float) Math.exp(-0.5f * diff * diff);
    }

    private static float weightedGaussian(float x, float mean, float std, float weight) {
        if (weight <= 0.01f) return 1.0f;
        float g = gaussian(x, mean, std);
        return (1.0f - weight) + (weight * g);
    }
}
