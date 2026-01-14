---
title: 'Embedding Store 向量存储'
description: '学习 LangChain4j 的 Embedding Store 向量存储 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 08 - EmbeddingStore 向量存储

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 理解 EmbeddingStore 的作用和重要性
- 掌握不同类型的 EmbeddingStore 实现
- 学会基本的 CRUD 操作（增删改查）
- 理解向量索引和搜索策略
- 掌握持久化向量存储的方法
- 实现一个完整的向量检索系统

## 前置知识

- 完成《01 - LangChain4j 简介》章节
- 完成《02 - 你的第一个 Chat 应用》章节
- 完成《03 - 深入理解 ChatModel》章节
- 完成《07 - EmbeddingModel 和向量》章节

## 核心概念

### 什么是 EmbeddingStore？

**EmbeddingStore** 是专门用于存储和检索 Embedding 向量的存储系统。它就像是一个专门为向量操作优化的数据库。

**为什么需要 EmbeddingStore？**

1. **快速检索** - 支持高效的向量相似度搜索
2. **元数据管理** - 除了向量，还存储文档的元数据（标题、内容、创建时间等）
3. **大规模支持** - 可以处理百万甚至数十亿级别的向量数据
4. **高级搜索** - 支持过滤、排序、分页等高级功能

**类比理解：**
如果把 Embedding 比作书籍的经纬度坐标，那么 `EmbeddingStore` 就像是地图索引系统，可以快速找到与查询位置最接近的所有书籍。

### Embedding 接口

LangChain4j 中的 `Embedding` 接口表示一个存储的向量：

```java
public interface Embedding {
    
    /**
     * 获取嵌入的向量
     * @return 向量数据
     */
    float[] vector();
    
    /**
     * 获取嵌入的文本
     * @return 原始文本
     */
    String text();
    
    /**
     * 获取嵌入的 ID
     * @return 嵌入 ID
     */
    String id();
    
    /**
     * 获取分数（相似度等）
     * @return 分数
     */
    Double score();
    
    /**
     * 获取用户自定义的数据
     * @return 用户数据
     */
    Map<String, Object> userMetadata();
}
```

### EmbeddingStore 接口

`EmbeddingStore` 提供了向量存储的核心功能：

```java
public interface EmbeddingStore<TextSegment> {
    
    /**
     * 添加嵌入
     * @param embedding 要添加的嵌入
     */
    void add(Embedding<TextSegment> embedding);
    
    /**
     * 批量添加嵌入
     * @param embeddings 嵌入列表
     */
    void addAll(List<Embedding<TextSegment>> embeddings);
    
    /**
     * 根据向量查找最相似的嵌入
     * @param referenceEmbedding 参考向量
     * @param maxResults 最大结果数
     * @param minScore 最小分数
     * @return 匹配的嵌入列表
     */
    List<Embedding<TextSegment>> findRelevant(
            float[] referenceEmbedding,
            int maxResults,
            double minScore
    );
    
    /**
     * 根据向量查找最相似的嵌入（带分数过滤器）
     * @param referenceEmbedding 参考向量
     * @param maxResults 最大结果数
     * @param minScore 最小分数
     * @param scoreThreshold 分数阈值
     * @return 匹配的嵌入列表
     */
    List<Embedding<TextSegment>> findRelevant(
            float[] referenceEmbedding,
            int maxResults,
            double minScore,
            double scoreThreshold
    );
    
    /**
     * 删除所有嵌入
     */
    void removeAll();
}
```

## 常见的 EmbeddingStore 实现

### 1. InMemoryEmbeddingStore

内存中的向量存储，适合小型应用和原型开发。

```java
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.data.embedding.Embedding;

/**
 * 内存向量存储示例
 */
public class InMemoryEmbeddingStoreExample {

    private final EmbeddingModel embeddingModel;
    private final InMemoryEmbeddingStore<TextSegment> embeddingStore;

    public InMemoryEmbeddingStoreExample() {
        // 创建 Embedding 模型
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("text-embedding-3-small")
                .build();
        
        // 创建内存存储
        this.embeddingStore = new InMemoryEmbeddingStore<>();
    }

    /**
     * 添加文档
     */
    public void addDocument(String documentId, String content) {
        // 1. 分段文档
        List<TextSegment> segments = segmentText(content, 100);
        
        // 2. 为每个分段生成 Embedding
        for (TextSegment segment : segments) {
            float[] vector = embeddingModel.embed(segment.text()).content().vector();
            
            Embedding embedding = Embedding.from(vector);
            embeddingStore.add(embedding, segment);
        }
        
        System.out.println("已添加文档: " + documentId);
        System.out.println("分段数量: " + segments.size());
        
        // 获取存储的向量数量
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(
            EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(new float[1]))
                .maxResults(1000)
                .build()
        );
        System.out.println("存储的向量数量: " + searchResult.matches().size());
    }

    /**
     * 搜索相关文档
     */
    public List<String> search(String query, int topK) {
        // 1. 生成查询的 Embedding
        float[] queryEmbedding = embeddingModel.embed(query).content().vector();
        
        // 2. 查找最相似的向量
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(queryEmbedding))
                .maxResults(topK)
                .minScore(0.0)
                .build();
        
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> relevantEmbeddings = result.matches();
        
        // 3. 提取文档 ID（去重）
        Set<String> documentIds = new java.util.HashSet<>();
        for (EmbeddingMatch<TextSegment> match : relevantEmbeddings) {
            String docId = match.embedded().metadata().getString("document_id");
            if (docId != null) {
                documentIds.add(docId);
            }
        }
        
        return new java.util.ArrayList<>(documentIds);
    }

    /**
     * 文本分段
     */
    private List<TextSegment> segmentText(String text, int maxLength) {
        List<TextSegment> segments = new java.util.ArrayList<>();
        
        for (int i = 0; i < text.length(); i += maxLength) {
            int end = Math.min(i + maxLength, text.length());
            segments.add(TextSegment.from(text.substring(i, end)));
        }
        
        return segments;
    }

    /**
     * 获取所有存储的向量
     */
    public List<EmbeddingMatch<TextSegment>> getAll() {
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(
            EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(new float[1]))
                .maxResults(1000)
                .build()
        );
        return searchResult.matches();
    }

    /**
     * 清空存储
     */
    public void clear() {
        embeddingStore.removeAll();
        System.out.println("向量存储已清空");
    }

    public static void main(String[] args) {
        InMemoryEmbeddingStoreExample example = new InMemoryEmbeddingStoreExample();

        // 添加文档
        example.addDocument("doc1", "Java 是一种面向对象的编程语言..." +
                           "它具有平台无关性、面向对象等特性。");
        example.addDocument("doc2", "Python 是一种解释型编程语言..." +
                           "它具有语法简洁、易于学习等特点。");
        example.addDocument("doc3", "JavaScript 是一种脚本语言..." +
                           "它主要用于 Web 开发，支持动态类型。");

        System.out.println();
        
        // 搜索
        String query = "编程语言的特点";
        List<String> results = example.search(query, 3);
        
        System.out.println("=== 搜索结果 ===");
        System.out.println("查询: " + query);
        System.out.println("找到 " + results.size() + " 个相关文档:");
        for (String docId : results) {
            System.out.println("  - " + docId);
        }
    }
}
```

### 2. 向量数据库集成

生产环境中，通常需要将向量存储到专门的向量数据库，如：

#### Milvus

```java
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStoreConfig;

EmbeddingStore<TextSegment> embeddingStore = MilvusEmbeddingStore.builder()
        .host("localhost")
        .port(19530)
        .collectionName("documents")
        .dimension(1536)
        .metricType(MilvusMetricType.COSINE)
        .build();
```

#### Pinecone

```java
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStoreConfig;

EmbeddingStore<TextSegment> embeddingStore = PineconeEmbeddingStore.builder()
        .apiKey(System.getenv("PINECONE_API_KEY"))
        .environment("production")
        .indexName("my-index")
        .dimension(1536)
        .build();
```

#### Weaviate

```java
import dev.langchain4j.store.embedding.weaviate.WeaviateEmbeddingStore;
import dev.langchain4j.store.embedding.weaviate.WeaviateEmbeddingStoreConfig;

EmbeddingStore<TextSegment> embeddingStore = WeaviateEmbeddingStore.builder()
        .apiKey(System.getenv("WEAVIATE_API_KEY"))
        .scheme("https")
        .host("cluster.weaviate.cloud")
        .className("Document")
        .dimension(1536)
        .build();
```

## 向量索引和搜索策略

### 搜索参数

**核心参数：**

| 参数 | 说明 | 推荐值 |
|------|------|---------|
| `maxResults` | 返回的最大结果数 | 5-20 |
| `minScore` | 最小相似度阈值 | 0.0-0.7（余弦相似度） |
| `scoreThreshold` | 相似度阈值过滤 | 0.7-0.9 |
| `embeddingMatch` | 是否必须精确匹配 | false |

**搜索策略：**

```java
import dev.langchain4j.data.embedding.Embedding;

/**
 * 向量搜索配置
 */
public class VectorSearchConfig {
    
    private final int maxResults;
    private final double minScore;
    private final double scoreThreshold;
    private final boolean requireExactMatch;

    public VectorSearchConfig(int maxResults, double minScore) {
        this.maxResults = maxResults;
        this.minScore = minScore;
        this.scoreThreshold = minScore;
        this.requireExactMatch = false;
    }

    public VectorSearchConfig(
            int maxResults,
            double minScore,
            double scoreThreshold,
            boolean requireExactMatch) {
        this.maxResults = maxResults;
        this.minScore = minScore;
        this.scoreThreshold = scoreThreshold;
        this.requireExactMatch = requireExactMatch;
    }

    // 预定义配置
    public static final VectorSearchConfig STRICT = 
        new VectorSearchConfig(10, 0.8, 0.9, false);
    
    public static final VectorSearchConfig RELAXED = 
        new VectorSearchConfig(20, 0.5, 0.7, false);
    
    public static final VectorSearchConfig PRECISE = 
        new VectorSearchConfig(5, 0.9, 0.95, false);

    public int getMaxResults() {
        return maxResults;
    }

    public double getMinScore() {
        return minScore;
    }

    public double getScoreThreshold() {
        return scoreThreshold;
    }

    public boolean isRequireExactMatch() {
        return requireExactMatch;
    }
}
```

### 混合搜索

结合向量搜索和传统关键词搜索：

```java
import dev.langchain4j.data.embedding.Embedding;

/**
 * 混合搜索
 */
public class HybridSearch {

    /**
     * 混合搜索：向量 + 关键词
     */
    public List<Embedding<TextSegment>> search(
            String query,
            List<Embedding<TextSegment>> candidateEmbeddings,
            VectorSearchConfig config,
            EmbeddingModel embeddingModel) {

        // 1. 向量搜索
        float[] queryVector = embeddingModel.embed(query).content().vector();
        List<Embedding<TextSegment>> vectorResults = candidateEmbeddings.stream()
                .filter(embedding -> {
                    double score = cosineSimilarity(queryVector, embedding.vector());
                    return score >= config.getMinScore();
                })
                .sorted((a, b) -> Double.compare(
                    cosineSimilarity(queryVector, b.vector()),
                    cosineSimilarity(queryVector, a.vector())
                ))
                .limit(config.getMaxResults())
                .collect(java.util.stream.Collectors.toList());

        // 2. 关键词过滤
        List<Embedding<TextSegment>> keywordResults = 
            candidateEmbeddings.stream()
                .filter(embedding -> containsKeyword(embedding.text(), query))
                .collect(java.util.stream.Collectors.toList());

        // 3. 混合排序（可以添加重排序逻辑）
        // 这里简化为只返回向量搜索结果
        // 实际应用中可以实现更复杂的混合排序算法
        
        return vectorResults;
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        // 简化的余弦相似度计算
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private static boolean containsKeyword(String text, String keyword) {
        String lowerText = text.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        return lowerText.contains(lowerKeyword);
    }
}
```

## 完整示例：文档检索系统

```java
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.service.AiServices;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 完整的文档检索系统
 */
public class DocumentRetrievalSystem {

    private final EmbeddingModel embeddingModel;
    private final InMemoryEmbeddingStore<TextSegment> embeddingStore;
    private final Map<String, Document> documentRegistry;

    public DocumentRetrievalSystem() {
        // 初始化组件
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("text-embedding-3-small")
                .build();
        
        this.embeddingStore = new InMemoryEmbeddingStore<>();
        this.documentRegistry = new HashMap<>();
    }

    /**
     * 添加文档到检索系统
     */
    public void addDocument(Document document) {
        // 注册文档
        documentRegistry.put(document.getId(), document);
        
        // 分段并索引
        List<TextSegment> segments = segmentDocument(document, 500);
        
        for (TextSegment segment : segments) {
            float[] vector = embeddingModel.embed(segment.text()).content().vector();
            
            // 创建带有元数据的 TextSegment
            Metadata metadata = Metadata.from("document_id", document.getId());
            
            TextSegment segmentWithMetadata = TextSegment.from(segment.text(), metadata);
            
            Embedding embedding = Embedding.from(vector);
            embeddingStore.add(embedding, segmentWithMetadata);
        }
        
        System.out.println("已索引文档: " + document.getTitle());
        System.out.println("分段数: " + segments.size());
    }

    /**
     * 批量添加文档
     */
    public void addDocuments(List<Document> documents) {
        for (Document doc : documents) {
            addDocument(doc);
        }
        System.out.println("总文档数: " + documentRegistry.size());
    }

    /**
     * 检索相关文档
     */
    public List<SearchResult> search(String query, int topK) {
        // 1. 生成查询向量
        float[] queryVector = embeddingModel.embed(query).content().vector();
        
        // 2. 向量搜索
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(queryVector))
                .maxResults(topK)
                .minScore(0.0)
                .build();
        
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> relevantEmbeddings = result.matches();
        
        // 3. 聚合文档结果
        Map<String, SearchResult> documentResults = new HashMap<>();
        
        for (EmbeddingMatch<TextSegment> match : relevantEmbeddings) {
            String docId = match.embedded().metadata().getString("document_id");
            
            // 如果文档已存在，更新分数
            if (documentResults.containsKey(docId)) {
                SearchResult existing = documentResults.get(docId);
                if (match.score() != null && match.score() > existing.getScore()) {
                    documentResults.put(docId, new SearchResult(
                        docId,
                        documentRegistry.get(docId),
                        match.score()
                    ));
                }
            } else {
                documentResults.put(docId, new SearchResult(
                    docId,
                    documentRegistry.get(docId),
                    match.score()
                ));
            }
        }
        
        // 4. 按分数排序并返回前 topK 个
        List<SearchResult> sortedResults = documentResults.values().stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .collect(Collectors.toList());
        
        return sortedResults;
    }

    /**
     * 高级搜索：带过滤
     */
    public List<SearchResult> searchWithFilter(
            String query,
            int topK,
            DocumentFilter filter) {
        
        // 1. 基础向量搜索
        float[] queryVector = embeddingModel.embed(query).content().vector();
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(queryVector))
                .maxResults(100)
                .minScore(0.0)
                .build();
        
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> relevantEmbeddings = result.matches();
        
        // 2. 应用过滤器
        List<EmbeddingMatch<TextSegment>> filteredEmbeddings = relevantEmbeddings.stream()
                .filter(embedding -> filter.matches(embedding.embedded().metadata().toMap()))
                .collect(Collectors.toList());
        
        // 3. 聚合并排序
        Map<String, SearchResult> documentResults = new HashMap<>();
        for (EmbeddingMatch<TextSegment> match : filteredEmbeddings.subList(0, Math.min(topK, filteredEmbeddings.size()))) {
            String docId = match.embedded().metadata().getString("document_id");
            documentResults.put(docId, new SearchResult(
                docId,
                documentRegistry.get(docId),
                match.score()
            ));
        }
        
        return new ArrayList<>(documentResults.values());
    }

    /**
     * 文档分段
     */
    private List<TextSegment> segmentDocument(Document document, int maxLength) {
        List<TextSegment> segments = new ArrayList<>();
        String content = document.getContent();
        
        // 按段落分段
        String[] paragraphs = content.split("\\n\\n+");
        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                continue;
            }
            
            // 如果段落太长，继续分段
            for (int i = 0; i < paragraph.length(); i += maxLength) {
                int end = Math.min(i + maxLength, paragraph.length());
                segments.add(TextSegment.from(paragraph.substring(i, end)));
            }
        }
        
        return segments;
    }

    /**
     * 获取文档统计
     */
    public RetrievalStats getStats() {
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(
            EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(new float[1]))
                .maxResults(10000)
                .build()
        );
        return new RetrievalStats(
            documentRegistry.size(),
            searchResult.matches().size()
        );
    }

    public void clear() {
        documentRegistry.clear();
        embeddingStore.removeAll();
        System.out.println("检索系统已清空");
    }

    // 数据类
    public static class Document {
        private final String id;
        private final String title;
        private final String content;
        private final String category;
        private final Date createdAt;

        public Document(String id, String title, String content, String category) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.category = category;
            this.createdAt = new Date();
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
        public String getCategory() { return category; }
    }

    public static class SearchResult {
        private final String documentId;
        private final Document document;
        private final Double score;

        public SearchResult(String documentId, Document document, Double score) {
            this.documentId = documentId;
            this.document = document;
            this.score = score;
        }

        public String getDocumentId() { return documentId; }
        public Document getDocument() { return document; }
        public Double getScore() { return score; }
    }

    public interface DocumentFilter {
        boolean matches(Map<String, Object> metadata);
    }

    public static class CategoryFilter implements DocumentFilter {
        private final String category;

        public CategoryFilter(String category) {
            this.category = category;
        }

        @Override
        public boolean matches(Map<String, Object> metadata) {
            return category.equals(metadata.get("category"));
        }
    }

    public static class RetrievalStats {
        private final int documentCount;
        private final int segmentCount;

        public RetrievalStats(int documentCount, int segmentCount) {
            this.documentCount = documentCount;
            this.segmentCount = segmentCount;
        }

        public int getDocumentCount() { return documentCount; }
        public int getSegmentCount() { return segmentCount; }
    }

    public static void main(String[] args) {
        DocumentRetrievalSystem retrieval = new DocumentRetrievalSystem();

        // 添加文档
        List<Document> documents = List.of(
            new Document("doc1", "Java 简介", 
                "Java 是一种面向对象的编程语言...", "编程语言"),
            new Document("doc2", "Python 教程", 
                "Python 是一种解释型编程语言...", "编程语言"),
            new Document("doc3", "JavaScript 指南", 
                "JavaScript 是一种脚本语言...", "前端开发"),
            new Document("doc4", "Spring 框架", 
                "Spring 是一个开源的应用框架...", "框架")
        );

        retrieval.addDocuments(documents);
        
        RetrievalStats stats = retrieval.getStats();
        System.out.println("检索系统统计:");
        System.out.println("文档数: " + stats.getDocumentCount());
        System.out.println("分段数: " + stats.getSegmentCount());
        System.out.println();

        // 搜索
        String query = "什么是编程语言？";
        List<SearchResult> results = retrieval.search(query, 3);

        System.out.println("=== 搜索结果 ===");
        System.out.println("查询: " + query);
        System.out.println();
        
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            System.out.printf("%d. %s (相似度: %.4f)%n",
                    i + 1,
                    result.getDocument().getTitle(),
                    result.getScore());
            System.out.println("   分类: " + result.getDocument().getCategory());
            System.out.println();
        }

        // 高级搜索：带过滤
        System.out.println("=== 分类过滤搜索 ===");
        System.out.println("查询: " + query);
        System.out.println("过滤: 前端开发");
        System.out.println();
        
        CategoryFilter frontendFilter = new CategoryFilter("前端开发");
        List<SearchResult> filteredResults = 
            retrieval.searchWithFilter(query, 5, frontendFilter);
        
        for (SearchResult result : filteredResults) {
            System.out.println("  - " + result.getDocument().getTitle());
            System.out.printf("    分类: %s, 相似度: %.4f%n",
                    result.getDocument().getCategory(),
                    result.getScore());
        }
    }
}
```

## 测试代码示例

**注意：** 以下代码示例使用了最新的 LangChain4j API。完整的测试代码请参考 `src/test/java/cn/langchat/learning/embeddingstore/EmbeddingStoreTest.java`。

### 关键 API 变化总结

| 旧 API | 新 API | 说明 |
|--------|--------|------|
| `InMemoryEmbeddingStore.create()` | `new InMemoryEmbeddingStore<>()` | 构造函数 |
| `Embedding.from(segment, vector)` | `add(Embedding.from(vector), segment)` | 分离参数 |
| `getAll()` | `search(EmbeddingSearchRequest.builder()...)` | 使用搜索 API |
| `findRelevant(vector, max, minScore)` | `search(EmbeddingSearchRequest.builder()...)` | 使用搜索请求 |
| `embeddingStore.add(embedding)` | `add(embedding, segment)` | 需要两个参数 |
| `metadata.get("key")` | `metadata.getString("key")` | 使用类型安全方法 |

### 完整测试示例

```java
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * EmbeddingStore 测试
 */
class EmbeddingStoreTest {

    private EmbeddingModel embeddingModel;
    private InMemoryEmbeddingStore<TextSegment> embeddingStore;

    @BeforeEach
    void setUp() {
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("text-embedding-3-small")
                .build();
        this.embeddingStore = new InMemoryEmbeddingStore<>();
    }

    @Test
    void should_add_and_retrieve_embedding() {
        // 添加嵌入
        TextSegment segment = TextSegment.from("测试文本");
        float[] vector = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
        Embedding embedding = Embedding.from(vector);
        embeddingStore.add(embedding, segment);

        // 验证添加成功
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(
            EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(new float[1]))
                .maxResults(10)
                .build()
        );
        List<EmbeddingMatch<TextSegment>> allEmbeddings = searchResult.matches();
        assertEquals(1, allEmbeddings.size());
        
        // 检索
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(vector))
                .maxResults(1)
                .minScore(0.0)
                .build();
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);
        assertEquals(1, result.matches().size());
    }

    @Test
    void should_find_most_similar() {
        // 添加多个嵌入
        float[] vector1 = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
        float[] vector2 = {0.2f, 0.3f, 0.4f, 0.5f, 0.6f};
        float[] vector3 = {0.9f, 0.8f, 0.7f, 0.6f, 0.5f};
        float[] query = {0.15f, 0.25f, 0.35f, 0.45f, 0.55f};

        embeddingStore.add(Embedding.from(vector1), TextSegment.from("文本1"));
        embeddingStore.add(Embedding.from(vector2), TextSegment.from("文本2"));
        embeddingStore.add(Embedding.from(vector3), TextSegment.from("文本3"));

        // 搜索
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(query))
                .maxResults(2)
                .minScore(0.0)
                .build();
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> results = searchResult.matches();

        // 验证结果：query 应该与 vector1 最相似
        assertEquals(2, results.size());
        assertEquals("文本1", results.get(0).embedded().text());
        assertEquals("文本2", results.get(1).embedded().text());
    }

    @Test
    void should_honor_min_score() {
        // 添加嵌入
        float[] vector1 = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
        float[] vector2 = {0.5f, 0.6f, 0.7f, 0.8f, 0.9f};
        float[] query = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};

        embeddingStore.add(Embedding.from(vector1), TextSegment.from("相似"));
        embeddingStore.add(Embedding.from(vector2), TextSegment.from("不相似"));

        // 搜索（minScore = 0.8）
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(query))
                .maxResults(10)
                .minScore(0.8)
                .build();
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> results = searchResult.matches();

        // 应该只返回相似度 >= 0.8 的结果
        for (EmbeddingMatch<TextSegment> result : results) {
            assertTrue(result.score() >= 0.8);
        }
    }

    @Test
    void should_return_max_results() {
        // 添加多个嵌入
        for (int i = 0; i < 10; i++) {
            float[] vector = {0.1f * i, 0.2f * i, 0.3f * i, 0.4f * i, 0.5f * i};
            embeddingStore.add(Embedding.from(vector), TextSegment.from("文本" + i));
        }

        float[] query = {0.15f, 0.25f, 0.35f, 0.45f, 0.55f};

        // 限制结果为 3
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(query))
                .maxResults(3)
                .minScore(0.0)
                .build();
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> results = searchResult.matches();

        // 验证最多返回 3 个结果
        assertTrue(results.size() <= 3);
    }

    @Test
    void should_clear_all() {
        // 添加嵌入
        for (int i = 0; i < 5; i++) {
            float[] vector = {0.1f * i, 0.2f * i, 0.3f * i, 0.4f * i, 0.5f * i};
            embeddingStore.add(Embedding.from(vector), TextSegment.from("文本" + i));
        }

        // 清空
        embeddingStore.removeAll();

        // 验证清空
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(
            EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(new float[1]))
                .maxResults(100)
                .build()
        );
        assertTrue(searchResult.matches().isEmpty());
    }

    @Test
    void should_support_user_metadata() {
        float[] vector = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};

        // 创建带元数据的 TextSegment
        dev.langchain4j.data.document.Metadata metadata = 
            dev.langchain4j.data.document.Metadata.from(
                java.util.Map.of(
                    "document_id", "doc123",
                    "category", "技术",
                    "author", "张三"
                )
            );

        TextSegment segment = TextSegment.from("测试文本", metadata);
        embeddingStore.add(Embedding.from(vector), segment);

        // 检索
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(vector))
                .maxResults(1)
                .minScore(0.0)
                .build();
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> results = searchResult.matches();

        // 验证元数据
        dev.langchain4j.data.document.Metadata retrievedMetadata = results.get(0).embedded().metadata();
        assertNotNull(retrievedMetadata);
        assertEquals("doc123", retrievedMetadata.getString("document_id"));
        assertEquals("技术", retrievedMetadata.getString("category"));
        assertEquals("张三", retrievedMetadata.getString("author"));
    }
}
```

## 实践练习

### 练习 1：实现增量索引

实现一个支持增量索引的文档检索系统：

```java
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 增量索引的文档检索系统
 */
public class IncrementalDocumentIndexer {

    private final EmbeddingModel embeddingModel;
    private final Map<String, float[]> documentCache;
    private final Set<String> indexedDocuments;

    public IncrementalDocumentIndexer(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        this.documentCache = new ConcurrentHashMap<>();
        this.indexedDocuments = ConcurrentHashMap.newKeySet();
    }

    /**
     * 增量索引新文档
     */
    public void indexNewDocuments(List<Document> documents) {
        System.out.println("=== 开始增量索引 ===");
        System.out.println("待索引文档数: " + documents.size());
        System.out.println();

        int indexedCount = 0;
        int skippedCount = 0;

        for (Document doc : documents) {
            // 检查是否已索引
            if (indexedDocuments.contains(doc.getId())) {
                skippedCount++;
                System.out.println("跳过已索引文档: " + doc.getId());
                continue;
            }

            // 生成文档向量（使用整个文档）
            float[] vector = getDocumentVector(doc);
            documentCache.put(doc.getId(), vector);
            indexedDocuments.add(doc.getId());
            indexedCount++;
            
            System.out.println("已索引: " + doc.getId());
        }

        System.out.println();
        System.out.println("=== 索引完成 ===");
        System.out.println("新索引: " + indexedCount);
        System.out.println("跳过: " + skippedCount);
        System.out.println("总文档数: " + documentCache.size());
    }

    /**
     * 更新文档
     */
    public void updateDocument(Document document) {
        if (!indexedDocuments.contains(document.getId())) {
            System.out.println("文档不存在: " + document.getId());
            return;
        }

        System.out.println("更新文档: " + document.getId());
        float[] newVector = getDocumentVector(document);
        documentCache.put(document.getId(), newVector);
    }

    /**
     * 删除文档
     */
    public void deleteDocument(String documentId) {
        System.out.println("删除文档: " + documentId);
        documentCache.remove(documentId);
        indexedDocuments.remove(documentId);
    }

    /**
     * 搜索文档
     */
    public List<SearchResult> search(String query, int topK) {
        float[] queryVector = embeddingModel.embed(query).content().vector();

        List<SearchResult> results = new ArrayList<>();
        for (Map.Entry<String, float[]> entry : documentCache.entrySet()) {
            double similarity = cosineSimilarity(queryVector, entry.getValue());
            results.add(new SearchResult(entry.getKey(), similarity));
        }

        // 按相似度排序
        results.sort((a, b) -> Double.compare(b.similarity, a.similarity));

        // 返回前 topK 个
        return results.subList(0, Math.min(topK, results.size()));
    }

    private float[] getDocumentVector(Document document) {
        // 简化：实际应用中应该对文档内容进行适当的处理
        String text = document.getTitle() + " " + document.getContent();
        var response = embeddingModel.embed(text);
        return response.content().vector();
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static class Document {
        private final String id;
        private final String title;
        private final String content;

        public Document(String id, String title, String content) {
            this.id = id;
            this.title = title;
            this.content = content;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
    }

    public static class SearchResult {
        private final String documentId;
        private final double similarity;

        public SearchResult(String documentId, double similarity) {
            this.documentId = documentId;
            this.similarity = similarity;
        }

        public String getDocumentId() { return documentId; }
        public double getSimilarity() { return similarity; }
    }

    public static void main(String[] args) {
        EmbeddingModel model = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("text-embedding-3-small")
                .build();

        IncrementalDocumentIndexer indexer = new IncrementalDocumentIndexer(model);

        // 初始索引
        List<Document> initialDocs = List.of(
            new Document("doc1", "Java 简介", "Java 是一种面向对象的编程语言..."),
            new Document("doc2", "Python 教程", "Python 是一种解释型编程语言..."),
            new Document("doc3", "JavaScript 指南", "JavaScript 是一种脚本语言...")
        );

        indexer.indexNewDocuments(initialDocs);

        // 增量索引
        List<Document> newDocs = List.of(
            new Document("doc4", "Go 语言", "Go 是 Google 开发的语言..."),
            new Document("doc5", "Rust 语言", "Rust 是一种系统编程语言...")
        );

        indexer.indexNewDocuments(newDocs);

        // 搜索
        String query = "编程语言";
        List<SearchResult> results = indexer.search(query, 3);

        System.out.println();
        System.out.println("=== 搜索结果 ===");
        System.out.println("查询: " + query);
        System.out.println();

        for (int i = 0; i < results.size(); i++) {
            System.out.printf("%d. %s (相似度: %.4f)%n",
                    i + 1,
                    results.get(i).getDocumentId(),
                    results.get(i).getSimilarity());
        }

        // 更新文档
        indexer.updateDocument(new Document("doc1", "Java 编程（更新）", 
            "Java 是一种面向对象的编程语言（更新版本）..."));

        // 搜索验证更新
        List<SearchResult> results2 = indexer.search("Java", 2);
        System.out.println("\n更新后的搜索结果:");
        for (SearchResult result : results2) {
            System.out.println("  " + result.getDocumentId() + ": " + result.getSimilarity());
        }
    }
}
```

### 练习 2：实现向量缓存

实现一个向量缓存层以减少 Embedding 计算：

```java
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 向量缓存
 */
public class EmbeddingCache {

    private final EmbeddingModel embeddingModel;
    private final Map<String, CachedEmbedding> cache;
    private final int maxCacheSize;
    private final long cacheExpiryMinutes;

    public EmbeddingCache(EmbeddingModel embeddingModel, int maxCacheSize, long cacheExpiryMinutes) {
        this.embeddingModel = embeddingModel;
        this.maxCacheSize = maxCacheSize;
        this.cacheExpiryMinutes = cacheExpiryMinutes;
        
        // 使用 LinkedHashMap 实现 LRU 缓存
        this.cache = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > maxCacheSize;
            }
        };
    }

    /**
     * 获取或计算嵌入
     */
    public float[] getEmbedding(String text) {
        // 检查缓存
        CachedEmbedding cached = cache.get(text);
        
        if (cached != null && !cached.isExpired()) {
            System.out.println("缓存命中: " + text.substring(0, Math.min(20, text.length())) + "...");
            return cached.getVector();
        }

        System.out.println("缓存未命中: " + text.substring(0, Math.min(20, text.length())) + "...");

        // 计算新嵌入
        float[] vector = embeddingModel.embed(text).content().vector();

        // 存入缓存
        cache.put(text, new CachedEmbedding(vector));

        return vector;
    }

    /**
     * 预热缓存
     */
    public void warmup(List<String> texts) {
        System.out.println("=== 预热向量缓存 ===");
        System.out.println("预热文本数: " + texts.size());
        System.out.println();

        long startTime = System.currentTimeMillis();
        
        for (String text : texts) {
            getEmbedding(text);  // 使用缓存方法自动预填充
        }

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("预热完成，耗时: " + duration + "ms");
        System.out.println("缓存大小: " + cache.size());
        System.out.println();
    }

    /**
     * 清理过期缓存
     */
    public void cleanExpired() {
        int beforeSize = cache.size();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int afterSize = cache.size();
        System.out.println("清理了 " + (beforeSize - afterSize) + " 个过期缓存");
    }

    /**
     * 获取缓存统计
     */
    public CacheStats getStats() {
        int hitCount = 0;
        int missCount = 0;

        for (CachedEmbedding cached : cache.values()) {
            if (cached.hitCount > 0) {
                hitCount += cached.hitCount;
            } else {
                missCount++;
            }
        }

        return new CacheStats(
            cache.size(),
            hitCount,
            missCount,
            hitCount / (double) (hitCount + missCount)
        );
    }

    /**
     * 清空缓存
     */
    public void clear() {
        cache.clear();
        System.out.println("缓存已清空");
    }

    private static class CachedEmbedding {
        private final float[] vector;
        private final long timestamp;
        private int hitCount;

        public CachedEmbedding(float[] vector) {
            this.vector = vector;
            this.timestamp = System.currentTimeMillis();
            this.hitCount = 0;
        }

        public float[] getVector() {
            hitCount++;
            return vector;
        }

        public boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > 
                   TimeUnit.MINUTES.toMillis(cacheExpiryMinutes);
        }
    }

    public static class CacheStats {
        private final int size;
        private final int hitCount;
        private final int missCount;
        private final double hitRate;

        public CacheStats(int size, int hitCount, int missCount, double hitRate) {
            this.size = size;
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.hitRate = hitRate;
        }

        public int getSize() { return size; }
        public int getHitCount() { return hitCount; }
        public int getMissCount() { return missCount; }
        public double getHitRate() { return hitRate; }
    }

    public static void main(String[] args) {
        EmbeddingModel model = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("text-embedding-3-small")
                .build();

        EmbeddingCache cache = new EmbeddingCache(model, 100, 60); // 最多100个，60分钟过期

        // 预热缓存
        List<String> warmupTexts = List.of(
            "Java",
            "Python",
            "JavaScript",
            "C++",
            "Go",
            "Rust"
        );

        cache.warmup(warmupTexts);

        // 获取统计
        CacheStats stats = cache.getStats();
        System.out.println("=== 缓存统计 ===");
        System.out.println("缓存大小: " + stats.getSize());
        System.out.println("命中次数: " + stats.getHitCount());
        System.out.println("未命中次数: " + stats.getMissCount());
        System.out.printf("命中率: %.2f%%%n", stats.getHitRate() * 100);
        System.out.println();

        // 测试缓存效果
        String query = "编程语言";
        System.out.println("=== 测试缓存效果 ===");
        System.out.println("第1次查询...");
        cache.getEmbedding(query);  // 缓存未命中
        System.out.println();

        System.out.println("第2次查询...");
        cache.getEmbedding(query);  // 缓存命中
        System.out.println();

        System.out.println("第3次查询...");
        cache.getEmbedding(query);  // 缓存命中
        System.out.println();
    }
}
```

### 练习 3：实现多租户向量存储

实现一个支持多租户的向量存储系统：

```java
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多租户向量存储
 */
public class MultiTenantEmbeddingStore {

    private final EmbeddingModel embeddingModel;
    private final Map<String, TenantEmbeddingStore> tenantStores;
    private final Map<String, Float> globalCache;

    public MultiTenantEmbeddingStore(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        this.tenantStores = new ConcurrentHashMap<>();
        this.globalCache = new ConcurrentHashMap<>();
    }

    /**
     * 获取或创建租户存储
     */
    public TenantEmbeddingStore getTenantStore(String tenantId) {
        return tenantStores.computeIfAbsent(tenantId, id -> {
            System.out.println("为租户 " + tenantId + " 创建向量存储");
            return new TenantEmbeddingStore(id, embeddingModel, globalCache);
        });
    }

    /**
     * 租户添加文档
     */
    public void addDocument(String tenantId, Document document) {
        TenantEmbeddingStore tenantStore = getTenantStore(tenantId);
        tenantStore.addDocument(document);
    }

    /**
     * 租户搜索
     */
    public List<SearchResult> search(String tenantId, String query, int topK) {
        TenantEmbeddingStore tenantStore = getTenantStore(tenantId);
        return tenantStore.search(query, topK);
    }

    /**
     * 获取所有租户统计
     */
    public Map<String, TenantStats> getAllTenantStats() {
        Map<String, TenantStats> stats = new HashMap<>();
        
        for (Map.Entry<String, TenantEmbeddingStore> entry : tenantStores.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().getStats());
        }
        
        return stats;
    }

    /**
     * 删除租户
     */
    public void deleteTenant(String tenantId) {
        TenantEmbeddingStore store = tenantStores.remove(tenantId);
        if (store != null) {
            store.clear();
            System.out.println("已删除租户: " + tenantId);
        }
    }

    public static class Document {
        private final String id;
        private final String title;
        private final String content;

        public Document(String id, String title, String content) {
            this.id = id;
            this.title = title;
            this.content = content;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
    }

    public static class SearchResult {
        private final String documentId;
        private final double score;

        public SearchResult(String documentId, double score) {
            this.documentId = documentId;
            this.score = score;
        }

        public String getDocumentId() { return documentId; }
        public double getScore() { return score; }
    }

    public static class TenantStats {
        private final int documentCount;
        private final int segmentCount;

        public TenantStats(int documentCount, int segmentCount) {
            this.documentCount = documentCount;
            this.segmentCount = segmentCount;
        }

        public int getDocumentCount() { return documentCount; }
        public int getSegmentCount() { return segmentCount; }
    }

    public static void main(String[] args) {
        EmbeddingModel model = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("text-embedding-3-small")
                .build();

        MultiTenantEmbeddingStore store = new MultiTenantEmbeddingStore(model);

        // 租户1 添加文档
        store.addDocument("tenant1", new Document("t1doc1", "Java 教程", "..."));
        store.addDocument("tenant1", new Document("t1doc2", "Python 教程", "..."));
        store.addDocument("tenant1", new Document("t1doc3", "JavaScript 教程", "..."));

        // 租户2 添加文档
        store.addDocument("tenant2", new Document("t2doc1", "Go 语言", "..."));
        store.addDocument("tenant2", new Document("t2doc2", "Rust 语言", "..."));

        // 搜索
        System.out.println("=== 租户1 搜索 ===");
        String query1 = "编程语言";
        List<SearchResult> results1 = store.search("tenant1", query1, 2);
        for (SearchResult result : results1) {
            System.out.println("  " + result.getDocumentId() + ": " + result.getScore());
        }
        System.out.println();

        System.out.println("=== 租户2 搜索 ===");
        String query2 = "系统语言";
        List<SearchResult> results2 = store.search("tenant2", query2, 2);
        for (SearchResult result : results2) {
            System.out.println("  " + result.getDocumentId() + ": " + result.getScore());
        }
        System.out.println();

        // 显示所有租户统计
        System.out.println("=== 所有租户统计 ===");
        Map<String, TenantStats> allStats = store.getAllTenantStats();
        for (Map.Entry<String, TenantStats> entry : allStats.entrySet()) {
            System.out.println("租户 " + entry.getKey() + ":");
            System.out.println("  文档数: " + entry.getValue().getDocumentCount());
            System.out.println("  分段数: " + entry.getValue().getSegmentCount());
        }
    }
}
```

## 总结

### 本章要点

1. **EmbeddingStore 的作用**
   - 专门存储和检索向量
   - 支持高效的相似度搜索
   - 管理文档元数据

2. **常用实现**
   - `InMemoryEmbeddingStore` - 内存存储，适合小型应用
   - 向量数据库集成（Milvus、Pinecone、Weaviate 等）- 适合生产环境

3. **CRUD 操作**
   - 添加（add）- 添加单个或批量嵌入
   - 查询（findRelevant）- 根据向量相似度搜索
   - 删除（removeAll）- 清空存储

4. **搜索策略**
   - 参数控制（maxResults、minScore、scoreThreshold）
   - 混合搜索（向量 + 关键词）
   - 高级过滤

5. **实际应用**
   - 文档检索系统
   - 增量索引
   - 向量缓存
   - 多租户支持

### 下一步

在下一章节中，我们将学习：
- Tool 工具调用基础
- 如何定义和创建自定义工具
- 工具参数和返回值
- 工具执行和错误处理

### 常见问题

**Q1：InMemoryEmbeddingStore 什么时候够用？**

A：
- 开发和测试阶段
- 小型应用（文档数 < 10,000）
- 原型验证
- 数据量不大的场景

**Q2：如何选择向量数据库？**

A：考虑以下因素：
- 数据量：小数据用内存，大数据用向量数据库
- 性能要求：高并发场景需要专业数据库
- 成本考虑：开源 vs 商业方案
- 功能需求：特定功能（如元数据过滤、分页等）

**Q3：向量搜索的时间复杂度是多少？**

A：
- 线性搜索：O(N)，其中 N 是向量数量
- 带索引的搜索：O(log N)
- 近似最近邻（ANN）算法：O(1) 或 O(log N)，但可能牺牲一定精度

**Q4：如何提高向量搜索的性能？**

A：
- 使用专业的向量数据库（如 Milvus、Pinecone）
- 启用索引（HNSW、IVF 等）
- 批量处理
- 缓存查询结果
- 设置合适的参数（maxResults、minScore）

**Q5：如何处理向量更新？**

A：
- 重新生成并替换旧向量
- 使用版本控制避免并发问题
- 实现增量更新策略
- 考虑使用软删除（标记而非物理删除）

## 参考资料

- [LangChain4j EmbeddingStore 文档](https://docs.langchain4j.dev/tutorials/embedding-stores)
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
