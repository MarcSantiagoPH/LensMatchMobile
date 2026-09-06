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
    private boolean lastIsBorderline = false;
    private String lastRunnerUpShape = "Round";
    private String lastNotes = null;
    private int lastActiveTab = 0;

    private String userName = "John Doe";
    private String userEmail = "johndoe@example.com";
    private String userPhotoUrl = null;
    private boolean isLoggedIn = false;

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
        state.lastIsBorderline = prefs.getBoolean("lastIsBorderline", false);
        state.lastRunnerUpShape = prefs.getString("lastRunnerUpShape", "Round");
        state.lastNotes = prefs.getString("lastNotes", null);
        state.lastActiveTab = prefs.getInt("lastActiveTab", 0);
        state.userName = prefs.getString("userName", "John Doe");
        state.userEmail = prefs.getString("userEmail", "johndoe@example.com");
        state.userPhotoUrl = prefs.getString("userPhotoUrl", null);
        state.isLoggedIn = prefs.getBoolean("isLoggedIn", false);
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

    public boolean getLastIsBorderline() { return lastIsBorderline; }
    public void setLastIsBorderline(boolean borderline) {
        this.lastIsBorderline = borderline;
        if (prefs != null) {
            prefs.edit().putBoolean("lastIsBorderline", borderline).apply();
        }
    }

    public String getLastRunnerUpShape() { return lastRunnerUpShape; }
    public void setLastRunnerUpShape(String runnerUp) {
        this.lastRunnerUpShape = runnerUp;
        if (prefs != null) {
            prefs.edit().putString("lastRunnerUpShape", runnerUp).apply();
        }
    }

    public String getLastNotes() { return lastNotes; }
    public void setLastNotes(String notes) {
        this.lastNotes = notes;
        if (prefs != null) {
            prefs.edit().putString("lastNotes", notes).apply();
        }
    }

    public int getLastActiveTab() { return lastActiveTab; }
    public void setLastActiveTab(int tabId) {
        this.lastActiveTab = tabId;
        if (prefs != null) {
            prefs.edit().putInt("lastActiveTab", tabId).apply();
        }
    }

    public String getUserName() { return userName; }
    public String getUserEmail() { return userEmail; }
    public String getUserPhotoUrl() { return userPhotoUrl; }
    public boolean isLoggedIn() { return isLoggedIn; }

    public void setUserProfile(String name, String email, String photoUrl) {
        this.userName = (name != null && !name.trim().isEmpty()) ? name : "User";
        this.userEmail = (email != null && !email.trim().isEmpty()) ? email : "user@lensmatch.com";
        this.userPhotoUrl = photoUrl;
        this.isLoggedIn = true;

        if (prefs != null) {
            prefs.edit()
                    .putString("userName", this.userName)
                    .putString("userEmail", this.userEmail)
                    .putString("userPhotoUrl", this.userPhotoUrl)
                    .putBoolean("isLoggedIn", true)
                    .apply();
        }
    }

    public void logout() {
        this.isLoggedIn = false;
        this.userName = "John Doe";
        this.userEmail = "johndoe@example.com";
        this.userPhotoUrl = null;
        this.lastActiveTab = 0;
        if (prefs != null) {
            prefs.edit()
                    .putBoolean("isLoggedIn", false)
                    .remove("userName")
                    .remove("userEmail")
                    .remove("userPhotoUrl")
                    .remove("lastActiveTab")
                    .apply();
        }
    }
}
