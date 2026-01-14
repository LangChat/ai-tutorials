package cn.langchat.learning.tutorial.embedding.utils;

import java.util.List;

/**
 * 向量工具类
 * 
 * 提供向量相似度计算、距离计算等常用方法
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
public class VectorUtils {

    private VectorUtils() {
        // 工具类，不允许实例化
    }

    /**
     * 计算余弦相似度
     * 
     * 余弦相似度是最常用的向量相似度计算方法
     * 值域：[-1, 1]，1 表示完全相同，-1 表示完全相反，0 表示不相关
     * 
     * @param vector1 向量 1
     * @param vector2 向量 2
     * @return 相似度（-1 到 1 之间）
     */
    public static double cosineSimilarity(float[] vector1, float[] vector2) {
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("向量长度必须相同");
        }

        // 1. 计算点积
        double dotProduct = 0.0;
        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
        }

        // 2. 计算向量的模（长度）
        double norm1 = 0.0;
        double norm2 = 0.0;
        for (int i = 0; i < vector1.length; i++) {
            norm1 += Math.pow(vector1[i], 2);
            norm2 += Math.pow(vector2[i], 2);
        }
        norm1 = Math.sqrt(norm1);
        norm2 = Math.sqrt(norm2);

        // 3. 计算余弦相似度
        return dotProduct / (norm1 * norm2);
    }

    /**
     * 计算欧几里得距离
     * 
     * 值域：[0, +∞)，0 表示完全相同，值越大越不相似
     * 
     * @param vector1 向量 1
     * @param vector2 向量 2
     * @return 距离（0 到 +∞）
     */
    public static double euclideanDistance(float[] vector1, float[] vector2) {
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("向量长度必须相同");
        }

        double sum = 0.0;
        for (int i = 0; i < vector1.length; i++) {
            double diff = vector1[i] - vector2[i];
            sum += Math.pow(diff, 2);
        }
        return Math.sqrt(sum);
    }

    /**
     * 找到最相似的向量
     * 
     * @param query 查询向量
     * @param candidates 候选向量列表
     * @return 最相似的向量和其索引
     */
    public static SimilarityResult findMostSimilar(float[] query, List<float[]> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("候选向量列表不能为空");
        }

        int bestIndex = 0;
        double bestSimilarity = -1.0;

        for (int i = 0; i < candidates.size(); i++) {
            double similarity = cosineSimilarity(query, candidates.get(i));
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestIndex = i;
            }
        }

        return new SimilarityResult(bestIndex, bestSimilarity);
    }

    /**
     * 找到最相似的向量（返回前K个）
     * 
     * @param query 查询向量
     * @param candidates 候选向量列表
     * @param topK 返回前K个最相似的
     * @return 最相似的向量列表（按相似度降序排列）
     */
    public static List<SimilarityResult> findTopKSimilar(
            float[] query, 
            List<float[]> candidates, 
            int topK) {
        
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("候选向量列表不能为空");
        }

        if (topK <= 0 || topK > candidates.size()) {
            topK = candidates.size();
        }

        // 计算所有相似度
        java.util.List<SimilarityResult> results = new java.util.ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            double similarity = cosineSimilarity(query, candidates.get(i));
            results.add(new SimilarityResult(i, similarity));
        }

        // 按相似度降序排序
        results.sort((a, b) -> Double.compare(b.similarity, a.similarity));

        // 返回前K个
        return results.subList(0, topK);
    }

    /**
     * 计算向量的模（长度）
     * 
     * @param vector 向量
     * @return 向量的模
     */
    public static double norm(float[] vector) {
        double sum = 0.0;
        for (float value : vector) {
            sum += Math.pow(value, 2);
        }
        return Math.sqrt(sum);
    }

    /**
     * 归一化向量
     * 
     * 将向量转换为单位向量（长度为1）
     * 
     * @param vector 原始向量
     * @return 归一化后的向量
     */
    public static float[] normalize(float[] vector) {
        double normValue = norm(vector);
        float[] normalized = new float[vector.length];
        
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = (float) (vector[i] / normValue);
        }
        
        return normalized;
    }

    /**
     * 相似度结果类
     */
    public static class SimilarityResult {
        private final int index;
        private final double similarity;

        public SimilarityResult(int index, double similarity) {
            this.index = index;
            this.similarity = similarity;
        }

        public int getIndex() {
            return index;
        }

        public double getSimilarity() {
            return similarity;
        }

        @Override
        public String toString() {
            return String.format("SimilarityResult{index=%d, similarity=%.4f}", index, similarity);
        }
    }
}
