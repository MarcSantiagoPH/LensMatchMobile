package com.lensmatch.mobile.data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FrameModel implements Serializable {
    private String id;
    private String frameName;
    private String brand;
    private String frameStyle;
    private double price;
    private String material;
    private String description;
    private String imageUrl;
    private String arModelUrl;
    private boolean availability = true;

    public static final List<String> APPROVED_FRAME_STYLES = Arrays.asList(
            "Round", "Cat-Eye", "Rectangle", "Wayfarer", "Square", "Aviator", "Geometric", "Browline", "Oval"
    );

    public FrameModel() {}

    public FrameModel(String id, String frameName, String brand, String frameStyle, double price,
                      String material, String description, String imageUrl, String arModelUrl, boolean availability) {
        this.id = id;
        this.frameName = frameName;
        this.brand = brand;
        this.frameStyle = frameStyle;
        this.price = price;
        this.material = material;
        this.description = description;
        this.imageUrl = imageUrl;
        this.arModelUrl = arModelUrl;
        this.availability = availability;
    }

    public FrameModel(String id, String name, String shape, String material, String price) {
        this.id = id;
        this.frameName = name;
        this.brand = "LensMatch";
        this.frameStyle = shape;
        this.material = material;
        this.price = parsePriceDouble(price);
        this.description = "Elevate your look with the " + name + ".";
        this.imageUrl = "";
        this.arModelUrl = "";
        this.availability = true;
    }

    public static FrameModel fromMap(Map<String, Object> data, String docId) {
        if (data == null) return new FrameModel();

        String id = docId != null ? docId : (String) data.get("id");
        String name = data.containsKey("frameName") ? String.valueOf(data.get("frameName"))
                : (data.containsKey("name") ? String.valueOf(data.get("name")) : "");
        String brand = data.containsKey("brand") ? String.valueOf(data.get("brand")) : "";
        String style = data.containsKey("frameStyle") ? String.valueOf(data.get("frameStyle"))
                : (data.containsKey("style") ? String.valueOf(data.get("style"))
                : (data.containsKey("shape") ? String.valueOf(data.get("shape")) : ""));
        String mat = data.containsKey("material") ? String.valueOf(data.get("material")) : "Acetate";
        String desc = data.containsKey("description") ? String.valueOf(data.get("description")) : "";
        String img = data.containsKey("imageUrl") ? String.valueOf(data.get("imageUrl")) : "";
        String arUrl = data.containsKey("arModelUrl") ? String.valueOf(data.get("arModelUrl")) : "";

        double priceVal = parsePriceDouble(data.get("price"));
        boolean isAvailable = parseAvailability(data.get("availability"));

        return new FrameModel(id, name, brand, style, priceVal, mat, desc, img, arUrl, isAvailable);
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

    private static boolean parseAvailability(Object raw) {
        if (raw instanceof Boolean) {
            return (Boolean) raw;
        } else if (raw instanceof String) {
            String lower = ((String) raw).trim().toLowerCase();
            return lower.equals("true") || lower.equals("available") || lower.equals("in stock");
        } else if (raw instanceof Number) {
            return ((Number) raw).intValue() != 0;
        }
        return true;
    }

    public String getId() { return id; }
    public String getFrameName() { return frameName != null ? frameName : ""; }
    public String getName() { return getFrameName(); }
    public String getBrand() { return brand != null ? brand : ""; }
    public String getFrameStyle() { return frameStyle != null ? frameStyle : ""; }
    public String getShape() { return getFrameStyle(); }
    public double getPriceValue() { return price; }
    public String getPrice() { return String.format("$%.2f", price); }
    public String getMaterial() { return material != null ? material : "Acetate"; }
    public String getDescription() { return description != null ? description : ""; }
    public String getImageUrl() { return imageUrl != null ? imageUrl : ""; }
    public String getArModelUrl() { return arModelUrl != null ? arModelUrl : ""; }
    public boolean isAvailability() { return availability; }
    public boolean isAvailable() { return availability; }

    public String getDisplayFrameStyle() {
        if (frameStyle == null || frameStyle.trim().isEmpty()) return "Standard";
        String trimmed = frameStyle.trim();
        for (String approved : APPROVED_FRAME_STYLES) {
            if (approved.equalsIgnoreCase(trimmed)) {
                return approved;
            }
        }
        return trimmed;
    }
}
