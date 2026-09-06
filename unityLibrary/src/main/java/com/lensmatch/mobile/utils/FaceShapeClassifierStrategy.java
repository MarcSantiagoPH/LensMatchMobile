package com.lensmatch.mobile.utils;

import java.util.Map;

/**
 * Strategy interface for face shape classification.
 * Allows transparent swapping between the explainable rule-based decision tree
 * and future machine learning / deep learning classifiers without altering the pipeline.
 */
public interface FaceShapeClassifierStrategy {

    class ClassificationResult {
        public final String primaryShape;
        public final float primaryConfidence;
        public final String runnerUpShape;
        public final float runnerUpConfidence;
        public final boolean isBorderline;
        public final Map<String, Float> shapeProbabilities;
        public final String notes;

        public ClassificationResult(String primaryShape, float primaryConfidence,
                                    String runnerUpShape, float runnerUpConfidence,
                                    boolean isBorderline, Map<String, Float> shapeProbabilities,
                                    String notes) {
            this.primaryShape = primaryShape;
            this.primaryConfidence = primaryConfidence;
            this.runnerUpShape = runnerUpShape;
            this.runnerUpConfidence = runnerUpConfidence;
            this.isBorderline = isBorderline;
            this.shapeProbabilities = shapeProbabilities;
            this.notes = notes;
        }

        public String getPrimaryShape() { return primaryShape; }
        public float getPrimaryConfidence() { return primaryConfidence; }
        public String getRunnerUpShape() { return runnerUpShape; }
        public float getRunnerUpConfidence() { return runnerUpConfidence; }
        public boolean isBorderline() { return isBorderline; }
        public Map<String, Float> getShapeProbabilities() { return shapeProbabilities; }
        public String getNotes() { return notes; }
    }

    ClassificationResult classify(FaceMetrics metrics);
}
