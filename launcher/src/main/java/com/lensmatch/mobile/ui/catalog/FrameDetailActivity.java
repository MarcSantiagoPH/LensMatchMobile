package com.lensmatch.mobile.ui.catalog;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.AppState;
import com.lensmatch.mobile.data.FrameModel;
import com.lensmatch.mobile.service.FirestoreService;
import com.lensmatch.mobile.utils.StatusBarUtils;
import com.unity3d.player.UnityPlayerGameActivity;

public class FrameDetailActivity extends AppCompatActivity {
    private FrameModel frame;
    private MaterialButton btnReserveNow;
    private ImageView ivDetailImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frame_detail);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            frame = getIntent().getSerializableExtra("frame", FrameModel.class);
        } else {
            frame = (FrameModel) getIntent().getSerializableExtra("frame");
        }

        ImageView btnBack = findViewById(R.id.btn_back_detail);
        btnBack.setOnClickListener(v -> finish());
        StatusBarUtils.applyTopMargin(btnBack);

        ivDetailImage = findViewById(R.id.iv_detail_image);
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
            tvShapeBadge.setText(frame.getDisplayFrameStyle());

            String mat = frame.getMaterial();
            tvMaterial.setText(mat != null && !mat.trim().isEmpty() ? mat : "—");
            tvShapeVal.setText(frame.getDisplayFrameStyle());

            if (frame.getDescription() != null && !frame.getDescription().trim().isEmpty()) {
                tvDescription.setText(frame.getDescription());
            } else {
                String matDesc = mat != null && !mat.trim().isEmpty() ? mat.toLowerCase() : "materials";
                tvDescription.setText("Elevate your look with the " + frame.getName() + ". Crafted from premium "
                        + matDesc + ", these frames offer a perfect blend of durability and luxury style. Ideal for all-day comfort.");
            }

            if (ivDetailImage != null) {
                ivDetailImage.setImageTintList(null);
            }
            String imageUrl = frame.getImageUrl();
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_eyeglasses)
                        .error(R.drawable.ic_eyeglasses)
                        .into(ivDetailImage);
            } else {
                ivDetailImage.setImageResource(R.drawable.ic_eyeglasses);
            }

            if (!frame.isAvailable()) {
                btnReserveNow.setEnabled(false);
                btnReserveNow.setText("Unavailable");
            }
        }

        updateReserveButtonState();

        MaterialButton btnTryAR = findViewById(R.id.btn_try_detail_ar);
        btnTryAR.setOnClickListener(v -> {
            if (frame != null) {
                Intent intent = new Intent(this, UnityPlayerGameActivity.class);
                intent.putExtra("frameStyle", frame.getShape());
                startActivity(intent);
            }
        });

        btnReserveNow.setOnClickListener(v -> {
            if (frame == null) return;
            btnReserveNow.setEnabled(false);
            btnReserveNow.setText("Reserving...");

            FirestoreService.createReservation(frame, new FirestoreService.Callback<String>() {
                @Override
                public void onSuccess(String reservationId) {
                    Toast.makeText(FrameDetailActivity.this, frame.getName() + " reserved successfully!", Toast.LENGTH_LONG).show();
                    updateReserveButtonState();
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(FrameDetailActivity.this, errorMessage != null ? errorMessage : "Reservation failed.", Toast.LENGTH_LONG).show();
                    updateReserveButtonState();
                }
            });
        });
    }

    private void updateReserveButtonState() {
        if (frame != null) {
            if (!frame.isAvailable()) {
                btnReserveNow.setEnabled(false);
                btnReserveNow.setText("Unavailable");
            } else if (AppState.getInstance().isReserved(frame.getId())) {
                btnReserveNow.setEnabled(false);
                btnReserveNow.setText("Already Reserved");
            } else {
                btnReserveNow.setEnabled(true);
                btnReserveNow.setText("Reserve Now");
            }
        }
    }
}
