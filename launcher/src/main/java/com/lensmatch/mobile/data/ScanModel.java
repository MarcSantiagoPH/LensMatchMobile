package com.lensmatch.mobile.data;

import android.graphics.Bitmap;
import android.util.Base64;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScanModel implements Serializable {
    private String id;
    private String customerId;
    private String customerName;
    private String faceShape;
    private float confidence;
    private boolean isBorderline;
    private String runnerUpShape;
    private String notes;
    private List<String> recommendedStyles;
    private List<String> avoidedStyles;
    private String recommendedReason;
    private String avoidedReason;
    private String imagePath;
    private String photoBase64;
    private Date timestamp;

    public ScanModel() {
        this.recommendedStyles = new ArrayList<>();
        this.avoidedStyles = new ArrayList<>();
        this.timestamp = new Date();
    }

    public ScanModel(String id, String customerId, String customerName, String faceShape,
                     float confidence, boolean isBorderline, String runnerUpShape, String notes,
                     List<String> recommendedStyles, List<String> avoidedStyles,
                     String recommendedReason, String avoidedReason, Date timestamp) {
        this(id, customerId, customerName, faceShape, confidence, isBorderline, runnerUpShape, notes,
                recommendedStyles, avoidedStyles, recommendedReason, avoidedReason, null, null, timestamp);
    }

    public ScanModel(String id, String customerId, String customerName, String faceShape,
                     float confidence, boolean isBorderline, String runnerUpShape, String notes,
                     List<String> recommendedStyles, List<String> avoidedStyles,
                     String recommendedReason, String avoidedReason, String imagePath, Date timestamp) {
        this(id, customerId, customerName, faceShape, confidence, isBorderline, runnerUpShape, notes,
                recommendedStyles, avoidedStyles, recommendedReason, avoidedReason, imagePath, null, timestamp);
    }

    public ScanModel(String id, String customerId, String customerName, String faceShape,
                     float confidence, boolean isBorderline, String runnerUpShape, String notes,
                     List<String> recommendedStyles, List<String> avoidedStyles,
                     String recommendedReason, String avoidedReason, String imagePath, String photoBase64, Date timestamp) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.faceShape = faceShape;
        this.confidence = confidence;
        this.isBorderline = isBorderline;
        this.runnerUpShape = runnerUpShape;
        this.notes = notes;
        this.recommendedStyles = recommendedStyles != null ? recommendedStyles : new ArrayList<>();
        this.avoidedStyles = avoidedStyles != null ? avoidedStyles : new ArrayList<>();
        this.recommendedReason = recommendedReason != null ? recommendedReason : "";
        this.avoidedReason = avoidedReason != null ? avoidedReason : "";
        this.imagePath = imagePath;
        this.photoBase64 = photoBase64;
        this.timestamp = timestamp != null ? timestamp : new Date();
    }

    public static ScanModel fromMap(Map<String, Object> data, String docId) {
        if (data == null) return new ScanModel();

        String id = docId != null ? docId : (String) data.get("id");
        String cId = "";
        if (data.containsKey("customerId") && data.get("customerId") != null) {
            cId = String.valueOf(data.get("customerId"));
        } else if (data.containsKey("userId") && data.get("userId") != null) {
            cId = String.valueOf(data.get("userId"));
        } else if (data.containsKey("uid") && data.get("uid") != null) {
            cId = String.valueOf(data.get("uid"));
        } else if (data.containsKey("customerEmail") && data.get("customerEmail") != null) {
            cId = String.valueOf(data.get("customerEmail"));
        }

        String cName = data.containsKey("customerName") && data.get("customerName") != null ? String.valueOf(data.get("customerName")) : "";
        String shape = data.containsKey("faceShape") && data.get("faceShape") != null ? String.valueOf(data.get("faceShape")) : "Unknown";

        float conf = 0.92f;
        if (data.containsKey("confidence")) {
            Object rawConf = data.get("confidence");
            if (rawConf instanceof Number) {
                conf = ((Number) rawConf).floatValue();
            } else {
                try {
                    conf = Float.parseFloat(String.valueOf(rawConf));
                } catch (Exception ignored) {}
            }
        }

        boolean borderline = false;
        if (data.containsKey("isBorderline")) {
            borderline = Boolean.parseBoolean(String.valueOf(data.get("isBorderline")));
        }

        String runnerUp = data.containsKey("runnerUpShape") && data.get("runnerUpShape") != null ? String.valueOf(data.get("runnerUpShape")) : "";
        String nts = data.containsKey("notes") && data.get("notes") != null ? String.valueOf(data.get("notes")) : "";
        String recReason = data.containsKey("recommendedReason") && data.get("recommendedReason") != null ? String.valueOf(data.get("recommendedReason")) : "";
        String avoidReason = data.containsKey("avoidedReason") && data.get("avoidedReason") != null ? String.valueOf(data.get("avoidedReason")) : "";

        List<String> recStyles = new ArrayList<>();
        if (data.get("recommendedStyles") instanceof List) {
            for (Object item : (List<?>) data.get("recommendedStyles")) {
                if (item != null) recStyles.add(String.valueOf(item));
            }
        }

        List<String> avStyles = new ArrayList<>();
        if (data.get("avoidedStyles") instanceof List) {
            for (Object item : (List<?>) data.get("avoidedStyles")) {
                if (item != null) avStyles.add(String.valueOf(item));
            }
        }

        Date time = null;
        Object rawTime = data.get("timestamp");
        if (rawTime instanceof Timestamp) {
            time = ((Timestamp) rawTime).toDate();
        } else if (rawTime instanceof Date) {
            time = (Date) rawTime;
        } else if (rawTime instanceof Number) {
            time = new Date(((Number) rawTime).longValue());
        } else {
            time = new Date();
        }

        String imgPath = data.containsKey("imagePath") && data.get("imagePath") != null ? String.valueOf(data.get("imagePath")) : null;
        String photoB64 = data.containsKey("photoBase64") && data.get("photoBase64") != null ? String.valueOf(data.get("photoBase64")) : null;

        return new ScanModel(id, cId, cName, shape, conf, borderline, runnerUp, nts, recStyles, avStyles, recReason, avoidReason, imgPath, photoB64, time);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("customerId", customerId != null ? customerId : "");
        map.put("customerName", customerName != null ? customerName : "");
        map.put("faceShape", faceShape != null ? faceShape : "");
        map.put("confidence", confidence);
        map.put("isBorderline", isBorderline);
        map.put("runnerUpShape", runnerUpShape != null ? runnerUpShape : "");
        map.put("notes", notes != null ? notes : "");
        map.put("recommendedStyles", recommendedStyles != null ? recommendedStyles : new ArrayList<>());
        map.put("avoidedStyles", avoidedStyles != null ? avoidedStyles : new ArrayList<>());
        map.put("recommendedReason", recommendedReason != null ? recommendedReason : "");
        map.put("avoidedReason", avoidedReason != null ? avoidedReason : "");
        if (imagePath != null && !imagePath.isEmpty()) {
            map.put("imagePath", imagePath);
        }
        if (photoBase64 != null && !photoBase64.isEmpty()) {
            map.put("photoBase64", photoBase64);
        }
        if (timestamp != null) {
            map.put("timestamp", new Timestamp(timestamp));
        } else {
            map.put("timestamp", FieldValue.serverTimestamp());
        }
        return map;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id != null ? id : "");
            json.put("customerId", customerId != null ? customerId : "");
            json.put("customerName", customerName != null ? customerName : "");
            json.put("faceShape", faceShape != null ? faceShape : "");
            json.put("confidence", (double) confidence);
            json.put("isBorderline", isBorderline);
            json.put("runnerUpShape", runnerUpShape != null ? runnerUpShape : "");
            json.put("notes", notes != null ? notes : "");
            json.put("recommendedReason", recommendedReason != null ? recommendedReason : "");
            json.put("avoidedReason", avoidedReason != null ? avoidedReason : "");
            json.put("imagePath", imagePath != null ? imagePath : "");
            json.put("photoBase64", photoBase64 != null ? photoBase64 : "");
            json.put("timestamp", timestamp != null ? timestamp.getTime() : System.currentTimeMillis());

            JSONArray recArr = new JSONArray();
            if (recommendedStyles != null) {
                for (String s : recommendedStyles) recArr.put(s);
            }
            json.put("recommendedStyles", recArr);

            JSONArray avoidArr = new JSONArray();
            if (avoidedStyles != null) {
                for (String s : avoidedStyles) avoidArr.put(s);
            }
            json.put("avoidedStyles", avoidArr);
        } catch (JSONException ignored) {}
        return json;
    }

    public static ScanModel fromJson(JSONObject json) {
        if (json == null) return null;
        ScanModel scan = new ScanModel();
        scan.id = json.optString("id", "");
        scan.customerId = json.optString("customerId", "");
        scan.customerName = json.optString("customerName", "");
        scan.faceShape = json.optString("faceShape", "Oval");
        scan.confidence = (float) json.optDouble("confidence", 0.94);
        scan.isBorderline = json.optBoolean("isBorderline", false);
        scan.runnerUpShape = json.optString("runnerUpShape", "");
        scan.notes = json.optString("notes", "");
        scan.recommendedReason = json.optString("recommendedReason", "");
        scan.avoidedReason = json.optString("avoidedReason", "");
        scan.imagePath = json.optString("imagePath", null);
        if (scan.imagePath != null && scan.imagePath.isEmpty()) {
            scan.imagePath = null;
        }
        scan.photoBase64 = json.optString("photoBase64", null);
        if (scan.photoBase64 != null && scan.photoBase64.isEmpty()) {
            scan.photoBase64 = null;
        }

        long time = json.optLong("timestamp", System.currentTimeMillis());
        scan.timestamp = new Date(time);

        scan.recommendedStyles = new ArrayList<>();
        JSONArray recArr = json.optJSONArray("recommendedStyles");
        if (recArr != null) {
            for (int i = 0; i < recArr.length(); i++) {
                scan.recommendedStyles.add(recArr.optString(i));
            }
        }

        scan.avoidedStyles = new ArrayList<>();
        JSONArray avoidArr = json.optJSONArray("avoidedStyles");
        if (avoidArr != null) {
            for (int i = 0; i < avoidArr.length(); i++) {
                scan.avoidedStyles.add(avoidArr.optString(i));
            }
        }
        return scan;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getFaceShape() { return faceShape != null ? faceShape : "Unknown"; }
    public void setFaceShape(String faceShape) { this.faceShape = faceShape; }
    public float getConfidence() { return confidence; }
    public void setConfidence(float confidence) { this.confidence = confidence; }
    public int getConfidencePercent() {
        int percent = Math.round(confidence * 100);
        return percent > 0 ? percent : 92;
    }
    public boolean isBorderline() { return isBorderline; }
    public void setBorderline(boolean borderline) { isBorderline = borderline; }
    public String getRunnerUpShape() { return runnerUpShape; }
    public void setRunnerUpShape(String runnerUpShape) { this.runnerUpShape = runnerUpShape; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<String> getRecommendedStyles() { return recommendedStyles; }
    public void setRecommendedStyles(List<String> recommendedStyles) { this.recommendedStyles = recommendedStyles; }
    public List<String> getAvoidedStyles() { return avoidedStyles; }
    public void setAvoidedStyles(List<String> avoidedStyles) { this.avoidedStyles = avoidedStyles; }
    public String getRecommendedReason() { return recommendedReason; }
    public void setRecommendedReason(String recommendedReason) { this.recommendedReason = recommendedReason; }
    public String getAvoidedReason() { return avoidedReason; }
    public void setAvoidedReason(String avoidedReason) { this.avoidedReason = avoidedReason; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getFormattedDate() {
        if (timestamp == null) return "Recent Scan";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.US);
        return sdf.format(timestamp);
    }

    public String getFormattedStylesSummary() {
        if (recommendedStyles == null || recommendedStyles.isEmpty()) {
            return "Curated optical styles";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < recommendedStyles.size(); i++) {
            sb.append(recommendedStyles.get(i));
            if (i < recommendedStyles.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    public String getPhotoBase64() { return photoBase64; }
    public void setPhotoBase64(String photoBase64) { this.photoBase64 = photoBase64; }

    public static String encodeBitmapToBase64Thumbnail(Bitmap source, int maxDimension, int quality) {
        if (source == null) return null;
        try {
            int width = source.getWidth();
            int height = source.getHeight();
            float scale = Math.min((float) maxDimension / width, (float) maxDimension / height);
            if (scale > 1.0f) scale = 1.0f;
            int scaledWidth = Math.max(1, Math.round(width * scale));
            int scaledHeight = Math.max(1, Math.round(height * scale));
            Bitmap scaled = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            if (scaled != source && !scaled.isRecycled()) {
                scaled.recycle();
            }
            byte[] byteArray = baos.toByteArray();
            return Base64.encodeToString(byteArray, Base64.NO_WRAP);
        } catch (Throwable t) {
            return null;
        }
    }
}
