package com.lensmatch.mobile.ui.profile;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.AppState;
import com.lensmatch.mobile.data.ClinicModel;
import com.lensmatch.mobile.data.ReservationModel;
import com.lensmatch.mobile.service.FirestoreService;
import com.lensmatch.mobile.utils.StatusBarUtils;

import java.util.Date;

public class ReservationDetailActivity extends AppCompatActivity {
    private ReservationModel reservation;
    private MaterialButton btnCancelReservation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation_detail);

        View root = findViewById(R.id.reservation_detail_root);
        if (root != null) {
            StatusBarUtils.applyWindowInsets(root);
        }

        getWindow().setStatusBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary_orange_dark));
        getWindow().setNavigationBarColor(android.graphics.Color.WHITE);
        androidx.core.view.WindowInsetsControllerCompat insetsController =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(false);
            insetsController.setAppearanceLightNavigationBars(true);
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar_reservation_detail);
        toolbar.setNavigationOnClickListener(v -> finish());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            reservation = getIntent().getSerializableExtra("reservation", ReservationModel.class);
        } else {
            reservation = (ReservationModel) getIntent().getSerializableExtra("reservation");
        }

        if (reservation != null) {
            ImageView ivFrame = findViewById(R.id.iv_detail_frame_image);
            TextView tvFrameName = findViewById(R.id.tv_detail_frame_name);
            TextView tvBrandStyle = findViewById(R.id.tv_detail_brand_style);
            TextView tvPrice = findViewById(R.id.tv_detail_price);

            TextView tvStatus = findViewById(R.id.tv_detail_status);
            TextView tvResId = findViewById(R.id.tv_detail_res_id);
            TextView tvDate = findViewById(R.id.tv_detail_date);
            LinearLayout layoutUpdatedDate = findViewById(R.id.layout_updated_date);
            TextView tvUpdatedDate = findViewById(R.id.tv_detail_updated_date);
            TextView tvStatusMessage = findViewById(R.id.tv_detail_status_message);

            tvFrameName.setText(reservation.getFrameName());
            String brandStyle = (reservation.getBrand() != null && !reservation.getBrand().isEmpty() ? reservation.getBrand() : "LensMatch")
                    + " · " + (reservation.getFrameStyle() != null ? reservation.getFrameStyle() : "Standard");
            tvBrandStyle.setText(brandStyle);
            tvPrice.setText(reservation.getFormattedPrice());

            String rawId = reservation.getReservationId() != null ? reservation.getReservationId() : "";
            String shortId = rawId.length() > 8 ? rawId.substring(0, 8).toUpperCase() : rawId.toUpperCase();
            tvResId.setText("#" + shortId);

            tvDate.setText(reservation.getFormattedDate());

            if (reservation.getFormattedUpdatedDate() != null) {
                if (layoutUpdatedDate != null) layoutUpdatedDate.setVisibility(View.VISIBLE);
                if (tvUpdatedDate != null) tvUpdatedDate.setText(reservation.getFormattedUpdatedDate());
            } else {
                if (layoutUpdatedDate != null) layoutUpdatedDate.setVisibility(View.GONE);
            }

            String status = reservation.getStatus() != null ? reservation.getStatus() : "Pending";
            tvStatus.setText(status);

            String statusUpper = status.trim().toUpperCase();
            if ("APPROVED".equals(statusUpper) || "CONFIRMED".equals(statusUpper)) {
                tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                tvStatusMessage.setText("Your reservation has been approved!\n\nPlease contact the clinic using the information below so our staff can assist you with the next steps of your reservation.");
            } else if ("CANCELLED".equals(statusUpper)) {
                tvStatus.setTextColor(Color.parseColor("#F44336"));
                tvStatusMessage.setText("You have cancelled this reservation.\n\nPlease contact the clinic if you have questions or would like to reserve a different frame.");
            } else if ("REJECTED".equals(statusUpper)) {
                tvStatus.setTextColor(Color.parseColor("#F44336"));
                tvStatusMessage.setText("Unfortunately, your reservation request was not approved.\n\nPlease contact the clinic if you have questions or would like to reserve a different frame.");
            } else if ("COMPLETED".equals(statusUpper)) {
                tvStatus.setTextColor(Color.parseColor("#2196F3"));
                tvStatusMessage.setText("Your reservation has been completed. Thank you for choosing Franselle Optical Clinic!");
            } else { // Pending
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.primary_orange));
                tvStatusMessage.setText("Your reservation request has been received.\n\nOur staff is currently reviewing your request. We'll update the status once the review is complete.");
            }

            TextView tvSpecialInstructions = findViewById(R.id.tv_special_instructions);
            if (tvSpecialInstructions != null) {
                if ("APPROVED".equals(statusUpper)) {
                    tvSpecialInstructions.setText(R.string.instruction_approved);
                } else if ("CANCELLED".equals(statusUpper)) {
                    tvSpecialInstructions.setText(R.string.instruction_cancelled);
                } else if ("REJECTED".equals(statusUpper)) {
                    tvSpecialInstructions.setText(R.string.instruction_rejected);
                } else { // PENDING or default
                    tvSpecialInstructions.setText(R.string.instruction_pending);
                }
            }

            if (ivFrame != null) {
                ivFrame.setImageTintList(null);
                if (reservation.getImageUrl() != null && !reservation.getImageUrl().trim().isEmpty()) {
                    Glide.with(this)
                            .load(reservation.getImageUrl())
                            .placeholder(R.drawable.ic_eyeglasses)
                            .error(R.drawable.ic_eyeglasses)
                            .into(ivFrame);
                } else {
                    ivFrame.setImageResource(R.drawable.ic_eyeglasses);
                }
            }

            LinearLayout layoutCancellationReason = findViewById(R.id.layout_cancellation_reason);
            TextView tvCancellationReason = findViewById(R.id.tv_detail_cancellation_reason);
            if ("CANCELLED".equals(statusUpper) && reservation.getCancellationReason() != null && !reservation.getCancellationReason().trim().isEmpty()) {
                if (layoutCancellationReason != null) layoutCancellationReason.setVisibility(View.VISIBLE);
                if (tvCancellationReason != null) tvCancellationReason.setText(reservation.getCancellationReason());
            } else {
                if (layoutCancellationReason != null) layoutCancellationReason.setVisibility(View.GONE);
            }

            btnCancelReservation = findViewById(R.id.btn_cancel_reservation);
            updateCancelButtonVisibility();
            if (btnCancelReservation != null) {
                btnCancelReservation.setOnClickListener(v -> confirmCancelReservation());
            }
        }

        loadClinicInformation();
    }

    private void updateCancelButtonVisibility() {
        if (btnCancelReservation == null || reservation == null) return;
        String status = reservation.getStatus() != null ? reservation.getStatus().trim().toUpperCase() : "PENDING";
        boolean canCancel = "PENDING".equals(status) || "APPROVED".equals(status) || "CONFIRMED".equals(status);
        btnCancelReservation.setVisibility(canCancel ? View.VISIBLE : View.GONE);
    }

    private void confirmCancelReservation() {
        if (reservation == null || reservation.getReservationId() == null) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cancel_reservation, null);
        TextView tvSubtitle = dialogView.findViewById(R.id.tv_cancel_dialog_subtitle);
        RadioGroup rgReasons = dialogView.findViewById(R.id.rg_cancel_reasons);
        TextInputLayout tilNotes = dialogView.findViewById(R.id.til_cancel_reason_notes);
        TextInputEditText etNotes = dialogView.findViewById(R.id.et_cancel_reason_notes);

        String frameName = reservation.getFrameName() != null ? reservation.getFrameName() : "this frame";
        if (tvSubtitle != null) {
            tvSubtitle.setText("Please select a reason for cancelling your reservation for \"" + frameName + "\":");
        }

        if (rgReasons != null && tilNotes != null) {
            rgReasons.setOnCheckedChangeListener((group, checkedId) -> {
                tilNotes.setError(null);
                if (checkedId == R.id.rb_reason_other) {
                    tilNotes.setHint("Please specify your reason (required)");
                } else {
                    tilNotes.setHint("Additional details (optional)");
                }
            });
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Cancel Reservation")
                .setIcon(R.drawable.ic_close)
                .setView(dialogView)
                .setPositiveButton("Cancel Reservation", null)
                .setNegativeButton("Keep Reservation", (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            View positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton instanceof TextView) {
                ((TextView) positiveButton).setTextColor(Color.parseColor("#D32F2F"));
            }
            if (positiveButton != null) {
                positiveButton.setOnClickListener(v -> {
                    int checkedId = rgReasons != null ? rgReasons.getCheckedRadioButtonId() : -1;
                    String notes = etNotes != null && etNotes.getText() != null ? etNotes.getText().toString().trim() : "";

                    if (checkedId == R.id.rb_reason_other) {
                        if (notes.isEmpty()) {
                            if (tilNotes != null) {
                                tilNotes.setError("Please specify the reason for cancellation");
                            }
                            return;
                        }
                    }

                    String reasonText;
                    if (checkedId == R.id.rb_reason_mind) {
                        reasonText = "Changed my mind";
                    } else if (checkedId == R.id.rb_reason_frame) {
                        reasonText = "Found another frame I prefer";
                    } else if (checkedId == R.id.rb_reason_schedule) {
                        reasonText = "Unable to visit clinic / Schedule conflict";
                    } else if (checkedId == R.id.rb_reason_mistake) {
                        reasonText = "Reserved by mistake";
                    } else if (checkedId == R.id.rb_reason_budget) {
                        reasonText = "Price or budget considerations";
                    } else if (checkedId == R.id.rb_reason_other) {
                        reasonText = "Other: " + notes;
                    } else {
                        reasonText = "Customer requested cancellation";
                    }

                    if (checkedId != R.id.rb_reason_other && !notes.isEmpty()) {
                        reasonText += " (" + notes + ")";
                    }

                    dialog.dismiss();
                    executeCancelReservation(reasonText);
                });
            }
        });

        dialog.show();
    }

    private void executeCancelReservation(String cancellationReason) {
        if (reservation == null || reservation.getReservationId() == null) return;

        if (btnCancelReservation != null) {
            btnCancelReservation.setEnabled(false);
            btnCancelReservation.setText("Cancelling...");
        }

        FirestoreService.cancelReservation(reservation.getReservationId(), cancellationReason, new FirestoreService.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (isFinishing() || isDestroyed()) return;

                if (reservation.getFrameId() != null) {
                    AppState.getInstance().removeReservation(reservation.getFrameId());
                }

                reservation.setStatus("Cancelled");
                reservation.setCancellationReason(cancellationReason);
                reservation.setStatusUpdatedAt(new Date());

                TextView tvStatus = findViewById(R.id.tv_detail_status);
                TextView tvStatusMessage = findViewById(R.id.tv_detail_status_message);
                TextView tvSpecialInstructions = findViewById(R.id.tv_special_instructions);
                LinearLayout layoutUpdatedDate = findViewById(R.id.layout_updated_date);
                TextView tvUpdatedDate = findViewById(R.id.tv_detail_updated_date);
                LinearLayout layoutCancellationReason = findViewById(R.id.layout_cancellation_reason);
                TextView tvCancellationReason = findViewById(R.id.tv_detail_cancellation_reason);

                if (tvStatus != null) {
                    tvStatus.setText("Cancelled");
                    tvStatus.setTextColor(Color.parseColor("#F44336"));
                }
                if (tvStatusMessage != null) {
                    tvStatusMessage.setText("You have cancelled this reservation.\n\nPlease contact the clinic if you have questions or would like to reserve a different frame.");
                }
                if (tvSpecialInstructions != null) {
                    tvSpecialInstructions.setText(R.string.instruction_cancelled);
                }
                if (layoutUpdatedDate != null && tvUpdatedDate != null) {
                    layoutUpdatedDate.setVisibility(View.VISIBLE);
                    tvUpdatedDate.setText(reservation.getFormattedUpdatedDate());
                }
                if (layoutCancellationReason != null && tvCancellationReason != null && cancellationReason != null && !cancellationReason.trim().isEmpty()) {
                    layoutCancellationReason.setVisibility(View.VISIBLE);
                    tvCancellationReason.setText(cancellationReason);
                }

                updateCancelButtonVisibility();

                Toast.makeText(ReservationDetailActivity.this, "Reservation cancelled successfully.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;

                if (btnCancelReservation != null) {
                    btnCancelReservation.setEnabled(true);
                    btnCancelReservation.setText("Cancel Reservation");
                }

                Toast.makeText(ReservationDetailActivity.this,
                        errorMessage != null ? errorMessage : "Failed to cancel reservation.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadClinicInformation() {
        TextView tvClinicName = findViewById(R.id.tv_clinic_name);
        LinearLayout layoutPhone = findViewById(R.id.layout_clinic_phone);
        TextView tvPhone = findViewById(R.id.tv_clinic_phone);

        LinearLayout layoutEmail = findViewById(R.id.layout_clinic_email);
        TextView tvEmail = findViewById(R.id.tv_clinic_email);

        LinearLayout layoutAddress = findViewById(R.id.layout_clinic_address);
        TextView tvAddress = findViewById(R.id.tv_clinic_address);

        LinearLayout layoutHours = findViewById(R.id.layout_clinic_hours);
        TextView tvHours = findViewById(R.id.tv_clinic_hours);

        FirestoreService.getClinicInformation(new FirestoreService.Callback<ClinicModel>() {
            @Override
            public void onSuccess(ClinicModel clinic) {
                if (isFinishing() || isDestroyed()) return;

                if (tvClinicName != null && clinic.getClinicName() != null) {
                    tvClinicName.setText(clinic.getClinicName());
                }

                String phone = clinic.getContactNumber();
                if (phone != null && !phone.isEmpty()) {
                    if (layoutPhone != null) layoutPhone.setVisibility(View.VISIBLE);
                    if (tvPhone != null) {
                        tvPhone.setText(phone);
                        tvPhone.setOnClickListener(v -> {
                            try {
                                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                                startActivity(intent);
                            } catch (Exception ignored) {}
                        });
                    }
                } else {
                    if (layoutPhone != null) layoutPhone.setVisibility(View.GONE);
                }

                String email = clinic.getEmail();
                if (email != null && !email.isEmpty()) {
                    if (layoutEmail != null) layoutEmail.setVisibility(View.VISIBLE);
                    if (tvEmail != null) {
                        tvEmail.setText(email);
                        tvEmail.setOnClickListener(v -> {
                            try {
                                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email));
                                startActivity(intent);
                            } catch (Exception ignored) {}
                        });
                    }
                } else {
                    if (layoutEmail != null) layoutEmail.setVisibility(View.GONE);
                }

                String address = clinic.getAddress();
                if (address != null && !address.isEmpty()) {
                    if (layoutAddress != null) layoutAddress.setVisibility(View.VISIBLE);
                    if (tvAddress != null) {
                        tvAddress.setText(address);
                        tvAddress.setOnClickListener(v -> {
                            try {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(address)));
                                startActivity(intent);
                            } catch (Exception ignored) {}
                        });
                    }
                } else {
                    if (layoutAddress != null) layoutAddress.setVisibility(View.GONE);
                }

                String hours = clinic.getBusinessHours();
                if (hours != null && !hours.isEmpty()) {
                    if (layoutHours != null) layoutHours.setVisibility(View.VISIBLE);
                    if (tvHours != null) tvHours.setText(hours);
                } else {
                    if (layoutHours != null) layoutHours.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String errorMessage) {
                // Keep default layout state
            }
        });
    }
}
