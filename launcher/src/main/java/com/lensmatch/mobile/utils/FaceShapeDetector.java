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
        FaceMetrics fallbackMetrics = null;
        try {
            fallbackMetrics = com.lensmatch.mobile.data.AppState.getInstance().getLastMetrics();
        } catch (Exception ignored) {}
        return getRecommendation(new TFLiteFaceDetector.FaceShapeResult(shape, 0.9f, null, false, null, fallbackMetrics));
    }

    public static ShapeRecommendation getRecommendation(TFLiteFaceDetector.FaceShapeResult result) {
        String shape = result != null ? result.getShape() : "Oval";
        if (shape == null) shape = "Oval";
        FaceMetrics metrics = result != null ? result.getMetrics() : null;
        if (metrics == null) {
            try {
                metrics = com.lensmatch.mobile.data.AppState.getInstance().getLastMetrics();
            } catch (Exception ignored) {}
        }

        List<String> primary = new ArrayList<>();
        List<String> secondary = new ArrayList<>();
        String explanation = "";

        switch (shape) {
            case "Oval":
                primary.addAll(Arrays.asList("Rectangle", "Wayfarer", "Square", "Geometric", "Browline"));
                secondary.addAll(Arrays.asList("Cat-Eye", "Aviator"));
                explanation = "Your balanced facial proportions work well with many frame styles. Rectangle, Wayfarer, geometric, and browline frames can add structure while maintaining your natural balance.";

                // Dynamic personalization: Narrow or elongated oval
                if (metrics != null && metrics.faceLengthToWidthRatio > 1.50f) {
                    primary.remove("Wayfarer");
                    primary.remove("Square");
                    if (!primary.contains("Round")) {
                        secondary.add(0, "Round");
                    }
                    explanation += " Because your face is slightly longer and narrower, softer frames with vertical depth help balance your features without overwhelming your facial width.";
                }
                break;

            case "Round":
                primary.addAll(Arrays.asList("Rectangle", "Square", "Geometric", "Browline", "Wayfarer"));
                secondary.addAll(Arrays.asList("Cat-Eye"));
                explanation = "Angular frame shapes can add definition and contrast to your softer facial contours.";

                if (metrics != null) {
                    if (metrics.faceLengthToWidthRatio > 1.35f) {
                        primary.remove("Square");
                    } else if (metrics.faceLengthToWidthRatio <= 1.20f) {
                        primary.remove("Rectangle");
                        primary.add(0, "Rectangle");
                        primary.remove("Geometric");
                        primary.add(1, "Geometric");
                        explanation += " Structured angular and geometric frames add sharp definition to your facial contours.";
                    }
                }
                break;

            case "Square":
                primary.addAll(Arrays.asList("Round", "Oval", "Aviator"));
                secondary.addAll(Arrays.asList("Cat-Eye", "Browline"));
                explanation = "Rounded and curved frames can provide visual balance against a stronger or more angular jawline.";

                // Dynamic personalization: Sharp mandibular angle or broad jaw
                if (metrics != null && (metrics.jawAngle < 120.0f || metrics.jawToCheekRatio > 0.85f)) {
                    primary.remove("Round");
                    primary.add(0, "Round");
                    if (!primary.contains("Oval")) {
                        primary.add(1, "Oval");
                    }
                    explanation += " Given your well-defined, sharp jawline, curved round and oval frames provide the ideal visual softening and contrast.";
                }
                break;

            case "Oblong":
                primary.addAll(Arrays.asList("Round", "Oval", "Aviator", "Browline"));
                secondary.addAll(Arrays.asList("Wayfarer", "Geometric"));
                explanation = "Frames with more height and visual depth can complement your longer facial proportions.";

                if (metrics != null && metrics.faceLengthToWidthRatio > 1.60f) {
                    primary.remove("Browline");
                    primary.add(0, "Browline");
                    primary.remove("Wayfarer");
                    primary.add(1, "Wayfarer");
                    explanation += " Deeper frames with prominent browlines break up the vertical length of your face gracefully.";
                }
                break;

            case "Heart":
                primary.addAll(Arrays.asList("Round", "Oval", "Aviator", "Cat-Eye"));
                secondary.addAll(Arrays.asList("Browline"));
                explanation = "Frames with softer curves and balanced proportions may complement your wider upper face and tapered jaw.";

                if (metrics != null && (metrics.foreheadToCheekRatio > 0.98f || metrics.jawToCheekRatio < 0.72f)) {
                    primary.remove("Aviator");
                    primary.add(0, "Aviator");
                    primary.remove("Round");
                    primary.add(1, "Round");
                    explanation += " Bottom-accented and aviator shapes bring visual harmony to your delicate, tapered jawline.";
                }
                break;

            case "Diamond":
                primary.addAll(Arrays.asList("Oval", "Round", "Cat-Eye", "Browline"));
                secondary.addAll(Arrays.asList("Aviator", "Geometric"));
                explanation = "Soft curves and upper-emphasized styles can complement your prominent cheekbones and balanced facial structure.";

                if (metrics != null && metrics.foreheadToCheekRatio < 0.85f) {
                    primary.remove("Browline");
                    primary.add(0, "Browline");
                    primary.remove("Cat-Eye");
                    primary.add(1, "Cat-Eye");
                    explanation += " Browline and cat-eye styles add width to your temple area, complementing your defined cheekbones.";
                }
                break;

            case "Triangle":
                primary.addAll(Arrays.asList("Browline", "Cat-Eye", "Aviator", "Wayfarer"));
                secondary.addAll(Arrays.asList("Geometric"));
                explanation = "Frames with more visual weight around the upper rim may help balance a stronger or wider jawline.";

                if (metrics != null && metrics.jawToCheekRatio > 0.85f) {
                    primary.remove("Browline");
                    primary.add(0, "Browline");
                    primary.remove("Cat-Eye");
                    primary.add(1, "Cat-Eye");
                    explanation += " Top-heavy browline and cat-eye frames balance your wide jawline by drawing visual emphasis toward your eyes.";
                }
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
