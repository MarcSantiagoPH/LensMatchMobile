package com.lensmatch.mobile.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.lensmatch.mobile.utils.FaceShapeDetector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class AppState {
    private static AppState instance;
    private static SharedPreferences prefs;
    private final List<FrameModel> reservedFrames = new ArrayList<>();
    private final List<ScanModel> scanHistory = new ArrayList<>();
    private String lastImagePath;
    private String lastDetectedShape = "Oval";
    private float lastConfidence = 0.94f;
    private boolean lastIsBorderline = false;
    private String lastRunnerUpShape = "Round";
    private String lastNotes = null;
    private int lastActiveTab = 0;

    private String userName = "John Doe";
    private String userEmail = "johndoe@example.com";
    private String userPhone = "";
    private String userAddress = "";
    private String userPhotoUrl = null;
    private boolean isLoggedIn = false;
    private String themeMode = "dark";

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
        state.userPhone = prefs.getString("userPhone", "");
        state.userAddress = prefs.getString("userAddress", "");
        state.userPhotoUrl = prefs.getString("userPhotoUrl", null);
        state.isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        state.themeMode = prefs.getString("themeMode", "dark");
        state.applyThemeMode();
        state.loadScanHistoryFromPrefs();
    }

    public String getThemeMode() {
        return themeMode;
    }

    public boolean isDarkMode() {
        return "dark".equalsIgnoreCase(themeMode);
    }

    public void setThemeMode(String mode) {
        this.themeMode = "light";
        if (prefs != null) {
            prefs.edit().putString("themeMode", "light").apply();
        }
        applyThemeMode();
    }

    public void applyThemeMode() {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
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
    public String getUserPhone() { return userPhone; }
    public String getUserAddress() { return userAddress; }
    public String getUserPhotoUrl() { return userPhotoUrl; }
    public boolean isLoggedIn() { return isLoggedIn; }

    public void updateAccountDetails(String name, String phone, String address, String email) {
        this.userName = (name != null && !name.trim().isEmpty()) ? name.trim() : "John Doe";
        this.userPhone = (phone != null && !phone.trim().isEmpty()) ? phone.trim() : "";
        this.userAddress = (address != null && !address.trim().isEmpty()) ? address.trim() : "";
        this.userEmail = (email != null && !email.trim().isEmpty()) ? email.trim() : "johndoe@example.com";

        if (prefs != null) {
            prefs.edit()
                    .putString("userName", this.userName)
                    .putString("userPhone", this.userPhone)
                    .putString("userAddress", this.userAddress)
                    .putString("userEmail", this.userEmail)
                    .apply();
        }
    }

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
        this.userPhone = "";
        this.userAddress = "";
        this.userPhotoUrl = null;
        this.lastActiveTab = 0;
        if (prefs != null) {
            prefs.edit()
                    .putBoolean("isLoggedIn", false)
                    .remove("userName")
                    .remove("userEmail")
                    .remove("userPhone")
                    .remove("userAddress")
                    .remove("userPhotoUrl")
                    .remove("lastActiveTab")
                    .apply();
        }
    }

    public synchronized List<ScanModel> getScanHistory() {
        return new ArrayList<>(scanHistory);
    }

    public synchronized void addScan(ScanModel scan) {
        if (scan == null) return;
        // If scan already exists by id, update it
        for (int i = 0; i < scanHistory.size(); i++) {
            ScanModel existing = scanHistory.get(i);
            if (scan.getId() != null && scan.getId().equals(existing.getId())) {
                scanHistory.set(i, scan);
                saveScanHistoryToPrefs();
                return;
            }
        }
        // Add new scan at top (newest first)
        scanHistory.add(0, scan);
        saveScanHistoryToPrefs();
    }

    public synchronized void removeScan(String scanId) {
        if (scanId == null) return;
        scanHistory.removeIf(s -> scanId.equals(s.getId()));
        saveScanHistoryToPrefs();
    }

    public synchronized void syncScanHistory(List<ScanModel> cloudScans) {
        if (cloudScans == null || cloudScans.isEmpty()) return;
        for (ScanModel cloudScan : cloudScans) {
            boolean exists = false;
            for (ScanModel local : scanHistory) {
                if ((cloudScan.getId() != null && cloudScan.getId().equals(local.getId())) ||
                    (cloudScan.getTimestamp() != null && local.getTimestamp() != null &&
                     Math.abs(cloudScan.getTimestamp().getTime() - local.getTimestamp().getTime()) < 5000 &&
                     cloudScan.getFaceShape().equals(local.getFaceShape()))) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                scanHistory.add(cloudScan);
            }
        }
        Collections.sort(scanHistory, (a, b) -> {
            if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
            if (a.getTimestamp() == null) return 1;
            if (b.getTimestamp() == null) return -1;
            return b.getTimestamp().compareTo(a.getTimestamp());
        });
        saveScanHistoryToPrefs();
    }

    private synchronized void saveScanHistoryToPrefs() {
        if (prefs == null) return;
        JSONArray array = new JSONArray();
        for (ScanModel scan : scanHistory) {
            array.put(scan.toJson());
        }
        prefs.edit().putString("scanHistoryList", array.toString()).apply();
    }

    private synchronized void loadScanHistoryFromPrefs() {
        scanHistory.clear();
        if (prefs == null) return;
        String jsonStr = prefs.getString("scanHistoryList", null);
        if (jsonStr != null && !jsonStr.trim().isEmpty()) {
            try {
                JSONArray array = new JSONArray(jsonStr);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    ScanModel scan = ScanModel.fromJson(obj);
                    if (scan != null) {
                        scanHistory.add(scan);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        // If scan history was empty but we have a last detected shape, add it as initial scan
        if (scanHistory.isEmpty() && lastDetectedShape != null && !lastDetectedShape.isEmpty() && !"Unknown".equalsIgnoreCase(lastDetectedShape)) {
            FaceShapeDetector.ShapeRecommendation rec = FaceShapeDetector.getRecommendationForShape(lastDetectedShape);
            List<String> recStyles = new ArrayList<>();
            List<String> avoidStyles = new ArrayList<>();
            if (rec != null) {
                if (rec.recommended != null) recStyles.addAll(Arrays.asList(rec.recommended));
                if (rec.avoided != null) avoidStyles.addAll(Arrays.asList(rec.avoided));
            }
            ScanModel initialScan = new ScanModel(
                    "scan_initial",
                    "local",
                    userName,
                    lastDetectedShape,
                    lastConfidence,
                    lastIsBorderline,
                    lastRunnerUpShape,
                    lastNotes,
                    recStyles,
                    avoidStyles,
                    rec != null ? rec.recommendedReason : "",
                    rec != null ? rec.avoidedReason : "",
                    new Date()
            );
            if (lastImagePath != null) {
                initialScan.setImagePath(lastImagePath);
            }
            scanHistory.add(initialScan);
            saveScanHistoryToPrefs();
        }
    }
}
