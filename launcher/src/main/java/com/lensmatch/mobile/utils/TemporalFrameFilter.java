package com.lensmatch.mobile.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Multi-frame temporal buffer and outlier filter using Median Absolute Deviation (MAD).
 * Collects 15-30 frames over ~1.5s, filters jitter and micro-movement, drops statistical
 * outliers (>2 standard deviations from median), and averages inliers for optimal stability.
 */
public class TemporalFrameFilter {
    public static final float MIN_LANDMARK_CONFIDENCE = 0.50f;
    public static final float MAD_OUTLIER_THRESHOLD = 2.0f; // Max std dev deviation from median

    public static FaceMetrics filterAndAverage(List<FaceMetrics> frameSamples) {
        if (frameSamples == null || frameSamples.isEmpty()) {
            return null;
        }

        // 1. Confidence filter
        List<FaceMetrics> confidentSamples = new ArrayList<>();
        for (FaceMetrics m : frameSamples) {
            if (m != null && m.jawConfidence >= MIN_LANDMARK_CONFIDENCE) {
                confidentSamples.add(m);
            }
        }

        if (confidentSamples.isEmpty()) {
            confidentSamples.addAll(frameSamples);
        }
        if (confidentSamples.size() <= 2) {
            return confidentSamples.get(0);
        }

        int n = confidentSamples.size();

        // 2. Extract series for MAD calculation
        float[] wToH = new float[n];
        float[] jawToCheek = new float[n];
        float[] foreToJaw = new float[n];
        float[] jawAngle = new float[n];
        float[] chinCurv = new float[n];

        for (int i = 0; i < n; i++) {
            FaceMetrics m = confidentSamples.get(i);
            wToH[i] = m.widthToHeightRatio;
            jawToCheek[i] = m.jawToCheekboneRatio;
            foreToJaw[i] = m.foreheadToJawRatio;
            jawAngle[i] = m.jawAngleScore;
            chinCurv[i] = m.chinCurvatureScore;
        }

        // 3. Compute Medians and Median Absolute Deviations (MAD)
        float medWToH = median(wToH);
        float madWToH = mad(wToH, medWToH);
        float stdWToH = Math.max(0.012f, 1.4826f * madWToH);

        float medJawCheek = median(jawToCheek);
        float madJawCheek = mad(jawToCheek, medJawCheek);
        float stdJawCheek = Math.max(0.012f, 1.4826f * madJawCheek);

        float medForeJaw = median(foreToJaw);
        float madForeJaw = mad(foreToJaw, medForeJaw);
        float stdForeJaw = Math.max(0.015f, 1.4826f * madForeJaw);

        float medAngle = median(jawAngle);
        float madAngle = mad(jawAngle, medAngle);
        float stdAngle = Math.max(2.0f, 1.4826f * madAngle);

        float medCurv = median(chinCurv);
        float madCurv = mad(chinCurv, medCurv);
        float stdCurv = Math.max(0.020f, 1.4826f * madCurv);

        // 4. Inlier selection: discard any frame where any metric deviates > 2 sigma from median
        List<FaceMetrics> inliers = new ArrayList<>();
        for (FaceMetrics m : confidentSamples) {
            boolean isOutlier = Math.abs(m.widthToHeightRatio - medWToH) > (MAD_OUTLIER_THRESHOLD * stdWToH)
                    || Math.abs(m.jawToCheekboneRatio - medJawCheek) > (MAD_OUTLIER_THRESHOLD * stdJawCheek)
                    || (!m.isForeheadOccluded && Math.abs(m.foreheadToJawRatio - medForeJaw) > (MAD_OUTLIER_THRESHOLD * stdForeJaw))
                    || Math.abs(m.jawAngleScore - medAngle) > (MAD_OUTLIER_THRESHOLD * stdAngle)
                    || Math.abs(m.chinCurvatureScore - medCurv) > (MAD_OUTLIER_THRESHOLD * stdCurv);

            if (!isOutlier) {
                inliers.add(m);
            }
        }

        if (inliers.isEmpty()) {
            inliers = confidentSamples; // Safe fallback
        }

        // 5. Average remaining inliers
        float sumWToH = 0, sumJawCheek = 0, sumForeJaw = 0, sumForeCheek = 0;
        float sumJawAngle = 0, sumChinCurv = 0;
        float sumForeConf = 0, sumJawConf = 0;
        float sumLenIpd = 0, sumCheekIpd = 0, sumForeIpd = 0, sumJawIpd = 0;
        int occludedCount = 0;

        for (FaceMetrics m : inliers) {
            sumWToH += m.widthToHeightRatio;
            sumJawCheek += m.jawToCheekboneRatio;
            sumForeJaw += m.foreheadToJawRatio;
            sumForeCheek += m.foreheadToCheekRatio;
            sumJawAngle += m.jawAngleScore;
            sumChinCurv += m.chinCurvatureScore;
            sumForeConf += m.foreheadConfidence;
            sumJawConf += m.jawConfidence;
            sumLenIpd += m.faceLengthIpd;
            sumCheekIpd += m.cheekWidthIpd;
            sumForeIpd += m.foreheadWidthIpd;
            sumJawIpd += m.jawWidthIpd;
            if (m.isForeheadOccluded) occludedCount++;
        }

        int count = inliers.size();
        return new FaceMetrics(
                sumWToH / count,
                sumJawCheek / count,
                sumForeJaw / count,
                sumForeCheek / count,
                sumJawAngle / count,
                sumChinCurv / count,
                sumForeConf / count,
                occludedCount > (count / 2),
                sumJawConf / count,
                sumLenIpd / count,
                sumCheekIpd / count,
                sumForeIpd / count,
                sumJawIpd / count
        );
    }

    private static float median(float[] arr) {
        float[] copy = Arrays.copyOf(arr, arr.length);
        Arrays.sort(copy);
        if (copy.length % 2 == 1) {
            return copy[copy.length / 2];
        } else {
            return (copy[(copy.length / 2) - 1] + copy[copy.length / 2]) / 2.0f;
        }
    }

    private static float mad(float[] arr, float medianVal) {
        float[] deviations = new float[arr.length];
        for (int i = 0; i < arr.length; i++) {
            deviations[i] = Math.abs(arr[i] - medianVal);
        }
        return median(deviations);
    }
}
