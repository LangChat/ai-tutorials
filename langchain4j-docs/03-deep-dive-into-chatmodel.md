---
title: 'ChatModel 深度解析'
description: '学习 LangChain4j 的 ChatModel 深度解析 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 03 - ChatModel 深入理解

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 深入理解 ChatModel 接口的所有方法
- 区分 ChatModel 和 ChatModel 的使用场景
- 掌握 StreamingChatModel 流式模型的使用
- 学会选择合适的模型
- 理解异常处理和错误管理

## 前置知识

- 完成《01 - LangChain4j 简介》章节
- 完成《02 - 第一个 Chat 应用》章节
- 了解基本的 Java 接口和异常处理

## 核心概念

### ChatModel 接口层次结构

LangChain4j 提供了多个层级的模型接口，让我们深入了解它们的关系和使用场景：

```java
// 接口继承关系
LanguageModel (已废弃，不推荐使用)
    ↓
ChatModel (推荐用于简单场景）
    ↓
ChatModel (推荐用于需要控制的场景）
    ↓
StreamingChatModel (用于流式响应场景）
```

### 接口对比

| 特性 | LanguageModel | ChatModel | ChatModel | StreamingChatModel |
|------|--------------|-------------------|-----------|-------------------|
| 输入类型 | String | String | ChatMessage | ChatMessage |
| 输出类型 | String | String | ChatResponse | TokenStream |
| 控制力 | 低 | 中 | 高 | 高 |
| 流式支持 | ❌ | ❌ | ❌ | ✅ |
| 元数据访问 | ❌ | ❌ | ✅ | ✅ |
| 推荐场景 | 不推荐 | 简单场景 | 进阶场景 | 流式响应 |

## ChatModel 详细使用

### 接口定义

```java
public interface ChatModel {
    
    /**
     * 生成响应（最简单的方法）
     * @param userMessage 用户消息
     * @return AI 生成的响应
     */
    String chat(String userMessage);
}
```

### 使用示例

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class ChatModelExample {

    public static void main(String[] args) {
        // 创建模型实例
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        // 场景 1：简单对话
        String greeting = model.chat("你好");
        System.out.println(greeting);

        // 场景 2：问答
        String answer = model.chat("Java 的特点是什么？");
        System.out.println(answer);

        // 场景 3：文本生成
        String story = model.chat("写一个关于 AI 的短故事");
        System.out.println(story);
    }
}
```

### 适用场景

**使用 ChatModel 的最佳时机：**
1. **快速原型开发** - 需要快速验证想法
2. **简单文本处理** - 只需要文本输入输出
3. **学习阶段** - 刚开始学习 LangChain4j
4. **脚本任务** - 批处理或自动化脚本

**不推荐使用的场景：**
1. 需要访问响应元数据（Token 使用、模型信息等）
2. 需要使用系统消息或消息历史
3. 需要流式输出
4. 生产环境中的复杂应用

## ChatModel 详细使用

### 接口定义

```java
public interface ChatModel {
    
    /**
     * 最简单的方法 - 便捷方法
     */
    String chat(String userMessage);
    
    /**
     * 发送单个消息
     */
    ChatResponse chat(ChatMessage message);
    
    /**
     * 发送多个消息（可变参数）
     */
    ChatResponse chat(ChatMessage... messages);
    
    /**
     * 发送多个消息（列表）
     */
    ChatResponse chat(List<ChatMessage> messages);
    
    /**
     * 完全控制 - 使用 ChatRequest
     */
    ChatResponse chat(ChatRequest chatRequest);
}
```

### 方法详解

#### 1. chat(String userMessage) - 便捷方法

```java
ChatModel model = ...;

// 最简单的方式，类似 ChatModel
String response = model.chat("你好");
System.out.println(response);
```

**特点：**
- 最简单快捷
- 直接返回字符串
- 适合快速测试

#### 2. chat(ChatMessage message) - 单消息

```java
import dev.langchain4j.data.message.UserMessage;

ChatModel model = ...;

// 使用 UserMessage
UserMessage userMessage = UserMessage.from("你好");
ChatResponse response = model.chat(userMessage);

// 获取响应文本
String text = response.aiMessage().text();
System.out.println(text);
```

**特点：**
- 可以使用不同类型的消息
- 返回完整的 ChatResponse 对象
- 可以访问元数据

#### 3. chat(ChatMessage... messages) - 多消息

```java
import dev.langchain4j.data.message.*;
import java.util.List;

ChatModel model = ...;

// 构建多轮对话
List<ChatMessage> messages = List.of(
    SystemMessage.from("你是一个友好的助手"),
    UserMessage.from("你好"),
    AiMessage.from("你好！有什么可以帮助你的吗？"),
    UserMessage.from("介绍一下 Java")
);

ChatResponse response = model.chat(messages);
System.out.println(response.aiMessage().text());
```

**特点：**
- 支持完整对话历史
- 可以包含系统消息
- LLM 可以理解上下文

#### 4. chat(ChatRequest chatRequest) - 完全控制

```java
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;

ChatModel model = ...;

// 构建请求参数
ChatRequestParameters parameters = ChatRequestParameters.builder()
        .temperature(0.7)
        .maxOutputTokens(500)
        .build();

// 构建完整请求
ChatRequest request = ChatRequest.builder()
        .messages(
            SystemMessage.from("你是一个专业的助手"),
            UserMessage.from("什么是机器学习？")
        )
        .parameters(parameters)
        .build();

ChatResponse response = model.chat(request);
System.out.println(response.aiMessage().text());
```

**特点：**
- 完全控制请求
- 可以设置所有参数
- 最灵活的方式

### ChatResponse 详细解析

```java
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.data.message.AiMessage;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.model.output.FinishReason;

ChatResponse response = model.chat("你好");

// 1. 获取 AI 消息
AiMessage aiMessage = response.aiMessage();
String text = aiMessage.text();              // 文本内容
String thinking = aiMessage.thinking();       // 思考内容（如果模型支持）

// 2. 获取元数据
ChatResponseMetadata metadata = response.metadata();
String modelId = metadata.modelName();          // 模型名称
String responseId = metadata.id();              // 响应 ID
TokenUsage tokenUsage = metadata.tokenUsage();   // Token 使用
FinishReason finishReason = metadata.finishReason(); // 完成原因

// 3. Token 使用详情
int inputTokens = tokenUsage.inputTokenCount();     // 输入 Token
int outputTokens = tokenUsage.outputTokenCount();   // 输出 Token
int totalTokens = tokenUsage.totalTokenCount();     // 总 Token

// 4. 完成原因
System.out.println("完成原因: " + finishReason);
```

## StreamingChatModel 流式响应

### 什么是流式响应？

流式响应（Streaming）允许你在 LLM 生成内容的同时，逐个 Token 地接收输出，而不是等待整个响应生成完成。

**类比理解：**
- **非流式**：等待一封信完整写好才送给你
- **流式响应**：看着对方写信，每写一个字就能看到一个字

### 为什么使用流式响应？

1. **更好的用户体验** - 用户看到内容逐步出现，感觉更快
2. **实时反馈** - 可以提前展示部分内容
3. **长时间生成** - 对于生成长文本的场景特别有用
4. **取消能力** - 可以中途取消生成

### StreamingChatModel 接口定义

```java
public interface StreamingChatModel {
    
    /**
     * 生成流式响应
     * @param messages 消息列表
     * @param responseHandler 响应处理器
     */
    void chat(List<ChatMessage> messages, 
                 StreamingChatResponseHandler responseHandler);
}
```

### 基本使用

```java
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.data.message.*;
import java.util.List;

public class StreamingExample {

    public static void main(String[] args) {
        StreamingChatModel model = OpenAiStreamingChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        List<ChatMessage> messages = List.of(
            UserMessage.from("讲一个关于编程的故事")
        );

        // 定义响应处理器
        StreamingChatResponseHandler handler = new StreamingChatResponseHandler() {
            
            @Override
            public void onPartialResponse(String partialResponse) {
                // 接收部分响应
                System.out.print(partialResponse);
            }
            
            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                // 响应完成
                System.out.println("\n\n生成完成！");
                System.out.println("Token 使用: " + 
                    completeResponse.metadata().tokenUsage().totalTokenCount());
            }
            
            @Override
            public void onError(Throwable error) {
                // 错误处理
                System.err.println("错误: " + error.getMessage());
            }
        };

        // 生成流式响应
        model.chat(messages, handler);
    }
}
```

### TokenStream 高级流式响应

使用 `TokenStream` 获得更灵活的控制：

```java
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.data.message.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TokenStreamExample {

    public static void main(String[] args) {
        StreamingChatModel model = OpenAiStreamingChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        List<ChatMessage> messages = List.of(
            UserMessage.from("写一首关于春天的诗")
        );

        // 获取 TokenStream
        TokenStream tokenStream = model.chat(messages);
        
        // 创建 Future 用于等待完成
        CompletableFuture<ChatResponse> futureResponse = new CompletableFuture<>();
        
        StringBuilder fullResponse = new StringBuilder();
        
        tokenStream
            // 接收部分响应
            .onPartialResponse(partialResponse -> {
                System.out.print(partialResponse);
                fullResponse.append(partialResponse);
            })
            
            // 接收部分思考（如果模型支持）
            .onPartialThinking(partialThinking -> {
                System.out.println("[思考中...] " + partialThinking);
            })
            
            // 响应完成
            .onCompleteResponse(response -> {
                System.out.println("\n\n生成完成！");
                futureResponse.complete(response);
            })
            
            // 错误处理
            .onError(error -> {
                System.err.println("错误: " + error.getMessage());
                futureResponse.completeExceptionally(error);
            })
            
            // 开始流式响应
            .start();
        
        // 等待完成
        try {
            ChatResponse response = futureResponse.join();
            System.out.println("完整响应: " + fullResponse.toString());
        } catch (Exception e) {
            System.err.println("生成失败: " + e.getMessage());
        }
    }
}
```

### 流式响应的回调

```java
TokenStream tokenStream = model.chat(messages);

tokenStream
    // 1. 部分响应
    .onPartialResponse(String partialResponse)
    
    // 2. 部分思考（Chain of Thought）
    .onPartialThinking(String partialThinking)
    
    // 3. 检索到的内容（RAG）
    .onRetrieved(List<Content> contents)
    
    // 4. 中间响应（工具调用后）
    .onIntermediateResponse(ChatResponse intermediateResponse)
    
    // 5. 部分工具调用
    .onPartialToolCall(PartialToolCall partialToolCall)
    
    // 6. 工具执行前
    .beforeToolExecution(BeforeToolExecution beforeToolExecution)
    
    // 7. 工具执行后
    .onToolExecuted(ToolExecution toolExecution)
    
    // 8. 完整响应
    .onCompleteResponse(ChatResponse response)
    
    // 9. 错误
    .onError(Throwable error)
    
    // 开始
    .start();
```

### 取消流式响应

```java
TokenStream tokenStream = model.chat(messages);

CompletableFuture<ChatResponse> future = new CompletableFuture<>();

tokenStream
    .onPartialResponseWithContext((partialResponse, context) -> {
        System.out.print(partialResponse);
        
        // 如果响应超过 100 个字符，取消
        if (partialResponse.length() > 100) {
            context.streamingHandle().cancel();
            future.completeExceptionally(
                new RuntimeException("响应过长，已取消"));
        }
    })
    .onCompleteResponse(response -> future.complete(response))
    .onError(error -> future.completeExceptionally(error))
    .start();

try {
    future.join();
} catch (Exception e) {
    System.out.println("生成被取消: " + e.getMessage());
}
```

## 模型选择策略

### OpenAI 模型对比

| 模型 | 上下文 | 速度 | 成本 | 能力 | 适用场景 |
|------|--------|------|------|------|----------|
| gpt-4o | 128K | 中 | 高 | 最高 | 复杂推理、代码生成 |
| gpt-4o-mini | 128K | 快 | 低 | 高 | 通用场景、平衡性能和成本 |
| gpt-3.5-turbo | 16K | 很快 | 很低 | 中 | 简单任务、批量处理 |

### 选择建议

**选择 gpt-4o：**
- 需要最高质量和推理能力
- 复杂的代码生成和分析
- 多步推理任务
- 预算充足

**选择 gpt-4o-mini（推荐）：**
- 平衡性能和成本
- 通用聊天应用
- 大多数生产环境
- 性价比最高

**选择 gpt-3.5-turbo：**
- 简单文本处理
- 批量任务
- 成本敏感
- 不需要复杂推理

### 其他模型提供商

```java
// Anthropic Claude - 擅长上下文
import dev.langchain4j.model.anthropic.AnthropicChatModel;
ChatModel claude = AnthropicChatModel.builder()
        .apiKey(System.getenv("ANTHROPIC_API_KEY"))
        .modelName("claude-3-5-sonnet-20241022")
        .build();

// Google Gemini - 多模态
import dev.langchain4j.model.google.GeminiChatModel;
ChatModel gemini = GeminiChatModel.builder()
        .apiKey(System.getenv("GOOGLE_API_KEY"))
        .modelName("gemini-1.5-pro")
        .build();

// Ollama - 本地运行
import dev.langchain4j.model.ollama.OllamaChatModel;
ChatModel ollama = OllamaChatModel.builder()
        .baseUrl("http://localhost:11434")
        .modelName("llama2")
        .build();
```

## 异常处理

### 常见异常类型

```java
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.exception.AuthenticationException;
import dev.langchain4j.exception.RateLimitException;
import dev.langchain4j.exception.TokenException;
import dev.langchain4j.model.chat.ChatModel;

ChatModel model = ...;

try {
    String response = model.chat("你好");
    System.out.println(response);
} catch (AuthenticationException e) {
    // API Key 无效或过期
    System.err.println("认证失败: " + e.getMessage());
} catch (RateLimitException e) {
    // 超过速率限制
    System.err.println("请求过快: " + e.getMessage());
} catch (TokenException e) {
    // Token 限制相关错误
    System.err.println("Token 错误: " + e.getMessage());
} catch (HttpException e) {
    // HTTP 请求错误
    System.err.println("HTTP 错误: " + e.getMessage());
} catch (Exception e) {
    // 其他错误
    System.err.println("未知错误: " + e.getMessage());
}
```

### 重试机制

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.time.Duration;

public class RetryExample {

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .maxRetries(3)              // 最大重试次数
                .timeout(Duration.ofSeconds(30)) // 每次请求的超时时间
                .build();
        
        try {
            String response = model.chat("你好");
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("重试失败: " + e.getMessage());
        }
    }
}
```

### 自定义异常处理

```java
public class RobustChatService {

    private final ChatModel model;
    
    public RobustChatService(ChatModel model) {
        this.model = model;
    }
    
    public String generateWithRetry(String message, int maxRetries) {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                return model.chat(message);
            } catch (RateLimitException e) {
                attempt++;
                if (attempt >= maxRetries) {
                    throw e;
                }
                // 指数退避
                long waitTime = (long) Math.pow(2, attempt) * 1000;
                System.out.println("速率限制，等待 " + waitTime + "ms 后重试...");
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试被中断", ie);
                }
            } catch (Exception e) {
                // 其他异常不重试
                throw e;
            }
        }
        throw new RuntimeException("重试次数耗尽");
    }
}
```

## 完整示例：智能聊天服务

```java
import dev.langchain4j.model.chat.*;
import dev.langchain4j.model.openai.*;
import dev.langchain4j.data.message.*;
import dev.langchain4j.exception.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 智能聊天服务 - 支持异常处理和重试
 */
public class SmartChatService {

    private final ChatModel model;
    private final List<ChatMessage> conversationHistory;
    
    public SmartChatService() {
        this.model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .temperature(0.7)
                .maxRetries(3)
                .timeout(Duration.ofSeconds(30))
                .logRequests(true)
                .logResponses(true)
                .build();
        this.conversationHistory = new ArrayList<>();
        
        // 添加系统消息
        conversationHistory.add(
            SystemMessage.from("你是一个友好、专业的助手")
        );
    }
    
    /**
     * 发送消息并获取响应（带重试）
     */
    public String chat(String userMessage) {
        // 添加用户消息到历史
        conversationHistory.add(UserMessage.from(userMessage));
        
        try {
            // 发送对话历史
            ChatResponse response = model.chat(conversationHistory);
            
            // 添加 AI 响应到历史
            conversationHistory.add(response.aiMessage());
            
            // 返回响应文本
            return response.aiMessage().text();
            
        } catch (RateLimitException e) {
            System.err.println("警告: 请求过快，请稍后再试");
            return "抱歉，我处理得有点慢，请稍后再试。";
            
        } catch (AuthenticationException e) {
            System.err.println("错误: API Key 无效");
            return "服务配置错误，请联系管理员。";
            
        } catch (HttpException e) {
            System.err.println("错误: 网络请求失败 - " + e.getMessage());
            return "网络连接问题，请检查网络后重试。";
            
        } catch (Exception e) {
            System.err.println("错误: 未知错误 - " + e.getMessage());
            return "抱歉，出现了未知错误。";
        }
    }
    
    /**
     * 清空对话历史
     */
    public void clearHistory() {
        // 保留系统消息
        List<ChatMessage> systemMessages = conversationHistory.stream()
                .filter(msg -> msg instanceof SystemMessage)
                .toList();
        conversationHistory.clear();
        conversationHistory.addAll(systemMessages);
    }
    
    /**
     * 获取 Token 使用统计
     */
    public TokenUsage getLastTokenUsage() {
        if (!conversationHistory.isEmpty() && 
            conversationHistory.get(conversationHistory.size() - 1) instanceof AiMessage) {
            AiMessage lastMessage = (AiMessage) conversationHistory.get(
                conversationHistory.size() - 1);
            // 注意：这里简化了，实际应该从 ChatResponse 获取
            return new TokenUsage(0, 0, 0);
        }
        return new TokenUsage(0, 0, 0);
    }
    
    public static void main(String[] args) {
        SmartChatService service = new SmartChatService();
        
        // 对话示例
        System.out.println("=== 智能聊天服务 ===");
        System.out.println();
        
        System.out.println("用户: 你好");
        System.out.println("AI: " + service.chat("你好"));
        System.out.println();
        
        System.out.println("用户: 什么是 LangChain4j？");
        System.out.println("AI: " + service.chat("什么是 LangChain4j？"));
        System.out.println();
        
        System.out.println("用户: 清空历史");
        service.clearHistory();
        System.out.println("对话历史已清空");
        System.out.println();
        
        System.out.println("用户: 你好");
        System.out.println("AI: " + service.chat("你好"));
    }
}
```

## 测试代码示例

```java
import dev.langchain4j.model.chat.*;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.data.message.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

class ChatModelAdvancedTest {

    private ChatModel chatModel;
    private StreamingChatModel streamingModel;

    @BeforeEach
    void setUp() {
        chatModel = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();
        
        streamingModel = OpenAiStreamingChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();
    }

    @Test
    void should_use_chat_model_with_multiple_messages() {
        List<ChatMessage> messages = List.of(
            SystemMessage.from("你是一个数学老师"),
            UserMessage.from("2 + 2 等于几？")
        );
        
        ChatResponse response = chatModel.chat(messages);
        
        assertNotNull(response);
        assertTrue(response.aiMessage().text().contains("4"));
        System.out.println("多消息响应: " + response.aiMessage().text());
    }

    @Test
    void should_provide_metadata_in_response() {
        ChatResponse response = chatModel.chat("你好");
        
        assertNotNull(response.metadata());
        assertNotNull(response.metadata().modelName());
        assertNotNull(response.metadata().tokenUsage());
        assertNotNull(response.metadata().finishReason());
        
        System.out.println("模型: " + response.metadata().modelName());
        System.out.println("Token 使用: " + 
            response.metadata().tokenUsage().totalTokenCount());
    }

    @Test
    void should_stream_response() throws Exception {
        List<ChatMessage> messages = List.of(
            UserMessage.from("用一句话介绍 Java")
        );
        
        CompletableFuture<ChatResponse> future = new CompletableFuture<>();
        StringBuilder fullResponse = new StringBuilder();
        
        streamingModel.chat(messages, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                fullResponse.append(partialResponse);
            }
            
            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                future.complete(completeResponse);
            }
            
            @Override
            public void onError(Throwable error) {
                future.completeExceptionally(error);
            }
        });
        
        ChatResponse response = future.get();
        assertNotNull(response);
        assertFalse(fullResponse.toString().isEmpty());
        
        System.out.println("流式响应: " + fullResponse.toString());
    }

    @Test
    void should_handle_long_context() {
        // 构建长对话上下文
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from("你是一个故事讲述者"));
        
        for (int i = 0; i < 10; i++) {
            messages.add(UserMessage.from("第 " + (i + 1) + " 部分"));
            ChatResponse response = chatModel.chat(messages);
            messages.add(response.aiMessage());
        }
        
        // 验证 LLM 记得了上下文
        assertTrue(messages.size() > 20);
        System.out.println("消息总数: " + messages.size());
    }
}
```

## 实践练习

### 练习 1：对比不同接口

创建一个对比程序，比较三种接口的使用：

```java
public class ModelComparison {

    public static void main(String[] args) {
        String message = "写一首关于春天的诗";
        
        // ChatModel
        System.out.println("=== ChatModel ===");
        ChatModel languageModel = ...;
        String response1 = languageModel.chat(message);
        System.out.println(response1);
        System.out.println("类型: String");
        
        // ChatModel
        System.out.println("\n=== ChatModel ===");
        ChatModel chatModel = ...;
        ChatResponse response2 = chatModel.chat(message);
        System.out.println(response2.aiMessage().text());
        System.out.println("类型: ChatResponse");
        System.out.println("Token 使用: " + 
            response2.metadata().tokenUsage().totalTokenCount());
        
        // StreamingChatModel
        System.out.println("\n=== StreamingChatModel ===");
        StreamingChatModel streamingModel = ...;
        streamingModel.chat(
            List.of(UserMessage.from(message)),
            new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    System.out.print(partialResponse);
                }
                
                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    System.out.println("\n类型: TokenStream");
                }
                
                @Override
                public void onError(Throwable error) {
                    System.err.println("错误: " + error.getMessage());
                }
            }
        );
    }
}
```

### 练习 2：实现智能重试

实现一个带指数退避的重试机制：

```java
public class ExponentialBackoffRetry {

    private final ChatModel model;
    private final int maxRetries;
    
    public ExponentialBackoffRetry(ChatModel model, int maxRetries) {
        this.model = model;
        this.maxRetries = maxRetries;
    }
    
    public String generateWithRetry(String message) {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                return model.chat(message);
            } catch (RateLimitException e) {
                attempt++;
                if (attempt >= maxRetries) {
                    throw new RuntimeException("重试次数耗尽", e);
                }
                
                // 指数退避：2^attempt 秒
                long waitTime = (long) Math.pow(2, attempt) * 1000;
                System.out.printf(
                    "速率限制，第 %d/%d 次重试，等待 %dms...%n",
                    attempt, maxRetries, waitTime
                );
                
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试被中断", ie);
                }
            } catch (Exception e) {
                // 其他异常不重试
                throw e;
            }
        }
        throw new RuntimeException("重试次数耗尽");
    }
    
    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();
        
        ExponentialBackoffRetry retry = 
            new ExponentialBackoffRetry(model, 5);
        
        try {
            String response = retry.generateWithRetry("你好");
            System.out.println("响应: " + response);
        } catch (Exception e) {
            System.err.println("失败: " + e.getMessage());
        }
    }
}
```

### 练习 3：流式响应进度条

创建一个带进度条的流式响应示例：

```java
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

public class StreamingProgressBar {

    private final StreamingChatModel model;
    
    public StreamingProgressBar(StreamingChatModel model) {
        this.model = model;
    }
    
    public void chatWithProgress(String message) {
        TokenStream tokenStream = model.chat(
            List.of(UserMessage.from(message))
        );
        
        StringBuilder fullResponse = new StringBuilder();
        int progress = 0;
        
        tokenStream
            .onPartialResponse(partial -> {
                System.out.print(partial);
                fullResponse.append(partial);
                
                // 显示进度（假设期望 100 个字符）
                progress = Math.min(100, fullResponse.length());
                System.out.printf("\n进度: [%-50s] %d%%", 
                    "=".repeat(progress / 2), progress);
            })
            .onCompleteResponse(response -> {
                System.out.println("\n完成！");
                System.out.println("总 Token: " + 
                    response.metadata().tokenUsage().totalTokenCount());
            })
            .onError(error -> {
                System.err.println("\n错误: " + error.getMessage());
            })
            .start();
    }
    
    public static void main(String[] args) {
        StreamingChatModel model = OpenAiStreamingChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();
        
        StreamingProgressBar progress = new StreamingProgressBar(model);
        progress.chatWithProgress("讲一个关于 AI 发展的故事");
    }
}
```

## 总结

### 本章要点

1. **ChatModel 接口层次**
   - LanguageModel（废弃）
   - ChatModel（简单场景）
   - ChatModel（进阶场景）
   - StreamingChatModel（流式响应）

2. **ChatModel**
   - 最简单的接口
   - 只接受 String，返回 String
   - 适合快速原型开发

3. **ChatModel**
   - 提供多种 generate 方法
   - 返回 ChatResponse 对象
   - 可以访问元数据和完整信息

4. **StreamingChatModel**
   - 支持流式输出
   - 提供 TokenStream 和响应处理器
   - 可以中途取消

5. **模型选择**
   - gpt-4o：最高能力
   - gpt-4o-mini：平衡选择（推荐）
   - gpt-3.5-turbo：低成本

6. **异常处理**
   - 常见异常类型
   - 内置重试机制
   - 自定义重试策略

### 下一步

在下一章节中，我们将学习：
- 模型参数的详细配置
- temperature、topP、topK 等参数的含义
- 参数调优技巧

### 常见问题

**Q1：什么时候应该使用 StreamingChatModel？**

A：当用户需要实时看到生成内容时，比如：
- 聊天机器人
- 长文本生成
- 需要提前展示结果的场景

**Q2：ChatModel 的 generate 方法有什么区别？**

A：
- `chat(String)` - 最简单
- `chat(ChatMessage)` - 单消息
- `chat(ChatMessage...)` - 多消息
- `chat(ChatRequest)` - 完全控制

**Q3：如何选择合适的模型？**

A：考虑成本、速度、能力三个因素：
- 高要求用 gpt-4o
- 平衡用 gpt-4o-mini（推荐）
- 低成本用 gpt-3.5-turbo

**Q4：流式响应会增加成本吗？**

A：不会，Token 使用量相同，只是接收方式不同。但可能需要处理更多的网络小包。

**Q5：如何处理网络不稳定？**

A：使用重试机制和超时设置，参考本文档的异常处理部分。

## 参考资料

- [LangChain4j ChatModel 文档](https://docs.langchain4j.dev/tutorials/chat-and-language-models)
- [OpenAI 官方文档](https://platform.openai.com/docs)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
