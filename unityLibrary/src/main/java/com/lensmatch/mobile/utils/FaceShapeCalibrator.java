package com.lensmatch.mobile.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Anthropometric calibration utility.
 * Accepts datasets of labeled facial measurements (e.g. from Filipino / Southeast Asian photo corpora),
 * computes empirical 10th, 50th (median), and 90th percentiles per shape,
 * and generates recommended thresholds for FaceShapeConfig.
 */
public class FaceShapeCalibrator {

    public static class FaceSample {
        public final String shape;
        public final float widthToHeight;
        public final float jawToCheek;
        public final float foreheadToJaw;
        public final float foreheadToCheek;
        public final float jawAngle;
        public final float chinCurvature;

        public FaceSample(String shape, float widthToHeight, float jawToCheek,
                          float foreheadToJaw, float foreheadToCheek,
                          float jawAngle, float chinCurvature) {
            this.shape = shape;
            this.widthToHeight = widthToHeight;
            this.jawToCheek = jawToCheek;
            this.foreheadToJaw = foreheadToJaw;
            this.foreheadToCheek = foreheadToCheek;
            this.jawAngle = jawAngle;
            this.chinCurvature = chinCurvature;
        }

        public static FaceSample fromFaceMetrics(String shape, FaceMetrics m) {
            return new FaceSample(
                    shape,
                    m.widthToHeightRatio,
                    m.jawToCheekboneRatio,
                    m.foreheadToJawRatio,
                    m.foreheadToCheekRatio,
                    m.jawAngleScore,
                    m.chinCurvatureScore
            );
        }
    }

    public static class Percentiles {
        public final float p10;
        public final float p50; // Median
        public final float p90;

        public Percentiles(float p10, float p50, float p90) {
            this.p10 = p10;
            this.p50 = p50;
            this.p90 = p90;
        }

        @Override
        public String toString() {
            return String.format("[p10=%.3f, p50=%.3f, p90=%.3f]", p10, p50, p90);
        }
    }

    public static class ShapeCalibrationReport {
        public final String shape;
        public final int sampleCount;
        public final Percentiles widthToHeight;
        public final Percentiles jawToCheek;
        public final Percentiles foreheadToJaw;
        public final Percentiles foreheadToCheek;
        public final Percentiles jawAngle;
        public final Percentiles chinCurvature;

        public ShapeCalibrationReport(String shape, int sampleCount,
                                      Percentiles widthToHeight,
                                      Percentiles jawToCheek,
                                      Percentiles foreheadToJaw,
                                      Percentiles foreheadToCheek,
                                      Percentiles jawAngle,
                                      Percentiles chinCurvature) {
            this.shape = shape;
            this.sampleCount = sampleCount;
            this.widthToHeight = widthToHeight;
            this.jawToCheek = jawToCheek;
            this.foreheadToJaw = foreheadToJaw;
            this.foreheadToCheek = foreheadToCheek;
            this.jawAngle = jawAngle;
            this.chinCurvature = chinCurvature;
        }
    }

    private final List<FaceSample> samples = new ArrayList<>();

    public void addSample(FaceSample sample) {
        if (sample != null && sample.shape != null) {
            samples.add(sample);
        }
    }

    /**
     * Parses CSV rows with format:
     * shape,width_to_height,jaw_to_cheek,forehead_to_jaw,forehead_to_cheek,jaw_angle,chin_curvature
     */
    public void parseCsv(String csvContent) {
        if (csvContent == null || csvContent.trim().isEmpty()) return;
        String[] lines = csvContent.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("shape") || line.startsWith("#")) continue;
            String[] tokens = line.split(",");
            if (tokens.length >= 7) {
                try {
                    String shape = tokens[0].trim();
                    float w2h = Float.parseFloat(tokens[1].trim());
                    float j2c = Float.parseFloat(tokens[2].trim());
                    float f2j = Float.parseFloat(tokens[3].trim());
                    float f2c = Float.parseFloat(tokens[4].trim());
                    float jAngle = Float.parseFloat(tokens[5].trim());
                    float cCurv = Float.parseFloat(tokens[6].trim());
                    addSample(new FaceSample(shape, w2h, j2c, f2j, f2c, jAngle, cCurv));
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    public Map<String, ShapeCalibrationReport> calibrate() {
        Map<String, List<FaceSample>> grouped = new HashMap<>();
        for (FaceSample s : samples) {
            grouped.computeIfAbsent(s.shape, k -> new ArrayList<>()).add(s);
        }

        Map<String, ShapeCalibrationReport> reports = new HashMap<>();
        for (Map.Entry<String, List<FaceSample>> entry : grouped.entrySet()) {
            String shape = entry.getKey();
            List<FaceSample> list = entry.getValue();

            List<Float> w2h = new ArrayList<>();
            List<Float> j2c = new ArrayList<>();
            List<Float> f2j = new ArrayList<>();
            List<Float> f2c = new ArrayList<>();
            List<Float> jAngle = new ArrayList<>();
            List<Float> cCurv = new ArrayList<>();

            for (FaceSample s : list) {
                w2h.add(s.widthToHeight);
                j2c.add(s.jawToCheek);
                f2j.add(s.foreheadToJaw);
                f2c.add(s.foreheadToCheek);
                jAngle.add(s.jawAngle);
                cCurv.add(s.chinCurvature);
            }

            reports.put(shape, new ShapeCalibrationReport(
                    shape,
                    list.size(),
                    calcPercentiles(w2h),
                    calcPercentiles(j2c),
                    calcPercentiles(f2j),
                    calcPercentiles(f2c),
                    calcPercentiles(jAngle),
                    calcPercentiles(cCurv)
            ));
        }

        return reports;
    }

    public static Percentiles calcPercentiles(List<Float> values) {
        if (values == null || values.isEmpty()) {
            return new Percentiles(0f, 0f, 0f);
        }
        Collections.sort(values);
        float p10 = getPercentileValue(values, 10);
        float p50 = getPercentileValue(values, 50);
        float p90 = getPercentileValue(values, 90);
        return new Percentiles(p10, p50, p90);
    }

    private static float getPercentileValue(List<Float> sorted, double percentile) {
        if (sorted.isEmpty()) return 0f;
        if (sorted.size() == 1) return sorted.get(0);
        double rank = (percentile / 100.0) * (sorted.size() - 1);
        int lowerIndex = (int) Math.floor(rank);
        int upperIndex = (int) Math.ceil(rank);
        if (lowerIndex == upperIndex) {
            return sorted.get(lowerIndex);
        }
        double weight = rank - lowerIndex;
        return (float) (sorted.get(lowerIndex) * (1.0 - weight) + sorted.get(upperIndex) * weight);
    }

    public static void main(String[] args) {
        System.out.println("FaceShapeCalibrator initialized. Load CSV data to generate FaceShapeConfig values.");
    }
}
