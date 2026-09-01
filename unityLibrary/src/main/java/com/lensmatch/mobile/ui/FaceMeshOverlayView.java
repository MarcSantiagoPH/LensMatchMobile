package com.lensmatch.mobile.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.google.mlkit.vision.face.Face;

import java.util.List;

public class FaceMeshOverlayView extends View {
    private List<Face> faces;
    private boolean isAligned = false;
    private int imageWidth = 1;
    private int imageHeight = 1;

    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public FaceMeshOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setColor(Color.parseColor("#8000FF66")); // Semi-transparent green

        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4f);
        boxPaint.setColor(Color.WHITE);
    }

    public void updateFaces(List<Face> faces, boolean isAligned, int imageWidth, int imageHeight) {
        this.faces = faces;
        this.isAligned = isAligned;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;

        if (isAligned) {
            pointPaint.setColor(Color.parseColor("#CC00FF66"));
            boxPaint.setColor(Color.parseColor("#00FF66"));
        } else {
            pointPaint.setColor(Color.parseColor("#80FFFFFF"));
            boxPaint.setColor(Color.parseColor("#80FFFFFF"));
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (faces == null || faces.isEmpty()) return;

        float scaleX = (float) getWidth() / (float) imageHeight; // Account for 90-degree camera rotation
        float scaleY = (float) getHeight() / (float) imageWidth;

        for (Face face : faces) {
            Rect bounds = face.getBoundingBox();

            // Flip horizontally for front-facing mirror camera
            float left = getWidth() - (bounds.right * scaleX);
            float right = getWidth() - (bounds.left * scaleX);
            float top = bounds.top * scaleY;
            float bottom = bounds.bottom * scaleY;

            canvas.drawRoundRect(left, top, right, bottom, 24f, 24f, boxPaint);
        }
    }
}
