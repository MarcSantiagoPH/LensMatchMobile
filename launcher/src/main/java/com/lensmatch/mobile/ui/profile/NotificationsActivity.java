package com.lensmatch.mobile.ui.profile;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.utils.StatusBarUtils;

public class NotificationsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        View root = findViewById(R.id.notifications_root);
        StatusBarUtils.applyTopWindowInsets(root);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_notifications);
        toolbar.setNavigationOnClickListener(v -> finish());
    }
}
