package com.lensmatch.mobile.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.AppState;
import com.lensmatch.mobile.ui.MainActivity;
import com.lensmatch.mobile.utils.StatusBarUtils;

/**
 * Animated Landing Activity shown when the user first opens the app.
 * Features an animated logo entrance, branding intro, and smooth tap/swipe
 * interaction to proceed to Login / Sign Up (or MainActivity if already signed in).
 * Pinned to Light Mode.
 */
public class LandingActivity extends AppCompatActivity {

    private GestureDetector gestureDetector;
    private boolean isNavigating = false;
    private float touchStartY = 0f;

    private MaterialCardView cardLogo;
    private TextView tvTitle;
    private TextView tvTagline;
    private TextView tvDescription;
    private LinearLayout layoutBadges;
    private LinearLayout layoutSwipePrompt;
    private ImageView ivSwipeChevron;
    private TextView tvSwipeHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Enforce Light Mode for Landing Page
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_landing);

        // Configure Status Bar for Blue Surf background (white icons)
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
        }

        View root = findViewById(R.id.landing_root);
        StatusBarUtils.applyTopWindowInsets(root);

        bindViews();
        setupGestures(root);
        prepareInitialAnimationState();
        startEntranceAnimations();
    }

    private void bindViews() {
        cardLogo = findViewById(R.id.card_logo_container);
        tvTitle = findViewById(R.id.tv_landing_title);
        tvTagline = findViewById(R.id.tv_landing_tagline);
        tvDescription = findViewById(R.id.tv_landing_description);
        layoutBadges = findViewById(R.id.layout_feature_badges);
        layoutSwipePrompt = findViewById(R.id.layout_swipe_prompt);
        ivSwipeChevron = findViewById(R.id.iv_swipe_chevron);
        tvSwipeHint = findViewById(R.id.tv_swipe_hint);

        if (layoutSwipePrompt != null) {
            layoutSwipePrompt.setOnClickListener(v -> proceedToAuth());
        }
    }

    private void setupGestures(View root) {
        View content = findViewById(R.id.landing_content);

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                proceedToAuth();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 != null && e2 != null) {
                    float deltaY = e1.getY() - e2.getY();
                    if (deltaY > 40 && Math.abs(velocityY) > 80) {
                        // Swipe Up detected
                        proceedToAuth();
                        return true;
                    }
                }
                return false;
            }
        });

        root.setOnTouchListener((v, event) -> {
            if (isNavigating) return true;

            gestureDetector.onTouchEvent(event);

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    touchStartY = event.getRawY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float deltaY = event.getRawY() - touchStartY;
                    if (deltaY < 0 && content != null) {
                        // Move landing page up in real-time under user's finger
                        content.setTranslationY(deltaY);
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    float totalDeltaY = event.getRawY() - touchStartY;
                    if (totalDeltaY < -90) {
                        // Dragged up past threshold -> proceed to auth
                        proceedToAuth();
                    } else if (content != null && totalDeltaY < 0) {
                        // Snap back down smoothly if didn't drag far enough
                        content.animate()
                                .translationY(0f)
                                .setDuration(260)
                                .setInterpolator(new OvershootInterpolator(1.2f))
                                .start();
                    }
                    return true;

                case MotionEvent.ACTION_CANCEL:
                    if (content != null) {
                        content.animate().translationY(0f).setDuration(200).start();
                    }
                    return true;
            }
            return true;
        });
    }

    private void prepareInitialAnimationState() {
        if (cardLogo != null) {
            cardLogo.setAlpha(0f);
            cardLogo.setScaleX(0.4f);
            cardLogo.setScaleY(0.4f);
        }
        if (tvTitle != null) {
            tvTitle.setAlpha(0f);
            tvTitle.setTranslationY(40f);
        }
        if (tvTagline != null) {
            tvTagline.setAlpha(0f);
            tvTagline.setTranslationY(40f);
        }
        if (tvDescription != null) {
            tvDescription.setAlpha(0f);
            tvDescription.setTranslationY(30f);
        }
        if (layoutBadges != null) {
            layoutBadges.setAlpha(0f);
            layoutBadges.setTranslationY(30f);
        }
        if (layoutSwipePrompt != null) {
            layoutSwipePrompt.setAlpha(0f);
            layoutSwipePrompt.setTranslationY(24f);
        }
    }

    private void startEntranceAnimations() {
        // 1. Logo scale & pop in
        if (cardLogo != null) {
            cardLogo.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(900)
                    .setInterpolator(new OvershootInterpolator(1.3f))
                    .start();
        }

        // 2. Title slide in
        if (tvTitle != null) {
            tvTitle.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(700)
                    .setStartDelay(250)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }

        // 3. Tagline slide in
        if (tvTagline != null) {
            tvTagline.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(700)
                    .setStartDelay(400)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }

        // 4. Description and Badges
        if (tvDescription != null) {
            tvDescription.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(700)
                    .setStartDelay(550)
                    .start();
        }
        if (layoutBadges != null) {
            layoutBadges.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(700)
                    .setStartDelay(700)
                    .start();
        }

        // 5. Swipe / Tap Prompt Entrance
        if (layoutSwipePrompt != null) {
            layoutSwipePrompt.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(800)
                    .setStartDelay(800)
                    .withEndAction(this::startPulseAnimation)
                    .start();
        }
    }

    private void startPulseAnimation() {
        if (ivSwipeChevron == null) return;
        ivSwipeChevron.animate()
                .translationY(-18f)
                .setDuration(750)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    if (ivSwipeChevron != null && !isFinishing()) {
                        ivSwipeChevron.animate()
                                .translationY(0f)
                                .setDuration(750)
                                .setInterpolator(new AccelerateDecelerateInterpolator())
                                .withEndAction(this::startPulseAnimation)
                                .start();
                    }
                })
                .start();
    }

    private synchronized void proceedToAuth() {
        if (isNavigating) return;
        isNavigating = true;

        // If user is already authenticated in Firebase, go straight to MainActivity
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String name = currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()
                    ? currentUser.getDisplayName() : "User";
            AppState.getInstance().setUserProfile(name, currentUser.getEmail(), null);

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
            finish();
            return;
        }

        // Otherwise navigate to LoginActivity
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("from_landing", true);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up);
        finish();
    }
}
