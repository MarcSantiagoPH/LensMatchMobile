package com.lensmatch.mobile.ui.catalog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        holder.tvName.setText(frame.getName());
        holder.tvShapeBadge.setText(frame.getShape());
        holder.tvMaterial.setText(frame.getMaterial());
        holder.tvPrice.setText(frame.getPrice());

        holder.itemView.setOnClickListener(v -> listener.onItemClick(frame));
    }

    @Override
    public int getItemCount() {
        return frames.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvShapeBadge, tvMaterial, tvPrice;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_frame_name);
            tvShapeBadge = itemView.findViewById(R.id.tv_shape_badge);
            tvMaterial = itemView.findViewById(R.id.tv_frame_material);
            tvPrice = itemView.findViewById(R.id.tv_frame_price);
        }
    }
}
