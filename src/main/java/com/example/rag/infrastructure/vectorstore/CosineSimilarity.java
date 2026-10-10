package com.example.rag.infrastructure.vectorstore;

public final class CosineSimilarity {

    private CosineSimilarity() {
    }

    public static double calculate(float[] left, float[] right) {
        validate(left, right);

        double dotProduct = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;

        for (int i = 0; i < left.length; i++) {
            double l = left[i];
            double r = right[i];
            dotProduct += l * r;
            leftNorm += l * l;
            rightNorm += r * r;
        }

        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private static void validate(float[] left, float[] right) {
        if (left == null || right == null || left.length == 0 || right.length == 0) {
            throw new IllegalArgumentException("向量不能为空");
        }
        if (left.length != right.length) {
            throw new IllegalArgumentException("向量维度不一致");
        }
    }
}
