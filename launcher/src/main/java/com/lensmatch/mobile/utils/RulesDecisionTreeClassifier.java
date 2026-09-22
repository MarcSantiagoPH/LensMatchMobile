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
        float fW = metrics.isForeheadOccluded ? 0.25f : 1.0f;
        float lenW = metrics.faceLengthToWidthRatio;
        float jawCheek = metrics.jawToCheekRatio;
        float foreCheek = metrics.foreheadToCheekRatio;
        float chinCurv = metrics.chinCurvatureScore;

        // 1. OVAL: Balanced length, gently tapering jaw, soft curved chin
        float ovalScore = geomMean(
                gaussian(lenW, 1.45f, 0.11f),
                gaussian(jawCheek, 0.76f, 0.06f),
                weightedGaussian(foreCheek, 0.84f, 0.07f, fW),
                gaussian(chinCurv, 0.45f, 0.14f)
        );
        rawScores.put("Oval", Math.max(0.001f, ovalScore));

        // 2. ROUND: Compact length, soft/tapered jaw, rounded circular chin (no sharp gonial angle)
        float roundJawPenalty = (jawCheek > 0.84f) ? 0.4f : 1.0f; // Wide jaw belongs to Square, not Round
        float roundScore = geomMean(
                gaussian(lenW, 1.18f, 0.09f),
                gaussian(jawCheek, 0.76f, 0.06f),
                gaussian(chinCurv, 0.58f, 0.13f),
                weightedGaussian(foreCheek, 0.86f, 0.08f, fW)
        ) * roundJawPenalty;
        rawScores.put("Round", Math.max(0.001f, roundScore));

        // 3. SQUARE: Compact length, strong broad jawbone, flat/angular chin
        float sqJawBonus = (jawCheek >= 0.82f) ? 1.0f : 0.3f;
        float sqChinBonus = (chinCurv >= 0.55f) ? 1.0f : 0.3f;
        float squareScore = geomMean(
                gaussian(lenW, 1.20f, 0.09f),
                gaussian(jawCheek, 0.88f, 0.06f),
                gaussian(chinCurv, 0.75f, 0.13f),
                weightedGaussian(foreCheek, 0.88f, 0.08f, fW)
        ) * sqJawBonus * sqChinBonus;
        rawScores.put("Square", Math.max(0.001f, squareScore));

        // 4. OBLONG: Elongated vertical face, straight sides, balanced jaw
        float oblongLenBonus = (lenW >= 1.50f) ? 1.0f : 0.3f;
        float oblongScore = geomMean(
                gaussian(lenW, 1.62f, 0.10f),
                gaussian(jawCheek, 0.80f, 0.07f),
                gaussian(chinCurv, 0.50f, 0.15f),
                weightedGaussian(foreCheek, 0.84f, 0.08f, fW)
        ) * oblongLenBonus;
        rawScores.put("Oblong", Math.max(0.001f, oblongScore));

        // 5. HEART: Forehead noticeably widest, slender tapering jaw, sharp pointed V-line chin
        float heartScore = geomMean(
                weightedGaussian(foreCheek, 0.98f, 0.08f, fW),
                gaussian(jawCheek, 0.67f, 0.06f),
                gaussian(chinCurv, 0.25f, 0.10f),
                gaussian(lenW, 1.38f, 0.10f)
        );
        rawScores.put("Heart", Math.max(0.001f, heartScore));

        // 6. DIAMOND: Cheekbones widest point, narrow forehead, narrow jaw, pointed V-line chin
        float diamondScore = geomMean(
                weightedGaussian(foreCheek, 0.75f, 0.07f, fW),
                gaussian(jawCheek, 0.67f, 0.06f),
                gaussian(chinCurv, 0.25f, 0.10f),
                gaussian(lenW, 1.42f, 0.10f)
        );
        rawScores.put("Diamond", Math.max(0.001f, diamondScore));

        // 7. TRIANGLE: Jaw is widest point, narrow forehead, broad/square chin base
        float triJawBonus = (jawCheek >= 0.84f) ? 1.0f : 0.4f;
        float triangleScore = geomMean(
                gaussian(jawCheek, 0.90f, 0.06f),
                weightedGaussian(foreCheek, 0.74f, 0.06f, fW),
                gaussian(chinCurv, 0.68f, 0.14f),
                gaussian(lenW, 1.32f, 0.10f)
        ) * triJawBonus;
        rawScores.put("Triangle", Math.max(0.001f, triangleScore));

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

    private static float geomMean(float... values) {
        if (values == null || values.length == 0) return 0f;
        double prod = 1.0;
        for (float v : values) {
            prod *= Math.max(0.0001, (double) v);
        }
        return (float) Math.pow(prod, 1.0 / values.length);
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
