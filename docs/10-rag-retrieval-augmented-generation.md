---
title: 'RAG 检索增强生成'
description: '学习 LangChain4j 的 RAG 检索增强生成 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 10 - RAG 检索增强生成

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 理解 RAG（Retrieval-Augmented Generation）的概念
- 掌握 RAG 的核心组件和工作原理
- 学会构建完整的 RAG 系统流程
- 理解文档分段、嵌入和检索策略
- 掌握上下文窗口管理和回答生成
- 实现一个基于 RAG 的问答系统

## 前置知识

- 完成《01 - LangChain4j 简介》章节
- 完成《02 - 你的第一个 Chat 应用》章节
- 完成《03 - 深入理解 ChatModel》章节
- 完成《07 - EmbeddingModel 和向量》章节
- 完成《08 - EmbeddingStore 向量存储》章节

## 核心概念

### 什么是 RAG？

**RAG（Retrieval-Augmented Generation，检索增强生成）**是一种结合了信息检索和大语言模型生成的技术架构。

**类比理解：**
如果把 LLM 比作一个知识渊博但有时会"幻觉"（胡编乱造）的专家，那么 RAG 就是给这个专家配备了一个随时可查阅的参考资料库（知识库）。

**为什么需要 RAG？**

1. **解决幻觉问题** - 减少 LLM 编造信息
2. **实时信息更新** - 无需重新训练模型即可更新知识
3. **可解释性** - 可以追溯答案来源
4. **成本优化** - 不需要为每个知识库微调大模型
5. **领域专业知识** - 让通用模型掌握特定领域知识

### RAG 的工作流程

```
┌─────────────────────────────────────────────────────────────┐
│                        知识库                              │
│                  ┌──────────────┐                              │
│                  │  文档 1-N   │                              │
│                  └──────────────┘                              │
└─────────────────────────┬───────────────────────────────────────┘
                      │
                      │ 1. 文档索引
                      │ (分段 + 嵌入)
                      ↓
┌─────────────────────────┴───────────────────────────────────────┐
│                   向量数据库 (EmbeddingStore)              │
│                  ┌──────────────┐                              │
│                  │  向量 1-M   │                              │
│                  └──────────────┘                              │
└─────────────────────────┬───────────────────────────────────────┘
                      │
                      │ 2. 向量检索
                      │ (相似度搜索)
                      ↓
                用户查询
                      │
                      ↓
            ┌─────────────────┐
            │  相关文档片段  │
            └─────────────────┘
                      │
                      │ 3. 构建提示词
                      │ (检索内容 + 问题)
                      ↓
┌─────────────────────────┴───────────────────────────────────────┐
│                       LLM                              │
└─────────────────────────┬───────────────────────────────────────┘
                      │
                      │ 4. 生成回答
                      │ (基于检索信息)
                      ↓
              ┌─────────────────┐
              │    最终答案     │
              └─────────────────┘
```

### RAG 的优势

| 特性 | 传统 LLM | RAG |
|------|-----------|-----|
| 知识更新 | 需要重新训练 | 索引即可更新 |
| 幻觉问题 | 较多 | 大幅减少 |
| 可解释性 | 难以追溯 | 可追溯来源 |
| 知识范围 | 仅限训练数据 | 可扩展任意文档 |
| 成本 | 高（微调费用）| 低（存储和检索） |
| 延迟 | 低 | 略高（检索时间）|

## RAG 核心组件

### 1. 文档分段器

将长文档分割成适合嵌入和检索的片段。

```java
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.splitter.DocumentSplitter;

/**
 * 文档分段器
 */
public class DocumentSplitter {

    /**
     * 按固定长度分段
     */
    public List<TextSegment> splitByLength(String text, int segmentLength) {
        List<TextSegment> segments = new ArrayList<>();
        
        for (int i = 0; i < text.length(); i += segmentLength) {
            int end = Math.min(i + segmentLength, text.length());
            segments.add(TextSegment.from(text.substring(i, end))));
        }
        
        return segments;
    }

    /**
     * 按段落分段
     */
    public List<TextSegment> splitByParagraphs(String text) {
        List<TextSegment> segments = new ArrayList<>();
        
        String[] paragraphs = text.split("\n\n+");
        for (String paragraph : paragraphs) {
            if (!paragraph.trim().isEmpty()) {
                segments.add(TextSegment.from(paragraph)));
            }
        }
        
        return segments;
    }

    /**
     * 智能分段（重叠 + 语义保持）
     */
    public List<TextSegment> splitIntelligently(
            String text, 
            int maxLength, 
            int overlap) {
        
        List<TextSegment> segments = new ArrayList<>();
        int start = 0;
        
        while (start < text.length()) {
            int end = Math.min(start + maxLength, text.length());
            String segment = text.substring(start, end);
            
            // 如果不在段落边界，尝试回退到最近的句子结尾
            if (!isAtSentenceEnd(segment, end)) {
                int lastPeriod = segment.lastIndexOf(".");
                if (lastPeriod > maxLength / 2) {  // 确保段落不会太短
                    end = start + lastPeriod + 1;
                    segment = text.substring(start, end);
                }
            }
            
            segments.add(TextSegment.from(segment));
            start += (segment.length() - overlap);
        }
        
        return segments;
    }

    private boolean isAtSentenceEnd(String segment, int end) {
        if (end == segment.length()) {
            return true;
        }
        
        char lastChar = segment.charAt(end - 1);
        return lastChar == '.' || lastChar == '!' || lastChar == '?';
    }
}
```

### 2. RAG 系统核心

```java
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.*;

/**
 * RAG 系统核心
 */
public class RAGSystem {

    private final ChatModel chatModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public RAGSystem(
            ChatModel chatModel,
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel) {
        this.chatModel = chatModel;
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    /**
     * 添加文档到知识库
     */
    public void addDocument(String documentId, String content) {
        System.out.println("索引文档: " + documentId);
        
        // 1. 分段文档
        DocumentSplitter splitter = new DocumentSplitter();
        List<TextSegment> segments = splitter.splitIntelligently(
            content, 
            500,    // 最大长度
            50       // 重叠长度
        );
        
        // 2. 为每个分段生成嵌入
        for (TextSegment segment : segments) {
            float[] embedding = embeddingModel.embed(segment.text())
                .content()
                .vector();
            
            // 3. 添加到向量存储
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("document_id", documentId);
            metadata.put("segment_index", segment.text().length());
            
            embeddingStore.add(Embedding.from(
                segment,
                embedding,
                metadata
            ));
        }
        
        System.out.println("已索引 " + segments.size() + " 个分段");
        System.out.println("向量存储中共有 " + 
            embeddingStore.getAll().size() + " 个向量");
        System.out.println();
    }

    /**
     * RAG 查询
     */
    public String query(String question, int topK) {
        // 1. 生成查询向量
        float[] queryEmbedding = embeddingModel.embed(question)
            .content()
            .vector();
        
        // 2. 向量检索
        List<Embedding<TextSegment>> relevantEmbeddings = 
            embeddingStore.findRelevant(queryEmbedding, topK, 0.0);
        
        // 3. 构建提示词
        String prompt = buildPrompt(question, relevantEmbeddings);
        
        // 4. 生成回答
        AiMessage response = chatModel.chat(prompt);
        
        // 5. 返回回答（可添加引用信息）
        return response.text();
    }

    /**
     * 构建 RAG 提示词
     */
    private String buildPrompt(
            String question, 
            List<Embedding<TextSegment>> relevantSegments) {
        
        StringBuilder prompt = new StringBuilder();
        
        // 系统提示词
        prompt.append("你是一个问答助手，请根据以下上下文信息回答用户的问题。\n");
        prompt.append("如果上下文中没有相关信息，请直接说不知道。\n\n");
        
        // 上下文信息
        prompt.append("上下文信息：\n");
        prompt.append("------------\n");
        
        for (int i = 0; i < relevantSegments.size(); i++) {
            TextSegment segment = relevantSegments.get(i).text();
            prompt.append("[段落 ").append(i + 1).append("] ");
            prompt.append(segment);
            prompt.append("\n\n");
        }
        
        prompt.append("------------\n\n");
        
        // 用户问题
        prompt.append("用户问题: ");
        prompt.append(question);
        
        return prompt.toString();
    }

    /**
     * 获取统计信息
     */
    public RAGStats getStats() {
        return new RAGStats(
            embeddingStore.getAll().size(),
            0  // 查询次数需要单独维护
        );
    }

    public static class RAGStats {
        private final int totalSegments;
        private final int queryCount;

        public RAGStats(int totalSegments, int queryCount) {
            this.totalSegments = totalSegments;
            this.queryCount = queryCount;
        }

        public int getTotalSegments() { return totalSegments; }
        public int getQueryCount() { return queryCount; }
    }
}
```

## 完整 RAG 系统

```java
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.*;

/**
 * 完整的 RAG 问答系统
 */
public class RAGQuestionAnsweringSystem {

    private final ChatModel chatModel;
    private final OpenAiEmbeddingModel embeddingModel;
    private final InMemoryEmbeddingStore<TextSegment> embeddingStore;
    private final Map<String, Document> documentRegistry;
    private final Map<String, Document> segmentToDocument;

    public RAGQuestionAnsweringSystem(String openaiApiKey) {
        // 初始化模型
        this.chatModel = OpenAiChatModel.builder()
                .apiKey(openaiApiKey)
                .modelName("gpt-4o-mini")
                .build();
        
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(openaiApiKey)
                .modelName("text-embedding-3-small")
                .build();
        
        this.embeddingStore = InMemoryEmbeddingStore.create();
        this.documentRegistry = new HashMap<>();
        this.segmentToDocument = new HashMap<>();
    }

    /**
     * 添加文档到知识库
     */
    public void addDocument(String documentId, String title, String content) {
        Document doc = new Document(documentId, title, content);
        documentRegistry.put(documentId, doc);
        
        // 分段并索引
        indexDocument(doc);
        
        System.out.println("✓ 文档已添加到知识库");
        System.out.println();
    }

    /**
     * 批量添加文档
     */
    public void addDocuments(List<Document> documents) {
        System.out.println("=== 批量添加文档 ===");
        System.out.println("文档数量: " + documents.size());
        System.out.println();
        
        for (Document doc : documents) {
            documentRegistry.put(doc.getId(), doc);
            indexDocument(doc);
        }
        
        System.out.println("✓ 所有文档已添加");
        System.out.println("知识库统计:");
        System.out.println("  文档数: " + documentRegistry.size());
        System.out.println("  向量数: " + embeddingStore.getAll().size());
        System.out.println();
    }

    /**
     * 索引文档
     */
    private void indexDocument(Document document) {
        // 分段
        List<TextSegment> segments = splitDocument(document.getContent());
        
        // 为每个分段生成嵌入
        int segmentIndex = 0;
        for (TextSegment segment : segments) {
            float[] embedding = embeddingModel.embed(segment.text())
                .content()
                .vector();
            
            // 创建带有元数据的嵌入
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("document_id", document.getId());
            metadata.put("document_title", document.getTitle());
            metadata.put("segment_index", segmentIndex++);
            
            // 添加到存储
            embeddingStore.add(Embedding.from(
                segment,
                embedding,
                metadata
            ));
            
            // 建立分段到文档的映射
            segmentToDocument.put(segment.text(), document);
        }
    }

    /**
     * 分段文档
     */
    private List<TextSegment> splitDocument(String content) {
        List<TextSegment> segments = new ArrayList<>();
        
        // 按段落分割
        String[] paragraphs = content.split("\n\n+");
        
        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                continue;
            }
            
            // 如果段落太长，继续分割
            if (paragraph.length() > 500) {
                for (int i = 0; i < paragraph.length(); i += 400) {
                    int end = Math.min(i + 400, paragraph.length());
                    segments.add(TextSegment.from(paragraph.substring(i, end))));
                }
            } else {
                segments.add(TextSegment.from(paragraph)));
            }
        }
        
        return segments;
    }

    /**
     * 查询
     */
    public RAGResponse query(String question, int topK) {
        System.out.println("=== RAG 查询 ===");
        System.out.println("问题: " + question);
        System.out.println("Top K: " + topK);
        System.out.println();
        
        // 1. 检索相关分段
        List<RetrievedSegment> retrievedSegments = retrieve(question, topK);
        
        // 2. 构建提示词
        String prompt = buildPrompt(question, retrievedSegments);
        
        System.out.println("检索到 " + retrievedSegments.size() + " 个相关片段");
        System.out.println();
        
        // 3. 生成回答
        AiMessage response = chatModel.chat(prompt);
        
        // 4. 构建响应
        RAGResponse ragResponse = new RAGResponse(
            response.text(),
            retrievedSegments
        );
        
        return ragResponse;
    }

    /**
     * 检索相关分段
     */
    private List<RetrievedSegment> retrieve(String query, int topK) {
        // 生成查询向量
        float[] queryVector = embeddingModel.embed(query)
            .content()
            .vector();
        
        // 向量搜索
        List<Embedding<TextSegment>> relevantEmbeddings = 
            embeddingStore.findRelevant(queryVector, topK, 0.0);
        
        // 转换为检索片段
        List<RetrievedSegment> retrievedSegments = new ArrayList<>();
        for (Embedding<TextSegment> embedding : relevantEmbeddings) {
            TextSegment segment = embedding.text();
            Map<String, Object> metadata = embedding.userMetadata();
            
            Document document = documentRegistry.get(
                metadata.get("document_id")
            );
            
            retrievedSegments.add(new RetrievedSegment(
                segment,
                document,
                embedding.score()
            ));
        }
        
        return retrievedSegments;
    }

    /**
     * 构建 RAG 提示词
     */
    private String buildPrompt(String question, List<RetrievedSegment> segments) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一个智能问答助手。");
        prompt.append("请基于以下上下文信息回答用户的问题。\n\n");
        
        prompt.append("上下文信息：\n");
        prompt.append("════════════════════════════════════\n");
        
        for (int i = 0; i < segments.size(); i++) {
            RetrievedSegment segment = segments.get(i);
            prompt.append("[文档 ").append(i + 1).append("] ");
            prompt.append(segment.getDocument().getTitle());
            prompt.append("\n");
            prompt.append(segment.getSegment().text());
            prompt.append("\n\n");
        }
        
        prompt.append("════════════════════════════════════\n\n");
        
        prompt.append("要求：\n");
        prompt.append("1. 只使用上下文中的信息回答\n");
        prompt.append("2. 如果上下文中没有相关信息，请直接说不知道\n");
        prompt.append("3. 回答要准确、简洁、易懂\n\n");
        
        prompt.append("用户问题: ");
        prompt.append(question);
        
        return prompt.toString();
    }

    /**
     * 获取统计信息
     */
    public SystemStats getSystemStats() {
        return new SystemStats(
            documentRegistry.size(),
            embeddingStore.getAll().size()
        );
    }

    /**
     * 清空知识库
     */
    public void clearKnowledgeBase() {
        documentRegistry.clear();
        segmentToDocument.clear();
        embeddingStore.removeAll();
        System.out.println("知识库已清空");
    }

    // 数据类
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

    public static class RetrievedSegment {
        private final TextSegment segment;
        private final Document document;
        private final Double score;

        public RetrievedSegment(TextSegment segment, Document document, Double score) {
            this.segment = segment;
            this.document = document;
            this.score = score;
        }

        public TextSegment getSegment() { return segment; }
        public Document getDocument() { return document; }
        public Double getScore() { return score; }
    }

    public static class RAGResponse {
        private final String answer;
        private final List<RetrievedSegment> sources;

        public RAGResponse(String answer, List<RetrievedSegment> sources) {
            this.answer = answer;
            this.sources = sources;
        }

        public String getAnswer() { return answer; }
        public List<RetrievedSegment> getSources() { return sources; }
    }

    public static class SystemStats {
        private final int documentCount;
        private final int segmentCount;

        public SystemStats(int documentCount, int segmentCount) {
            this.documentCount = documentCount;
            this.segmentCount = segmentCount;
        }

        public int getDocumentCount() { return documentCount; }
        public int getSegmentCount() { return segmentCount; }
    }

    public static void main(String[] args) {
        RAGQuestionAnsweringSystem rag = new RAGQuestionAnsweringSystem(
            System.getenv("OPENAI_API_KEY")
        );

        // 添加文档到知识库
        System.out.println("=== 构建知识库 ===");
        System.out.println();

        rag.addDocument("doc1", "LangChain4j 简介", 
            "LangChain4j 是一个用于构建 LLM 应用的 Java 库。" +
            "它提供了完整的工具链和组件，帮助开发者轻松地将大语言模型集成到 Java 应用程序中。" +
            "该库的设计灵感来源于 LangChain，但充分利用了 Java 的类型安全性和面向对象特性。");
        
        rag.addDocument("doc2", "Java 并发编程", 
            "Java 并发编程是 Java 中一个重要且复杂的主题。" +
            "Java 的多线程机制使得并发编程成为可能，但同时也带来了许多挑战，如线程安全、死锁、竞态条件等。" +
            "本专题将深入探讨 Java 并发的核心概念、常用工具和最佳实践，帮助开发者写出高效、安全的并发程序。");
        
        rag.addDocument("doc3", "Spring Boot 快速开始", 
            "Spring Boot 是一个基于 Spring 框架的快速应用开发框架。" +
            "它简化了基于 Spring 的应用开发过程，通过约定优于配置的理念，" +
            "使得开发者可以快速地创建独立运行、生产级别的 Spring 应用。" +
            "Spring Boot 提供了自动配置、内嵌服务器、监控和管理等特性。");

        System.out.println();
        
        // 查询测试
        String question1 = "什么是 LangChain4j？";
        System.out.println("查询 1: " + question1);
        RAGResponse response1 = rag.query(question1, 3);
        System.out.println("回答: " + response1.getAnswer());
        System.out.println();
        
        String question2 = "Java 并发编程有哪些挑战？";
        System.out.println("查询 2: " + question2);
        RAGResponse response2 = rag.query(question2, 3);
        System.out.println("回答: " + response2.getAnswer());
        System.out.println();
        
        String question3 = "Spring Boot 的特点是什么？";
        System.out.println("查询 3: " + question3);
        RAGResponse response3 = rag.query(question3, 2);
        System.out.println("回答: " + response3.getAnswer());
        System.out.println();
        
        // 显示系统统计
        SystemStats stats = rag.getSystemStats();
        System.out.println("=== 系统统计 ===");
        System.out.println("文档数: " + stats.getDocumentCount());
        System.out.println("分段数: " + stats.getSegmentCount());
    }
}
```

## RAG 优化策略

### 1. 文档分段优化

```java
/**
 * 高级文档分段器
 */
public class AdvancedDocumentSplitter {

    /**
     * 递归分段
     * 按层级结构分段文档（如标题、段落）
     */
    public List<TextSegment> splitByHierarchy(String text) {
        List<TextSegment> segments = new ArrayList<>();
        String[] lines = text.split("\n");
        
        StringBuilder currentSegment = new StringBuilder();
        String currentLevel = "";
        
        for (String line : lines) {
            // 检测标题层级
            String level = detectHeadingLevel(line);
            
            if (level != null && !level.equals(currentLevel)) {
                // 保存当前段落
                if (currentSegment.length() > 0) {
                    segments.add(TextSegment.from(currentSegment.toString())));
                    currentSegment = new StringBuilder();
                }
                currentLevel = level;
            }
            
            // 添加行到当前段落
            currentSegment.append(line).append("\n");
        }
        
        // 添加最后一个段落
        if (currentSegment.length() > 0) {
            segments.add(TextSegment.from(currentSegment.toString())));
        }
        
        return segments;
    }

    /**
     * 语义分段
     * 在语义边界处分段（如句子）
     */
    public List<TextSegment> splitBySentences(String text, int maxLength) {
        List<TextSegment> segments = new ArrayList<>();
        
        String[] sentences = text.split("(?<=[.!?])|\\n\\s*");
        StringBuilder currentSegment = new StringBuilder();
        
        for (String sentence : sentences) {
            if (currentSegment.length() + sentence.length() > maxLength) {
                if (currentSegment.length() > 0) {
                    segments.add(TextSegment.from(currentSegment.toString().trim())));
                }
                currentSegment = new StringBuilder(sentence);
            } else {
                currentSegment.append(sentence);
            }
        }
        
        if (currentSegment.length() > 0) {
            segments.add(TextSegment.from(currentSegment.toString().trim())));
        }
        
        return segments;
    }

    private String detectHeadingLevel(String line) {
        line = line.trim();
        
        if (line.matches("^#{1,3}\\s+.*")) {
            return "#" + line.indexOf(" ");
        }
        
        return null;
    }
}
```

### 2. 检索优化

```java
/**
 * 高级检索策略
 */
public class AdvancedRetrievalStrategy {

    /**
     * 混合检索（向量 + 关键词）
     */
    public List<TextSegment> hybridRetrieve(
            EmbeddingStore<TextSegment> store,
            EmbeddingModel embeddingModel,
            String query,
            int topK) {
        
        // 向量检索
        float[] queryVector = embeddingModel.embed(query)
            .content()
            .vector();
        
        List<Embedding<TextSegment>> vectorResults = 
            store.findRelevant(queryVector, topK, 0.0);
        
        // 关键词过滤
        List<TextSegment> filteredResults = new ArrayList<>();
        String[] keywords = extractKeywords(query);
        
        for (Embedding<TextSegment> embedding : vectorResults) {
            String text = embedding.text().text().toLowerCase();
            boolean containsKeyword = false;
            
            for (String keyword : keywords) {
                if (text.contains(keyword.toLowerCase())) {
                    containsKeyword = true;
                    break;
                }
            }
            
            if (containsKeyword) {
                filteredResults.add(embedding.text());
            }
        }
        
        return filteredResults;
    }

    /**
     * 多样性检索
     * 确保检索结果的多样性，避免重复或过于相似的内容
     */
    public List<TextSegment> diverseRetrieve(
            EmbeddingStore<TextSegment> store,
            EmbeddingModel embeddingModel,
            String query,
            int topK) {
        
        float[] queryVector = embeddingModel.embed(query)
            .content()
            .vector();
        
        List<Embedding<TextSegment>> candidates = 
            store.findRelevant(queryVector, topK * 2, 0.0);  // 获取更多候选
        
        List<TextSegment> selected = new ArrayList<>();
        Set<String> usedSources = new HashSet<>();
        
        for (Embedding<TextSegment> embedding : candidates) {
            String docId = (String) embedding.userMetadata().get("document_id");
            
            // 确保文档多样性
            if (!usedSources.contains(docId)) {
                selected.add(embedding.text());
                usedSources.add(docId);
                
                if (selected.size() >= topK) {
                    break;
                }
            }
        }
        
        return selected;
    }

    private String[] extractKeywords(String text) {
        // 简化：实际应用中应该使用更复杂的 NLP 技术
        return text.replaceAll("[^a-zA-Z0-9\u4e00-\u9fa5\\s]", " ")
                .split("\\s+");
    }
}
```

## 测试代码示例

```java
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RAG 系统测试
 */
class RAGSystemTest {

    private ChatModel chatModel;
    private OpenAiEmbeddingModel embeddingModel;
    private InMemoryEmbeddingStore<TextSegment> embeddingStore;

    @BeforeEach
    void setUp() {
        this.chatModel = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();
        
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("text-embedding-3-small")
                .build();
        
        this.embeddingStore = InMemoryEmbeddingStore.create();
    }

    @Test
    void should_index_document_and_retrieve() {
        // 索引文档
        String document = "Java 是一种编程语言。" +
                       "它具有平台无关性、面向对象等特性。" +
                       "Java 广泛应用于企业级应用开发。";
        
        List<TextSegment> segments = splitDocument(document);
        for (TextSegment segment : segments) {
            float[] vector = embeddingModel.embed(segment.text())
                .content()
                .vector();
            embeddingStore.add(Embedding.from(segment, vector));
        }
        
        // 检索
        float[] queryVector = embeddingModel.embed("Java 特性")
            .content()
            .vector();
        
        List<Embedding<TextSegment>> results = 
            embeddingStore.findRelevant(queryVector, 3, 0.0);
        
        // 验证
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.size() <= 3);
        
        // 至少有一个结果包含"平台无关性"
        boolean hasPlatformIndependence = results.stream()
                .any(emb -> emb.text().text().contains("平台无关性"));
        
        assertTrue(hasPlatformIndependence);
    }

    @Test
    void should_build_rag_prompt() {
        // 添加检索内容
        List<RetrievedSegment> retrievedSegments = new ArrayList<>();
        retrievedSegments.add(new RetrievedSegment(
            TextSegment.from("Java 是一种编程语言"),
            new Document("doc1", "Java 文档"),
            0.95
        ));
        
        // 构建提示词
        String prompt = buildPrompt("什么是 Java？", retrievedSegments);
        
        // 验证提示词包含必要的元素
        assertTrue(prompt.contains("上下文信息"));
        assertTrue(prompt.contains("用户问题"));
        assertTrue(prompt.contains("什么是 Java？"));
        assertTrue(prompt.contains("编程语言"));
    }

    @Test
    void should_generate_answer_from_context() {
        // 添加文档
        String document = "LangChain4j 是一个 Java 框架。" +
                       "它用于构建 LLM 应用。" +
                       "支持多种 LLM 提供商。";
        
        List<TextSegment> segments = splitDocument(document);
        for (TextSegment segment : segments) {
            float[] vector = embeddingModel.embed(segment.text())
                .content()
                .vector();
            embeddingStore.add(Embedding.from(segment, vector));
        }
        
        // 查询
        String question = "LangChain4j 支持哪些提供商？";
        
        // 构建 RAG 提示词
        float[] queryVector = embeddingModel.embed(question)
            .content()
            .vector();
        
        List<Embedding<TextSegment>> results = 
            embeddingStore.findRelevant(queryVector, 3, 0.0);
        
        String prompt = buildPrompt(question, results);
        
        // 生成回答
        String answer = chatModel.chat(prompt);
        
        // 验证
        assertNotNull(answer);
        assertFalse(answer.isEmpty());
        
        System.out.println("问题: " + question);
        System.out.println("回答: " + answer);
    }

    private List<TextSegment> splitDocument(String text) {
        List<TextSegment> segments = new ArrayList<>();
        
        String[] paragraphs = text.split("\\n\\n+");
        for (String paragraph : paragraphs) {
            segments.add(TextSegment.from(paragraph));
        }
        
        return segments;
    }

    private String buildPrompt(String question, List<Embedding<TextSegment>> segments) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("基于以下上下文回答问题:\n\n");
        
        for (Embedding<TextSegment> segment : segments) {
            prompt.append("- ").append(segment.text().text()).append("\n");
        }
        
        prompt.append("\n问题: ").append(question);
        
        return prompt.toString();
    }

    private static class Document {
        Document(String id, String title) {
            this.id = id;
            this.title = title;
        }
        String id;
        String title;
    }

    private static class RetrievedSegment {
        private final TextSegment segment;
        private final Document document;
        private final Double score;

        RetrievedSegment(TextSegment segment, Document document, Double score) {
            this.segment = segment;
            this.document = document;
            this.score = score;
        }

        TextSegment getSegment() { return segment; }
        Document getDocument() { return document; }
        Double getScore() { return score; }
    }
}
```

## 实践练习

### 练习 1：创建 RAG 聊天机器人

实现一个基于 RAG 的对话系统：

```java
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.service.AiServices;

import java.util.*;

/**
 * RAG 聊天机器人
 */
public class RAGChatBot {

    private final ChatModel chatModel;
    private final OpenAiEmbeddingModel embeddingModel;
    private final InMemoryEmbeddingStore<TextSegment> embeddingStore;

    public RAGChatBot(String openaiApiKey) {
        this.chatModel = OpenAiChatModel.builder()
                .apiKey(openaiApiKey)
                .modelName("gpt-4o-mini")
                .build();
        
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(openaiApiKey)
                .modelName("text-embedding-3-small")
                .build();
        
        this.embeddingStore = InMemoryEmbeddingStore.create();
    }

    /**
     * 知识库服务
     */
    public void addKnowledge(String topic, String content) {
        System.out.println("添加知识: " + topic);
        indexDocument(topic, content);
        System.out.println("✓ 知识已添加");
        System.out.println();
    }

    /**
     * 批量添加知识
     */
    public void addKnowledge(Map<String, String> knowledge) {
        System.out.println("=== 批量添加知识 ===");
        
        for (Map.Entry<String, String> entry : knowledge.entrySet()) {
            addKnowledge(entry.getKey(), entry.getValue());
        }
        
        System.out.println();
    }

    /**
     * RAG 对话
     */
    public String chat(String userMessage) {
        // RAG 检索
        float[] queryVector = embeddingModel.embed(userMessage)
            .content()
            .vector();
        
        List<Embedding<TextSegment>> relevantSegments = 
            embeddingStore.findRelevant(queryVector, 3, 0.0);
        
        // 构建 RAG 提示词
        String ragContext = buildRAGContext(relevantSegments);
        
        // 构建完整提示词
        String prompt = String.format(
            "你是 LangChain4j 的智能助手，请基于以下知识库信息回答用户的问题。\n\n" +
            "知识库信息：\n%s\n\n" +
            "用户问题：%s",
            ragContext,
            userMessage
        );
        
        // 生成回答
        String response = chatModel.chat(prompt);
        
        // 添加到向量存储以增强对话历史
        // 注意：实际应用中应该更谨慎地处理这一点
        float[] vector = embeddingModel.embed(userMessage)
            .content()
            .vector();
        
        TextSegment segment = TextSegment.from(
            "用户：" + userMessage + "\nAI:" + response
        );
        
        embeddingStore.add(Embedding.from(segment, vector));
        
        return response;
    }

    private void indexDocument(String topic, String content) {
        List<TextSegment> segments = splitDocument(content);
        
        for (TextSegment segment : segments) {
            float[] vector = embeddingModel.embed(segment.text())
                .content()
                .vector();
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("topic", topic);
            
            embeddingStore.add(Embedding.from(
                segment,
                vector,
                metadata
            ));
        }
    }

    private String buildRAGContext(List<Embedding<TextSegment>> segments) {
        StringBuilder context = new StringBuilder();
        
        for (Embedding<TextSegment> embedding : segments) {
            TextSegment segment = embedding.text();
            String topic = (String) embedding.userMetadata().get("topic");
            
            context.append("[主题: ").append(topic).append("] ");
            context.append(segment.text().text());
            context.append("\n\n");
        }
        
        return context.toString();
    }

    private List<TextSegment> splitDocument(String content) {
        List<TextSegment> segments = new ArrayList<>();
        
        String[] paragraphs = content.split("\n\n+");
        for (String paragraph : paragraphs) {
            if (!paragraph.trim().isEmpty()) {
                segments.add(TextSegment.from(paragraph)));
            }
        }
        
        return segments;
    }

    public static void main(String[] args) {
        RAGChatBot bot = new RAGChatBot(System.getenv("OPENAI_API_KEY"));

        // 添加知识库
        Map<String, String> knowledge = new HashMap<>();
        knowledge.put("LangChain4j 简介", 
            "LangChain4j 是一个用于构建 LLM 应用的 Java 库。");
        knowledge.put("Java 并发", 
            "Java 并发编程涉及多线程、线程安全、死锁等概念。");
        knowledge.put("Spring Boot", 
            "Spring Boot 简化了 Spring 应用的开发和部署。");
        
        bot.addKnowledge(knowledge);

        // 对话
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║              RAG 聊天机器人                   ║");
        System.out.println("╚════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("聊天中...（输入 'exit' 退出）");
        System.out.println();

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("用户: ");
            String input = scanner.nextLine().trim();

            if ("exit".equalsIgnoreCase(input)) {
                System.out.println("\n再见！");
                break;
            }

            if (input.isEmpty()) {
                continue;
            }

            String response = bot.chat(input);
            System.out.println("AI: " + response);
            System.out.println();
        }

        scanner.close();
    }
}
```

### 练习 2：实现增量知识更新

实现一个支持增量更新的 RAG 系统：

```java
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.*;

/**
 * 增量知识更新系统
 */
public class IncrementalKnowledgeSystem {

    private final ChatModel chatModel;
    private final OpenAiEmbeddingModel embeddingModel;
    private final InMemoryEmbeddingStore<TextSegment> embeddingStore;
    private final Map<String, DocumentVersion> documentVersions;

    public IncrementalKnowledgeSystem(String openaiApiKey) {
        this.chatModel = OpenAiChatModel.builder()
                .apiKey(openaiApiKey)
                .modelName("gpt-4o-mini")
                .build();
        
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(openaiApiKey)
                .modelName("text-embedding-3-small")
                .build();
        
        this.embeddingStore = InMemoryEmbeddingStore.create();
        this.documentVersions = new HashMap<>();
    }

    /**
     * 更新文档
     */
    public void updateDocument(String documentId, String newContent) {
        DocumentVersion oldVersion = documentVersions.get(documentId);
        
        if (oldVersion != null) {
            // 保存旧版本
            oldVersion.setActive(false);
            System.out.println("保留旧版本 " + oldVersion.getVersion());
        }
        
        // 创建新版本
        int version = oldVersion != null ? oldVersion.getVersion() + 1 : 1;
        Document newDocument = new Document(documentId, newContent, version, true);
        
        // 索引新版本
        indexDocument(newDocument);
        
        documentVersions.put(documentId, newDocument);
        System.out.println("文档 " + documentId + " 已更新到版本 " + version);
        System.out.println();
    }

    /**
     * 获取文档历史版本
     */
    public List<DocumentVersion> getDocumentHistory(String documentId) {
        List<DocumentVersion> history = new ArrayList<>();
        
        // 收集所有版本
        for (DocumentVersion doc : documentVersions.values()) {
            if (doc.getId().equals(documentId)) {
                history.add(doc);
            }
        }
        
        Collections.sort(history, (a, b) -> Integer.compare(b.getVersion(), a.getVersion()));
        
        return history;
    }

    /**
     * 回滚到指定版本
     */
    public void rollbackToVersion(String documentId, int version) {
        List<DocumentVersion> history = getDocumentHistory(documentId);
        
        for (DocumentVersion doc : history) {
            doc.setActive(doc.getVersion() == version);
        }
        
        System.out.println("文档 " + documentId + " 已回滚到版本 " + version);
    }

    private void indexDocument(Document document) {
        List<TextSegment> segments = splitDocument(document.getContent());
        
        for (TextSegment segment : segments) {
            float[] vector = embeddingModel.embed(segment.text())
                .content()
                .vector();
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("document_id", document.getId());
            metadata.put("version", document.getVersion());
            metadata.put("active", document.isActive());
            
            embeddingStore.add(Embedding.from(
                segment,
                vector,
                metadata
            ));
        }
    }

    private List<TextSegment> splitDocument(String content) {
        List<TextSegment> segments = new ArrayList<>();
        
        String[] paragraphs = content.split("\n\n+");
        for (String paragraph : paragraphs) {
            if (!paragraph.trim().isEmpty()) {
                segments.add(TextSegment.from(paragraph)));
            }
        }
        
        return segments;
    }

    public static class Document {
        private final String id;
        private final String content;
        private final int version;
        private boolean active;

        public Document(String id, String content, int version, boolean active) {
            this.id = id;
            this.content = content;
            this.version = version;
            this.active = active;
        }

        public String getId() { return id; }
        public String getContent() { return content; }
        public int getVersion() { return version; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    public static void main(String[] args) {
        IncrementalKnowledgeSystem system = new IncrementalKnowledgeSystem(
            System.getenv("OPENAI_API_KEY")
        );

        // 初始添加文档
        String docId = "doc1";
        system.updateDocument(docId, "初始内容版本 1");
        
        // 更新文档
        System.out.println("=== 文档更新 ===");
        system.updateDocument(docId, "更新后的内容版本 2");
        system.updateDocument(docId, "再次更新的内容版本 3");
        
        // 查看历史
        System.out.println();
        System.out.println("=== 文档历史 ===");
        List<DocumentVersion> history = system.getDocumentHistory(docId);
        for (DocumentVersion version : history) {
            System.out.printf("版本 %d: %s %s\n",
                    version.getVersion(),
                    version.getContent().substring(0, 20),
                    version.isActive() ? "(当前)" : ""
            );
        }
        
        // 回滚
        System.out.println();
        System.out.println("=== 回滚测试 ===");
        system.rollbackToVersion(docId, 2);
        
        List<DocumentVersion> afterRollback = system.getDocumentHistory(docId);
        for (DocumentVersion version : afterRollback) {
            System.out.printf("版本 %d: %s %s\n",
                    version.getVersion(),
                    version.getContent().substring(0, 20),
                    version.isActive() ? "(当前)" : ""
            );
        }
    }
}
```

### 练习 3：实现多知识库 RAG

实现一个支持多个独立知识库的 RAG 系统：

```java
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.*;

/**
 * 多知识库 RAG 系统
 */
public class MultiKnowledgeBaseRAGSystem {

    private final ChatModel chatModel;
    private final OpenAiEmbeddingModel embeddingModel;
    private final Map<String, KnowledgeBase> knowledgeBases;

    public MultiKnowledgeBaseRAGSystem(String openaiApiKey) {
        this.chatModel = OpenAiChatModel.builder()
                .apiKey(openaiApiKey)
                .modelName("gpt-4o-mini")
                .build();
        
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(openaiApiKey)
                .modelName("text-embedding-3-small")
                .build();
        
        this.knowledgeBases = new HashMap<>();
    }

    /**
     * 创建知识库
     */
    public void createKnowledgeBase(String kbId, String name) {
        KnowledgeBase kb = new KnowledgeBase(
            kbId,
            name,
            embeddingModel,
            chatModel
        );
        
        knowledgeBases.put(kbId, kb);
        System.out.println("创建知识库: " + kbId + " - " + name);
    }

    /**
     * 向知识库添加文档
     */
    public void addDocument(String kbId, String documentId, String content) {
        KnowledgeBase kb = knowledgeBases.get(kbId);
        if (kb != null) {
            kb.addDocument(documentId, content);
            System.out.println("✓ 已添加到知识库 " + kb.getName());
        } else {
            System.out.println("✗ 知识库 " + kbId + " 不存在");
        }
    }

    /**
     * 查询指定知识库
     */
    public String query(String kbId, String question, int topK) {
        KnowledgeBase kb = knowledgeBases.get(kbId);
        if (kb != null) {
            return kb.query(question, topK);
        }
        return "知识库 " + kbId + " 不存在";
    }

    /**
     * 查询所有知识库
     */
    public Map<String, String> queryAll(String question, int topKPerKB) {
        Map<String, String> results = new HashMap<>();
        
        for (Map.Entry<String, KnowledgeBase> entry : knowledgeBases.entrySet()) {
            String answer = entry.getValue().query(question, topKPerKB);
            results.put(entry.getKey(), answer);
        }
        
        return results;
    }

    /**
     * 获取知识库统计
     */
    public Map<String, KnowledgeBaseStats> getStats() {
        Map<String, KnowledgeBaseStats> stats = new HashMap<>();
        
        for (Map.Entry<String, KnowledgeBase> entry : knowledgeBases.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().getStats());
        }
        
        return stats;
    }

    /**
     * 知识库类
     */
    public static class KnowledgeBase {
        private final String id;
        private final String name;
        private final EmbeddingStore<TextSegment> embeddingStore;
        private final ChatModel chatModel;
        private final EmbeddingModel embeddingModel;
        private final Map<String, Document> documents;

        public KnowledgeBase(String id, String name, 
                            EmbeddingModel embeddingModel,
                            ChatModel chatModel) {
            this.id = id;
            this.name = name;
            this.embeddingModel = embeddingModel;
            this.chatModel = chatModel;
            this.embeddingStore = new InMemoryEmbeddingStore<>();
            this.documents = new HashMap<>();
        }

        public void addDocument(String documentId, String content) {
            Document doc = new Document(documentId, content);
            documents.put(documentId, doc);
            indexDocument(doc);
        }

        public String query(String question, int topK) {
            // 生成查询向量
            float[] queryVector = embeddingModel.embed(question)
                .content()
                .vector();
            
            // 向量检索
            List<Embedding<TextSegment>> relevantSegments = 
                embeddingStore.findRelevant(queryVector, topK, 0.0);
            
            // 构建提示词
            String prompt = buildPrompt(question, relevantSegments, name);
            
            // 生成回答
            String response = chatModel.chat(prompt);
            
            return response;
        }

        public KnowledgeBaseStats getStats() {
            return new KnowledgeBaseStats(
                id,
                name,
                documents.size(),
                embeddingStore.getAll().size()
            );
        }

        private void indexDocument(Document document) {
            List<TextSegment> segments = splitDocument(document.getContent());
            
            for (TextSegment segment : segments) {
                float[] vector = embeddingModel.embed(segment.text())
                    .content()
                    .vector();
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("document_id", document.getId());
                metadata.put("kb_id", id);
                
                embeddingStore.add(Embedding.from(
                    segment,
                    vector,
                    metadata
                ));
            }
        }

        private String buildPrompt(String question, 
                                List<Embedding<TextSegment>> segments, 
                                String kbName) {
            StringBuilder prompt = new StringBuilder();
            prompt.append("你是知识库「").append(kbName).append("」的智能助手。\n");
            prompt.append("请基于以下知识库信息回答问题。\n\n");
            
            prompt.append("知识库内容：\n");
            for (Embedding<TextSegment> embedding : segments) {
                prompt.append("- ").append(embedding.text().text()).append("\n");
            }
            
            prompt.append("\n问题: ").append(question);
            
            return prompt.toString();
        }

        private List<TextSegment> splitDocument(String content) {
            List<TextSegment> segments = new ArrayList<>();
            
            String[] paragraphs = content.split("\n\n+");
            for (String paragraph : paragraphs) {
                if (!paragraph.trim().isEmpty()) {
                    segments.add(TextSegment.from(paragraph)));
                }
            }
            
            return segments;
        }

        public String getId() { return id; }
        public String getName() { return name; }
    }

    public static class KnowledgeBaseStats {
        private final String id;
        private final String name;
        private final int documentCount;
        private final int segmentCount;

        public KnowledgeBaseStats(String id, String name, int documentCount, int segmentCount) {
            this.id = id;
            this.name = name;
            this.documentCount = documentCount;
            this.segmentCount = segmentCount;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public int getDocumentCount() { return documentCount; }
        public int getSegmentCount() { return segmentCount; }
    }

    public static class Document {
        private final String id;
        private final String content;

        public Document(String id, String content) {
            this.id = id;
            this.content = content;
        }

        public String getId() { return id; }
        public String getContent() { return content; }
    }

    public static void main(String[] args) {
        MultiKnowledgeBaseRAGSystem system = new MultiKnowledgeBaseRAGSystem(
            System.getenv("OPENAI_API_KEY")
        );

        // 创建多个知识库
        system.createKnowledgeBase("kb1", "LangChain4j 文档");
        system.createKnowledgeBase("kb2", "Spring Boot 文档");
        system.createKnowledgeBase("kb3", "Java 并发文档");

        // 添加文档
        System.out.println("=== 添加文档 ===");
        System.out.println();
        system.addDocument("kb1", "doc1", "LangChain4j 的核心组件...");
        system.addDocument("kb2", "doc1", "Spring Boot 的自动配置...");
        system.addDocument("kb3", "doc1", "Java 并发的线程安全...");
        System.out.println();

        // 查询所有知识库
        System.out.println("=== 查询所有知识库 ===");
        System.out.println("问题: 什么是 LangChain4j？");
        System.out.println();
        
        Map<String, String> results = system.queryAll("什么是 LangChain4j？", 2);
        
        for (Map.Entry<String, String> entry : results.entrySet()) {
            KnowledgeBase kb = system.knowledgeBases.get(entry.getKey());
            System.out.println("[" + kb.getName() + "] " + entry.getValue());
            System.out.println();
        }

        // 获取统计
        System.out.println("=== 知识库统计 ===");
        Map<String, KnowledgeBaseStats> stats = system.getStats();
        
        for (Map.Entry<String, KnowledgeBaseStats> entry : stats.entrySet()) {
            KnowledgeBaseStats kbStats = entry.getValue();
            System.out.printf("%s (%s):\n", kbStats.getName(), kbStats.getId());
            System.out.printf("  文档数: %d\n", kbStats.getDocumentCount());
            System.out.printf("  分段数: %d\n", kbStats.getSegmentCount());
            System.out.println();
        }
    }
}
```

## 总结

### 本章要点

1. **RAG 概念**
   - 结合检索和生成的技术
   - 减少幻觉，提供可追溯性
   - 支持实时知识更新

2. **核心组件**
   - 文档分段器
   - 向量数据库
   - 嵌入模型
   - 大语言模型

3. **工作流程**
   - 文档索引（分段 + 嵌入）
   - 向量检索（相似度搜索）
   - 提示词构建（上下文 + 问题）
   - 回答生成

4. **优化策略**
   - 智能分段
   - 混合检索
   - 多样性检索
   - 增量更新

5. **应用场景**
   - 问答系统
   - 文档助手
   - 知识库搜索
   - 客服机器人

### 下一步

在下一章节中，我们将学习：
- AI Services 高级特性
- 多轮对话中的 RAG 应用
- 流式输出优化
- 完整的 RAG 最佳实践
- 生产环境部署建议

### 常见问题

**Q1：RAG 的主要优势是什么？**

A：
1. 减少幻觉 - 基于实际数据回答
2. 可追溯性 - 可以引用来源
3. 灵活更新 - 无需重新训练即可更新知识
4. 成本优化 - 比微调更经济

**Q2：如何选择分段长度？**

A：考虑因素：
- 平均句子长度（通常 20-40 个词）
- 模型上下文窗口限制
- 检索效果测试
- 存储和检索性能

**Q3：Top K 应该设置多大？**

A：取决于：
- 问题复杂度 - 简单问题用小值（2-5），复杂问题用大值（10-20）
- 噪音容忍度 - 噪音多用小值，精确检索用大值
- 性能要求 - 性能敏感用小值

**Q4：如何评估 RAG 系统？**

A：评估指标：
- 准确率 - 回答是否正确
- 召回率 - 知识库中是否有答案
- 回答相关性 - 回答与问题的相关性
- 响应时间 - 整个 RAG 流程耗时

**Q5：如何处理知识库中的冲突信息？**

A：
- 优先级策略 - 给不同来源设置优先级
- 时间戳策略 - 使用最新的信息
- 来源标注 - 在回答中标注信息来源
- 信任度评分 - 对不同来源设置信任度

## 参考资料

- [LangChain4j RAG 文档](https://docs.langchain4j.dev/tutorials/rag)
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
