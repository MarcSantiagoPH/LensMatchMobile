package com.lensmatch.mobile.utils;

/**
 * Configurable anthropometric boundary thresholds.
 * Centralized logic parameters for interpreting FaceMetrics ratios into Face Shapes.
 */
public class FaceShapeConfig {
    
    // Quality & Pose Thresholds
    public static float MIN_LANDMARK_CONFIDENCE = 0.50f;
    public static float MAX_VALID_YAW = 15.0f;
    public static float MAX_VALID_PITCH = 15.0f;
    public static float MAX_VALID_ROLL = 15.0f;

    // =========================================================================
    // Geometric Shape Rules (Ratio-based)
    // Note: Cheekbone width is 1.0 (the base normalization factor)
    // =========================================================================

    // General Length
    public static float ROUND_LENGTH_WIDTH_MAX = 1.25f; // Short/compact face
    public static float OVAL_LENGTH_WIDTH_MIN = 1.35f;  // Balanced
    public static float OVAL_LENGTH_WIDTH_MAX = 1.55f;
    public static float OBLONG_LENGTH_WIDTH_MIN = 1.58f; // Long face

    // Jaw Width & Taper
    public static float STRONG_JAW_TO_CHEEK_MIN = 0.82f; // Square / Triangle
    public static float NARROW_JAW_TO_CHEEK_MAX = 0.72f; // Heart / Diamond
    
    // Forehead Width
    public static float WIDE_FOREHEAD_TO_CHEEK_MIN = 0.95f; // Heart
    public static float NARROW_FOREHEAD_TO_CHEEK_MAX = 0.85f; // Diamond / Triangle

    // Chin Shape
    public static float SHARP_CHIN_CURVATURE_MAX = 0.35f; // Heart / Diamond
    public static float BROAD_CHIN_CURVATURE_MIN = 0.65f; // Square

}
