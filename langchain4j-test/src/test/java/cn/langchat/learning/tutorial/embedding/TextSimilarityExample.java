package cn.langchat.learning.tutorial.embedding;

import cn.langchat.learning.util.TestModelProvider;
import cn.langchat.learning.tutorial.embedding.utils.VectorUtils;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文本相似度示例 - 可运行示例
 * 
 * 演示如何使用 EmbeddingModel 计算文本相似度
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
public class TextSimilarityExample {

    private final EmbeddingModel embeddingModel;

    public TextSimilarityExample() {
        this.embeddingModel = TestModelProvider.getEmbeddingModel();
    }

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                     文本相似度示例                                    ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════╣");
        System.out.println();

        TestModelProvider.printConfig();

        TextSimilarityExample example = new TextSimilarityExample();

        // 示例 1：计算两个文本的相似度
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║ 示例 1: 计算两个文本的相似度                               ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════╣\n");

        String text1 = "我喜欢编程";
        String text2 = "我喜欢写代码";
        double similarity1 = example.calculateSimilarity(text1, text2);

        System.out.println("文本 1: \"" + text1 + "\"");
        System.out.println("文本 2: \"" + text2 + "\"");
        System.out.printf("相似度: %.4f%n", similarity1);
        System.out.println();

        // 示例 2：找到最相似的文本
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║ 示例 2: 找到最相似的文本                                    ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════╣\n");

        String query = "Java 编程";
        List<String> candidates = List.of(
            "Python 编程",
            "JavaScript 开发",
            "Java 开发",
            "C++ 编程",
            "前端开发"
        );

        String mostSimilar = example.findMostSimilar(query, candidates);

        System.out.println("查询: \"" + query + "\"");
        System.out.println("最相似: \"" + mostSimilar + "\"");
        System.out.println();
        System.out.println("所有候选文本及其相似度:");
        for (String candidate : candidates) {
            double sim = example.calculateSimilarity(query, candidate);
            System.out.printf("  \"%s\": %.4f%n", candidate, sim);
        }

        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║ ✅ 示例执行完成                                              ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════╝");
    }

    /**
     * 计算两个文本的相似度
     */
    public double calculateSimilarity(String text1, String text2) {
        // 获取两个文本的 Embedding
        float[] vector1 = getEmbedding(text1);
        float[] vector2 = getEmbedding(text2);

        // 计算余弦相似度
        return VectorUtils.cosineSimilarity(vector1, vector2);
    }

    /**
     * 找到最相似的文本
     */
    public String findMostSimilar(String query, List<String> candidates) {
        // 获取查询文本的 Embedding
        float[] queryVector = getEmbedding(query);

        // 获取所有候选文本的 Embedding
        List<float[]> candidateVectors = candidates.stream()
                .map(this::getEmbedding)
                .collect(Collectors.toList());

        // 找到最相似的
        VectorUtils.SimilarityResult result =
            VectorUtils.findMostSimilar(queryVector, candidateVectors);

        return candidates.get(result.getIndex());
    }

    private float[] getEmbedding(String text) {
        Response<Embedding> response = embeddingModel.embed(text);
        Embedding embedding = response.content();
        return embedding.vector();
    }
}
