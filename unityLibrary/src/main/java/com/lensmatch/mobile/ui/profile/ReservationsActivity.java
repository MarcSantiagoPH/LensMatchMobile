package com.lensmatch.mobile.ui.profile;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.AppState;
import com.lensmatch.mobile.ui.catalog.FrameAdapter;

public class ReservationsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservations);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_reservations);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rvReservations = findViewById(R.id.rv_reservations);
        rvReservations.setLayoutManager(new LinearLayoutManager(this));

        FrameAdapter adapter = new FrameAdapter(AppState.getInstance().getReservedFrames(), frame -> {});
        rvReservations.setAdapter(adapter);
    }
}
