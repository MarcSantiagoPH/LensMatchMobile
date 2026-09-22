package com.lensmatch.mobile.ui.guidelines;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.AppState;
import com.lensmatch.mobile.ui.MainActivity;
import com.lensmatch.mobile.utils.StatusBarUtils;

import java.util.ArrayList;
import java.util.List;

public class AppGuidelinesActivity extends AppCompatActivity {
    public static final String EXTRA_FROM_SUPPORT = "from_support";

    private ViewPager2 viewPager;
    private LinearLayout layoutDots;
    private MaterialButton btnPrev;
    private MaterialButton btnNext;
    private MaterialButton btnSkip;
    private List<GuidelineSlideModel> slides;
    private boolean isFromSupport = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_guidelines);

        isFromSupport = getIntent().getBooleanExtra(EXTRA_FROM_SUPPORT, false);

        View root = findViewById(R.id.guidelines_root);
        StatusBarUtils.applyWindowInsets(root);

        getWindow().setStatusBarColor(android.graphics.Color.WHITE);
        getWindow().setNavigationBarColor(android.graphics.Color.WHITE);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(true);
            controller.setAppearanceLightNavigationBars(true);
        }

        bindViews();
        setupSlides();
        setupViewPager();
        setupDotsIndicator();
        setupListeners();
        updateControls(0);
    }

    private void bindViews() {
        viewPager = findViewById(R.id.view_pager_guidelines);
        layoutDots = findViewById(R.id.layout_dots_indicator);
        btnPrev = findViewById(R.id.btn_guidelines_prev);
        btnNext = findViewById(R.id.btn_guidelines_next);
        btnSkip = findViewById(R.id.btn_guidelines_skip);

        if (isFromSupport && btnSkip != null) {
            btnSkip.setText("Close");
        }
    }

    private void setupSlides() {
        slides = new ArrayList<>();

        // Slide 1: Face Scan
        slides.add(new GuidelineSlideModel(
                R.drawable.ic_camera,
                "STEP 1 OF 4 • CAMERA SCAN",
                "Scan Your Face",
                "AI Facial Structure Analysis",
                "Tap the camera button on the bottom navigation bar to capture or upload your photo. LensMatch evaluates your face proportions, cheekbones, jawline, and forehead in real-time.",
                "Hold your phone steady at eye level inside the oval guide with balanced lighting for the most accurate face shape classification."
        ));

        // Slide 2: Recommendations
        slides.add(new GuidelineSlideModel(
                R.drawable.ic_lightbulb,
                "STEP 2 OF 4 • RECOMMENDATIONS",
                "Smart Recommendations",
                "Curated Eyewear by Face Shape",
                "Once your face shape (Oval, Round, Square, Heart, Oblong, Diamond) is identified, LensMatch curates frame styles designed to balance and flatter your facial contours.",
                "Explore the 'Recommendations' tab to view tailored frame picks and detailed styling advice explaining why they match your face."
        ));

        // Slide 3: Frame Preview with AR
        slides.add(new GuidelineSlideModel(
                R.drawable.ic_eyeglasses,
                "STEP 3 OF 4 • FRAME PREVIEW WITH AR",
                "Frame Preview with AR",
                "Interactive 3D Augmented Reality",
                "Tap 'Preview Frame' on any eyewear to inspect how frames look and fit on your face in real-time 3D augmented reality.",
                "Turn your head side-to-side to view frame width, temple angles, and nose bridge positioning before reserving."
        ));

        // Slide 4: Reservations
        slides.add(new GuidelineSlideModel(
                R.drawable.ic_calendar,
                "STEP 4 OF 4 • RESERVATIONS",
                "In-Clinic Fitting & Pickup",
                "Reserve Your Favorite Frames",
                "Found the perfect frames? Reserve them directly in the app to hold them at our optical clinic, check store hours, and complete your professional fitting in person.",
                "Track all your reservations and live appointment updates on the Homepage and in your Profile tab."
        ));
    }

    private void setupViewPager() {
        AppGuidelinesAdapter adapter = new AppGuidelinesAdapter(slides);
        viewPager.setAdapter(adapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateControls(position);
            }
        });
    }

    private void setupDotsIndicator() {
        if (layoutDots == null || slides == null) return;
        layoutDots.removeAllViews();

        int marginHorizontal = (int) (4 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < slides.size(); i++) {
            ImageView dot = new ImageView(this);
            dot.setImageDrawable(androidx.core.content.ContextCompat.getDrawable(this,
                    i == 0 ? R.drawable.bg_indicator_dot_active : R.drawable.bg_indicator_dot_inactive));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(marginHorizontal, 0, marginHorizontal, 0);
            dot.setLayoutParams(params);
            layoutDots.addView(dot);
        }
    }

    private void updateControls(int position) {
        // Update Dots
        if (layoutDots != null) {
            for (int i = 0; i < layoutDots.getChildCount(); i++) {
                View child = layoutDots.getChildAt(i);
                if (child instanceof ImageView) {
                    ((ImageView) child).setImageDrawable(androidx.core.content.ContextCompat.getDrawable(this,
                            i == position ? R.drawable.bg_indicator_dot_active : R.drawable.bg_indicator_dot_inactive));
                }
            }
        }

        // Update Prev button
        if (btnPrev != null) {
            btnPrev.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
        }

        // Update Next button
        if (btnNext != null) {
            if (position == slides.size() - 1) {
                btnNext.setText(isFromSupport ? "Done" : "Get Started");
            } else {
                btnNext.setText("Next");
            }
        }
    }

    private void setupListeners() {
        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> {
                int current = viewPager.getCurrentItem();
                if (current > 0) {
                    viewPager.setCurrentItem(current - 1, true);
                }
            });
        }

        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                int current = viewPager.getCurrentItem();
                if (current < slides.size() - 1) {
                    viewPager.setCurrentItem(current + 1, true);
                } else {
                    finishGuidelines();
                }
            });
        }

        if (btnSkip != null) {
            btnSkip.setOnClickListener(v -> finishGuidelines());
        }
    }

    private void finishGuidelines() {
        if (isFromSupport) {
            finish();
            return;
        }

        AppState.getInstance().setHasSeenGuidelines(true);
        AppState.getInstance().setLastActiveTab(R.id.nav_home);

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("open_tab", R.id.nav_home);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
