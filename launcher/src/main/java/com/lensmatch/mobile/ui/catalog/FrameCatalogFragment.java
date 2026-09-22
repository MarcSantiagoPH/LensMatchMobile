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
import android.widget.HorizontalScrollView;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.FrameModel;
import com.lensmatch.mobile.service.FirestoreService;
import com.lensmatch.mobile.ui.catalog.FrameDetailActivity;

import java.util.ArrayList;
import java.util.List;

public class FrameCatalogFragment extends Fragment {
    private RecyclerView rvFrames;
    private ProgressBar progressCatalog;
    private TextView tvEmptyCatalog;
    private ChipGroup chipGroupFilter;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;

    private LinearLayout layoutTitleRow;
    private LinearLayout layoutSearchRow;
    private ImageButton btnSortCatalog;
    private ImageButton btnOpenSearch;
    private ImageButton btnCloseSearch;
    private TextInputEditText etCatalogSearch;

    private final List<FrameModel> allLoadedFrames = new ArrayList<>();
    private String selectedStyle = "All";
    private String searchQuery = "";
    private int selectedSortIndex = 0;
    private boolean isSyncingChips = false;
    private final String[] sortOptions = {
            "Default",
            "New Arrivals First",
            "Price: Low to High",
            "Price: High to Low",
            "Name: A to Z"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_frame_catalog, container, false);

        rvFrames = root.findViewById(R.id.rv_frames);
        progressCatalog = root.findViewById(R.id.progress_catalog);
        tvEmptyCatalog = root.findViewById(R.id.tv_empty_catalog);
        chipGroupFilter = root.findViewById(R.id.chip_group_style_filter);
        swipeRefresh = root.findViewById(R.id.swipe_refresh_catalog);

        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeColors(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_orange),
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_orange_dark)
            );
            swipeRefresh.setOnRefreshListener(this::loadCatalogFrames);
        }

        layoutTitleRow = root.findViewById(R.id.layout_title_row);
        layoutSearchRow = root.findViewById(R.id.layout_search_row);
        btnSortCatalog = root.findViewById(R.id.btn_sort_catalog);
        btnOpenSearch = root.findViewById(R.id.btn_open_search);
        btnCloseSearch = root.findViewById(R.id.btn_close_search);
        etCatalogSearch = root.findViewById(R.id.et_catalog_search);

        if (btnSortCatalog != null) {
            btnSortCatalog.setOnClickListener(v -> showSortDialog());
        }

        rvFrames.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (isSyncingChips) return;
            if (!checkedIds.isEmpty()) {
                Chip chip = group.findViewById(checkedIds.get(0));
                if (chip != null && chip.getText() != null) {
                    selectedStyle = chip.getText().toString().trim();
                } else {
                    selectedStyle = "All";
                }
            } else {
                selectedStyle = "All";
            }
            com.lensmatch.mobile.data.AppState.getInstance().setPendingCatalogStyle(selectedStyle);
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

        String pending = com.lensmatch.mobile.data.AppState.getInstance().getPendingCatalogStyle();
        if (pending != null && !pending.trim().isEmpty()) {
            selectedStyle = pending.trim();
        }

        syncChipWithSelectedStyle();
        updateChipStyles();
        loadCatalogFrames();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        String pending = com.lensmatch.mobile.data.AppState.getInstance().getPendingCatalogStyle();
        if (pending != null && !pending.trim().isEmpty()) {
            this.selectedStyle = pending.trim();
            if (chipGroupFilter != null) {
                syncChipWithSelectedStyle();
                updateChipStyles();
            }
        }
        loadCatalogFrames();
    }

    public void setInitialStyle(String style) {
        if (style == null || style.trim().isEmpty()) return;
        this.selectedStyle = style.trim();
        com.lensmatch.mobile.data.AppState.getInstance().setPendingCatalogStyle(this.selectedStyle);
        if (chipGroupFilter != null && rvFrames != null) {
            syncChipWithSelectedStyle();
            updateChipStyles();
            applyLocalFilters();
        }
    }

    private void syncChipWithSelectedStyle() {
        if (chipGroupFilter == null) return;
        String targetStyle = (selectedStyle == null || selectedStyle.trim().isEmpty()) ? "All" : selectedStyle.trim();
        int targetChipId = R.id.chip_filter_all;

        for (int i = 0; i < chipGroupFilter.getChildCount(); i++) {
            View child = chipGroupFilter.getChildAt(i);
            if (child instanceof Chip chip) {
                if (chip.getText() != null && chip.getText().toString().trim().equalsIgnoreCase(targetStyle)) {
                    targetChipId = chip.getId();
                    this.selectedStyle = chip.getText().toString().trim();
                    break;
                }
            }
        }

        isSyncingChips = true;
        try {
            chipGroupFilter.clearCheck();
            chipGroupFilter.check(targetChipId);
        } finally {
            isSyncingChips = false;
        }

        final int finalTargetId = targetChipId;
        chipGroupFilter.post(() -> {
            View selectedChip = chipGroupFilter.findViewById(finalTargetId);
            if (selectedChip != null && chipGroupFilter.getParent() instanceof HorizontalScrollView hsv) {
                int scrollX = selectedChip.getLeft() - (hsv.getWidth() / 4);
                hsv.smoothScrollTo(Math.max(0, scrollX), 0);
            }
        });
    }

    private void updateChipStyles() {
        if (chipGroupFilter == null || getContext() == null) return;
        int activeCheckedId = chipGroupFilter.getCheckedChipId();
        if (activeCheckedId == View.NO_ID) {
            for (int i = 0; i < chipGroupFilter.getChildCount(); i++) {
                View child = chipGroupFilter.getChildAt(i);
                if (child instanceof Chip chip && chip.getText() != null &&
                        chip.getText().toString().trim().equalsIgnoreCase(selectedStyle)) {
                    activeCheckedId = chip.getId();
                    break;
                }
            }
            if (activeCheckedId == View.NO_ID) {
                activeCheckedId = R.id.chip_filter_all;
            }
        }

        int selBg = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.chipSelectedBg);
        int selStroke = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.chipStrokeSelected);
        int selText = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.chipSelectedText);

        int unselBg = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.chipUnselectedBg);
        int unselStroke = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.chipStrokeUnselected);
        int unselText = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.chipUnselectedText);

        final int targetCheckedId = activeCheckedId;
        for (int i = 0; i < chipGroupFilter.getChildCount(); i++) {
            View child = chipGroupFilter.getChildAt(i);
            if (child instanceof Chip chip) {
                if (chip.getId() == targetCheckedId) {
                    chip.setChipBackgroundColor(ColorStateList.valueOf(selBg));
                    chip.setChipStrokeColor(ColorStateList.valueOf(selStroke));
                    chip.setChipStrokeWidth(3f);
                    chip.setTextColor(selText);
                } else {
                    chip.setChipBackgroundColor(ColorStateList.valueOf(unselBg));
                    chip.setChipStrokeColor(ColorStateList.valueOf(unselStroke));
                    chip.setChipStrokeWidth(1.5f);
                    chip.setTextColor(unselText);
                }
            }
        }
    }

    private void showSortDialog() {
        if (getContext() == null) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sort Frames")
                .setSingleChoiceItems(sortOptions, selectedSortIndex, (dialog, which) -> {
                    selectedSortIndex = which;
                    applyLocalFilters();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadCatalogFrames() {
        if (swipeRefresh == null || !swipeRefresh.isRefreshing()) {
            if (progressCatalog != null) progressCatalog.setVisibility(View.VISIBLE);
        }
        if (tvEmptyCatalog != null) tvEmptyCatalog.setVisibility(View.GONE);

        FirestoreService.getFrames(new FirestoreService.Callback<List<FrameModel>>() {
            @Override
            public void onSuccess(List<FrameModel> frames) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
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
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (!isAdded() || getContext() == null) return;
                if (progressCatalog != null) progressCatalog.setVisibility(View.GONE);

                // Offline fallback frames
                allLoadedFrames.clear();
                allLoadedFrames.add(new FrameModel("1", "Classic Aviator", "Aviator", "Metal", "₱9,500.00", true));
                allLoadedFrames.add(new FrameModel("2", "Retro Square", "Square", "Acetate", "₱7,800.00", false));
                allLoadedFrames.add(new FrameModel("3", "Minimalist Wire", "Round", "Titanium", "₱11,200.00", true));
                allLoadedFrames.add(new FrameModel("4", "Bold Rectangle", "Rectangle", "Acetate", "₱6,500.00", false));

                applyLocalFilters();
            }
        });
    }

    private boolean matchesStyle(String frameStyle, String targetStyle) {
        if (frameStyle == null || targetStyle == null) return false;
        String f = frameStyle.trim().toLowerCase().replace("-", "").replace(" ", "");
        String t = targetStyle.trim().toLowerCase().replace("-", "").replace(" ", "");
        return f.equals(t) || f.contains(t) || t.contains(f);
    }

    private void applyLocalFilters() {
        List<FrameModel> filtered = new ArrayList<>();
        String q = searchQuery != null ? searchQuery.trim().toLowerCase() : "";

        for (FrameModel frame : allLoadedFrames) {
            boolean styleMatches = false;
            if ("All".equalsIgnoreCase(selectedStyle) || selectedStyle == null || selectedStyle.trim().isEmpty()) {
                styleMatches = true;
            } else if (matchesStyle(frame.getFrameStyle(), selectedStyle)) {
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

        switch (selectedSortIndex) {
            case 1: // ✨ New Arrivals First
                filtered.sort((a, b) -> Boolean.compare(b.isNew(), a.isNew()));
                break;
            case 2: // Price: Low to High
                filtered.sort((a, b) -> Double.compare(a.getPriceValue(), b.getPriceValue()));
                break;
            case 3: // Price: High to Low
                filtered.sort((a, b) -> Double.compare(b.getPriceValue(), a.getPriceValue()));
                break;
            case 4: // Name: A to Z
                filtered.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                break;
            case 0:
            default:
                break;
        }

        if (rvFrames != null) {
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
}
