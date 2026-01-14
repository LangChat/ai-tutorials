package cn.langchat.learning.tutorial.rag;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 10 - RAG（检索增强生成）测试
 * <p>
 * 测试文档中的完整使用场景，包括：
 * - RAG 系统的基本组件
 * - 文档索引和检索
 * - 上下文构建
 * - 检索增强的问答
 * - RAG 系统的端到端测试
 *
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
@DisplayName("10 - RAG（检索增强生成）测试")
class RagTest {

    private ChatModel chatModel;
    private EmbeddingModel embeddingModel;
    private InMemoryEmbeddingStore<TextSegment> embeddingStore;
    private RagSystem ragSystem;

    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        chatModel = TestModelProvider.getChatModel();
        embeddingModel = TestModelProvider.getEmbeddingModel();
        embeddingStore = new InMemoryEmbeddingStore<>();

        // 初始化 RAG 系统
        ragSystem = new RagSystem(embeddingModel, embeddingStore, chatModel);
    }

    @Test
    @DisplayName("应该能索引文档")
    void shouldIndexDocument() {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 索引文档                                               ║");
        log.info("╚═════════════════════════════════════════════════════════════════════╣\n");

        // 创建测试文档
        Document doc1 = new Document("doc1", "Java 简介",
                "Java 是一种面向对象的编程语言，具有平台无关性、面向对象、安全性等特点。");
        Document doc2 = new Document("doc2", "Python 简介",
                "Python 是一种解释型编程语言，语法简洁，易于学习，适合快速开发。");
        Document doc3 = new Document("doc3", "JavaScript 简介",
                "JavaScript 是一种脚本语言，主要用于 Web 开发，支持动态类型和函数式编程。");

        // 索引文档
        ragSystem.indexDocument(doc1);
        ragSystem.indexDocument(doc2);
        ragSystem.indexDocument(doc3);

        // 验证索引成功
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(new float[1]))
                .maxResults(100)
                .build());
        List<EmbeddingMatch<TextSegment>> allEmbeddings = searchResult.matches();
        assertTrue(allEmbeddings.size() > 0);

        log.info("已索引 3 个文档");
        log.info("生成的分段数: {}", allEmbeddings.size());
        log.info("\n✅ 测试通过：能够成功索引文档\n");
    }

    @Test
    @DisplayName("应该能检索相关文档")
    void shouldRetrieveRelevantDocuments() {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 检索相关文档                                           ║");
        log.info("╚═════════════════════════════════════════════════════════════════════╣\n");

        // 先索引一些文档
        Document doc1 = new Document("doc1", "Java 编程",
                "Java 是一种面向对象的编程语言，广泛应用于企业级开发。");
        Document doc2 = new Document("doc2", "Python 编程",
                "Python 是一种解释型编程语言，适合数据科学和机器学习。");

        ragSystem.indexDocument(doc1);
        ragSystem.indexDocument(doc2);

        // 检索相关文档
        String query = "什么是面向对象的编程语言？";
        List<Document> relevantDocs = ragSystem.retrieve(query, 2);

        // 验证检索结果
        assertNotNull(relevantDocs);
        assertTrue(relevantDocs.size() >= 1);

        log.info("查询: \"{}\"", query);
        log.info("检索到 {} 个相关文档", relevantDocs.size());
        for (int i = 0; i < relevantDocs.size(); i++) {
            log.info("  {}. \"{}\"", i + 1, relevantDocs.get(i).getTitle());
        }
        log.info("\n✅ 测试通过：能够检索相关文档\n");
    }

    @Test
    @DisplayName("应该能构建上下文")
    void shouldBuildContext() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 构建上下文                                               ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 索引文档
        Document doc = new Document("doc1", "Java 特性",
                "Java 具有以下特性：平台无关性、面向对象、安全性、高性能、多线程支持。");
        ragSystem.indexDocument(doc);

        // 检索文档
        String query = "Java 的主要特点是什么？";
        List<Document> relevantDocs = ragSystem.retrieve(query, 1);

        // 构建上下文
        String context = ragSystem.buildContext(relevantDocs);

        assertNotNull(context);
        assertFalse(context.isEmpty());
        assertTrue(context.contains("Java"));

        log.info("查询: \"{}\"", query);
        log.info("检索到的文档: {}", relevantDocs.get(0).getTitle());
        log.info("构建的上下文:\n{}", context);
        log.info("\n✅ 测试通过：能够构建上下文\n");
    }

    @Test
    @DisplayName("应该能执行 RAG 问答")
    void shouldExecuteRagQuery() {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 执行 RAG 问答                                          ║");
        log.info("╚═════════════════════════════════════════════════════════════════════════╣\n");

        // 索引文档
        Document doc1 = new Document("doc1", "LangChain4j 简介",
                "LangChain4j 是一个用于构建大语言模型（LLM）应用的 Java 库，提供了完整的工具链。");
        Document doc2 = new Document("doc2", "Embedding 说明",
                "Embedding 是将文本转换为数值向量的过程，用于计算文本相似度。");
        Document doc3 = new Document("doc3", "RAG 系统说明",
                "RAG（检索增强生成）是一种结合了信息检索和文本生成的技术，可以提高答案的准确性。");

        ragSystem.indexDocument(doc1);
        ragSystem.indexDocument(doc2);
        ragSystem.indexDocument(doc3);

        // 执行 RAG 查询
        String query = "什么是 RAG 系统？";
        RagQueryResult result = ragSystem.query(query, 2);

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getAnswer());
        assertFalse(result.getAnswer().isEmpty());

        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║ RAG 查询结果                                                 ║");
        log.info("╠═══════════════════════════════════════════════════════════════════════════════════════╣");
        log.info("║ 查询: {}", query);
        log.info("╠═════════════════════════════════════════════════════════════════════════════════════════════╣");
        log.info("║ 检索到的文档数: {}", result.getRetrievedDocuments().size());
        log.info("╠═════════════════════════════════════════════════════════════════════════════════════════╣");
        log.info("║ 使用的上下文:");
        for (Document doc : result.getRetrievedDocuments()) {
            log.info("║   - {} ({})", doc.getTitle(), doc.getId());
        }
        log.info("╠═════════════════════════════════════════════════════════════════════════════════════════╣");
        log.info("║ 生成的答案:");
        log.info("║ {}", result.getAnswer());
        log.info("╚═════════════════════════════════════════════════════════════════════════════════════════════╝\n");

        log.info("✅ 测试通过：能够执行 RAG 问答\n");
    }

    @Test
    @DisplayName("应该能处理无相关文档的情况")
    void shouldHandleNoRelevantDocuments() {
        log.info("╔════════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 处理无相关文档的情况                                   ║");
        log.info("╚═════════════════════════════════════════════════════════════════════════════╣\n");

        // 索引不相关的文档
        Document doc = new Document("doc1", "历史",
                "唐朝是中国历史上的一个朝代，公元618年建立，907年灭亡。");
        ragSystem.indexDocument(doc);

        // 查询不相关的问题
        String query = "如何编写 Java 代码？";
        RagQueryResult result = ragSystem.query(query, 2);

        // 验证结果
        assertNotNull(result);
        log.info("查询: \"{}\"", query);
        log.info("检索到的文档数: {}", result.getRetrievedDocuments().size());
        log.info("答案: {}", result.getAnswer());
        log.info("\n✅ 测试通过：能够正确处理无相关文档的情况\n");
    }

    /**
     * RAG 系统类
     */
    private static class RagSystem {
        private final EmbeddingModel embeddingModel;
        private final InMemoryEmbeddingStore<TextSegment> embeddingStore;
        private final ChatModel chatModel;
        private final List<Document> documentRegistry;

        public RagSystem(EmbeddingModel embeddingModel,
                         InMemoryEmbeddingStore<TextSegment> embeddingStore,
                         ChatModel chatModel) {
            this.embeddingModel = embeddingModel;
            this.embeddingStore = embeddingStore;
            this.chatModel = chatModel;
            this.documentRegistry = new ArrayList<>();
        }

        /**
         * 索引文档
         */
        public void indexDocument(Document document) {
            documentRegistry.add(document);

            // 分段并嵌入
            List<TextSegment> segments = segmentDocument(document);
            for (TextSegment segment : segments) {
                float[] vector = embeddingModel.embed(segment.text()).content().vector();
                embeddingStore.add(Embedding.from(vector), segment);
            }
        }

        /**
         * 检索相关文档
         */
        public List<Document> retrieve(String query, int topK) {
            float[] queryVector = embeddingModel.embed(query).content().vector();

            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(Embedding.from(queryVector))
                    .maxResults(topK)
                    .minScore(0.0)
                    .build();

            EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);
            List<EmbeddingMatch<TextSegment>> relevantEmbeddings = result.matches();

            // 提取文档（去重）
            return relevantEmbeddings.stream()
                    .map(embedding -> {
                        String id = embedding.embedded().metadata().getString("document_id");
                        return id != null ? getDocumentById(id) : null;
                    })
                    .distinct()
                    .filter(doc -> doc != null)
                    .collect(Collectors.toList());
        }

        /**
         * 构建上下文
         */
        public String buildContext(List<Document> documents) {
            StringBuilder context = new StringBuilder();
            for (int i = 0; i < documents.size(); i++) {
                Document doc = documents.get(i);
                context.append(String.format(
                        "[文档 %d: %s]\n%s\n\n",
                        i + 1,
                        doc.getTitle(),
                        doc.getContent()
                ));
            }
            return context.toString();
        }

        /**
         * 执行 RAG 查询
         */
        public RagQueryResult query(String userQuery, int topK) {
            // 1. 检索相关文档
            List<Document> relevantDocs = retrieve(userQuery, topK);

            // 2. 构建上下文
            String context = buildContext(relevantDocs);

            // 3. 构建提示词
            String prompt = String.format(
                    "基于以下文档回答问题。如果文档中没有答案，请说不知道。\n\n%s\n\n问题: %s",
                    context,
                    userQuery
            );

            // 4. 生成答案
            AiMessage aiMessage = chatModel.chat(UserMessage.from(prompt)).aiMessage();
            String answer = aiMessage.text();

            return new RagQueryResult(userQuery, relevantDocs, answer, context);
        }

        /**
         * 文档分段
         */
        private List<TextSegment> segmentDocument(Document document) {
            List<TextSegment> segments = new ArrayList<>();
            String content = document.getContent();

            // 简化：按段落分段
            String[] paragraphs = content.split("\\n\\n+");
            for (String paragraph : paragraphs) {
                segments.add(TextSegment.from(paragraph, document.getMetadata()));
            }

            return segments;
        }

        private Document getDocumentById(String id) {
            return documentRegistry.stream()
                    .filter(doc -> doc.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * 文档类
     */
    private static class Document {
        private final String id;
        private final String title;
        private final String content;
        private final Metadata metadata;

        public Document(String id, String title, String content) {
            this.id = id;
            this.title = title;
            this.content = content;
            // 使用 Metadata.from(String, String) 创建元数据
            this.metadata = Metadata.from("document_id", id);
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }

        public Metadata getMetadata() {
            return metadata;
        }
    }

    /**
     * RAG 查询结果
     */
    private static class RagQueryResult {
        private final String query;
        private final List<Document> retrievedDocuments;
        private final String answer;
        private final String context;

        public RagQueryResult(String query, List<Document> retrievedDocuments,
                              String answer, String context) {
            this.query = query;
            this.retrievedDocuments = retrievedDocuments;
            this.answer = answer;
            this.context = context;
        }

        public String getQuery() {
            return query;
        }

        public List<Document> getRetrievedDocuments() {
            return retrievedDocuments;
        }

        public String getAnswer() {
            return answer;
        }

        public String getContext() {
            return context;
        }
    }
}
