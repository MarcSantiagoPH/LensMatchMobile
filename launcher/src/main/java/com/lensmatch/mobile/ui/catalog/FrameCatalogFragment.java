package com.lensmatch.mobile.ui.catalog;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.FrameModel;
import com.lensmatch.mobile.service.FirestoreService;

import java.util.ArrayList;
import java.util.List;

public class FrameCatalogFragment extends Fragment {
    private RecyclerView rvFrames;
    private ProgressBar progressCatalog;
    private TextView tvEmptyCatalog;
    private ChipGroup chipGroupFilter;
    private String selectedStyle = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_frame_catalog, container, false);

        rvFrames = root.findViewById(R.id.rv_frames);
        progressCatalog = root.findViewById(R.id.progress_catalog);
        tvEmptyCatalog = root.findViewById(R.id.tv_empty_catalog);
        chipGroupFilter = root.findViewById(R.id.chip_group_style_filter);

        rvFrames.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip chip = group.findViewById(checkedIds.get(0));
                if (chip != null) {
                    selectedStyle = chip.getText().toString();
                } else {
                    selectedStyle = "All";
                }
            } else {
                selectedStyle = "All";
            }
            loadCatalogFrames();
        });

        loadCatalogFrames();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCatalogFrames();
    }

    private void loadCatalogFrames() {
        if (progressCatalog != null) progressCatalog.setVisibility(View.VISIBLE);
        if (tvEmptyCatalog != null) tvEmptyCatalog.setVisibility(View.GONE);

        FirestoreService.getFramesByStyle(selectedStyle, new FirestoreService.Callback<List<FrameModel>>() {
            @Override
            public void onSuccess(List<FrameModel> frames) {
                if (!isAdded() || getContext() == null) return;
                if (progressCatalog != null) progressCatalog.setVisibility(View.GONE);

                if (frames == null || frames.isEmpty()) {
                    if (tvEmptyCatalog != null) tvEmptyCatalog.setVisibility(View.VISIBLE);
                    rvFrames.setAdapter(new FrameAdapter(new ArrayList<>(), frame -> {}));
                } else {
                    if (tvEmptyCatalog != null) tvEmptyCatalog.setVisibility(View.GONE);
                    FrameAdapter adapter = new FrameAdapter(frames, frame -> {
                        Intent intent = new Intent(requireContext(), FrameDetailActivity.class);
                        intent.putExtra("frame", frame);
                        startActivity(intent);
                    });
                    rvFrames.setAdapter(adapter);
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded() || getContext() == null) return;
                if (progressCatalog != null) progressCatalog.setVisibility(View.GONE);
                if (tvEmptyCatalog != null) {
                    tvEmptyCatalog.setText("Unable to load frames. Showing offline samples.");
                    tvEmptyCatalog.setVisibility(View.VISIBLE);
                }

                // Offline fallback frames
                List<FrameModel> fallback = new ArrayList<>();
                fallback.add(new FrameModel("1", "Classic Aviator", "Aviator", "Metal", "₱129.00"));
                fallback.add(new FrameModel("2", "Retro Square", "Square", "Acetate", "₱145.00"));
                fallback.add(new FrameModel("3", "Minimalist Wire", "Round", "Titanium", "₱189.00"));
                fallback.add(new FrameModel("4", "Bold Rectangle", "Rectangle", "Acetate", "₱115.00"));

                FrameAdapter adapter = new FrameAdapter(fallback, frame -> {
                    Intent intent = new Intent(requireContext(), FrameDetailActivity.class);
                    intent.putExtra("frame", frame);
                    startActivity(intent);
                });
                rvFrames.setAdapter(adapter);
            }
        });
    }
}
