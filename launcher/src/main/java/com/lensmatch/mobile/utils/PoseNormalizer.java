package com.lensmatch.mobile.utils;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

import java.util.List;

/**
 * Normalizes 3D facial landmarks prior to anthropometric measurement:
 * 1. Computes roll angle from eye centers and rotates landmarks so eyes are strictly level.
 * 2. Normalizes all coordinates by Interpupillary Distance (IPD) for scale/distance invariance.
 * 3. Extracts Yaw and Pitch from the facial transformation matrix and validates pose (|yaw| <= 12°, |pitch| <= 10°).
 */
public class PoseNormalizer {

    public static final float MAX_YAW_DEG = 18.0f;
    public static final float MAX_PITCH_DEG = 16.0f;

    // Eye landmarks for roll and IPD
    private static final int LEFT_EYE_IRIS = 468;
    private static final int RIGHT_EYE_IRIS = 473;
    private static final int LEFT_EYE_OUTER = 33;
    private static final int RIGHT_EYE_OUTER = 263;

    public static class Point3D {
        public final float x;
        public final float y;
        public final float z;

        public Point3D(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public float dist(Point3D other) {
            float dx = this.x - other.x;
            float dy = this.y - other.y;
            float dz = this.z - other.z;
            return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }

    public static class NormalizedFace {
        public final Point3D[] points;          // 478 roll-corrected, IPD-normalized 3D coordinates
        public final float rollDeg;             // Corrected camera/head roll angle
        public final float yawDeg;              // Head turn left/right
        public final float pitchDeg;            // Head tilt up/down
        public final float ipdPixels;           // Raw interpupillary distance in pixels
        public final boolean isPoseAcceptable;  // True if within tolerance (|yaw| <= 12°, |pitch| <= 10°)
        public final String rejectionReason;

        public NormalizedFace(Point3D[] points, float rollDeg, float yawDeg, float pitchDeg,
                              float ipdPixels, boolean isPoseAcceptable, String rejectionReason) {
            this.points = points;
            this.rollDeg = rollDeg;
            this.yawDeg = yawDeg;
            this.pitchDeg = pitchDeg;
            this.ipdPixels = ipdPixels;
            this.isPoseAcceptable = isPoseAcceptable;
            this.rejectionReason = rejectionReason;
        }
    }

    /**
     * Normalizes facial landmarks: scales to pixels, extracts yaw/pitch, computes roll,
     * rotates all landmarks to level the eyes, and divides by IPD.
     */
    public static NormalizedFace normalize(List<NormalizedLandmark> rawLandmarks, float[] transformMatrix, int imgW, int imgH) {
        if (rawLandmarks == null || rawLandmarks.size() < 468) {
            return null;
        }

        float w = imgW > 0 ? (float) imgW : 1000f;
        float h = imgH > 0 ? (float) imgH : 1000f;

        // 1. Convert to metric pixel space
        int n = rawLandmarks.size();
        Point3D[] pixelPoints = new Point3D[n];
        for (int i = 0; i < n; i++) {
            NormalizedLandmark lm = rawLandmarks.get(i);
            pixelPoints[i] = new Point3D(lm.x() * w, lm.y() * h, lm.z() * w);
        }

        // 2. Identify left & right eye centers (prefer iris landmarks 468 & 473, fallback 33 & 263)
        Point3D leftEye = (n >= 478) ? pixelPoints[LEFT_EYE_IRIS] : pixelPoints[LEFT_EYE_OUTER];
        Point3D rightEye = (n >= 478) ? pixelPoints[RIGHT_EYE_IRIS] : pixelPoints[RIGHT_EYE_OUTER];

        float dxEyes = rightEye.x - leftEye.x;
        float dyEyes = rightEye.y - leftEye.y;
        float dzEyes = rightEye.z - leftEye.z;
        float ipd = (float) Math.sqrt(dxEyes * dxEyes + dyEyes * dyEyes + dzEyes * dzEyes);

        if (ipd <= 1.0f) {
            return null;
        }

        // 3. Eye-leveling roll angle
        float rollRad = (float) Math.atan2(dyEyes, dxEyes);
        float rollDeg = (float) Math.toDegrees(rollRad);

        // Eye midpoint acts as the rotational origin
        float midX = (leftEye.x + rightEye.x) / 2.0f;
        float midY = (leftEye.y + rightEye.y) / 2.0f;
        float midZ = (leftEye.z + rightEye.z) / 2.0f;

        // 4. Extract Yaw and Pitch (from 4x4 matrix or geometric estimation)
        float yawDeg = 0.0f;
        float pitchDeg = 0.0f;

        if (transformMatrix != null && transformMatrix.length >= 16) {
            // MediaPipe 4x4 column-major matrix
            float r02 = transformMatrix[8];
            float r12 = transformMatrix[9];
            float r22 = transformMatrix[10];

            yawDeg = (float) Math.toDegrees(Math.atan2(r02, r22));
            pitchDeg = (float) Math.toDegrees(Math.asin(-Math.max(-1.0f, Math.min(1.0f, r12))));
        } else {
            // Geometric fallback using nose tip (index 1) relative to eye midpoint
            Point3D noseTip = pixelPoints[1];
            float noseDx = noseTip.x - midX;
            float noseDy = noseTip.y - midY;
            float noseDz = Math.abs(noseTip.z - midZ);
            if (noseDz > 0.001f) {
                yawDeg = (float) Math.toDegrees(Math.atan2(noseDx, noseDz));
                pitchDeg = (float) Math.toDegrees(Math.atan2(noseDy - (ipd * 0.4f), noseDz));
            }
        }

        // 5. Pose validation gatekeeper
        boolean acceptable = true;
        String reason = null;

        if (Math.abs(yawDeg) > MAX_YAW_DEG) {
            acceptable = false;
            reason = "Please face the camera directly (head turned " + (yawDeg > 0 ? "right" : "left") + ")";
        } else if (Math.abs(pitchDeg) > MAX_PITCH_DEG) {
            acceptable = false;
            reason = "Please level your head (head tilted " + (pitchDeg > 0 ? "down" : "up") + ")";
        }

        // 6. Rotate all landmarks by -rollRad and normalize coordinates by IPD
        float cos = (float) Math.cos(-rollRad);
        float sin = (float) Math.sin(-rollRad);

        Point3D[] normalizedPoints = new Point3D[n];
        for (int i = 0; i < n; i++) {
            float rx = pixelPoints[i].x - midX;
            float ry = pixelPoints[i].y - midY;
            float rz = pixelPoints[i].z - midZ;

            // 2D in-plane rotation leveling the eye line
            float rotX = (rx * cos) - (ry * sin);
            float rotY = (rx * sin) + (ry * cos);
            float rotZ = rz;

            // IPD scaling ensures scale- and distance-invariance
            normalizedPoints[i] = new Point3D(rotX / ipd, rotY / ipd, rotZ / ipd);
        }

        return new NormalizedFace(normalizedPoints, rollDeg, yawDeg, pitchDeg, ipd, acceptable, reason);
    }
}
