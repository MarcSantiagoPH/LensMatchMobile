package com.lensmatch.mobile.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;

import java.util.ArrayList;
import java.util.List;

/**
 * Biometric Face Mesh Overlay View matching the reference design.
 * Features:
 * 1. Clean, 100% unobstructed full-screen camera view (no dark vignette).
 * 2. Dense green face mesh dots accurately tracking the face in real-time
 *    (jawline contour, forehead rows, cheek grid, eyebrows, eyes, nose, lips).
 * 3. Exact pixel-for-pixel coordinate alignment using CameraX CoordinateTransform matrix.
 * 4. Fluid, jitter-free movement following user face placement and tilt.
 */
public class FaceMeshOverlayView extends View {

    public enum GuideState {
        SEARCHING,
        MISALIGNED,
        TILTED,
        ALIGNED,
        SCANNING,
        SUCCESS
    }

    private List<Face> faces;
    private GuideState guideState = GuideState.SEARCHING;
    private float scanProgress = 0f;
    private int imageWidth = 1;
    private int imageHeight = 1;
    private Matrix transformMatrix = null;

    // Dot Paints
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float dotRadius;
    private float haloRadius;

    // Mesh Dot structure
    public static class MeshDot {
        public float x, y;
        public float targetX, targetY;
        public boolean active = false;
    }

    private final List<MeshDot> dots = new ArrayList<>();
    private final float[] pointBuffer = new float[2];

    public FaceMeshOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        dotRadius = dpToPx(3.2f);
        haloRadius = dpToPx(5.5f);

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(Color.parseColor("#00E676")); // Vibrant neon green

        dotHaloPaint.setStyle(Paint.Style.FILL);
        dotHaloPaint.setColor(Color.parseColor("#4500E676"));
    }

    public void updateState(List<Face> faces, GuideState state, int imgW, int imgH) {
        updateState(faces, state, this.scanProgress, null, imgW, imgH);
    }

    public void updateState(List<Face> faces, GuideState state, float progress, int imgW, int imgH) {
        updateState(faces, state, progress, null, imgW, imgH);
    }

    public void updateState(List<Face> faces, GuideState state, float progress, Matrix matrix, int imgW, int imgH) {
        this.faces = faces;
        this.guideState = state;
        this.scanProgress = Math.max(0f, Math.min(1f, progress));
        this.imageWidth = imgW;
        this.imageHeight = imgH;
        this.transformMatrix = matrix;

        applyColorsForState(state);

        if (faces != null && !faces.isEmpty()) {
            updateMeshPoints(faces.get(0), matrix, imgW, imgH);
        } else {
            for (MeshDot dot : dots) {
                dot.active = false;
            }
        }
        invalidate();
    }

    public void setProgress(float progress) {
        this.scanProgress = Math.max(0f, Math.min(1f, progress));
        invalidate();
    }

    public float getProgress() {
        return scanProgress;
    }

    public void updateFaces(List<Face> faces, boolean isAligned, int imageWidth, int imageHeight) {
        GuideState state;
        if (faces == null || faces.isEmpty()) {
            state = GuideState.SEARCHING;
        } else if (isAligned) {
            state = GuideState.ALIGNED;
        } else {
            state = GuideState.MISALIGNED;
        }
        updateState(faces, state, imageWidth, imageHeight);
    }

    private void applyColorsForState(GuideState state) {
        int color;
        int halo;

        switch (state) {
            case SUCCESS:
            case SCANNING:
            case ALIGNED:
                color = Color.parseColor("#00E676"); // Vibrant Green
                halo = Color.parseColor("#4500E676");
                break;
            case TILTED:
                color = Color.parseColor("#FFB300"); // Amber
                halo = Color.parseColor("#45FFB300");
                break;
            case MISALIGNED:
                color = Color.parseColor("#FF5252"); // Red
                halo = Color.parseColor("#45FF5252");
                break;
            case SEARCHING:
            default:
                color = Color.parseColor("#8000E676");
                halo = Color.parseColor("#2500E676");
                break;
        }

        dotPaint.setColor(color);
        dotHaloPaint.setColor(halo);
    }

    /**
     * Builds a comprehensive face mesh consisting of:
     * - All standard ML Kit face contours (face perimeter, eyes, eyebrows, nose, mouth)
     * - Interpolated curved rows across the forehead
     * - Interpolated radial grid lines across both cheeks
     * - Interpolated chin mesh points
     * Uses CameraX CoordinateTransform matrix for 100% pixel-perfect alignment.
     */
    private static final int[] ORDERED_CONTOURS = {
        FaceContour.FACE,
        FaceContour.LEFT_EYEBROW_TOP,
        FaceContour.LEFT_EYEBROW_BOTTOM,
        FaceContour.RIGHT_EYEBROW_TOP,
        FaceContour.RIGHT_EYEBROW_BOTTOM,
        FaceContour.LEFT_EYE,
        FaceContour.RIGHT_EYE,
        FaceContour.NOSE_BRIDGE,
        FaceContour.NOSE_BOTTOM,
        FaceContour.UPPER_LIP_TOP,
        FaceContour.UPPER_LIP_BOTTOM,
        FaceContour.LOWER_LIP_TOP,
        FaceContour.LOWER_LIP_BOTTOM,
        FaceContour.LEFT_CHEEK,
        FaceContour.RIGHT_CHEEK
    };

    /**
     * Builds a comprehensive biometric face mesh consisting of:
     * - All standard ML Kit face contours in fixed deterministic order
     * - Concentric curved rows across the forehead dome
     * - Cheek mesh points anchored to cheek centers and jawline
     * - Chin mesh points anchored to lower lip and chin apex
     * Uses CameraX CoordinateTransform matrix for pixel-perfect alignment.
     */
    private void updateMeshPoints(Face face, Matrix matrix, int imgW, int imgH) {
        if (face == null || imgW <= 0 || imgH <= 0 || getWidth() <= 0 || getHeight() <= 0) {
            for (MeshDot dot : dots) {
                dot.active = false;
            }
            return;
        }

        int dotIdx = 0;

        // 1. All contour points from ML Kit in stable deterministic order
        for (int type : ORDERED_CONTOURS) {
            FaceContour contour = face.getContour(type);
            if (contour == null || contour.getPoints() == null) continue;
            for (PointF pt : contour.getPoints()) {
                mapPoint(pt.x, pt.y, pointBuffer, matrix, imgW, imgH);
                addOrUpdateDot(dotIdx++, pointBuffer[0], pointBuffer[1]);
            }
        }

        // 2. Forehead Interior Mesh (concentric curved rows following brows to hairline)
        FaceContour faceContour = face.getContour(FaceContour.FACE);
        FaceContour leftBrow = face.getContour(FaceContour.LEFT_EYEBROW_TOP);
        FaceContour rightBrow = face.getContour(FaceContour.RIGHT_EYEBROW_TOP);

        if (faceContour != null && faceContour.getPoints() != null && faceContour.getPoints().size() >= 36) {
            List<PointF> facePts = faceContour.getPoints();

            // Right brow to upper right forehead
            if (rightBrow != null && rightBrow.getPoints() != null) {
                List<PointF> rBrowPts = rightBrow.getPoints();
                int rCount = Math.min(rBrowPts.size(), 5);
                for (int i = 0; i < rCount; i++) {
                    PointF b = rBrowPts.get(i);
                    PointF f = facePts.get(31 + i);
                    for (int row = 1; row <= 2; row++) {
                        float t = row / 3.0f;
                        float ix = b.x + t * (f.x - b.x);
                        float iy = b.y + t * (f.y - b.y);
                        mapPoint(ix, iy, pointBuffer, matrix, imgW, imgH);
                        addOrUpdateDot(dotIdx++, pointBuffer[0], pointBuffer[1]);
                    }
                }
            }

            // Left brow to upper left forehead
            if (leftBrow != null && leftBrow.getPoints() != null) {
                List<PointF> lBrowPts = leftBrow.getPoints();
                int lCount = Math.min(lBrowPts.size(), 5);
                for (int i = 0; i < lCount; i++) {
                    PointF b = lBrowPts.get(i);
                    PointF f = facePts.get(1 + i);
                    for (int row = 1; row <= 2; row++) {
                        float t = row / 3.0f;
                        float ix = b.x + t * (f.x - b.x);
                        float iy = b.y + t * (f.y - b.y);
                        mapPoint(ix, iy, pointBuffer, matrix, imgW, imgH);
                        addOrUpdateDot(dotIdx++, pointBuffer[0], pointBuffer[1]);
                    }
                }
            }

            // 3. Cheeks Interior Mesh
            FaceContour leftCheek = face.getContour(FaceContour.LEFT_CHEEK);
            FaceContour rightCheek = face.getContour(FaceContour.RIGHT_CHEEK);

            if (leftCheek != null && leftCheek.getPoints() != null && !leftCheek.getPoints().isEmpty()) {
                PointF lc = leftCheek.getPoints().get(0);
                for (int jIdx : new int[]{9, 11, 13}) {
                    PointF jPt = facePts.get(jIdx);
                    float ix = lc.x + 0.45f * (jPt.x - lc.x);
                    float iy = lc.y + 0.45f * (jPt.y - lc.y);
                    mapPoint(ix, iy, pointBuffer, matrix, imgW, imgH);
                    addOrUpdateDot(dotIdx++, pointBuffer[0], pointBuffer[1]);
                }
            }

            if (rightCheek != null && rightCheek.getPoints() != null && !rightCheek.getPoints().isEmpty()) {
                PointF rc = rightCheek.getPoints().get(0);
                for (int jIdx : new int[]{23, 25, 27}) {
                    PointF jPt = facePts.get(jIdx);
                    float ix = rc.x + 0.45f * (jPt.x - rc.x);
                    float iy = rc.y + 0.45f * (jPt.y - rc.y);
                    mapPoint(ix, iy, pointBuffer, matrix, imgW, imgH);
                    addOrUpdateDot(dotIdx++, pointBuffer[0], pointBuffer[1]);
                }
            }

            // 4. Chin Interior Mesh
            FaceContour lowerLipBottom = face.getContour(FaceContour.LOWER_LIP_BOTTOM);
            if (lowerLipBottom != null && lowerLipBottom.getPoints() != null && !lowerLipBottom.getPoints().isEmpty()) {
                PointF midLip = lowerLipBottom.getPoints().get(lowerLipBottom.getPoints().size() / 2);
                PointF chinApex = facePts.get(18);
                PointF jawLeft = facePts.get(16);
                PointF jawRight = facePts.get(20);

                for (float t : new float[]{0.35f, 0.70f}) {
                    float cx = midLip.x + t * (chinApex.x - midLip.x);
                    float cy = midLip.y + t * (chinApex.y - midLip.y);
                    mapPoint(cx, cy, pointBuffer, matrix, imgW, imgH);
                    addOrUpdateDot(dotIdx++, pointBuffer[0], pointBuffer[1]);

                    float xOffset = (jawRight.x - jawLeft.x) * 0.22f;
                    mapPoint(cx - xOffset, cy, pointBuffer, matrix, imgW, imgH);
                    addOrUpdateDot(dotIdx++, pointBuffer[0], pointBuffer[1]);
                    mapPoint(cx + xOffset, cy, pointBuffer, matrix, imgW, imgH);
                    addOrUpdateDot(dotIdx++, pointBuffer[0], pointBuffer[1]);
                }
            }
        }

        // Deactivate unused dots
        for (int i = dotIdx; i < dots.size(); i++) {
            dots.get(i).active = false;
        }
    }

    private void addOrUpdateDot(int index, float targetX, float targetY) {
        while (dots.size() <= index) {
            dots.add(new MeshDot());
        }

        MeshDot dot = dots.get(index);
        dot.targetX = targetX;
        dot.targetY = targetY;
        dot.x = targetX;
        dot.y = targetY;
        dot.active = true;
    }

    /**
     * Maps an ML Kit point to screen coordinates using CameraX transform matrix,
     * with fallback to calibrated Google GraphicOverlay ratio scaling.
     */
    private void mapPoint(float inX, float inY, float[] out, Matrix matrix, int imgW, int imgH) {
        if (matrix != null) {
            out[0] = inX;
            out[1] = inY;
            matrix.mapPoints(out);
        } else {
            out[0] = mapX(inX, imgW, imgH, getWidth(), getHeight());
            out[1] = mapY(inY, imgW, imgH, getWidth(), getHeight());
        }
    }

    private float mapX(float x, int imgW, int imgH, int viewW, int viewH) {
        if (imgW <= 0 || imgH <= 0 || viewW <= 0 || viewH <= 0) return x;
        float viewAspectRatio = (float) viewW / viewH;
        float imageAspectRatio = (float) imgW / imgH;
        float scaleFactor;
        float postScaleWidthOffset = 0f;

        if (viewAspectRatio > imageAspectRatio) {
            scaleFactor = (float) viewW / imgW;
        } else {
            scaleFactor = (float) viewH / imgH;
            postScaleWidthOffset = ((float) viewH * imageAspectRatio - viewW) / 2f;
        }
        // Mirror horizontally for front camera
        return viewW - (x * scaleFactor - postScaleWidthOffset);
    }

    private float mapY(float y, int imgW, int imgH, int viewW, int viewH) {
        if (imgW <= 0 || imgH <= 0 || viewW <= 0 || viewH <= 0) return y;
        float viewAspectRatio = (float) viewW / viewH;
        float imageAspectRatio = (float) imgW / imgH;
        float scaleFactor;
        float postScaleHeightOffset = 0f;

        if (viewAspectRatio > imageAspectRatio) {
            scaleFactor = (float) viewW / imgW;
            postScaleHeightOffset = ((float) viewW / imageAspectRatio - viewH) / 2f;
        } else {
            scaleFactor = (float) viewH / imgH;
        }
        return y * scaleFactor - postScaleHeightOffset;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (dots.isEmpty()) return;

        for (MeshDot dot : dots) {
            if (!dot.active) continue;
            // Draw crisp green face mesh dot with soft radiant aura
            canvas.drawCircle(dot.x, dot.y, haloRadius, dotHaloPaint);
            canvas.drawCircle(dot.x, dot.y, dotRadius, dotPaint);
        }
    }

    public RectF getGuideOvalRect() {
        float ovalH = getHeight() * 0.52f;
        float ovalW = ovalH * 0.72f;
        float cx = getWidth() / 2.0f;
        float cy = getHeight() * 0.44f;
        return new RectF(cx - ovalW / 2f, cy - ovalH / 2f, cx + ovalW / 2f, cy + ovalH / 2f);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
