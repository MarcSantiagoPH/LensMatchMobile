package com.lensmatch.mobile.ui.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.AnnouncementModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.ViewHolder> {

    private final List<AnnouncementModel> announcements;

    public AnnouncementAdapter(List<AnnouncementModel> announcements) {
        this.announcements = announcements;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification_announcement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AnnouncementModel item = announcements.get(position);

        // Category chip
        holder.tvCategory.setText(item.getCategory().toUpperCase());

        // Title
        holder.tvTitle.setText(item.getTitle());

        // Description
        holder.tvDescription.setText(item.getDescription());

        // Posted date (from createdAt timestamp)
        Timestamp createdAt = item.getCreatedAt();
        if (createdAt != null) {
            Date date = createdAt.toDate();
            String formatted = new SimpleDateFormat("MMM d, yyyy", Locale.US).format(date);
            holder.tvDate.setText(formatted);
        } else {
            holder.tvDate.setText("");
        }

        // Date range (valid period) — show only when both dates are set
        String start = item.getStartDate();
        String end = item.getEndDate();
        if (!start.isEmpty() && !end.isEmpty()) {
            holder.layoutDateRange.setVisibility(View.VISIBLE);
            holder.tvRange.setText(formatDateRange(start, end));
        } else {
            holder.layoutDateRange.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return announcements.size();
    }

    /** Converts "2025-01-01" to "Jan 1, 2025" */
    private String formatDateRange(String start, String end) {
        try {
            SimpleDateFormat inFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat outFmt = new SimpleDateFormat("MMM d, yyyy", Locale.US);
            String s = outFmt.format(inFmt.parse(start));
            String e = outFmt.format(inFmt.parse(end));
            return s + "  –  " + e;
        } catch (Exception ex) {
            return start + " – " + end;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvDate, tvTitle, tvDescription, tvRange;
        LinearLayout layoutDateRange;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory      = itemView.findViewById(R.id.tv_announcement_category);
            tvDate          = itemView.findViewById(R.id.tv_announcement_date);
            tvTitle         = itemView.findViewById(R.id.tv_announcement_title);
            tvDescription   = itemView.findViewById(R.id.tv_announcement_description);
            tvRange         = itemView.findViewById(R.id.tv_announcement_range);
            layoutDateRange = itemView.findViewById(R.id.layout_date_range);
        }
    }
}
