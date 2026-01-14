---
title: '结构化输出'
description: '学习 LangChain4j 的 结构化输出 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 19 - 结构化输出

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 理解 LangChain4j 中结构化输出的概念和用途
- 掌握 `@StructuredPrompt` 和 `@EnumPrompt` 注解的使用
- 学会将 LLM 的响应自动转换为强类型对象
- 理解 JSON Schema 和响应格式验证
- 掌握复杂对象和枚举类型的输出
- 实现一个完整的结构化输出应用

## 前置知识

- 完成《01 - LangChain4j 简介》章节
- 完成《02 - 你的第一个 Chat 应用》章节
- 完成《14 - JSON 处理》章节（推荐）

## 核心概念

### 什么是结构化输出？

**结构化输出**是指将 LLM 的响应自动转换为强类型的 Java 对象，而不是简单的文本字符串。

**类比理解：**
- **普通输出** = 手动解析文本字符串，容易出错
- **结构化输出** = 自动映射到预定义的类结构，类型安全

**为什么需要结构化输出？**

1. **类型安全** - 编译时检查，减少运行时错误
2. **代码可读性** - 清晰的数据模型，易于理解和维护
3. **自动验证** - 根据类的定义验证响应格式
4. **简化代码** - 无需手动解析 JSON 字符串
5. **集成友好** - 直接使用 Java 对象，无需额外转换

### 结构化输出类型

| 类型 | 说明 | 示例 |
|------|------|------|
| **基本类型** | String, Integer, Boolean 等 | "Hello", 25, true |
| **复杂对象** | 嵌套的 POJO 类 | User {name, age, email} |
| **枚举类型** | 预定义的枚举值 | Sentiment.POSITIVE |
| **集合类型** | List, Map, Set | `List<String> tags` |
| **Optional** | 可能为空的字段 | `Optional<String> nickname` |

## 基本结构化输出

### 基础 POJO 输出

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * 基础结构化输出示例
 */
public class BasicStructuredOutput {

    /**
     * 简单的输出类
     */
    public static class SimpleResponse {
        private final String message;
        private final int number;
        private final boolean confirmed;

        public SimpleResponse(String message, int number, boolean confirmed) {
            this.message = message;
            this.number = number;
            this.confirmed = confirmed;
        }

        public String getMessage() { return message; }
        public int getNumber() { return number; }
        public boolean isConfirmed() { return confirmed; }

        @Override
        public String toString() {
            return String.format("SimpleResponse{message='%s', number=%d, confirmed=%b}",
                    message, number, confirmed);
        }
    }

    /**
     * 简单的 AI 服务
     */
    @AiService
    public interface SimpleService {
        SimpleResponse generateSimpleResponse(@UserMessage String prompt);
    }

    /**
     * 创建服务
     */
    public static SimpleService createSimpleService(String apiKey) {
        return AiServices.builder(SimpleService.class)
                .chatModel(OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("gpt-4o-mini")
                        .build())
                .build();
    }

    public static void main(String[] args) {
        SimpleService service = createSimpleService(
            System.getenv("OPENAI_API_KEY")
        );

        // 生成结构化响应
        SimpleResponse response = service.generateSimpleResponse(
            "生成一个简单响应，包含消息、数字和确认标志"
        );

        System.out.println("消息: " + response.getMessage());
        System.out.println("数字: " + response.getNumber());
        System.out.println("确认: " + response.isConfirmed());
    }
}
```

## @StructuredPrompt 注解

### 复杂对象输出

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * @StructuredPrompt 注解示例
 */
public class StructuredPromptExample {

    /**
     * 用户信息 POJO
     */
    public static class UserInfo {
        private final String name;
        private final int age;
        private final String email;
        private final String occupation;

        public UserInfo(String name, int age, String email, String occupation) {
            this.name = name;
            this.age = age;
            this.email = email;
            this.occupation = occupation;
        }

        // Getters
        public String getName() { return name; }
        public int getAge() { return age; }
        public String getEmail() { return email; }
        public String getOccupation() { return occupation; }

        @Override
        public String toString() {
            return String.format("UserInfo{name='%s', age=%d, email='%s', occupation='%s'}",
                    name, age, email, occupation);
        }
    }

    /**
     * AI 服务
     */
    @AiService
    public interface UserService {
        /**
         * 从文本提取用户信息
         */
        UserInfo extractUserInfo(@UserMessage String text);
    }

    /**
     * 创建服务
     */
    public static UserService createUserService(String apiKey) {
        return AiServices.builder(UserService.class)
                .chatModel(OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("gpt-4o-mini")
                        .build())
                .systemMessageProvider(chatMemoryId -> 
                    "你是一个信息提取专家。" +
                    "从给定的文本中提取用户信息，" +
                    "包括姓名、年龄、邮箱和职业。" +
                    "如果某些信息在文本中没有，使用合理的默认值。"
                )
                .build();
    }

    public static void main(String[] args) {
        UserService service = createUserService(
            System.getenv("OPENAI_API_KEY")
        );

        String text = "我叫张三，今年 25 岁，" +
                     "我的邮箱是 zhangsan@example.com，" +
                     "我是一名软件工程师";

        // 提取用户信息
        UserInfo user = service.extractUserInfo(text);

        System.out.println("姓名: " + user.getName());
        System.out.println("年龄: " + user.getAge());
        System.out.println("邮箱: " + user.getEmail());
        System.out.println("职业: " + user.getOccupation());
    }
}
```

## 枚举类型输出

### @EnumPrompt 注解

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 枚举类型输出示例
 */
public class EnumPromptExample {

    /**
     * 情感枚举
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
     * 分类枚举
     */
    public enum Category {
        TECH("技术"),
        BUSINESS("商业"),
        SPORTS("体育"),
        ENTERTAINMENT("娱乐"),
        SCIENCE("科学"),
        OTHER("其他");

        private final String description;

        Category(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 分类结果
     */
    public static class ClassificationResult {
        private final Sentiment sentiment;
        private final Category category;
        private final double confidence;

        public ClassificationResult(Sentiment sentiment, Category category, double confidence) {
            this.sentiment = sentiment;
            this.category = category;
            this.confidence = confidence;
        }

        public Sentiment getSentiment() { return sentiment; }
        public Category getCategory() { return category; }
        public double getConfidence() { return confidence; }

        @Override
        public String toString() {
            return String.format("ClassificationResult{sentiment=%s, category=%s, confidence=%.2f}",
                    sentiment, category, confidence);
        }
    }

    /**
     * 分类服务
     */
    @AiService
    public interface ClassificationService {
        /**
         * 分析文本的情感和分类
         */
        ClassificationResult analyzeText(
            @UserMessage String text
        );
    }

    /**
     * 创建分类服务
     */
    public static ClassificationService createClassificationService(String apiKey) {
        return AiServices.builder(ClassificationService.class)
                .chatModel(OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("gpt-4o-mini")
                        .build())
                .systemMessageProvider(chatMemoryId -> 
                    "你是一个文本分析专家。" +
                    "分析给定文本的情感倾向（正面、负面、中性）" +
                    "和内容分类（技术、商业、体育、娱乐、科学、其他）。" +
                    "同时给出你对分析的置信度（0.0 到 1.0）。"
                )
                .build();
    }

    public static void main(String[] args) {
        ClassificationService service = createClassificationService(
            System.getenv("OPENAI_API_KEY")
        );

        String text = "LangChain4j 是一个功能强大的 Java 框架，" +
                     "它让集成 LLM 变得非常简单和高效，" +
                     "我强烈推荐给所有的 Java 开发者。";

        // 分析文本
        ClassificationResult result = service.analyzeText(text);

        System.out.println("情感: " + result.getSentiment().getDescription());
        System.out.println("分类: " + result.getCategory().getDescription());
        System.out.println("置信度: " + result.getConfidence());
    }
}
```

## 复杂嵌套结构

### 嵌套对象输出

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import java.util.List;
import java.util.Optional;

/**
 * 复杂嵌套结构示例
 */
public class NestedStructureExample {

    /**
     * 地址 POJO
     */
    public static class Address {
        private final String street;
        private final String city;
        private final String country;
        private final String postalCode;

        public Address(String street, String city, String country, String postalCode) {
            this.street = street;
            this.city = city;
            this.country = country;
            this.postalCode = postalCode;
        }

        public String getStreet() { return street; }
        public String getCity() { return city; }
        public String getCountry() { return country; }
        public String getPostalCode() { return postalCode; }
    }

    /**
     * 公司信息 POJO
     */
    public static class CompanyInfo {
        private final String name;
        private final String industry;
        private final int employeeCount;
        private final Optional<String> website;
        private final Address address;

        public CompanyInfo(String name, String industry, int employeeCount, String website, Address address) {
            this.name = name;
            this.industry = industry;
            this.employeeCount = employeeCount;
            this.website = Optional.ofNullable(website);
            this.address = address;
        }

        public String getName() { return name; }
        public String getIndustry() { return industry; }
        public int getEmployeeCount() { return employeeCount; }
        public Optional<String> getWebsite() { return website; }
        public Address getAddress() { return address; }
    }

    /**
     * 联系信息 POJO
     */
    public static class ContactInfo {
        private final String firstName;
        private final String lastName;
        private final String phone;
        private final Optional<String> mobile;
        private final String email;

        public ContactInfo(String firstName, String lastName, String phone, String mobile, String email) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.phone = phone;
            this.mobile = Optional.ofNullable(mobile);
            this.email = email;
        }

        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getPhone() { return phone; }
        public Optional<String> getMobile() { return mobile; }
        public String getEmail() { return email; }
    }

    /**
     * 完整的用户资料 POJO
     */
    public static class User {
        private final ContactInfo contact;
        private final CompanyInfo company;
        private final List<String> skills;
        private final String bio;

        public User(ContactInfo contact, CompanyInfo company, List<String> skills, String bio) {
            this.contact = contact;
            this.company = company;
            this.skills = skills;
            this.bio = bio;
        }

        public ContactInfo getContact() { return contact; }
        public CompanyInfo getCompany() { return company; }
        public List<String> getSkills() { return skills; }
        public String getBio() { return bio; }
    }

    /**
     * 用户资料服务
     */
    @AiService
    public interface UserProfileService {
        /**
         * 从文本提取用户资料信息
         */
        User extractUserProfile(@UserMessage String text);
    }

    /**
     * 创建用户资料服务
     */
    public static UserProfileService createUserProfileService(String apiKey) {
        return AiServices.builder(UserProfileService.class)
                .chatModel(OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("gpt-4o-mini")
                        .build())
                .systemMessageProvider(chatMemoryId -> 
                    "你是一个信息提取专家。" +
                    "从给定的文本中提取完整的用户资料信息，" +
                    "包括联系信息、公司信息、技能列表和简短的个人简介。" +
                    "返回格式应该使用强类型对象结构。" +
                    "联系信息包括：姓名、电话、手机（可选）、邮箱。" +
                    "公司信息包括：公司名称、行业、员工人数、网站（可选）、地址。" +
                    "地址包括：街道、城市、国家、邮编。" +
                    "技能列表应该包含所有提到的技能。" +
                    "如果某些信息在文本中没有提及，使用合理的默认值或 Optional 表示。"
                )
                .build();
    }

    public static void main(String[] args) {
        UserProfileService service = createUserProfileService(
            System.getenv("OPENAI_API_KEY")
        );

        String text = "我叫李四，电话是 138-0000-0000，邮箱是 lisi@example.com。" +
                     "我在创新科技公司工作，这是一家有 500 员工的软件开发公司。" +
                     "公司地址是北京市朝阳区科技园区 1 号，邮编 100000。" +
                     "我掌握 Java、Python、Spring Boot、MySQL 等技术栈。" +
                     "我热爱编程，喜欢学习新技术和解决复杂的技术难题。";

        // 提取用户资料
        User profile = service.extractUserProfile(text);

        // 显示联系信息
        System.out.println("=== 联系信息 ===");
        System.out.println("姓名: " + profile.getContact().getFirstName() + 
                           " " + profile.getContact().getLastName());
        System.out.println("电话: " + profile.getContact().getPhone());
        System.out.println("手机: " + profile.getContact().getMobile().orElse("未提供"));
        System.out.println("邮箱: " + profile.getContact().getEmail());
        System.out.println();

        // 显示公司信息
        System.out.println("=== 公司信息 ===");
        System.out.println("公司名称: " + profile.getCompany().getName());
        System.out.println("行业: " + profile.getCompany().getIndustry());
        System.out.println("员工人数: " + profile.getCompany().getEmployeeCount());
        System.out.println("网站: " + profile.getCompany().getWebsite().orElse("未提供"));
        System.out.println("地址: " + profile.getCompany().getAddress().getStreet());
        System.out.println("      " + profile.getCompany().getAddress().getCity());
        System.out.println("      " + profile.getCompany().getAddress().getCountry());
        System.out.println("      " + profile.getCompany().getAddress().getPostalCode());
        System.out.println();

        // 显示技能
        System.out.println("=== 技能 ===");
        for (String skill : profile.getSkills()) {
            System.out.println("  - " + skill);
        }
        System.out.println();

        // 显示个人简介
        System.out.println("=== 个人简介 ===");
        System.out.println(profile.getBio());
    }
}
```

## 列表和集合输出

### 集合类型输出

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import java.util.List;
import java.util.Map;

/**
 * 集合和列表输出示例
 */
public class CollectionOutputExample {

    /**
     * 主题标签
     */
    public static class TopicTags {
        private final List<String> tags;
        private final Map<String, Integer> tagCounts;
        private final String primaryTopic;

        public TopicTags(List<String> tags, Map<String, Integer> tagCounts, String primaryTopic) {
            this.tags = tags;
            this.tagCounts = tagCounts;
            this.primaryTopic = primaryTopic;
        }

        public List<String> getTags() { return tags; }
        public Map<String, Integer> getTagCounts() { return tagCounts; }
        public String getPrimaryTopic() { return primaryTopic; }

        @Override
        public String toString() {
            return String.format("TopicTags{tags=%s, tagCounts=%s, primaryTopic='%s'}",
                    tags, tagCounts, primaryTopic);
        }
    }

    /**
     * 阅读推荐结果
     */
    public static class BookRecommendation {
        private final String title;
        private final String author;
        private final double rating;
        private final List<String> genres;
        private final String summary;
        private final List<String> similarBooks;

        public BookRecommendation(String title, String author, double rating, 
                               List<String> genres, String summary, List<String> similarBooks) {
            this.title = title;
            this.author = author;
            this.rating = rating;
            this.genres = genres;
            this.summary = summary;
            this.similarBooks = similarBooks;
        }

        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public double getRating() { return rating; }
        public List<String> getGenres() { return genres; }
        public String getSummary() { return summary; }
        public List<String> getSimilarBooks() { return similarBooks; }

        @Override
        public String toString() {
            return String.format("BookRecommendation{title='%s', author='%s', rating=%.1f}",
                    title, author, rating);
        }
    }

    /**
     * 主题标签服务
     */
    @AiService
    public interface TopicTaggingService {
        /**
         * 从文本提取主题标签
         */
        TopicTags extractTopics(@UserMessage String text);
    }

    /**
     * 创建主题标签服务
     */
    public static TopicTaggingService createTopicTaggingService(String apiKey) {
        return AiServices.builder(TopicTaggingService.class)
                .chatModel(OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("gpt-4o-mini")
                        .build())
                .systemMessageProvider(chatMemoryId -> 
                    "你是一个主题分析专家。" +
                    "从给定的文本中提取相关的主题标签。" +
                    "返回一个包含以下信息的对象：" +
                    "- tags: 标签列表，包含文本中所有相关的主题" +
                    "- tagCounts: 每个标签的出现次数统计" +
                    "- primaryTopic: 文本的主要主题" +
                    "标签应该简洁明了，每个标签不超过 3 个词。"
                )
                .build();
    }

    public static void main(String[] args) {
        TopicTaggingService service = createTopicTaggingService(
            System.getenv("OPENAI_API_KEY")
        );

        String text = "人工智能、机器学习、深度学习等技术正在改变世界。" +
                     "云计算、大数据、区块链等新兴技术也在快速发展。" +
                     "这些技术的结合将带来更多创新和机遇。";

        // 提取主题标签
        TopicTags topics = service.extractTopics(text);

        // 显示标签
        System.out.println("=== 主题标签 ===");
        System.out.println("主要主题: " + topics.getPrimaryTopic());
        System.out.println();

        System.out.println("标签列表:");
        for (String tag : topics.getTags()) {
            System.out.println("  - " + tag);
        }
        System.out.println();

        // 显示标签统计
        System.out.println("标签统计:");
        topics.getTagCounts().forEach((tag, count) -> {
            System.out.printf("  %s: %d 次\n", tag, count);
        });
    }
}
```

## 测试代码示例

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 结构化输出测试
 */
class StructuredOutputTest {

    private ChatModel model;

    @BeforeEach
    void setUp() {
        this.model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();
    }

    @Test
    void should_generate_simple_response() {
        // 创建简单服务
        SimpleService service = AiServices.builder(SimpleService.class)
                .chatModel(model)
                .build();

        // 生成响应
        SimpleResponse response = service.generateSimpleResponse(
            "生成一个包含消息、数字和确认标志的简单响应"
        );

        // 验证
        assertNotNull(response);
        assertNotNull(response.getMessage());
        assertNotNull(response.getNumber());
        assertNotNull(response.isConfirmed());
    }

    @Test
    void should_extract_user_info() {
        // 创建用户信息服务
        UserService service = AiServices.builder(UserService.class)
                .chatModel(model)
                .build();

        // 提取用户信息
        UserInfo user = service.extractUserInfo(
            "我叫王五，今年 30 岁，邮箱是 wangwu@example.com，我是一名数据分析师"
        );

        // 验证
        assertNotNull(user);
        assertEquals("王五", user.getName());
        assertEquals(30, user.getAge());
        assertEquals("wangwu@example.com", user.getEmail());
        assertEquals("数据分析师", user.getOccupation());
    }

    @Test
    void should_analyze_sentiment() {
        // 创建分类服务
        ClassificationService service = AiServices.builder(ClassificationService.class)
                .chatModel(model)
                .build();

        // 分析文本
        ClassificationResult result = service.analyzeText(
            "LangChain4j 是一个很棒的框架！"
        );

        // 验证
        assertNotNull(result);
        assertNotNull(result.getSentiment());
        assertNotNull(result.getCategory());
        assertTrue(result.getConfidence() > 0);
        assertTrue(result.getConfidence() <= 1.0);
    }

    @Test
    void should_extract_complex_structure() {
        // 创建用户资料服务
        UserProfileService service = AiServices.builder(UserProfileService.class)
                .chatModel(model)
                .build();

        // 提取用户资料
        User profile = service.extractUserProfile(
            "我叫赵六，电话 139-0000-0000，邮箱 zhaoliu@example.com。" +
            "在智慧科技有限公司，有 300 员工，地址是上海市浦东新区张江高科技园区。"
        );

        // 验证
        assertNotNull(profile);
        assertNotNull(profile.getContact());
        assertNotNull(profile.getCompany());
        assertNotNull(profile.getSkills());
        assertNotNull(profile.getBio());
    }

    @Test
    void should_extract_topic_tags() {
        // 创建主题标签服务
        TopicTaggingService service = AiServices.builder(TopicTaggingService.class)
                .chatModel(model)
                .build();

        // 提取主题标签
        TopicTags topics = service.extractTopics(
            "Java 和 Python 是流行的编程语言，" +
            "Spring 和 Django 是流行的开发框架，" +
            "MySQL 和 PostgreSQL 是流行的数据库。"
        );

        // 验证
        assertNotNull(topics);
        assertNotNull(topics.getTags());
        assertFalse(topics.getTags().isEmpty());
        assertNotNull(topics.getTagCounts());
        assertFalse(topics.getTagCounts().isEmpty());
        assertNotNull(topics.getPrimaryTopic());
    }
}
```

## 实践练习

### 练习 1：实现天气查询服务

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 天气查询服务
 */
public class WeatherService {

    /**
     * 天气状况枚举
     */
    public enum WeatherCondition {
        SUNNY("晴朗"),
        CLOUDY("多云"),
        RAINY("下雨"),
        SNOWY("下雪"),
        WINDY("大风"),
        THUNDERSTORM("雷暴"),
        FOGGY("有雾");

        private final String description;

        WeatherCondition(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 温度范围
     */
    public static class TemperatureRange {
        private final int min;
        private final int max;
        private final String description;

        public TemperatureRange(int min, int max, String description) {
            this.min = min;
            this.max = max;
            this.description = description;
        }

        public int getMin() { return min; }
        public int getMax() { return max; }
        public String getDescription() { return description; }
    }

    /**
     * 风速和风向
     */
    public static class WindInfo {
        private final int speed;  // km/h
        private final String direction;  // 北、东北、西南等
        private final String description;

        public WindInfo(int speed, String direction, String description) {
            this.speed = speed;
            this.direction = direction;
            this.description = description;
        }

        public int getSpeed() { return speed; }
        public String getDirection() { return direction; }
        public String getDescription() { return description; }
    }

    /**
     * 完整的天气信息
     */
    public static class WeatherInfo {
        private final WeatherCondition condition;
        private final TemperatureRange temperature;
        private final int humidity;  // 百分比
        private final WindInfo wind;
        private final Optional<Integer> airQuality;  // 空气质量指数
        private final String city;
        private final LocalDate date;
        private final List<String> suggestions;

        public WeatherInfo(WeatherCondition condition, TemperatureRange temperature, 
                         int humidity, WindInfo wind, Integer airQuality,
                         String city, LocalDate date, List<String> suggestions) {
            this.condition = condition;
            this.temperature = temperature;
            this.humidity = humidity;
            this.wind = wind;
            this.airQuality = Optional.ofNullable(airQuality);
            this.city = city;
            this.date = date;
            this.suggestions = suggestions;
        }

        public WeatherCondition getCondition() { return condition; }
        public TemperatureRange getTemperatureRange() { return temperature; }
        public int getHumidity() { return humidity; }
        public WindInfo getWind() { return wind; }
        public Optional<Integer> getAirQuality() { return airQuality; }
        public String getCity() { return city; }
        public LocalDate getDate() { return date; }
        public List<String> getSuggestions() { return suggestions; }
    }

    /**
     * 天气服务接口
     */
    @AiService
    public interface WeatherQueryService {
        /**
         * 查询指定城市和日期的天气信息
         */
        WeatherInfo queryWeather(
            String city,
            LocalDate date
        );
    }

    /**
     * 创建天气查询服务
     */
    public static WeatherQueryService createWeatherService(String apiKey) {
        return AiServices.builder(WeatherQueryService.class)
                .chatModel(OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("gpt-4o-mini")
                        .build())
                .systemMessageProvider(chatMemoryId -> 
                    "你是一个天气查询助手。" +
                    "根据用户提供的城市和日期，" +
                    "提供详细的天气信息，包括：" +
                    "- 天气状况（晴、多云、雨等）" +
                    "- 温度范围" +
                    "- 湿度（百分比）" +
                    "- 风速和风向" +
                    "- 空气质量指数（如有）" +
                    "- 基于天气的建议（如穿什么、是否需要带伞等）" +
                    "返回的数据应该是真实的、合理的，" +
                    "如果某些信息无法确定，给出合理的估计值。"
                )
                .build();
    }

    /**
     * 查询天气
     */
    public static WeatherInfo queryWeather(String city, LocalDate date) {
        WeatherQueryService service = createWeatherService(
            System.getenv("OPENAI_API_KEY")
        );

        return service.queryWeather(city, date);
    }

    /**
     * 显示天气信息
     */
    public static void displayWeatherInfo(WeatherInfo weather) {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║              天气信息                               ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║ 城市: " + weather.getCity());
        System.out.println("║ 日期: " + weather.getDate());
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║ 天气: " + weather.getCondition().getDescription());
        System.out.println("║ 温度: " + weather.getTemperature().getMin() + 
                           " - " + weather.getTemperature().getMax() + "°C");
        System.out.println("║ 湿度: " + weather.getHumidity() + "%");
        System.out.println("║ 风速: " + weather.getWind().getSpeed() + 
                           " km/h (" + weather.getWind().getDirection() + ")");
        System.out.println("║ 建议: " + weather.getSuggestions().get(0));
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
    }

    public static void main(String[] args) {
        // 查询今天北京的天气
        WeatherInfo weather = queryWeather("北京", LocalDate.now());

        // 显示天气信息
        displayWeatherInfo(weather);
    }
}
```

### 练习 2：实现产品目录服务

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 产品目录服务
 */
public class ProductCatalogService {

    /**
     * 产品 POJO
     */
    public static class Product {
        private final String id;
        private final String name;
        private final String description;
        private final BigDecimal price;
        private final String category;
        private final String brand;
        private final int stock;
        private final Map<String, String> attributes;

        public Product(String id, String name, String description, BigDecimal price,
                       String category, String brand, int stock, Map<String, String> attributes) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
            this.category = category;
            this.brand = brand;
            this.stock = stock;
            this.attributes = attributes;
        }

        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public BigDecimal getPrice() { return price; }
        public String getCategory() { return category; }
        public String getBrand() { return brand; }
        public int getStock() { return stock; }
        public Map<String, String> getAttributes() { return attributes; }

        @Override
        public String toString() {
            return String.format("Product{id='%s', name='%s', price=%s}",
                        id, name, price);
        }
    }

    /**
     * 产品信息
     */
    public static class ProductInfo {
        private final List<Product> products;
        private final String summary;
        private final Map<String, Integer> categoryCount;
        private final BigDecimal averagePrice;
        private final String totalValue;

        public ProductInfo(List<Product> products, String summary, 
                         Map<String, Integer> categoryCount,
                         BigDecimal averagePrice, String totalValue) {
            this.products = products;
            this.summary = summary;
            this.categoryCount = categoryCount;
            this.averagePrice = averagePrice;
            this.totalValue = totalValue;
        }

        public List<Product> getProducts() { return products; }
        public String getSummary() { return summary; }
        public Map<String, Integer> getCategoryCount() { return categoryCount; }
        public BigDecimal getAveragePrice() { return averagePrice; }
        public String getTotalValue() { return totalValue; }

        @Override
        public String toString() {
            return String.format("ProductInfo{products=%d, averagePrice=%s}",
                        products.size(), averagePrice);
        }
    }

    /**
     * 产品目录服务接口
     */
    @AiService
    public interface ProductCatalog {
        /**
         * 根据查询信息生成产品目录
         */
        ProductInfo generateCatalog(
            @UserMessage String query
        );
    }

    /**
     * 创建产品目录服务
     */
    public static ProductCatalog createProductCatalog(String apiKey) {
        return AiServices.builder(ProductCatalog.class)
                .chatModel(OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("gpt-4o-mini")
                        .build())
                .systemMessageProvider(chatMemoryId -> 
                    "你是一个产品目录生成专家。" +
                    "根据用户的查询信息，生成相关的产品目录。" +
                    "返回一个包含以下信息的对象：" +
                    "- products: 产品列表，每个产品包含 id、名称、描述、价格、分类、品牌、库存和属性" +
                    "- summary: 产品目录的简要总结" +
                    "- categoryCount: 每个分类的产品数量统计" +
                    "- averagePrice: 平均价格" +
                    "- totalValue: 总价值（库存 * 平均价格）" +
                    "产品应该是真实的、合理的，价格符合市场行情。" +
                    "属性应该包含产品的重要特征。"
                )
                .build();
    }

    /**
     * 生成产品目录
     */
    public static ProductInfo generateCatalog(String query) {
        ProductCatalog service = createProductCatalog(
            System.getenv("OPENAI_API_KEY")
        );

        return service.generateCatalog(query);
    }

    /**
     * 显示产品目录
     */
    public static void displayProductCatalog(ProductInfo info) {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║              产品目录                               ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║ " + info.getSummary());
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║ 产品数量: " + info.getProducts().size());
        System.out.println("║ 平均价格: " + info.getAveragePrice());
        System.out.println("║ 总价值: " + info.getTotalValue());
        System.out.println("╠══════════════════════════════════════════════════════════╣");

        System.out.println("║ 分类统计:");
        info.getCategoryCount().forEach((category, count) -> {
            System.out.printf("║   %s: %d 个产品\n", category, count);
        });
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║ 产品列表:                                               ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");

        for (Product product : info.getProducts()) {
            System.out.printf("║ [%s] %s - %s - %s\n      ║\n",
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    product.getBrand());
            System.out.printf("║     %s\n", 
                        product.getDescription().substring(0, 
                            Math.min(60, product.getDescription().length())));
            System.out.printf("║     库存: %d | 分类: %s\n",
                        product.getStock(),
                        product.getCategory());
            System.out.println("╠─────────────────────────────────────────────────────────────╣");
        }

        System.out.println("╚═════════════════════════════════════════════════════════════════╝");
    }

    public static void main(String[] args) {
        // 生成电子产品目录
        ProductInfo catalog = generateCatalog(
            "生成一个包含 10 个电子产品的目录，" +
            "包括手机、电脑、平板、耳机等，" +
            "价格在 500 到 10000 元之间，品牌包括苹果、华为、小米等知名品牌"
        );

        // 显示产品目录
        displayProductCatalog(catalog);
    }
}
```

## 总结

### 本章要点

1. **结构化输出概念**
   - 将 LLM 响应转换为强类型对象
   - 提高类型安全和代码可读性
   - 简化数据处理流程

2. **注解使用**
   - `@AiService` - 创建 AI 服务
   - `@SystemMessage` - 系统提示词
   - `@UserMessage` - 用户输入
   - 自动类型推断和映射

3. **输出类型**
   - 基本类型（String, Integer, Boolean）
   - 复杂对象（POJO）
   - 枚举类型（@EnumPrompt）
   - 集合类型（List, Map）
   - Optional 类型

4. **最佳实践**
   - 保持 POJO 简单和可序列化
   - 使用构造器或 Builder 模式
   - 为复杂结构提供 toString() 方法
   - 使用合理的默认值

5. **应用场景**
   - 信息提取（用户资料、产品信息等）
   - 文本分类和情感分析
   - 结构化问答
   - 数据验证和清洗

### 下一步

在下一章节中，我们将学习：
- 测试和评估策略
- 自动化测试框架
- 性能基准测试
- A/B 测试
- 错误注入测试

### 常见问题

**Q1：@AiService 和普通的 ChatModel 有什么区别？**

A：
- **@AiService** - 提供类型安全、结构化输出
- **ChatModel** - 返回简单的字符串响应
- 建议：使用 @AiService 获得更好的开发体验

**Q2：如何处理嵌套的复杂对象？**

A：方法：
1. 创建嵌套的 POJO 类
2. 在父类中包含子类引用
3. LangChain4j 自动映射复杂结构
4. 使用 Jackson 处理 JSON 序列化

**Q3：枚举类型如何工作？**

A：工作原理：
1. 定义包含所有可能值的枚举
2. LangChain4j 在提示词中列出所有枚举值
3. LLM 选择合适的枚举值
4. 自动映射回 Java 枚举

**Q4：如何处理可选字段？**

A：方法：
1. 使用 `Optional<String>` 类型
2. 如果 LLM 返回 null 或空字符串，自动包装为 Optional
3. 使用 `orElse()` 提供默认值
4. 使用 `orElseThrow()` 处理必需字段

**Q5：结构化输出会限制 Token 使用量吗？**

A：影响分析：
- 结构化输出可能需要更多 Token（因为有类型定义）
- 但能提高响应质量和准确性
- 权衡：少量额外 Token 换取更好的开发体验
- 建议：优化类型定义，使用简洁的类名

## 参考资料

- [LangChain4j 结构化输出文档](https://docs.langchain4j.dev/tutorials/structured-outputs)
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
