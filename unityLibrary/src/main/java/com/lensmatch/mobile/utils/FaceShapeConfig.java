package com.lensmatch.mobile.utils;

/**
 * Configurable anthropometric boundary thresholds tailored for
 * Filipino / Southeast Asian (SEA) facial morphology.
 *
 * Separated from business logic so thresholds can be updated via
 * local calibration datasets or external JSON profiles without refactoring.
 */
public class FaceShapeConfig {

    public static final String POPULATION_PROFILE = "Southeast Asian / Filipino (SEA-2026)";

    // Minimum landmark confidence before flagging occlusion
    public static float MIN_LANDMARK_CONFIDENCE = 0.50f;

    // Weight of forehead_to_jaw_ratio when unoccluded vs occluded by bangs/hair
    public static float FOREHEAD_WEIGHT_DEFAULT = 1.0f;
    public static float FOREHEAD_WEIGHT_OCCLUDED = 0.25f;

    // =========================================================================
    // Population-Calibrated Thresholds (SEA / Filipino cranial proportions)
    // Note: SEA populations exhibit higher bizygomatic breadth (wider cheekbones)
    // and compact cranial height compared to Western averages.
    // =========================================================================

    // 1. OVAL
    public static float OVAL_WIDTH_TO_HEIGHT_MIN = 0.68f;
    public static float OVAL_WIDTH_TO_HEIGHT_MAX = 0.78f;
    public static float OVAL_JAW_TO_CHEEK_MIN    = 0.72f;
    public static float OVAL_JAW_TO_CHEEK_MAX    = 0.83f;
    public static float OVAL_FOREHEAD_TO_JAW_MIN = 1.08f;
    public static float OVAL_FOREHEAD_TO_JAW_MAX = 1.25f;

    // 2. ROUND
    public static float ROUND_WIDTH_TO_HEIGHT_MIN = 0.79f; // Compact face
    public static float ROUND_JAW_TO_CHEEK_MAX    = 0.83f; // Tapered/curved, not boxy
    public static float ROUND_CHIN_CURVATURE_MIN  = 0.48f; // Soft, rounded chin

    // 3. SQUARE
    public static float SQUARE_WIDTH_TO_HEIGHT_MIN = 0.78f;
    public static float SQUARE_JAW_TO_CHEEK_MIN    = 0.84f; // Strong, wide mandible
    public static float SQUARE_CHIN_CURVATURE_MIN  = 0.60f; // Broad, blunt chin base

    // 4. OBLONG
    public static float OBLONG_WIDTH_TO_HEIGHT_MAX = 0.67f; // Long, slender face
    public static float OBLONG_JAW_TO_CHEEK_MIN    = 0.78f; // Straight, parallel contours

    // 5. HEART (Inverted Triangle)
    public static float HEART_FOREHEAD_TO_JAW_MIN = 1.22f; // Forehead significantly wider than jaw
    public static float HEART_JAW_TO_CHEEK_MAX    = 0.77f;
    public static float HEART_CHIN_CURVATURE_MAX  = 0.38f; // Tapering, pointed V-line chin

    // 6. DIAMOND
    public static float DIAMOND_FOREHEAD_TO_CHEEK_MAX = 0.79f; // Forehead narrow relative to cheekbones
    public static float DIAMOND_JAW_TO_CHEEK_MAX      = 0.75f; // Jaw narrow relative to cheekbones
    public static float DIAMOND_CHIN_CURVATURE_MAX    = 0.38f; // Pointed chin

    // 7. TRIANGLE (Pear Shape)
    public static float TRIANGLE_FOREHEAD_TO_JAW_MAX = 0.96f; // Jaw wider than forehead
    public static float TRIANGLE_JAW_TO_CHEEK_MIN    = 0.84f; // Broad jawline
}
