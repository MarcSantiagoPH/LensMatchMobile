package com.lensmatch.mobile.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;

import java.util.List;

/**
 * Modern facial scanning overlay view.
 * Uses futuristic scanning dots instead of solid oval lines or rectangular boxes:
 * 1. A ring of discrete guidance dots indicating where the user should position their face.
 * 2. Active scanning dots marking the key facial biometric locations (forehead, cheekbones,
 *    jawline, chin, and facial perimeter contours).
 * 3. Reactive color states:
 *    - Vibrant Green (#00E676): Fully aligned, leveled, and ready to scan.
 *    - Warning Amber (#FFB300): Face detected but head is tilted/turned.
 *    - Alert Red (#FF5252): Face too far, too close, off-center, or bad lighting.
 *    - Translucent Cyan/White (#80FFFFFF): Searching for face.
 */
public class FaceMeshOverlayView extends View {

    public enum GuideState {
        SEARCHING,
        MISALIGNED, // Too close, too far, off-center, bad lighting
        TILTED,     // Pose yaw/pitch/roll out of tolerance
        ALIGNED     // Ready for scan
    }

    private List<Face> faces;
    private GuideState guideState = GuideState.SEARCHING;
    private int imageWidth = 1;
    private int imageHeight = 1;

    // Paints for guidance dots
    private final Paint guideDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guideDotGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Paints for active scanning measurement dots
    private final Paint scanPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scanPointGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scanHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF guideBoundaryRect = new RectF();
    private static final int NUM_GUIDE_DOTS = 28;

    public FaceMeshOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Guidance dots
        guideDotPaint.setStyle(Paint.Style.FILL);
        guideDotPaint.setColor(Color.parseColor("#90FFFFFF"));

        guideDotGlowPaint.setStyle(Paint.Style.FILL);
        guideDotGlowPaint.setColor(Color.parseColor("#25FFFFFF"));

        // Active scan dots
        scanPointPaint.setStyle(Paint.Style.FILL);
        scanPointPaint.setColor(Color.parseColor("#CCFFFFFF"));

        scanPointGlowPaint.setStyle(Paint.Style.FILL);
        scanPointGlowPaint.setColor(Color.parseColor("#35FFFFFF"));

        scanHighlightPaint.setStyle(Paint.Style.STROKE);
        scanHighlightPaint.setStrokeWidth(2f);
        scanHighlightPaint.setColor(Color.WHITE);
    }

    public void updateState(List<Face> faces, GuideState state, int imgW, int imgH) {
        this.faces = faces;
        this.guideState = state;
        this.imageWidth = imgW;
        this.imageHeight = imgH;

        int dotColor;
        int glowColor;
        int highlightColor;

        switch (state) {
            case ALIGNED:
                dotColor = Color.parseColor("#00E676"); // Vibrant Green
                glowColor = Color.parseColor("#4000E676");
                highlightColor = Color.parseColor("#B300E676");
                break;
            case TILTED:
                dotColor = Color.parseColor("#FFB300"); // Warning Amber
                glowColor = Color.parseColor("#40FFB300");
                highlightColor = Color.parseColor("#B3FFB300");
                break;
            case MISALIGNED:
                dotColor = Color.parseColor("#FF5252"); // Alert Red
                glowColor = Color.parseColor("#40FF5252");
                highlightColor = Color.parseColor("#B3FF5252");
                break;
            case SEARCHING:
            default:
                dotColor = Color.parseColor("#90FFFFFF");
                glowColor = Color.parseColor("#25FFFFFF");
                highlightColor = Color.parseColor("#80FFFFFF");
                break;
        }

        guideDotPaint.setColor(dotColor);
        guideDotGlowPaint.setColor(glowColor);

        scanPointPaint.setColor(dotColor);
        scanPointGlowPaint.setColor(glowColor);
        scanHighlightPaint.setColor(highlightColor);

        invalidate();
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

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculateGuideBoundary(w, h);
    }

    private void calculateGuideBoundary(int viewW, int viewH) {
        float ovalH = viewH * 0.50f;
        float ovalW = ovalH * 0.72f;
        float centerX = viewW / 2.0f;
        float centerY = viewH * 0.44f;

        guideBoundaryRect.set(
                centerX - (ovalW / 2.0f),
                centerY - (ovalH / 2.0f),
                centerX + (ovalW / 2.0f),
                centerY + (ovalH / 2.0f)
        );
    }

    public RectF getGuideOvalRect() {
        return guideBoundaryRect;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (guideBoundaryRect.isEmpty()) {
            calculateGuideBoundary(getWidth(), getHeight());
        }

        // 1. Draw Guidance Reticle Dots (Dots indicating ideal facial framing)
        float cx = guideBoundaryRect.centerX();
        float cy = guideBoundaryRect.centerY();
        float rx = guideBoundaryRect.width() / 2.0f;
        float ry = guideBoundaryRect.height() / 2.0f;

        for (int i = 0; i < NUM_GUIDE_DOTS; i++) {
            double angle = (2.0 * Math.PI * i) / NUM_GUIDE_DOTS;
            float dx = cx + (float) (rx * Math.cos(angle));
            float dy = cy + (float) (ry * Math.sin(angle));

            boolean isCardinal = (i % (NUM_GUIDE_DOTS / 4) == 0);
            float coreRadius = isCardinal ? 6f : 3.5f;
            float glowRadius = isCardinal ? 14f : 8f;

            canvas.drawCircle(dx, dy, glowRadius, guideDotGlowPaint);
            canvas.drawCircle(dx, dy, coreRadius, guideDotPaint);
        }

        // 2. Draw Active Facial Biometric Scan Dots
        if (faces != null && !faces.isEmpty()) {
            float scaleX = (float) getWidth() / (float) imageHeight; // Mirror rotation compensation
            float scaleY = (float) getHeight() / (float) imageWidth;

            for (Face face : faces) {
                // A. Draw fine contour scan dots along the facial boundary
                FaceContour faceContour = face.getContour(FaceContour.FACE);
                if (faceContour != null && faceContour.getPoints() != null) {
                    List<PointF> pts = faceContour.getPoints();
                    for (int i = 0; i < pts.size(); i++) {
                        PointF pt = pts.get(i);
                        float sx = getWidth() - (pt.x * scaleX);
                        float sy = pt.y * scaleY;

                        // Draw scanning dot
                        canvas.drawCircle(sx, sy, 7f, scanPointGlowPaint);
                        canvas.drawCircle(sx, sy, 3.5f, scanPointPaint);
                    }
                }

                // Also draw eye contour dots
                drawContourDots(canvas, face.getContour(FaceContour.LEFT_EYE), scaleX, scaleY);
                drawContourDots(canvas, face.getContour(FaceContour.RIGHT_EYE), scaleX, scaleY);
                drawContourDots(canvas, face.getContour(FaceContour.NOSE_BRIDGE), scaleX, scaleY);

                // B. Draw prominent Anthropometric Measurement Target Dots
                Rect bounds = face.getBoundingBox();
                float bLeft = getWidth() - (bounds.right * scaleX);
                float bRight = getWidth() - (bounds.left * scaleX);
                float bTop = bounds.top * scaleY;
                float bBottom = bounds.bottom * scaleY;
                float bCenterX = (bLeft + bRight) / 2.0f;
                float bW = bRight - bLeft;
                float bH = bBottom - bTop;

                // 1. Forehead width scan points (Trichion & bifrontal points)
                drawScanTargetDot(canvas, bCenterX, bTop + (bH * 0.06f));
                drawScanTargetDot(canvas, bLeft + (bW * 0.16f), bTop + (bH * 0.20f));
                drawScanTargetDot(canvas, bRight - (bW * 0.16f), bTop + (bH * 0.20f));

                // 2. Cheekbone width scan points (Zygomatic prominence)
                drawScanTargetDot(canvas, bLeft + (bW * 0.04f), bTop + (bH * 0.48f));
                drawScanTargetDot(canvas, bRight - (bW * 0.04f), bTop + (bH * 0.48f));

                // 3. Jawline angle scan points (Gonial angles)
                drawScanTargetDot(canvas, bLeft + (bW * 0.14f), bTop + (bH * 0.76f));
                drawScanTargetDot(canvas, bRight - (bW * 0.14f), bTop + (bH * 0.76f));

                // 4. Chin apex and curvature scan points
                drawScanTargetDot(canvas, bCenterX, bBottom - (bH * 0.03f));
                drawScanTargetDot(canvas, bCenterX - (bW * 0.09f), bBottom - (bH * 0.06f));
                drawScanTargetDot(canvas, bCenterX + (bW * 0.09f), bBottom - (bH * 0.06f));

                // 5. Eye / IPD center reference scan points
                drawScanTargetDot(canvas, bCenterX - (bW * 0.20f), bTop + (bH * 0.38f));
                drawScanTargetDot(canvas, bCenterX + (bW * 0.20f), bTop + (bH * 0.38f));
            }
        }
    }

    private void drawContourDots(Canvas canvas, FaceContour contour, float scaleX, float scaleY) {
        if (contour == null || contour.getPoints() == null) return;
        List<PointF> pts = contour.getPoints();
        for (int i = 0; i < pts.size(); i += 2) { // Step by 2 for balanced density
            PointF pt = pts.get(i);
            float sx = getWidth() - (pt.x * scaleX);
            float sy = pt.y * scaleY;
            canvas.drawCircle(sx, sy, 5f, scanPointGlowPaint);
            canvas.drawCircle(sx, sy, 2.5f, scanPointPaint);
        }
    }

    private void drawScanTargetDot(Canvas canvas, float x, float y) {
        // Glowing concentric biometric scanning point
        canvas.drawCircle(x, y, 11f, scanPointGlowPaint);
        canvas.drawCircle(x, y, 6.5f, scanHighlightPaint);
        canvas.drawCircle(x, y, 3.5f, scanPointPaint);
    }
}
