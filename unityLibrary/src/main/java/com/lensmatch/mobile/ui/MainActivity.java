package com.lensmatch.mobile.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.ui.camera.CameraActivity;
import com.lensmatch.mobile.ui.catalog.FrameCatalogFragment;
import com.lensmatch.mobile.ui.home.HomeFragment;
import com.lensmatch.mobile.ui.profile.ProfileFragment;
import com.lensmatch.mobile.ui.result.ResultFragment;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;
    private final Fragment homeFragment = new HomeFragment();
    private final ResultFragment resultFragment = new ResultFragment();
    private final Fragment catalogFragment = new FrameCatalogFragment();
    private final Fragment profileFragment = new ProfileFragment();

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    bottomNav.setSelectedItemId(R.id.nav_result);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);
        FloatingActionButton fabCamera = findViewById(R.id.fab_camera);

        loadFragment(homeFragment);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                loadFragment(homeFragment);
                return true;
            } else if (itemId == R.id.nav_result) {
                loadFragment(resultFragment);
                return true;
            } else if (itemId == R.id.nav_frame) {
                loadFragment(catalogFragment);
                return true;
            } else if (itemId == R.id.nav_profile) {
                loadFragment(profileFragment);
                return true;
            }
            return false;
        });

        fabCamera.setOnClickListener(v -> openCameraScan());
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
