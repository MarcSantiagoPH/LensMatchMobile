package com.lensmatch.mobile.ui.profile;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.ReservationModel;

import java.util.List;

public class ReservationAdapter extends RecyclerView.Adapter<ReservationAdapter.ViewHolder> {
    private final List<ReservationModel> reservations;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ReservationModel item);
    }

    public ReservationAdapter(List<ReservationModel> reservations, OnItemClickListener listener) {
        this.reservations = reservations;
        this.listener = listener;
    }

    public ReservationAdapter(List<ReservationModel> reservations) {
        this(reservations, null);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reservation_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReservationModel item = reservations.get(position);
        Context context = holder.itemView.getContext();

        holder.tvFrameName.setText(item.getFrameName());
        String brandStyle = (item.getBrand() != null && !item.getBrand().isEmpty() ? item.getBrand() : "LensMatch")
                + " · " + (item.getFrameStyle() != null ? item.getFrameStyle() : "Standard");
        holder.tvBrandStyle.setText(brandStyle);
        holder.tvPrice.setText(item.getFormattedPrice());
        holder.tvDate.setText(item.getFormattedDate());

        String status = item.getStatus();
        holder.tvStatus.setText(status);

        if ("Confirmed".equalsIgnoreCase(status) || "Approved".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
        } else if ("Completed".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#2196F3"));
        } else if ("Cancelled".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#F44336"));
        } else {
            holder.tvStatus.setTextColor(context.getColor(R.color.accentGold));
        }

        if (item.getImageUrl() != null && !item.getImageUrl().trim().isEmpty()) {
            holder.ivImage.setImageTintList(null);
            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_eyeglasses)
                    .error(R.drawable.ic_eyeglasses)
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(R.drawable.ic_eyeglasses);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return reservations != null ? reservations.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvFrameName;
        TextView tvBrandStyle;
        TextView tvPrice;
        TextView tvStatus;
        TextView tvDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_res_image);
            tvFrameName = itemView.findViewById(R.id.tv_res_frame_name);
            tvBrandStyle = itemView.findViewById(R.id.tv_res_brand_style);
            tvPrice = itemView.findViewById(R.id.tv_res_price);
            tvStatus = itemView.findViewById(R.id.tv_res_status);
            tvDate = itemView.findViewById(R.id.tv_res_date);
        }
    }
}
