package com.lensmatch.mobile.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

import com.lensmatch.mobile.R;

public class MaxHeightNestedScrollView extends NestedScrollView {
    private int maxHeight = -1;

    public MaxHeightNestedScrollView(@NonNull Context context) {
        super(context);
    }

    public MaxHeightNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MaxHeightNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setNestedScrollingEnabled(true);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MaxHeightNestedScrollView);
            if (a.hasValue(R.styleable.MaxHeightNestedScrollView_maxHeight)) {
                maxHeight = a.getDimensionPixelSize(R.styleable.MaxHeightNestedScrollView_maxHeight, -1);
            } else if (a.hasValue(R.styleable.MaxHeightNestedScrollView_android_maxHeight)) {
                maxHeight = a.getDimensionPixelSize(R.styleable.MaxHeightNestedScrollView_android_maxHeight, -1);
            }
            a.recycle();
        }
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (maxHeight > 0) {
            int hSize = MeasureSpec.getSize(heightMeasureSpec);
            int hMode = MeasureSpec.getMode(heightMeasureSpec);
            if (hMode == MeasureSpec.UNSPECIFIED) {
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST);
            } else {
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(Math.min(hSize, maxHeight), MeasureSpec.AT_MOST);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private float lastRawY = 0f;
    private float lastRawX = 0f;
    private float downRawY = 0f;
    private float downRawX = 0f;
    private boolean isBeingDragged = false;
    private int touchSlop = 0;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                downRawY = ev.getRawY();
                downRawX = ev.getRawX();
                lastRawY = ev.getRawY();
                lastRawX = ev.getRawX();
                isBeingDragged = false;
                // If this view has scrollable content, initially prevent parent from stealing the touch
                if (canScrollVertically(1) || canScrollVertically(-1)) {
                    requestDisallowInterceptTouchEvent(true);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                float currentRawY = ev.getRawY();
                float currentRawX = ev.getRawX();
                float stepDy = lastRawY - currentRawY; // positive = dragging finger up (scrolling down)
                float totalDy = downRawY - currentRawY;
                float totalDx = downRawX - currentRawX;

                lastRawY = currentRawY;
                lastRawX = currentRawX;

                if (!isBeingDragged) {
                    if (Math.abs(totalDy) > touchSlop && Math.abs(totalDy) > Math.abs(totalDx)) {
                        isBeingDragged = true;
                    }
                }

                if (isBeingDragged) {
                    // Check if this view can scroll in the direction of the current finger movement
                    boolean canScrollInDirection = (stepDy > 0 && canScrollVertically(1))
                            || (stepDy < 0 && canScrollVertically(-1));

                    if (canScrollInDirection) {
                        requestDisallowInterceptTouchEvent(true);
                    } else {
                        // Reached top or bottom edge -> allow outer NestedScrollView to take over
                        requestDisallowInterceptTouchEvent(false);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isBeingDragged = false;
                requestDisallowInterceptTouchEvent(false);
                break;
        }
        return super.dispatchTouchEvent(ev);
    }
}
