package com.lensmatch.mobile.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class AppState {
    private static AppState instance;
    private static SharedPreferences prefs;
    private final List<FrameModel> reservedFrames = new ArrayList<>();
    private String lastImagePath;
    private String lastDetectedShape = "Oval";
    private float lastConfidence = 0.94f;

    private AppState() {}

    public static synchronized AppState getInstance() {
        if (instance == null) {
            instance = new AppState();
        }
        return instance;
    }

    public static void init(Context context) {
        prefs = context.getSharedPreferences("lensmatch_prefs", Context.MODE_PRIVATE);
        AppState state = getInstance();
        state.lastImagePath = prefs.getString("lastImagePath", null);
        state.lastDetectedShape = prefs.getString("lastDetectedShape", "Oval");
        state.lastConfidence = prefs.getFloat("lastConfidence", 0.94f);
    }

    public List<FrameModel> getReservedFrames() {
        return reservedFrames;
    }

    public void addReservation(FrameModel frame) {
        for (FrameModel item : reservedFrames) {
            if (item.getId().equals(frame.getId())) {
                return;
            }
        }
        reservedFrames.add(frame);
    }

    public boolean isReserved(String frameId) {
        for (FrameModel item : reservedFrames) {
            if (item.getId().equals(frameId)) {
                return true;
            }
        }
        return false;
    }

    public void removeReservation(String frameId) {
        reservedFrames.removeIf(item -> item.getId().equals(frameId));
    }

    public String getLastImagePath() { return lastImagePath; }
    public void setLastImagePath(String path) {
        this.lastImagePath = path;
        if (prefs != null) {
            prefs.edit().putString("lastImagePath", path).apply();
        }
    }

    public String getLastDetectedShape() { return lastDetectedShape; }
    public void setLastDetectedShape(String shape) {
        this.lastDetectedShape = shape;
        if (prefs != null) {
            prefs.edit().putString("lastDetectedShape", shape).apply();
        }
    }

    public float getLastConfidence() { return lastConfidence; }
    public void setLastConfidence(float confidence) {
        this.lastConfidence = confidence;
        if (prefs != null) {
            prefs.edit().putFloat("lastConfidence", confidence).apply();
        }
    }
}
