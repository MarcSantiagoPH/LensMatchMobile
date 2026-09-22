package com.lensmatch.mobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.AppState;
import com.lensmatch.mobile.ui.camera.CameraActivity;
import com.lensmatch.mobile.ui.catalog.FrameCatalogFragment;
import com.lensmatch.mobile.ui.home.HomeFragment;
import com.lensmatch.mobile.ui.profile.ProfileFragment;
import com.lensmatch.mobile.ui.result.ResultFragment;
import com.lensmatch.mobile.utils.StatusBarUtils;

public class MainActivity extends AppCompatActivity {
    private static final String KEY_SELECTED_TAB = "selected_nav_item";
    private BottomNavigationView bottomNav;
    private final Fragment homeFragment = new HomeFragment();
    private final ResultFragment resultFragment = new ResultFragment();
    private final Fragment catalogFragment = new FrameCatalogFragment();
    private final Fragment profileFragment = new ProfileFragment();

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    navigateToResult();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View fragmentContainer = findViewById(R.id.fragment_container);
        StatusBarUtils.applyTopWindowInsets(fragmentContainer);

        androidx.core.view.WindowInsetsControllerCompat controller = 
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
            controller.setAppearanceLightNavigationBars(false);
        }
        getWindow().setNavigationBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.bottomBarBackground));

        bottomNav = findViewById(R.id.bottom_navigation);
        FloatingActionButton fabCamera = findViewById(R.id.fab_camera);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_placeholder) {
                return false;
            }
            selectTab(itemId);
            return true;
        });

        // Determine target tab:
        // 1. Intent extra takes highest priority (e.g. from LandingActivity swipe-up or notification)
        // 2. savedInstanceState (for recreation/orientation)
        // 3. AppState last active tab
        // 4. Default to R.id.nav_home
        int targetTab = R.id.nav_home;
        if (getIntent() != null && getIntent().hasExtra("open_tab")) {
            targetTab = getIntent().getIntExtra("open_tab", R.id.nav_home);
        } else if (savedInstanceState != null) {
            targetTab = savedInstanceState.getInt(KEY_SELECTED_TAB, R.id.nav_home);
        } else {
            int savedTab = AppState.getInstance().getLastActiveTab();
            if (savedTab != 0) {
                targetTab = savedTab;
            }
        }

        // CRITICAL FIX: Explicitly call selectTab(targetTab) BEFORE or REGARDLESS of setSelectedItemId().
        // When targetTab is nav_home (the default in bottom_nav_menu), bottomNav.setSelectedItemId(nav_home)
        // does NOT trigger the onItemSelectedListener because it is already marked selected by default.
        // Calling selectTab explicitly guarantees the fragment is immediately loaded and never leaves a blank screen.
        selectTab(targetTab);
        if (bottomNav.getSelectedItemId() != targetTab) {
            bottomNav.setSelectedItemId(targetTab);
        }

        handleIncomingIntent(getIntent());

        // Sync latest customer profile details and any pending scan records from Firestore
        com.lensmatch.mobile.service.FirestoreService.loadCustomerProfile(null);
        com.lensmatch.mobile.service.FirestoreService.syncPendingScans(null);

        fabCamera.setOnClickListener(v -> openCameraScan());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (bottomNav != null && bottomNav.getSelectedItemId() != R.id.nav_home) {
                    navigateToTab(R.id.nav_home);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        String style = null;
        if (intent.hasExtra("selectedStyle")) {
            style = intent.getStringExtra("selectedStyle");
        }
        if (style == null || style.trim().isEmpty()) {
            style = AppState.getInstance().getPendingCatalogStyle();
        }
        if (style != null && !style.trim().isEmpty()) {
            AppState.getInstance().setPendingCatalogStyle(style);
            if (catalogFragment instanceof FrameCatalogFragment) {
                ((FrameCatalogFragment) catalogFragment).setInitialStyle(style);
            }
        }
        if (intent.hasExtra("open_tab")) {
            int targetTab = intent.getIntExtra("open_tab", R.id.nav_home);
            navigateToTab(targetTab);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (bottomNav != null) {
            outState.putInt(KEY_SELECTED_TAB, bottomNav.getSelectedItemId());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) {
            updateStatusBarForTab(bottomNav.getSelectedItemId());
        }
        if (bottomNav != null && bottomNav.getSelectedItemId() == R.id.nav_result) {
            resultFragment.updateUI();
        }
    }

    public void openCameraScan() {
        Intent intent = new Intent(this, CameraActivity.class);
        cameraLauncher.launch(intent);
    }

    public void navigateToResult() {
        navigateToTab(R.id.nav_result);
        resultFragment.updateUI();
    }

    public void navigateToTab(int tabId) {
        selectTab(tabId);
        if (bottomNav != null && bottomNav.getSelectedItemId() != tabId) {
            bottomNav.setSelectedItemId(tabId);
        }
    }

    private int currentTab = -1;

    private void selectTab(int tabId) {
        if (tabId == R.id.nav_placeholder) {
            return;
        }

        updateStatusBarForTab(tabId);
        AppState.getInstance().setLastActiveTab(tabId);

        // If this tab is already active and fragment is present in container, avoid duplicate commit
        if (currentTab == tabId && getSupportFragmentManager().findFragmentById(R.id.fragment_container) != null) {
            if (tabId == R.id.nav_result) {
                resultFragment.updateUI();
            }
            return;
        }
        currentTab = tabId;

        Fragment fragment;
        if (tabId == R.id.nav_home) {
            fragment = homeFragment;
        } else if (tabId == R.id.nav_result) {
            fragment = resultFragment;
        } else if (tabId == R.id.nav_frame) {
            fragment = catalogFragment;
        } else if (tabId == R.id.nav_profile) {
            fragment = profileFragment;
        } else {
            fragment = homeFragment;
        }

        loadFragment(fragment);

        if (tabId == R.id.nav_result) {
            resultFragment.updateUI();
        } else if (tabId == R.id.nav_frame) {
            String pending = AppState.getInstance().getPendingCatalogStyle();
            if (pending != null && !pending.trim().isEmpty()) {
                if (catalogFragment instanceof FrameCatalogFragment) {
                    ((FrameCatalogFragment) catalogFragment).setInitialStyle(pending);
                }
            }
        }
    }

    private void loadFragment(Fragment fragment) {
        if (isFinishing() || isDestroyed()) return;
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commitAllowingStateLoss();
    }

    private void updateStatusBarForTab(int tabId) {
        View coordinator = findViewById(R.id.coordinator_main);
        androidx.core.view.WindowInsetsControllerCompat controller = 
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        int orangeColor = androidx.core.content.ContextCompat.getColor(this, R.color.primary_orange_dark);

        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(orangeColor);
        if (coordinator != null) {
            coordinator.setBackgroundColor(orangeColor);
        }
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
        }
    }
}
