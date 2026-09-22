package com.lensmatch.mobile.ui.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.AnnouncementModel;
import com.lensmatch.mobile.service.FirestoreService;
import com.lensmatch.mobile.utils.StatusBarUtils;

import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private View layoutEmpty;
    private RecyclerView rvAnnouncements;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Status bar styling
        View root = findViewById(R.id.notifications_root);
        StatusBarUtils.applyWindowInsets(root);
        getWindow().setStatusBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary_orange_dark));
        getWindow().setNavigationBarColor(android.graphics.Color.WHITE);
        androidx.core.view.WindowInsetsControllerCompat insetsController =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(false);
            insetsController.setAppearanceLightNavigationBars(true);
        }

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar_notifications);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Views
        progressBar     = findViewById(R.id.progress_notifications);
        layoutEmpty     = findViewById(R.id.layout_empty_notifications);
        rvAnnouncements = findViewById(R.id.rv_announcements);
        swipeRefresh    = findViewById(R.id.swipe_refresh_notifications);

        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeColors(
                    androidx.core.content.ContextCompat.getColor(this, R.color.primary_orange),
                    androidx.core.content.ContextCompat.getColor(this, R.color.primary_orange_dark)
            );
            swipeRefresh.setOnRefreshListener(this::loadAnnouncements);
            swipeRefresh.setOnChildScrollUpCallback((parent, child) ->
                    rvAnnouncements != null && rvAnnouncements.getVisibility() == View.VISIBLE && rvAnnouncements.canScrollVertically(-1));
        }

        rvAnnouncements.setLayoutManager(new LinearLayoutManager(this));

        loadAnnouncements();
    }

    private void loadAnnouncements() {
        if (swipeRefresh == null || !swipeRefresh.isRefreshing()) {
            showLoading();
        }

        FirestoreService.getAnnouncements(new FirestoreService.Callback<List<AnnouncementModel>>() {
            @Override
            public void onSuccess(List<AnnouncementModel> result) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (result == null || result.isEmpty()) {
                    showEmpty();
                } else {
                    AnnouncementAdapter adapter = new AnnouncementAdapter(result);
                    rvAnnouncements.setAdapter(adapter);
                    showList();
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                // On error, show empty state so the screen isn't stuck loading
                showEmpty();
            }
        });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        rvAnnouncements.setVisibility(View.GONE);
    }

    private void showEmpty() {
        progressBar.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        rvAnnouncements.setVisibility(View.GONE);
    }

    private void showList() {
        progressBar.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        rvAnnouncements.setVisibility(View.VISIBLE);
    }
}
