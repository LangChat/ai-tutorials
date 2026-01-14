package cn.langchat.learning.tutorial.embedding;

import cn.langchat.learning.util.TestModelProvider;
import cn.langchat.learning.tutorial.embedding.utils.VectorUtils;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 07 - EmbeddingModel 和向量测试
 * 
 * 测试文档中的完整使用场景，包括：
 * - EmbeddingModel 的基本使用
 * - 单个文本嵌入
 * - 批量文本嵌入
 * - 向量相似度计算
 * - 查找最相似的向量
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
@DisplayName("07 - EmbeddingModel 和向量测试")
class EmbeddingModelTest {

    private EmbeddingModel embeddingModel;

    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        embeddingModel = TestModelProvider.getEmbeddingModel();
    }

    @Test
    @DisplayName("应该能嵌入单个文本")
    void shouldEmbedSingleText() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 嵌入单个文本                                             ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        String text = "Hello, LangChain4j!";
        
        Response<Embedding> response = embeddingModel.embed(text);
        
        assertNotNull(response);
        assertNotNull(response.content());
        
        Embedding embedding = response.content();
        assertNotNull(embedding.vector());
        
        float[] vector = embedding.vector();
        log.info("文本: {}", text);
        log.info("向量维度: {}", vector.length);
        log.info("向量（前10个）: {}", Arrays.toString(Arrays.copyOf(vector, 10)));
        log.info("\n✅ 测试通过：能够嵌入单个文本\n");
    }

    @Test
    @DisplayName("应该能嵌入多个文本")
    void shouldEmbedMultipleTexts() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 嵌入多个文本                                             ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        List<String> texts = List.of(
            "Java 编程语言",
            "Python 编程语言",
            "JavaScript 编程语言"
        );
        
        List<TextSegment> textSegments = texts.stream()
                .map(TextSegment::from)
                .collect(Collectors.toList());
        
        Response<List<Embedding>> response = embeddingModel.embedAll(textSegments);
        
        assertNotNull(response);
        assertNotNull(response.content());
        assertEquals(texts.size(), response.content().size());
        
        log.info("文本数量: {}", texts.size());
        log.info("生成的向量数: {}", response.content().size());
        
        for (int i = 0; i < texts.size(); i++) {
            log.info("  文本 {}: \"{}\"", i + 1, texts.get(i));
            log.info("    向量维度: {}", response.content().get(i).vector().length);
        }
        
        log.info("\n✅ 测试通过：能够嵌入多个文本\n");
    }

    @Test
    @DisplayName("应该能计算余弦相似度")
    void shouldCalculateCosineSimilarity() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 计算余弦相似度                                          ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        String text1 = "猫";
        String text2 = "狗";
        String text3 = "猫";  // 应该与 text1 相似度为 1
        
        float[] vector1 = getEmbedding(text1);
        float[] vector2 = getEmbedding(text2);
        float[] vector3 = getEmbedding(text3);
        
        double similarity12 = VectorUtils.cosineSimilarity(vector1, vector2);
        double similarity13 = VectorUtils.cosineSimilarity(vector1, vector3);
        
        log.info("文本 1: \"{}\"", text1);
        log.info("文本 2: \"{}\"", text2);
        log.info("文本 3: \"{}\"", text3);
        log.info("相似度 (1-2): {:.4f}", similarity12);
        log.info("相似度 (1-3): {:.4f}", similarity13);
        
        assertEquals(1.0, similarity13, 0.001, "相同文本的相似度应该为 1.0");
        assertTrue(similarity12 < 1.0, "不同文本的相似度应该小于 1.0");
        assertTrue(similarity12 > 0.5, "猫和狗都是动物，应该有一定相似度");
        
        log.info("\n✅ 测试通过：能够正确计算余弦相似度\n");
    }

    @Test
    @DisplayName("应该能找到最相似的文本")
    void shouldFindMostSimilarText() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 找到最相似的文本                                       ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        String query = "编程语言";
        List<String> candidates = List.of(
            "Python",
            "Java",
            "JavaScript",
            "C++",
            "Go"
        );
        
        // 获取查询文本的 Embedding
        float[] queryVector = getEmbedding(query);
        
        // 获取所有候选文本的 Embedding
        List<float[]> candidateVectors = candidates.stream()
                .map(this::getEmbedding)
                .collect(Collectors.toList());
        
        // 找到最相似的
        VectorUtils.SimilarityResult result = 
            VectorUtils.findMostSimilar(queryVector, candidateVectors);
        
        log.info("查询: \"{}\"", query);
        log.info("最相似的文本: \"{}\"", candidates.get(result.getIndex()));
        log.info("相似度: {:.4f}", result.getSimilarity());
        log.info("\n所有候选文本及其相似度:");
        for (int i = 0; i < candidates.size(); i++) {
            float[] candidateVector = candidateVectors.get(i);
            double similarity = VectorUtils.cosineSimilarity(queryVector, candidateVector);
            log.info("  \"{}\": {:.4f}", candidates.get(i), similarity);
        }
        
        assertNotNull(result);
        assertTrue(result.getSimilarity() > 0.5, "相似度应该较高");
        
        log.info("\n✅ 测试通过：能够找到最相似的文本\n");
    }

    @Test
    @DisplayName("应该能计算欧几里得距离")
    void shouldCalculateEuclideanDistance() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 计算欧几里得距离                                        ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        String text1 = "Java";
        String text2 = "Python";
        String text3 = "Java";  // 应该与 text1 距离为 0
        
        float[] vector1 = getEmbedding(text1);
        float[] vector2 = getEmbedding(text2);
        float[] vector3 = getEmbedding(text3);
        
        double distance12 = VectorUtils.euclideanDistance(vector1, vector2);
        double distance13 = VectorUtils.euclideanDistance(vector1, vector3);
        
        log.info("文本 1: \"{}\"", text1);
        log.info("文本 2: \"{}\"", text2);
        log.info("文本 3: \"{}\"", text3);
        log.info("距离 (1-2): {:.4f}", distance12);
        log.info("距离 (1-3): {:.4f}", distance13);
        
        assertEquals(0.0, distance13, 0.001, "相同文本的距离应该为 0.0");
        assertTrue(distance12 > 0.0, "不同文本的距离应该大于 0.0");
        
        log.info("\n✅ 测试通过：能够正确计算欧几里得距离\n");
    }

    @Test
    @DisplayName("应该能验证向量质量")
    void shouldValidateVectorQuality() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 验证向量质量                                             ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        String text = "测试文本";
        float[] vector = getEmbedding(text);
        
        log.info("文本: \"{}\"", text);
        log.info("向量维度: {}", vector.length);
        
        // 验证向量不包含无效值
        boolean hasNaN = false;
        boolean hasInfinite = false;
        boolean hasZero = false;
        
        for (float value : vector) {
            if (Float.isNaN(value)) {
                hasNaN = true;
            }
            if (Float.isInfinite(value)) {
                hasInfinite = true;
            }
            if (value == 0.0f) {
                hasZero = true;
            }
        }
        
        assertFalse(hasNaN, "向量不应包含 NaN");
        assertFalse(hasInfinite, "向量不应包含无穷大");
        assertTrue(hasZero, "向量通常应该包含一些零值（稀疏性）");
        
        log.info("包含 NaN: {}", hasNaN);
        log.info("包含无穷大: {}", hasInfinite);
        log.info("包含零值: {}", hasZero);
        log.info("\n✅ 测试通过：向量质量符合预期\n");
    }

    /**
     * 获取文本的 Embedding
     */
    private float[] getEmbedding(String text) {
        Response<Embedding> response = embeddingModel.embed(text);
        Embedding embedding = response.content();
        return embedding.vector();
    }
}
