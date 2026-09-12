package com.lensmatch.mobile.ui.catalog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.FrameModel;

import java.util.List;

public class FrameAdapter extends RecyclerView.Adapter<FrameAdapter.ViewHolder> {
    private final List<FrameModel> frames;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(FrameModel frame);
    }

    public FrameAdapter(List<FrameModel> frames, OnItemClickListener listener) {
        this.frames = frames;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_frame_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FrameModel frame = frames.get(position);
        Context context = holder.itemView.getContext();

        holder.tvName.setText(frame.getName());
        holder.tvShapeBadge.setText(frame.getDisplayFrameStyle());
        String mat = frame.getMaterial();
        holder.tvMaterial.setText(mat != null && !mat.trim().isEmpty() ? mat : "—");
        holder.tvPrice.setText(frame.getPrice());

        if (!frame.isAvailable()) {
            holder.tvShapeBadge.setText("Unavailable");
            holder.tvShapeBadge.setTextColor(context.getColor(R.color.textSecondary));
            holder.itemView.setAlpha(0.6f);
        } else {
            holder.tvShapeBadge.setTextColor(context.getColor(R.color.accentGold));
            holder.itemView.setAlpha(1.0f);
        }

        holder.ivFrame.setImageTintList(null);
        String imageUrl = frame.getImageUrl();
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_eyeglasses)
                    .error(R.drawable.ic_eyeglasses)
                    .into(holder.ivFrame);
        } else {
            holder.ivFrame.setImageResource(R.drawable.ic_eyeglasses);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(frame));
    }

    @Override
    public int getItemCount() {
        return frames != null ? frames.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFrame;
        TextView tvName, tvShapeBadge, tvMaterial, tvPrice;

        ViewHolder(View itemView) {
            super(itemView);
            ivFrame = itemView.findViewById(R.id.iv_frame);
            tvName = itemView.findViewById(R.id.tv_frame_name);
            tvShapeBadge = itemView.findViewById(R.id.tv_shape_badge);
            tvMaterial = itemView.findViewById(R.id.tv_frame_material);
            tvPrice = itemView.findViewById(R.id.tv_frame_price);
        }
    }
}
