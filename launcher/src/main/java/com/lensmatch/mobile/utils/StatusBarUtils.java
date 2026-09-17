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
 * Utility class to apply safe window insets (status bar, display cutout, and navigation bar)
 * across all screens. Ensures titles, toolbars, content, and bottom action buttons are NEVER
 * covered, blocked, or overlaid by the phone's status bar or system navigation bar (3-button or gesture pill).
 */
public class StatusBarUtils {

    /**
     * Applies safe top padding (status bar & cutout) AND bottom padding (navigation bar)
     * to the specified view, added to the view's base padding.
     * Prevents BOTH the status bar at the top and the phone's system navbar at the bottom
     * from ever overlaying the app's headers, content, or buttons.
     */
    public static void applyWindowInsets(@NonNull View view) {
        final int initialPaddingLeft = view.getPaddingLeft();
        final int initialPaddingTop = view.getPaddingTop();
        final int initialPaddingRight = view.getPaddingRight();
        final int initialPaddingBottom = view.getPaddingBottom();

        int sbHeight = getStatusBarHeight(view.getContext());
        int nbHeight = getNavigationBarHeight(view.getContext());
        if (sbHeight > 0 || nbHeight > 0) {
            view.setPadding(initialPaddingLeft, initialPaddingTop + sbHeight, initialPaddingRight, initialPaddingBottom + nbHeight);
        }

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets statusInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout()
            );
            Insets navInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.navigationBars()
            );
            int topInset = statusInsets.top > 0 ? statusInsets.top : sbHeight;
            int bottomInset = navInsets.bottom > 0 ? navInsets.bottom : nbHeight;
            v.setPadding(initialPaddingLeft, initialPaddingTop + topInset, initialPaddingRight, initialPaddingBottom + bottomInset);
            return windowInsets;
        });

        ViewCompat.requestApplyInsets(view);
    }

    /**
     * Applies bottom padding to the view equal to the system navigation bar height.
     * Useful for sticky bottom bars or action button containers.
     */
    public static void applyBottomWindowInsets(@NonNull View view) {
        final int initialPaddingLeft = view.getPaddingLeft();
        final int initialPaddingTop = view.getPaddingTop();
        final int initialPaddingRight = view.getPaddingRight();
        final int initialPaddingBottom = view.getPaddingBottom();

        int nbHeight = getNavigationBarHeight(view.getContext());
        if (nbHeight > 0) {
            view.setPadding(initialPaddingLeft, initialPaddingTop, initialPaddingRight, initialPaddingBottom + nbHeight);
        }

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets navInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.navigationBars()
            );
            int bottomInset = navInsets.bottom > 0 ? navInsets.bottom : nbHeight;
            v.setPadding(initialPaddingLeft, initialPaddingTop, initialPaddingRight, initialPaddingBottom + bottomInset);
            return windowInsets;
        });

        ViewCompat.requestApplyInsets(view);
    }

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

    /**
     * Safe platform fallback to retrieve standard navigation bar height in pixels.
     */
    public static int getNavigationBarHeight(Context context) {
        if (context == null) return 0;
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return (int) (48 * resources.getDisplayMetrics().density);
    }
}
