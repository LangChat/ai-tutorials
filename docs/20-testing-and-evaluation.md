---
title: '测试和评估'
description: '学习 LangChain4j 的 测试和评估 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 20 - 测试和评估

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 理解 LangChain4j 应用测试的重要性和挑战
- 掌握单元测试、集成测试和端到端测试的方法
- 学会评估 LLM 响应质量和准确性
- 理解基准测试和性能测试
- 掌握测试 Mock 和模拟技术
- 实现一个完整的测试和评估解决方案

## 前置知识

- 完成《01 - LangChain4j 简介》章节
- 完成《02 - 你的第一个 Chat 应用》章节
- Java 测试框架基础（JUnit, AssertJ）

## 核心概念

### 为什么测试 LLM 应用特殊？

**LLM 应用的测试挑战：**

1. **非确定性输出**
   - 相同的输入可能产生不同的输出
   - 难以编写精确的断言

2. **API 成本**
   - 每次测试都消耗 Token
   - 需要平衡测试覆盖率和成本

3. **依赖外部服务**
   - 依赖 LLM API 的可用性
   - 需要使用 Mock 进行隔离测试

4. **响应质量评估**
   - 难以量化"质量"好坏
   - 需要人工评估或自动化指标

### 测试金字塔

```
        ╱═══════════════╗
        ║  端到端测试  ║  ← 少量，慢，覆盖真实场景
        ╠═══════════════╣
        ║  集成测试    ║  ← 中等数量，中等速度，测试模块集成
        ╠═══════════════╣
        ║  单元测试      ║  ← 大量，快速，测试独立功能
        ╚═══════════════╝
```

## 单元测试

### 基础单元测试

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 单元测试示例
 */
class UnitTestExample {

    /**
     * AI 服务接口
     */
    @AiService
    public interface ChatService {
        String chat(String message);
    }

    /**
     * 测试 AI 服务基本功能
     */
    @Test
    void should_return_response() {
        // 创建 Mock 模型
        ChatModel mockModel = mock(ChatModel.class);
        when(mockModel.chat(anyString())).thenReturn("Mocked response");

        // 创建服务
        ChatService service = AiServices.builder(ChatService.class)
                .chatModel(mockModel)
                .build();

        // 执行测试
        String response = service.chat("Hello");

        // 验证
        assertNotNull(response);
        assertEquals("Mocked response", response);
        verify(mockModel).chat("Hello");
    }

    /**
     * 测试异常情况
     */
    @Test
    void should_handle_errors() {
        // 创建 Mock 模型，抛出异常
        ChatModel mockModel = mock(ChatModel.class);
        when(mockModel.chat(anyString()))
                .thenThrow(new RuntimeException("API error"));

        ChatService service = AiServices.builder(ChatService.class)
                .chatModel(mockModel)
                .build();

        // 执行和验证
        assertThrows(RuntimeException.class, () -> {
            service.chat("Hello");
        });
    }

    /**
     * 测试参数验证
     */
    @Test
    void should_validate_parameters() {
        ChatModel mockModel = mock(ChatModel.class);
        
        ChatService service = AiServices.builder(ChatService.class)
                .chatModel(mockModel)
                .build();

        // 测试空消息
        assertThrows(IllegalArgumentException.class, () -> {
            service.chat("");
        });

        // 测试 null 消息
        assertThrows(IllegalArgumentException.class, () -> {
            service.chat(null);
        });
    }

    /**
     * 测试多个调用
     */
    @Test
    void should_handle_multiple_calls() {
        ChatModel mockModel = mock(ChatModel.class);
        when(mockModel.chat(anyString())).thenReturn("Response");

        ChatService service = AiServices.builder(ChatService.class)
                .chatModel(mockModel)
                .build();

        // 多次调用
        String response1 = service.chat("Message 1");
        String response2 = service.chat("Message 2");
        String response3 = service.chat("Message 3");

        // 验证
        assertEquals("Response", response1);
        assertEquals("Response", response2);
        assertEquals("Response", response3);
        verify(mockModel, times(3)).chat(anyString());
    }
}
```

## 集成测试

### 真实模型测试

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * 集成测试示例
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@MethodOrderer(MethodOrderer.Order.class)
class IntegrationTestExample {

    private static OpenAiChatModel model;

    /**
     * 设置测试环境（一次性）
     */
    @BeforeAll
    static void setUp() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        assumeTrue(apiKey != null && !apiKey.isEmpty(), "OPENAI_API_KEY 未设置");

        model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .timeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * 测试基本对话
     */
    @Test
    @Order(1)
    void should_perform_basic_chat() {
        // 创建服务
        ChatService service = AiServices.builder(ChatService.class)
                .chatModel(model)
                .build();

        // 执行对话
        String response = service.chat("你好，请介绍一下自己");

        // 验证
        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertTrue(response.length() > 10);
    }

    /**
     * 测试对话上下文
     */
    @Test
    @Order(2)
    void should_maintain_conversation_context() {
        ConversationalChatService service = 
            AiServices.builder(ConversationalChatService.class)
                    .chatModel(model)
                    .build();

        // 多轮对话
        String response1 = service.chat("我叫张三");
        String response2 = service.chat("我的名字是什么？");

        // 验证上下文
        assertNotNull(response2);
        // response2 应该能够记住名字"张三"
    }

    /**
     * 测试流式响应
     */
    @Test
    @Order(3)
    void should_stream_responses() {
        StreamingChatService service = 
            AiServices.builder(StreamingChatService.class)
                    .streamingChatModel(model)
                    .build();

        // 执行流式对话
        StringBuilder fullResponse = new StringBuilder();
        String token = service.chat("请生成一个短故事", 
                                    nextToken -> fullResponse.append(nextToken));

        // 验证
        assertFalse(fullResponse.isEmpty());
        assertNotNull(fullResponse.toString());
    }

    /**
     * 测试错误处理
     */
    @Test
    @Order(4)
    void should_handle_api_errors() {
        // 使用无效 API Key（应该会失败）
        String invalidKey = "sk-invalid";
        
        OpenAiChatModel invalidModel = OpenAiChatModel.builder()
                .apiKey(invalidKey)
                .modelName("gpt-4o-mini")
                .timeout(Duration.ofSeconds(5))
                .build();

        ChatService service = AiServices.builder(ChatService.class)
                .chatModel(invalidModel)
                .build();

        // 执行并验证异常
        Exception exception = assertThrows(Exception.class, () -> {
            service.chat("测试");
        });

        assertNotNull(exception);
    }

    /**
     * 测试性能
     */
    @Test
    @Order(5)
    @Timeout(30)
    void should_respond_within_time_limit() {
        ChatService service = AiServices.builder(ChatService.class)
                .chatModel(model)
                .build();

        long startTime = System.currentTimeMillis();
        String response = service.chat("你好");
        long duration = System.currentTimeMillis() - startTime;

        // 验证响应时间
        assertNotNull(response);
        assertTrue(duration < 10000, "响应时间应该少于 10 秒");
    }

    /**
     * 测试 Chat 服务
     */
    @AiService
    public interface ChatService {
        String chat(String message);
    }

    /**
     * 对话服务
     */
    @AiService
    public interface ConversationalChatService {
        String chat(String message);
    }

    /**
     * 流式对话服务
     */
    @AiService
    public interface StreamingChatService {
        String chat(String message, Consumer<String> nextTokenConsumer);
    }
}
```

## Mock 测试

### 使用 Mock 模型

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.data.message.AiMessage;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Mock 测试示例
 */
class MockTestExample {

    /**
     * 创建 Mock 模型
     */
    private ChatModel createMockModel(String response) {
        ChatModel mockModel = mock(ChatModel.class);
        when(mockModel.chat(anyString()))
                .thenReturn(new AiMessage(response));
        return mockModel;
    }

    /**
     * 测试预设响应
     */
    @Test
    void should_use_mocked_responses() {
        // 创建 Mock 模型，返回特定响应
        String expectedResponse = "这是一个模拟的响应";
        ChatModel mockModel = createMockModel(expectedResponse);

        ChatService service = AiServices.builder(ChatService.class)
                .chatModel(mockModel)
                .build();

        // 执行
        String actualResponse = service.chat("任何输入");

        // 验证
        assertEquals(expectedResponse, actualResponse);
    }

    /**
     * 测试不同场景的响应
     */
    @Test
    void should_simulate_different_scenarios() {
        ChatModel mockModel = mock(ChatModel.class);

        // 根据输入返回不同的响应
        when(mockModel.chat(argThat(arg -> arg.contains("问候"))))
                .thenReturn(new AiMessage("你好！"));
        when(mockModel.chat(argThat(arg -> arg.contains("提问"))))
                .thenReturn(new AiMessage("答案"));
        when(mockModel.chat(anyString()))
                .thenReturn(new AiMessage("默认回复"));

        ChatService service = AiServices.builder(ChatService.class)
                .chatModel(mockModel)
                .build();

        // 测试不同场景
        assertEquals("你好！", service.chat("问候所有人"));
        assertEquals("答案", service.chat("提问：天为什么是蓝的？"));
        assertEquals("默认回复", service.chat("其他内容"));
    }

    /**
     * 测试错误场景
     */
    @Test
    void should_simulate_error_scenarios() {
        ChatModel mockModel = mock(ChatModel.class);

        // 模拟超时错误
        when(mockModel.chat(argThat(arg -> arg.contains("超时测试"))))
                .thenThrow(new RuntimeException("请求超时"));

        // 模拟认证错误
        when(mockModel.chat(argThat(arg -> arg.contains("认证测试"))))
                .thenThrow(new RuntimeException("认证失败"));

        ChatService service = AiServices.builder(ChatService.class)
                .chatModel(mockModel)
                .build();

        // 测试超时
        Exception timeoutException = assertThrows(RuntimeException.class, () -> {
            service.chat("超时测试输入");
        });
        assertEquals("请求超时", timeoutException.getMessage());

        // 测试认证失败
        Exception authException = assertThrows(RuntimeException.class, () -> {
            service.chat("认证测试输入");
        });
        assertEquals("认证失败", authException.getMessage());
    }

    /**
     * 测试流式响应
     */
    @Test
    void should_mock_streaming_responses() {
        // 注意：流式 Mock 较为复杂，这里简化演示
        String[] mockTokens = {"这是", "一个", "模拟的", "流式", "响应"};
        StringBuilder fullResponse = new StringBuilder();

        ChatService service = AiServices.builder(ChatService.class)
                .chatModel(mock(ChatModel.class))
                .build();

        // 简化：实际应该使用流式模型
        for (String token : mockTokens) {
            fullResponse.append(token);
        }

        String expectedResponse = "这是一个模拟的流式响应";
        assertEquals(expectedResponse, fullResponse.toString());
    }

    /**
     * 验证 Mock 调用
     */
    @Test
    void should_verify_mock_interactions() {
        ChatModel mockModel = mock(ChatModel.class);
        when(mockModel.chat(anyString()))
                .thenReturn(new AiMessage("响应"));

        ChatService service = AiServices.builder(ChatService.class)
                .chatModel(mockModel)
                .build();

        service.chat("测试消息");

        // 验证 Mock 被正确调用
        verify(mockModel, times(1)).chat("测试消息");
        verify(mockModel, never()).chat(argThat(arg -> arg == null));
    }

    /**
     * Chat 服务
     */
    @AiService
    public interface ChatService {
        String chat(String message);
    }
}
```

## 基准测试

### 性能基准测试

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.junit.jupiter.api.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.TimeUnit;

import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 性能基准测试
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class PerformanceBenchmark {

    private static OpenAiChatModel model;

    /**
     * 设置基准测试
     */
    @Setup
    public static void setUp() throws Exception {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            // 如果没有 API Key，使用 Mock
            return;
        }

        model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    /**
     * 基准：简单对话
     */
    @Benchmark
    @Warmup(iterations = 3)
    @Measurement(iterations = 10)
    public String benchmarkSimpleChat() {
        ChatService service = AiServices.builder(ChatService.class)
                .chatModel(model)
                .build();

        return service.chat("你好");
    }

    /**
     * 基准：批量对话
     */
    @Benchmark
    @Warmup(iterations = 3)
    @Measurement(iterations = 10)
    public List<String> benchmarkBatchChat() {
        ChatService service = AiServices.builder(ChatService.class)
                .chatModel(model)
                .build();

        List<String> responses = new ArrayList<>();
        String[] messages = {
            "问题1", "问题2", "问题3", "问题4", "问题5"
        };

        for (String message : messages) {
            responses.add(service.chat(message));
        }

        return responses;
    }

    /**
     * 基准：上下文对话
     */
    @Benchmark
    @Warmup(iterations = 3)
    @Measurement(iterations = 10)
    public String benchmarkContextualChat() {
        ConversationalChatService service = 
            AiServices.builder(ConversationalChatService.class)
                    .chatModel(model)
                    .build();

        // 设置初始上下文
        service.chat("我叫李四");
        
        // 基准：查询
        return service.chat("我的名字是什么？");
    }

    /**
     * Chat 服务
     */
    @AiService
    public interface ChatService {
        String chat(String message);
    }

    /**
     * 对话服务
     */
    @AiService
    public interface ConversationalChatService {
        String chat(String message);
    }

    /**
     * 运行基准测试
     */
    public static void main(String[] args) throws Exception {
        if (model == null) {
            System.out.println("警告: 未设置 OPENAI_API_KEY，跳过基准测试");
            return;
        }

        Options opt = new OptionsBuilder()
                .include(PerformanceBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}
```

## 评估指标

### 质量评估

```java
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 质量评估
 */
class QualityAssessmentExample {

    /**
     * 评估指标
     */
    public static class EvaluationMetrics {
        private final String prompt;
        private final String response;
        private final int responseLength;
        private final double relevanceScore;
        private final double coherenceScore;
        private final double fluencyScore;
        private final double overallScore;

        public EvaluationMetrics(String prompt, String response, 
                              int responseLength, double relevanceScore,
                              double coherenceScore, double fluencyScore) {
            this.prompt = prompt;
            this.response = response;
            this.responseLength = responseLength;
            this.relevanceScore = relevanceScore;
            this.coherenceScore = coherenceScore;
            this.fluencyScore = fluencyScore;
            this.overallScore = (relevanceScore + coherenceScore + fluencyScore) / 3.0;
        }

        // Getters
        public String getPrompt() { return prompt; }
        public String getResponse() { return response; }
        public int getResponseLength() { return responseLength; }
        public double getRelevanceScore() { return relevanceScore; }
        public double getCoherenceScore() { return coherenceScore; }
        public double getFluencyScore() { return fluencyScore; }
        public double getOverallScore() { return overallScore; }
    }

    /**
     * 评估响应相关性
     */
    public static double evaluateRelevance(String prompt, String response) {
        // 简化实现：实际应该使用更复杂的算法
        // 检查响应是否包含提示词中的关键词
        
        String[] promptWords = prompt.toLowerCase().split("\\s+");
        String responseWords = response.toLowerCase().split("\\s+");

        int matchCount = 0;
        for (String promptWord : promptWords) {
            for (String responseWord : responseWords) {
                if (promptWord.equals(responseWord)) {
                    matchCount++;
                    break;
                }
            }
        }

        // 相关性得分（0.0 到 1.0）
        double relevance = (double) matchCount / promptWords.length();
        return Math.min(relevance, 1.0);
    }

    /**
     * 评估响应连贯性
     */
    public static double evaluateCoherence(String response) {
        // 简化实现：检查句子结构
        
        String[] sentences = response.split("[.!?。！]");
        
        if (sentences.length == 0) {
            return 0.0;
        }

        // 计算平均句子长度
        int totalLength = 0;
        for (String sentence : sentences) {
            totalLength += sentence.trim().length();
        }

        double avgSentenceLength = (double) totalLength / sentences.length();

        // 连贯性得分（0.0 到 1.0）
        // 假设平均句子长度在 20-100 字之间比较合理
        double coherence = 0.0;
        if (avgSentenceLength >= 20 && avgSentenceLength <= 100) {
            coherence += 0.5;
        }
        if (avgSentenceLength >= 30 && avgSentenceLength <= 80) {
            coherence += 0.5;
        }

        return Math.min(coherence, 1.0);
    }

    /**
     * 评估响应流畅度
     */
    public static double evaluateFluency(String response) {
        // 简化实现：检查流畅度指标
        
        // 检查重复词汇
        String[] words = response.toLowerCase().split("\\s+");
        Map<String, Integer> wordCounts = new HashMap<>();
        for (String word : words) {
            wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
        }

        // 计算重复率
        int totalWords = words.length;
        int repeatedWords = 0;
        for (int count : wordCounts.values()) {
            if (count > 1) {
                repeatedWords += count;
            }
        }

        double repetitionRate = (double) repeatedWords / totalWords;

        // 流畅度得分（0.0 到 1.0），重复率越低得分越高
        return 1.0 - Math.min(repetitionRate, 1.0);
    }

    /**
     * 评估响应
     */
    public static EvaluationMetrics evaluate(String prompt, String response) {
        int responseLength = response.length();
        double relevanceScore = evaluateRelevance(prompt, response);
        double coherenceScore = evaluateCoherence(response);
        double fluencyScore = evaluateFluency(response);

        return new EvaluationMetrics(
            prompt,
            response,
            responseLength,
            relevanceScore,
            coherenceScore,
            fluencyScore
        );
    }

    /**
     * 批量评估
     */
    public static List<EvaluationMetrics> evaluateBatch(
            List<String> prompts,
            List<String> responses
    ) {
        if (prompts.size() != responses.size()) {
            throw new IllegalArgumentException("提示词和响应列表长度不匹配");
        }

        List<EvaluationMetrics> results = new ArrayList<>();
        for (int i = 0; i < prompts.size(); i++) {
            results.add(evaluate(prompts.get(i), responses.get(i)));
        }

        return results;
    }

    /**
     * 生成评估报告
     */
    public static String generateReport(List<EvaluationMetrics> metrics) {
        double avgOverallScore = metrics.stream()
                .mapToDouble(EvaluationMetrics::getOverallScore)
                .average()
                .orElse(0.0);

        double avgRelevance = metrics.stream()
                .mapToDouble(EvaluationMetrics::getRelevanceScore)
                .average()
                .orElse(0.0);

        double avgCoherence = metrics.stream()
                .mapToDouble(EvaluationMetrics::getCoherenceScore)
                .average()
                .orElse(0.0);

        double avgFluency = metrics.stream()
                .mapToDouble(EvaluationMetrics::getFluencyScore)
                .average()
                .orElse(0.0);

        return String.format(
            "╔═════════════════════════════════════════════════════╗\n" +
            "║                  质量评估报告                          ║\n" +
            "╠═════════════════════════════════════════════════════╣\n" +
            "║ 评估数量: %d                                      ║\n" +
            "╠═════════════════════════════════════════════════════╣\n" +
            "║ 总体得分: %.2f/1.00                              ║\n" +
            "║ 相关性得分: %.2f/1.00                           ║\n" +
            "║ 连贯性得分: %.2f/1.00                           ║\n" +
            "║ 流畅度得分: %.2f/1.00                           ║\n" +
            "╠═════════════════════════════════════════════════════╣\n" +
            "║ 详细结果:                                               ║\n",
            metrics.size(),
            avgOverallScore,
            avgRelevance,
            avgCoherence,
            avgFluency
        );
    }

    public static void main(String[] args) {
        // 评估示例
        String[] prompts = {
            "介绍一下 Java 编程语言",
            "什么是机器学习？",
            "如何优化代码性能？"
        };

        String[] responses = {
            "Java 是一种面向对象的编程语言，由 Sun 公司在 1995 年推出。" +
            "它具有简单、面向对象、安全、平台无关等特点。" +
            "Java 广泛应用于企业级应用、移动应用、嵌入式系统等领域。" +
            "Java 的跨平台特性使得它可以在 Windows、Linux、macOS 等操作系统上运行。" +
            "Java 还有丰富的生态系统，包括 Spring、Hibernate、MyBatis 等框架，" +
            "使得开发者可以快速构建高质量的应用程序。",
            
            "机器学习是人工智能的一个分支，它使计算机系统能够从数据中学习。" +
            "机器学习算法构建模型，使计算机能够在没有显式编程的情况下执行任务。" +
            "常见的机器学习类型包括监督学习、无监督学习、强化学习和半监督学习。" +
            "监督学习使用标记数据进行训练，如分类和回归问题。" +
            "无监督学习使用未标记数据发现数据中的模式，如聚类和降维。" +
            "强化学习通过与环境交互学习最优策略，如游戏机器人。",
            
            "代码性能优化是软件开发中的重要环节。" +
            "常见的优化方法包括：选择合适的数据结构和算法、减少不必要的计算、" +
            "使用缓存避免重复计算、并行化处理提高吞吐量、" +
            "使用更高效的算法和库、减少 I/O 操作、优化数据库查询等。" +
            "优化应该基于实际的性能分析，而不是过早优化。" +
            "常用的性能分析工具包括 Java VisualVM、JProfiler 和 YourKit 等。"
        };

        // 批量评估
        List<EvaluationMetrics> metrics = evaluateBatch(
            List.of(prompts),
            List.of(responses)
        );

        // 生成报告
        String report = generateReport(metrics);
        System.out.println(report);

        System.out.println("║ 详细评估:                                               ║");
        System.out.println("╠═════════════════════════════════════════════════════╣");
        
        for (int i = 0; i < metrics.size(); i++) {
            EvaluationMetrics metric = metrics.get(i);
            System.out.printf("║ [%d] 得分: %.2f | 长度: %d                 ║\n",
                        i + 1,
                        metric.getOverallScore(),
                        metric.getResponseLength());
        }

        System.out.println("╚═══════════════════════════════════════════════════════════╝");
    }
}
```

## 测试最佳实践

### 测试指南

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * 测试最佳实践
 */
class TestingBestPractices {

    /**
     * 使用参数化测试
     */
    @ParameterizedTest
    @ValueSource(strings = {"你好", "Hello", "123", "测试", ""})
    void should_handle_various_inputs(String input) {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        ChatService service = AiServices.builder(ChatService.class)
                .chatModel(model)
                .build();

        // 跳过空输入测试（如果模型要求非空）
        if (input.isEmpty()) {
            assertThrows(IllegalArgumentException.class, () -> {
                service.chat(input);
            });
            return;
        }

        // 执行和验证
        String response = service.chat(input);
        assertNotNull(response);
        assertFalse(response.isEmpty());
    }

    /**
     * 使用条件测试
     */
    @Test
    void should_test_conditional_logic() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        assumeTrue(apiKey != null && !apiKey.isEmpty(), "需要有效的 API Key");

        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .build();

        ChatService service = AiServices.builder(ChatService.class)
                .chatModel(model)
                .build();

        // 测试正常情况
        String response1 = service.chat("正常输入");
        assertNotNull(response1);
        assertFalse(response1.isEmpty());

        // 测试超长输入
        String longInput = "测试".repeat(1000);
        String response2 = service.chat(longInput);
        assertNotNull(response2);
        assertFalse(response2.isEmpty());

        // 测试特殊字符
        String specialChars = "测试 @#$%^&*()[]{}|\\:;\"'<>?,./";
        String response3 = service.chat(specialChars);
        assertNotNull(response3);
    }

    /**
     * 测试超时和重试
     */
    @Test
    void should_test_timeout_and_retry() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        assumeTrue(apiKey != null && !apiKey.isEmpty(), "需要有效的 API Key");

        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .timeout(Duration.ofSeconds(5))    // 短超时
                .maxRetries(2)                // 重试次数
                .build();

        ChatService service = AiServices.builder(ChatService.class)
                .chatModel(model)
                .build();

        // 测试超时
        long startTime = System.currentTimeMillis();
        String response = service.chat("测试超时");
        long duration = System.currentTimeMillis() - startTime;

        // 验证
        assertNotNull(response);
        // 注意：如果真正超时，可能会抛出异常
        System.out.println("响应时间: " + duration + "ms");
    }

    /**
     * 使用 @Before 和 @After
     */
    @Test
    void should_use_before_and_after() {
        ChatService service = createService();

        // 测试前状态
        String response1 = service.chat("测试 1");
        assertNotNull(response1);

        // 测试中状态
        String response2 = service.chat("测试 2");
        assertNotNull(response2);

        // 测试后清理
        // 可以在这里清理资源
    }

    /**
     * 测试异常处理
     */
    @Test
    void should_test_exception_handling() {
        // 使用无效的模型配置
        String invalidApiKey = "sk-invalid";

        OpenAiChatModel invalidModel = OpenAiChatModel.builder()
                .apiKey(invalidApiKey)
                .modelName("gpt-4o-mini")
                .timeout(Duration.ofSeconds(5))
                .build();

        ChatService service = AiServices.builder(ChatService.class)
                .chatModel(invalidModel)
                .build();

        // 测试并验证异常
        assertThrows(Exception.class, () -> {
            service.chat("测试");
        });
    }

    /**
     * Chat 服务
     */
    @AiService
    public interface ChatService {
        String chat(String message);
    }

    /**
     * 创建服务
     */
    private static ChatService createService() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            return null;
        }

        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .build();

        return AiServices.builder(ChatService.class)
                .chatModel(model)
                .build();
    }
}
```

## 实践练习

### 练习 1：实现自动化测试套件

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 自动化测试套件
 */
public class AutomatedTestSuite {

    private static OpenAiChatModel model;
    private static final List<String> testCases = new ArrayList<>();
    private static final List<TestResult> results = new ArrayList<>();

    /**
     * 测试结果
     */
    public static class TestResult {
        private final String testName;
        private final String prompt;
        private final String response;
        private final long durationMs;
        private final int responseLength;
        private final boolean passed;
        private final String errorMessage;

        public TestResult(String testName, String prompt, String response, 
                         long durationMs, int responseLength, 
                         boolean passed, String errorMessage) {
            this.testName = testName;
            this.prompt = prompt;
            this.response = response;
            this.durationMs = durationMs;
            this.responseLength = responseLength;
            this.passed = passed;
            this.errorMessage = errorMessage;
        }

        public String getTestName() { return testName; }
        public String getPrompt() { return prompt; }
        public String getResponse() { return response; }
        public long getDurationMs() { return durationMs; }
        public int getResponseLength() { return responseLength; }
        public boolean isPassed() { return passed; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * 测试用例
     */
    public static class TestCase {
        private final String name;
        private final String prompt;
        private final List<String> expectedKeywords;
        private final int minLength;
        private final int maxLength;

        public TestCase(String name, String prompt, List<String> expectedKeywords, 
                      int minLength, int maxLength) {
            this.name = name;
            this.prompt = prompt;
            this.expectedKeywords = expectedKeywords;
            this.minLength = minLength;
            this.maxLength = maxLength;
        }

        public String getName() { return name; }
        public String getPrompt() { return prompt; }
        public List<String> getExpectedKeywords() { return expectedKeywords; }
        public int getMinLength() { return minLength; }
        public int getMaxLength() { return maxLength; }
    }

    /**
     * 初始化测试套件
     */
    @BeforeAll
    static void setUp() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("警告: 未设置 OPENAI_API_KEY，跳过真实测试");
            return;
        }

        model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .timeout(Duration.ofSeconds(30))
                .build();

        // 定义测试用例
        testCases.add(new TestCase(
            "基础问候",
            "你好，请介绍一下自己",
            List.of("介绍", "语言模型", "助手"),
            20,
            500
        ));

        testCases.add(new TestCase(
            "技术问题",
            "什么是 Java？",
            List.of("编程", "语言", "面向对象"),
            30,
            400
        ));

        testCases.add(new TestCase(
            "数学计算",
            "计算 123 + 456",
            List.of("579"),
            5,
            20
        ));

        testCases.add(new TestCase(
            "代码生成",
            "生成一个 Java 方法来计算斐波那契数列",
            List.of("public", "int", "fibonacci", "for"),
            50,
            500
        ));
    }

    /**
     * 运行所有测试
     */
    @Test
    @EnabledIf("model != null")
    void should_run_all_tests() {
        System.out.println("╔═════════════════════════════════════════════════════════════════════╗");
        System.out.println("║              自动化测试套件                              ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════╣");
        System.out.println("║ 测试数量: " + testCases.size() + "                                      ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════╝");
        System.out.println();

        ChatService service = AiServices.builder(ChatService.class)
                .chatModel(model)
                .build();

        int passedCount = 0;
        int failedCount = 0;

        for (int i = 0; i < testCases.size(); i++) {
            TestCase testCase = testCases.get(i);
            TestResult result = runTestCase(service, testCase);
            results.add(result);

            if (result.isPassed()) {
                passedCount++;
            } else {
                failedCount++;
            }

            displayResult(result);
        }

        // 显示总结
        displaySummary(passedCount, failedCount);

        // 导出结果
        exportResults();
    }

    /**
     * 运行单个测试用例
     */
    private static TestResult runTestCase(ChatService service, TestCase testCase) {
        try {
            long startTime = System.currentTimeMillis();
            String response = service.chat(testCase.getPrompt());
            long duration = System.currentTimeMillis() - startTime;
            int responseLength = response.length();

            // 验证响应
            boolean passed = true;
            StringBuilder errorMessage = new StringBuilder();

            // 检查长度
            if (responseLength < testCase.getMinLength()) {
                passed = false;
                errorMessage.append(String.format("响应太短 (%d < %d)", 
                            responseLength, testCase.getMinLength()));
            } else if (responseLength > testCase.getMaxLength()) {
                passed = false;
                errorMessage.append(String.format("响应太长 (%d > %d)", 
                            responseLength, testCase.getMaxLength()));
            }

            // 检查关键词
            String lowerResponse = response.toLowerCase();
            for (String keyword : testCase.getExpectedKeywords()) {
                if (!lowerResponse.contains(keyword.toLowerCase())) {
                    if (errorMessage.length() > 0) {
                        errorMessage.append(", ");
                    }
                    errorMessage.append(String.format("缺少关键词: %s", keyword));
                    passed = false;
                }
            }

            return new TestResult(
                testCase.getName(),
                testCase.getPrompt(),
                response,
                duration,
                responseLength,
                passed,
                errorMessage.length() > 0 ? errorMessage.toString() : null
            );
        } catch (Exception e) {
            return new TestResult(
                testCase.getName(),
                testCase.getPrompt(),
                "ERROR: " + e.getMessage(),
                0,
                0,
                false,
                e.getMessage()
            );
        }
    }

    /**
     * 显示测试结果
     */
    private static void displayResult(TestResult result) {
        String status = result.isPassed() ? "✅ PASS" : "❌ FAIL";
        System.out.printf("║ [%d] %s - %s - 耗时: %dms, 长度: %d      ║\n",
                    results.size() + 1,
                    result.getTestName(),
                    status,
                    result.getDurationMs(),
                    result.getResponseLength());

        if (!result.isPassed()) {
            System.out.printf("║     错误: %s                                       ║\n",
                        result.getErrorMessage());
        }

        System.out.println("║ 响应: " + 
            result.getResponse().substring(0, 
                Math.min(80, result.getResponse().length())) + 
            "...                                                 ║\n");
        System.out.println("╠─────────────────────────────────────────────────────────────────╣");
    }

    /**
     * 显示总结
     */
    private static void displaySummary(int passedCount, int failedCount) {
        System.out.println("╔═════════════════════════════════════════════════════════════════════╗");
        System.out.println("║              测试总结                                  ║");
        System.out.println("╠════════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ 通过: %d (%.1f%%)                                    ║\n",
                    passedCount, (double) passedCount / (passedCount + failedCount) * 100);
        System.out.printf("║ 失败: %d (%.1f%%)                                    ║\n",
                    failedCount, (double) failedCount / (passedCount + failedCount) * 100);
        System.out.printf("║ 总计: %d                                                 ║\n",
                    passedCount + failedCount);
        System.out.println("╚═════════════════════════════════════════════════════════════════════════╝");
    }

    /**
     * 导出结果
     */
    private static void exportResults() {
        // 简化：实际应该导出为 JSON 或 CSV
        System.out.println();
        System.out.println("详细结果:");
        for (TestResult result : results) {
            System.out.printf("- %s: %s (耗时: %dms)\n",
                        result.getTestName(),
                        result.isPassed() ? "PASS" : "FAIL",
                        result.getDurationMs());
        }
    }

    /**
     * Chat 服务
     */
    @AiService
    public interface ChatService {
        String chat(String message);
    }

    public static void main(String[] args) {
        AutomatedTestSuite suite = new AutomatedTestSuite();

        suite.setUp();
        suite.should_run_all_tests();
    }
}
```

## 总结

### 本章要点

1. **测试重要性**
   - 确保应用质量和可靠性
   - 提早发现和修复问题
   - 建立对应用的信心

2. **测试类型**
   - 单元测试：快速、独立、可重复
   - 集成测试：测试模块间交互
   - 端到端测试：测试真实场景
   - 性能测试：测量和优化性能

3. **Mock 和模拟**
   - 使用 Mock 隔离外部依赖
   - 控制测试环境和结果
   - 减少测试成本和时间

4. **评估指标**
   - 响应相关性
   - 响应连贯性
   - 响应流畅度
   - 整体质量得分

5. **最佳实践**
   - 使用测试金字塔平衡测试类型
   - 编写可维护的测试代码
   - 使用参数化测试覆盖多种情况
   - 持续集成测试到 CI/CD 流程

### 下一步

在下一章节中，我们将学习：
- Spring Boot 集成
- Web 应用开发
- REST API 创建
- WebSocket 实时通信
- 安全和认证

### 常见问题

**Q1：如何测试非确定性的 LLM 输出？**

A：测试策略：
1. 使用模糊断言（assertNotContains, assertMatches）
2. 关注响应质量而非精确匹配
3. 测试多次，取平均结果
4. 使用人工评估验证

**Q2：如何减少测试成本？**

A：优化方法：
1. 使用 Mock 模型替代真实 API 调用
2. 使用缓存记录和重用测试结果
3. 只在必要时运行集成测试
4. 使用并行测试加快速度
5. 使用轻量级模型进行测试

**Q3：如何测试流式响应？**

A：测试方法：
1. 验证 Token 流的正确性
2. 检查流是否正常结束
3. 验证累计的完整响应
4. 测试流中的错误处理

**Q4：如何自动化评估？**

A：自动化策略：
1. 定义评估指标和阈值
2. 使用 NLP 技术自动评分
3. 对比多个模型或提示词
4. 定期运行评估并生成报告
5. 设置性能回归测试

**Q5：如何处理测试中的随机性？**

A：处理方法：
1. 设置随机种子获得可重复结果
2. 多次运行取平均值
3. 使用统计方法分析结果分布
4. 定义可接受的范围而非精确值
5. 关注趋势而非单次结果

## 参考资料

- [LangChain4j 测试文档](https://docs.langchain4j.dev/tutorials/testing-and-evaluation)
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
