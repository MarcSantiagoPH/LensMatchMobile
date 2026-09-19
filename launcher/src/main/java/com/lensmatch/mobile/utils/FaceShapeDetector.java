package com.lensmatch.mobile.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FaceShapeDetector {

    public static class ShapeRecommendation {
        public final List<String> primary;
        public final List<String> secondary;
        public final String explanation;

        public ShapeRecommendation(List<String> primary, List<String> secondary, String explanation) {
            this.primary = primary;
            this.secondary = secondary;
            this.explanation = explanation;
        }
    }

    public static ShapeRecommendation getRecommendation(String shape) {
        return getRecommendation(new TFLiteFaceDetector.FaceShapeResult(shape, 0.9f, null, false, null, null));
    }

    public static ShapeRecommendation getRecommendation(TFLiteFaceDetector.FaceShapeResult result) {
        String shape = result != null ? result.getShape() : "Oval";
        if (shape == null) shape = "Oval";
        FaceMetrics metrics = result != null ? result.getMetrics() : null;

        List<String> primary = new ArrayList<>();
        List<String> secondary = new ArrayList<>();
        String explanation = "";

        switch (shape) {
            case "Oval":
                primary.addAll(Arrays.asList("Rectangle", "Wayfarer", "Square", "Geometric", "Browline"));
                secondary.addAll(Arrays.asList("Cat-Eye", "Aviator"));
                explanation = "Your balanced facial proportions work well with many frame styles. Rectangle, Wayfarer, geometric, and browline frames can add structure while maintaining your natural balance.";
                break;
            case "Round":
                primary.addAll(Arrays.asList("Rectangle", "Square", "Geometric", "Browline", "Wayfarer"));
                secondary.addAll(Arrays.asList("Cat-Eye"));
                explanation = "Angular frame shapes can add definition and contrast to your softer facial contours.";
                
                // Personalization: If face is a bit longer than a typical round face, prioritize Rectangle/Geometric even more.
                if (metrics != null && metrics.faceLengthToWidthRatio > 1.35f) {
                    primary.remove("Square"); 
                }
                break;
            case "Square":
                primary.addAll(Arrays.asList("Round", "Oval", "Aviator"));
                secondary.addAll(Arrays.asList("Cat-Eye", "Browline"));
                explanation = "Rounded and curved frames can provide visual balance against a stronger or more angular jawline.";
                break;
            case "Oblong":
                primary.addAll(Arrays.asList("Round", "Oval", "Aviator", "Browline"));
                secondary.addAll(Arrays.asList("Wayfarer", "Geometric"));
                explanation = "Frames with more height and visual depth can complement your longer facial proportions.";
                break;
            case "Heart":
                primary.addAll(Arrays.asList("Round", "Oval", "Aviator", "Cat-Eye"));
                secondary.addAll(Arrays.asList("Browline"));
                explanation = "Frames with softer curves and balanced proportions may complement your wider upper face and tapered jaw.";
                break;
            case "Diamond":
                primary.addAll(Arrays.asList("Oval", "Round", "Cat-Eye", "Browline"));
                secondary.addAll(Arrays.asList("Aviator", "Geometric"));
                explanation = "Soft curves and upper-emphasized styles can complement your prominent cheekbones and balanced facial structure.";
                break;
            case "Triangle":
                primary.addAll(Arrays.asList("Browline", "Cat-Eye", "Aviator", "Wayfarer"));
                secondary.addAll(Arrays.asList("Geometric"));
                explanation = "Frames with more visual weight around the upper rim may help balance a stronger or wider jawline.";
                break;
            default:
                primary.addAll(Arrays.asList("Rectangle", "Wayfarer", "Geometric"));
                secondary.addAll(Arrays.asList("Oval", "Round"));
                explanation = "These versatile frames suit most face shapes by balancing natural proportions.";
                break;
        }

        // Limit primary to 3, and secondary to 1-2 as requested.
        while (primary.size() > 3) {
            primary.remove(primary.size() - 1);
        }
        while (secondary.size() > 2) {
            secondary.remove(secondary.size() - 1);
        }

        return new ShapeRecommendation(primary, secondary, explanation);
    }
}
