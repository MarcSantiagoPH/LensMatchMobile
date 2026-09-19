package com.lensmatch.mobile.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Multi-frame temporal buffer and outlier filter using Median Absolute Deviation (MAD).
 */
public class TemporalFrameFilter {
    public static final float MIN_LANDMARK_CONFIDENCE = 0.50f;
    public static final float MAD_OUTLIER_THRESHOLD = 2.0f; // Max std dev deviation from median

    public static FaceMetrics filterAndAverage(List<FaceMetrics> frameSamples) {
        if (frameSamples == null || frameSamples.isEmpty()) return null;

        List<FaceMetrics> confidentSamples = new ArrayList<>();
        for (FaceMetrics m : frameSamples) {
            if (m != null && m.jawConfidence >= MIN_LANDMARK_CONFIDENCE) confidentSamples.add(m);
        }

        if (confidentSamples.isEmpty()) confidentSamples.addAll(frameSamples);
        if (confidentSamples.size() <= 2) return confidentSamples.get(0);

        int n = confidentSamples.size();

        float[] lenW = new float[n];
        float[] foreCheek = new float[n];
        float[] jawCheek = new float[n];
        float[] chinJaw = new float[n];
        float[] jawAng = new float[n];
        float[] jawTap = new float[n];
        float[] chinCurv = new float[n];

        for (int i = 0; i < n; i++) {
            FaceMetrics m = confidentSamples.get(i);
            lenW[i] = m.faceLengthToWidthRatio;
            foreCheek[i] = m.foreheadToCheekRatio;
            jawCheek[i] = m.jawToCheekRatio;
            chinJaw[i] = m.chinToJawRatio;
            jawAng[i] = m.jawAngle;
            jawTap[i] = m.jawTaper;
            chinCurv[i] = m.chinCurvatureScore;
        }

        float medLenW = median(lenW); float stdLenW = Math.max(0.012f, 1.4826f * mad(lenW, medLenW));
        float medForeCheek = median(foreCheek); float stdForeCheek = Math.max(0.015f, 1.4826f * mad(foreCheek, medForeCheek));
        float medJawCheek = median(jawCheek); float stdJawCheek = Math.max(0.012f, 1.4826f * mad(jawCheek, medJawCheek));
        float medChinJaw = median(chinJaw); float stdChinJaw = Math.max(0.012f, 1.4826f * mad(chinJaw, medChinJaw));
        float medJawAng = median(jawAng); float stdJawAng = Math.max(2.0f, 1.4826f * mad(jawAng, medJawAng));
        float medJawTap = median(jawTap); float stdJawTap = Math.max(0.015f, 1.4826f * mad(jawTap, medJawTap));
        float medChinCurv = median(chinCurv); float stdChinCurv = Math.max(0.020f, 1.4826f * mad(chinCurv, medChinCurv));

        List<FaceMetrics> inliers = new ArrayList<>();
        for (FaceMetrics m : confidentSamples) {
            boolean isOutlier = Math.abs(m.faceLengthToWidthRatio - medLenW) > (MAD_OUTLIER_THRESHOLD * stdLenW)
                    || (!m.isForeheadOccluded && Math.abs(m.foreheadToCheekRatio - medForeCheek) > (MAD_OUTLIER_THRESHOLD * stdForeCheek))
                    || Math.abs(m.jawToCheekRatio - medJawCheek) > (MAD_OUTLIER_THRESHOLD * stdJawCheek)
                    || Math.abs(m.chinToJawRatio - medChinJaw) > (MAD_OUTLIER_THRESHOLD * stdChinJaw)
                    || Math.abs(m.jawAngle - medJawAng) > (MAD_OUTLIER_THRESHOLD * stdJawAng)
                    || Math.abs(m.jawTaper - medJawTap) > (MAD_OUTLIER_THRESHOLD * stdJawTap)
                    || Math.abs(m.chinCurvatureScore - medChinCurv) > (MAD_OUTLIER_THRESHOLD * stdChinCurv);
            if (!isOutlier) inliers.add(m);
        }

        if (inliers.isEmpty()) inliers = confidentSamples;

        float sumLenW = 0, sumForeCheek = 0, sumJawCheek = 0, sumChinJaw = 0;
        float sumJawAng = 0, sumJawTap = 0, sumChinCurv = 0;
        float sumYaw = 0, sumPitch = 0, sumRoll = 0;
        float sumForeConf = 0, sumJawConf = 0;
        int occludedCount = 0;

        for (FaceMetrics m : inliers) {
            sumLenW += m.faceLengthToWidthRatio;
            sumForeCheek += m.foreheadToCheekRatio;
            sumJawCheek += m.jawToCheekRatio;
            sumChinJaw += m.chinToJawRatio;
            sumJawAng += m.jawAngle;
            sumJawTap += m.jawTaper;
            sumChinCurv += m.chinCurvatureScore;
            sumYaw += m.yaw;
            sumPitch += m.pitch;
            sumRoll += m.roll;
            sumForeConf += m.foreheadConfidence;
            sumJawConf += m.jawConfidence;
            if (m.isForeheadOccluded) occludedCount++;
        }

        int count = inliers.size();
        return new FaceMetrics(
                sumLenW / count,
                sumForeCheek / count,
                sumJawCheek / count,
                sumChinJaw / count,
                sumJawAng / count,
                sumJawTap / count,
                sumChinCurv / count,
                sumYaw / count,
                sumPitch / count,
                sumRoll / count,
                sumForeConf / count,
                occludedCount > (count / 2),
                sumJawConf / count
        );
    }

    private static float median(float[] arr) {
        float[] copy = Arrays.copyOf(arr, arr.length);
        Arrays.sort(copy);
        if (copy.length % 2 == 1) return copy[copy.length / 2];
        else return (copy[(copy.length / 2) - 1] + copy[copy.length / 2]) / 2.0f;
    }

    private static float mad(float[] arr, float medianVal) {
        float[] deviations = new float[arr.length];
        for (int i = 0; i < arr.length; i++) deviations[i] = Math.abs(arr[i] - medianVal);
        return median(deviations);
    }
}
