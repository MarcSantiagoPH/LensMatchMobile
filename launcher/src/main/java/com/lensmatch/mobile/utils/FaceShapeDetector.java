package com.lensmatch.mobile.utils;

public class FaceShapeDetector {
    public static class ShapeRecommendation {
        public final String[] recommended;
        public final String[] avoided;
        public final String description;

        public ShapeRecommendation(String[] recommended, String[] avoided, String description) {
            this.recommended = recommended;
            this.avoided = avoided;
            this.description = description;
        }
    }

    public static ShapeRecommendation getRecommendationForShape(String shape) {
        if (shape == null) shape = "Oval";

        switch (shape) {
            case "Round":
                return new ShapeRecommendation(
                        new String[]{"Rectangle", "Square", "Geometric"},
                        new String[]{"Round", "Oval"},
                        "Angular frames like Rectangle and Square help contrast soft facial contours, adding definition and balancing your round face shape."
                );
            case "Square":
                return new ShapeRecommendation(
                        new String[]{"Round", "Cat Eye", "Oval", "Aviator", "Browline"},
                        new String[]{"Square", "Rectangle"},
                        "Curved and rounded frames soften strong jawlines and angular proportions, creating a balanced, harmonious look for square faces."
                );
            case "Heart":
                return new ShapeRecommendation(
                        new String[]{"Cat Eye", "Rectangle", "Wayfarer", "Oval", "Browline"},
                        new String[]{"Geometric"},
                        "Frames that are slightly wider than your forehead or feature bottom-heavy details help balance a broader forehead and narrower chin."
                );
            case "Oblong":
                return new ShapeRecommendation(
                        new String[]{"Wayfarer", "Geometric", "Browline", "Square"},
                        new String[]{"Rectangle"},
                        "Tall, oversized, or deep frames break up facial length and add width, making oblong faces appear more balanced and proportional."
                );
            case "Diamond":
                return new ShapeRecommendation(
                        new String[]{"Oval", "Cat Eye", "Round"},
                        new String[]{"Square"},
                        "Subtle curved frames highlight high cheekbones while softening cheek angles for diamond face structures."
                );
            case "Triangle":
                return new ShapeRecommendation(
                        new String[]{"Round", "Cat Eye", "Wayfarer", "Square", "Aviator"},
                        new String[]{"Rectangle"},
                        "Detailed browlines and cat-eye designs draw focus upwards, balancing a wider jawline."
                );
            case "Oval":
            default:
                return new ShapeRecommendation(
                        new String[]{"Rectangle", "Wayfarer", "Square", "Aviator", "Cat Eye", "Round", "Geometric", "Browline", "Oval"},
                        new String[]{},
                        "Oval faces are naturally balanced, making almost any frame shape look stylish and flattering on you!"
                );
        }
    }
}
