package com.lensmatch.mobile.data;

import com.google.firebase.Timestamp;

import java.io.Serializable;
import java.util.Map;

public class AnnouncementModel implements Serializable {
    private String id;
    private String title;
    private String category;
    private String description;
    private String status;
    private String startDate; // YYYY-MM-DD
    private String endDate;   // YYYY-MM-DD
    private Timestamp createdAt;

    public AnnouncementModel() {}

    public AnnouncementModel(String id, String title, String category, String description,
                             String status, String startDate, String endDate, Timestamp createdAt) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.description = description;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
    }

    public static AnnouncementModel fromMap(Map<String, Object> data, String docId) {
        if (data == null) return new AnnouncementModel();

        String id = docId != null ? docId : (data.containsKey("id") ? String.valueOf(data.get("id")) : "");
        String title = data.containsKey("title") && data.get("title") != null ? String.valueOf(data.get("title")).trim() : "";
        String category = data.containsKey("category") && data.get("category") != null ? String.valueOf(data.get("category")).trim() : "GENERAL";
        String description = data.containsKey("description") && data.get("description") != null ? String.valueOf(data.get("description")).trim() : "";
        String status = data.containsKey("status") && data.get("status") != null ? String.valueOf(data.get("status")).trim() : "Active";
        String startDate = data.containsKey("startDate") && data.get("startDate") != null ? String.valueOf(data.get("startDate")).trim() : "";
        String endDate = data.containsKey("endDate") && data.get("endDate") != null ? String.valueOf(data.get("endDate")).trim() : "";

        Timestamp createdAt = null;
        if (data.containsKey("createdAt") && data.get("createdAt") instanceof Timestamp) {
            createdAt = (Timestamp) data.get("createdAt");
        }

        return new AnnouncementModel(id, title, category, description, status, startDate, endDate, createdAt);
    }

    public String getId() { return id != null ? id : ""; }
    public String getTitle() { return title != null ? title : ""; }
    public String getCategory() { return category != null ? category : "GENERAL"; }
    public String getDescription() { return description != null ? description : ""; }
    public String getStatus() { return status != null ? status : "Active"; }
    public String getStartDate() { return startDate != null ? startDate : ""; }
    public String getEndDate() { return endDate != null ? endDate : ""; }
    public Timestamp getCreatedAt() { return createdAt; }
}
