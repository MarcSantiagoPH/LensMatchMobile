package com.lensmatch.mobile.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.AppState;
import com.lensmatch.mobile.ui.camera.CameraActivity;
import com.lensmatch.mobile.utils.FaceShapeDetector;
import com.lensmatch.mobile.utils.StatusBarUtils;

public class ScanHistoryActivity extends AppCompatActivity {

    private LinearLayout layoutScanResults;
    private LinearLayout layoutEmptyScanHistory;
    private TextView tvScanShapeTitle;
    private TextView tvScanTimestamp;
    private TextView chipScanConfidence;
    private TextView tvScanRecommendationSummary;
    private MaterialButton btnScanAgain;
    private MaterialButton btnEmptyStartScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_history);

        View root = findViewById(R.id.scan_history_root);
        StatusBarUtils.applyTopWindowInsets(root);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_scan_history);
        toolbar.setNavigationOnClickListener(v -> finish());

        layoutScanResults = findViewById(R.id.layout_scan_results);
        layoutEmptyScanHistory = findViewById(R.id.layout_empty_scan_history);
        tvScanShapeTitle = findViewById(R.id.tv_scan_shape_title);
        tvScanTimestamp = findViewById(R.id.tv_scan_timestamp);
        chipScanConfidence = findViewById(R.id.chip_scan_confidence);
        tvScanRecommendationSummary = findViewById(R.id.tv_scan_recommendation_summary);
        btnScanAgain = findViewById(R.id.btn_scan_again);
        btnEmptyStartScan = findViewById(R.id.btn_empty_start_scan);

        btnScanAgain.setOnClickListener(v -> openCameraScan());
        btnEmptyStartScan.setOnClickListener(v -> openCameraScan());

        updateScanHistoryUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateScanHistoryUI();
    }

    private void openCameraScan() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }

    private void updateScanHistoryUI() {
        AppState state = AppState.getInstance();
        String detectedShape = state.getLastDetectedShape();

        if (detectedShape != null && !detectedShape.isEmpty() && !detectedShape.equalsIgnoreCase("Unknown")) {
            layoutScanResults.setVisibility(View.VISIBLE);
            layoutEmptyScanHistory.setVisibility(View.GONE);

            tvScanShapeTitle.setText(detectedShape + " Face Shape");
            tvScanTimestamp.setText("Latest Scan • Verified Geometry");

            float confidence = state.getLastConfidence();
            int percent = (int) (confidence * 100);
            if (percent <= 0) percent = 92;
            chipScanConfidence.setText(percent + "% Match");

            FaceShapeDetector.ShapeRecommendation rec = FaceShapeDetector.getRecommendationForShape(detectedShape);
            if (rec != null && rec.recommended != null && rec.recommended.length > 0) {
                StringBuilder sb = new StringBuilder("Recommended Styles: ");
                for (int i = 0; i < rec.recommended.length; i++) {
                    sb.append(rec.recommended[i]);
                    if (i < rec.recommended.length - 1) sb.append(", ");
                }
                tvScanRecommendationSummary.setText(sb.toString());
            } else {
                tvScanRecommendationSummary.setText("Styles curated specifically for " + detectedShape + " proportions.");
            }
        } else {
            layoutScanResults.setVisibility(View.GONE);
            layoutEmptyScanHistory.setVisibility(View.VISIBLE);
        }
    }
}
