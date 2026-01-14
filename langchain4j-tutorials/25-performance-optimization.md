---
title: '性能优化'
description: '学习 LangChain4j 的 性能优化 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 25 - 性能优化技巧

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 理解 LangChain4j 应用的性能瓶颈
- 掌握响应时间的优化策略
- 学会 Token 使用优化方法
- 理解并发和异步处理
- 掌握缓存和批处理技巧
- 实现一个高性能的 LLM 应用

## 前置知识

- 完成《01 - LangChain4j 简介》章节
- 完成《12 - 流式输出详解》章节
- Java 性能优化基础知识

## 核心概念

### 性能瓶颈分析

**LLM 应用的常见瓶颈：**

```
┌─────────────────────────────────────────────────────────┐
│                  性能瓶颈分析                          │
├─────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────┐    ┌──────────┐    ┌───────────┐  │
│  │ 网络    │    │  模型     │    │  应用层    │  │
│  │ I/O     │    │  响应时间 │    │  处理逻辑  │  │
│  └────┬────┘    └────┬─────┘    └────┬──────┘  │
│       │              │                 │          │  │
│       └──────────────┴─────────────────┘          │  │
│                    模型调用                           │  │
│                       │                             │  │
│           ┌─────────┴─────────┐                     │  │
│           │                  │                     │  │
│           │     数据处理     │                     │  │
│           │                  │                     │  │
│           │                  │                     │  │
│           └──────────────────┘                     │  │
│                                               │  │
│                                          总响应时间  │
│                                                             │
└─────────────────────────────────────────────────────┘

网络 I/O: 10-30% ┃ 模型处理: 60-80% ┃ 应用层: 10-20%
```

### 优化策略概览

| 优化方向 | 具体措施 | 预期效果 |
|---------|---------|---------|
| **模型选择** | 使用更快的模型 | 30-50% 提升 |
| **Token 优化** | 优化 Prompt 和响应 | 20-40% 节省 |
| **并发处理** | 异步和并行调用 | 2-5x 吞吐量 |
| **缓存策略** | 缓存常见查询 | 50-90% 节省 |
| **批处理** | 合并多个请求 | 20-30% 提升 |
| **连接池** | 复用 HTTP 连接 | 10-20% 提升 |
| **流式输出** | 实时反馈体验 | 提升用户体验 |

## 响应时间优化

### Prompt 优化

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prompt 优化
 */
public class PromptOptimization {

    private static final Logger logger = LoggerFactory.getLogger(PromptOptimization.class);

    /**
     * 优化前后的 Prompt 对比
     */
    public static class PromptComparison {
        private final String before;
        private final String after;
        private final long beforeTokens;
        private final long afterTokens;
        private final double improvement;

        public PromptComparison(String before, String after, 
                             long beforeTokens, long afterTokens) {
            this.before = before;
            this.after = after;
            this.beforeTokens = beforeTokens;
            this.afterTokens = afterTokens;
            this.improvement = ((double) (beforeTokens - afterTokens) / beforeTokens) * 100;
        }

        public String getBefore() { return before; }
        public String getAfter() { return after; }
        public long getBeforeTokens() { return beforeTokens; }
        public long getAfterTokens() { return afterTokens; }
        public double getImprovement() { return improvement; }
    }

    /**
     * 未优化的 Prompt（冗长、模糊）
     */
    public static String getUnoptimizedPrompt(String question) {
        return String.format(
            "请你作为一名专业的 AI 助手，" +
            "我需要你帮我回答以下问题。" +
            "问题内容是：%s" +
            "请你详细地分析这个问题，" +
            "并且给出一个全面、准确、有帮助的答案。" +
            "如果你不确定答案，请直接告诉我。" +
            "请注意你的回答应该尽量详细，" +
            "同时也要保持逻辑清晰和条理分明。" +
            "谢谢你的帮助！",
            question
        );
    }

    /**
     * 优化后的 Prompt（简洁、明确）
     */
    public static String getOptimizedPrompt(String question) {
        return String.format(
            "Answer: %s\n" +
            "Be concise, accurate, and clear.\n" +
            "If uncertain, state it directly.",
            question
        );
    }

    /**
     * 使用角色模板优化
     */
    public static String getRoleBasedPrompt(String role, String task) {
        return String.format(
            "Role: %s\n" +
            "Task: %s\n" +
            "Provide a brief, accurate response.",
            role,
            task
        );
    }

    /**
     * 使用示例模板优化
     */
    public static String getExampleBasedPrompt(String[] examples, String input) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Examples:\n");
        for (String example : examples) {
            prompt.append("Q: ").append(example).append("\n");
            prompt.append("A: [Answer]\n\n");
        }
        
        prompt.append("Q: ").append(input).append("\n");
        prompt.append("A:");
        
        return prompt.toString();
    }

    /**
     * 优化 Prompt
     */
    public static PromptComparison optimizePrompt(String originalPrompt, String question) {
        String optimizedPrompt = getOptimizedPrompt(question);

        // 估算 Token 数（近似值：中文字符 * 2，英文单词 * 1）
        long beforeTokens = originalPrompt.length() + question.length() * 2;
        long afterTokens = optimizedPrompt.length() + question.length() * 2;

        return new PromptComparison(
            originalPrompt,
            optimizedPrompt,
            beforeTokens,
            afterTokens
        );
    }

    /**
     * 系统提示词优化
     */
    @AiService
    public interface OptimizedAssistant {
        String chat(@UserMessage String message);
    }

    /**
     * 创建优化的 AI 服务
     */
    public static OptimizedAssistant createOptimizedService(ChatModel model) {
        return AiServices.builder(OptimizedAssistant.class)
                .chatModel(model)
                .systemMessageProvider(chatMemoryId -> 
                    "You are a concise, helpful AI assistant." +
                    "Answer directly and accurately." +
                    "Be brief - 1-2 sentences when possible." +
                    "If uncertain, say so."
                )
                .build();
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("OPENAI_API_KEY");

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("未设置 OPENAI_API_KEY 环境变量");
            return;
        }

        // 创建模型
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .build();

        // 未优化的 Prompt
        String question = "Java 和 Python 的主要区别是什么？";
        String unoptimizedPrompt = getUnoptimizedPrompt(question);

        // 优化后的 Prompt
        String optimizedPrompt = getOptimizedPrompt(question);

        // 对比优化效果
        PromptComparison comparison = optimizePrompt(unoptimizedPrompt, question);

        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║              Prompt 优化对比                         ║");
        System.out.println("╠═══════════════════════════════════════════════════════════╣");
        System.out.println("║ 优化前                                                  ║");
        System.out.println("╠═══════════════════════════════════════════════════════════╣");
        System.out.println("║ Prompt:                                                  ║");
        System.out.println("║ " + unoptimizedPrompt.substring(0, Math.min(70, unoptimizedPrompt.length())));
        System.out.println("║ ...                                                      ║");
        System.out.println("╠─────────────────────────────────────────────────────────────╣");
        System.out.println("║ Token 数: " + comparison.getBeforeTokens() + "                    ║");
        System.out.println("╠═══════════════════════════════════════════════════════════╣");
        System.out.println("║ 优化后                                                  ║");
        System.out.println("╠═══════════════════════════════════════════════════════════╣");
        System.out.println("║ Prompt: " + optimizedPrompt);
        System.out.println("╠─────────────────────────────────────────────────────────────╣");
        System.out.println("║ Token 数: " + comparison.getAfterTokens() + "                    ║");
        System.out.println("╠─────────────────────────────────────────────────────────────╣");
        System.out.println("║ 优化效果:                                                ║");
        System.out.println("║ 节省 Token: " + (comparison.getBeforeTokens() - comparison.getAfterTokens()) + "                    ║");
        System.out.println("║ 改善幅度: " + String.format("%.1f%%", comparison.getImprovement()) + "                     ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
    }
}
```

## Token 使用优化

### 输出长度控制

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Token 优化
 */
public class TokenOptimization {

    private static final Logger logger = LoggerFactory.getLogger(TokenOptimization.class);

    /**
     * Token 使用统计
     */
    public static class TokenUsage {
        private final String prompt;
        private final int promptTokens;
        private final String response;
        private final int responseTokens;
        private final int totalTokens;

        public TokenUsage(String prompt, int promptTokens, 
                          String response, int responseTokens) {
            this.prompt = prompt;
            this.promptTokens = promptTokens;
            this.response = response;
            this.responseTokens = responseTokens;
            this.totalTokens = promptTokens + responseTokens;
        }

        public String getPrompt() { return prompt; }
        public int getPromptTokens() { return promptTokens; }
        public String getResponse() { return response; }
        public int getResponseTokens() { return responseTokens; }
        public int getTotalTokens() { return totalTokens; }
    }

    /**
     * Token 优化配置
     */
    public static class TokenOptimizationConfig {
        private final int maxResponseTokens;
        private final int targetResponseTokens;
        private final String brevityInstruction;
        private final boolean enableStreaming;
        private final int streamChunkSize;

        public TokenOptimizationConfig(int maxResponseTokens, int targetResponseTokens,
                                       String brevityInstruction, boolean enableStreaming,
                                       int streamChunkSize) {
            this.maxResponseTokens = maxResponseTokens;
            this.targetResponseTokens = targetResponseTokens;
            this.brevityInstruction = brevityInstruction;
            this.enableStreaming = enableStreaming;
            this.streamChunkSize = streamChunkSize;
        }

        public int getMaxResponseTokens() { return maxResponseTokens; }
        public int getTargetResponseTokens() { return targetResponseTokens; }
        public String getBrevityInstruction() { return brevityInstruction; }
        public boolean isEnableStreaming() { return enableStreaming; }
        public int getStreamChunkSize() { return streamChunkSize; }
    }

    /**
     * 创建默认 Token 优化配置
     */
    public static TokenOptimizationConfig createDefaultConfig() {
        return new TokenOptimizationConfig(
                500,       // 最大 500 Token
                300,       // 目标 300 Token
                "Be very brief - use under 300 tokens",
                true,       // 启用流式
                20         // 每次流式 20 Token
        );
    }

    /**
     * 创建精简模式配置
     */
    public static TokenOptimizationConfig createConciseConfig() {
        return new TokenOptimizationConfig(
                200,       // 最大 200 Token
                100,       // 目标 100 Token
                "Use 1-2 short sentences (under 100 tokens)",
                true,
                10
        );
    }

    /**
     * 优化的 AI 服务
     */
    @AiService
    public interface TokenOptimizedAssistant {
        String chat(@UserMessage String message);
    }

    /**
     * 创建 Token 优化的服务
     */
    public static TokenOptimizedAssistant createTokenOptimizedService(
            ChatModel model,
            TokenOptimizationConfig config
    ) {
        return AiServices.builder(TokenOptimizedAssistant.class)
                .chatModel(model)
                .systemMessageProvider(chatMemoryId -> 
                    String.format(
                        "You are a concise AI assistant." +
                        "Limit your response to ~%d tokens (%d words)." +
                        "Brevity: %s" +
                        "Answer directly, skip pleasantries.",
                        config.getTargetResponseTokens(),
                        config.getTargetResponseTokens() * 4 / 3,
                        config.getBrevityInstruction()
                    )
                )
                .build();
    }

    /**
     * 估算 Token 数（简单估算：中文字符 * 2，英文单词 * 1）
     */
    public static int estimateTokens(String text) {
        // 简化估算
        int chineseChars = (int) text.chars()
                .filter(ch -> Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.BASIC_LATIN)
                .filter(Character::isIdeographic)
                .count();
        int otherChars = text.length() - chineseChars;

        return chineseChars * 2 + otherChars / 3;
    }

    /**
     * 优化后的聊天
     */
    public static TokenUsage optimizedChat(
            TokenOptimizedAssistant service,
            String message,
            TokenOptimizationConfig config
    ) {
        long startTime = System.currentTimeMillis();

        // 估算输入 Token
        int promptTokens = estimateTokens(message);

        // 发送消息
        String response = service.chat(message);

        // 估算输出 Token
        int responseTokens = estimateTokens(response);

        long duration = System.currentTimeMillis() - startTime;

        TokenUsage usage = new TokenUsage(
            message,
            promptTokens,
            response,
            responseTokens
        );

        logger.info("Token 使用 - 输入: {}, 输出: {}, 总计: {}, 耗时: {}ms",
                    usage.getPromptTokens(),
                    usage.getResponseTokens(),
                    usage.getTotalTokens(),
                    duration);

        return usage;
    }

    /**
     * Token 优化建议
     */
    public static class TokenOptimizationSuggestions {
        private final String prompt;
        private final String issue;
        private final String suggestion;
        private final int potentialSavings;

        public TokenOptimizationSuggestions(String prompt, String issue, 
                                           String suggestion, int potentialSavings) {
            this.prompt = prompt;
            this.issue = issue;
            this.suggestion = suggestion;
            this.potentialSavings = potentialSavings;
        }

        public String getPrompt() { return prompt; }
        public String getIssue() { return issue; }
        public String getSuggestion() { return suggestion; }
        public int getPotentialSavings() { return potentialSavings; }
    }

    /**
     * 分析 Prompt 并给出优化建议
     */
    public static List<TokenOptimizationSuggestions> analyzeAndOptimizePrompts(
            List<String> prompts
    ) {
        List<TokenOptimizationSuggestions> suggestions = new ArrayList<>();

        for (String prompt : prompts) {
            int estimatedTokens = estimateTokens(prompt);

            if (estimatedTokens > 500) {
                suggestions.add(new TokenOptimizationSuggestions(
                    prompt.substring(0, Math.min(50, prompt.length())) + "...",
                    "Prompt 过长",
                    "压缩 Prompt，移除冗余信息，使用更简洁的表达",
                    estimatedTokens / 3
                ));
            } else if (prompt.length() < 10) {
                suggestions.add(new TokenOptimizationSuggestions(
                    prompt,
                    "Prompt 可能过于简短",
                    "确保 Prompt 包含足够上下文信息",
                    0
                ));
            } else if (prompt.contains("详细") || prompt.contains("全面")) {
                suggestions.add(new TokenOptimizationSuggestions(
                    prompt.substring(0, Math.min(50, prompt.length())),
                    "使用了详细的修饰词",
                    "移除修饰词，直接表达需求",
                    estimatedTokens / 5
                ));
            }
        }

        return suggestions;
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("OPENAI_API_KEY");

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("未设置 OPENAI_API_KEY 环境变量");
            return;
        }

        // 创建模型
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .build();

        // 创建优化配置
        TokenOptimizationConfig config = createDefaultConfig();

        // 创建优化服务
        TokenOptimizedAssistant service = createTokenOptimizedService(model, config);

        // 测试查询
        List<String> testQueries = List.of(
            "请解释一下机器学习的基本概念",
            "Java 8 和 Java 17 的新特性分别是什么？",
            "编写一个快速排序算法"
        );

        // 分析并优化 Prompts
        List<TokenOptimizationSuggestions> suggestions = analyzeAndOptimizePrompts(testQueries);

        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║              Prompt 分析和优化                       ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ 分析结果:                                                ║");
        System.out.println("║                                                         ║");
        System.out.println("║ " + testQueries.size() + " 个 Prompts 被分析，发现 " + suggestions.size() + " 个可优化点");
        System.out.println("║                                                         ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ 优化建议:                                                ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════════╣");

        for (int i = 0; i < Math.min(5, suggestions.size()); i++) {
            TokenOptimizationSuggestions suggestion = suggestions.get(i);

            System.out.printf("║ [%d] Prompt: %-45s           ║\n",
                        i + 1, suggestion.getPrompt());
            System.out.printf("║     问题: %s                                        ║\n",
                        suggestion.getIssue());
            System.out.printf("║     建议: %s                              ║\n",
                        suggestion.getSuggestion());
            System.out.printf("║     潜在节省: %d tokens                              ║\n",
                        suggestion.getPotentialSavings());
            System.out.printf("║     估算改善: %.1f%%                                   ║\n",
                        (double) suggestion.getPotentialSavings() / estimateTokens(testQueries.get(i % testQueries.size())) * 100);
            System.out.printf("╠─────────────────────────────────────────────────────╣\n");
        }

        System.out.println("╠═══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ Token 优化配置:                                          ║");
        System.out.printf("║ 最大响应 Token: %d                                    ║\n", config.getMaxResponseTokens());
        System.out.printf("║ 目标响应 Token: %d                                    ║\n", config.getTargetResponseTokens());
        System.out.printf("║ 简洁指令: %s                        ║\n", config.getBrevityInstruction());
        System.out.printf("║ 启用流式: %s                                           ║\n", config.isEnableStreaming());
        System.out.printf("║ 流式块大小: %d                                          ║\n", config.getStreamChunkSize());
        System.out.println("╚═══════════════════════════════════════════════════════════════════════════╝");
    }
}
```

## 并发处理优化

### 异步并发调用

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.model.chat.ChatModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 并发处理优化
 */
public class ConcurrentProcessingOptimization {

    private static final Logger logger = LoggerFactory.getLogger(ConcurrentProcessingOptimization.class);

    /**
     * 并发配置
     */
    public static class ConcurrentConfig {
        private final int maxConcurrentRequests;
        private final int maxRetries;
        private final Duration timeout;
        private final boolean enableAsync;

        public ConcurrentConfig(int maxConcurrentRequests, int maxRetries, 
                             Duration timeout, boolean enableAsync) {
            this.maxConcurrentRequests = maxConcurrentRequests;
            this.maxRetries = maxRetries;
            this.timeout = timeout;
            this.enableAsync = enableAsync;
        }

        public int getMaxConcurrentRequests() { return maxConcurrentRequests; }
        public int getMaxRetries() { return maxRetries; }
        public Duration getTimeout() { return timeout; }
        public boolean isEnableAsync() { return enableAsync; }
    }

    /**
     * 创建默认并发配置
     */
    public static ConcurrentConfig createDefaultConfig() {
        return new ConcurrentConfig(
                10,                      // 最多 10 个并发请求
                2,                       // 最多重试 2 次
                Duration.ofSeconds(30),  // 30 秒超时
                true                     // 启用异步
        );
    }

    /**
     * 创建高性能并发配置
     */
    public static ConcurrentConfig createHighPerformanceConfig() {
        return new ConcurrentConfig(
                50,                      // 50 个并发请求
                2,
                Duration.ofSeconds(15),  // 15 秒超时
                true
        );
    }

    /**
     * 异步调用服务
     */
    public static class AsyncChatService {
        private final ChatModel model;
        private final ConcurrentConfig config;

        public AsyncChatService(ChatModel model, ConcurrentConfig config) {
            this.model = model;
            this.config = config;
        }

        /**
         * 并发处理单个消息（异步）
         */
        public CompletableFuture<String> chatAsync(String message) {
            return CompletableFuture.supplyAsync(() -> {
                return model.chat(message);
            });
        }

        /**
         * 批量异步聊天
         */
        public CompletableFuture<List<String>> batchChatAsync(List<String> messages) {
            // 创建线程池
            ExecutorService executor = Executors.newFixedThreadPool(config.getMaxConcurrentRequests());

            try {
                // 创建异步任务列表
                List<CompletableFuture<String>> futures = messages.stream()
                        .map(message -> chatAsync(message))
                        .collect(Collectors.toList());

                // 等待所有任务完成
                CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
                );

                return allFutures.thenApply(v -> 
                    futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList())
                ).get(config.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                logger.error("批量聊天失败", e);
                return CompletableFuture.failedFuture(e);
            } finally {
                executor.shutdown();
            }
        }

        /**
         * 并发调用多个服务
         */
        public CompletableFuture<List<String>> parallelChat(
                List<String> messages,
                ChatModel... models
        ) {
            if (models.length == 0) {
                return CompletableFuture.failedFuture(
                    new IllegalArgumentException("至少需要一个模型")
                );
            }

            ExecutorService executor = Executors.newFixedThreadPool(config.getMaxConcurrentRequests());

            try {
                List<CompletableFuture<String>> futures = new ArrayList<>();

                for (String message : messages) {
                    ChatModel model = models[(int) (Math.random() * models.length)];
                    
                    CompletableFuture<String> future = CompletableFuture.supplyAsync(
                        () -> model.chat(message),
                        executor
                    );
                    futures.add(future);
                }

                CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
                );

                return allFutures.thenApply(v ->
                    futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList())
                ).get(config.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                logger.error("并行聊天失败", e);
                return CompletableFuture.failedFuture(e);
            } finally {
                executor.shutdown();
            }
        }
    }

    /**
     * 同步调用服务（对比基准）
     */
    public static class SyncChatService {
        private final ChatModel model;

        public SyncChatService(ChatModel model) {
            this.model = model;
        }

        /**
         * 批量同步聊天
         */
        public List<String> batchChat(List<String> messages) {
            List<String> responses = new ArrayList<>();

            for (String message : messages) {
                responses.add(model.chat(message));
            }

            return responses;
        }
    }

    /**
     * 性能测试
     */
    public static class PerformanceBenchmark {
        private final String testMessages;
        private final int iterations;

        public PerformanceBenchmark(String testMessages, int iterations) {
            this.testMessages = testMessages;
            this.iterations = iterations;
        }

        /**
         * 运行基准测试
         */
        public BenchmarkResult runBenchmark(ChatModel model) {
            List<String> messages = List.of(testMessages.split("\n"));

            // 测试同步处理
            long syncTime = benchmarkSync(model, messages);

            // 测试异步处理
            AsyncChatService asyncService = new AsyncChatService(
                model, 
                ConcurrentConfig.createHighPerformanceConfig()
            );
            long asyncTime = benchmarkAsync(asyncService, messages);

            // 计算加速比
            double speedup = (double) syncTime / asyncTime;

            return new BenchmarkResult(
                messages.size() * iterations,
                syncTime,
                asyncTime,
                speedup
            );
        }

        /**
         * 同步基准
         */
        private long benchmarkSync(ChatModel model, List<String> messages) {
            long startTime = System.currentTimeMillis();

            SyncChatService syncService = new SyncChatService(model);

            for (int i = 0; i < iterations; i++) {
                syncService.batchChat(messages);
            }

            return System.currentTimeMillis() - startTime;
        }

        /**
         * 异步基准
         */
        private long benchmarkAsync(AsyncChatService service, List<String> messages) {
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < iterations; i++) {
                try {
                    service.batchChatAsync(messages).get();
                } catch (Exception e) {
                    logger.error("异步基准测试失败", e);
                }
            }

            return System.currentTimeMillis() - startTime;
        }

        /**
         * 基准结果
         */
        public static class BenchmarkResult {
            private final int totalRequests;
            private final long syncTimeMs;
            private final long asyncTimeMs;
            private final double speedup;
            private final String model;

            public BenchmarkResult(int totalRequests, long syncTimeMs, 
                               long asyncTimeMs, double speedup) {
                this.totalRequests = totalRequests;
                this.syncTimeMs = syncTimeMs;
                this.asyncTimeMs = asyncTimeMs;
                this.speedup = speedup;
                this.model = model;
            }

            public int getTotalRequests() { return totalRequests; }
            public long getSyncTimeMs() { return syncTimeMs; }
            public long getAsyncTimeMs() { return asyncTimeMs; }
            public double getSpeedup() { return speedup; }

            @Override
            public String toString() {
                return String.format(
                        "BenchmarkResult{totalRequests=%d, syncTime=%dms, asyncTime=%dms, speedup=%.2fx}",
                        totalRequests, syncTimeMs, asyncTimeMs, speedup
                );
            }
        }
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("OPENAI_API_KEY");

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("未设置 OPENAI_API_KEY 环境变量");
            return;
        }

        // 创建模型
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .timeout(Duration.ofSeconds(30))
                .build();

        // 运行基准测试
        PerformanceBenchmark benchmark = new PerformanceBenchmark(
                "请介绍一下你自己\n" +
                "什么是 Java？\n" +
                "解释一下机器学习\n" +
                "编写一个简单的算法",
                5  // 5 次迭代
        );

        BenchmarkResult result = benchmark.runBenchmark(model);

        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║              并发处理基准测试                       ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ 总请求数: " + result.getTotalRequests() + "                                  ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ 同步处理时间: %6d ms (%6.2f 秒)              ║\n",
                    result.getSyncTimeMs(), result.getSyncTimeMs() / 1000.0);
        System.out.printf("║ 异步处理时间: %6d ms (%6.2f 秒)              ║\n",
                    result.getAsyncTimeMs(), result.getAsyncTimeMs() / 1000.0);
        System.out.printf("║ 加速比: %.2fx                                       ║\n",
                    result.getSpeedup());
        System.out.printf("║ 时间节省: %6d ms (%6.2f 秒, %.1f%%)     ║\n",
                    result.getSyncTimeMs() - result.getAsyncTimeMs(),
                    (result.getSyncTimeMs() - result.getAsyncTimeMs()) / 1000.0,
                    ((double) (result.getSyncTimeMs() - result.getAsyncTimeMs()) / result.getSyncTimeMs()) * 100);
        System.out.printf("║ 吞吐量提升: %.1fx                                       ║\n",
                    result.getSpeedup());
        System.out.println("╠═══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ 并发优化建议:                                           ║");
        System.out.println("║ 1. 使用异步处理可以显著提升吞吐量                        ║");
        System.out.println("║ 2. 线程池大小应根据机器配置调整                         ║");
        System.out.println("║ 3. 使用流式输出可以改善用户体验                             ║");
        System.out.println("║ 4. 批量处理多个请求比单个请求更高效                       ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════════╝");
    }
}
```

## 缓存优化

### 响应缓存策略

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 缓存优化
 */
public class CacheOptimization {

    private static final Logger logger = LoggerFactory.getLogger(CacheOptimization.class);

    /**
     * 缓存配置
     */
    public static class CacheConfig {
        private final int maxSize;
        private final long expireAfterWriteMs;
        private final boolean enableCache;
        private final CacheStrategy strategy;

        public CacheConfig(int maxSize, long expireAfterWriteMs, 
                          boolean enableCache, CacheStrategy strategy) {
            this.maxSize = maxSize;
            this.expireAfterWriteMs = expireAfterWriteMs;
            this.enableCache = enableCache;
            this.strategy = strategy;
        }

        public int getMaxSize() { return maxSize; }
        public long getExpireAfterWriteMs() { return expireAfterWriteMs; }
        public boolean isEnableCache() { return enableCache; }
        public CacheStrategy getStrategy() { return strategy; }
    }

    /**
     * 缓存策略
     */
    public enum CacheStrategy {
        LRU("最近最少使用"),
        LFU("最不常用"),
        TTL("时间过期"),
        ADAPTIVE("自适应");

        private final String description;

        CacheStrategy(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }

    /**
     * 创建默认缓存配置
     */
    public static CacheConfig createDefaultConfig() {
        return new CacheConfig(
                1000,             // 最多缓存 1000 个响应
                Duration.ofMinutes(5).toMillis(),  // 5 分钟过期
                true,             // 启用缓存
                CacheStrategy.LRU
        );
    }

    /**
     * 创建高性能缓存配置
     */
    public static CacheConfig createHighPerformanceConfig() {
        return new CacheConfig(
                10000,            // 最多缓存 10000 个响应
                Duration.ofHours(1).toMillis(),   // 1 小时过期
                true,
                CacheStrategy.LFU
        );
    }

    /**
     * 缓存条目
     */
    public static class CacheEntry {
        private final String key;
        private final String response;
        private final long createTime;
        private volatile long lastAccessTime;
        private volatile int accessCount;

        public CacheEntry(String key, String response) {
            this.key = key;
            this.response = response;
            this.createTime = System.currentTimeMillis();
            this.lastAccessTime = createTime;
            this.accessCount = 1;
        }

        public void access() {
            this.lastAccessTime = System.currentTimeMillis();
            this.accessCount++;
        }

        // Getters
        public String getKey() { return key; }
        public String getResponse() { return response; }
        public long getCreateTime() { return createTime; }
        public long getLastAccessTime() { return lastAccessTime; }
        public int getAccessCount() { return accessCount; }

        public boolean isExpired(long maxAgeMs) {
            return System.currentTimeMillis() - createTime > maxAgeMs;
        }
    }

    /**
     * 响应缓存
     */
    public static class ResponseCache {
        private final Map<String, CacheEntry> cache;
        private final CacheConfig config;
        private final int maxSize;

        public ResponseCache(CacheConfig config) {
            this.config = config;
            this.maxSize = config.getMaxSize();
            this.cache = new ConcurrentHashMap<>();

            // 启动清理线程
            if (config.isEnableCache()) {
                startEvictionThread();
            }
        }

        /**
         * 获取缓存响应
         */
        public String get(String key) {
            if (!config.isEnableCache()) {
                return null;
            }

            CacheEntry entry = cache.get(key);
            if (entry != null) {
                if (entry.isExpired(config.getExpireAfterWriteMs())) {
                    cache.remove(key);
                    logger.debug("缓存已过期: {}", key);
                    return null;
                }

                entry.access();
                logger.debug("缓存命中: {}", key);
                return entry.getResponse();
            }

            logger.debug("缓存未命中: {}", key);
            return null;
        }

        /**
         * 存储响应到缓存
         */
        public void put(String key, String response) {
            if (!config.isEnableCache()) {
                return;
            }

            // 检查是否需要淘汰
            if (cache.size() >= maxSize) {
                evictEntries();
            }

            CacheEntry entry = new CacheEntry(key, response);
            cache.put(key, entry);

            logger.debug("缓存响应: {}, 当前大小: {}", key, cache.size());
        }

        /**
         * 淘汰条目
         */
        private void evictEntries() {
            switch (config.getStrategy()) {
                case LRU:
                    evictLRU();
                    break;
                case LFU:
                    evictLFU();
                    break;
                case TTL:
                    evictExpired();
                    break;
                default:
                    evictLRU();
            }
        }

        /**
         * LRU 淘汰
         */
        private void evictLRU() {
            cache.entrySet().stream()
                    .min(Map.Entry.comparingByValue(e -> e.getLastAccessTime()))
                    .ifPresent(entry -> cache.remove(entry.getKey()));

            logger.debug("LRU 淘汰后大小: {}", cache.size());
        }

        /**
         * LFU 淘汰
         */
        private void evictLFU() {
            cache.entrySet().stream()
                    .min(Map.Entry.comparingByValue(e -> e.getAccessCount()))
                    .ifPresent(entry -> cache.remove(entry.getKey()));

            logger.debug("LFU 淘汰后大小: {}", cache.size());
        }

        /**
         * 过期淘汰
         */
        private void evictExpired() {
            cache.entrySet().removeIf(entry -> 
                entry.getValue().isExpired(config.getExpireAfterWriteMs())
            );

            logger.debug("过期淘汰后大小: {}", cache.size());
        }

        /**
         * 启动淘汰线程
         */
        private void startEvictionThread() {
            Thread thread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(60000); // 每分钟清理一次
                        evictEntries();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });

            thread.setName("CacheEvictionThread");
            thread.setDaemon(true);
            thread.start();

            logger.info("启动缓存淘汰线程");
        }

        /**
         * 清除缓存
         */
        public void clear() {
            cache.clear();
            logger.info("缓存已清除");
        }

        /**
         * 获取缓存统计
         */
        public CacheStats getStats() {
            int totalEntries = cache.size();
            int expiredEntries = (int) cache.values().stream()
                    .filter(entry -> entry.isExpired(config.getExpireAfterWriteMs()))
                    .count();
            long totalAccesses = cache.values().stream()
                    .mapToInt(CacheEntry::getAccessCount)
                    .sum();

            return new CacheStats(
                totalEntries,
                expiredEntries,
                totalAccesses,
                config.getMaxSize()
            );
        }

        /**
         * 缓存统计
         */
        public static class CacheStats {
            private final int totalEntries;
            private final int expiredEntries;
            private final long totalAccesses;
            private final int maxSize;

            public CacheStats(int totalEntries, int expiredEntries, 
                            long totalAccesses, int maxSize) {
                this.totalEntries = totalEntries;
                this.expiredEntries = expiredEntries;
                this.totalAccesses = totalAccesses;
                this.maxSize = maxSize;
            }

            public int getTotalEntries() { return totalEntries; }
            public int getExpiredEntries() { return expiredEntries; }
            public long getTotalAccesses() { return totalAccesses; }
            public int getMaxSize() { return maxSize; }

            public double getHitRate() {
                return totalAccesses > 0 ? 
                       (double) (totalAccesses - expiredEntries) / totalAccesses : 0;
            }

            @Override
            public String toString() {
                return String.format(
                        "CacheStats{entries=%d/%d, expired=%d, hits=%d, hitRate=%.2f}",
                        totalEntries, maxSize, expiredEntries,
                        totalAccesses - expiredEntries,
                        getHitRate()
                );
            }
        }
    }

    /**
     * 带缓存的 AI 服务
     */
    @AiService
    public interface CachedAssistant {
        String chat(@UserMessage String message);
    }

    /**
     * 创建带缓存的 AI 服务
     */
    public static CachedAssistant createCachedAssistant(
            ChatModel model,
            ResponseCache cache
    ) {
        return AiServices.builder(CachedAssistant.class)
                .chatModel(model)
                .systemMessageProvider(chatMemoryId -> 
                    "You are a helpful AI assistant." +
                    "Provide accurate, concise responses."
                )
                .build();
    }

    /**
     * 带缓存的聊天
     */
    public static String cachedChat(
            CachedAssistant service,
            ResponseCache cache,
            String message
    ) {
        // 尝试从缓存获取
        String cachedResponse = cache.get(message);
        if (cachedResponse != null) {
            logger.info("使用缓存响应");
            return cachedResponse;
        }

        // 生成新响应
        String response = service.chat(message);

        // 存储到缓存
        cache.put(message, response);

        return response;
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("OPENAI_API_KEY");

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("未设置 OPENAI_API_KEY 环境变量");
            return;
        }

        // 创建模型
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .build();

        // 创建缓存
        ResponseCache cache = new ResponseCache(CacheConfig.createDefaultConfig());

        // 创建带缓存的服务
        CachedAssistant service = createCachedAssistant(model, cache);

        // 测试缓存
        String testMessage = "请介绍一下你自己";
        
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║              缓存优化测试                               ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ 测试消息: " + testMessage);
        System.out.println("╠═══════════════════════════════════════════════════════════════════╣");

        // 第一次调用（未命中缓存）
        long startTime = System.currentTimeMillis();
        String response1 = cachedChat(service, cache, testMessage);
        long duration1 = System.currentTimeMillis() - startTime;

        System.out.printf("║ 第1次调用: %4d ms - %s                    ║\n",
                    duration1, response1.substring(0, Math.min(60, response1.length())));
        System.out.println("╠═══════════════════════════════════════════════════════════════════╣");

        // 第二次调用（应该命中缓存）
        startTime = System.currentTimeMillis();
        String response2 = cachedChat(service, cache, testMessage);
        long duration2 = System.currentTimeMillis() - startTime;

        System.out.printf("║ 第2次调用: %4d ms - %s                    ║\n",
                    duration2, response2.substring(0, Math.min(60, response2.length())));
        System.out.println("╠═══════════════════════════════════════════════════════════════════╣");

        // 第三次调用（应该命中缓存）
        startTime = System.currentTimeMillis();
        String response3 = cachedChat(service, cache, testMessage);
        long duration3 = System.currentTimeMillis() - startTime;

        System.out.printf("║ 第3次调用: %4d ms - %s                    ║\n",
                    duration3, response3.substring(0, Math.min(60, response3.length())));
        System.out.println("╠═══════════════════════════════════════════════════════════════════╣");

        // 显示缓存统计
        ResponseCache.CacheStats stats = cache.getStats();

        System.out.println("║ 缓存统计:                                                ║");
        System.out.printf("║ 缓存条目: %d/%d                                       ║\n",
                    stats.getTotalEntries(), stats.getMaxSize());
        System.out.printf("║ 总访问次数: %d                                         ║\n",
                    stats.getTotalAccesses());
        System.out.printf("║ 命中率: %.2f%%                                          ║\n",
                    stats.getHitRate() * 100);

        // 计算时间节省
        long firstTime = duration1;
        long cachedTime = (duration2 + duration3) / 2;
        double timeSaved = ((double) (firstTime - cachedTime) / firstTime) * 100;

        System.out.printf("║ 时间节省: %.1f%% (%d ms)                              ║\n",
                    timeSaved, firstTime - cachedTime);
        System.out.println("╚═════════════════════════════════════════════════════════════════════════════════════════╝");
    }
}
```

## 综合优化示例

### 完整的优化服务

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 完整的优化服务
 */
public class CompleteOptimizationService {

    private static final Logger logger = LoggerFactory.getLogger(CompleteOptimizationService.class);

    private final ChatModel model;
    private final ResponseCache cache;
    private final TokenOptimizationConfig tokenConfig;
    private final ConcurrentConfig concurrentConfig;

    /**
     * 创建优化的服务
     */
    public static CompleteOptimizationService createOptimizedService(String apiKey) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .timeout(Duration.ofSeconds(30))
                .build();

        ResponseCache cache = new ResponseCache(CacheConfig.createDefaultConfig());

        TokenOptimizationConfig tokenConfig = TokenOptimizationConfig.createDefaultConfig();

        ConcurrentConfig concurrentConfig = ConcurrentConfig.createHighPerformanceConfig();

        return new CompleteOptimizationService(model, cache, tokenConfig, concurrentConfig);
    }

    /**
     * 构造函数
     */
    private CompleteOptimizationService(ChatModel model,
                                    ResponseCache cache,
                                    TokenOptimizationConfig tokenConfig,
                                    ConcurrentConfig concurrentConfig) {
        this.model = model;
        this.cache = cache;
        this.tokenConfig = tokenConfig;
        this.concurrentConfig = concurrentConfig;
    }

    /**
     * 批量处理消息
     */
    public List<OptimizedResult> processBatch(List<String> messages) {
        long startTime = System.currentTimeMillis();

        // 创建并发服务
        AsyncChatService asyncService = new AsyncChatService(
            model,
            concurrentConfig
        );

        // 创建 Token 优化的服务
        TokenOptimizedAssistant tokenOptimizedService = 
            TokenOptimization.createTokenOptimizedService(model, tokenConfig);

        // 处理结果
        List<OptimizedResult> results = new ArrayList<>();

        try {
            // 异步处理
            CompletableFuture<List<String>> future = asyncService.batchChatAsync(messages);
            List<String> responses = future.get(concurrentConfig.getTimeout().toMillis(), 
                                                 java.util.concurrent.TimeUnit.MILLISECONDS);

            long asyncTime = System.currentTimeMillis() - startTime;

            // 分析每个结果
            for (int i = 0; i < messages.size(); i++) {
                String message = messages.get(i);
                String response = responses.get(i);

                int promptTokens = TokenOptimization.estimateTokens(message);
                int responseTokens = TokenOptimization.estimateTokens(response);

                results.add(new OptimizedResult(
                    message,
                    response,
                    promptTokens,
                    responseTokens,
                    promptTokens + responseTokens,
                    asyncTime / messages.size()
                ));
            }

        } catch (Exception e) {
            logger.error("批量处理失败", e);
        }

        return results;
    }

    /**
     * 智能处理（带缓存）
     */
    public OptimizedResult smartProcess(String message) {
        // 尝试从缓存获取
        String cachedResponse = cache.get(message);
        if (cachedResponse != null) {
            return new OptimizedResult(
                message,
                cachedResponse,
                TokenOptimization.estimateTokens(message),
                TokenOptimization.estimateTokens(cachedResponse),
                TokenOptimization.estimateTokens(message) + TokenOptimization.estimateTokens(cachedResponse),
                10  // 假设缓存响应很快
            );
        }

        // 生成新响应
        long startTime = System.currentTimeMillis();
        String response = model.chat(message);
        long duration = System.currentTimeMillis() - startTime;

        // 存储到缓存
        cache.put(message, response);

        return new OptimizedResult(
            message,
            response,
            TokenOptimization.estimateTokens(message),
            TokenOptimization.estimateTokens(response),
            TokenOptimization.estimateTokens(message) + TokenOptimization.estimateTokens(response),
            duration
        );
    }

    /**
     * 优化结果
     */
    public static class OptimizedResult {
        private final String message;
        private final String response;
        private final int promptTokens;
        private final int responseTokens;
        private final int totalTokens;
        private final long durationMs;

        public OptimizedResult(String message, String response, 
                             int promptTokens, int responseTokens, 
                             int totalTokens, long durationMs) {
            this.message = message;
            this.response = response;
            this.promptTokens = promptTokens;
            this.responseTokens = responseTokens;
            this.totalTokens = totalTokens;
            this.durationMs = durationMs;
        }

        // Getters
        public String getMessage() { return message; }
        public String getResponse() { return response; }
        public int getPromptTokens() { return promptTokens; }
        public int getResponseTokens() { return responseTokens; }
        public int getTotalTokens() { return totalTokens; }
        public long getDurationMs() { return durationMs; }

        public double getTokensPerMs() {
            return durationMs > 0 ? (double) totalTokens / durationMs * 1000 : 0;
        }

        @Override
        public String toString() {
            return String.format(
                        "OptimizedResult{message='%s...', tokens=%d, duration=%dms}",
                        message.substring(0, Math.min(30, message.length())),
                        totalTokens,
                        durationMs
                    );
        }
    }

    /**
     * 性能报告
     */
    public static class PerformanceReport {
        private final int totalRequests;
        private final int cacheHits;
        private final int totalTokens;
        private final long totalTimeMs;
        private final double avgTokensPerRequest;
        private final double avgTimePerRequest;

        public PerformanceReport(List<OptimizedResult> results) {
            this.totalRequests = results.size();
            this.cacheHits = (int) results.stream()
                    .filter(r -> r.getDurationMs() < 100)  // 假设缓存响应很快
                    .count();
            this.totalTokens = results.stream()
                    .mapToInt(OptimizedResult::getTotalTokens)
                    .sum();
            this.totalTimeMs = results.stream()
                    .mapToLong(OptimizedResult::getDurationMs)
                    .sum();
            this.avgTokensPerRequest = (double) totalTokens / totalRequests;
            this.avgTimePerRequest = (double) totalTimeMs / totalRequests;
        }

        public int getTotalRequests() { return totalRequests; }
        public int getCacheHits() { return cacheHits; }
        public int getTotalTokens() { return totalTokens; }
        public long getTotalTimeMs() { return totalTimeMs; }
        public double getAvgTokensPerRequest() { return avgTokensPerRequest; }
        public double getAvgTimePerRequest() { return avgTimePerRequest; }

        public double getCacheHitRate() {
            return (double) cacheHits / totalRequests;
        }

        public double getTokensPerSecond() {
            return totalTimeMs > 0 ? (double) totalTokens / totalTimeMs * 1000 : 0;
        }
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("OPENAI_API_KEY");

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("未设置 OPENAI_API_KEY 环境变量");
            return;
        }

        // 创建优化服务
        CompleteOptimizationService service = createOptimizedService(apiKey);

        // 测试消息
        List<String> messages = List.of(
            "请介绍一下你自己",
            "什么是 Java？",
            "解释一下机器学习的基本概念",
            "编写一个快速排序算法",
            "Python 和 JavaScript 的主要区别是什么？",
            "什么是面向对象编程？",
            "SQL 和 NoSQL 数据库的区别",
            "什么是微服务架构？"
        );

        // 批量处理
        List<OptimizedResult> results = service.processBatch(messages);

        // 生成性能报告
        PerformanceReport report = new PerformanceReport(results);

        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║              性能优化报告                              ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ 总请求数: %d                                                ║\n", report.getTotalRequests());
        System.out.printf("║ 缓存命中: %d (%.1f%%)                                  ║\n",
                    report.getCacheHits(),
                    report.getCacheHitRate() * 100);
        System.out.printf("║ 总 Token 数: %d                                            ║\n",
                    report.getTotalTokens());
        System.out.printf("║ 平均 Token/请求: %.1f                                        ║\n",
                    report.getAvgTokensPerRequest());
        System.out.printf("║ 总耗时: %d ms (%.2f 秒)                             ║\n",
                    report.getTotalTimeMs(),
                    report.getTotalTimeMs() / 1000.0);
        System.out.printf("║ 平均耗时/请求: %.1f ms                                      ║\n",
                    report.getAvgTimePerRequest());
        System.out.printf("║ Token/秒: %.1f                                           ║\n",
                    report.getTokensPerSecond());
        System.out.println("╠═══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ 优化建议:                                                ║");
        System.out.println("║ 1. 使用缓存可以大幅减少响应时间                              ║");
        System.out.println("║ 2. 优化 Prompt 可以显著节省 Token 费用                      ║");
        System.out.println("║ 3. 并发处理可以提高整体吞吐量                                 ║");
        System.out.println("║ 4. 组合多种优化策略效果最佳                              ║");
        System.out.println("╚═════════════════════════════════════════════════════════════════════════════════════════════════╝");
    }
}
```

## 测试和基准

### 性能测试代码

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 性能优化测试
 */
class PerformanceOptimizationTest {

    @Test
    void should_optimize_prompt_tokens() {
        String originalPrompt = "请你作为一名专业的 AI 助手，" +
                               "我需要你帮我回答以下问题。" +
                               "问题内容是：什么是机器学习？";

        String optimizedPrompt = "Answer: What is machine learning? " +
                                "Be concise and accurate.";

        int originalTokens = originalPrompt.length() * 4 / 3;  // 估算
        int optimizedTokens = optimizedPrompt.length() * 4 / 3;

        assertTrue(optimizedTokens < originalTokens);
        double improvement = ((double) (originalTokens - optimizedTokens) / originalTokens) * 100;
        assertTrue(improvement > 30);  // 至少节省 30% Token
    }

    @Test
    void should_use_cache_effectively() {
        ResponseCache cache = new ResponseCache(CacheConfig.createDefaultConfig());

        String key = "test question";
        String response = "test answer";

        // 第一次调用（未命中）
        String result1 = cache.get(key);
        assertNull(result1);

        cache.put(key, response);

        // 第二次调用（应该命中）
        String result2 = cache.get(key);
        assertEquals(response, result2);

        // 验证缓存统计
        ResponseCache.CacheStats stats = cache.getStats();
        assertEquals(1, stats.getTotalAccesses());
        assertEquals(1.0, stats.getHitRate());
    }

    @Test
    void should_concurrent_process_faster() throws Exception {
        String apiKey = System.getenv("OPENAI_API_KEY");

        ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .timeout(Duration.ofSeconds(30))
                .build();

        List<String> messages = List.of("Hi", "Hello", "你好");

        // 测试同步处理
        long syncStart = System.currentTimeMillis();
        List<String> syncResponses = new ArrayList<>();
        for (String message : messages) {
            syncResponses.add(model.chat(message));
        }
        long syncTime = System.currentTimeMillis() - syncStart;

        // 测试异步处理
        AsyncChatService asyncService = new AsyncChatService(
            model, 
            ConcurrentConfig.createDefaultConfig()
        );

        long asyncStart = System.currentTimeMillis();
        CompletableFuture<List<String>> asyncFuture = asyncService.batchChatAsync(messages);
        List<String> asyncResponses = asyncFuture.get();
        long asyncTime = System.currentTimeMillis() - asyncStart;

        // 异步应该更快（在并发场景下）
        assertTrue(asyncTime <= syncTime * 1.5);  // 允许 50% 容差
    }

    @Test
    void should_achieve_performance_targets() {
        PerformanceReport report = new PerformanceReport(createMockResults());

        // 验证性能指标
        assertTrue(report.getCacheHitRate() > 0.5);  // 至少 50% 缓存命中率
        assertTrue(report.getAvgTimePerRequest() < 5000);  // 平均响应时间小于 5 秒
        assertTrue(report.getTokensPerSecond() > 10);  // Token/秒大于 10
    }

    /**
     * 创建模拟结果
     */
    private static List<OptimizedResult> createMockResults() {
        return List.of(
            new OptimizedResult("Q1", "A1", 10, 50, 60, 100),
            new OptimizedResult("Q2", "A2", 15, 45, 60, 150),
            new OptimizedResult("Q3", "A3", 10, 50, 60, 50)  // 假设是缓存命中
        );
    }
}
```

## 总结

### 本章要点

1. **性能瓶颈**
   - 网络 I/O：10-30%
   - 模型处理：60-80%
   - 应用层：10-20%

2. **优化策略**
   - Prompt 优化：20-40% 节省 Token
   - Token 优化：控制响应长度
   - 并发处理：2-5x 吞吐量提升
   - 缓存策略：50-90% 响应时间节省

3. **最佳实践**
   - 优化 Prompt 以减少 Token 使用
   - 使用并发处理提高吞吐量
   - 实现智能缓存策略
   - 监控性能指标
   - 持续优化和调整

4. **应用场景**
   - 高并发聊天系统
   - 批量内容生成
   - 实时代码补全
   - 大规模文本处理
   - 企业级应用

### 下一步

在下一章节中，我们将学习：
- 安全和认证
- 生产环境部署
- 监控和告警
- 成本优化
- 故障排查

### 常见问题

**Q1：如何平衡性能和质量？**

A：平衡策略：
1. 不要过度牺牲质量换速度
2. 优化 Prompt 而非减少重要信息
3. 使用更快的模型进行预处理
4. 对不同任务使用不同优化策略
5. A/B 测试找到最佳平衡点

**Q2：并发处理会增加 API 调用成本吗？**

A：成本影响：
1. 可能因为更多请求而增加成本
2. 但可以通过 Token 优化抵消
3. 缓存可以减少重复请求
4. 整体成本可能更低（因为响应更快）

**Q3：如何选择合适的缓存策略？**

A：选择依据：
1. 访问模式（均匀还是热点）
2. 数据更新频率
3. 内存限制
4. 命中率要求
5. 过期策略需求

**Q4：如何监控性能？**

A：监控指标：
1. 响应时间分布
2. Token 使用统计
3. 缓存命中率
4. 并发连接数
5. 错误率

**Q5：如何进行压力测试？**

A：测试方法：
1. 使用工具（JMeter、Gatling）
2. 逐步增加负载
3. 监控关键指标
4. 找到性能拐点
5. 优化瓶颈

## 参考资料

- [LangChain4j 性能优化文档](https://docs.langchain4j.dev/tutorials/performance-optimization)
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
