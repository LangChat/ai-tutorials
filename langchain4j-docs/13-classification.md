---
title: '意图识别'
description: '学习 LangChain4j 的 分类 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 13 - 意图识别

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 理解意图识别的概念和应用场景
- 掌握 `@Classifier` 注解的使用方法
- 学会实现自定义分类器
- 理解分类器的配置和参数
- 掌握多标签分类和零样本学习
- 实现一个完整的意图识别应用

## 前置知识

- 完成《01 - LangChain4j 简介》章节
- 完成《02 - 你的第一个 Chat 应用》章节
- 完成《03 - 深入理解 ChatModel》章节（可选，推荐）

## 核心概念

### 什么是意图识别？

**意图识别**是指将文本自动分配到一个或多个预定义类别的过程。

**类比理解：**
如果把 LLM 比作一个通用的翻译官，那么意图识别就像是专业的标签员 - 他们通读文章，然后给每个文章贴上对应的主题标签（如"科技"、"体育"、"娱乐"等）。

**为什么需要意图识别？**

1. **内容审核** - 自动识别有害或不当内容
2. **情感分析** - 判断文本的情感倾向（正面、负面、中性）
3. **新闻分类** - 自动归类新闻到不同领域
4. **垃圾邮件检测** - 识别垃圾邮件
5. **客户服务** - 自动分类用户咨询
6. **文档管理** - 自动归档和检索文档

### 分类类型

| 分类类型 | 说明 | 输出示例 |
|---------|------|---------|
| **二分类** | 将文本分为两个类别 | 垃圾邮件 / 正常邮件 |
| **多分类** | 将文本分为多个类别 | 科技 / 体育 / 娱乐 / 财经 |
| **多标签分类** | 文本可以属于多个类别 | [科技, Python], [科技, Java] |
| **层次分类** | 分类器有层级结构 | 编程语言 -> 动态语言 -> Java |
| **零样本分类** | 无需训练数据，基于提示词分类 | 新产品 -> 类别 |

## @Classifier 注解

### 基本使用

```java
import dev.langchain4j.service.classifier.*;

/**
 * 简单分类器
 */
public class SimpleClassifier {

    /**
     * 情感分类器
     * 
     * 返回值：POSITIVE（正面）, NEGATIVE（负面）, NEUTRAL（中性）
     */
    @Classifier(
        modelId = "gpt-4o-mini",
        name = "sentiment",
        description = "分析文本的情感倾向"
    )
    Sentiment analyzeSentiment(String text) {
        // 实现由 LangChain4j 自动处理
        return Sentiment.NEUTRAL;  // 占位符
    }

    /**
     * 情感类别
     */
    public enum Sentiment {
        POSITIVE,
        NEGATIVE,
        NEUTRAL
    }
}
```

### AI Services 集成

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.classifier.*;

/**
 * 分类服务
 */
public class ClassificationService {

    private final SentimentAnalyzer analyzer;

    public ClassificationService(String apiKey) {
        this.analyzer = AiServices.builder(SentimentAnalyzer.class)
                .chatModel(OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("gpt-4o-mini")
                        .build())
                .build();
    }

    /**
     * 情感分析接口
     */
    @Classifier
    public interface SentimentAnalyzer {
        Sentiment analyzeSentiment(String text);
    }

    /**
     * 情感类别
     */
    public enum Sentiment {
        POSITIVE("正面"),
        NEGATIVE("负面"),
        NEUTRAL("中性");

        private final String description;

        Sentiment(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 分析文本情感
     */
    public Sentiment analyze(String text) {
        return analyzer.analyzeSentiment(text);
    }

    public static void main(String[] args) {
        ClassificationService service = new ClassificationService(
            System.getenv("OPENAI_API_KEY")
        );

        // 测试情感分析
        String text1 = "今天天气真好！";
        String text2 = "这个产品太差了，我非常失望";
        String text3 = "这是一个关于历史的书籍";

        System.out.println("=== 情感分析 ===");
        System.out.println();

        System.out.println("文本: " + text1);
        System.out.println("情感: " + service.analyze(text1).getDescription());
        System.out.println();

        System.out.println("文本: " + text2);
        System.out.println("情感: " + service.analyze(text2).getDescription());
        System.out.println();

        System.out.println("文本: " + text3);
        System.out.println("情感: " + service.analyze(text3).getDescription());
    }
}
```

## 自定义分类器

### 内容审核分类器

```java
import dev.langchain4j.service.classifier.*;

/**
 * 内容审核分类器
 */
public class ContentModerationClassifier {

    /**
     * 内容审核接口
     */
    @Classifier
    public interface ContentModerator {
        /**
         * 检查内容是否安全
         * 
         * 返回值：SAFE（安全）, HARMFUL（有害）
         */
        ModerationResult moderateContent(String text);
    }

    /**
     * 审核结果
     */
    public static class ModerationResult {
        private final ModerationStatus status;
        private final String reason;

        public ModerationResult(ModerationStatus status, String reason) {
            this.status = status;
            this.reason = reason;
        }

        public ModerationStatus getStatus() {
            return status;
        }

        public String getReason() {
            return reason;
        }
    }

    /**
     * 审核状态
     */
    public enum ModerationStatus {
        SAFE("安全"),
        HARMFUL("有害"),
        UNCERTAIN("不确定");

        private final String description;

        ModerationStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 实现内容审核分类器
     */
    @Classifier(
        modelId = "gpt-4o-mini",
        name = "content-moderator",
        description = "检查内容是否包含有害信息"
    )
    public static class Moderator implements ContentModerator {
        public ModerationResult moderateContent(String text) {
            // 实现由 LangChain4j 自动处理
            // 这里返回占位符结果
            return new ModerationResult(
                ModerationStatus.SAFE,
                "内容看起来正常"
            );
        }
    }

    /**
     * 内容审核服务
     */
    public static class ContentModerationService {

        private final ContentModerator moderator;

        public ContentModerationService(String apiKey) {
            this.moderator = new Moderator();
        }

        /**
         * 检查内容
         */
        public ModerationResult checkContent(String text) {
            return moderator.moderateContent(text);
        }
    }

    public static void main(String[] args) {
        ContentModerationService service = new ContentModerationService(
            System.getenv("OPENAI_API_KEY")
        );

        // 测试内容审核
        String safeText = "这是一篇关于编程技术文章，介绍 Java 语言的使用方法。";
        String harmfulText = "这篇文章描述了如何制造危险物品...";

        System.out.println("=== 内容审核 ===");
        System.out.println();

        System.out.println("文本: " + safeText);
        ModerationResult result1 = service.checkContent(safeText);
        System.out.println("审核结果: " + result1.getStatus().getDescription());
        System.out.println("原因: " + result1.getReason());
        System.out.println();

        System.out.println("文本: " + harmfulText);
        ModerationResult result2 = service.checkContent(harmfulText);
        System.out.println("审核结果: " + result2.getStatus().getDescription());
        System.out.println("原因: " + result2.getReason());
    }
}
```

### 新闻分类器

```java
import dev.langchain4j.service.classifier.*;

/**
 * 新闻分类器
 */
public class NewsClassifier {

    /**
     * 新闻类别
     */
    public enum NewsCategory {
        POLITICS("政治"),
        BUSINESS("商业"),
        TECHNOLOGY("科技"),
        SPORTS("体育"),
        ENTERTAINMENT("娱乐"),
        HEALTH("健康"),
        SCIENCE("科学"),
        OTHER("其他");

        private final String description;

        NewsCategory(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 新闻分类接口
     */
    @Classifier
    public interface NewsClassifier {
        NewsCategory classifyNews(String title, String content);
    }

    /**
     * 实现新闻分类器
     */
    @Classifier(
        modelId = "gpt-4o-mini",
        name = "news-classifier",
        description = "根据新闻标题和内容分类新闻"
    )
    public static class NewsClassifierImpl implements NewsClassifier {
        public NewsCategory classifyNews(String title, String content) {
            // 实现由 LangChain4j 自动处理
            return NewsCategory.TECHNOLOGY;  // 占位符
        }
    }

    /**
     * 新闻分类服务
     */
    public static class NewsClassificationService {

        private final NewsClassifier classifier;

        public NewsClassificationService(String apiKey) {
            this.classifier = new NewsClassifierImpl();
        }

        /**
         * 分类新闻
         */
        public NewsCategory classify(String title, String content) {
            return classifier.classifyNews(title, content);
        }
    }

    public static void main(String[] args) {
        NewsClassificationService service = new NewsClassificationService(
            System.getenv("OPENAI_API_KEY")
        );

        // 测试新闻分类
        String news1 = "人工智能取得突破，新模型能够生成高质量代码";
        String news2 = "股市今日大涨，科技板块表现强劲";
        String news3 = "中国队在奥运会获得历史性突破";

        System.out.println("=== 新闻分类 ===");
        System.out.println();

        System.out.println("标题: " + news1);
        System.out.println("分类: " + service.classify(news1, "").getDescription());
        System.out.println();

        System.out.println("标题: " + news2);
        System.out.println("分类: " + service.classify(news2, "").getDescription());
        System.out.println();

        System.out.println("标题: " + news3);
        System.out.println("分类: " + service.classify(news3, "").getDescription());
    }
}
```

## 多标签分类

### 使用 List 作为返回类型

```java
import dev.langchain4j.service.classifier.*;

import java.util.List;

/**
 * 多标签分类器
 */
public class MultiLabelClassifier {

    /**
     * 主题标签
     */
    public enum TopicTag {
        JAVA("Java"),
        PYTHON("Python"),
        JAVASCRIPT("JavaScript"),
        GO("Go"),
        RUST("Rust"),
        SPRING("Spring"),
        SQL("SQL"),
        GIT("Git");

        private final String description;

        TopicTag(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 主题标签分类接口
     */
    @Classifier
    public interface TopicTagger {
        /**
         * 标记文本的主题标签
         * 
         * @return 标签列表
         */
        List<TopicTag> tagTopic(String text);
    }

    /**
     * 实现多标签分类器
     */
    @Classifier(
        modelId = "gpt-4o-mini",
        name = "topic-tagger",
        description = "为文本分配多个主题标签"
    )
    public static class TopicTaggerImpl implements TopicTagger {
        public List<TopicTag> tagTopic(String text) {
            // 实现由 LangChain4j 自动处理
            // 这里返回占位符结果
            return List.of(TopicTag.JAVA, TopicTag.SPRING);
        }
    }

    /**
     * 多标签分类服务
     */
    public static class MultiLabelService {

        private final TopicTagger tagger;

        public MultiLabelService(String apiKey) {
            this.tagger = new TopicTaggerImpl();
        }

        /**
         * 标记文本主题
         */
        public List<TopicTag> tag(String text) {
            return tagger.tagTopic(text);
        }
    }

    public static void main(String[] args) {
        MultiLabelService service = new MultiLabelService(
            System.getenv("OPENAI_API_KEY")
        );

        // 测试多标签分类
        String text = "本文介绍 Spring Boot 框架的核心概念和配置方法，包括自动配置、数据访问和依赖注入。";
        List<TopicTag> tags = service.tag(text);

        System.out.println("=== 多标签分类 ===");
        System.out.println("文本: " + text);
        System.out.println();
        System.out.println("分配的标签:");
        for (TopicTag tag : tags) {
            System.out.println("  - " + tag.getDescription());
        }
    }
}
```

## 零样本分类

### 使用示例提示词进行分类

```java
import dev.langchain4j.service.classifier.*;
import dev.langchain4j.service.classifier.Classification;
import java.util.List;

/**
 * 零样本分类示例
 */
public class ZeroShotClassificationExample {

    /**
     * 产品类别
     */
    public static class ProductCategory {
        private final String name;
        private final String description;
        private final List<String> exampleProducts;

        public ProductCategory(String name, String description, List<String> exampleProducts) {
            this.name = name;
            this.description = description;
            this.exampleProducts = exampleProducts;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public List<String> getExampleProducts() { return exampleProducts; }
    }

    /**
     * 产品类别定义
     */
    public static final List<ProductCategory> PRODUCT_CATEGORIES = List.of(
        new ProductCategory(
            "电子产品",
            "包括手机、电脑、平板等电子设备",
            List.of("iPhone", "MacBook Pro", "Samsung Galaxy")
        ),
        new ProductCategory(
            "服装配饰",
            "包括衣服、鞋子、包包等",
            List.of("T恤", "牛仔裤", "运动鞋", "手提包")
        ),
        new ProductCategory(
            "家居用品",
            "包括家具、家电、装饰品等",
            List.of("沙发", "电视", "冰箱", "台灯")
        ),
        new ProductCategory(
            "食品饮料",
            "包括各种食物和饮料",
            List.of("牛奶", "面包", "可乐", "巧克力")
        )
    );

    /**
     * 产品分类接口
     */
    @Classifier(
        modelId = "gpt-4o-mini",
        name = "product-classifier",
        description = "将产品分配到适当的类别"
    )
    public interface ProductClassifier {
        /**
         * 分类产品
         * 
         * @param productName 产品名称
         * @param categoryOptions 可选的类别列表，用于减少类别空间
         * @return 产品类别
         */
        ProductCategory classifyProduct(
            String productName,
            @Classification.ExampleProductExamples String categoryOptions
        );
    }

    /**
     * 实现零样本分类器
     */
    @Classifier(
        modelId = "gpt-4o-mini",
        name = "product-classifier",
        description = "将产品分配到适当的类别"
    )
    public static class ProductClassifierImpl implements ProductClassifier {
        public ProductCategory classifyProduct(String productName, String categoryOptions) {
            // 实现由 LangChain4j 自动处理
            // 这里返回占位符结果
            return PRODUCT_CATEGORIES.get(0);  // 占位符
        }
    }

    /**
     * 产品分类服务
     */
    public static class ProductClassificationService {

        private final ProductClassifier classifier;

        public ProductClassificationService(String apiKey) {
            this.classifier = new ProductClassifierImpl();
        }

        /**
         * 分类产品
         */
        public ProductCategory classify(String productName) {
            // 构建类别选项提示
            StringBuilder categoryOptions = new StringBuilder();
            for (int i = 0; i < PRODUCT_CATEGORIES.size(); i++) {
                ProductCategory category = PRODUCT_CATEGORIES.get(i);
                categoryOptions.append("- ").append(category.getName()).append("\n");
            }

            return classifier.classifyProduct(productName, categoryOptions.toString());
        }

        /**
         * 获取所有类别
         */
        public List<ProductCategory> getAllCategories() {
            return new ArrayList<>(PRODUCT_CATEGORIES);
        }

        /**
         * 显示类别和示例
         */
        public void displayCategories() {
            System.out.println("=== 可用产品类别 ===");
            for (ProductCategory category : PRODUCT_CATEGORIES) {
                System.out.println("类别: " + category.getName());
                System.out.println("描述: " + category.getDescription());
                System.out.println("示例产品: " + String.join(", ", category.getExampleProducts()));
                System.out.println();
            }
        }
    }

    public static void main(String[] args) {
        ProductClassificationService service = new ProductClassificationService(
            System.getenv("OPENAI_API_KEY")
        );

        // 显示可用类别
        service.displayCategories();

        // 测试产品分类
        String product1 = "iPhone 15 Pro";
        String product2 = "耐克运动裤";
        String product3 = "海尔冰箱";

        System.out.println("=== 产品分类 ===");
        System.out.println();

        System.out.println("产品: " + product1);
        System.out.println("类别: " + service.classify(product1).getName());
        System.out.println();

        System.out.println("产品: " + product2);
        System.out.println("类别: " + service.classify(product2).getName());
        System.out.println();

        System.out.println("产品: " + product3);
        System.out.println("类别: " + service.classify(product3).getName());
    }
}
```

## 层次分类

### 使用提示词实现层次结构

```java
import dev.langchain4j.service.classifier.*;
import dev.langchain4j.service.classifier.Classification;

import java.util.List;

/**
 * 层次分类示例
 */
public class HierarchicalClassifier {

    /**
     * 分类节点
     */
    public static class ClassificationNode {
        private final String id;
        private final String name;
        private final ClassificationNode parent;
        private final List<ClassificationNode> children;

        public ClassificationNode(String id, String name, ClassificationNode parent, List<ClassificationNode> children) {
            this.id = id;
            this.name = name;
            this.parent = parent;
            this.children = children != null ? children : new ArrayList<>();
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public ClassificationNode getParent() { return parent; }
        public List<ClassificationNode> getChildren() { return children; }
    }

    /**
     * 编程语言层次结构
     */
    public static final ClassificationNode PROGRAMMING_LANGUAGE_TREE = 
        new ClassificationNode(
            "programming",
            "编程语言",
            null,
            List.of(
                new ClassificationNode("dynamic", "动态语言", 
                    "programming", List.of(
                        new ClassificationNode("java", "Java", null, null),
                        new ClassificationNode("python", "Python", null, null),
                        new ClassificationNode("javascript", "JavaScript", null, null)
                    )
                ),
                new ClassificationNode("static", "静态语言", 
                    "programming", List.of(
                        new ClassificationNode("c", "C", null, null),
                        new ClassificationNode("cpp", "C++", null, null),
                        new ClassificationNode("rust", "Rust", null, null)
                    )
                )
            )
        );

    /**
     * 层次分类接口
     */
    @Classifier(
        modelId = "gpt-4o-mini",
        name = "hierarchical-classifier",
        description = "将文本分配到适当的分类节点"
    )
    public interface HierarchicalClassifier {
        /**
         * 层次分类
         * 
         * @param text 要分类的文本
         * @param nodePath 分类路径，例如 "programming/dynamic/java"
         * @return 分类节点路径
         */
        String classify(String text, @Classification.ClassificationNodePath String nodePath);
    }

    /**
     * 层次分类服务
     */
    public static class HierarchicalClassificationService {

        private final HierarchicalClassifier classifier;

        public HierarchicalClassificationService(String apiKey) {
            // 假设我们有一个分类器实现
            // this.classifier = new HierarchicalClassifierImpl();
        }

        /**
         * 层次分类
         */
        public String classify(String text, String nodePath) {
            return classifier.classify(text, nodePath);
        }

        /**
         * 显示分类树
         */
        public void displayTree() {
            System.out.println("=== 分类树 ===");
            displayNode(PROGRAMMING_LANGUAGE_TREE, 0);
        }

        private void displayNode(ClassificationNode node, int indent) {
            String indentStr = "  ".repeat(indent);
            System.out.println(indentStr + node.getName());

            for (ClassificationNode child : node.getChildren()) {
                displayNode(child, indent + 1);
            }
        }

        public static void main(String[] args) {
            HierarchicalClassificationService service = new HierarchicalClassificationService(
                System.getenv("OPENAI_API_KEY")
            );

            // 显示分类树
            service.displayTree();

            // 测试层次分类
            String text1 = "Java 是一种面向对象的编程语言";
            String text2 = "C 语言以其高性能著称";
            String text3 = "Python 是一种流行的脚本语言";

            System.out.println("=== 层次分类 ===");
            System.out.println();

            System.out.println("文本: " + text1);
            String result1 = service.classify(text1, "programming/dynamic");
            System.out.println("分类路径: " + result1);
            System.out.println();

            System.out.println("文本: " + text2);
            String result2 = service.classify(text2, "programming/static");
            System.out.println("分类路径: " + result2);
            System.out.println();

            System.out.println("文本: " + text3);
            String result3 = service.classify(text3, "programming/dynamic");
            System.out.println("分类路径: " + result3);
        }
    }
}
```

## 批量分类

### 使用 List 输入

```java
import dev.langchain4j.service.classifier.*;

import java.util.List;

/**
 * 批量分类示例
 */
public class BatchClassificationExample {

    /**
     * 批量分类接口
     */
    @Classifier
    public interface BatchClassifier {
        /**
         * 批量分类文本列表
         * 
         * @param texts 文本列表
         * @return 分类结果列表
         */
        List<ClassificationResult> classifyAll(List<String> texts);
    }

    /**
     * 分类结果
     */
    public static class ClassificationResult {
        private final String text;
        private final String category;

        public ClassificationResult(String text, String category) {
            this.text = text;
            this.category = category;
        }

        public String getText() { return text; }
        public String getCategory() { return category; }
    }

    /**
     * 情感分类结果
     */
    public static class SentimentResult extends ClassificationResult {
        private final String sentiment;
        private final double confidence;

        public SentimentResult(String text, String category, String sentiment, double confidence) {
            super(text, category);
            this.sentiment = sentiment;
            this.confidence = confidence;
        }

        public String getSentiment() { return sentiment; }
        public double getConfidence() { return confidence; }
    }

    /**
     * 批量分类服务
     */
    public static class BatchClassificationService {

        private final BatchClassifier classifier;

        public BatchClassificationService(String apiKey) {
            // 假设我们有一个分类器实现
            // this.classifier = new BatchClassifierImpl();
        }

        /**
         * 批量分类
         */
        public List<SentimentResult> batchSentimentAnalyze(List<String> texts) {
            List<SentimentResult> results = new ArrayList<>();

            for (String text : texts) {
                // 执行分类（这里模拟）
                String category = "neutral";
                String sentiment = "neutral";
                double confidence = 0.8 + Math.random() * 0.2;

                results.add(new SentimentResult(text, category, sentiment, confidence));
            }

            return results;
        }

        /**
         * 显示批量分类结果
         */
        public void displayResults(List<SentimentResult> results) {
            System.out.println("=== 批量分类结果 ===");
            System.out.println();

            for (int i = 0; i < results.size(); i++) {
                SentimentResult result = results.get(i);
                System.out.printf("%d. 文本: %s\n", i + 1, result.getText());
                System.out.printf("   分类: %s\n", result.getCategory());
                System.out.printf("   情感: %s\n", result.getSentiment());
                System.out.printf("   置信度: %.2f\n", result.getConfidence());
                System.out.println();
            }
        }
    }

    public static void main(String[] args) {
        BatchClassificationService service = new BatchClassificationService(
            System.getenv("OPENAI_API_KEY")
        );

        // 测试文本
        List<String> texts = List.of(
            "今天天气真好！",
            "这个产品太差了",
            "这是一个关于历史的书籍"
        );

        // 批量分类
        List<SentimentResult> results = service.batchSentimentAnalyze(texts);

        // 显示结果
        service.displayResults(results);
    }
}
```

## 测试代码示例

```java
import dev.langchain4j.service.classifier.*;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 分类器测试
 */
class ClassificationTest {

    private static class MockSentimentAnalyzer {
        public String analyze(String text) {
            if (text.contains("好") || text.contains("棒") || text.contains("优秀")) {
                return "POSITIVE";
            } else if (text.contains("差") || text.contains("坏") || text.contains("失望")) {
                return "NEGATIVE";
            } else {
                return "NEUTRAL";
            }
        }
    }

    @Test
    void should_classify_positive_sentiment() {
        MockSentimentAnalyzer analyzer = new MockSentimentAnalyzer();

        String text = "这部电影太棒了，强烈推荐！";
        String result = analyzer.analyze(text);

        assertEquals("POSITIVE", result);
    }

    @Test
    void should_classify_negative_sentiment() {
        MockSentimentAnalyzer analyzer = new MockSentimentAnalyzer();

        String text = "服务质量太差，非常失望";
        String result = analyzer.analyze(text);

        assertEquals("NEGATIVE", result);
    }

    @Test
    void should_classify_neutral_sentiment() {
        MockSentimentAnalyzer analyzer = new MockSentimentAnalyzer();

        String text = "这是一本关于历史的书";
        String result = analyzer.analyze(text);

        assertEquals("NEUTRAL", result);
    }

    @Test
    void should_classify_news() {
        MockNewsClassifier classifier = new MockNewsClassifier();

        String title = "人工智能取得重大突破";
        String result = classifier.classify(title, "");

        // 验证结果
        assertNotNull(result);
        assertTrue(result.equals("TECHNOLOGY") || result.equals("SCIENCE"));
    }

    @Test
    void should_tag_multiple_topics() {
        MockTopicTagger tagger = new MockTopicTagger();

        String text = "Spring Boot 框架用于快速应用开发";
        List<String> tags = tagger.tag(text);

        assertNotNull(tags);
        assertFalse(tags.isEmpty());
        assertTrue(tags.contains("SPRING"));
        assertTrue(tags.contains("JAVA"));
    }

    /**
     * 模拟新闻分类器
     */
    private static class MockNewsClassifier {
        public String classify(String title, String content) {
            if (title.contains("AI") || title.contains("科技") || title.contains("计算机")) {
                return "TECHNOLOGY";
            } else if (title.contains("股市") || title.contains("经济") || title.contains("金融")) {
                return "BUSINESS";
            } else {
                return "OTHER";
            }
        }
    }

    /**
     * 模拟主题标记器
     */
    private static class MockTopicTagger {
        public List<String> tag(String text) {
            List<String> tags = new ArrayList<>();

            if (text.contains("Java")) {
                tags.add("JAVA");
            }
            if (text.contains("Python")) {
                tags.add("PYTHON");
            }
            if (text.contains("Spring")) {
                tags.add("SPRING");
            }
            if (text.contains("SQL")) {
                tags.add("SQL");
            }
            if (text.contains("Git")) {
                tags.add("GIT");
            }
            if (text.contains("JavaScript")) {
                tags.add("JAVASCRIPT");
            }

            return tags;
        }
    }
}
```

## 实践练习

### 练习 1：实现垃圾邮件检测器

```java
import dev.langchain4j.service.classifier.*;

/**
 * 垃圾邮件检测器
 */
public class SpamDetector {

    /**
     * 垃圾邮件类别
     */
    public enum SpamCategory {
        HAM("正常邮件"),
        SPAM("垃圾邮件");

        private final String description;

        SpamCategory(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 垃圾邮件检测接口
     */
    @Classifier(
        modelId = "gpt-4o-mini",
        name = "spam-detector",
        description = "检测邮件是否为垃圾邮件"
    )
    public interface SpamDetector {
        /**
         * 检测邮件是否为垃圾邮件
         * 
         * @param subject 邮件主题
         * @param body 邮件正文
         * @return 垃圾邮件类别
         */
        SpamCategory detectSpam(String subject, String body);
    }

    /**
     * 垃圾邮件检测服务
     */
    public static class SpamDetectionService {

        private final SpamDetector detector;

        public SpamDetectionService(String apiKey) {
            // 假设我们有一个检测器实现
            // this.detector = new SpamDetectorImpl();
        }

        /**
         * 检测邮件
         */
        public SpamCategory detect(String subject, String body) {
            return detector.detectSpam(subject, body);
        }

        /**
         * 分析邮件内容
         */
        public void analyzeEmail(String subject, String body) {
            SpamCategory category = detect(subject, body);

            System.out.println("=== 垃圾邮件检测 ===");
            System.out.println();
            System.out.println("邮件主题: " + subject);
            System.out.println("邮件正文: " + (body.length() > 100 ? body.substring(0, 100) + "..." : body));
            System.out.println();
            System.out.println("检测结果: " + category.getDescription());
        }
    }

    public static void main(String[] args) {
        SpamDetectionService service = new SpamDetectionService(
            System.getenv("OPENAI_API_KEY")
        );

        // 测试邮件
        String hamEmail = "关于下周会议安排的通知";
        String hamBody = "大家好，下周二下午 3 点在 A 会议室举行项目评审会议，请准时参加。";

        String spamEmail = "恭喜您中奖了！点击链接领取奖金！";
        String spamBody = "尊敬的用户，我们很高兴地通知您，您已被选中获得 100 万美元奖金！请在 24 小时内点击链接领取您的奖金。";

        service.analyzeEmail(hamEmail, hamBody);
        System.out.println();

        service.analyzeEmail(spamEmail, spamBody);
    }
}
```

### 练习 2：实现情感分析工具

```java
import dev.langchain4j.service.classifier.*;
import dev.langchain4j.service.classifier.Classification;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolParam;

import java.util.List;

/**
 * 情感分析工具
 */
public class SentimentAnalysisTool {

    /**
     * 批量情感分析
     */
    @Tool("批量分析多个文本的情感倾向")
    public String analyzeSentiments(@ToolParam("文本列表") List<String> texts) {
        StringBuilder results = new StringBuilder();
        
        for (String text : texts) {
            String sentiment = analyzeSingle(text);
            results.append(String.format("文本: %s -> 情感: %s\n", 
                text.length() > 50 ? text.substring(0, 50) + "..." : text,
                sentiment));
        }
        
        return results.toString();
    }

    /**
     * 详细情感分析
     */
    @Tool("对单个文本进行详细的情感分析")
    public String analyzeSentimentDetailed(@ToolParam("文本") String text) {
        return String.format(
            "情感分析结果：\n" +
            "文本: %s\n" +
            "情感: %s\n" +
            "置信度: %.2f\n" +
            "情感强度: %s\n" +
            "关键情感词: %s",
            text,
            analyzeSingle(text),
            0.85,
            getSentimentStrength(text)
        );
    }

    /**
     * 获取情感统计
     */
    @Tool("统计多个文本的情感分布")
    public String getSentimentStatistics(@ToolParam("文本列表") List<String> texts) {
        int positive = 0;
        int negative = 0;
        int neutral = 0;

        for (String text : texts) {
            String sentiment = analyzeSingle(text);
            switch (sentiment) {
                case "POSITIVE" -> positive++;
                case "NEGATIVE" -> negative++;
                case "NEUTRAL" -> neutral++;
            }
        }

        int total = texts.size();

        return String.format(
            "情感统计：\n" +
            "总计: %d\n" +
            "正面: %d (%.1f%%)\n" +
            "负面: %d (%.1f%%)\n" +
            "中性: %d (%.1f%%)",
            total,
            positive,
            negative,
            neutral,
            (double) positive / total * 100,
            (double) negative / total * 100,
            (double) neutral / total * 100
        );
    }

    /**
     * 分析单个文本的情感
     */
    private String analyzeSingle(String text) {
        // 简化实现
        if (text.contains("好") || text.contains("棒") || text.contains("优秀") || 
            text.contains("喜欢") || text.contains("爱")) {
            return "POSITIVE";
        } else if (text.contains("差") || text.contains("坏") || text.contains("恨") || 
                   text.contains("失望") || text.contains("糟糕")) {
            return "NEGATIVE";
        } else {
            return "NEUTRAL";
        }
    }

    /**
     * 获取情感强度
     */
    private String getSentimentStrength(String text) {
        String sentiment = analyzeSingle(text);
        int strength = 0;

        // 统计情感词
        if ("POSITIVE".equals(sentiment)) {
            if (text.contains("很") || text.contains("非常") || text.contains("特别")) {
                strength += 2;
            }
            if (text.contains("好") || text.contains("棒") || text.contains("优秀")) {
                strength++;
            }
            if (text.contains("喜欢") || text.contains("爱")) {
                strength++;
            }
        } else if ("NEGATIVE".equals(sentiment)) {
            if (text.contains("很") || text.contains("非常") || text.contains("特别")) {
                strength += 2;
            }
            if (text.contains("差") || text.contains("坏") || text.contains("糟糕")) {
                strength++;
            }
            if (text.contains("恨") || text.contains("讨厌")) {
                strength++;
            }
        }

        return switch (strength) {
            case 0 -> "无情感";
            case 1 -> "轻微";
            case 2 -> "中度";
            case 3 -> "强烈";
            default -> "非常强烈";
        };
    }
}
```

### 练习 3：实现新闻分类系统

```java
import dev.langchain4j.service.classifier.*;
import java.util.*;

/**
 * 新闻分类系统
 */
public class NewsClassificationSystem {

    /**
     * 新闻类别
     */
    public enum NewsCategory {
        POLITICS("政治"),
        BUSINESS("商业"),
        TECHNOLOGY("科技"),
        SPORTS("体育"),
        ENTERTAINMENT("娱乐"),
        HEALTH("健康"),
        SCIENCE("科学"),
        WORLD("国际"),
        OPINION("观点"),
        OTHER("其他");

        private final String description;

        NewsCategory(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 新闻文章
     */
    public static class NewsArticle {
        private final String id;
        private final String title;
        private final String content;
        private final String date;
        private final String author;

        public NewsArticle(String id, String title, String content, String date, String author) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.date = date;
            this.author = author;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
        public String getDate() { return date; }
        public String getAuthor() { return author; }
    }

    /**
     * 分类结果
     */
    public static class ClassificationResult {
        private final String articleId;
        private final NewsCategory category;
        private final double confidence;

        public ClassificationResult(String articleId, NewsCategory category, double confidence) {
            this.articleId = articleId;
            this.category = category;
            this.confidence = confidence;
        }

        public String getArticleId() { return articleId; }
        public NewsCategory getCategory() { return category; }
        public double getConfidence() { return confidence; }
    }

    /**
     * 新闻分类系统
     */
    public static class NewsClassificationSystem {

        private final Map<String, NewsArticle> articles;
        private final Map<String, NewsCategory> articleCategories;

        public NewsClassificationSystem() {
            this.articles = new HashMap<>();
            this.articleCategories = new HashMap<>();
        }

        /**
         * 添加新闻文章
         */
        public void addArticle(NewsArticle article) {
            articles.put(article.getId(), article);
        }

        /**
         * 批量分类文章
         */
        public List<ClassificationResult> classifyAll() {
            List<ClassificationResult> results = new ArrayList<>();

            for (NewsArticle article : articles.values()) {
                // 模拟分类（实际应用中应该使用分类器）
                NewsCategory category = classifyArticle(article);
                double confidence = 0.7 + Math.random() * 0.3;

                articleCategories.put(article.getId(), category);
                results.add(new ClassificationResult(article.getId(), category, confidence));
            }

            return results;
        }

        /**
         * 分类单个文章
         */
        private NewsCategory classifyArticle(NewsArticle article) {
            String title = article.getTitle();
            String content = article.getContent();

            if (title.contains("科技") || title.contains("AI") || 
                title.contains("计算机") || title.contains("软件")) {
                return NewsCategory.TECHNOLOGY;
            } else if (title.contains("股市") || title.contains("金融") || 
                       title.contains("银行") || title.contains("经济")) {
                return NewsCategory.BUSINESS;
            } else if (title.contains("体育") || title.contains("比赛") || 
                       title.contains("奥运") || title.contains("世界杯")) {
                return NewsCategory.SPORTS;
            } else if (title.contains("电影") || title.contains("明星") || 
                       title.contains("娱乐") || title.contains("音乐")) {
                return NewsCategory.ENTERTAINMENT;
            } else if (title.contains("政治") || title.contains("政府") || 
                       title.contains("政策") || title.contains("选举")) {
                return NewsCategory.POLITICS;
            } else if (title.contains("健康") || title.contains("医疗") || 
                       title.contains("药品") || title.contains("疫苗")) {
                return NewsCategory.HEALTH;
            } else if (title.contains("科学") || title.contains("研究") || 
                       title.contains("发现") || title.contains("实验")) {
                return NewsCategory.SCIENCE;
            } else if (title.contains("国际") || title.contains("全球") || 
                       title.contains("世界") || title.contains("海外")) {
                return NewsCategory.WORLD;
            } else if (title.contains("观点") || title.contains("评论") || 
                       title.contains("分析") || title.contains("看法")) {
                return NewsCategory.OPINION;
            } else {
                return NewsCategory.OTHER;
            }
        }

        /**
         * 显示分类统计
         */
        public void displayStatistics(List<ClassificationResult> results) {
            Map<NewsCategory, Integer> counts = new HashMap<>();

            for (ClassificationResult result : results) {
                counts.put(result.getCategory(), counts.getOrDefault(result.getCategory(), 0) + 1);
            }

            System.out.println("=== 新闻分类统计 ===");
            System.out.println();

            for (NewsCategory category : counts.keySet()) {
                System.out.printf("%s: %d (%.1f%%)\n",
                        category.getDescription(),
                        (double) counts.get(category) / results.size() * 100);
            }
        }

        /**
         * 按类别分组
         */
        public Map<NewsCategory, List<String>> groupByCategory() {
            Map<NewsCategory, List<String>> groups = new HashMap<>();

            for (Map.Entry<String, NewsCategory> entry : articleCategories.entrySet()) {
                NewsCategory category = entry.getValue();
                groups.computeIfAbsent(category, k -> new ArrayList<>()).add(entry.getKey());
            }

            return groups;
        }
    }

    public static void main(String[] args) {
        NewsClassificationSystem system = new NewsClassificationSystem();

        // 添加新闻文章
        system.addArticle(new NewsArticle(
            "news1",
            "人工智能取得重大突破",
            "研究人员在深度学习领域取得突破，新模型在各种基准测试中刷新纪录。这项突破将推动 AI 技术的发展...",
            "2024-01-15",
            "张三"
        ));

        system.addArticle(new NewsArticle(
            "news2",
            "股市今日大涨",
            "受利好消息刺激，今日股市大幅上涨。科技板块表现强劲，多只股票涨停...",
            "2024-01-15",
            "李四"
        ));

        system.addArticle(new NewsArticle(
            "news3",
            "奥运会首枚金牌产生",
            "在本届奥运会首日比赛中，中国代表团夺得首枚金牌。体操选手在团体项目中表现出色...",
            "2024-01-15",
            "王五"
        ));

        system.addArticle(new Article(
            "news4",
            "政府发布新政策",
            "政府今日发布了一系列促进经济发展的重要政策，包括减税降费、扩大内需等措施...",
            "2024-01-15",
            "赵六"
        ));

        // 批量分类
        List<ClassificationResult> results = system.classifyAll();

        // 显示统计
        system.displayStatistics(results);

        // 按类别分组
        Map<NewsCategory, List<String>> groups = system.groupByCategory();

        System.out.println();
        System.out.println("=== 新闻分组 ===");
        System.out.println();
        for (Map.Entry<NewsCategory, List<String>> entry : groups.entrySet()) {
            System.out.println(entry.getKey().getDescription() + ":");
            for (String articleId : entry.getValue()) {
                System.out.println("  - " + articleId);
            }
            System.out.println();
        }
    }
}
```

## 总结

### 本章要点

1. **分类的概念**
   - 将文本分配到预定义的类别
   - 应用场景：情感分析、内容审核、新闻分类等

2. **@Classifier 注解**
   - 简化分类器的创建和使用
   - 支持多种分类类型
   - 可配置参数和提示词

3. **分类类型**
   - 二分类
   - 多分类
   - 多标签分类
   - 层次分类
   - 零样本分类

4. **应用场景**
   - 情感分析
   - 内容审核
   - 垃圾邮件检测
   - 新闻分类
   - 产品分类

5. **最佳实践**
   - 提供清晰的类别定义和描述
   - 在提示词中包含示例
   - 考虑边缘情况和不确定性
   - 收集数据评估和改进分类器

### 下一步

在下一章节中，我们将学习：
- LangChain4j 模型集成
- 自定义模型调用
- 异步操作和并发处理
- 错误处理和重试机制
- 性能优化技巧

### 常见问题

**Q1：@Classifier 和普通 AI 服务有什么区别？**

A：
- **@Classifier**：专门用于分类任务的注解，返回分类结果
- **AI 服务**：通用对话接口，可以执行各种任务
- 选择：如果是分类任务，使用 @Classifier；如果是通用对话，使用 AI Services

**Q2：如何提高分类准确率？**

A：
1. 提供高质量的训练数据（对于微调）
2. 在提示词中包含更多示例
3. 使用更大的模型（如果成本允许）
4. 添加明确的类别描述和边界
5. 收集和分析错误案例

**Q3：多标签分类和单标签分类有什么区别？**

A：
- **单标签分类**：每个文本只分配到一个类别
- **多标签分类**：每个文本可以属于多个类别
- 选择：如果一个文本可能属于多个类别，使用多标签分类

**Q4：零样本分类需要训练数据吗？**

A：
- **零样本分类**：不需要训练数据，依靠模型的知识和提示词
- **有监督分类**：需要标记的训练数据
- 选择：如果训练数据有限，使用零样本分类；如果有大量数据，使用有监督分类

**Q5：如何评估分类器的性能？**

A：评估指标：
- 准确率：正确分类的样本比例
- 精确率、召回率、F1 分数
- 混淆矩阵：显示各类别之间的混淆情况
- AUC-ROC：评估模型的整体性能
- 类别分布：检查各类别的平衡性

## 参考资料

- [LangChain4j 分类文档](https://docs.langchain4j.dev/tutorials/classification)
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
