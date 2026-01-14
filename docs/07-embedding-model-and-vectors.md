---
title: 'Embedding 模型和向量'
description: '学习 LangChain4j 的 Embedding 模型和向量 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 07 - EmbeddingModel 和向量

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 理解什么是 Embedding（嵌入）
- 掌握 EmbeddingModel 的使用方法
- 了解向量表示的含义
- 学会计算文本之间的相似度
- 理解不同 Embedding 模型的区别

## 前置知识

- 完成《01 - LangChain4j 简介》章节
- 完成《02 - 你的第一个 Chat 应用》章节
- 了解基本的线性代数概念（可选）

## 核心概念

### 什么是 Embedding？

**Embedding（嵌入）**是将文本、图像或其他数据转换为数值向量（Vector）的过程。

**为什么需要 Embedding？**

1. **语义理解** - 计算机可以理解词语之间的语义关系
2. **相似度计算** - 可以计算两个文本的相似程度
3. **搜索和检索** - 可以快速找到最相似的文档
4. **机器学习输入** - 向量可以作为机器学习模型的输入

**类比理解：**
如果把文本比作一个复杂的地址（如"北京市海淀区中关村"），那么 Embedding 就像是将其转换为经纬度坐标（39.98°N, 116.31°E）。有了坐标，就可以轻松计算两个地址的距离（即文本的相似度）。

**示例：**

```
文本: "猫"
Embedding: [0.234, -0.567, 0.891, ...]  （768维向量）

文本: "狗"
Embedding: [0.123, -0.432, 0.765, ...]  （768维向量）

相似度: 0.78（猫和狗都是动物，有一定相似度）
```

### 向量的维度

Embedding 生成的向量通常有固定的维度（Dimension）：

| 模型 | 维度 | 用途 |
|------|------|------|
| text-embedding-3-small | 1536 | 通用场景，性能好 |
| text-embedding-ada-002 | 1536 | 平衡性能和质量 |
| text-embedding-3-large | 3072 | 高质量，需要更多计算 |
| bge-base-zh-v1.5 | 1024 | 中文模型，1024维 |

**维度的意义：**
- 更高维度通常能表示更多信息
- 但也意味着更多的计算和存储成本
- 需要根据应用场景选择合适的模型

### Embedding 应用场景

1. **语义搜索** - 找到与查询最相似的文档
2. **推荐系统** - 根据用户喜好推荐相似内容
3. **聚类分析** - 将相似的文档分组
4. **去重** - 识别重复或相似的内容
5. **知识库检索** - RAG（检索增强生成）的基础

## EmbeddingModel 接口

### 接口定义

```java
public interface EmbeddingModel {
    
    /**
     * 将文本嵌入为向量
     * @param text 要嵌入的文本
     * @return 向量表示
     */
    Response embed(String text);
    
    /**
     * 批量嵌入多个文本
     * @param textSegments 要嵌入的文本片段列表
     * @return 包含向量列表的响应对象
     */
    Response<List<Embedding>> embedAll(List<TextSegment> textSegments);
}
```

### 基本使用

```java
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.data.embedding.Embedding;

// 创建 Embedding 模型
EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("text-embedding-3-small")
        .build();

// 嵌入单个文本
Response<Embedding> response = embeddingModel.embed("什么是 Java？");
float[] embedding = response.content().vector();

System.out.println("向量维度: " + embedding.length);
System.out.println("向量: " + Arrays.toString(embedding));
```

### 常见 Embedding 模型提供商

#### OpenAI Embeddings

```java
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

EmbeddingModel model = OpenAiEmbeddingModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("text-embedding-3-small")  // 或 text-embedding-ada-002
        .build();
```

#### Hugging Face Embeddings

```java
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;

EmbeddingModel model = HuggingFaceEmbeddingModel.builder()
        .accessToken(System.getenv("HUGGINGFACE_ACCESS_TOKEN"))
        .modelId("sentence-transformers/all-MiniLM-L6-v2")
        .build();
```

#### Ollama Embeddings（本地）

```java
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;

EmbeddingModel model = OllamaEmbeddingModel.builder()
        .baseUrl("http://localhost:11434")
        .modelName("nomic-embed-text-v1.5")
        .build();
```

#### Cohere Embeddings

```java
import dev.langchain4j.model.cohere.CohereEmbeddingModel;

EmbeddingModel model = CohereEmbeddingModel.builder()
        .apiKey(System.getenv("COHERE_API_KEY"))
        .modelName("embed-english-v3.0")
        .build();
```

## 向量相似度计算

### 余弦相似度（Cosine Similarity）

余弦相似度是最常用的向量相似度计算方法：

```java
/**
 * 向量工具类
 */
public class VectorUtils {

    /**
     * 计算余弦相似度
     * @param vector1 向量 1
     * @param vector2 向量 2
     * @return 相似度（0-1之间，1 表示完全相同）
     */
    public static double cosineSimilarity(float[] vector1, float[] vector2) {
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
     * @param vector1 向量 1
     * @param vector2 向量 2
     * @return 距离（越小越相似）
     */
    public static double euclideanDistance(float[] vector1, float[] vector2) {
        double sum = 0.0;
        for (int i = 0; i < vector1.length; i++) {
            double diff = vector1[i] - vector2[i];
            sum += Math.pow(diff, 2);
        }
        return Math.sqrt(sum);
    }

    /**
     * 找到最相似的向量
     * @param query 查询向量
     * @param candidates 候选向量列表
     * @return 最相似的向量和其索引
     */
    public static SimilarityResult findMostSimilar(
            float[] query,
            List<float[]> candidates) {

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

    public static class SimilarityResult {
        final int index;
        final double similarity;

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
    }
}
```

### 完整示例：文本相似度计算

```java
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;

/**
 * 文本相似度示例
 */
public class TextSimilarityExample {

    private final EmbeddingModel embeddingModel;

    public TextSimilarityExample() {
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("text-embedding-3-small")
                .build();
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
    public String findMostSimilar(
            String query,
            List<String> candidates) {

        // 获取查询文本的 Embedding
        float[] queryVector = getEmbedding(query);

        // 获取所有候选文本的 Embedding
        List<float[]> candidateVectors = candidates.stream()
                .map(this::getEmbedding)
                .collect(java.util.stream.Collectors.toList());

        // 找到最相似的
        VectorUtils.SimilarityResult result = 
            VectorUtils.findMostSimilar(queryVector, candidateVectors);

        return candidates.get(result.getIndex());
    }

    private float[] getEmbedding(String text) {
        Response response = embeddingModel.embed(text);
        return response.content().vector();
    }

    public static void main(String[] args) {
        TextSimilarityExample example = new TextSimilarityExample();

        // 示例 1：计算两个文本的相似度
        String text1 = "我喜欢编程";
        String text2 = "我喜欢写代码";
        double similarity1 = example.calculateSimilarity(text1, text2);

        System.out.println("=== 文本相似度计算 ===");
        System.out.println("文本 1: " + text1);
        System.out.println("文本 2: " + text2);
        System.out.printf("相似度: %.4f%n", similarity1);
        System.out.println();

        // 示例 2：找到最相似的文本
        String query = "Java 编程";
        List<String> candidates = List.of(
            "Python 编程",
            "JavaScript 开发",
            "Java 开发",
            "C++ 编程",
            "前端开发"
        );

        String mostSimilar = example.findMostSimilar(query, candidates);

        System.out.println("=== 查找最相似的文本 ===");
        System.out.println("查询: " + query);
        System.out.println("最相似: " + mostSimilar);
        System.out.println();
        System.out.println("所有候选文本及其相似度:");
        for (String candidate : candidates) {
            double sim = example.calculateSimilarity(query, candidate);
            System.out.printf("  %s: %.4f%n", candidate, sim);
        }
    }
}
```

### 运行示例

```
=== 文本相似度计算 ===
文本 1: 我喜欢编程
文本 2: 我喜欢写代码
相似度: 0.8234

=== 查找最相似的文本 ===
查询: Java 编程
最相似: Java 开发

所有候选文本及其相似度:
  Python 编程: 0.6523
  JavaScript 开发: 0.5341
  Java 开发: 0.9127
  C++ 编程: 0.6789
  前端开发: 0.4321
```

## 批量 Embedding

### 为什么需要批量 Embedding？

1. **性能优化** - 批量处理比单独处理更快
2. **成本降低** - 减少 API 调用次数
3. **效率提升** - 减少网络开销

### 批量 Embedding 示例

```java
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;

import java.util.List;
import java.util.ArrayList;

/**
 * 批量 Embedding 示例
 */
public class BatchEmbeddingExample {

    private final EmbeddingModel embeddingModel;

    public BatchEmbeddingExample() {
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("text-embedding-3-small")
                .build();
    }

    /**
     * 批量嵌入文本列表
     */
    public List<float[]> embedTexts(List<String> texts) {
        long startTime = System.currentTimeMillis();

        // 将 String 转换为 TextSegment
        List<TextSegment> textSegments = texts.stream()
                .map(TextSegment::from)
                .collect(Collectors.toList());

        // 批量嵌入
        Response<List<Embedding>> response = embeddingModel.embedAll(textSegments);
        
        long endTime = System.currentTimeMillis();
        TokenUsage tokenUsage = response.tokenUsage();
        
        List<float[]> embeddings = new ArrayList<>();
        for (Embedding embedding : response.content()) {
            embeddings.add(embedding.vector());
        }
        
        // 显示统计信息
        System.out.println("=== 批量 Embedding 统计 ===");
        System.out.println("文本数量: " + texts.size());
        System.out.println("耗时: " + (endTime - startTime) + "ms");
        System.out.println("Token 使用: " + tokenUsage.totalTokenCount());
        System.out.println("成本估算: $" + estimateCost(tokenUsage));
        System.out.println();

        return embeddings;
    }

    /**
     * 计算文本片段的 Embedding（用于长文档）
     */
    public List<float[]> embedSegments(
            String text, 
            int segmentLength, 
            int overlap) {

        List<String> segments = new ArrayList<>();
        int start = 0;
        
        while (start < text.length()) {
            int end = Math.min(start + segmentLength, text.length());
            segments.add(text.substring(start, end));
            
            start += (segmentLength - overlap);
        }

        System.out.println("=== 文本分段 ===");
        System.out.println("原文长度: " + text.length());
        System.out.println("分段长度: " + segmentLength);
        System.out.println("重叠长度: " + overlap);
        System.out.println("分段数量: " + segments.size());
        System.out.println();

        return embedTexts(segments);
    }

    /**
     * 估算成本
     */
    private double estimateCost(TokenUsage tokenUsage) {
        // OpenAI text-embedding-3-small 价格（2024年参考）
        double pricePer1kTokens = 0.00002;  // 每 1K tokens $0.00002
        return (tokenUsage.totalTokenCount() / 1000.0) * pricePer1kTokens;
    }

    public static void main(String[] args) {
        BatchEmbeddingExample example = new BatchEmbeddingExample();

        // 示例 1：批量嵌入多个文本
        List<String> texts = List.of(
            "什么是 Java？",
            "什么是 Python？",
            "什么是 JavaScript？",
            "什么是 C++？"
        );

        List<float[]> embeddings = example.embedTexts(texts);

        System.out.println("Embedding 结果:");
        for (int i = 0; i < texts.size(); i++) {
            System.out.println(texts.get(i) + ":");
            System.out.println("  向量维度: " + embeddings.get(i).length);
        }
        System.out.println();

        // 示例 2：嵌入长文档的片段
        String longText = "LangChain4j 是一个用于构建大语言模型（LLM）应用的 Java 库。" +
                       "它提供了完整的工具链和组件，帮助开发者轻松地将大语言模型集成到 Java 应用程序中。" +
                       "该库的设计灵感来源于 LangChain（一个流行的 Python 框架），但充分利用了 Java 的类型安全性和面向对象特性。" +
                       "LangChain4j 支持多种 LLM 提供商，包括 OpenAI、Anthropic、Google、Mistral AI 等，" +
                       "也支持本地模型（如通过 Ollama），让开发者可以在完全控制的环境中运行模型。";

        System.out.println("=== 嵌入长文档片段 ===");
        List<float[]> segmentEmbeddings = example.embedSegments(
            longText, 
            100,    // 每段 100 字符
            20       // 重叠 20 字符
        );

        System.out.println("生成了 " + segmentEmbeddings.size() + " 个向量");
    }
}
```

## 实际应用场景

### 场景一：语义搜索

```java
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

/**
 * 语义搜索示例
 */
public class SemanticSearchExample {

    private final EmbeddingModel embeddingModel;
    private final List<Document> documents;

    public SemanticSearchExample(List<Document> documents) {
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("text-embedding-3-small")
                .build();
        this.documents = documents;
    }

    /**
     * 搜索最相关的文档
     */
    public SearchResult search(String query, int topK) {
        // 获取查询的 Embedding
        float[] queryVector = getEmbedding(query);

        // 计算所有文档的相似度
        List<SimilarityScore> scores = new ArrayList<>();
        for (Document doc : documents) {
            float[] docVector = getEmbedding(doc.content());
            double similarity = VectorUtils.cosineSimilarity(queryVector, docVector);
            scores.add(new SimilarityScore(doc, similarity));
        }

        // 按相似度排序
        scores.sort((a, b) -> Double.compare(b.similarity, a.similarity));

        // 返回前 topK 个结果
        List<Document> results = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, scores.size()); i++) {
            results.add(scores.get(i).document);
        }

        return new SearchResult(query, results);
    }

    private float[] getEmbedding(String text) {
        var response = embeddingModel.embed(text);
        return response.content().vector();
    }

    public static class Document {
        final String id;
        final String title;
        final String content;

        public Document(String id, String title, String content) {
            this.id = id;
            this.title = title;
            this.content = content;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
    }

    public static class SimilarityScore {
        final Document document;
        final double similarity;

        public SimilarityScore(Document document, double similarity) {
            this.document = document;
            this.similarity = similarity;
        }

        public Document getDocument() { return document; }
        public double getSimilarity() { return similarity; }
    }

    public static class SearchResult {
        final String query;
        final List<Document> results;

        public SearchResult(String query, List<Document> results) {
            this.query = query;
            this.results = results;
        }

        public void print() {
            System.out.println("=== 搜索结果 ===");
            System.out.println("查询: " + query);
            System.out.println("找到 " + results.size() + " 个相关文档:");
            System.out.println();
            
            for (int i = 0; i < results.size(); i++) {
                Document doc = results.get(i);
                System.out.println((i + 1) + ". " + doc.getTitle());
                System.out.println("   ID: " + doc.getId());
                System.out.println();
            }
        }
    }

    public static void main(String[] args) {
        // 创建文档集合
        List<Document> documents = List.of(
            new Document("doc1", "Java 简介", "Java 是一种面向对象的编程语言..."),
            new Document("doc2", "Python 简介", "Python 是一种解释型语言..."),
            new Document("doc3", "JavaScript 教程", "JavaScript 是 Web 开发的脚本语言..."),
            new Document("doc4", "C++ 指南", "C++ 是一种通用的编程语言..."),
            new Document("doc5", "Go 语言入门", "Go 是 Google 开发的开源语言...")
        );

        SemanticSearchExample search = new SemanticSearchExample(documents);

        // 搜索
        SearchResult result = search.search("如何学习 Java 编程？", 3);
        result.print();
    }
}
```

### 场景二：文本聚类

```java
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 文本聚类示例
 */
public class TextClusteringExample {

    private final EmbeddingModel embeddingModel;
    private final int numClusters;

    public TextClusteringExample(int numClusters) {
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("text-embedding-3-small")
                .build();
        this.numClusters = numClusters;
    }

    /**
     * 使用 K-Means 聚类算法
     */
    public Map<Integer, List<String>> cluster(List<String> texts) {
        // 1. 获取所有文本的 Embedding
        Map<String, float[]> embeddings = new HashMap<>();
        for (String text : texts) {
            embeddings.put(text, getEmbedding(text));
        }

        // 2. 初始化聚类中心（随机选择）
        List<String> textList = new ArrayList<>(texts);
        Map<Integer, float[]> centroids = new HashMap<>();
        Random random = new Random();

        for (int i = 0; i < numClusters; i++) {
            centroids.put(i, embeddings.get(textList.get(random.nextInt(textList.size()))));
        }

        // 3. 迭代优化聚类中心
        for (int iteration = 0; iteration < 10; iteration++) {
            // 为每个文本分配最近的聚类中心
            Map<Integer, List<String>> clusters = new HashMap<>();
            for (int i = 0; i < numClusters; i++) {
                clusters.put(i, new ArrayList<>());
            }

            for (String text : textList) {
                int bestCluster = findClosestCentroid(embeddings.get(text), centroids);
                clusters.get(bestCluster).add(text);
            }

            // 更新聚类中心
            centroids.clear();
            for (Map.Entry<Integer, List<String>> entry : clusters.entrySet()) {
                centroids.put(entry.getKey(), computeCentroid(entry.getValue(), embeddings));
            }
        }

        return clusters;
    }

    private int findClosestCentroid(float[] vector, Map<Integer, float[]> centroids) {
        int bestCluster = 0;
        double maxSimilarity = -1.0;

        for (Map.Entry<Integer, float[]> entry : centroids.entrySet()) {
            double similarity = VectorUtils.cosineSimilarity(vector, entry.getValue());
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                bestCluster = entry.getKey();
            }
        }

        return bestCluster;
    }

    private float[] computeCentroid(List<String> texts, Map<String, float[]> embeddings) {
        float[] centroid = new float[embeddings.values().iterator().next().length];
        
        for (String text : texts) {
            float[] vector = embeddings.get(text);
            for (int i = 0; i < centroid.length; i++) {
                centroid[i] += vector[i];
            }
        }
        
        // 计算平均值
        for (int i = 0; i < centroid.length; i++) {
            centroid[i] /= texts.size();
        }
        
        return centroid;
    }

    private float[] getEmbedding(String text) {
        var response = embeddingModel.embed(text);
        return response.content().vector();
    }

    public static void main(String[] args) {
        TextClusteringExample clustering = new TextClusteringExample(3);

        // 示例文本
        List<String> texts = List.of(
            "我喜欢苹果和香蕉",
            "我经常吃葡萄和橙子",
            "西红柿和黄瓜很好吃",
            "编程很有趣",
            "Java 是一门编程语言",
            "Python 也是一种编程语言"
        );

        // 执行聚类
        Map<Integer, List<String>> clusters = clustering.cluster(texts);

        // 显示结果
        System.out.println("=== 文本聚类结果 ===");
        for (Map.Entry<Integer, List<String>> entry : clusters.entrySet()) {
            System.out.println("聚类 " + entry.getKey() + ":");
            for (String text : entry.getValue()) {
                System.out.println("  - " + text);
            }
            System.out.println();
        }
    }
}
```

### 场景三：文本去重

```java
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

import java.util.*;

/**
 * 文本去重示例
 */
public class TextDeduplicationExample {

    private final EmbeddingModel embeddingModel;
    private final double similarityThreshold;

    public TextDeduplicationExample(double similarityThreshold) {
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("text-embedding-3-small")
                .build();
        this.similarityThreshold = similarityThreshold;
    }

    /**
     * 去除重复或高度相似的文本
     */
    public List<String> deduplicate(List<String> texts) {
        Set<String> uniqueTexts = new LinkedHashSet<>();
        List<DuplicateGroup> duplicates = new ArrayList<>();

        for (String text : texts) {
            boolean isDuplicate = false;

            // 检查是否与已有文本高度相似
            for (String existing : uniqueTexts) {
                double similarity = calculateSimilarity(text, existing);
                if (similarity > similarityThreshold) {
                    isDuplicate = true;
                    break;
                }
            }

            if (!isDuplicate) {
                uniqueTexts.add(text);
            }
        }

        return new ArrayList<>(uniqueTexts);
    }

    private double calculateSimilarity(String text1, String text2) {
        float[] vector1 = getEmbedding(text1);
        float[] vector2 = getEmbedding(text2);
        return VectorUtils.cosineSimilarity(vector1, vector2);
    }

    private float[] getEmbedding(String text) {
        var response = embeddingModel.embed(text);
        return response.content().vector();
    }

    public static class DuplicateGroup {
        final String originalText;
        final List<String> duplicates;

        public DuplicateGroup(String originalText, List<String> duplicates) {
            this.originalText = originalText;
            this.duplicates = duplicates;
        }
    }

    public static void main(String[] args) {
        TextDeduplicationExample deduplicator = new TextDeduplicationExample(0.9);

        // 包含重复的文本列表
        List<String> texts = List.of(
            "什么是 Java？",
            "什么是 Java？",           // 完全相同
            "如何学习 Java？",
            "怎么学习 Java？",         // 高度相似
            "Python 简介",
            "Python 教程",            // 较低相似
            "C++ 指南",
            "C++ 入门"               // 较低相似
        );

        System.out.println("=== 去重前 ===");
        System.out.println("文本数量: " + texts.size());
        System.out.println();

        // 去重
        List<String> uniqueTexts = deduplicator.deduplicate(texts);

        System.out.println("=== 去重后 ===");
        System.out.println("唯一文本数量: " + uniqueTexts.size());
        System.out.println("去重数量: " + (texts.size() - uniqueTexts.size()));
        System.out.println();

        System.out.println("唯一文本:");
        uniqueTexts.forEach(System.out::println);
    }
}
```

## 测试代码示例

```java
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * EmbeddingModel 测试
 */
class EmbeddingModelTest {

    private EmbeddingModel embeddingModel;

    @BeforeEach
    void setUp() {
        embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("text-embedding-3-small")
                .build();
    }

    @Test
    void should_embed_single_text() {
        String text = "Hello, World!";
        
        Response response = embeddingModel.embed(text);
        
        assertNotNull(response);
        assertNotNull(response.content());
        float[] embedding = response.content().vector();
        
        // 验证向量维度
        assertEquals(1536, embedding.length, 
            "text-embedding-3-small 应该生成 1536 维向量");
        
        // 验证向量值
        for (float value : embedding) {
            assertFalse(Float.isNaN(value), "向量不应包含 NaN");
            assertFalse(Float.isInfinite(value), "向量不应包含无穷大");
        }
        
        System.out.println("文本: " + text);
        System.out.println("向量维度: " + embedding.length);
    }

    @Test
    void should_embed_multiple_texts() {
        List<String> texts = List.of(
            "Java",
            "Python",
            "JavaScript"
        );
        
        List<TextSegment> textSegments = texts.stream()
                .map(TextSegment::from)
                .collect(Collectors.toList());
        
        Response<List<Embedding>> response = embeddingModel.embedAll(textSegments);
        
        assertNotNull(response);
        assertEquals(3, response.content().size());
        
        for (int i = 0; i < response.content().size(); i++) {
            assertNotNull(response.content().get(i).vector());
        }
        
        System.out.println("嵌入的文本数: " + texts.size());
        System.out.println("生成的向量数: " + response.content().size());
    }

    @Test
    void should_calculate_similarity() {
        String text1 = "猫";
        String text2 = "狗";
        String text3 = "猫";  // 应该与 text1 相似度为 1
        
        float[] vector1 = getEmbedding(text1);
        float[] vector2 = getEmbedding(text2);
        float[] vector3 = getEmbedding(text3);
        
        double sim12 = VectorUtils.cosineSimilarity(vector1, vector2);
        double sim13 = VectorUtils.cosineSimilarity(vector1, vector3);
        
        // 验证相似度
        assertTrue(sim13 == 1.0, "相同文本的相似度应该为 1.0");
        assertTrue(sim12 < 1.0, "不同文本的相似度应该小于 1.0");
        assertTrue(sim12 > 0.5, "猫和狗都是动物，应该有一定相似度");
        
        System.out.println("'猫' 和 '狗' 的相似度: " + sim12);
        System.out.println("'猫' 和 '猫' 的相似度: " + sim13);
    }

    private float[] getEmbedding(String text) {
        Response response = embeddingModel.embed(text);
        return response.content().vector();
    }

    @Test
    void should_find_most_similar() {
        String query = "编程语言";
        List<String> candidates = List.of(
            "Python",
            "Java",
            "JavaScript",
            "C++",
            "Go"
        );
        
        // 获取所有 Embedding
        List<float[]> vectors = candidates.stream()
                .map(this::getEmbedding)
                .collect(Collectors.toList());
        
        float[] queryVector = getEmbedding(query);
        
        // 找到最相似的
        VectorUtils.SimilarityResult result = 
            VectorUtils.findMostSimilar(queryVector, vectors);
        
        // 验证结果
        assertEquals(1, result.getIndex(), 
            "Java 应该是最相似的");
        assertTrue(result.getSimilarity() > 0.8, 
            "相似度应该较高");
        
        System.out.println("查询: " + query);
        System.out.println("最相似: " + candidates.get(result.getIndex()));
        System.out.printf("相似度: %.4f%n", result.getSimilarity());
    }
}
```

## 实践练习

### 练习 1：实现语义搜索引擎

创建一个简单的语义搜索引擎：

```java
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import java.util.*;

/**
 * 简单的语义搜索引擎
 */
public class SimpleSemanticSearchEngine {

    private final EmbeddingModel embeddingModel;
    private final Map<String, float[]> documentIndex;

    public SimpleSemanticSearchEngine() {
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("text-embedding-3-small")
                .build();
        this.documentIndex = new HashMap<>();
    }

    /**
     * 索引文档
     */
    public void indexDocument(String docId, String title, String content) {
        String combinedText = title + " " + content;
        float[] embedding = getEmbedding(combinedText);
        documentIndex.put(docId, embedding);
        
        System.out.println("已索引文档: " + title);
    }

    /**
     * 搜索文档
     */
    public List<SearchResult> search(String query, int topK) {
        float[] queryVector = getEmbedding(query);
        
        // 计算所有文档的相似度
        List<SearchResult> results = new ArrayList<>();
        for (Map.Entry<String, float[]> entry : documentIndex.entrySet()) {
            double similarity = VectorUtils.cosineSimilarity(queryVector, entry.getValue());
            results.add(new SearchResult(entry.getKey(), similarity));
        }
        
        // 按相似度排序并返回前 topK 个
        results.sort((a, b) -> Double.compare(b.similarity, a.similarity));
        return results.subList(0, Math.min(topK, results.size()));
    }

    private float[] getEmbedding(String text) {
        var response = embeddingModel.embed(text);
        return response.content().vector();
    }

    public static class SearchResult {
        final String documentId;
        final double similarity;

        public SearchResult(String documentId, double similarity) {
            this.documentId = documentId;
            this.similarity = similarity;
        }

        public String getDocumentId() { return documentId; }
        public double getSimilarity() { return similarity; }
    }

    public static void main(String[] args) {
        SimpleSemanticSearchEngine engine = new SimpleSemanticSearchEngine();

        // 索引一些文档
        engine.indexDocument("doc1", "Java 编程入门", "Java 是一种面向对象的编程语言...");
        engine.indexDocument("doc2", "Python 基础教程", "Python 是一种解释型编程语言...");
        engine.indexDocument("doc3", "JavaScript 高级指南", "JavaScript 是 Web 开发的脚本语言...");
        engine.indexDocument("doc4", "C++ 实战教程", "C++ 是一种通用的编程语言...");
        engine.indexDocument("doc5", "Go 语言入门", "Go 是 Google 开发的开源语言...");

        System.out.println("已索引 " + engine.documentIndex.size() + " 个文档");
        System.out.println();

        // 搜索
        List<SearchResult> results = engine.search("如何学习编程语言？", 3);

        System.out.println("=== 搜索结果 ===");
        System.out.println("查询: 如何学习编程语言？");
        System.out.println("找到 " + results.size() + " 个相关文档:");
        System.out.println();

        for (int i = 0; i < results.size(); i++) {
            System.out.printf("%d. 文档 ID: %s, 相似度: %.4f%n",
                i + 1,
                results.get(i).getDocumentId(),
                results.get(i).getSimilarity());
        }
    }
}
```

### 练习 2：实现推荐系统

创建一个基于协同过滤的推荐系统：

```java
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import java.util.*;

/**
 * 基于内容的推荐系统
 */
public class ContentBasedRecommender {

    private final EmbeddingModel embeddingModel;
    private final Map<String, Item> itemIndex;

    public ContentBasedRecommender() {
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("text-embedding-3-small")
                .build();
        this.itemIndex = new HashMap<>();
    }

    /**
     * 添加商品
     */
    public void addItem(String itemId, String title, String description) {
        String itemText = title + " " + description;
        float[] embedding = getEmbedding(itemText);
        itemIndex.put(itemId, new Item(itemId, title, description, embedding));
    }

    /**
     * 为用户推荐商品
     */
    public List<Item> recommend(String userPreferences, int topN) {
        float[] userVector = getEmbedding(userPreferences);

        // 计算所有商品的相似度
        List<ItemScore> scores = new ArrayList<>();
        for (Item item : itemIndex.values()) {
            double similarity = VectorUtils.cosineSimilarity(userVector, item.embedding);
            scores.add(new ItemScore(item, similarity));
        }

        // 按相似度排序
        scores.sort((a, b) -> Double.compare(b.score, a.score));

        // 返回前 topN 个商品
        List<Item> recommendations = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, scores.size()); i++) {
            recommendations.add(scores.get(i).item);
        }

        return recommendations;
    }

    private float[] getEmbedding(String text) {
        var response = embeddingModel.embed(text);
        return response.content().vector();
    }

    public static class Item {
        final String id;
        final String title;
        final String description;
        final float[] embedding;

        public Item(String id, String title, String description, float[] embedding) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.embedding = embedding;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
    }

    public static class ItemScore {
        final Item item;
        final double score;

        public ItemScore(Item item, double score) {
            this.item = item;
            this.score = score;
        }
    }

    public static void main(String[] args) {
        ContentBasedRecommender recommender = new ContentBasedRecommender();

        // 添加一些商品
        recommender.addItem("item1", "Java 编程思想", "Bruce Eckel 著作...");
        recommender.addItem("item2", "Effective Java", "Joshua Bloch 著作...");
        recommender.addItem("item3", "Java 并发编程实战", "Brian Goetz 著作...");
        recommender.addItem("item4", "设计模式", "Erich Gamma 等人著作...");
        recommender.addItem("item5", "重构", "Martin Fowler 著作...");

        System.out.println("已索引 " + recommender.itemIndex.size() + " 个商品");
        System.out.println();

        // 用户偏好
        String userPreferences = "我想学习 Java 并发编程和设计模式";

        // 推荐
        List<Item> recommendations = recommender.recommend(userPreferences, 3);

        System.out.println("=== 推荐结果 ===");
        System.out.println("用户偏好: " + userPreferences);
        System.out.println("推荐 " + recommendations.size() + " 个商品:");
        System.out.println();

        for (int i = 0; i < recommendations.size(); i++) {
            Item item = recommendations.get(i);
            System.out.printf("%d. %s - %s%n",
                    i + 1,
                    item.getTitle(),
                    item.getDescription());
            System.out.printf("   相似度: %.4f%n%n",
                    VectorUtils.cosineSimilarity(
                        getEmbedding(userPreferences),
                        item.embedding
                    ));
        }
    }
}
```

### 练习 3：Embedding 性能对比

对比不同 Embedding 模型的性能：

```java
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Embedding 性能对比
 */
public class EmbeddingPerformanceBenchmark {

    public static void main(String[] args) {
        List<String> testTexts = List.of(
            "Java is a programming language",
            "Python is an interpreted language",
            "JavaScript is used for web development",
            "C++ is a general-purpose language",
            "Go is an open-source language",
            "Rust is focused on safety and performance",
            "TypeScript adds optional typing to JavaScript",
            "Swift is used for iOS development",
            "Kotlin is designed to interoperate with Java"
        );

        // 测试不同的模型
        testModel("text-embedding-3-small", testTexts);
        testModel("text-embedding-ada-002", testTexts);
        testModel("text-embedding-3-large", testTexts);
    }

        private static void testModel(String modelName, List<String> texts) {
        EmbeddingModel model = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName(modelName)
                .logRequests(true)
                .logResponses(true)
                .build();

        System.out.println("=== 测试模型: " + modelName + " ===");
        System.out.println("文本数量: " + texts.size());
        System.out.println();

        // 单个嵌入测试
        long singleStart = System.nanoTime();
        Response<Embedding> singleResponse = model.embed(texts.get(0));
        long singleEnd = System.nanoTime();
        long singleDuration = TimeUnit.NANOSECONDS.toMillis(singleEnd - singleStart);
        System.out.printf("单个嵌入耗时: %d ms%n", singleDuration);

        // 批量嵌入测试
        long batchStart = System.nanoTime();
        List<TextSegment> textSegments = texts.stream()
                .map(TextSegment::from)
                .collect(Collectors.toList());
        Response<List<Embedding>> batchResponse = model.embedAll(textSegments);
        long batchEnd = System.nanoTime();
        long batchDuration = TimeUnit.NANOSECONDS.toMillis(batchEnd - batchStart);
        System.out.printf("批量嵌入耗时: %d ms%n", batchDuration);
        System.out.printf("平均每个: %.2f ms%n%n", (double) batchDuration / texts.size());

        // Token 使用统计
        System.out.println("Token 使用统计:");
        System.out.println("  单个: " + singleResponse.tokenUsage().totalTokenCount());
        System.out.println("  批量: " + batchResponse.tokenUsage().totalTokenCount());
        System.out.printf("  节省: %d tokens (%.1f%%)%n",
                singleResponse.tokenUsage().totalTokenCount() - batchResponse.tokenUsage().totalTokenCount(),
                100.0 * (singleResponse.tokenUsage().totalTokenCount() - batchResponse.tokenUsage().totalTokenCount()) / singleResponse.tokenUsage().totalTokenCount());
        System.out.println();
    }

    private static float[] getEmbedding(EmbeddingModel model, String text) {
        Response<Embedding> response = model.embed(text);
        return response.content().vector();
    }
}
```

## 总结

### 本章要点

1. **Embedding 概念**
   - 将文本转换为数值向量
   - 用于计算相似度和语义理解
   - 是 RAG 和语义搜索的基础

2. **EmbeddingModel**
   - 提供文本到向量的转换
   - 支持单个和批量嵌入
   - 多种提供商选择（OpenAI、Hugging Face 等）

3. **向量相似度**
   - 余弦相似度是最常用的方法
   - 欧几里得距离等也是可选方法
   - 相似度范围通常是 0-1 之间

4. **应用场景**
   - 语义搜索
   - 推荐系统
   - 文本聚类
   - 文本去重

5. **性能优化**
   - 使用批量嵌入提高性能
   - 选择合适的模型平衡质量和速度
   - 缓存 Embedding 结果避免重复计算

### 下一步

在下一章节中，我们将学习：
- EmbeddingStore 向量存储
- 如何持久化向量数据
- 向量检索和搜索
- 搭建完整的 RAG 系统

### 常见问题

**Q1：Embedding 向量的维度越高越好吗？**

A：不一定。更高维度可以表示更多信息，但也会增加计算和存储成本。需要根据应用场景选择合适的模型。

**Q2：余弦相似度和欧几里得距离有什么区别？**

A：
- 余弦相似度：衡量向量方向的一致性（-1 到 1），1 表示完全相同
- 欧几里得距离：衡量向量的绝对距离（0 到无穷大），0 表示完全相同
- 余弦相似度对向量长度不敏感，更常用

**Q3：为什么需要批量 Embedding？**

A：
- 性能优化：批量处理比单独处理更快
- 成本降低：减少 API 调用次数
- 网络效率：减少网络开销

**Q4：Embedding 模型和 LLM 有什么区别？**

A：
- Embedding 模型：专门用于生成向量表示，用于相似度计算
- LLM：用于生成文本、对话等，通过概率预测下一个词
- Embedding 输出是固定维度的数值向量，LLM 输出是文本序列

**Q5：如何选择合适的 Embedding 模型？**

A：考虑以下因素：
- 语言（中文、英文、多语言）
- 应用场景（通用、特定领域）
- 性能要求（速度 vs 质量）
- 成本预算
- 支持的模型大小和维度

## 参考资料

- [LangChain4j Embedding 文档](https://docs.langchain4j.dev/tutorials/embeddings)
- [OpenAI Embeddings 文档](https://platform.openai.com/docs/guides/embeddings)
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
