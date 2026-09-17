package com.lensmatch.mobile.utils;

public class FaceShapeDetector {
    public static class ShapeRecommendation {
        public final String[] recommended;
        public final String[] avoided;
        public final String description;
        public final String recommendedReason;
        public final String avoidedReason;

        public ShapeRecommendation(String[] recommended, String[] avoided, String description) {
            this(recommended, avoided, description,
                    "Flattering frame silhouettes that balance your unique facial geometry.",
                    "Styles that may visually conflict with or overpower your natural facial symmetry.");
        }

        public ShapeRecommendation(String[] recommended, String[] avoided, String description,
                                   String recommendedReason, String avoidedReason) {
            this.recommended = recommended;
            this.avoided = avoided;
            this.description = description;
            this.recommendedReason = recommendedReason;
            this.avoidedReason = avoidedReason;
        }
    }

    public static ShapeRecommendation getRecommendationForShape(String shape) {
        if (shape == null) shape = "Oval";

        switch (shape) {
            case "Round":
                return new ShapeRecommendation(
                        new String[]{"Rectangle", "Square", "Geometric"},
                        new String[]{"Round", "Oval"},
                        "Angular frames like Rectangle and Square help contrast soft facial contours, adding definition and balancing your round face shape.",
                        "Sharp rectangular and angular edges contrast soft curves, adding structure and making your face look slimmer.",
                        "Circular and oval frames echo facial roundness, exaggerating fullness without adding structure."
                );
            case "Square":
                return new ShapeRecommendation(
                        new String[]{"Round", "Cat Eye", "Oval", "Aviator", "Browline"},
                        new String[]{"Square", "Rectangle"},
                        "Curved and rounded frames soften strong jawlines and angular proportions, creating a balanced, harmonious look for square faces.",
                        "Rounded and curving silhouettes soften angular jawlines and broad foreheads, creating natural balance.",
                        "Boxy square and sharp rectangular frames duplicate hard angles, making features look rigid."
                );
            case "Heart":
                return new ShapeRecommendation(
                        new String[]{"Cat Eye", "Rectangle", "Wayfarer", "Oval", "Browline"},
                        new String[]{"Geometric", "Heavy Browline"},
                        "Frames that are slightly wider than your forehead or feature bottom-heavy details help balance a broader forehead and narrower chin.",
                        "Light, bottom-balanced frames draw gentle attention downward to complement a pointed chin.",
                        "Top-heavy or oversized geometric frames add bulk across the brow, making the forehead appear excessively broad."
                );
            case "Oblong":
                return new ShapeRecommendation(
                        new String[]{"Wayfarer", "Geometric", "Browline", "Square"},
                        new String[]{"Rectangle", "Small Narrow"},
                        "Tall, oversized, or deep frames break up facial length and add width, making oblong faces appear more balanced and proportional.",
                        "Deep, taller frames break vertical face length and create proportional horizontal breadth.",
                        "Narrow, horizontal rectangular frames emphasize facial length, making the face look longer."
                );
            case "Diamond":
                return new ShapeRecommendation(
                        new String[]{"Oval", "Cat Eye", "Round", "Browline"},
                        new String[]{"Square", "Sharp Rectangle"},
                        "Subtle curved frames highlight high cheekbones while softening cheek angles for diamond face structures.",
                        "Curved rims and lifted cat-eye tops accent cheekbone beauty while softening narrow temples and jaw.",
                        "Sharp, narrow square frames clash with prominent cheekbones and make temples look narrower."
                );
            case "Triangle":
                return new ShapeRecommendation(
                        new String[]{"Round", "Cat Eye", "Wayfarer", "Square", "Aviator"},
                        new String[]{"Rectangle", "Narrow Bottom"},
                        "Detailed browlines and cat-eye designs draw focus upwards, balancing a wider jawline.",
                        "Decorative top brows and cat-eye wings draw eyes upward to balance a wider jawline.",
                        "Bottom-heavy or rimless top frames draw focus to the lower jaw, exaggerating triangular width."
                );
            case "Oval":
            default:
                return new ShapeRecommendation(
                        new String[]{"Rectangle", "Wayfarer", "Square", "Aviator", "Cat Eye", "Round", "Geometric", "Browline", "Oval"},
                        new String[]{"Oversized Geometric"},
                        "Oval faces feature naturally balanced proportions, making almost any frame shape look stylish and flattering on you!",
                        "Naturally symmetrical proportions allow you to rock angular, curved, or classic silhouettes with ease.",
                        "Extremely oversized or excessively bulky frames can overwhelm your delicate facial balance."
                );
        }
    }
}
