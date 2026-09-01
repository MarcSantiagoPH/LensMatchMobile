package com.lensmatch.mobile.ui.profile;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.lensmatch.mobile.R;

public class AccountDetailsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservations);
        MaterialToolbar toolbar = findViewById(R.id.toolbar_reservations);
        toolbar.setTitle("Account Details");
        toolbar.setNavigationOnClickListener(v -> finish());
    }
}
