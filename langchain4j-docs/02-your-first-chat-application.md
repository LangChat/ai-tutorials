---
title: '第一个聊天应用'
description: '学习 LangChain4j 的 第一个聊天应用 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 02 - 第一个 Chat 应用

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 深入理解 ChatModel 的基本概念
- 掌握创建和配置 ChatModel 实例的方法
- 学会发送不同类型的消息
- 理解如何接收和处理 LLM 响应
- 构建一个完整的聊天应用示例

## 前置知识

- 完成《01 - LangChain4j 简介》章节
- 理解基本的 LLM 概念
- 已配置好开发环境和 API Key

## 核心概念

### ChatModel 简介

`ChatModel` 是 LangChain4j 中最核心的接口之一，用于与大语言模型进行交互。它代表了与大语言模型对话的能力。

**类比理解：**
如果把 LLM 比作一个智能客服人员，那么 `ChatModel` 就是你的电话——通过它你可以与客服人员（LLM）进行对话。

### ChatModel 接口层次

LangChain4j 提供了多个层级的接口，从简单到复杂：

```java
// 1. 最简单的接口（已废弃，不推荐使用）
interface LanguageModel {
    String chat(String prompt);
}

// 2. 推荐使用的聊天模型接口
interface ChatModel {
    String chat(String userMessage);
}

// 3. 更强大的 ChatModel 接口
interface ChatModel {
    ChatResponse chat(ChatMessage... messages);
    ChatResponse chat(List<ChatMessage> messages);
    ChatResponse chat(ChatRequest chatRequest);
    String chat(String userMessage);  // 便捷方法
}
```

**接口选择建议：**
- **初学者**：使用 `chatModel.chat(String)` 最简单
- **进阶使用**：使用 `ChatModel` 获得更多控制力
- **高级用户**：使用 `chatModel.chat(ChatRequest)` 完全控制

### ChatMessage 类型

在深入了解 `ChatModel` 之前，先了解 `ChatMessage` 的类型：

```java
// 1. 用户消息 - 来自用户或应用的消息
UserMessage userMessage = UserMessage.from("你好");

// 2. AI 消息 - LLM 生成的消息
AiMessage aiMessage = AiMessage.from("你好！有什么可以帮助你的吗？");

// 3. 系统消息 - 设置 LLM 行为的消息
SystemMessage systemMessage = SystemMessage.from("你是一个友好的助手");

// 4. 工具执行结果消息 - 工具执行后返回的消息
ToolExecutionResultMessage toolResult = ToolExecutionResultMessage.from(...);
```

## 创建 ChatModel 实例

### 使用 OpenAI 模型

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

// 基本配置
ChatModel model = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .build();
```

### 高级配置

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.data.message.AiMessage;
import java.time.Duration;

ChatModel model = OpenAiChatModel.builder()
        // 基本配置
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        
        // 参数配置（下一章详细讲解）
        .temperature(0.7)              // 创造性：0.0-2.0，越高越有创意
        .topP(0.9)                    // 核采样
        .maxTokens(1000)               // 最大输出 Token 数
        .timeout(Duration.ofSeconds(60)) // 超时时间
        
        // 日志配置（便于调试）
        .logRequests(true)              // 记录请求
        .logResponses(true)             // 记录响应
        
        .build();
```

### 使用其他模型提供商

```java
// Anthropic Claude
import dev.langchain4j.model.anthropic.AnthropicChatModel;
ChatModel claudeModel = AnthropicChatModel.builder()
        .apiKey(System.getenv("ANTHROPIC_API_KEY"))
        .modelName("claude-3-5-sonnet-20241022")
        .build();

// Google Gemini
import dev.langchain4j.model.google.GeminiChatModel;
ChatModel geminiModel = GeminiChatModel.builder()
        .apiKey(System.getenv("GOOGLE_API_KEY"))
        .modelName("gemini-1.5-pro")
        .build();

// Ollama 本地模型
import dev.langchain4j.model.ollama.OllamaChatModel;
ChatModel ollamaModel = OllamaChatModel.builder()
        .baseUrl("http://localhost:11434")
        .modelName("llama2")
        .build();
```

## 发送消息

### 方式一：简单的字符串消息

最简单的方式，直接发送字符串：

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

ChatModel model = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .build();

String userMessage = "你好，请介绍一下你自己";
String response = model.chat(userMessage);

System.out.println("用户: " + userMessage);
System.out.println("AI: " + response);
```

### 方式二：使用 ChatMessage

使用 `ChatMessage` 类型发送消息，获得更多控制：

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;

ChatModel model = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .build();

// 创建用户消息
UserMessage userMessage = UserMessage.from("你好，请介绍一下你自己");

// 发送消息并接收完整响应
ChatResponse response = model.chat(userMessage);

// 获取 AI 消息
AiMessage aiMessage = response.aiMessage();

System.out.println("用户: " + userMessage.text());
System.out.println("AI: " + aiMessage.text());
```

### 方式三：多消息对话

发送多个消息，构建完整对话上下文：

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.*;
import java.util.ArrayList;
import java.util.List;

ChatModel model = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .build();

// 构建对话历史
List<ChatMessage> messages = new ArrayList<>();

// 添加系统消息
messages.add(SystemMessage.from("你是一个专业的 Java 编程助手"));

// 添加用户消息
messages.add(UserMessage.from("什么是 Java？"));

// 发送对话
ChatResponse response1 = model.chat(messages);
messages.add(response1.aiMessage());

System.out.println("AI: " + response1.aiMessage().text());

// 继续对话
messages.add(UserMessage.from("Java 有哪些特点？"));
ChatResponse response2 = model.chat(messages);

System.out.println("AI: " + response2.aiMessage().text());
```

### 方式四：使用 ChatRequest 完全控制

使用 `ChatRequest` 对象进行完全控制：

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.*;
import java.util.List;

ChatModel model = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .build();

// 构建请求参数
ChatRequestParameters parameters = ChatRequestParameters.builder()
        .temperature(0.7)
        .topP(0.9)
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

// 发送请求
ChatResponse response = model.chat(request);

System.out.println("AI: " + response.aiMessage().text());
```

## 接收响应

### 获取响应文本

最简单的获取响应文本的方式：

```java
ChatModel model = ...;
String response = model.chat("你好");
System.out.println(response);
```

### 获取完整响应信息

获取响应的详细信息，包括元数据：

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.output.TokenUsage;

ChatModel model = ...;
ChatResponse response = model.chat("你好");

// 获取 AI 消息
AiMessage aiMessage = response.aiMessage();
String text = aiMessage.text();

// 获取响应元数据
ChatResponseMetadata metadata = response.metadata();
String modelId = metadata.modelName();           // 使用的模型
String responseId = metadata.id();               // 响应 ID
TokenUsage tokenUsage = metadata.tokenUsage();    // Token 使用情况

// Token 使用详情
int inputTokens = tokenUsage.inputTokenCount();    // 输入 Token 数
int outputTokens = tokenUsage.outputTokenCount();  // 输出 Token 数
int totalTokens = tokenUsage.totalTokenCount();    // 总 Token 数

System.out.println("响应文本: " + text);
System.out.println("模型: " + modelId);
System.out.println("输入 Token: " + inputTokens);
System.out.println("输出 Token: " + outputTokens);
System.out.println("总 Token: " + totalTokens);
```

### 获取完成原因

了解为什么 LLM 停止生成：

```java
import dev.langchain4j.model.output.FinishReason;

ChatResponse response = model.chat("讲一个故事");
FinishReason finishReason = response.metadata().finishReason();

switch (finishReason) {
    case STOP:
        System.out.println("正常结束");
        break;
    case LENGTH:
        System.out.println("达到最大 Token 限制");
        break;
    case CONTENT_FILTER:
        System.out.println("内容被过滤");
        break;
    case TOOL_EXECUTIONS:
        System.out.println("等待工具执行");
        break;
    default:
        System.out.println("其他原因: " + finishReason);
}
```

## 完整示例：简单的聊天应用

### 示例代码

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.util.Scanner;

/**
 * 简单的聊天应用示例
 */
public class SimpleChatApp {

    private final ChatModel model;
    private final Scanner scanner;

    public SimpleChatApp() {
        this.model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .logRequests(true)
                .logResponses(true)
                .build();
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   LangChain4j 简单聊天应用 v1.0    ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println();
        System.out.println("使用提示:");
        System.out.println("  - 直接输入消息与 AI 对话");
        System.out.println("  - 输入 'clear' 清屏");
        System.out.println("  - 输入 'exit' 退出");
        System.out.println();

        int messageCount = 0;

        while (true) {
            System.out.print("[" + (++messageCount) + "] 你: ");
            String input = scanner.nextLine().trim();

            // 处理命令
            if ("exit".equalsIgnoreCase(input)) {
                System.out.println("\n再见！感谢使用。");
                break;
            } else if ("clear".equalsIgnoreCase(input)) {
                for (int i = 0; i < 50; i++) {
                    System.out.println();
                }
                continue;
            } else if (input.isEmpty()) {
                continue;
            }

            // 发送消息
            try {
                long startTime = System.currentTimeMillis();
                String response = model.chat(input);
                long endTime = System.currentTimeMillis();

                System.out.println("[" + messageCount + "] AI: " + response);
                System.out.println("        (耗时: " + (endTime - startTime) + "ms)");
                System.out.println();
            } catch (Exception e) {
                System.err.println("错误: " + e.getMessage());
                System.out.println();
            }
        }

        scanner.close();
    }

    public static void main(String[] args) {
        SimpleChatApp app = new SimpleChatApp();
        app.start();
    }
}
```

### 运行示例

```
╔══════════════════════════════════════╗
║   LangChain4j 简单聊天应用 v1.0    ║
╚══════════════════════════════════════╝

使用提示:
  - 直接输入消息与 AI 对话
  - 输入 'clear' 清屏
  - 输入 'exit' 退出

[1] 你: 你好，我是 Java 初学者
[1] AI: 你好！很高兴认识你。作为一个 Java 初学者，你可以从学习 Java 的基础语法开始，比如变量、数据类型、控制流等。有什么具体的问题我可以帮助你吗？
        (耗时: 1234ms)

[2] 你: Java 有哪些特点？
[2] AI: Java 有以下主要特点：

1. 平台无关性：Java 程序可以"一次编写，到处运行"（Write Once, Run Anywhere）
2. 面向对象：Java 是纯面向对象的编程语言
3. 简单易学：语法相对简单，去除了 C++ 中复杂的概念
4. 健壮性：强类型检查、异常处理机制提高了程序的稳定性
5. 安全性：内置安全机制，如字节码验证
6. 多线程：内置多线程支持，便于开发并发程序
7. 高性能：通过 JIT 编译器实现较高的执行效率

有什么具体想了解的吗？
        (耗时: 2345ms)

[3] 你: exit

再见！感谢使用。
```

## 测试代码示例

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Chat 应用测试
 */
class ChatAppTest {

    private ChatModel model;

    @BeforeEach
    void setUp() {
        model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();
    }

    @Test
    void should_respond_to_greeting() {
        String greeting = "你好";
        String response = model.chat(greeting);
        
        assertNotNull(response);
        assertFalse(response.isEmpty());
        System.out.println("问候响应: " + response);
    }

    @Test
    void should_answer_simple_question() {
        String question = "2 + 2 等于几？";
        String response = model.chat(question);
        
        assertNotNull(response);
        assertTrue(response.contains("4") || response.contains("四"));
        System.out.println("数学问题响应: " + response);
    }

    @Test
    void should_generate_code_when_asked() {
        String request = "写一个 Java 的 Hello World 程序";
        String response = model.chat(request);
        
        assertNotNull(response);
        assertTrue(response.contains("public") || response.contains("class"));
        assertTrue(response.contains("System.out.println"));
        System.out.println("代码生成响应:\n" + response);
    }

    @Test
    void should_follow_instructions() {
        String instruction = "请用一句话介绍 Java";
        String response = model.chat(instruction);
        
        assertNotNull(response);
        // 检查是否是一句话（简单检查句号数量）
        long sentenceCount = response.chars()
                .filter(ch -> ch == '。' || ch == '.' || ch == '!')
                .count();
        assertTrue(sentenceCount <= 2, "响应应该包含最多2个句子，实际: " + sentenceCount);
        System.out.println("一句话介绍: " + response);
    }

    @Test
    void should_handle_empty_message() {
        String emptyMessage = "";
        String response = model.chat(emptyMessage);
        
        assertNotNull(response);
        System.out.println("空消息响应: " + response);
    }

    @Test
    void should_respond_in_chinese_when_asked_in_chinese() {
        String message = "你是谁？";
        String response = model.chat(message);
        
        assertNotNull(response);
        // 验证响应中包含中文字符
        assertTrue(response.matches(".*[\\u4e00-\\u9fa5].*"));
        System.out.println("中文响应: " + response);
    }
}
```

## 实践练习

### 练习 1：统计 Token 使用

修改聊天应用，显示每次对话的 Token 使用情况：

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import java.util.Scanner;

public class TokenTrackingChat {

    private final ChatModel model;
    private final Scanner scanner;
    private int totalInputTokens = 0;
    private int totalOutputTokens = 0;

    public TokenTrackingChat() {
        this.model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println("=== Token 统计聊天应用 ===");
        System.out.println();

        while (true) {
            System.out.print("你: ");
            String input = scanner.nextLine();

            if ("exit".equalsIgnoreCase(input.trim())) {
                break;
            }

            ChatResponse response = model.chat(input);
            TokenUsage usage = response.metadata().tokenUsage();

            totalInputTokens += usage.inputTokenCount();
            totalOutputTokens += usage.outputTokenCount();

            System.out.println("AI: " + response.aiMessage().text());
            System.out.println("本次: 输入=" + usage.inputTokenCount() + 
                             ", 输出=" + usage.outputTokenCount());
            System.out.println("累计: 输入=" + totalInputTokens + 
                             ", 输出=" + totalOutputTokens);
            System.out.println();
        }

        System.out.println("=== Token 使用统计 ===");
        System.out.println("总输入 Token: " + totalInputTokens);
        System.out.println("总输出 Token: " + totalOutputTokens);
        System.out.println("总计: " + (totalInputTokens + totalOutputTokens));
        System.out.println("估算费用: $" + estimateCost(totalInputTokens, totalOutputTokens));
        scanner.close();
    }

    private double estimateCost(int inputTokens, int outputTokens) {
        // GPT-4o-mini 价格（示例，实际价格请查询 OpenAI）
        double inputPrice = 0.00015 / 1000.0;  // 每 1000 tokens 价格
        double outputPrice = 0.0006 / 1000.0;
        return (inputTokens * inputPrice) + (outputTokens * outputPrice);
    }

    public static void main(String[] args) {
        new TokenTrackingChat().start();
    }
}
```

### 练习 2：添加系统提示

让聊天应用支持自定义系统提示：

```java
public class SystemPromptChat {

    private final ChatModel model;
    private final Scanner scanner;
    private String systemPrompt = "你是一个友好的助手";

    public SystemPromptChat() {
        this.model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println("=== 系统提示聊天应用 ===");
        System.out.println("命令:");
        System.out.println("  /set <提示> - 设置系统提示");
        System.out.println("  /show - 显示当前系统提示");
        System.out.println("  /exit - 退出");
        System.out.println();

        while (true) {
            System.out.print("你: ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            if (input.equals("/exit")) {
                break;
            } else if (input.startsWith("/set ")) {
                systemPrompt = input.substring(5);
                System.out.println("系统提示已更新: " + systemPrompt);
                System.out.println();
                continue;
            } else if (input.equals("/show")) {
                System.out.println("当前系统提示: " + systemPrompt);
                System.out.println();
                continue;
            }

            // 使用系统提示发送消息
            ChatResponse response = model.chat(
                SystemMessage.from(systemPrompt),
                UserMessage.from(input)
            );
            System.out.println("AI: " + response.aiMessage().text());
            System.out.println();
        }
        scanner.close();
    }

    public static void main(String[] args) {
        new SystemPromptChat().start();
    }
}
```

### 练习 3：响应时间对比

对比不同模型的响应时间：

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class ModelComparison {

    public static void main(String[] args) {
        String message = "写一首关于编程的诗";

        // GPT-3.5 Turbo
        ChatModel gpt35 = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-3.5-turbo")
                .build();

        // GPT-4o Mini
        ChatModel gpt4oMini = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        System.out.println("=== 模型对比测试 ===");
        System.out.println("消息: " + message);
        System.out.println();

        // 测试 GPT-3.5 Turbo
        long start = System.currentTimeMillis();
        String response35 = gpt35.chat(message);
        long end = System.currentTimeMillis();
        System.out.println("GPT-3.5 Turbo (" + (end - start) + "ms):");
        System.out.println(response35);
        System.out.println();

        // 测试 GPT-4o Mini
        start = System.currentTimeMillis();
        String response4o = gpt4oMini.chat(message);
        end = System.currentTimeMillis();
        System.out.println("GPT-4o Mini (" + (end - start) + "ms):");
        System.out.println(response4o);
    }
}
```

## 总结

### 本章要点

1. **ChatModel 接口层次**
   - `LanguageModel`（废弃）
   - `ChatModel`（推荐初学者）
   - `ChatModel`（提供完整控制）

2. **创建 ChatModel**
   - 基本配置（API Key、模型名称）
   - 高级配置（参数、超时、日志）
   - 支持多种模型提供商

3. **发送消息的方式**
   - 简单字符串：`model.chat(String)`
   - ChatMessage：`model.chat(ChatMessage)`
   - 多消息：`model.chat(List<ChatMessage>)`
   - ChatRequest：`model.chat(ChatRequest)`

4. **接收响应**
   - 获取响应文本
   - 获取完整元数据（模型、Token 使用等）
   - 获取完成原因

5. **完整示例**
   - 构建了一个可运行的聊天应用
   - 包含输入输出、命令处理、错误处理

### 下一步

在下一章节中，我们将深入学习：
- ChatModel 接口的详细方法
- StreamingChatModel 流式模型
- 模型选择策略
- 异常处理和错误管理

### 常见问题

**Q1：ChatModel 和 ChatModel 有什么区别？**

A：`ChatModel` 是简化接口，只接受字符串输入返回字符串输出。`ChatModel` 是更强大的接口，接受 `ChatMessage` 对象，返回包含元数据的 `ChatResponse`。

**Q2：如何选择合适的模型？**

A：考虑以下因素：
- 成本：GPT-3.5 更便宜，GPT-4o 更强大
- 速度：GPT-3.5 更快
- 任务复杂度：简单任务用便宜的模型，复杂任务用强大模型
- 下一章我们将详细介绍。

**Q3：为什么多轮对话中 AI 不记得之前的内容？**

A：LLM 本身是无状态的。要实现多轮对话记忆，需要使用 `ChatMemory`（将在后续章节介绍）。

**Q4：如何处理 API 错误？**

A：可以使用 try-catch 捕获异常，或配置重试机制。我们将在后续章节详细介绍错误处理。

**Q5：logRequests 和 logResponses 有什么作用？**

A：这两个参数会开启请求和响应的日志记录，便于调试和监控。生产环境中可以关闭以提高性能。

## 参考资料

- [LangChain4j ChatModel 文档](https://docs.langchain4j.dev/tutorials/chat-and-language-models)
- [OpenAI 官方文档](https://platform.openai.com/docs)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
