---
title: '流式输出详解'
description: '学习 LangChain4j 的 流式输出详解 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 12 - 流式输出详解

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 理解流式输出的概念和优势
- 掌握 `StreamingChatModel` 的使用方法
- 学会使用 `TokenStream` 处理 Token
- 理解 `ResponseHandler` 的作用和实现
- 学会实现自定义的 Token 流处理
- 掌握流式输出的应用场景和最佳实践

## 前置知识

- 完成《01 - LangChain4j 简介》章节
- 完成《03 - 深入理解 ChatModel》章节
- 完成《11 - AI Services》章节（可选，推荐）

## 核心概念

### 什么是流式输出？

**流式输出（Streaming）**是指逐步生成和返回 AI 响应，而不是等待整个响应生成完毕后再一次性返回。

**类比理解：**
- **批量输出**：像是看完整的一部电影，必须下载整个文件后才能播放
- **流式输出**：像是在线视频网站看视频，边缓冲边播放，几乎即时开始

**为什么需要流式输出？**

1. **更好的用户体验** - 用户可以更快看到响应的开始，而不必等待整个响应生成完毕
2. **实时反馈** - 对于长文本生成，用户可以立即看到生成进度
3. **降低延迟感** - 用户感觉系统响应更快，体验更流畅
4. **适用于长内容** - 长文章、代码生成等场景下优势明显
5. **资源效率** - 可以更早地释放部分资源

### 流式输出的工作流程

```
用户发送查询
      ↓
┌─────────────────┐
│  ChatModel      │
└────────┬────────┘
         │
         │ 开始生成响应
         │
         ↓
    ┌────┴──────────────────────────┐
    │ Token 1 │ Token 2 │ ... │
    └─────────────────────────────┘
         │
         │ 实时推送到客户端
         │
         ↓
    客户实时显示 Token
```

### 批量输出 vs 流式输出对比

| 特性 | 批量输出 | 流式输出 |
|------|-----------|-----------|
| 首次 Token 时间 | 较慢（需等待全部生成） | 几乎立即 |
| 整体完成时间 | 快 | 相同 |
| 内存占用 | 较高（需缓存全部） | 较低 |
| 用户体验 | 较差（感觉等待） | 较好（即时反馈） |
| 实现复杂度 | 简单 | 稍复杂 |

## StreamingChatModel

### 基本使用

```java
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.V;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;

/**
 * 流式输出示例
 */
public class StreamingOutputExample {

    private final StreamingChatModel model;

    public StreamingOutputExample(String apiKey) {
        this.model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .build();
    }

    /**
     * 流式生成
     */
    public void streamchat(String userMessage, TokenStreamHandler handler) {
        // 发送消息给流式模型
        model.chat(userMessage, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                // 处理每个 Token
                handler.onToken(partialResponse);
            }
            
            @Override
            public void complete() {
                // 流式完成
                handler.onComplete();
            }
            
            @Override
            public void completeWithError(Throwable error) {
                // 流式出错
                handler.onError(error);
            }
        });
    }

    /**
     * Token 流处理接口
     */
    public interface TokenStreamHandler {
        void onToken(String partialResponse);
        void onComplete();
        void onError(Throwable error);
    }

    /**
     * 使用 @V 注解的流式输出
     */
    public void streamWithVAnnotation(String userMessage) {
        System.out.println("AI: ");

        model.chat(userMessage, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                System.out.print(partialResponse);
                System.out.flush();
            }
            
            @Override
            public void complete() {
                System.out.println();
                System.out.println("--- 流式输出完成 ---");
            }
        });

        System.out.println();
    }

    public static void main(String[] args) {
        StreamingOutputExample example = new StreamingOutputExample(
            System.getenv("OPENAI_API_KEY")
        );

        // 使用 @V 注解的流式输出
        System.out.println("=== 使用 @V 注解 ===");
        System.out.println();

        example.streamWithVAnnotation("请写一首关于春天的诗");

        // 使用自定义处理器的流式输出
        System.out.println("=== 使用自定义处理器 ===");
        System.out.println();

        example.streamchat("请介绍一下 LangChain4j", new TokenStreamHandler() {
            @Override
            public void onToken(String partialResponse) {
                System.out.print(partialResponse);
                System.out.flush();
            }

            @Override
            public void onComplete() {
                System.out.println();
                System.out.println("--- 完成 ---");
            }

            @Override
            public void onError(Throwable error) {
                System.err.println("错误: " + error.getMessage());
            }
        });
    }
}
```

## TokenStream 接口

### TokenStream 方法

```java
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

/**
 * TokenStream 接口详解
 */
public class TokenStreamExplanation {

    /**
     * next 方法
     * 
     * 当新的 Token 可用时调用
     * @param partialResponse 新生成的 Token 字符串
     */
    void next(String partialResponse);

    /**
     * complete 方法
     * 
     * 当所有 Token 都生成完毕时调用
     */
    void complete();

    /**
     * completeWithError 方法
     * 
     * 当 Token 生成过程中发生错误时调用
     * @param error 发生的错误
     */
    void completeWithError(Throwable error);
}
```

### 使用 @V 注解

```java
import dev.langchain4j.service.V;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * 使用 @V 注解的流式 Service
 */
public class StreamingServiceWithV {

    /**
     * 流式服务接口
     */
    public interface StreamingAssistant {
        @V
        String stream(@UserMessage String message);
    }

    /**
     * 创建流式服务
     */
    public static StreamingAssistant createService(String apiKey) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .build();

        return AiServices.builder(StreamingAssistant.class)
                .chatModel(model)
                .build();
    }

    public static void main(String[] args) {
        StreamingAssistant assistant = createService(
            System.getenv("OPENAI_API_KEY")
        );

        System.out.println("=== 流式输出 ===");
        System.out.println();

        // @V 注解会自动处理 Token 流
        String result = assistant.stream("请写一篇关于人工智能的文章");

        System.out.println();
        System.out.println("--- 完成 ---");
    }
}
```

## ResponseHandler

### ResponseHandler 接口

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.data.message.*;

/**
 * ResponseHandler 示例
 */
public class ResponseHandlerExample {

    /**
     * 自定义 ResponseHandler
     */
    public static class TokenAccumulator {
        private final StringBuilder builder;
        private int partialResponseCount;

        public TokenAccumulator() {
            this.builder = new StringBuilder();
            this.partialResponseCount = 0;
        }

        public void onNext(String partialResponse) {
            builder.append(partialResponse);
            partialResponseCount++;
            System.out.print(partialResponse);
            System.out.flush();
        }

        public void onComplete() {
            System.out.println();
            System.out.printf("生成完成，共 %d 个 Token\n", partialResponseCount);
            System.out.printf("总字符数: %d\n", builder.length());
        }

        public void onError(Throwable error) {
            System.err.println("流式输出错误: " + error.getMessage());
        }

        public String getFullResponse() {
            return builder.toString();
        }

        public int getTokenCount() {
            return partialResponseCount;
        }
    }

    /**
     * 使用自定义 ResponseHandler
     */
    public static void generateWithCustomHandler(String apiKey, String prompt) {
        StreamingChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .build();

        TokenAccumulator accumulator = new TokenAccumulator();

        System.out.println("AI: ");

        model.chat(prompt, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                accumulator.onNext(partialResponse);
            }

            @Override
            public void complete() {
                accumulator.onComplete();
            }

            @Override
            public void completeWithError(Throwable error) {
                accumulator.onError(error);
            }
        });

        System.out.println();
    }

    public static void main(String[] args) {
        generateWithCustomHandler(
            System.getenv("OPENAI_API_KEY"),
            "请用三个关键词描述 LangChain4j"
        );
    }
}
```

## 完整流式聊天系统

```java
import dev.langchain4j.service.V;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

import java.util.Scanner;

/**
 * 完整的流式聊天系统
 */
public class StreamingChatSystem {

    private final StreamingAssistant assistant;
    private final Scanner scanner;

    public StreamingChatSystem(String apiKey) {
        // 创建流式模型
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .build();

        this.assistant = AiServices.builder(StreamingAssistant.class)
                .chatModel(model)
                .systemMessageProvider(chatMemoryId -> "你是一个友好的助手")
                .build();

        this.scanner = new Scanner(System.in);
    }

    /**
     * 流式服务接口
     */
    public interface StreamingAssistant {
        @V
        String chat(@MemoryId String userId, @UserMessage String message);
    }

    /**
     * 开始聊天会话
     */
    public void startSession(String userId) {
        System.out.println("╔═════════════════════════════════════════════╗");
        System.out.println("║         流式聊天系统                       ║");
        System.out.println("╚═════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("用户 ID: " + userId);
        System.out.println("输入 'exit' 退出");
        System.out.println();

        while (true) {
            System.out.print("你: ");
            String input = scanner.nextLine().trim();

            if ("exit".equalsIgnoreCase(input)) {
                System.out.println("\n再见！");
                break;
            }

            if (input.isEmpty()) {
                continue;
            }

            // 流式响应
            System.out.print("AI: ");

            String response = assistant.chat(userId, input);

            System.out.println();
            System.out.println();
        }

        scanner.close();
    }

    public static void main(String[] args) {
        StreamingChatSystem chat = new StreamingChatSystem(
            System.getenv("OPENAI_API_KEY")
        );

        // 开始聊天
        chat.startSession("user123");
    }
}
```

## 流式输出进阶

### Token 累计和处理

```java
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

/**
 * Token 处理器
 */
public class TokenProcessor {

    /**
     * 累计 Token
     */
    public static class TokenAccumulator {
        private final StringBuilder fullText;
        private final List<String> partialResponses;
        private long startTime;

        public TokenAccumulator() {
            this.fullText = new StringBuilder();
            this.partialResponses = new ArrayList<>();
            this.startTime = System.currentTimeMillis();
        }

        public void onNext(String partialResponse) {
            partialResponses.add(partialResponse);
            fullText.append(partialResponse);
        }

        public void onComplete() {
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("========== 流式输出统计 ==========");
            System.out.printf("Token 数量: %d\n", partialResponses.size());
            System.out.printf("字符数量: %d\n", fullText.length());
            System.out.printf("总耗时: %d ms\n", duration);
            System.out.printf("平均每个 Token: %.2f ms\n", 
                (double) duration / partialResponses.size());
            System.out.println("====================================");
        }

        public void onError(Throwable error) {
            System.err.println("流式输出错误: " + error.getMessage());
            System.err.println("已生成 Token 数: " + partialResponses.size());
        }

        public String getFullText() {
            return fullText.toString();
        }

        public List<String> getTokens() {
            return new ArrayList<>(partialResponses);
        }
    }

    /**
     * Token 分类处理器
     */
    public static class TokenCategorizer {
        private final Map<String, Integer> partialResponseCounts;

        public TokenCategorizer() {
            this.partialResponseCounts = new HashMap<>();
        }

        public void onNext(String partialResponse) {
            partialResponseCounts.put(partialResponse, partialResponseCounts.getOrDefault(partialResponse, 0) + 1);
        }

        public void onComplete() {
            System.out.println("========== Token 统计 ==========");
            System.out.println("唯一 Token 数: " + partialResponseCounts.size());
            System.out.println("总 Token 数: " + partialResponseCounts.values().stream().mapToInt(Integer::intValue).sum());
            
            // 显示最常见的 Token
            System.out.println("\n最常见的 Token (Top 10):");
            partialResponseCounts.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(10)
                    .forEach(entry -> {
                        System.out.printf("  %s: %d 次\n", entry.getKey(), entry.getValue());
                    });
            
            System.out.println("====================================");
        }

        public void onError(Throwable error) {
            System.err.println("流式输出错误: " + error.getMessage());
        }
    }
}
```

### 多会话流式输出

```java
import dev.langchain4j.service.V;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.HashMap;
import java.util.Map;

/**
 * 多会话流式输出管理
 */
public class MultiSessionStreaming {

    private final Map<String, StreamingSession> sessions;
    private final ChatModel model;

    public MultiSessionStreaming(String apiKey) {
        this.model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .build();
        this.sessions = new HashMap<>();
    }

    /**
     * 获取或创建会话
     */
    public StreamingSession getSession(String userId) {
        return sessions.computeIfAbsent(userId, id -> {
            System.out.println("创建新会话: " + id);
            return new StreamingSession(id, model);
        });
    }

    /**
     * 移除会话
     */
    public void removeSession(String userId) {
        sessions.remove(userId);
        System.out.println("移除会话: " + userId);
    }

    /**
     * 流式服务接口
     */
    public interface StreamingAssistant {
        @V
        String chat(@MemoryId String userId, @UserMessage String message);
    }

    /**
     * 创建多会话流式服务
     */
    public static StreamingAssistant createService(String apiKey) {
        return AiServices.builder(StreamingAssistant.class)
                .chatModel(OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("gpt-4o-mini")
                        .build())
                .build();
    }

    /**
     * 流式会话
     */
    public static class StreamingSession {
        private final String userId;
        private final StreamingAssistant assistant;
        private final StringBuilder responseBuffer;

        public StreamingSession(String userId, ChatModel model) {
            this.userId = userId;
            this.assistant = AiServices.builder(StreamingAssistant.class)
                    .chatModel(model)
                    .systemMessageProvider(chatMemoryId -> "你是用户 " + userId + " 的助手")
                    .build();
            this.responseBuffer = new StringBuilder();
        }

        public String chat(String message) {
            // 流式响应
            String response = assistant.chat(userId, message);
            
            // 缓存完整响应
            responseBuffer.append(response);
            
            return response;
        }

        public String getConversationHistory() {
            return responseBuffer.toString();
        }

        public String getUserId() {
            return userId;
        }
    }

    public static void main(String[] args) {
        MultiSessionStreaming manager = new MultiSessionStreaming(
            System.getenv("OPENAI_API_KEY")
        );

        // 获取会话
        StreamingSession session1 = manager.getSession("user1");
        StreamingSession session2 = manager.getSession("user2");

        // 多用户对话
        System.out.println("=== 用户 1 对话 ===");
        System.out.println(session1.chat("你好，我是小明"));
        System.out.println(session1.chat("我叫什么？"));
        System.out.println();

        System.out.println("=== 用户 2 对话 ===");
        System.out.println(session2.chat("你好，我是小红"));
        System.out.println(session2.chat("我叫什么？"));
        System.out.println();

        // 验证独立性
        System.out.println("=== 验证：用户 1 继续对话 ===");
        System.out.println(session1.chat("我今年 25 岁"));
        System.out.println();

        // 获取会话历史
        System.out.println("=== 对话历史 ===");
        System.out.println("用户 1 历史: " + session1.getConversationHistory());
        System.out.println("用户 2 历史: " + session2.getConversationHistory());
    }
}
```

## 测试代码示例

```java
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.service.V;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.AiMessage;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 流式输出测试
 */
class StreamingOutputTest {

    private ChatModel model;

    @BeforeEach
    void setUp() {
        this.model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();
    }

    @Test
    void should_stream_partialResponses() {
        // 创建 Token 累积器
        TokenAccumulator accumulator = new TokenAccumulator();

        // 流式生成
        model.chat("你好", new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                accumulator.onNext(partialResponse);
            }

            @Override
            public void complete() {
                accumulator.onComplete();
            }

            @Override
            public void completeWithError(Throwable error) {
                accumulator.onError(error);
            }
        });

        // 验证
        String fullText = accumulator.getFullText();
        assertNotNull(fullText);
        assertFalse(fullText.isEmpty());
        
        // 验证 Token 数
        int partialResponseCount = accumulator.getTokenCount();
        assertTrue(partialResponseCount > 0);
        
        System.out.println("生成文本: " + fullText);
        System.out.println("Token 数量: " + partialResponseCount);
    }

    @Test
    void should_handle_streaming_error() {
        // 创建错误处理器
        ErrorTrackingHandler errorHandler = new ErrorTrackingHandler();

        // 测试错误处理
        try {
            // 这里会抛出异常（模拟）
            throw new RuntimeException("模拟流式错误");
        } catch (Exception e) {
            errorHandler.onError(e);
        }

        // 验证错误被捕获
        assertTrue(errorHandler.hasError());
        assertEquals("模拟流式错误", errorHandler.getErrorMessage());
    }

    @Test
    void should_support_v_annotation() {
        // 使用 @V 注解的服务
        interface StreamingService {
            @V
            String stream(String message);
        }

        StreamingService service = AiServices.builder(StreamingService.class)
                .chatModel(model)
                .build();

        // 流式生成
        TokenAccumulator accumulator = new TokenAccumulator();

        // @V 注解会自动处理 TokenStream
        String result = service.stream("你好");

        // 验证
        assertNotNull(result);
        assertEquals(accumulator.getFullText(), result);
    }

    /**
     * Token 累积器
     */
    public static class TokenAccumulator {
        private final StringBuilder builder;
        private int partialResponseCount;
        private boolean completed;

        public TokenAccumulator() {
            this.builder = new StringBuilder();
            this.partialResponseCount = 0;
            this.completed = false;
        }

        public void onNext(String partialResponse) {
            builder.append(partialResponse);
            partialResponseCount++;
        }

        public void onComplete() {
            this.completed = true;
        }

        public void onError(Throwable error) {
            System.err.println("错误: " + error.getMessage());
        }

        public String getFullText() {
            return builder.toString();
        }

        public int getTokenCount() {
            return partialResponseCount;
        }

        public boolean isCompleted() {
            return completed;
        }
    }

    /**
     * 错误跟踪处理器
     */
    public static class ErrorTrackingHandler {
        private Throwable error;

        public void onError(Throwable error) {
            this.error = error;
        }

        public boolean hasError() {
            return error != null;
        }

        public String getErrorMessage() {
            return error != null ? error.getMessage() : null;
        }
    }
}
```

## 实践练习

### 练习 1：创建打字机效果

实现一个打字机效果的流式输出：

```java
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

/**
 * 打字机效果流式输出
 */
public class TypewriterEffect {

    /**
     * 带打字机效果的 Token 处理器
     */
    public static class TypewriterTokenHandler {
        private final long delayMillis;
        private boolean paused;

        public TypewriterTokenHandler(long delayMillis) {
            this.delayMillis = delayMillis;
            this.paused = false;
        }

        public void onNext(String partialResponse) {
            if (paused) {
                return;
            }

            System.out.print(partialResponse);
            System.out.flush();

            // 延迟模拟打字
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public void onComplete() {
            System.out.println();
            System.out.println("--- 打字完成 ---");
        }

        public void onError(Throwable error) {
            System.err.println("错误: " + error.getMessage());
        }

        public void togglePause() {
            paused = !paused;
        System.out.println(paused ? "已暂停" : "已继续");
        }

        public void setDelay(long delayMillis) {
            this.delayMillis = delayMillis;
            System.out.println("打字速度已调整: " + delayMillis + "ms");
        }
    }

    public static void main(String[] args) {
        // 创建打字机处理器（50ms 延迟）
        TypewriterTokenHandler handler = new TypewriterTokenHandler(50);

        System.out.println("=== 打字机效果 ===");
        System.out.println();
        System.out.println("AI: ");

        // 模拟流式输出
        String response = "流式输出是一个很好的特性，它可以让用户更快地看到响应的开始，而不必等待整个响应生成完毕。对于长文本生成，流式输出特别有用。";

        // 逐字输出
        String[] partialResponses = response.split("");
        for (String partialResponse : partialResponses) {
            handler.onNext(partialResponse);
        }

        handler.onComplete();
    }
}
```

### 练习 2：实现实时Token统计

实现一个实时显示 Token 统计的处理器：

```java
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

/**
 * 实时 Token 统计
 */
public class RealTimeTokenStatistics {

    /**
     * 实时统计处理器
     */
    public static class RealTimeStatsHandler {
        private int partialResponseCount;
        private int charCount;
        private long startTime;
        private long lastUpdateTime;

        public RealTimeStatsHandler() {
            this.partialResponseCount = 0;
            this.charCount = 0;
            this.startTime = System.currentTimeMillis();
            this.lastUpdateTime = startTime;
        }

        public void onNext(String partialResponse) {
            partialResponseCount++;
            charCount += partialResponse.length();

            long now = System.currentTimeMillis();
            long elapsed = now - startTime;
            long sinceLastUpdate = now - lastUpdateTime;

            // 每 10 个 Token 或每秒更新一次统计
            if (partialResponseCount % 10 == 0 || sinceLastUpdate > 1000) {
                printStats(elapsed, partialResponseCount, charCount);
                lastUpdateTime = now;
            }
        }

        public void onComplete() {
            long totalElapsed = System.currentTimeMillis() - startTime;
            System.out.println("\n========== 最终统计 ==========");
            System.out.printf("总耗时: %.2f 秒\n", totalElapsed / 1000.0);
            System.out.printf("总 Token 数: %d\n", partialResponseCount);
            System.out.printf("总字符数: %d\n", charCount);
            System.out.printf("Token 生成速度: %.2f partialResponses/s\n", 
                    (double) partialResponseCount / (totalElapsed / 1000.0));
            System.out.printf("字符生成速度: %.2f chars/s\n", 
                    (double) charCount / (totalElapsed / 1000.0));
            System.out.printf("平均每个 Token: %.2f ms\n", 
                    (double) totalElapsed / partialResponseCount);
            System.out.println("================================");
        }

        public void onError(Throwable error) {
            System.err.println("\n流式输出错误: " + error.getMessage());
            printStats(System.currentTimeMillis() - startTime, partialResponseCount, charCount);
        }

        private void printStats(long elapsed, int partialResponseCount, int charCount) {
            System.out.printf("\r[统计] Token: %d, 字符: %d, 耗时: %.1fs  ", 
                    partialResponseCount, charCount, elapsed / 1000.0);
        }

        public int getTokenCount() { return partialResponseCount; }
        public int getCharCount() { return charCount; }
    }

    public static void main(String[] args) {
        RealTimeStatsHandler handler = new RealTimeStatsHandler();

        System.out.println("=== 实时 Token 统计 ===");
        System.out.println();

        // 模拟流式输出
        String response = "这个示例展示了如何在流式输出过程中实时统计 Token 和字符数量。";
        System.out.print("AI: ");

        String[] partialResponses = response.split("");
        for (String partialResponse : partialResponses) {
            handler.onNext(partialResponse);
            try {
                Thread.sleep(100);  // 模拟生成延迟
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        handler.onComplete();
    }
}
```

### 练习 3：实现流式进度条

实现一个显示进度的流式处理器：

```java
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

/**
 * 流式进度条
 */
public class StreamingProgressBar {

    /**
     * 进度条处理器
     */
    public static class ProgressBarHandler {
        private final int totalExpectedTokens;
        private int currentTokenCount;
        private final int barLength;
        private final char fillChar;
        private final char emptyChar;

        public ProgressBarHandler(int totalExpectedTokens) {
            this.totalExpectedTokens = totalExpectedTokens;
            this.currentTokenCount = 0;
            this.barLength = 50;  // 进度条长度
            this.fillChar = '=';
            this.emptyChar = '-';
        }

        public void onNext(String partialResponse) {
            currentTokenCount++;
            updateProgress();
        }

        private void updateProgress() {
            // 计算进度
            double progress = (double) currentTokenCount / totalExpectedTokens;
            int filled = (int) (progress * barLength);

            // 构建进度条
            StringBuilder bar = new StringBuilder("[");
            for (int i = 0; i < barLength; i++) {
                bar.append(i < filled ? fillChar : emptyChar);
            }
            bar.append("] ");

            // 显示进度
            System.out.printf("\r[进度] %s %.1f%% (%d/%d)", 
                    bar.toString(),
                    progress * 100,
                    currentTokenCount,
                    totalExpectedTokens);
        }

        public void onComplete() {
            // 显示完成状态
            System.out.print("\r");
            System.out.printf("\r[完成] %s (%d/%d partialResponses)\n", 
                    "=".repeat(barLength),
                    currentTokenCount,
                    totalExpectedTokens);
        }

        public void onError(Throwable error) {
            System.err.println("\n错误: " + error.getMessage());
            System.out.print("\r[错误] 流式输出失败");
        }

        public int getCurrentTokenCount() {
            return currentTokenCount;
        }
    }

    public static void main(String[] args) {
        // 预期 100 个 Token
        ProgressBarHandler handler = new ProgressBarHandler(100);

        System.out.println("=== 流式进度条 ===");
        System.out.println();
        System.out.println("AI: ");

        // 模拟流式输出（每 10 个 Token 更新一次进度）
        for (int i = 0; i < 100; i++) {
            handler.onNext("partialResponse");
            
            try {
                Thread.sleep(50);  // 模拟生成延迟
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        handler.onComplete();
    }
}
```

## 总结

### 本章要点

1. **流式输出概念**
   - 逐步生成和返回响应
   - 提升用户体验，降低延迟感
   - 适用于长内容生成场景

2. **核心组件**
   - `StreamingChatModel` - 流式模型
   - `TokenStream` - Token 流接口
   - `@V` 注解 - 简化流式输出
   - `ResponseHandler` - 响应处理器

3. **实现方式**
   - 使用 `@V` 注解（推荐，简洁）
   - 使用 `TokenStream` 接口（灵活）
   - 使用 `ResponseHandler`（高级）

4. **应用场景**
   - 打字机效果
   - 实时统计
   - 进度显示
   - 多会话管理

5. **最佳实践**
   - 合理设置 Token 延迟
   - 实现错误处理和重试
   - 优化网络和性能
   - 考虑用户体验

### 下一步

在下一章节中，我们将学习：
- 请求拦截器和中间件
- 自定义响应处理
- 错误处理和重试机制
- 流式输出的性能优化
- 生产环境部署建议

### 常见问题

**Q1：流式输出和批量输出有什么区别？**

A：
- **流式输出**：逐步生成，实时显示，用户体验更好
- **批量输出**：一次性返回，实现简单，用户体验较差
- 建议：生产环境推荐使用流式输出

**Q2：如何选择 Token 流输出的延迟？**

A：考虑因素：
- 用户体验：太快可能看不清，太慢感觉延迟
- 生成速度：根据模型生成速度调整
- 网络延迟：考虑网络往返时间
- 推荐：通常 10-50ms 比较合适

**Q3：流式输出会增加 API 成本吗？**

A：通常不会增加成本，成本是基于 Token 数量计算的，与输出方式无关。但流式输出可能会略微增加网络请求次数。

**Q4：如何在生产环境中监控流式输出？**

A：监控指标：
- Token 生成速率
- 响应时间
- 错误率和重试次数
- 网络延迟和吞吐量
- 资源使用情况

**Q5：流式输出支持所有模型吗？**

A：不是所有模型都支持流式输出。需要：
- 模型本身支持流式输出
- LangChain4j 实现了相应的 `StreamingChatModel`
- 具体支持情况请参考模型文档

## 参考资料

- [LangChain4j 流式输出文档](https://docs.langchain4j.dev/tutorials/response-streaming)
- [LangChain4j TokenStream 文档](https://docs.langchain4j.dev/tutorials/response-streaming)
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
