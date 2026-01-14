package cn.langchat.learning.tutorial.embeddingstore;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 08 - EmbeddingStore 向量存储测试
 * <p>
 * 测试文档中的完整使用场景，包括：
 * - EmbeddingStore 的基本使用
 * - 添加和检索嵌入
 * - 向量相似度搜索
 * - 元数据管理
 * - 搜索参数控制
 *
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
@DisplayName("08 - EmbeddingStore 向量存储测试")
class EmbeddingStoreTest {

    private EmbeddingModel embeddingModel;
    private InMemoryEmbeddingStore<TextSegment> embeddingStore;

    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        embeddingModel = TestModelProvider.getEmbeddingModel();
        embeddingStore = new InMemoryEmbeddingStore<>();
    }

    @Test
    @DisplayName("应该能添加和检索嵌入")
    void shouldAddAndRetrieveEmbedding() {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 添加和检索嵌入                                         ║");
        log.info("╚═════════════════════════════════════════════════════════════════════╣\n");

        // 添加嵌入
        TextSegment segment = TextSegment.from("测试文本");
        float[] vector = embeddingModel.embed(segment.text()).content().vector();

        Embedding embedding = Embedding.from(vector);

        embeddingStore.add(embedding, segment);

        log.info("已添加嵌入: {}", segment.text());

        // 验证添加成功
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(embeddingModel.embed(segment.text()).content())
                .maxResults(10)
                .build());

        List<EmbeddingMatch<TextSegment>> allEmbeddings = searchResult.matches();
        assertTrue(allEmbeddings.size() >= 1);
        assertEquals("测试文本", allEmbeddings.get(0).embedded().text());

        log.info("存储的嵌入数: {}", allEmbeddings.size());
        log.info("\n✅ 测试通过：能够添加和检索嵌入\n");
    }

    @Test
    @DisplayName("应该能批量添加嵌入")
    void shouldAddMultipleEmbeddings() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 批量添加嵌入                                          ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 创建多个嵌入
        List<String> texts = List.of(
                "Java 编程语言",
                "Python 编程语言",
                "JavaScript 编程语言"
        );

        List<TextSegment> segments = texts.stream()
                .map(TextSegment::from)
                .toList();

        List<Embedding> embeddings = texts.stream()
                .map(text -> Embedding.from(embeddingModel.embed(text).content().vector()))
                .toList();

        // 批量添加
        embeddingStore.addAll(embeddings, segments);

        // 验证添加成功
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(new float[1]))
                .maxResults(100)
                .build());

        List<EmbeddingMatch<TextSegment>> allEmbeddings = searchResult.matches();
        assertEquals(texts.size(), allEmbeddings.size());

        log.info("批量添加了 {} 个嵌入", texts.size());
        log.info("存储的嵌入数: {}", allEmbeddings.size());
        log.info("\n✅ 测试通过：能够批量添加嵌入\n");
    }

    @Test
    @DisplayName("应该能找到最相似的嵌入")
    void shouldFindMostSimilarEmbedding() {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 找到最相似的嵌入                                     ║");
        log.info("╚═════════════════════════════════════════════════════════════════════╣\n");

        // 添加多个嵌入
        List<String> texts = List.of(
                "Java 编程语言",
                "Python 编程语言",
                "JavaScript 编程语言",
                "前端开发",
                "后端开发"
        );

        texts.forEach(text -> {
            TextSegment segment = TextSegment.from(text);
            float[] vector = embeddingModel.embed(text).content().vector();
            embeddingStore.add(Embedding.from(vector), segment);
        });

        log.info("已添加 {} 个嵌入", texts.size());

        // 搜索最相似的
        String query = "编程语言";
        float[] queryVector = embeddingModel.embed(query).content().vector();

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(queryVector))
                .maxResults(3)
                .minScore(0.0)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> results = result.matches();

        // 验证结果
        assertNotNull(results);
        assertTrue(results.size() >= 1);
        assertTrue(results.get(0).embedded().text().contains("编程") ||
                   results.get(0).embedded().text().contains("语言"));

        log.info("查询: \"{}\"", query);
        log.info("找到 {} 个相关嵌入", results.size());
        for (int i = 0; i < results.size(); i++) {
            log.info("  {}. \"{}\" (相似度: {:.4f})",
                    i + 1,
                    results.get(i).embedded().text(),
                    results.get(i).score());
        }
        log.info("\n✅ 测试通过：能够找到最相似的嵌入\n");
    }

    @Test
    @DisplayName("应该能支持用户元数据")
    void shouldSupportUserMetadata() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 支持用户元数据                                         ║");
        log.info("╚═════════════════════════════════════════════════════════════════════╣\n");

        // 创建带元数据的 TextSegment
        // 使用 Metadata.from(Map) 将 Map 转换为 Metadata
        Metadata metadata =
                Metadata.from(
                        java.util.Map.of(
                                "document_id", "doc123",
                                "category", "技术",
                                "author", "张三"
                        )
                );

        TextSegment segment = TextSegment.from("测试文档", metadata);
        float[] vector = embeddingModel.embed(segment.text()).content().vector();

        embeddingStore.add(Embedding.from(vector), segment);

        // 搜索并验证元数据
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(embeddingModel.embed(segment.text()).content())
                .maxResults(10)
                .build());

        List<EmbeddingMatch<TextSegment>> allEmbeddings = searchResult.matches();
        assertEquals(1, allEmbeddings.size());

        Metadata retrievedMetadata = allEmbeddings.get(0).embedded().metadata();
        assertNotNull(retrievedMetadata);
        assertEquals("doc123", retrievedMetadata.getString("document_id"));
        assertEquals("技术", retrievedMetadata.getString("category"));
        assertEquals("张三", retrievedMetadata.getString("author"));

        log.info("已添加嵌入，包含元数据:");
        retrievedMetadata.toMap().forEach((key, value) -> log.info("  {}: {}", key, value));
        log.info("\n✅ 测试通过：能够正确处理用户元数据\n");
    }

    @Test
    @DisplayName("应该能限制最大结果数")
    void shouldLimitMaxResults() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 限制最大结果数                                         ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 添加多个嵌入
        for (int i = 0; i < 10; i++) {
            TextSegment segment = TextSegment.from("文本 " + i);
            float[] vector = embeddingModel.embed(segment.text()).content().vector();
            embeddingStore.add(Embedding.from(vector), segment);
        }

        log.info("已添加 10 个嵌入");

        // 搜索，限制结果为 3
        String query = "文本";
        float[] queryVector = embeddingModel.embed(query).content().vector();

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(queryVector))
                .maxResults(3)
                .minScore(0.0)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> results = result.matches();

        // 验证最多返回 3 个结果
        assertTrue(results.size() <= 3);

        log.info("查询: \"{}\"", query);
        log.info("请求最多 3 个结果");
        log.info("实际返回 {} 个结果", results.size());
        log.info("\n✅ 测试通过：能够正确限制最大结果数\n");
    }

    @Test
    @DisplayName("应该能清空所有嵌入")
    void shouldClearAllEmbeddings() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 清空所有嵌入                                           ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 添加嵌入
        for (int i = 0; i < 5; i++) {
            TextSegment segment = TextSegment.from("文本 " + i);
            float[] vector = embeddingModel.embed(segment.text()).content().vector();
            embeddingStore.add(Embedding.from(vector), segment);
        }

        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(new float[1]))
                .maxResults(100)
                .build());
        assertEquals(5, searchResult.matches().size());
        log.info("已添加 5 个嵌入");

        // 清空
        embeddingStore.removeAll();

        // 验证清空
        EmbeddingSearchResult<TextSegment> emptyResult = embeddingStore.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(new float[1]))
                .maxResults(100)
                .build());
        assertTrue(emptyResult.matches().isEmpty());
        log.info("已清空所有嵌入");
        log.info("\n✅ 测试通过：能够清空所有嵌入\n");
    }

    @Test
    @DisplayName("应该能处理空搜索结果")
    void shouldHandleEmptySearchResults() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 处理空搜索结果                                         ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 不添加任何嵌入

        // 搜索
        String query = "测试查询";
        float[] queryVector = embeddingModel.embed(query).content().vector();

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(queryVector))
                .maxResults(10)
                .minScore(0.0)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> results = result.matches();

        // 验证返回空列表
        assertNotNull(results);
        assertTrue(results.isEmpty());

        log.info("查询: \"{}\"", query);
        log.info("存储为空，返回结果数: {}", results.size());
        log.info("\n✅ 测试通过：能够正确处理空搜索结果\n");
    }
}
