package com.lensmatch.mobile.ui.catalog;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lensmatch.mobile.R;
import com.lensmatch.mobile.data.FrameModel;

import java.util.ArrayList;
import java.util.List;

public class FrameCatalogFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_frame_catalog, container, false);

        RecyclerView rvFrames = root.findViewById(R.id.rv_frames);
        rvFrames.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        List<FrameModel> frames = new ArrayList<>();
        frames.add(new FrameModel("1", "Classic Aviator", "Aviator", "Metal", "$129.00"));
        frames.add(new FrameModel("2", "Retro Square", "Square", "Acetate", "$145.00"));
        frames.add(new FrameModel("3", "Minimalist Wire", "Round", "Titanium", "$189.00"));
        frames.add(new FrameModel("4", "Bold Rectangle", "Rectangle", "Acetate", "$115.00"));

        FrameAdapter adapter = new FrameAdapter(frames, frame -> {
            Intent intent = new Intent(requireContext(), FrameDetailActivity.class);
            intent.putExtra("frame", frame);
            startActivity(intent);
        });

        rvFrames.setAdapter(adapter);
        return root;
    }
}
