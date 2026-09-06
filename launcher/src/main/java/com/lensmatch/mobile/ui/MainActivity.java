package com.lensmatch.mobile.ui;

import android.content.Intent;
import android.os.Bundle;

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

        bottomNav = findViewById(R.id.bottom_navigation);
        FloatingActionButton fabCamera = findViewById(R.id.fab_camera);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                loadFragment(homeFragment);
                AppState.getInstance().setLastActiveTab(R.id.nav_home);
                return true;
            } else if (itemId == R.id.nav_result) {
                loadFragment(resultFragment);
                AppState.getInstance().setLastActiveTab(R.id.nav_result);
                return true;
            } else if (itemId == R.id.nav_frame) {
                loadFragment(catalogFragment);
                AppState.getInstance().setLastActiveTab(R.id.nav_frame);
                return true;
            } else if (itemId == R.id.nav_profile) {
                loadFragment(profileFragment);
                AppState.getInstance().setLastActiveTab(R.id.nav_profile);
                return true;
            }
            return false;
        });

        int savedTab = AppState.getInstance().getLastActiveTab();
        if (savedInstanceState != null) {
            savedTab = savedInstanceState.getInt(KEY_SELECTED_TAB, savedTab);
        }
        int targetTab = (savedTab != 0) ? savedTab : R.id.nav_home;
        bottomNav.setSelectedItemId(targetTab);

        fabCamera.setOnClickListener(v -> openCameraScan());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (bottomNav != null && bottomNav.getSelectedItemId() != R.id.nav_home) {
                    bottomNav.setSelectedItemId(R.id.nav_home);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
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
        if (bottomNav != null && bottomNav.getSelectedItemId() == R.id.nav_result) {
            resultFragment.updateUI();
        }
    }

    public void openCameraScan() {
        Intent intent = new Intent(this, CameraActivity.class);
        cameraLauncher.launch(intent);
    }

    public void navigateToResult() {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_result);
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
