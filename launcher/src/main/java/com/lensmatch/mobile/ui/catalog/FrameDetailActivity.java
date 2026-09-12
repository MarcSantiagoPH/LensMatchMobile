package com.lensmatch.mobile.ui.catalog;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.AppState;
import com.lensmatch.mobile.data.FrameModel;
import com.lensmatch.mobile.utils.StatusBarUtils;

public class FrameDetailActivity extends AppCompatActivity {
    private FrameModel frame;
    private MaterialButton btnReserveNow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frame_detail);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            frame = getIntent().getSerializableExtra("frame", FrameModel.class);
        } else {
            frame = (FrameModel) getIntent().getSerializableExtra("frame");
        }

        ImageView btnBack = findViewById(R.id.btn_back_detail);
        btnBack.setOnClickListener(v -> finish());
        StatusBarUtils.applyTopMargin(btnBack);

        TextView tvName = findViewById(R.id.tv_detail_name);
        TextView tvPrice = findViewById(R.id.tv_detail_price);
        TextView tvShapeBadge = findViewById(R.id.tv_detail_shape_badge);
        TextView tvMaterial = findViewById(R.id.tv_detail_material);
        TextView tvShapeVal = findViewById(R.id.tv_detail_shape_val);
        TextView tvDescription = findViewById(R.id.tv_detail_description);
        btnReserveNow = findViewById(R.id.btn_reserve_now);

        if (frame != null) {
            tvName.setText(frame.getName());
            tvPrice.setText(frame.getPrice());
            tvShapeBadge.setText(frame.getShape());
            tvMaterial.setText(frame.getMaterial());
            tvShapeVal.setText(frame.getShape());
            tvDescription.setText("Elevate your look with the " + frame.getName() + ". Crafted from premium "
                    + frame.getMaterial().toLowerCase() + ", these frames offer a perfect blend of durability and luxury style. Ideal for all-day comfort.");
        }

        updateReserveButtonState();

        MaterialButton btnTryAR = findViewById(R.id.btn_try_detail_ar);
        btnTryAR.setOnClickListener(v -> {
            if (frame != null) {
                Intent intent = new Intent(this, com.unity3d.player.UnityPlayerGameActivity.class);
                intent.putExtra("frameStyle", frame.getShape());
                startActivity(intent);
            }
        });

        btnReserveNow.setOnClickListener(v -> {
            if (frame != null) {
                AppState.getInstance().addReservation(frame);
                Toast.makeText(this, frame.getName() + " reserved successfully!", Toast.LENGTH_SHORT).show();
                updateReserveButtonState();
                finish();
            }
        });
    }

    private void updateReserveButtonState() {
        if (frame != null && AppState.getInstance().isReserved(frame.getId())) {
            btnReserveNow.setEnabled(false);
            btnReserveNow.setText("Already Reserved");
        } else {
            btnReserveNow.setEnabled(true);
            btnReserveNow.setText("Reserve Now");
        }
    }
}
