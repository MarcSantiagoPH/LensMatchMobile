package com.lensmatch.mobile.ui.catalog;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
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

    private LinearLayout layoutTitleRow;
    private LinearLayout layoutSearchRow;
    private ImageButton btnOpenSearch;
    private ImageButton btnCloseSearch;
    private TextInputEditText etCatalogSearch;

    private final List<FrameModel> allLoadedFrames = new ArrayList<>();
    private String selectedStyle = "All";
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_frame_catalog, container, false);

        rvFrames = root.findViewById(R.id.rv_frames);
        progressCatalog = root.findViewById(R.id.progress_catalog);
        tvEmptyCatalog = root.findViewById(R.id.tv_empty_catalog);
        chipGroupFilter = root.findViewById(R.id.chip_group_style_filter);

        layoutTitleRow = root.findViewById(R.id.layout_title_row);
        layoutSearchRow = root.findViewById(R.id.layout_search_row);
        btnOpenSearch = root.findViewById(R.id.btn_open_search);
        btnCloseSearch = root.findViewById(R.id.btn_close_search);
        etCatalogSearch = root.findViewById(R.id.et_catalog_search);

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
            updateChipStyles();
            applyLocalFilters();
        });

        if (btnOpenSearch != null) {
            btnOpenSearch.setOnClickListener(v -> {
                if (layoutTitleRow != null) layoutTitleRow.setVisibility(View.GONE);
                if (layoutSearchRow != null) layoutSearchRow.setVisibility(View.VISIBLE);
                if (etCatalogSearch != null) etCatalogSearch.requestFocus();
            });
        }

        if (btnCloseSearch != null) {
            btnCloseSearch.setOnClickListener(v -> {
                if (etCatalogSearch != null) etCatalogSearch.setText("");
                searchQuery = "";
                if (layoutSearchRow != null) layoutSearchRow.setVisibility(View.GONE);
                if (layoutTitleRow != null) layoutTitleRow.setVisibility(View.VISIBLE);
                applyLocalFilters();
            });
        }

        if (etCatalogSearch != null) {
            etCatalogSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s != null ? s.toString() : "";
                    applyLocalFilters();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        updateChipStyles();
        loadCatalogFrames();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCatalogFrames();
    }

    private void updateChipStyles() {
        if (chipGroupFilter == null) return;
        int activeCheckedId = chipGroupFilter.getCheckedChipId();
        if (activeCheckedId == View.NO_ID) {
            Chip allChip = chipGroupFilter.findViewById(R.id.chip_filter_all);
            if (allChip != null) {
                allChip.setChecked(true);
                activeCheckedId = R.id.chip_filter_all;
            }
        }

        final int targetCheckedId = activeCheckedId;
        for (int i = 0; i < chipGroupFilter.getChildCount(); i++) {
            View child = chipGroupFilter.getChildAt(i);
            if (child instanceof Chip chip) {
                if (chip.getId() == targetCheckedId) {
                    chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#221E14")));
                    chip.setChipStrokeColor(ColorStateList.valueOf(Color.parseColor("#D4AF37")));
                    chip.setChipStrokeWidth(3f);
                    chip.setTextColor(Color.parseColor("#D4AF37"));
                } else {
                    chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#18181B")));
                    chip.setChipStrokeColor(ColorStateList.valueOf(Color.parseColor("#27272A")));
                    chip.setChipStrokeWidth(1f);
                    chip.setTextColor(Color.parseColor("#A1A1AA"));
                }
            }
        }
    }

    private void loadCatalogFrames() {
        if (progressCatalog != null) progressCatalog.setVisibility(View.VISIBLE);
        if (tvEmptyCatalog != null) tvEmptyCatalog.setVisibility(View.GONE);

        FirestoreService.getFrames(new FirestoreService.Callback<List<FrameModel>>() {
            @Override
            public void onSuccess(List<FrameModel> frames) {
                if (!isAdded() || getContext() == null) return;
                if (progressCatalog != null) progressCatalog.setVisibility(View.GONE);

                allLoadedFrames.clear();
                if (frames != null) {
                    allLoadedFrames.addAll(frames);
                }
                applyLocalFilters();
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded() || getContext() == null) return;
                if (progressCatalog != null) progressCatalog.setVisibility(View.GONE);

                // Offline fallback frames
                allLoadedFrames.clear();
                allLoadedFrames.add(new FrameModel("1", "Classic Aviator", "Aviator", "Metal", "₱129.00"));
                allLoadedFrames.add(new FrameModel("2", "Retro Square", "Square", "Acetate", "₱145.00"));
                allLoadedFrames.add(new FrameModel("3", "Minimalist Wire", "Round", "Titanium", "₱189.00"));
                allLoadedFrames.add(new FrameModel("4", "Bold Rectangle", "Rectangle", "Acetate", "₱115.00"));

                applyLocalFilters();
            }
        });
    }

    private void applyLocalFilters() {
        List<FrameModel> filtered = new ArrayList<>();
        String q = searchQuery != null ? searchQuery.trim().toLowerCase() : "";

        for (FrameModel frame : allLoadedFrames) {
            boolean styleMatches = false;
            if ("All".equalsIgnoreCase(selectedStyle) || selectedStyle == null || selectedStyle.trim().isEmpty()) {
                styleMatches = true;
            } else if (frame.getFrameStyle() != null && frame.getFrameStyle().equalsIgnoreCase(selectedStyle.trim())) {
                styleMatches = true;
            }

            boolean searchMatches = false;
            if (q.isEmpty()) {
                searchMatches = true;
            } else {
                String name = frame.getName() != null ? frame.getName().toLowerCase() : "";
                String brand = frame.getBrand() != null ? frame.getBrand().toLowerCase() : "";
                if (name.contains(q) || brand.contains(q)) {
                    searchMatches = true;
                }
            }

            if (styleMatches && searchMatches) {
                filtered.add(frame);
            }
        }

        if (filtered.isEmpty()) {
            if (tvEmptyCatalog != null) {
                if (!q.isEmpty()) {
                    tvEmptyCatalog.setText(getString(R.string.catalog_no_frames_matching, searchQuery.trim()));
                } else {
                    tvEmptyCatalog.setText(getString(R.string.catalog_no_frames_style));
                }
                tvEmptyCatalog.setVisibility(View.VISIBLE);
            }
            rvFrames.setAdapter(new FrameAdapter(new ArrayList<>(), frame -> {}));
        } else {
            if (tvEmptyCatalog != null) tvEmptyCatalog.setVisibility(View.GONE);
            FrameAdapter adapter = new FrameAdapter(filtered, frame -> {
                Intent intent = new Intent(requireContext(), FrameDetailActivity.class);
                intent.putExtra("frame", frame);
                startActivity(intent);
            });
            rvFrames.setAdapter(adapter);
        }
    }
}
