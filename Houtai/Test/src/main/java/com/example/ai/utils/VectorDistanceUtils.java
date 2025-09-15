package com.example.ai.utils;

public class VectorDistanceUtils {
    // 防止实例化
    private VectorDistanceUtils() {}

    // 浮点数计算精度阈值
    private static final double EPSILON = 1e-12;

    /**
     * 计算欧氏距离
     * @param vectorA 向量A (非空且与B等长)
     * @param vectorB 向量B (非空且与A等长)
     * @return 欧氏距离
     * @throws IllegalArgumentException 参数不合法时抛出
     */
    public static double euclideanDistance(float[] vectorA, float[] vectorB) {
        validateVectors(vectorA, vectorB);

        double sum = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            double diff = vectorA[i] - vectorB[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    /**
     * 计算两个向量的余弦距离。
     *
     * @param vectorA 向量A，必须是非空且与vectorB长度一致。
     * @param vectorB 向量B，必须是非空且与vectorA长度一致。
     * @return 范围[0,2]
     * @throws IllegalArgumentException 如果向量为空、长度不一致或为空向量，则抛出此异常。
     */
    public static double cosineDistance(float[] vectorA, float[] vectorB) {
        // 调用统一的向量校验方法，确保两个向量不为空、长度一致且非空。
        validateVectors(vectorA, vectorB);

        double dotProduct = 0.0; // 向量点积
        double normA = 0.0;      // 向量A的L2范数的平方
        double normB = 0.0;      // 向量B的L2范数的平方

        // 循环计算点积以及两个向量的模的平方
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        // 计算向量的L2范数（模）
        normA = Math.sqrt(normA);
        normB = Math.sqrt(normB);

        // 处理零向量情况，即向量所有元素均为0，它们的模为0。
        // 使用一个非常小的数（EPSILON）来处理浮点数计算的精度问题，
        // 避免因除以零而产生错误。
        // 如果向量为零向量，则抛出异常，因为余弦距离无意义。
        if (normA < EPSILON || normB < EPSILON) {
            throw new IllegalArgumentException("Vectors cannot be zero vectors");
        }

        // 处理浮点误差，确保计算结果在[-1, 1]范围内。
        // 余弦相似度 = 点积 / (||A|| * ||B||)
        double similarity = dotProduct / (normA * normB);
        similarity = Math.max(Math.min(similarity, 1.0), -1.0);

        // 余弦距离 = 1 - 余弦相似度
        return 1 - similarity;
    }

    /**
     * 统一的参数校验方法
     * @param a 向量a
     * @param b 向量b
     * @throws IllegalArgumentException 如果向量为空或者维度不一致
     */
    private static void validateVectors(float[] a, float[] b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Vectors cannot be null");
        }
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vectors must have same dimension");
        }
        if (a.length == 0) {
            throw new IllegalArgumentException("Vectors cannot be empty");
        }
    }
}
