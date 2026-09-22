package com.lensmatch.mobile.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.lensmatch.mobile.utils.FaceMetrics;
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
    private FaceMetrics lastMetrics = null;
    private int lastActiveTab = 0;
    private String pendingCatalogStyle = null;

    private String userName = "John Doe";
    private String userEmail = "johndoe@example.com";
    private String userPhone = "";
    private String userAddress = "";
    private String userPhotoUrl = null;
    private boolean isLoggedIn = false;
    private String themeMode = "dark";
    private boolean hasAcceptedEula = false;
    private long eulaAcceptanceTimestamp = 0;
    private boolean hasSeenGuidelines = false;

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
        state.pendingCatalogStyle = prefs.getString("pendingCatalogStyle", null);
        state.userName = prefs.getString("userName", "John Doe");
        state.userEmail = prefs.getString("userEmail", "johndoe@example.com");
        state.userPhone = prefs.getString("userPhone", "");
        state.userAddress = prefs.getString("userAddress", "");
        state.userPhotoUrl = prefs.getString("userPhotoUrl", null);
        state.isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        state.themeMode = prefs.getString("themeMode", "dark");
        state.hasAcceptedEula = prefs.getBoolean("hasAcceptedEula", false);
        state.eulaAcceptanceTimestamp = prefs.getLong("eulaAcceptanceTimestamp", 0);
        state.hasSeenGuidelines = prefs.getBoolean("hasSeenGuidelines", false);
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

    public boolean hasAcceptedEula() { return hasAcceptedEula; }
    public long getEulaAcceptanceTimestamp() { return eulaAcceptanceTimestamp; }

    public void setEulaAccepted(boolean accepted) {
        this.hasAcceptedEula = accepted;
        this.eulaAcceptanceTimestamp = accepted ? System.currentTimeMillis() : 0;
        if (prefs != null) {
            prefs.edit()
                .putBoolean("hasAcceptedEula", this.hasAcceptedEula)
                .putLong("eulaAcceptanceTimestamp", this.eulaAcceptanceTimestamp)
                .apply();
        }
    }

    public boolean hasSeenGuidelines() { return hasSeenGuidelines; }

    public void setHasSeenGuidelines(boolean seen) {
        this.hasSeenGuidelines = seen;
        if (prefs != null) {
            prefs.edit()
                .putBoolean("hasSeenGuidelines", this.hasSeenGuidelines)
                .apply();
        }
    }

    public String getPendingCatalogStyle() {
        return pendingCatalogStyle;
    }

    public void setPendingCatalogStyle(String style) {
        this.pendingCatalogStyle = style;
        if (prefs != null) {
            if (style != null) {
                prefs.edit().putString("pendingCatalogStyle", style).apply();
            } else {
                prefs.edit().remove("pendingCatalogStyle").apply();
            }
        }
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

    public FaceMetrics getLastMetrics() { return lastMetrics; }
    public void setLastMetrics(FaceMetrics metrics) {
        this.lastMetrics = metrics;
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
        this.hasSeenGuidelines = false;
        this.userName = "John Doe";
        this.userEmail = "johndoe@example.com";
        this.userPhone = "";
        this.userAddress = "";
        this.userPhotoUrl = null;
        this.lastActiveTab = 0;
        this.scanHistory.clear();
        if (prefs != null) {
            prefs.edit()
                    .putBoolean("isLoggedIn", false)
                    .putBoolean("hasSeenGuidelines", false)
                    .remove("userName")
                    .remove("userEmail")
                    .remove("userPhone")
                    .remove("userAddress")
                    .remove("userPhotoUrl")
                    .remove("lastActiveTab")
                    .remove("scanHistoryList")
                    .apply();
        }
    }

    public synchronized List<ScanModel> getScanHistory() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        List<ScanModel> filtered = new ArrayList<>();
        String currentUid = user != null ? user.getUid() : null;
        boolean modified = false;

        for (ScanModel scan : scanHistory) {
            if (scan == null) continue;
            String cId = scan.getCustomerId();
            if (currentUid != null) {
                // Adopt any orphan/local/guest scans to the current authenticated user
                if (cId == null || cId.isEmpty() || "local".equalsIgnoreCase(cId) || "guest".equalsIgnoreCase(cId)) {
                    scan.setCustomerId(currentUid);
                    if (scan.getCustomerName() == null || scan.getCustomerName().isEmpty() || "User".equals(scan.getCustomerName())) {
                        scan.setCustomerName(userName);
                    }
                    filtered.add(scan);
                    modified = true;
                } else if (currentUid.equalsIgnoreCase(cId)) {
                    filtered.add(scan);
                } else if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(cId)) {
                    scan.setCustomerId(currentUid);
                    filtered.add(scan);
                    modified = true;
                }
            } else {
                // If unauthenticated, show all available local scans
                filtered.add(scan);
            }
        }
        if (modified) {
            saveScanHistoryToPrefs();
        }
        return filtered;
    }

    public synchronized List<ScanModel> getAllRawScans() {
        return new ArrayList<>(scanHistory);
    }

    public synchronized void updateScanId(String oldId, String newId) {
        if (oldId == null || newId == null) return;
        for (ScanModel scan : scanHistory) {
            if (scan != null && oldId.equals(scan.getId())) {
                scan.setId(newId);
                saveScanHistoryToPrefs();
                return;
            }
        }
    }

    public synchronized void addScan(ScanModel scan) {
        if (scan == null) return;
        // Auto-backfill Base64 thumbnail from local disk file if missing
        if ((scan.getPhotoBase64() == null || scan.getPhotoBase64().isEmpty()) && scan.getImagePath() != null) {
            java.io.File file = new java.io.File(scan.getImagePath());
            if (file.exists()) {
                try {
                    android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath());
                    if (bmp != null) {
                        scan.setPhotoBase64(ScanModel.encodeBitmapToBase64Thumbnail(bmp, 240, 70));
                    }
                } catch (Throwable ignored) {}
            }
        }

        // If scan already exists by id or timestamp+shape, update it
        for (int i = 0; i < scanHistory.size(); i++) {
            ScanModel existing = scanHistory.get(i);
            if (existing != null) {
                if (scan.getId() != null && scan.getId().equals(existing.getId())) {
                    scanHistory.set(i, scan);
                    saveScanHistoryToPrefs();
                    return;
                }
                if (scan.getTimestamp() != null && existing.getTimestamp() != null
                        && Math.abs(scan.getTimestamp().getTime() - existing.getTimestamp().getTime()) < 5000
                        && scan.getFaceShape().equalsIgnoreCase(existing.getFaceShape())) {
                    if (existing.getId() != null && !existing.getId().startsWith("scan_")) {
                        scan.setId(existing.getId());
                    }
                    scanHistory.set(i, scan);
                    saveScanHistoryToPrefs();
                    return;
                }
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

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String currentUid = user != null ? user.getUid() : null;

        for (ScanModel cloudScan : cloudScans) {
            if (cloudScan == null) continue;
            // Do not merge cloud scans belonging to another user
            if (currentUid != null && cloudScan.getCustomerId() != null
                    && !cloudScan.getCustomerId().isEmpty()
                    && !"local".equalsIgnoreCase(cloudScan.getCustomerId())
                    && !"guest".equalsIgnoreCase(cloudScan.getCustomerId())
                    && !currentUid.equalsIgnoreCase(cloudScan.getCustomerId())) {
                continue;
            }

            boolean exists = false;
            for (int i = 0; i < scanHistory.size(); i++) {
                ScanModel local = scanHistory.get(i);
                if (local != null && ((cloudScan.getId() != null && cloudScan.getId().equals(local.getId())) ||
                    (cloudScan.getTimestamp() != null && local.getTimestamp() != null &&
                     Math.abs(cloudScan.getTimestamp().getTime() - local.getTimestamp().getTime()) < 5000 &&
                     cloudScan.getFaceShape().equalsIgnoreCase(local.getFaceShape())))) {
                    if (local.getImagePath() != null && new java.io.File(local.getImagePath()).exists()) {
                        cloudScan.setImagePath(local.getImagePath());
                    }
                    if (cloudScan.getPhotoBase64() == null || cloudScan.getPhotoBase64().isEmpty()) {
                        cloudScan.setPhotoBase64(local.getPhotoBase64());
                    }
                    scanHistory.set(i, cloudScan);
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                // If cloudScan has photoBase64 but no local file, write a cache file to disk
                if (cloudScan.getPhotoBase64() != null && !cloudScan.getPhotoBase64().isEmpty() &&
                    (cloudScan.getImagePath() == null || !new java.io.File(cloudScan.getImagePath()).exists())) {
                    try {
                        java.io.File scansDir = new java.io.File(android.os.Environment.getExternalStorageDirectory(), "scans");
                        // Fallback to internal storage if needed
                    } catch (Throwable ignored) {}
                }
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
        prefs.edit().putString("scanHistoryList", array.toString()).commit();
    }

    private synchronized void loadScanHistoryFromPrefs() {
        scanHistory.clear();
        if (prefs == null) return;
        String jsonStr = prefs.getString("scanHistoryList", null);
        boolean needResave = false;
        if (jsonStr != null && !jsonStr.trim().isEmpty()) {
            try {
                JSONArray array = new JSONArray(jsonStr);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    ScanModel scan = ScanModel.fromJson(obj);
                    if (scan != null) {
                        // Auto-backfill Base64 thumbnail if missing but local disk image is available
                        if ((scan.getPhotoBase64() == null || scan.getPhotoBase64().isEmpty()) && scan.getImagePath() != null) {
                            java.io.File file = new java.io.File(scan.getImagePath());
                            if (file.exists()) {
                                try {
                                    android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath());
                                    if (bmp != null) {
                                        scan.setPhotoBase64(ScanModel.encodeBitmapToBase64Thumbnail(bmp, 240, 70));
                                        needResave = true;
                                    }
                                } catch (Throwable ignored) {}
                            }
                        }
                        scanHistory.add(scan);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (needResave) {
            saveScanHistoryToPrefs();
        }
        // If scan history was empty but we have a last detected shape, add it as initial scan
        restoreLastScanIfEmpty();
    }

    public synchronized void restoreLastScanIfEmpty() {
        if (scanHistory.isEmpty() && lastDetectedShape != null && !lastDetectedShape.isEmpty() && !"Unknown".equalsIgnoreCase(lastDetectedShape)) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String cId = user != null ? user.getUid() : "local";
            String cName = (userName != null && !userName.isEmpty() && !"John Doe".equals(userName))
                    ? userName
                    : (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty() ? user.getDisplayName() : "Customer");

            FaceShapeDetector.ShapeRecommendation rec = FaceShapeDetector.getRecommendation(lastDetectedShape);
            List<String> recStyles = new ArrayList<>();
            List<String> avoidStyles = new ArrayList<>();
            if (rec != null) {
                if (rec.primary != null) recStyles.addAll(rec.primary);
                if (rec.secondary != null) avoidStyles.addAll(rec.secondary);
            }
            ScanModel initialScan = new ScanModel(
                    "scan_initial_" + System.currentTimeMillis(),
                    cId,
                    cName,
                    lastDetectedShape,
                    lastConfidence > 0 ? lastConfidence : 0.94f,
                    lastIsBorderline,
                    lastRunnerUpShape,
                    lastNotes,
                    recStyles,
                    avoidStyles,
                    rec != null ? rec.explanation : "",
                    "",
                    new Date()
            );
            if (lastImagePath != null && new java.io.File(lastImagePath).exists()) {
                initialScan.setImagePath(lastImagePath);
                try {
                    android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeFile(lastImagePath);
                    if (bmp != null) {
                        initialScan.setPhotoBase64(ScanModel.encodeBitmapToBase64Thumbnail(bmp, 240, 70));
                    }
                } catch (Throwable ignored) {}
            }
            scanHistory.add(initialScan);
            saveScanHistoryToPrefs();
        }
    }
}
