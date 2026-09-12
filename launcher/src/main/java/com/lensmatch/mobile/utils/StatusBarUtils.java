package com.lensmatch.mobile.utils;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Utility class to apply safe top window insets and status bar spacing across all screens.
 * Ensures titles, toolbars, and top buttons are never covered or blocked by the phone's
 * status bar, notification icons, or display cutout (camera notch/punch hole).
 */
public class StatusBarUtils {

    /**
     * Applies top padding to the specified view equal to the status bar and display cutout height,
     * added to the view's base top padding.
     */
    public static void applyTopWindowInsets(@NonNull View view) {
        final int initialPaddingLeft = view.getPaddingLeft();
        final int initialPaddingTop = view.getPaddingTop();
        final int initialPaddingRight = view.getPaddingRight();
        final int initialPaddingBottom = view.getPaddingBottom();

        int sbHeight = getStatusBarHeight(view.getContext());
        if (sbHeight > 0) {
            view.setPadding(initialPaddingLeft, initialPaddingTop + sbHeight, initialPaddingRight, initialPaddingBottom);
        }

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout()
            );
            int topInset = insets.top > 0 ? insets.top : sbHeight;
            v.setPadding(initialPaddingLeft, initialPaddingTop + topInset, initialPaddingRight, initialPaddingBottom);
            return windowInsets;
        });

        ViewCompat.requestApplyInsets(view);
    }

    /**
     * Applies top margin to the specified view equal to the status bar and display cutout height,
     * added to the view's base top margin. Useful for floating buttons (e.g., back or close buttons).
     */
    public static void applyTopMargin(@NonNull View view) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (!(lp instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }

        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
        final int initialMarginTop = mlp.topMargin;

        int sbHeight = getStatusBarHeight(view.getContext());
        if (sbHeight > 0) {
            mlp.topMargin = initialMarginTop + sbHeight;
            view.setLayoutParams(mlp);
        }

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout()
            );
            int topInset = insets.top > 0 ? insets.top : sbHeight;
            if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                params.topMargin = initialMarginTop + topInset;
                v.setLayoutParams(params);
            }
            return windowInsets;
        });

        ViewCompat.requestApplyInsets(view);
    }

    /**
     * Safe platform fallback to retrieve standard status bar height in pixels.
     */
    public static int getStatusBarHeight(Context context) {
        if (context == null) return 0;
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return (int) (24 * resources.getDisplayMetrics().density);
    }
}
