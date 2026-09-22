package com.lensmatch.mobile.ui.profile;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.ScanModel;

import java.io.File;
import java.util.List;

public class ScanHistoryAdapter extends RecyclerView.Adapter<ScanHistoryAdapter.ViewHolder> {

    public interface OnScanActionListener {
        void onViewResult(ScanModel scan);
        void onDeleteScan(ScanModel scan, int position);
    }

    private final List<ScanModel> scans;
    private final OnScanActionListener listener;

    public ScanHistoryAdapter(List<ScanModel> scans, OnScanActionListener listener) {
        this.scans = scans;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scan_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanModel scan = scans.get(position);

        holder.tvShape.setText(scan.getFaceShape() + " Face Shape");
        holder.tvDate.setText(scan.getFormattedDate());
        holder.tvConfidence.setText(scan.getConfidencePercent() + "% Match");
        holder.tvStyles.setText(scan.getFormattedStylesSummary());

        if (scan.getImagePath() != null && new File(scan.getImagePath()).exists()) {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;
                Bitmap bmp = BitmapFactory.decodeFile(scan.getImagePath(), options);
                if (bmp != null) {
                    holder.ivScanPhoto.setImageTintList(null);
                    holder.ivScanPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    holder.ivScanPhoto.setPadding(0, 0, 0, 0);
                    holder.ivScanPhoto.setImageBitmap(bmp);
                } else {
                    showDefaultScanIcon(holder.ivScanPhoto);
                }
            } catch (Throwable t) {
                showDefaultScanIcon(holder.ivScanPhoto);
            }
        } else {
            showDefaultScanIcon(holder.ivScanPhoto);
        }

        holder.btnViewResult.setOnClickListener(v -> {
            if (listener != null) listener.onViewResult(scan);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteScan(scan, holder.getAdapterPosition());
        });
    }

    private void showDefaultScanIcon(ImageView iv) {
        if (iv == null) return;
        int p = (int) (8 * iv.getContext().getResources().getDisplayMetrics().density);
        iv.setPadding(p, p, p, p);
        iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        iv.setImageResource(R.drawable.ic_scan);
        iv.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(iv.getContext(), R.color.secondary_teal)));
    }

    @Override
    public int getItemCount() {
        return scans != null ? scans.size() : 0;
    }

    public void removeItem(int position) {
        if (position >= 0 && position < scans.size()) {
            scans.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, scans.size());
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivScanPhoto;
        TextView tvShape;
        TextView tvDate;
        TextView tvConfidence;
        TextView tvStyles;
        MaterialButton btnViewResult;
        ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivScanPhoto = itemView.findViewById(R.id.iv_item_scan_photo);
            tvShape = itemView.findViewById(R.id.tv_item_scan_shape);
            tvDate = itemView.findViewById(R.id.tv_item_scan_date);
            tvConfidence = itemView.findViewById(R.id.tv_item_scan_confidence);
            tvStyles = itemView.findViewById(R.id.tv_item_scan_styles);
            btnViewResult = itemView.findViewById(R.id.btn_item_view_result);
            btnDelete = itemView.findViewById(R.id.btn_item_delete_scan);
        }
    }
}
