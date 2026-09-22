package com.lensmatch.mobile.ui.guidelines;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lensmatch.mobile.R;

import java.util.List;

public class AppGuidelinesAdapter extends RecyclerView.Adapter<AppGuidelinesAdapter.SlideViewHolder> {
    private final List<GuidelineSlideModel> slides;

    public AppGuidelinesAdapter(List<GuidelineSlideModel> slides) {
        this.slides = slides;
    }

    @NonNull
    @Override
    public SlideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_guideline_slide, parent, false);
        return new SlideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SlideViewHolder holder, int position) {
        GuidelineSlideModel item = slides.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return slides != null ? slides.size() : 0;
    }

    static class SlideViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivIcon;
        private final TextView tvStepTag;
        private final TextView tvTitle;
        private final TextView tvSubtitle;
        private final TextView tvDescription;
        private final TextView tvTip;

        public SlideViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_guideline_icon);
            tvStepTag = itemView.findViewById(R.id.tv_guideline_step_tag);
            tvTitle = itemView.findViewById(R.id.tv_guideline_title);
            tvSubtitle = itemView.findViewById(R.id.tv_guideline_subtitle);
            tvDescription = itemView.findViewById(R.id.tv_guideline_description);
            tvTip = itemView.findViewById(R.id.tv_guideline_tip);
        }

        public void bind(GuidelineSlideModel item) {
            if (ivIcon != null) {
                ivIcon.setImageResource(item.getIconResId());
            }
            if (tvStepTag != null) {
                tvStepTag.setText(item.getStepTag());
            }
            if (tvTitle != null) {
                tvTitle.setText(item.getTitle());
            }
            if (tvSubtitle != null) {
                tvSubtitle.setText(item.getSubtitle());
            }
            if (tvDescription != null) {
                tvDescription.setText(item.getDescription());
            }
            if (tvTip != null) {
                tvTip.setText(item.getTip());
            }
        }
    }
}
