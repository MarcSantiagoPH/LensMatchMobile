package com.lensmatch.mobile.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;

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

    private float startY = 0f;
    private float startX = 0f;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                startY = ev.getY();
                startX = ev.getX();
                if (canScrollVertically(1) || canScrollVertically(-1)) {
                    requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float dy = startY - ev.getY();
                float dx = startX - ev.getX();
                if (Math.abs(dy) > Math.abs(dx)) {
                    if (dy > 0 && canScrollVertically(1)) {
                        requestDisallowInterceptTouchEvent(true);
                    } else if (dy < 0 && canScrollVertically(-1)) {
                        requestDisallowInterceptTouchEvent(true);
                    } else {
                        requestDisallowInterceptTouchEvent(false);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                requestDisallowInterceptTouchEvent(false);
                break;
        }
        return super.dispatchTouchEvent(ev);
    }
}
