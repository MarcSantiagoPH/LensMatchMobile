package com.lensmatch.mobile.ui.catalog;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.ClinicModel;
import com.lensmatch.mobile.service.FirestoreService;
import com.lensmatch.mobile.ui.MainActivity;
import com.lensmatch.mobile.utils.StatusBarUtils;

public class FrameReservationNoticeActivity extends AppCompatActivity {

    public static final String EXTRA_FRAME_STYLE = "frameStyle";
    public static final String EXTRA_COLOR_VARIANT = "colorVariant";

    private String frameStyle = "Wayfarer";
    private String colorVariant = "Original Black";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Clear any fullscreen flags that might carry over from Unity
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Allow edge-to-edge window decor so we can control status bar & nav bar insets precisely
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_frame_reservation_notice);

        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);

        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(true);
            insetsController.setAppearanceLightNavigationBars(true);
            insetsController.show(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
        }

        View root = findViewById(R.id.notice_root);
        View bottomActions = findViewById(R.id.layout_notice_bottom_actions);

        int sbFallback = StatusBarUtils.getStatusBarHeight(this);
        int nbFallback = StatusBarUtils.getNavigationBarHeight(this);
        int baseBottomPadding = (int) (12 * getResources().getDisplayMetrics().density);

        // Initial safe padding before insets listener fires
        if (root != null) {
            root.setPadding(0, sbFallback, 0, 0);
        }
        if (bottomActions != null) {
            bottomActions.setPadding(
                    bottomActions.getPaddingLeft(),
                    bottomActions.getPaddingTop(),
                    bottomActions.getPaddingRight(),
                    baseBottomPadding + nbFallback
            );
        }

        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
                Insets statusInsets = windowInsets.getInsets(
                        WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout()
                );
                Insets navInsets = windowInsets.getInsets(
                        WindowInsetsCompat.Type.navigationBars()
                );

                int topInset = statusInsets.top > 0 ? statusInsets.top : sbFallback;
                int bottomInset = navInsets.bottom > 0 ? navInsets.bottom : nbFallback;

                // Push toolbar safely below status bar
                v.setPadding(0, topInset, 0, 0);

                // Push action buttons safely above system navigation bar without excessive floating gap
                if (bottomActions != null) {
                    bottomActions.setPadding(
                            bottomActions.getPaddingLeft(),
                            bottomActions.getPaddingTop(),
                            bottomActions.getPaddingRight(),
                            baseBottomPadding + bottomInset
                    );
                }

                return windowInsets;
            });
            ViewCompat.requestApplyInsets(root);
        }

        if (getIntent() != null) {
            if (getIntent().hasExtra(EXTRA_FRAME_STYLE)) {
                String raw = getIntent().getStringExtra(EXTRA_FRAME_STYLE);
                if (raw != null && !raw.trim().isEmpty()) {
                    frameStyle = toCatalogStyleName(raw);
                }
            }
            if (getIntent().hasExtra(EXTRA_COLOR_VARIANT)) {
                String rawColor = getIntent().getStringExtra(EXTRA_COLOR_VARIANT);
                if (rawColor != null && !rawColor.trim().isEmpty()) {
                    colorVariant = formatColorVariant(rawColor);
                }
            }
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar_notice);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvFrameName = findViewById(R.id.tv_notice_frame_name);
        TextView tvColorVariant = findViewById(R.id.tv_notice_color_variant);
        tvFrameName.setText(frameStyle + " Frame");
        tvColorVariant.setText("Color: " + colorVariant);

        TextView tvClinicName = findViewById(R.id.tv_notice_clinic_name);
        TextView tvClinicHours = findViewById(R.id.tv_notice_clinic_hours);

        FirestoreService.getClinicInformation(new FirestoreService.Callback<ClinicModel>() {
            @Override
            public void onSuccess(ClinicModel clinic) {
                if (isFinishing() || isDestroyed() || clinic == null) return;
                if (tvClinicName != null && clinic.getClinicName() != null && !clinic.getClinicName().isEmpty()) {
                    tvClinicName.setText(clinic.getClinicName());
                }
                if (tvClinicHours != null && clinic.getBusinessHours() != null && !clinic.getBusinessHours().isEmpty()) {
                    tvClinicHours.setText(clinic.getBusinessHours());
                }
            }

            @Override
            public void onError(String errorMessage) {
                // Keep default layout fallback
            }
        });

        MaterialButton btnProceed = findViewById(R.id.btn_proceed_to_catalog);
        btnProceed.setEnabled(false);

        MaterialCheckBox cbAgree = findViewById(R.id.cb_agree_notice);
        if (cbAgree != null) {
            cbAgree.setOnCheckedChangeListener((buttonView, isChecked) -> btnProceed.setEnabled(isChecked));
        }

        btnProceed.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("open_tab", R.id.nav_frame);
            intent.putExtra("selectedStyle", frameStyle);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        MaterialButton btnBackPreview = findViewById(R.id.btn_back_to_preview);
        btnBackPreview.setOnClickListener(v -> finish());
    }

    private String toCatalogStyleName(String raw) {
        if (raw == null) return "Wayfarer";
        String lower = raw.trim().toLowerCase();
        if (lower.contains("aviator")) return "Aviator";
        if (lower.contains("bowline") || lower.contains("browline")) return "Browline";
        if (lower.contains("cat")) return "Cat-Eye";
        if (lower.contains("geometric")) return "Geometric";
        if (lower.contains("oval")) return "Oval";
        if (lower.contains("rect")) return "Rectangle";
        if (lower.contains("round")) return "Round";
        if (lower.contains("square")) return "Square";
        return "Wayfarer";
    }

    private String formatColorVariant(String raw) {
        if (raw == null) return "Original Black";
        String lower = raw.trim().toLowerCase();
        if (lower.contains("gold")) return "Gold Accent";
        if (lower.contains("silver")) return "Silver Accent";
        return "Original Black";
    }
}
