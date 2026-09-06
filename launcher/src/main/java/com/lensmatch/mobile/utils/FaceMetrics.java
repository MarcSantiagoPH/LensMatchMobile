package com.lensmatch.mobile.utils;

/**
 * Immutable container representing the 5 key normalized anthropometric ratios
 * plus occlusion confidence and scale-invariant IPD measurements.
 */
public class FaceMetrics {
    public final float widthToHeightRatio;     // Cheekbone width / Face length
    public final float jawToCheekboneRatio;    // Jaw width / Cheekbone width
    public final float foreheadToJawRatio;     // Forehead width / Jaw width
    public final float foreheadToCheekRatio;   // Forehead width / Cheekbone width
    public final float jawAngleScore;          // Jaw angle in degrees at chin apex
    public final float chinCurvatureScore;     // [0.0 = sharp V-line, 1.0 = broad/square]

    // Occlusion metadata
    public final float foreheadConfidence;
    public final boolean isForeheadOccluded;
    public final float jawConfidence;

    // IPD-normalized dimensions
    public final float faceLengthIpd;
    public final float cheekWidthIpd;
    public final float foreheadWidthIpd;
    public final float jawWidthIpd;

    public FaceMetrics(float widthToHeightRatio, float jawToCheekboneRatio,
                       float foreheadToJawRatio, float foreheadToCheekRatio,
                       float jawAngleScore, float chinCurvatureScore,
                       float foreheadConfidence, boolean isForeheadOccluded, float jawConfidence,
                       float faceLengthIpd, float cheekWidthIpd,
                       float foreheadWidthIpd, float jawWidthIpd) {
        this.widthToHeightRatio = widthToHeightRatio;
        this.jawToCheekboneRatio = jawToCheekboneRatio;
        this.foreheadToJawRatio = foreheadToJawRatio;
        this.foreheadToCheekRatio = foreheadToCheekRatio;
        this.jawAngleScore = jawAngleScore;
        this.chinCurvatureScore = chinCurvatureScore;
        this.foreheadConfidence = foreheadConfidence;
        this.isForeheadOccluded = isForeheadOccluded;
        this.jawConfidence = jawConfidence;
        this.faceLengthIpd = faceLengthIpd;
        this.cheekWidthIpd = cheekWidthIpd;
        this.foreheadWidthIpd = foreheadWidthIpd;
        this.jawWidthIpd = jawWidthIpd;
    }
}
