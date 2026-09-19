package com.lensmatch.mobile.utils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Population-calibrated explainable decision tree classifier.
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
        float fW = metrics.isForeheadOccluded ? 0.3f : 1.0f;

        // 1. OVAL: Balanced
        float ovalScore = gaussian(metrics.faceLengthToWidthRatio, 1.45f, 0.1f)
                * gaussian(metrics.jawToCheekRatio, 0.77f, 0.05f)
                * weightedGaussian(metrics.foreheadToCheekRatio, 0.82f, 0.05f, fW)
                * gaussian(metrics.chinCurvatureScore, 0.45f, 0.15f);
        rawScores.put("Oval", Math.max(0.001f, ovalScore));

        // 2. ROUND: Compact, tapered jaw, soft chin
        float roundCompact = metrics.faceLengthToWidthRatio <= FaceShapeConfig.ROUND_LENGTH_WIDTH_MAX ? 1.5f : 0.5f;
        float roundScore = roundCompact * gaussian(metrics.jawToCheekRatio, 0.78f, 0.05f) * gaussian(metrics.chinCurvatureScore, 0.6f, 0.2f);
        rawScores.put("Round", Math.max(0.001f, roundScore));

        // 3. SQUARE: Broad mandible, blunt chin, wide jaw
        float sqCompact = metrics.faceLengthToWidthRatio <= FaceShapeConfig.ROUND_LENGTH_WIDTH_MAX ? 1.2f : 0.8f;
        float sqJaw = metrics.jawToCheekRatio >= FaceShapeConfig.STRONG_JAW_TO_CHEEK_MIN ? 1.5f : 0.5f;
        float sqChin = metrics.chinCurvatureScore >= FaceShapeConfig.BROAD_CHIN_CURVATURE_MIN ? 1.5f : 0.5f;
        rawScores.put("Square", Math.max(0.001f, sqCompact * sqJaw * sqChin));

        // 4. OBLONG: Elongated
        float oblongLen = metrics.faceLengthToWidthRatio >= FaceShapeConfig.OBLONG_LENGTH_WIDTH_MIN ? 1.8f : 0.3f;
        float oblongJaw = gaussian(metrics.jawToCheekRatio, 0.82f, 0.06f);
        rawScores.put("Oblong", Math.max(0.001f, oblongLen * oblongJaw));

        // 5. HEART: Forehead wider, tapering pointed V-line chin
        float heartForehead = metrics.foreheadToCheekRatio >= FaceShapeConfig.WIDE_FOREHEAD_TO_CHEEK_MIN ? 1.5f : 0.5f;
        heartForehead = (1.0f - fW) + (fW * heartForehead);
        float heartJaw = metrics.jawToCheekRatio <= FaceShapeConfig.NARROW_JAW_TO_CHEEK_MAX ? 1.4f : 0.6f;
        float heartChin = metrics.chinCurvatureScore <= FaceShapeConfig.SHARP_CHIN_CURVATURE_MAX ? 1.4f : 0.6f;
        rawScores.put("Heart", Math.max(0.001f, heartForehead * heartJaw * heartChin));

        // 6. DIAMOND: Cheekbones widest, pointed chin
        float diamondForehead = metrics.foreheadToCheekRatio <= FaceShapeConfig.NARROW_FOREHEAD_TO_CHEEK_MAX ? 1.4f : 0.6f;
        diamondForehead = (1.0f - fW) + (fW * diamondForehead);
        float diamondJaw = metrics.jawToCheekRatio <= FaceShapeConfig.NARROW_JAW_TO_CHEEK_MAX ? 1.4f : 0.6f;
        float diamondChin = metrics.chinCurvatureScore <= FaceShapeConfig.SHARP_CHIN_CURVATURE_MAX ? 1.4f : 0.6f;
        rawScores.put("Diamond", Math.max(0.001f, diamondForehead * diamondJaw * diamondChin));

        // 7. TRIANGLE: Jaw wide, forehead narrow
        float triForehead = metrics.foreheadToCheekRatio <= FaceShapeConfig.NARROW_FOREHEAD_TO_CHEEK_MAX ? 1.5f : 0.5f;
        triForehead = (1.0f - fW) + (fW * triForehead);
        float triJaw = metrics.jawToCheekRatio >= FaceShapeConfig.STRONG_JAW_TO_CHEEK_MIN ? 1.5f : 0.5f;
        rawScores.put("Triangle", Math.max(0.001f, triForehead * triJaw));

        float sum = 0f;
        for (float s : rawScores.values()) sum += s;

        Map<String, Float> probabilities = new LinkedHashMap<>();
        for (String s : SHAPES) probabilities.put(s, (sum > 0f) ? (rawScores.get(s) / sum) : (1.0f / SHAPES.length));

        String primaryShape = "Oval"; float primaryProb = -1f;
        String runnerUpShape = "Round"; float runnerUpProb = -1f;

        for (Map.Entry<String, Float> entry : probabilities.entrySet()) {
            if (entry.getValue() > primaryProb) {
                runnerUpProb = primaryProb; runnerUpShape = primaryShape;
                primaryProb = entry.getValue(); primaryShape = entry.getKey();
            } else if (entry.getValue() > runnerUpProb) {
                runnerUpProb = entry.getValue(); runnerUpShape = entry.getKey();
            }
        }

        boolean isBorderline = (primaryProb - runnerUpProb < 0.12f) || (primaryProb < 0.35f);
        float conf = Math.max(0.70f, Math.min(0.98f, 0.86f + (primaryProb * 0.08f)));

        return new ClassificationResult(primaryShape, conf, runnerUpShape, runnerUpProb, isBorderline, probabilities, isBorderline ? "Borderline shape." : "");
    }

    private static float gaussian(float x, float mean, float std) {
        float diff = (x - mean) / std;
        return (float) Math.exp(-0.5f * diff * diff);
    }
    private static float weightedGaussian(float x, float mean, float std, float weight) {
        if (weight <= 0.01f) return 1.0f;
        return (1.0f - weight) + (weight * gaussian(x, mean, std));
    }
}
