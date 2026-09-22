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

    // Center Biometric Guide Reticle Paints
    private final RectF guideBoundaryRect = new RectF();
    private final Paint ovalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ovalGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bracketPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint laserPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint laserGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final android.graphics.Path clipOvalPath = new android.graphics.Path();
    private long animationStartTime = System.currentTimeMillis();

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
        dotPaint.setColor(Color.parseColor("#66BB6A")); // Soft Green

        dotHaloPaint.setStyle(Paint.Style.FILL);
        dotHaloPaint.setColor(Color.parseColor("#4566BB6A"));

        ovalPaint.setStyle(Paint.Style.STROKE);
        ovalPaint.setStrokeWidth(dpToPx(2.2f));
        ovalPaint.setColor(Color.parseColor("#70FFFFFF"));

        ovalGlowPaint.setStyle(Paint.Style.STROKE);
        ovalGlowPaint.setStrokeWidth(dpToPx(6.5f));
        ovalGlowPaint.setColor(Color.parseColor("#20FFFFFF"));

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(dpToPx(4.5f));
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setColor(Color.parseColor("#66BB6A"));

        bracketPaint.setStyle(Paint.Style.STROKE);
        bracketPaint.setStrokeWidth(dpToPx(2.5f));
        bracketPaint.setStrokeCap(Paint.Cap.ROUND);
        bracketPaint.setColor(Color.parseColor("#70FFFFFF"));

        laserPaint.setStyle(Paint.Style.STROKE);
        laserPaint.setStrokeWidth(dpToPx(2.2f));
        laserPaint.setColor(Color.parseColor("#E066BB6A"));

        laserGlowPaint.setStyle(Paint.Style.STROKE);
        laserGlowPaint.setStrokeWidth(dpToPx(8.0f));
        laserGlowPaint.setColor(Color.parseColor("#3566BB6A"));
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
        int ovalColor;
        int glowColor;

        switch (state) {
            case SUCCESS:
                color = Color.parseColor("#66BB6A"); // Green Success
                halo = Color.parseColor("#4566BB6A");
                ovalColor = Color.parseColor("#66BB6A");
                glowColor = Color.parseColor("#6066BB6A");
                break;
            case SCANNING:
            case ALIGNED:
                color = Color.parseColor("#66BB6A");
                halo = Color.parseColor("#4566BB6A");
                ovalColor = Color.parseColor("#66BB6A");
                glowColor = Color.parseColor("#3566BB6A");
                break;
            case TILTED:
                color = Color.parseColor("#D9A441"); // Amber Warning
                halo = Color.parseColor("#45D9A441");
                ovalColor = Color.parseColor("#D9A441");
                glowColor = Color.parseColor("#35D9A441");
                break;
            case MISALIGNED:
                color = Color.parseColor("#E57373"); // Red Error
                halo = Color.parseColor("#45E57373");
                ovalColor = Color.parseColor("#E57373");
                glowColor = Color.parseColor("#35E57373");
                break;
            case SEARCHING:
            default:
                color = Color.parseColor("#8066BB6A");
                halo = Color.parseColor("#2566BB6A");
                ovalColor = Color.parseColor("#70FFFFFF");
                glowColor = Color.parseColor("#20FFFFFF");
                break;
        }

        dotPaint.setColor(color);
        dotHaloPaint.setColor(halo);
        ovalPaint.setColor(ovalColor);
        ovalGlowPaint.setColor(glowColor);
        bracketPaint.setColor(ovalColor);
        progressPaint.setColor(color);
        laserPaint.setColor(color);
        laserGlowPaint.setColor(halo);
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
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculateGuideBoundary(w, h);
    }

    private void calculateGuideBoundary(int viewW, int viewH) {
        if (viewW <= 0 || viewH <= 0) return;
        float ovalH = viewH * 0.52f;
        float ovalW = ovalH * 0.76f;
        float centerX = viewW / 2.0f;
        float centerY = viewH / 2.0f; // Exactly centered

        guideBoundaryRect.set(
                centerX - (ovalW / 2.0f),
                centerY - (ovalH / 2.0f),
                centerX + (ovalW / 2.0f),
                centerY + (ovalH / 2.0f)
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (guideBoundaryRect.isEmpty() && getWidth() > 0 && getHeight() > 0) {
            calculateGuideBoundary(getWidth(), getHeight());
        }

        if (!guideBoundaryRect.isEmpty()) {
            // 1. Draw subtle guide oval with glow
            canvas.drawOval(guideBoundaryRect, ovalGlowPaint);
            canvas.drawOval(guideBoundaryRect, ovalPaint);

            // 2. Draw 4 subtle corner reticle brackets
            float bSize = dpToPx(20f);
            float pad = dpToPx(6f);
            float left = guideBoundaryRect.left - pad;
            float top = guideBoundaryRect.top - pad;
            float right = guideBoundaryRect.right + pad;
            float bottom = guideBoundaryRect.bottom + pad;

            // Top-Left corner bracket
            canvas.drawLine(left, top + bSize, left, top, bracketPaint);
            canvas.drawLine(left, top, left + bSize, top, bracketPaint);

            // Top-Right corner bracket
            canvas.drawLine(right - bSize, top, right, top, bracketPaint);
            canvas.drawLine(right, top, right, top + bSize, bracketPaint);

            // Bottom-Left corner bracket
            canvas.drawLine(left, bottom - bSize, left, bottom, bracketPaint);
            canvas.drawLine(left, bottom, left + bSize, bottom, bracketPaint);

            // Bottom-Right corner bracket
            canvas.drawLine(right - bSize, bottom, right, bottom, bracketPaint);
            canvas.drawLine(right, bottom, right, bottom - bSize, bracketPaint);

            // 3. Draw biometric circular progress arc clockwise around the oval from the top apex
            if (scanProgress > 0.005f) {
                canvas.drawArc(guideBoundaryRect, -90f, scanProgress * 360f, false, progressPaint);
            }

            // 4. Draw futuristic animated horizontal laser sweep beam while actively scanning
            boolean isActivelyScanning = (guideState == GuideState.ALIGNED || guideState == GuideState.SCANNING || guideState == GuideState.SUCCESS) && scanProgress > 0.05f;
            if (isActivelyScanning) {
                long elapsed = System.currentTimeMillis() - animationStartTime;
                float cycle = (float) ((Math.sin(elapsed * 0.003) + 1.0) / 2.0); // 0.0 to 1.0 oscillation
                float sweepY = guideBoundaryRect.top + (cycle * guideBoundaryRect.height());

                canvas.save();
                clipOvalPath.reset();
                clipOvalPath.addOval(guideBoundaryRect, android.graphics.Path.Direction.CW);
                canvas.clipPath(clipOvalPath);

                canvas.drawLine(guideBoundaryRect.left, sweepY, guideBoundaryRect.right, sweepY, laserGlowPaint);
                canvas.drawLine(guideBoundaryRect.left, sweepY, guideBoundaryRect.right, sweepY, laserPaint);
                canvas.restore();

                // Request continuous redraw while laser is active
                postInvalidateOnAnimation();
            }
        }

        // Draw face mesh dots tracking facial contours
        if (!dots.isEmpty()) {
            for (MeshDot dot : dots) {
                if (!dot.active) continue;
                canvas.drawCircle(dot.x, dot.y, haloRadius, dotHaloPaint);
                canvas.drawCircle(dot.x, dot.y, dotRadius, dotPaint);
            }
        }
    }

    public RectF getGuideOvalRect() {
        if (guideBoundaryRect.isEmpty() && getWidth() > 0 && getHeight() > 0) {
            calculateGuideBoundary(getWidth(), getHeight());
        }
        return guideBoundaryRect;
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
