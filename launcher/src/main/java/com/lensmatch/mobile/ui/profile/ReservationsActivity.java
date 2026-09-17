package com.lensmatch.mobile.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.ReservationModel;
import com.lensmatch.mobile.service.FirestoreService;
import com.lensmatch.mobile.utils.StatusBarUtils;

import java.util.ArrayList;
import java.util.List;

public class ReservationsActivity extends AppCompatActivity {
    private RecyclerView rvReservations;
    private ProgressBar progressReservations;
    private TextView tvEmptyReservations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservations);

        View root = findViewById(R.id.reservations_root);
        if (root != null) {
            StatusBarUtils.applyWindowInsets(root);
        }

        getWindow().setStatusBarColor(android.graphics.Color.WHITE);
        getWindow().setNavigationBarColor(android.graphics.Color.WHITE);
        androidx.core.view.WindowInsetsControllerCompat insetsController =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(true);
            insetsController.setAppearanceLightNavigationBars(true);
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar_reservations);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvReservations = findViewById(R.id.rv_reservations);
        progressReservations = findViewById(R.id.progress_reservations);
        tvEmptyReservations = findViewById(R.id.tv_empty_reservations);

        rvReservations.setLayoutManager(new LinearLayoutManager(this));

        loadReservations();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReservations();
    }

    private void loadReservations() {
        if (progressReservations != null) progressReservations.setVisibility(View.VISIBLE);
        if (tvEmptyReservations != null) tvEmptyReservations.setVisibility(View.GONE);

        FirestoreService.getUserReservations(new FirestoreService.Callback<List<ReservationModel>>() {
            @Override
            public void onSuccess(List<ReservationModel> list) {
                if (isFinishing() || isDestroyed()) return;
                if (progressReservations != null) progressReservations.setVisibility(View.GONE);

                if (list == null || list.isEmpty()) {
                    if (tvEmptyReservations != null) tvEmptyReservations.setVisibility(View.VISIBLE);
                    rvReservations.setAdapter(new ReservationAdapter(new ArrayList<>()));
                } else {
                    if (tvEmptyReservations != null) tvEmptyReservations.setVisibility(View.GONE);
                    rvReservations.setAdapter(new ReservationAdapter(list, item -> {
                        Intent intent = new Intent(ReservationsActivity.this, ReservationDetailActivity.class);
                        intent.putExtra("reservation", item);
                        startActivity(intent);
                    }));
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;
                if (progressReservations != null) progressReservations.setVisibility(View.GONE);
                if (tvEmptyReservations != null) {
                    tvEmptyReservations.setText("No active reservations.");
                    tvEmptyReservations.setVisibility(View.VISIBLE);
                }
                rvReservations.setAdapter(new ReservationAdapter(new ArrayList<>()));
            }
        });
    }
}
