package com.lensmatch.mobile.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.AppState;
import com.lensmatch.mobile.data.ScanModel;
import com.lensmatch.mobile.service.FirestoreService;
import com.lensmatch.mobile.ui.MainActivity;
import com.lensmatch.mobile.ui.camera.CameraActivity;
import com.lensmatch.mobile.utils.FaceShapeDetector;
import com.lensmatch.mobile.utils.StatusBarUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ScanHistoryActivity extends AppCompatActivity {

    private RecyclerView rvScanHistory;
    private ProgressBar progressScanHistory;
    private LinearLayout layoutEmptyScanHistory;
    private TextView tvScanCount;
    private MaterialButton btnScanAgain;
    private MaterialButton btnEmptyStartScan;

    private ScanHistoryAdapter adapter;
    private final List<ScanModel> scanList = new ArrayList<>();
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_history);

        View root = findViewById(R.id.scan_history_root);
        StatusBarUtils.applyWindowInsets(root);

        getWindow().setStatusBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary_orange_dark));
        getWindow().setNavigationBarColor(android.graphics.Color.WHITE);
        androidx.core.view.WindowInsetsControllerCompat insetsController =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(false);
            insetsController.setAppearanceLightNavigationBars(true);
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar_scan_history);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvScanHistory = findViewById(R.id.rv_scan_history);
        progressScanHistory = findViewById(R.id.progress_scan_history);
        layoutEmptyScanHistory = findViewById(R.id.layout_empty_scan_history);
        tvScanCount = findViewById(R.id.tv_scan_count);
        btnScanAgain = findViewById(R.id.btn_scan_again);
        btnEmptyStartScan = findViewById(R.id.btn_empty_start_scan);
        swipeRefresh = findViewById(R.id.swipe_refresh_scan_history);

        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeColors(
                    androidx.core.content.ContextCompat.getColor(this, R.color.primary_orange),
                    androidx.core.content.ContextCompat.getColor(this, R.color.primary_orange_dark)
            );
            swipeRefresh.setOnRefreshListener(this::loadScanHistory);
            swipeRefresh.setOnChildScrollUpCallback((parent, child) ->
                    rvScanHistory != null && rvScanHistory.getVisibility() == View.VISIBLE && rvScanHistory.canScrollVertically(-1));
        }

        rvScanHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScanHistoryAdapter(scanList, new ScanHistoryAdapter.OnScanActionListener() {
            @Override
            public void onViewResult(ScanModel scan) {
                applyScanAsActiveResult(scan);
            }

            @Override
            public void onDeleteScan(ScanModel scan, int position) {
                confirmDeleteScan(scan, position);
            }
        });
        rvScanHistory.setAdapter(adapter);

        btnScanAgain.setOnClickListener(v -> openCameraScan());
        btnEmptyStartScan.setOnClickListener(v -> openCameraScan());

        loadScanHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadScanHistory();
    }

    private void openCameraScan() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }

    private void loadScanHistory() {
        // 0. Ensure any un-indexed previous scan in AppState is recovered
        AppState.getInstance().restoreLastScanIfEmpty();

        // 1. Immediately load all local scans from persistent AppState
        List<ScanModel> localScans = AppState.getInstance().getScanHistory();
        scanList.clear();
        scanList.addAll(localScans);
        if (adapter != null) adapter.notifyDataSetChanged();
        updateUIState();

        // 2. Sync any pending un-uploaded local scans to Firestore, then refresh from cloud
        FirestoreService.syncPendingScans(new FirestoreService.Callback<Integer>() {
            @Override
            public void onSuccess(Integer count) {
                fetchCloudScanHistory();
            }

            @Override
            public void onError(String errorMessage) {
                fetchCloudScanHistory();
            }
        });
    }

    private void fetchCloudScanHistory() {
        FirestoreService.getUserScanHistory(new FirestoreService.Callback<List<ScanModel>>() {
            @Override
            public void onSuccess(List<ScanModel> result) {
                if (isFinishing() || isDestroyed()) return;
                if (result != null && !result.isEmpty()) {
                    AppState.getInstance().syncScanHistory(result);
                    scanList.clear();
                    scanList.addAll(AppState.getInstance().getScanHistory());
                    if (adapter != null) adapter.notifyDataSetChanged();
                }
                updateUIState();
            }

            @Override
            public void onError(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;
                // Keep local scans safe and displayed — do not wipe or reset!
                updateUIState();
            }
        });
    }

    private void updateUIState() {
        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
        if (progressScanHistory != null) progressScanHistory.setVisibility(View.GONE);
        if (scanList.isEmpty()) {
            if (rvScanHistory != null) rvScanHistory.setVisibility(View.GONE);
            if (layoutEmptyScanHistory != null) layoutEmptyScanHistory.setVisibility(View.VISIBLE);
            if (tvScanCount != null) tvScanCount.setText("0 Scans");
        } else {
            if (rvScanHistory != null) rvScanHistory.setVisibility(View.VISIBLE);
            if (layoutEmptyScanHistory != null) layoutEmptyScanHistory.setVisibility(View.GONE);
            if (tvScanCount != null) {
                int count = scanList.size();
                tvScanCount.setText(count + (count == 1 ? " Scan Saved" : " Scans Saved"));
            }
        }
    }

    private void applyScanAsActiveResult(ScanModel scan) {
        AppState state = AppState.getInstance();
        state.setLastDetectedShape(scan.getFaceShape());
        state.setLastConfidence(scan.getConfidence());
        state.setLastIsBorderline(scan.isBorderline());
        state.setLastRunnerUpShape(scan.getRunnerUpShape());
        state.setLastNotes(scan.getNotes());

        if (scan.getImagePath() != null && new java.io.File(scan.getImagePath()).exists()) {
            state.setLastImagePath(scan.getImagePath());
        } else if (scan.getPhotoBase64() != null && !scan.getPhotoBase64().isEmpty()) {
            try {
                java.io.File scansDir = new java.io.File(getFilesDir(), "scans");
                if (!scansDir.exists()) scansDir.mkdirs();
                java.io.File restoredFile = new java.io.File(scansDir, "restored_scan_" + (scan.getId() != null ? scan.getId() : System.currentTimeMillis()) + ".jpg");
                byte[] bytes = android.util.Base64.decode(scan.getPhotoBase64(), android.util.Base64.DEFAULT);
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(restoredFile)) {
                    fos.write(bytes);
                }
                scan.setImagePath(restoredFile.getAbsolutePath());
                state.setLastImagePath(restoredFile.getAbsolutePath());
            } catch (Exception ignored) {}
        }

        state.setLastActiveTab(R.id.nav_result);

        Toast.makeText(this, "Loaded " + scan.getFaceShape() + " recommendations", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("open_tab", R.id.nav_result);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void confirmDeleteScan(ScanModel scan, int position) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Scan Record")
                .setMessage("Are you sure you want to remove this " + scan.getFaceShape() + " face scan from your history?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Remove from persistent local storage
                    AppState.getInstance().removeScan(scan.getId());

                    if (adapter != null) {
                        adapter.removeItem(position);
                    }
                    updateUIState();

                    // Also try deleting photo file if present
                    if (scan.getImagePath() != null) {
                        try {
                            java.io.File file = new java.io.File(scan.getImagePath());
                            if (file.exists()) file.delete();
                        } catch (Exception ignored) {}
                    }

                    // Delete from Firestore if it has a remote ID
                    if (scan.getId() != null && !scan.getId().startsWith("scan_")) {
                        FirestoreService.deleteScanHistoryItem(scan.getId(), new FirestoreService.Callback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                // Deleted from cloud
                            }

                            @Override
                            public void onError(String errorMessage) {
                                // Cloud delete failed, local already removed
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
