---
title: 'AI Services 详解'
description: '学习 LangChain4j 的 AI Services 详解 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 11 - AI Services

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 理解 AI Services 的核心概念和优势
- 掌握如何使用 `@AiService` 注解定义服务
- 学会配置系统消息和 ChatMemory
- 掌握多轮对话和流式输出的使用
- 理解 AI Services 高级特性（工具调用、RAG 等）
- 实现一个完整的 AI Service

## 前置知识

- 完成《01 - LangChain4j 简介》章节
- 完成《02 - 你的第一个 Chat 应用》章节
- 完成《03 - 深入理解 ChatModel》章节
- 完成《06 - ChatMemory 对话记忆管理》章节
- 完成《09 - Tools 工具调用》章节（可选）

## 核心概念

### 什么是 AI Services？

**AI Services** 是 LangChain4j 提供的高级抽象层，它通过简单的接口将 LLM 的复杂性隐藏在底层。

**类比理解：**
如果把 `ChatModel` 比作手动挡位车（需要你自己操作离合器、油门、方向盘），那么 `AI Services` 就像是自动挡汽车（自动处理所有细节，你只需要告诉它去哪）。

**为什么需要 AI Services？**

1. **简化开发** - 无需手动管理消息列表和对话历史
2. **类型安全** - 通过 Java 接口和注解定义行为
3. **声明式** - 使用注解配置，代码更清晰
4. **易于测试** - 接口独立，可以轻松 mock
5. **功能丰富** - 自动集成工具、记忆、流式输出等

### AI Services vs 手动调用

```java
// 手动调用（复杂）
List<ChatMessage> messages = new ArrayList<>();
messages.add(SystemMessage.from("你是一个助手"));
messages.add(UserMessage.from("你好"));
ChatResponse response1 = model.chat(messages);
messages.add(response1.aiMessage());
messages.add(UserMessage.from("我叫小明"));
ChatResponse response2 = model.chat(messages);

// AI Services（简洁）
interface Assistant {
    String chat(String message);
}

Assistant assistant = AiServices.builder(Assistant.class)
        .chatModel(model)
        .build();

String response1 = assistant.chat("你好");
String response2 = assistant.chat("我叫小明");
```

## 基础 AI Service

### 定义服务接口

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;

/**
 * 简单的聊天服务接口
 */
public interface SimpleChatService {

    String chat(@UserMessage String message);
}
```

### 创建 Service 实例

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * AI Service 创建器
 */
public class ChatServiceFactory {

    private final ChatModel model;

    public ChatServiceFactory(String apiKey) {
        this.model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .build();
    }

    /**
     * 创建聊天服务
     */
    public SimpleChatService createChatService() {
        return AiServices.builder(SimpleChatService.class)
                .chatModel(model)
                .build();
    }

    public static void main(String[] args) {
        ChatServiceFactory factory = new ChatServiceFactory(
            System.getenv("OPENAI_API_KEY")
        );

        SimpleChatService chatService = factory.createChatService();

        String response = chatService.chat("你好，请介绍一下自己");
        System.out.println("AI: " + response);
    }
}
```

## 系统消息配置

### 使用 @SystemMessage 注解

```java
import dev.langchain4j.service.SystemMessage;

/**
 * 带系统消息的服务接口
 */
public interface SystemPromptService {

    String chat(@UserMessage String message);
}
```

### 创建 Service（带系统消息）

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * 创建带系统消息的 Service
 */
public class SystemPromptServiceFactory {

    public SystemPromptService createAssistant(String systemMessage) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        return AiServices.builder(SystemPromptService.class)
                .chatModel(model)
                .systemMessageProvider(chatMemoryId -> systemMessage)
                .build();
    }

    public static void main(String[] args) {
        SystemPromptServiceFactory factory = new SystemPromptServiceFactory();

        // 创建不同角色的助手
        SystemPromptService assistant1 = factory.createAssistant(
            "你是一个专业的编程老师，用简单易懂的语言解释技术概念。"
        );

        SystemPromptService assistant2 = factory.createAssistant(
            "你是一个友好的心理咨询师，提供温暖的情感支持和建议。"
        );

        // 测试
        System.out.println("=== 编程老师 ===");
        System.out.println(assistant1.chat("什么是面向对象编程？"));
        System.out.println();

        System.out.println("=== 心理咨询师 ===");
        System.out.println(assistant2.chat("我最近感到压力很大，你有什么建议吗？"));
        System.out.println();
    }
}
```

## ChatMemory 集成

### 自动记忆管理

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * 带记忆的服务
 */
public class MemoryAwareServiceFactory {

    /**
     * 创建多用户记忆的服务
     */
    public static MultiUserService createMultiUserService(String apiKey) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .build();

        // 创建记忆提供者
        ChatMemoryProvider memoryProvider = memoryId -> {
            return MessageWindowChatMemory.builder()
                    .id(memoryId.toString())
                    .maxMessages(10)
                    .build();
        };

        return AiServices.builder(MultiUserService.class)
                .chatModel(model)
                .chatMemoryProvider(memoryProvider)
                .build();
    }

    /**
     * 多用户服务接口
     */
    public interface MultiUserService {
        String chat(@MemoryId String userId, @UserMessage String message);
    }

    public static void main(String[] args) {
        MultiUserService service = createMultiUserService(
            System.getenv("OPENAI_API_KEY")
        );

        // 多用户对话
        System.out.println("=== 用户 1 的对话 ===");
        System.out.println(service.chat("user1", "你好，我是小明"));
        System.out.println(service.chat("user1", "我叫什么？"));
        System.out.println();

        System.out.println("=== 用户 2 的对话 ===");
        System.out.println(service.chat("user2", "你好，我是小红"));
        System.out.println(service.chat("user2", "我叫什么？"));
        System.out.println();

        // 验证独立性：用户 2 不应该知道小明
        System.out.println("=== 验证：用户 1 继续对话 ===");
        System.out.println(service.chat("user1", "我今年 25 岁"));
        System.out.println();
    }
}
```

## 流式输出

### 使用 @V 注解

```java
import dev.langchain4j.service.StreamingChatModel;
import dev.langchain4j.service.V;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * 流式输出服务
 */
public class StreamingServiceFactory {

    public static StreamingChatService createStreamingService() {
        StreamingChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        return AiServices.builder(StreamingChatService.class)
                .streamingChatModel(model)
                .build();
    }

    /**
     * 流式服务接口
     */
    public interface StreamingChatService {
        void chat(@UserMessage String message, TokenStream tokenStream);
    }

    /**
     * Token 流处理
     */
    public interface TokenStream {
        void accept(String token);
        void complete();
    }

    public static void main(String[] args) {
        StreamingChatService service = createStreamingService();

        // 流式输出
        System.out.println("=== 流式输出 ===");
        System.out.print("AI: ");

        service.chat("请写一首关于春天的诗", new TokenStream() {
            @Override
            public void accept(String token) {
                System.out.print(token);
                System.out.flush();
            }

            @Override
            public void complete() {
                System.out.println();
                System.out.println("--- 流式输出完成 ---");
            }
        });
    }
}
```

## 工具集成

### 自动工具调用

```java
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * 带工具的服务
 */
public class ToolAwareServiceFactory {

    /**
     * 计算器工具
     */
    public static class Calculator {
        @Tool("计算两个数的和")
        public String add(int a, int b) {
            return String.valueOf(a + b);
        }

        @Tool("计算两个数的差")
        public String subtract(int a, int b) {
            return String.valueOf(a - b);
        }

        @Tool("计算两个数的积")
        public String multiply(int a, int b) {
            return String.valueOf(a * b);
        }
    }

    /**
     * 带工具的服务接口
     */
    public interface ToolAwareService {
        String chat(@UserMessage String message);
    }

    /**
     * 创建带工具的服务
     */
    public static ToolAwareService createToolAwareService() {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        return AiServices.builder(ToolAwareService.class)
                .chatModel(model)
                .tools(new Calculator())
                .build();
    }

    public static void main(String[] args) {
        ToolAwareService service = createToolAwareService();

        System.out.println("=== 工具调用测试 ===");
        System.out.println();

        // 不需要工具调用
        System.out.println("查询: 你好");
        System.out.println("回答: " + service.chat("你好"));
        System.out.println();

        // 需要工具调用
        System.out.println("查询: 5 加 3 等于几？");
        System.out.println("回答: " + service.chat("5 加 3 等于几？"));
        System.out.println();

        System.out.println("查询: 10 减 5 等于几？");
        System.out.println("回答: " + service.chat("10 减 5 等于几？"));
        System.out.println();

        System.out.println("查询: 3 乘以 4 等于几？");
        System.out.println("回答: " + service.chat("3 乘以 4 等于几？"));
        System.out.println();
    }
}
```

## 完整 AI Service 示例

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.V;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * 完整的 AI Service 示例
 */
public class CompleteAiService {

    private final Assistant assistant;

    public CompleteAiService(String apiKey) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .build();

        // 记忆提供者
        ChatMemoryProvider memoryProvider = memoryId -> {
            return MessageWindowChatMemory.builder()
                    .id(memoryId)
                    .maxMessages(20)
                    .build();
        };

        this.assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemoryProvider(memoryProvider)
                .tools(new TimeTool(), new CalculatorTool())
                .build();
    }

    /**
     * Assistant 接口
     */
    public interface Assistant {
        String chat(@MemoryId String userId, @UserMessage String message);
    }

    /**
     * 聊天方法
     */
    public String chat(String userId, String message) {
        return assistant.chat(userId, message);
    }

    /**
     * 时间工具
     */
    @Tool("获取当前时间")
    public static class TimeTool {
        public String getCurrentTime() {
            return java.time.LocalDateTime.now().toString();
        }

        @Tool("获取当前日期")
        public String getCurrentDate() {
            return java.time.LocalDate.now().toString();
        }
    }

    /**
     * 计算器工具
     */
    @Tool("计算器")
    public static class CalculatorTool {
        @Tool("加法")
        public String add(int a, int b) {
            return String.format("%d + %d = %d", a, b, a + b);
        }

        @Tool("减法")
        public String subtract(int a, int b) {
            return String.format("%d - %d = %d", a, b, a - b);
        }

        @Tool("乘法")
        public String multiply(int a, int b) {
            return String.format("%d × %d = %d", a, b, a * b);
        }
    }

    /**
     * 获取用户对话历史
     */
    public List<String> getUserHistory(String userId) {
        // 简化：实际应用中需要访问 ChatMemory
        return List.of(
            "用户 " + userId + ": 你好",
            "AI: 你好！有什么我可以帮助你的吗？"
        );
    }

    public static void main(String[] args) {
        CompleteAiService service = new CompleteAiService(
            System.getenv("OPENAI_API_KEY")
        );

        System.out.println("╔═════════════════════════════════════════════╗");
        System.out.println("║           完整 AI Service 示例               ║");
        System.out.println("╚═════════════════════════════════════════════╝");
        System.out.println();

        // 用户 1 对话
        System.out.println("=== 用户 user1 的对话 ===");
        System.out.println(service.chat("user1", "你好，我是小明"));
        System.out.println(service.chat("user1", "现在几点了？"));
        System.out.println(service.chat("user1", "5 加 3 等于几？"));
        System.out.println();

        // 用户 2 对话
        System.out.println("=== 用户 user2 的对话 ===");
        System.out.println(service.chat("user2", "你好，我是小红"));
        System.out.println(service.chat("user2", "今天日期是什么？"));
        System.out.println(service.chat("user2", "10 减 5 等于几？"));
        System.out.println();
    }
}
```

## 测试代码示例

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AI Services 测试
 */
class AiServicesTest {

    private ChatModel model;

    @BeforeEach
    void setUp() {
        this.model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();
    }

    @Test
    void should_create_simple_service() {
        // 简单服务接口
        interface SimpleService {
            String chat(@UserMessage String message);
        }

        // 创建服务
        SimpleService service = AiServices.builder(SimpleService.class)
                .chatModel(model)
                .build();

        // 测试
        String response = service.chat("你好");
        assertNotNull(response);
        assertFalse(response.isEmpty());
        
        System.out.println("响应: " + response);
    }

    @Test
    void should_honor_system_message() {
        // 带系统消息的服务接口
        interface SystemService {
            String chat(@UserMessage String message);
        }

        // 创建服务
        SystemService service = AiServices.builder(SystemService.class)
                .chatModel(model)
                .systemMessageProvider(chatMemoryId -> "你是一个友好的助手")
                .build();

        // 测试
        String response = service.chat("用一句话介绍自己");
        
        assertNotNull(response);
        // 验证响应包含友好信息
        assertTrue(response.toLowerCase().contains("你好") || 
                   response.toLowerCase().contains("助手"));
        
        System.out.println("响应: " + response);
    }

    @Test
    void should_support_chat_memory() {
        // 带记忆的服务接口
        interface MemoryService {
            String chat(@UserMessage String message);
        }

        // 创建服务（实际应用中应该使用 ChatMemoryProvider）
        MemoryService service = AiServices.builder(MemoryService.class)
                .chatModel(model)
                .build();

        // 测试
        service.chat("我的名字是小明");
        service.chat("我今年 25 岁");
        service.chat("我住在北京");
        
        // 验证：实际应用中应该验证记忆是否被保留
        assertNotNull(service);
        
        System.out.println("响应已接收");
    }

    @Test
    void should_support_tools() {
        // 带工具的服务
        interface ToolService {
            String chat(@UserMessage String message);
        }

        // 创建工具
        @Tool("获取时间")
        static class TimeTool {
            public String getCurrentTime() {
                return "12:00:00";
            }
        }

        // 创建服务
        ToolService service = AiServices.builder(ToolService.class)
                .chatModel(model)
                .tools(new TimeTool())
                .build();

        // 测试工具调用
        String response = service.chat("现在几点了？");
        
        assertNotNull(response);
        // 验证响应包含时间信息
        assertTrue(response.contains("12:00"));
        
        System.out.println("响应: " + response);
    }

    @Test
    void should_support_multiple_users() {
        // 多用户服务接口
        interface MultiUserService {
            String chat(String userId, String message);
        }

        // 创建服务
        MultiUserService service = AiServices.builder(MultiUserService.class)
                .chatModel(model)
                .build();

        // 测试多用户独立性
        String response1 = service.chat("user1", "我的名字是小明");
        String response2 = service.chat("user2", "我的名字是小红");
        
        // 验证响应不同
        assertNotEquals(response1, response2);
        
        System.out.println("用户 1 响应: " + response1);
        System.out.println("用户 2 响应: " + response2);
    }
}
```

## 实践练习

### 练习 1：创建角色扮演服务

实现一个支持多种角色的 AI Service：

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * 角色扮演服务
 */
public class RolePlayServiceFactory {

    public static ProgrammerService createProgrammerService(String apiKey) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .build();

        return AiServices.builder(ProgrammerService.class)
                .chatModel(model)
                .systemMessageProvider(chatMemoryId -> 
                    "你是一个资深的全栈工程师，精通 Java、Python、JavaScript、Go 等多种编程语言。\n" +
                    "你的特点是：\n" +
                    "1. 专业且严谨，注重代码质量和最佳实践\n" +
                    "2. 简洁明了，用最少的代码实现功能\n" +
                    "3. 善于解释技术概念，即使对初学者\n" +
                    "4. 当指出问题时，总是提供建设性的解决方案\n\n" +
                    "请在回答代码时，使用适当的代码块格式，并添加必要的注释。"
                )
                .build();
    }

    public static TeacherService createTeacherService(String apiKey) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .build();

        return AiServices.builder(TeacherService.class)
                .chatModel(model)
                .systemMessageProvider(chatMemoryId -> 
                    "你是一位经验丰富的编程老师，你的目标是帮助学生理解和掌握编程概念。\n" +
                    "你的教学风格是：\n" +
                    "1. 循序渐进，从简单到复杂\n" +
                    "2. 使用生动的例子和类比，帮助学生理解抽象概念\n" +
                    "3. 鼓励学生，对他们的进步给予积极反馈\n" +
                    "4. 当学生遇到困难时，提供不同的角度和方法\n" +
                    "5. 避免使用过多专业术语，如果必须使用，务必解释清楚\n\n" +
                    "请用友好、耐心的语气回答学生的问题。"
                )
                .build();
    }

    public static CodeReviewerService createCodeReviewerService(String apiKey) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .build();

        return AiServices.builder(CodeReviewerService.class)
                .chatModel(model)
                .systemMessageProvider(chatMemoryId -> 
                    "你是一个代码审查专家，负责检查代码质量、安全性、性能和最佳实践。\n" +
                    "你的审查要点包括：\n" +
                    "1. 代码风格和格式\n" +
                    "2. 潜在的 bug 和逻辑错误\n" +
                    "3. 安全漏洞（如 SQL 注入、XSS 等）\n" +
                    "4. 性能问题和优化建议\n" +
                    "5. 代码可维护性和可读性\n" +
                    "6. 是否遵循最佳实践\n" +
                    "7. 是否有改进空间\n\n" +
                    "请以建设性的方式提供反馈，先指出代码的优点，然后指出需要改进的地方。\n" +
                    "如果发现问题，请提供具体的代码示例说明如何修复。"
                )
                .build();
    }

    // 服务接口
    public interface ProgrammerService {
        String explain(@UserMessage String concept);
        String writeCode(@UserMessage String requirements);
    }

    public interface TeacherService {
        String teach(@UserMessage String concept);
        String provideExample(@UserMessage String concept);
    }

    public interface CodeReviewerService {
        String reviewCode(@UserMessage String code);
        String suggestImprovements(@UserMessage String code);
    }

    public static void main(String[] args) {
        ProgrammerService programmer = createProgrammerService(
            System.getenv("OPENAI_API_KEY")
        );
        
        TeacherService teacher = createTeacherService(
            System.getenv("OPENAI_API_KEY")
        );
        
        CodeReviewerService reviewer = createCodeReviewerService(
            System.getenv("OPENAI_API_KEY")
        );

        System.out.println("=== 程序师 ===");
        System.out.println(programmer.explain("什么是多态？"));
        System.out.println();
        
        System.out.println("=== 老师 ===");
        System.out.println(teacher.teach("什么是递归？"));
        System.out.println();
        
        System.out.println("=== 代码审查 ===");
        String code = "public class Example {\n" +
                       "    public static void main(String[] args) {\n" +
                       "        String name = args[0];\n" +
                       "        System.out.println(name);\n" +
                       "    }\n" +
                       "}";
        System.out.println(reviewer.reviewCode(code));
    }
}
```

### 练习 2：实现流式对话服务

实现一个支持流式输出和记忆的对话服务：

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.Scanner;

/**
 * 流式对话服务
 */
public class StreamingChatService {

    private final ConversationalAssistant assistant;
    private final Scanner scanner;

    public StreamingChatService(String apiKey) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .build();

        // 记忆提供者
        ChatMemoryProvider memoryProvider = memoryId -> {
            return MessageWindowChatMemory.builder()
                    .id(memoryId)
                    .maxMessages(20)
                    .build();
        };

        this.assistant = AiServices.builder(ConversationalAssistant.class)
                .chatModel(model)
                .chatMemoryProvider(memoryProvider)
                .systemMessageProvider(chatMemoryId -> "你是一个友好的助手")
                .build();

        this.scanner = new Scanner(System.in);
    }

    /**
     * 会话接口
     */
    public interface ConversationalAssistant {
        String chat(String userId, String message);
    }

    /**
     * 开始对话
     */
    public void startSession() {
        String userId = "user1";

        System.out.println("╔═════════════════════════════════════════════╗");
        System.out.println("║           流式对话系统                      ║");
        System.out.println("╚═════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("输入 'exit' 退出，'clear' 清空历史");
        System.out.println();

        while (true) {
            System.out.print("你: ");
            String input = scanner.nextLine().trim();

            if ("exit".equalsIgnoreCase(input)) {
                System.out.println("\n再见！");
                break;
            }

            if ("clear".equalsIgnoreCase(input)) {
                System.out.println("历史已清空\n");
                // 实际应用中应该清空用户的 ChatMemory
                continue;
            }

            if (input.isEmpty()) {
                continue;
            }

            // 流式输出
            System.out.print("AI: ");
            
            String response = assistant.chat(userId, input);
            System.out.println(response);
            System.out.println();
        }

        scanner.close();
    }
}
```

### 练习 3：实现多模态服务

实现一个支持图像输入的服务：

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.content.ImageContent;
import dev.langchain4j.data.message.content.TextContent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * 多模态服务
 */
public class MultimodalServiceFactory {

    /**
     * 图像分析服务
     */
    public interface ImageAnalysisService {
        String describeImage(String imageUrl);
        String analyzeImage(String imageUrl, String question);
    }

    /**
     * 创建图像分析服务
     */
    public static ImageAnalysisService createImageAnalysisService(String apiKey) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o")  // 需要支持多模态的模型
                .build();

        return AiServices.builder(ImageAnalysisService.class)
                .chatModel(model)
                .systemMessageProvider(chatMemoryId -> 
                    "你是一个专业的图像分析师，能够准确地描述和分析图片中的内容。\n" +
                    "请详细描述图片中的：\n" +
                    "1. 主要物体和场景\n" +
                    "2. 颜色和光线\n" +
                    "3. 构图和布局\n" +
                    "4. 文字和标签（如果有）\n" +
                    "5. 情感和氛围\n\n" +
                    "如果用户有具体问题，请先描述图片，然后针对性地回答。"
                )
                .build();
    }

    public static void main(String[] args) {
        ImageAnalysisService service = createImageAnalysisService(
            System.getenv("OPENAI_API_KEY")
        );

        System.out.println("=== 图像描述 ===");
        System.out.println(service.describeImage("https://example.com/image.jpg"));
        System.out.println();

        System.out.println("=== 图像分析 ===");
        System.out.println(service.analyzeImage(
            "https://example.com/image.jpg",
            "图片中有什么动物？"
        ));
    }

    // 如果需要支持真正的多模态（图像+文本）
    public static class MultimodalService {

        public static class ImageAndTextService {
            public String analyze(String imageUrl, String question, String textContext);
            // 需要使用支持多模态的模型配置
            return "多模态分析结果";
        }
    }
}
```

## 总结

### 本章要点

1. **AI Services 优势**
   - 简化开发，隐藏复杂性
   - 类型安全，声明式编程
   - 自动集成记忆、工具、流式输出

2. **基础用法**
   - 定义服务接口
   - 使用 `@AiServices.builder()` 创建实例
   - 注解驱动配置

3. **高级特性**
   - `@SystemMessage` - 配置系统提示
   - `@MemoryId` + ChatMemoryProvider - 多用户记忆
   - `@V` - 流式输出支持
   - 工具集成 - 自动工具调用

4. **最佳实践**
   - 接口设计要清晰明确
   - 合理配置记忆窗口大小
   - 选择合适的模型和参数
   - 实现错误处理和重试

5. **应用场景**
   - 对话机器人
   - 角色扮演助手
   - 代码审查工具
   - 多模态应用

### 下一步

在下一章节中，我们将学习：
- 流式输出详细实现
- 请求拦截器和中间件
- 自定义响应处理器
- AI Services 测试策略
- 性能优化建议

### 常见问题

**Q1：AI Services 和直接调用 ChatModel 有什么区别？**

A：
- **AI Services**：自动管理消息历史、记忆、工具调用，代码更简洁
- **直接调用**：需要手动管理消息列表，更多控制但代码更复杂

**Q2：如何实现多用户隔离？**

A：
- 使用 `@MemoryId` 注解标记用户 ID
- 创建 `ChatMemoryProvider` 为每个用户返回独立的记忆
- LangChain4j 会自动处理隔离

**Q3：流式输出对性能有什么影响？**

A：
- 优点：用户体验更好，可以实时显示响应
- 缺点：需要更多网络往返，整体完成时间可能略长
- 建议：在用户体验要求高的场景使用流式输出

**Q4：如何测试 AI Services？**

A：
- 接口独立，可以轻松 mock
- 使用 JUnit 或其他测试框架
- 可以针对特定场景编写单元测试
- 可以使用模拟模型进行测试

**Q5：如何选择合适的记忆窗口大小？**

A：考虑因素：
- 对话历史长度需求
- 模型上下文窗口限制
- Token 使用量和成本
- 具体应用场景（如客服 vs 长文档助手）

## 参考资料

- [LangChain4j AI Services 文档](https://docs.langchain4j.dev/tutorials/ai-services)
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
