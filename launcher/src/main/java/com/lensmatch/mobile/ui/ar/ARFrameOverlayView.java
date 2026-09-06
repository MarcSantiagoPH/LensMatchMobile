package com.lensmatch.mobile.ui.ar;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.List;

public class ARFrameOverlayView extends View {
    private List<Face> faces;
    private int imageWidth = 1;
    private int imageHeight = 1;

    private String selectedFrame = "Wayfarer";
    private int frameColor = Color.parseColor("#141414");

    // Cached face position for invalidate throttling (avoids redraw when face is still)
    private float mLastFaceCenterX = -1f;
    private float mLastFaceCenterY = -1f;
    private static final float REDRAW_THRESHOLD_PX = 4f;

    // Rim – thick stroke for the frame border
    private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // Rim highlight for 3-D effect
    private final Paint rimHighlight = new Paint(Paint.ANTI_ALIAS_FLAG);
    // Translucent tinted lens fill
    private final Paint lensFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    // Lens glare
    private final Paint glare = new Paint(Paint.ANTI_ALIAS_FLAG);
    // Temple arms
    private final Paint templePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // Nose bridge
    private final Paint bridgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // GPU-compatible drop shadow (semi-transparent offset copy, no BlurMaskFilter)
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ARFrameOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Hardware layer: renders on GPU for smooth 30fps+ overlay
        setLayerType(LAYER_TYPE_HARDWARE, null);
        initPaints();
    }

    private void initPaints() {
        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setStrokeWidth(14f);
        rimPaint.setStrokeCap(Paint.Cap.ROUND);
        rimPaint.setStrokeJoin(Paint.Join.ROUND);

        rimHighlight.setStyle(Paint.Style.STROKE);
        rimHighlight.setStrokeWidth(3f);
        rimHighlight.setStrokeCap(Paint.Cap.ROUND);
        rimHighlight.setAlpha(100);
        rimHighlight.setColor(Color.WHITE);

        lensFill.setStyle(Paint.Style.FILL);

        glare.setStyle(Paint.Style.FILL);
        glare.setColor(Color.argb(40, 255, 255, 255));

        templePaint.setStyle(Paint.Style.STROKE);
        templePaint.setStrokeWidth(10f);
        templePaint.setStrokeCap(Paint.Cap.ROUND);
        templePaint.setStrokeJoin(Paint.Join.ROUND);

        bridgePaint.setStyle(Paint.Style.STROKE);
        bridgePaint.setStrokeWidth(10f);
        bridgePaint.setStrokeCap(Paint.Cap.ROUND);

        // Offset drop-shadow: semi-transparent, slightly thicker than the rim — no blur needed.
        // Drawn shifted (+5px, +6px) before the rim to simulate depth without BlurMaskFilter.
        shadowPaint.setStyle(Paint.Style.STROKE);
        shadowPaint.setColor(Color.argb(60, 0, 0, 0));
        shadowPaint.setStrokeWidth(20f);
        shadowPaint.setStrokeCap(Paint.Cap.ROUND);
        shadowPaint.setStrokeJoin(Paint.Join.ROUND);
    }

    public void updateFaces(List<Face> faces, int imageWidth, int imageHeight) {
        this.faces = faces;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;

        // Throttle: only redraw if the primary face has moved more than REDRAW_THRESHOLD_PX.
        // This prevents unnecessary GPU layer re-compositing when the user's head is still.
        if (faces != null && !faces.isEmpty()) {
            android.graphics.RectF box = faces.get(0).getBoundingBox() != null
                    ? new android.graphics.RectF(faces.get(0).getBoundingBox())
                    : null;
            if (box != null) {
                float cx = box.centerX();
                float cy = box.centerY();
                if (Math.abs(cx - mLastFaceCenterX) < REDRAW_THRESHOLD_PX
                        && Math.abs(cy - mLastFaceCenterY) < REDRAW_THRESHOLD_PX) {
                    return; // face hasn't moved enough — skip redraw
                }
                mLastFaceCenterX = cx;
                mLastFaceCenterY = cy;
            }
        }
        invalidate();
    }

    public void setFrameStyle(String style) {
        this.selectedFrame = style;
        invalidate();
    }

    public void setFrameColor(String hexColor) {
        try {
            this.frameColor = Color.parseColor(hexColor);
        } catch (Exception e) {
            this.frameColor = Color.parseColor("#141414");
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (faces == null || faces.isEmpty() || "None".equalsIgnoreCase(selectedFrame)) return;

        // Derive a slightly lighter shade for the highlight/bevel
        int r = Color.red(frameColor);
        int g = Color.green(frameColor);
        int b = Color.blue(frameColor);
        int highlight = Color.rgb(Math.min(255, r + 70), Math.min(255, g + 70), Math.min(255, b + 70));
        int shadow   = Color.rgb(Math.max(0, r - 40), Math.max(0, g - 40), Math.max(0, b - 40));

        rimPaint.setColor(frameColor);
        rimHighlight.setColor(highlight);
        templePaint.setColor(frameColor);
        bridgePaint.setColor(frameColor);
        shadowPaint.setColor(Color.argb(90, Math.max(0, r - 60), Math.max(0, g - 60), Math.max(0, b - 60)));

        // Translucent tinted lens (blue-tinted look like real lenses)
        int lensAlpha = 45;
        int lensR = Math.min(255, r + 20);
        int lensG = Math.min(255, g + 20);
        int lensB = Math.min(255, b + 50);
        lensFill.setColor(Color.argb(lensAlpha, lensR, lensG, lensB));

        float scaleX = (float) getWidth() / (float) Math.max(1, imageWidth);
        float scaleY = (float) getHeight() / (float) Math.max(1, imageHeight);

        for (Face face : faces) {
            FaceLandmark leftEye  = face.getLandmark(FaceLandmark.LEFT_EYE);
            FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);

            if (leftEye == null || rightEye == null) continue;

            // Mirror for front camera
            float lx = getWidth() - (leftEye.getPosition().x  * scaleX);
            float ly =               leftEye.getPosition().y  * scaleY;
            float rx = getWidth() - (rightEye.getPosition().x * scaleX);
            float ry =               rightEye.getPosition().y * scaleY;

            float centerX = (lx + rx) / 2f;
            float centerY = (ly + ry) / 2f;
            float dx = rx - lx;
            float dy = ry - ly;
            float eyeDistance = (float) Math.hypot(dx, dy);
            float angle = (float) Math.toDegrees(Math.atan2(dy, dx));

            canvas.save();
            canvas.translate(centerX, centerY);
            canvas.rotate(angle);

            drawGlassesFrame(canvas, eyeDistance, selectedFrame, highlight);
            canvas.restore();
        }
    }

    private void drawGlassesFrame(Canvas canvas, float eyeDist, String style, int highlight) {
        // Size everything relative to inter-eye distance
        float lensW   = eyeDist * 1.05f;   // width of one lens
        float gap     = eyeDist * 0.10f;   // nose bridge gap
        float lcx     = -(lensW / 2f + gap / 2f);
        float rcx     =   lensW / 2f + gap / 2f;
        float templeL = lensW * 0.90f;

        switch (style) {
            case "Wayfarer":
            default:
                drawWayfarer(canvas, lensW, lcx, rcx, templeL, highlight);
                break;
            case "Rectangle":
                drawRectangle(canvas, lensW, lcx, rcx, templeL, highlight);
                break;
            case "Square":
                drawSquare(canvas, lensW, lcx, rcx, templeL, highlight);
                break;
            case "Round":
                drawRound(canvas, lensW, lcx, rcx, templeL, highlight);
                break;
            case "Oval":
                drawOval(canvas, lensW, lcx, rcx, templeL, highlight);
                break;
            case "Aviator":
                drawAviator(canvas, lensW, lcx, rcx, templeL, highlight);
                break;
            case "Cat Eye":
                drawCatEye(canvas, lensW, lcx, rcx, templeL, highlight);
                break;
            case "Geometric":
                drawGeometric(canvas, lensW, lcx, rcx, templeL, highlight);
                break;
            case "Browline":
                drawBrowline(canvas, lensW, lcx, rcx, templeL, highlight);
                break;
        }
    }

    // ───────────────────────────── WAYFARER ─────────────────────────────
    private void drawWayfarer(Canvas canvas, float lensW, float lcx, float rcx, float templeL, int hl) {
        float h = lensW * 0.72f;
        float r = lensW * 0.18f;
        RectF lRect = new RectF(lcx - lensW/2, -h/2, lcx + lensW/2, h/2);
        RectF rRect = new RectF(rcx - lensW/2, -h/2, rcx + lensW/2, h/2);
        drawLensAndRim(canvas, lRect, rRect, r, lcx, rcx, lensW, h, templeL, hl);
    }

    // ───────────────────────────── RECTANGLE ─────────────────────────────
    private void drawRectangle(Canvas canvas, float lensW, float lcx, float rcx, float templeL, int hl) {
        float h = lensW * 0.58f;
        float r = lensW * 0.06f;
        RectF lRect = new RectF(lcx - lensW/2, -h/2, lcx + lensW/2, h/2);
        RectF rRect = new RectF(rcx - lensW/2, -h/2, rcx + lensW/2, h/2);
        drawLensAndRim(canvas, lRect, rRect, r, lcx, rcx, lensW, h, templeL, hl);
    }

    // ───────────────────────────── SQUARE ─────────────────────────────
    private void drawSquare(Canvas canvas, float lensW, float lcx, float rcx, float templeL, int hl) {
        float h = lensW * 0.90f;
        float r = lensW * 0.08f;
        RectF lRect = new RectF(lcx - lensW/2, -h/2, lcx + lensW/2, h/2);
        RectF rRect = new RectF(rcx - lensW/2, -h/2, rcx + lensW/2, h/2);
        drawLensAndRim(canvas, lRect, rRect, r, lcx, rcx, lensW, h, templeL, hl);
    }

    // ───────────────────────────── ROUND ─────────────────────────────
    private void drawRound(Canvas canvas, float lensW, float lcx, float rcx, float templeL, int hl) {
        float rad = lensW / 2f;
        // Shadow
        canvas.drawCircle(lcx, 4f, rad, shadowPaint);
        canvas.drawCircle(rcx, 4f, rad, shadowPaint);
        // Fill
        canvas.drawCircle(lcx, 0, rad, lensFill);
        canvas.drawCircle(rcx, 0, rad, lensFill);
        // Glare
        canvas.drawOval(new RectF(lcx - rad*0.4f, -rad*0.6f, lcx + rad*0.1f, -rad*0.1f), glare);
        canvas.drawOval(new RectF(rcx - rad*0.4f, -rad*0.6f, rcx + rad*0.1f, -rad*0.1f), glare);
        // Rim
        canvas.drawCircle(lcx, 0, rad, rimPaint);
        canvas.drawCircle(rcx, 0, rad, rimPaint);
        // Highlight bevel
        rimHighlight.setColor(Color.argb(100, 255, 255, 255));
        canvas.drawArc(new RectF(lcx-rad, -rad, lcx+rad, rad), 200, 140, false, rimHighlight);
        canvas.drawArc(new RectF(rcx-rad, -rad, rcx+rad, rad), 200, 140, false, rimHighlight);
        // Bridge & temples
        canvas.drawLine(lcx + rad, -rad*0.15f, rcx - rad, -rad*0.15f, bridgePaint);
        canvas.drawLine(lcx - rad, -rad*0.15f, lcx - rad - templeL, 0, templePaint);
        canvas.drawLine(rcx + rad, -rad*0.15f, rcx + rad + templeL, 0, templePaint);
    }

    // ───────────────────────────── OVAL ─────────────────────────────
    private void drawOval(Canvas canvas, float lensW, float lcx, float rcx, float templeL, int hl) {
        float h = lensW * 0.65f;
        float r = lensW * 0.35f;
        RectF lRect = new RectF(lcx - lensW/2, -h/2, lcx + lensW/2, h/2);
        RectF rRect = new RectF(rcx - lensW/2, -h/2, rcx + lensW/2, h/2);
        // Shadow
        shadowPaint.setStyle(Paint.Style.FILL);
        canvas.drawOval(new RectF(lcx - lensW/2, -h/2+6, lcx + lensW/2, h/2+6), shadowPaint);
        canvas.drawOval(new RectF(rcx - lensW/2, -h/2+6, rcx + lensW/2, h/2+6), shadowPaint);
        shadowPaint.setStyle(Paint.Style.STROKE);
        // Fill + glare + rim
        canvas.drawOval(lRect, lensFill);
        canvas.drawOval(rRect, lensFill);
        drawGlare(canvas, lRect);
        drawGlare(canvas, rRect);
        canvas.drawOval(lRect, rimPaint);
        canvas.drawOval(rRect, rimPaint);
        rimHighlight.setColor(Color.argb(100, 255, 255, 255));
        canvas.drawArc(lRect, 200, 130, false, rimHighlight);
        canvas.drawArc(rRect, 200, 130, false, rimHighlight);
        // Bridge & temples
        canvas.drawLine(lcx + lensW/2, -h*0.1f, rcx - lensW/2, -h*0.1f, bridgePaint);
        canvas.drawLine(lcx - lensW/2, -h*0.1f, lcx - lensW/2 - templeL, -h*0.05f, templePaint);
        canvas.drawLine(rcx + lensW/2, -h*0.1f, rcx + lensW/2 + templeL, -h*0.05f, templePaint);
    }

    // ───────────────────────────── AVIATOR ─────────────────────────────
    private void drawAviator(Canvas canvas, float lensW, float lcx, float rcx, float templeL, int hl) {
        float h = lensW * 0.95f;
        RectF lRect = new RectF(lcx - lensW/2, -h*0.42f, lcx + lensW/2, h*0.42f);
        RectF rRect = new RectF(rcx - lensW/2, -h*0.42f, rcx + lensW/2, h*0.42f);
        // Shadow
        shadowPaint.setStyle(Paint.Style.FILL);
        canvas.drawOval(new RectF(lRect.left, lRect.top+6, lRect.right, lRect.bottom+6), shadowPaint);
        canvas.drawOval(new RectF(rRect.left, rRect.top+6, rRect.right, rRect.bottom+6), shadowPaint);
        shadowPaint.setStyle(Paint.Style.STROKE);
        // Fill + glare + rim
        canvas.drawOval(lRect, lensFill);
        canvas.drawOval(rRect, lensFill);
        drawGlare(canvas, lRect);
        drawGlare(canvas, rRect);
        canvas.drawOval(lRect, rimPaint);
        canvas.drawOval(rRect, rimPaint);
        rimHighlight.setColor(Color.argb(100, 255, 255, 255));
        canvas.drawArc(lRect, 210, 120, false, rimHighlight);
        canvas.drawArc(rRect, 210, 120, false, rimHighlight);
        // Double bar bridge (aviator signature)
        canvas.drawLine(lcx + lensW/2, -h*0.25f, rcx - lensW/2, -h*0.25f, bridgePaint);
        canvas.drawLine(lcx + lensW/2, -h*0.38f, rcx - lensW/2, -h*0.38f, bridgePaint);
        // Temples with curve
        canvas.drawLine(lcx - lensW/2, -h*0.18f, lcx - lensW/2 - templeL, -h*0.08f, templePaint);
        canvas.drawLine(rcx + lensW/2, -h*0.18f, rcx + lensW/2 + templeL, -h*0.08f, templePaint);
    }

    // ───────────────────────────── CAT EYE ─────────────────────────────
    private void drawCatEye(Canvas canvas, float lensW, float lcx, float rcx, float templeL, int hl) {
        float h = lensW * 0.62f;
        Path lPath = catEyePath(lcx, lensW, h, true);
        Path rPath = catEyePath(rcx, lensW, h, false);
        canvas.drawPath(lPath, lensFill);
        canvas.drawPath(rPath, lensFill);
        canvas.drawPath(lPath, rimPaint);
        canvas.drawPath(rPath, rimPaint);
        // Bridge & temples
        canvas.drawLine(lcx + lensW/2, -h*0.15f, rcx - lensW/2, -h*0.15f, bridgePaint);
        canvas.drawLine(lcx - lensW/2, -h*0.1f, lcx - lensW/2 - templeL*0.7f, -h*0.5f, templePaint);
        canvas.drawLine(rcx + lensW/2, -h*0.1f, rcx + lensW/2 + templeL*0.7f, -h*0.5f, templePaint);
    }

    private Path catEyePath(float cx, float lensW, float h, boolean isLeft) {
        Path p = new Path();
        float ow = lensW / 2f;
        float oh = h / 2f;
        float tipX = isLeft ? cx + ow : cx - ow;
        float tipY = -oh * 1.1f;
        p.moveTo(cx - ow, 0);
        p.quadTo(cx - ow, oh, cx, oh * 0.9f);
        p.quadTo(cx + ow * 0.8f, oh * 0.8f, cx + ow, 0);
        p.quadTo(cx + ow * 0.85f, -oh * 0.5f, tipX, tipY);
        p.quadTo(cx, -oh * 1.0f, cx - ow, 0);
        p.close();
        return p;
    }

    // ───────────────────────────── GEOMETRIC (hexagon) ─────────────────────────────
    private void drawGeometric(Canvas canvas, float lensW, float lcx, float rcx, float templeL, int hl) {
        float h = lensW * 0.75f;
        Path lPath = hexagonPath(lcx, lensW * 0.52f, h * 0.52f);
        Path rPath = hexagonPath(rcx, lensW * 0.52f, h * 0.52f);
        canvas.drawPath(lPath, lensFill);
        canvas.drawPath(rPath, lensFill);
        canvas.drawPath(lPath, rimPaint);
        canvas.drawPath(rPath, rimPaint);
        canvas.drawLine(lcx + lensW*0.45f, -h*0.15f, rcx - lensW*0.45f, -h*0.15f, bridgePaint);
        canvas.drawLine(lcx - lensW*0.5f, -h*0.1f, lcx - lensW*0.5f - templeL, -h*0.05f, templePaint);
        canvas.drawLine(rcx + lensW*0.5f, -h*0.1f, rcx + lensW*0.5f + templeL, -h*0.05f, templePaint);
    }

    private Path hexagonPath(float cx, float rx, float ry) {
        Path p = new Path();
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI / 180 * (60 * i - 30);
            float x = cx + rx * (float) Math.cos(angle);
            float y =      ry * (float) Math.sin(angle);
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.close();
        return p;
    }

    // ───────────────────────────── BROWLINE ─────────────────────────────
    private void drawBrowline(Canvas canvas, float lensW, float lcx, float rcx, float templeL, int hl) {
        float h = lensW * 0.68f;
        float r = lensW * 0.10f;
        RectF lRect = new RectF(lcx - lensW/2, -h/2, lcx + lensW/2, h/2);
        RectF rRect = new RectF(rcx - lensW/2, -h/2, rcx + lensW/2, h/2);
        // Draw thin bottom half (just the wire rim bottom arc)
        Paint wireBottom = new Paint(rimPaint);
        wireBottom.setStrokeWidth(5f);
        canvas.drawOval(lRect, lensFill);
        canvas.drawOval(rRect, lensFill);
        drawGlare(canvas, lRect);
        drawGlare(canvas, rRect);
        // Full outline thin
        canvas.drawRoundRect(lRect, r, r, wireBottom);
        canvas.drawRoundRect(rRect, r, r, wireBottom);
        // Thick top brow bar
        Paint browBar = new Paint(rimPaint);
        browBar.setStrokeWidth(22f);
        browBar.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(lcx - lensW/2, -h/2, lcx + lensW/2, -h/2, browBar);
        canvas.drawLine(rcx - lensW/2, -h/2, rcx + lensW/2, -h/2, browBar);
        // Bridge & temples
        canvas.drawLine(lcx + lensW/2, -h*0.15f, rcx - lensW/2, -h*0.15f, bridgePaint);
        canvas.drawLine(lcx - lensW/2, -h*0.25f, lcx - lensW/2 - templeL, -h*0.1f, templePaint);
        canvas.drawLine(rcx + lensW/2, -h*0.25f, rcx + lensW/2 + templeL, -h*0.1f, templePaint);
    }

    // ───────────────────────────── SHARED HELPERS ─────────────────────────────
    private void drawLensAndRim(Canvas canvas, RectF lRect, RectF rRect, float r,
                                 float lcx, float rcx, float lensW, float h, float templeL, int hl) {
        // Shadow
        shadowPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(lRect.left, lRect.top + 6, lRect.right, lRect.bottom + 6), r, r, shadowPaint);
        canvas.drawRoundRect(new RectF(rRect.left, rRect.top + 6, rRect.right, rRect.bottom + 6), r, r, shadowPaint);
        shadowPaint.setStyle(Paint.Style.STROKE);
        // Translucent fill
        canvas.drawRoundRect(lRect, r, r, lensFill);
        canvas.drawRoundRect(rRect, r, r, lensFill);
        // Glare
        drawGlare(canvas, lRect);
        drawGlare(canvas, rRect);
        // Thick rim
        canvas.drawRoundRect(lRect, r, r, rimPaint);
        canvas.drawRoundRect(rRect, r, r, rimPaint);
        // Bevel highlight on top-left arc
        rimHighlight.setColor(Color.argb(100, 255, 255, 255));
        canvas.drawArc(lRect, 200, 130, false, rimHighlight);
        canvas.drawArc(rRect, 200, 130, false, rimHighlight);
        // Bridge
        canvas.drawLine(lcx + lensW/2, -h*0.2f, rcx - lensW/2, -h*0.2f, bridgePaint);
        // Temple arms
        canvas.drawLine(lcx - lensW/2, -h*0.15f, lcx - lensW/2 - templeL, -h*0.05f, templePaint);
        canvas.drawLine(rcx + lensW/2, -h*0.15f, rcx + lensW/2 + templeL, -h*0.05f, templePaint);
    }

    private void drawGlare(Canvas canvas, RectF bounds) {
        float gw = bounds.width()  * 0.30f;
        float gh = bounds.height() * 0.28f;
        RectF gRect = new RectF(bounds.left + bounds.width()*0.12f,
                bounds.top  + bounds.height()*0.10f,
                bounds.left + bounds.width()*0.12f + gw,
                bounds.top  + bounds.height()*0.10f + gh);
        canvas.drawOval(gRect, glare);
    }
}
