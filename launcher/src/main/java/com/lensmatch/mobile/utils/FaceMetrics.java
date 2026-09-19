package com.lensmatch.mobile.utils;

/**
 * Immutable container representing normalized anthropometric ratios
 * and pose measurements derived from MediaPipe landmarks.
 */
public class FaceMetrics {
    // Relative ratios (Normalized primarily against cheekbone width)
    public final float faceLengthToWidthRatio;   // Face length / Cheekbone width
    public final float foreheadToCheekRatio;     // Forehead width / Cheekbone width
    public final float jawToCheekRatio;          // Jaw width / Cheekbone width
    public final float chinToJawRatio;           // Chin width / Jaw width
    
    // Geometric shape angles and scores
    public final float jawAngle;                 // Gonial angle estimate
    public final float jawTaper;                 // Rate of narrowing from jaw to chin
    public final float chinCurvatureScore;       // [0.0 = sharp V-line, 1.0 = broad/square]

    // 3D Head Pose
    public final float yaw;
    public final float pitch;
    public final float roll;

    // Occlusion & Confidence
    public final float foreheadConfidence;
    public final boolean isForeheadOccluded;
    public final float jawConfidence;

    public FaceMetrics(float faceLengthToWidthRatio, float foreheadToCheekRatio,
                       float jawToCheekRatio, float chinToJawRatio,
                       float jawAngle, float jawTaper, float chinCurvatureScore,
                       float yaw, float pitch, float roll,
                       float foreheadConfidence, boolean isForeheadOccluded, float jawConfidence) {
        this.faceLengthToWidthRatio = faceLengthToWidthRatio;
        this.foreheadToCheekRatio = foreheadToCheekRatio;
        this.jawToCheekRatio = jawToCheekRatio;
        this.chinToJawRatio = chinToJawRatio;
        this.jawAngle = jawAngle;
        this.jawTaper = jawTaper;
        this.chinCurvatureScore = chinCurvatureScore;
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
        this.foreheadConfidence = foreheadConfidence;
        this.isForeheadOccluded = isForeheadOccluded;
        this.jawConfidence = jawConfidence;
    }
}
