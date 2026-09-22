package com.lensmatch.mobile.data;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ReservationModel implements Serializable {
    private String reservationId;
    private String customerId;
    private String customerName;
    private String customerEmail;
    private String frameId;
    private String frameName;
    private String brand;
    private String frameStyle;
    private double price;
    private String imageUrl;
    private String status;
    private Date createdAt;
    private Date statusUpdatedAt;
    private String cancellationReason;
    private Date reservationDeadline;
    private String rejectionReason;

    public ReservationModel() {}

    public ReservationModel(String reservationId, String customerId, String customerName, String customerEmail,
                            String frameId, String frameName, String brand, String frameStyle,
                            double price, String imageUrl, String status, Date createdAt, Date statusUpdatedAt) {
        this.reservationId = reservationId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.frameId = frameId;
        this.frameName = frameName;
        this.brand = brand;
        this.frameStyle = frameStyle;
        this.price = price;
        this.imageUrl = imageUrl;
        this.status = status != null ? status : "Pending";
        this.createdAt = createdAt;
        this.statusUpdatedAt = statusUpdatedAt;
    }

    public static ReservationModel fromMap(Map<String, Object> data, String docId) {
        if (data == null) return new ReservationModel();

        String id = docId != null ? docId : (String) data.get("reservationId");
        String cId = data.containsKey("customerId") ? String.valueOf(data.get("customerId")) : "";
        String cName = data.containsKey("customerName") ? String.valueOf(data.get("customerName")) : "";
        String cEmail = data.containsKey("customerEmail") ? String.valueOf(data.get("customerEmail")) : "";
        String fId = data.containsKey("frameId") ? String.valueOf(data.get("frameId")) : "";
        String fName = data.containsKey("frameName") ? String.valueOf(data.get("frameName")) : "";
        String brandStr = data.containsKey("brand") ? String.valueOf(data.get("brand")) : "";
        String fStyle = data.containsKey("frameStyle") ? String.valueOf(data.get("frameStyle")) : "";
        String img = data.containsKey("imageUrl") ? String.valueOf(data.get("imageUrl")) : "";
        String statusStr = data.containsKey("status") ? String.valueOf(data.get("status")) : "Pending";

        Object rawPrice = data.containsKey("price") ? data.get("price") : data.get("framePrice");
        double priceVal = parsePriceDouble(rawPrice);
        Date createdDate = parseDate(data.get("createdAt"));
        Date updatedDate = parseDate(data.get("statusUpdatedAt"));
        Date deadlineDate = parseDate(data.get("reservationDeadline"));
        String cancelReason = data.containsKey("cancellationReason") && data.get("cancellationReason") != null
                ? String.valueOf(data.get("cancellationReason")) : null;
        String rejectReason = data.containsKey("rejectionReason") && data.get("rejectionReason") != null
                ? String.valueOf(data.get("rejectionReason")).trim() : null;

        ReservationModel model = new ReservationModel(id, cId, cName, cEmail, fId, fName, brandStr, fStyle, priceVal, img, statusStr, createdDate, updatedDate);
        model.setCancellationReason(cancelReason);
        model.setReservationDeadline(deadlineDate);
        model.setRejectionReason(rejectReason);
        return model;
    }

    private static double parsePriceDouble(Object raw) {
        if (raw instanceof Number) {
            return ((Number) raw).doubleValue();
        } else if (raw instanceof String) {
            try {
                String sanitized = ((String) raw).replaceAll("[^\\d.]", "");
                return Double.parseDouble(sanitized);
            } catch (Exception e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private static Date parseDate(Object raw) {
        if (raw instanceof Timestamp) {
            return ((Timestamp) raw).toDate();
        } else if (raw instanceof Date) {
            return (Date) raw;
        } else if (raw instanceof String) {
            try {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse((String) raw);
            } catch (Exception ignored) {}
        }
        return null;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("customerId", customerId);
        map.put("customerName", customerName);
        map.put("customerEmail", customerEmail);
        map.put("frameId", frameId);
        map.put("frameName", frameName);
        map.put("brand", brand);
        map.put("frameStyle", frameStyle);
        map.put("price", price);
        map.put("imageUrl", imageUrl);
        map.put("status", status != null ? status : "Pending");
        map.put("createdAt", FieldValue.serverTimestamp());
        map.put("statusUpdatedAt", null);
        if (cancellationReason != null && !cancellationReason.trim().isEmpty()) {
            map.put("cancellationReason", cancellationReason.trim());
        }
        return map;
    }

    public String getReservationId() { return reservationId; }
    public String getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public String getFrameId() { return frameId; }
    public String getFrameName() { return frameName; }
    public String getBrand() { return brand; }
    public String getFrameStyle() { return frameStyle; }
    public double getPriceValue() { return price; }
    public String getFormattedPrice() { return String.format(Locale.US, "₱%,.2f", price); }
    public String getImageUrl() { return imageUrl; }
    public String getStatus() { return status != null ? status : "Pending"; }
    public Date getCreatedAt() { return createdAt; }
    public Date getStatusUpdatedAt() { return statusUpdatedAt; }
    public String getCancellationReason() { return cancellationReason; }
    public Date getReservationDeadline() { return reservationDeadline; }
    public String getRejectionReason() { return rejectionReason; }

    public String getFormattedDate() {
        if (createdAt == null) return "Recent";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.US);
        return sdf.format(createdAt);
    }

    public String getFormattedUpdatedDate() {
        if (statusUpdatedAt == null) return null;
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.US);
        return sdf.format(statusUpdatedAt);
    }

    public String getFormattedDeadline() {
        if (reservationDeadline == null) return null;
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.US);
        return sdf.format(reservationDeadline);
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setStatusUpdatedAt(Date statusUpdatedAt) {
        this.statusUpdatedAt = statusUpdatedAt;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public void setReservationDeadline(Date reservationDeadline) {
        this.reservationDeadline = reservationDeadline;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
