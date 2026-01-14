---
title: 'ChatMessage 类型详解'
description: '学习 LangChain4j 的 ChatMessage 类型详解 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 05 - ChatMessage 类型详解

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 理解 ChatMessage 接口及其实现
- 掌握 UserMessage、AiMessage、SystemMessage 的使用
- 了解 ToolExecutionResultMessage 的作用
- 学会组合不同类型的消息构建对话
- 掌握多模态消息的处理

## 前置知识

- 完成《01 - LangChain4j 简介》章节
- 完成《02 - 你的第一个 Chat 应用》章节
- 完成《03 - 深入理解 ChatModel》章节
- 完成《04 - 模型参数配置》章节

## 核心概念

### ChatMessage 接口层次

`ChatMessage` 是所有消息类型的基接口，代表与 LLM 交互的任何消息。

```java
// ChatMessage 接口层次
interface ChatMessage
    ↓
├── UserMessage         // 用户消息
├── AiMessage           // AI 生成的消息
├── SystemMessage       // 系统消息
├── ToolExecutionResultMessage  // 工具执行结果消息
└── CustomMessage       // 自定义消息（Ollama 支持）
```

**类比理解：**
如果把与 LLM 的对话比作一场讨论会，那么：
- **UserMessage** = 你说的话
- **AiMessage** = LLM 说的回答
- **SystemMessage** = 会场规则或主持人指示
- **ToolExecutionResultMessage** = 工具助手执行任务后的报告

### 消息类型概览

| 消息类型 | 来源 | 作用 | 使用场景 |
|---------|------|------|----------|
| UserMessage | 用户或应用 | 发送问题或请求 | 对话、问答、任务描述 |
| AiMessage | LLM 生成 | AI 的回答或响应 | 接收 LLM 输出、对话历史 |
| SystemMessage | 开发者 | 设置 LLM 行为规则 | 角色定义、行为约束、风格指导 |
| ToolExecutionResultMessage | 工具执行 | 返回工具执行结果 | 工具调用、函数执行 |
| CustomMessage | 自定义 | 扩展消息类型 | 特殊场景（Ollama） |

## UserMessage 详解

### 基本用法

`UserMessage` 代表来自用户或应用的消息。

```java
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

// 创建简单用户消息
UserMessage userMessage = UserMessage.from("你好");

// 或使用静态工厂方法
UserMessage message = UserMessage.from("什么是 Java？");

ChatModel model = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .build();

String response = model.chat(userMessage.singleText());
System.out.println(response);
```

### UserMessage 的属性

```java
import dev.langchain4j.data.message.UserMessage;

UserMessage message = UserMessage.from("你好，我叫小明");

// 获取文本内容
String text = message.singleText();

// 获取用户名称（可选）
String name = message.name();  // 返回 "小明"

// 获取额外属性
Map<String, Object> attributes = message.attributes();
```

**属性说明：**
- `text()` - 消息的文本内容
- `name()` - 用户名称（可选，用于区分不同用户）
- `attributes()` - 额外属性，不发送给模型，但存储在 ChatMemory 中

### 多模态 UserMessage

`UserMessage` 可以包含多种内容类型：

```java
import dev.langchain4j.data.message.*;
import dev.langchain4j.data.message.content.*;

// 1. 纯文本
UserMessage textOnly = UserMessage.from("你好");

// 2. 文本 + 图像
UserMessage textAndImage = UserMessage.from(
    TextContent.from("描述这张图片"),
    ImageContent.from("https://example.com/cat.jpg")
);

// 3. 多种内容组合
UserMessage multiModal = UserMessage.from(
    TextContent.from("分析这个文件"),
    ImageContent.from("screenshot.png"),
    AudioContent.from("audio.mp3")
);
```

### 实际应用示例

```java
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class UserMessageExample {

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        // 场景 1：简单问题
        UserMessage question = UserMessage.from("Java 是什么？");
        ChatResponse response1 = model.chat(question);
        System.out.println("Q: " + question.singleText());
        System.out.println("A: " + response1.aiMessage().text());
        System.out.println();

        // 场景 2：带用户名
        UserMessage withName = UserMessage.from(
            "小明",  // 用户名
            "介绍一下你自己"
        );
        ChatResponse response2 = model.chat(withName);
        System.out.println("用户 [" + withName.name() + "]: " + withName.singleText());
        System.out.println("AI: " + response2.aiMessage().text());
        System.out.println();

        // 场景 3：多模态
        UserMessage multiModal = UserMessage.from(
            TextContent.from("这张图片显示了什么？"),
            ImageContent.from("https://example.com/image.jpg")
        );
        ChatResponse response3 = model.chat(multiModal);
        System.out.println("Q: " + multiModal.singleText());
        System.out.println("A: " + response3.aiMessage().text());
    }
}
```

## AiMessage 详解

### 基本用法

`AiMessage` 代表 LLM 生成的响应消息。

```java
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

ChatModel model = ...;

// 发送消息并接收 AiMessage
AiMessage aiMessage = model.chat("你好").aiMessage();

// 获取 AI 的文本响应
String text = aiMessage.text();

// 获取工具执行请求（如果有）
if (aiMessage.hasToolExecutionRequests()) {
    var requests = aiMessage.toolExecutionRequests();
}

// 获取思考内容（如果模型支持）
if (aiMessage.hasThinking()) {
    String thinking = aiMessage.thinking();
}
```

### AiMessage 的属性

```java
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.tool.ToolExecutionRequest;
import java.util.List;

AiMessage aiMessage = ...;

// 1. 文本内容
String text = aiMessage.text();

// 2. 思考内容（Chain of Thought）
String thinking = aiMessage.thinking();

// 3. 工具执行请求
if (aiMessage.hasToolExecutionRequests()) {
    List<ToolExecutionRequest> toolRequests = 
        aiMessage.toolExecutionRequests();
    for (ToolExecutionRequest request : toolRequests) {
        System.out.println("工具: " + request.name());
        System.out.println("参数: " + request.arguments());
    }
}

// 4. 额外属性
Map<String, Object> attributes = aiMessage.attributes();
```

### 多轮对话中的 AiMessage

```java
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import java.util.ArrayList;
import java.util.List;

public class ConversationExample {

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        // 构建对话历史
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from("你是一个友好的助手"));

        // 第一轮对话
        UserMessage user1 = UserMessage.from("你好");
        messages.add(user1);
        AiMessage ai1 = model.chat(messages).aiMessage();
        messages.add(ai1);
        
        System.out.println("用户: " + user1.singleText());
        System.out.println("AI: " + ai1.text());
        System.out.println();

        // 第二轮对话
        UserMessage user2 = UserMessage.from("我叫什么？");
        messages.add(user2);
        AiMessage ai2 = model.chat(messages).aiMessage();
        messages.add(ai2);
        
        System.out.println("用户: " + user2.singleText());
        System.out.println("AI: " + ai2.text());
        System.out.println();

        // AI 记得了之前的信息
        System.out.println("对话历史消息数: " + messages.size());
    }
}
```

### AiMessage 与工具调用

```java
import dev.langchain4j.data.message.*;
import dev.langchain4j.data.message.tool.ToolExecutionRequest;
import dev.langchain4j.model.chat.ChatModel;
import java.util.List;

public class ToolCallExample {

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .tools(new Calculator())
                .build();

        UserMessage message = UserMessage.from("2 + 2 等于几？");
        AiMessage aiMessage = model.chat(message).aiMessage();

        if (aiMessage.hasToolExecutionRequests()) {
            List<ToolExecutionRequest> requests = 
                aiMessage.toolExecutionRequests();
            for (ToolExecutionRequest request : requests) {
                System.out.println("AI 想要调用工具: " + request.name());
                System.out.println("参数: " + request.arguments());
                
                // 执行工具
                String result = executeTool(request);
                
                // 创建工具执行结果消息
                ToolExecutionResultMessage toolResult = 
                    ToolExecutionResultMessage.from(
                        request.id(),
                        request.name(),
                        result
                    );
                
                // 继续对话
                AiMessage finalResponse = model.chat(
                    message,
                    aiMessage,
                    toolResult
                ).aiMessage();
                
                System.out.println("AI: " + finalResponse.text());
            }
        } else {
            System.out.println("AI: " + aiMessage.text());
        }
    }
    
    private static String executeTool(ToolExecutionRequest request) {
        // 实现工具执行逻辑
        if ("add".equals(request.name())) {
            // 解析参数并计算
            return "4";
        }
        return "未知的工具";
    }
    
    static class Calculator {
        @Tool
        String add(int a, int b) {
            return String.valueOf(a + b);
        }
    }
}
```

## SystemMessage 详解

### 基本用法

`SystemMessage` 用于设置 LLM 的行为规则、角色和风格。

```java
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

ChatModel model = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .build();

// 设置系统消息
SystemMessage systemMsg = SystemMessage.from(
    "你是一个专业的 Java 编程老师"
);

ChatModel modelWithSystem = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .build();

// 通过 ChatModel 使用系统消息
import dev.langchain4j.model.chat.ChatModel;
ChatModel chatModel = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .build();

ChatResponse response = chatModel.chat(
    systemMsg,
    UserMessage.from("什么是 Java？")
);

System.out.println(response.aiMessage().text());
```

### SystemMessage 的最佳实践

#### 1. 角色定义

```java
SystemMessage roleDefinition = SystemMessage.from(
    "你是一个经验丰富的软件架构师，" +
    "擅长帮助开发者设计可扩展的系统架构。"
);
```

#### 2. 行为约束

```java
SystemMessage constraints = SystemMessage.from(
    "回答时遵循以下规则：\n" +
    "1. 使用简洁明确的语言\n" +
    "2. 每个回答不超过 200 字\n" +
    "3. 避免使用技术术语，除非必要\n" +
    "4. 如果不确定，直接说不知道"
);
```

#### 3. 风格指导

```java
SystemMessage styleGuide = SystemMessage.from(
    "你的回答风格：\n" +
    "- 友好而专业\n" +
    "- 使用示例说明概念\n" +
    "- 分点列举关键信息\n" +
    "- 以鼓励的话语结束"
);
```

#### 4. 输出格式

```java
SystemMessage formatGuide = SystemMessage.from(
    "所有回答必须使用以下格式：\n" +
    "【要点】\n" +
    "- 第一点\n" +
    "- 第二点\n" +
    "【总结】\n" +
    "一句话总结"
);
```

### SystemMessage 安全注意事项

**重要：** 不要让用户能够修改或注入 `SystemMessage`！

```java
// ❌ 不安全 - 允许用户直接使用系统消息
public class UnsafeChat {

    public String chat(String systemMessage, String userMessage) {
        return model.chat(
            SystemMessage.from(systemMessage),  // 用户可以注入
            UserMessage.from(userMessage)
        ).aiMessage().text();
    }
}

// ✅ 安全 - 系统消息固定
public class SafeChat {

    private static final SystemMessage FIXED_SYSTEM = 
        SystemMessage.from("你是一个友好的助手");

    public String chat(String userMessage) {
        return model.chat(
            FIXED_SYSTEM,  // 系统消息固定
            UserMessage.from(userMessage)
        ).aiMessage().text();
    }
}
```

### SystemMessage 实际应用

```java
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.util.List;

public class SystemMessageExample {

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        // 场景 1：技术顾问
        SystemMessage techConsultant = SystemMessage.from(
            "你是一个技术顾问，专注于帮助用户解决技术问题。" +
            "你的回答应该：\n" +
            "1. 准确且实用\n" +
            "2. 包含代码示例\n" +
            "3. 解释技术概念\n" +
            "4. 提供最佳实践"
        );

        ChatResponse response1 = model.chat(
            techConsultant,
            UserMessage.from("如何在 Java 中创建线程？")
        );
        System.out.println("技术顾问回答:\n" + response1.aiMessage().text());
        System.out.println();

        // 场景 2：创意写作助手
        SystemMessage creativeAssistant = SystemMessage.from(
            "你是一个创意写作助手，擅长创作各种类型的文字。" +
            "你的风格：\n" +
            "- 富有想象力和感染力\n" +
            "- 使用生动的比喻和形象的语言\n" +
            "- 注重情感表达\n" +
            "- 文字优美流畅"
        );

        ChatResponse response2 = model.chat(
            creativeAssistant,
            UserMessage.from("写一段关于春天的描述")
        );
        System.out.println("创意写作:\n" + response2.aiMessage().text());
    }
}
```

## ToolExecutionResultMessage 详解

### 基本用法

`ToolExecutionResultMessage` 用于将工具执行的结果返回给 LLM。

```java
import dev.langchain4j.data.message.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;

// 创建工具执行结果消息
ToolExecutionResultMessage resultMessage = 
    ToolExecutionResultMessage.from(
        "call_123",           // 工具调用 ID
        "get_weather",       // 工具名称
        "北京今天晴，25°C"    // 执行结果
    );

// 或使用 ToolExecutionRequest 对象
ToolExecutionRequest request = ToolExecutionRequest.builder()
        .id("call_123")
        .name("get_weather")
        .arguments("{\"city\": \"北京\"}")
        .build();

ToolExecutionResultMessage result = ToolExecutionResultMessage.from(
    request,
    "北京今天晴，25°C"
);
```

### 完整的工具调用流程

```java
import dev.langchain4j.data.message.*;
import dev.langchain4j.data.message.tool.ToolExecutionRequest;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.util.List;

public class ToolCallFlow {

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .tools(new WeatherService())
                .build();

        // 1. 用户请求
        UserMessage userMessage = UserMessage.from("北京今天天气怎么样？");
        
        // 2. 发送给 LLM
        AiMessage aiMessage = model.chat(userMessage).aiMessage();
        
        // 3. 检查是否需要调用工具
        if (aiMessage.hasToolExecutionRequests()) {
            List<ToolExecutionRequest> requests = 
                aiMessage.toolExecutionRequests();
            
            for (ToolExecutionRequest request : requests) {
                System.out.println("LLM 请求调用工具: " + request.name());
                
                // 4. 执行工具
                String toolResult = executeTool(request);
                
                // 5. 创建工具执行结果消息
                ToolExecutionResultMessage toolResultMessage = 
                    ToolExecutionResultMessage.from(
                        request.id(),
                        request.name(),
                        toolResult
                    );
                
                // 6. 将结果返回给 LLM
                AiMessage finalResponse = model.chat(
                    userMessage,
                    aiMessage,
                    toolResultMessage
                ).aiMessage();
                
                System.out.println("\n最终回答: " + finalResponse.text());
            }
        }
    }
    
    private static String executeTool(ToolExecutionRequest request) {
        WeatherService service = new WeatherService();
        
        if ("get_weather".equals(request.name())) {
            return service.getWeather(extractCity(request.arguments()));
        }
        
        return "未知工具";
    }
    
    private static String extractCity(String arguments) {
        // 解析 JSON 参数
        return "北京"; // 简化示例
    }
    
    static class WeatherService {
        @Tool
        String getWeather(String city) {
            return city + "今天晴，25°C";
        }
    }
}
```

## 消息组合使用

### 单消息场景

```java
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

ChatModel model = ...;

// 只发送用户消息
ChatResponse response = model.chat(
    UserMessage.from("你好")
);

System.out.println(response.aiMessage().text());
```

### 多消息对话

```java
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import java.util.List;

ChatModel model = ...;

// 构建完整对话历史
List<ChatMessage> conversation = List.of(
    SystemMessage.from("你是一个数学老师"),
    UserMessage.from("什么是勾股定理？"),
    AiMessage.from("勾股定理是直角三角形的三边关系..."),
    UserMessage.from("能举个例子吗？")
);

ChatResponse response = model.chat(conversation);

System.out.println(response.aiMessage().text());
```

### 消息顺序很重要

```java
// ✅ 正确的顺序
List<ChatMessage> correctOrder = List.of(
    SystemMessage.from("你是一个助手"),
    UserMessage.from("你好"),
    AiMessage.from("你好！有什么可以帮助你的吗？"),
    UserMessage.from("介绍一下 Java")
);

// ❌ 错误的顺序（系统消息应该在开始）
List<ChatMessage> wrongOrder = List.of(
    UserMessage.from("你好"),
    SystemMessage.from("你是一个助手"),  // 系统消息不在开始
    AiMessage.from("你好！有什么可以帮助你的吗？")
);
```

### 消息清理和优化

```java
import dev.langchain4j.data.message.*;
import java.util.ArrayList;
import java.util.List;

public class MessageOptimizer {

    /**
     * 保留最近的 N 条消息
     */
    public static List<ChatMessage> keepRecentMessages(
            List<ChatMessage> messages, 
            int count) {
        
        // 保留系统消息
        List<ChatMessage> result = new ArrayList<>();
        for (ChatMessage msg : messages) {
            if (msg instanceof SystemMessage) {
                result.add(msg);
            }
        }
        
        // 添加最近的 N-1 条非系统消息
        int toKeep = Math.min(count, messages.size());
        int start = Math.max(0, messages.size() - toKeep);
        
        for (int i = start; i < messages.size(); i++) {
            if (!(messages.get(i) instanceof SystemMessage)) {
                result.add(messages.get(i));
            }
        }
        
        return result;
    }

    /**
     * 清理工具执行请求（只保留结果）
     */
    public static List<ChatMessage> cleanToolMessages(
            List<ChatMessage> messages) {
        
        List<ChatMessage> cleaned = new ArrayList<>();
        
        for (ChatMessage message : messages) {
            if (message instanceof AiMessage aiMsg) {
                // 只保留 AiMessage 的文本，移除工具请求
                cleaned.add(AiMessage.from(aiMsg.text()));
            } else {
                cleaned.add(message);
            }
        }
        
        return cleaned;
    }

    /**
     * 去除连续的重复消息
     */
    public static List<ChatMessage> removeConsecutiveDuplicates(
            List<ChatMessage> messages) {
        
        List<ChatMessage> result = new ArrayList<>();
        String lastText = null;
        
        for (ChatMessage message : messages) {
            String currentText = extractText(message);
            
            if (!currentText.equals(lastText)) {
                result.add(message);
                lastText = currentText;
            }
        }
        
        return result;
    }
    
    private static String extractText(ChatMessage message) {
        if (message instanceof UserMessage) {
            return ((UserMessage) message).text();
        } else if (message instanceof AiMessage) {
            return ((AiMessage) message).text();
        } else if (message instanceof SystemMessage) {
            return ((SystemMessage) message).text();
        }
        return "";
    }
}
```

## 多模态消息处理

### 内容类型

LangChain4j 支持多种内容类型：

```java
import dev.langchain4j.data.message.content.*;

// 1. 文本内容
TextContent textContent = TextContent.from("这是一段文本");

// 2. 图像内容 - URL
ImageContent imageFromUrl = ImageContent.from(
    "https://example.com/image.jpg"
);

// 3. 图像内容 - Base64
byte[] imageBytes = ...;  // 读取图像文件
String base64Image = Base64.getEncoder().encodeToString(imageBytes);
ImageContent imageFromBase64 = ImageContent.from(
    base64Image, 
    "image/jpeg"
);

// 4. 音频内容
AudioContent audioContent = AudioContent.from("audio.mp3");

// 5. 视频内容
VideoContent videoContent = VideoContent.from("video.mp4");

// 6. PDF 文件内容
PdfFileContent pdfContent = PdfFileContent.from("document.pdf");
```

### 多模态 UserMessage

```java
import dev.langchain4j.data.message.*;
import dev.langchain4j.data.message.content.*;

// 文本 + 图像
UserMessage textAndImage = UserMessage.from(
    TextContent.from("这张图片显示了什么？"),
    ImageContent.from("https://example.com/photo.jpg")
);

// 多种内容类型
UserMessage multiModal = UserMessage.from(
    TextContent.from("分析这个文件"),
    ImageContent.from("screenshot.png"),
    AudioContent.from("voice-note.mp3"),
    PdfFileContent.from("document.pdf")
);

ChatModel model = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .build();

ChatResponse response = model.chat(multiModal);
System.out.println(response.aiMessage().text());
```

### 内容详情级别

```java
import dev.langchain4j.data.message.content.ImageContent;
import dev.langchain4j.data.message.content.ImageContent.DetailLevel;

// 设置图像处理详情级别
ImageContent highDetail = ImageContent.from(
    "https://example.com/image.jpg",
    ImageContent.DetailLevel.HIGH  // HIGH, LOW, AUTO
);

ImageContent autoDetail = ImageContent.builder()
        .url("https://example.com/image.jpg")
        .detailLevel(ImageContent.DetailLevel.AUTO)
        .build();
```

## 完整示例：智能对话管理

```java
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.util.ArrayList;
import java.util.List;

/**
 * 智能对话管理器
 */
public class SmartConversationManager {

    private final ChatModel model;
    private final List<ChatMessage> conversationHistory;
    private final int maxMessages;
    private final SystemMessage systemMessage;

    public SmartConversationManager(int maxMessages) {
        this.model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();
        
        this.conversationHistory = new ArrayList<>();
        this.maxMessages = maxMessages;
        
        // 设置系统消息
        this.systemMessage = SystemMessage.from(
            "你是一个友好、专业的助手。" +
            "你的回答应该：\n" +
            "1. 准确且实用\n" +
            "2. 简洁明了\n" +
            "3. 使用适当的格式"
        );
        
        // 添加系统消息到历史
        conversationHistory.add(systemMessage);
    }

    /**
     * 发送消息并获取响应
     */
    public String chat(String userMessage) {
        // 添加用户消息
        UserMessage userMsg = UserMessage.from(userMessage);
        conversationHistory.add(userMsg);
        
        // 检查是否需要清理历史
        if (conversationHistory.size() > maxMessages) {
            cleanupHistory();
        }
        
        // 发送到 LLM
        ChatResponse response = model.chat(conversationHistory);
        AiMessage aiMessage = response.aiMessage();
        
        // 添加 AI 响应到历史
        conversationHistory.add(aiMessage);
        
        return aiMessage.text();
    }

    /**
     * 清理对话历史
     */
    private void cleanupHistory() {
        // 保留系统消息
        List<ChatMessage> newHistory = new ArrayList<>();
        newHistory.add(systemMessage);
        
        // 保留最近的 N 条消息
        int keepCount = maxMessages - 1; // 减去系统消息
        int startIndex = conversationHistory.size() - keepCount;
        
        for (int i = startIndex; i < conversationHistory.size(); i++) {
            newHistory.add(conversationHistory.get(i));
        }
        
        conversationHistory.clear();
        conversationHistory.addAll(newHistory);
        
        System.out.println("对话历史已清理，当前消息数: " + conversationHistory.size());
    }

    /**
     * 获取对话统计
     */
    public void printStatistics() {
        int userMessages = 0;
        int aiMessages = 0;
        int totalLength = 0;
        
        for (ChatMessage msg : conversationHistory) {
            if (msg instanceof UserMessage) {
                userMessages++;
                totalLength += ((UserMessage) msg).text().length();
            } else if (msg instanceof AiMessage) {
                aiMessages++;
                totalLength += ((AiMessage) msg).text().length();
            }
        }
        
        System.out.println("=== 对话统计 ===");
        System.out.println("总消息数: " + conversationHistory.size());
        System.out.println("用户消息: " + userMessages);
        System.out.println("AI 消息: " + aiMessages);
        System.out.println("总字符数: " + totalLength);
    }

    /**
     * 清空对话历史（保留系统消息）
     */
    public void clear() {
        conversationHistory.clear();
        conversationHistory.add(systemMessage);
        System.out.println("对话历史已清空");
    }

    public static void main(String[] args) {
        SmartConversationManager manager = new SmartConversationManager(10);
        
        // 多轮对话
        System.out.println(manager.chat("你好，我是 Java 初学者"));
        System.out.println();
        
        System.out.println(manager.chat("Java 有哪些主要特点？"));
        System.out.println();
        
        System.out.println(manager.chat("我应该从哪里开始学习？"));
        System.out.println();
        
        System.out.println(manager.chat("推荐一些学习资源"));
        System.out.println();
        
        // 打印统计
        manager.printStatistics();
    }
}
```

## 测试代码示例

```java
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class ChatMessageTypesTest {

    private ChatModel model;

    @BeforeEach
    void setUp() {
        model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();
    }

    @Test
    void should_handle_user_message() {
        UserMessage userMessage = UserMessage.from("你好");
        ChatResponse response = model.chat(userMessage);
        
        assertNotNull(response);
        assertNotNull(response.aiMessage());
        assertNotNull(response.aiMessage().text());
        
        System.out.println("用户消息: " + userMessage.text());
        System.out.println("AI 响应: " + response.aiMessage().text());
    }

    @Test
    void should_honor_system_message() {
        SystemMessage systemMessage = SystemMessage.from(
            "你是一个数学老师"
        );
        UserMessage userMessage = UserMessage.from("2 + 2 等于几？");
        
        ChatResponse response = model.chat(
            systemMessage,
            userMessage
        );
        
        assertNotNull(response);
        assertTrue(response.aiMessage().text().contains("4"));
        
        System.out.println("系统消息: " + systemMessage.text());
        System.out.println("AI 响应: " + response.aiMessage().text());
    }

    @Test
    void should_maintain_conversation_context() {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from("你是一个友好的助手"));
        messages.add(UserMessage.from("我叫小明"));
        
        ChatResponse response1 = model.chat(messages);
        messages.add(response1.aiMessage());
        
        messages.add(UserMessage.from("我叫什么？"));
        ChatResponse response2 = model.chat(messages);
        
        assertTrue(response2.aiMessage().text().contains("小明"),
            "AI 应该记得用户的名字");
        
        System.out.println("第一轮 AI: " + response1.aiMessage().text());
        System.out.println("第二轮 AI: " + response2.aiMessage().text());
    }

    @Test
    void should_create_multimodal_message() {
        UserMessage multiModal = UserMessage.from(
            TextContent.from("你好"),
            ImageContent.from("https://example.com/image.jpg")
        );
        
        assertNotNull(multiModal);
        assertEquals(2, multiModal.contents().size());
    }
}
```

## 实践练习

### 练习 1：创建角色系统

创建一个包含多个角色的对话系统：

```java
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.util.List;

public class RoleBasedChat {

    private final ChatModel model;
    
    public enum Role {
        TECHNICAL_EXPERT("技术专家", "你是一个技术专家，擅长解答技术问题"),
        CREATIVE_WRITER("创意作家", "你是一个创意作家，擅长创作各种类型的文字"),
        MATH_TEACHER("数学老师", "你是一个数学老师，擅长解释数学概念"),
        LIFE_COACH("生活教练", "你是一个生活教练，擅长提供人生建议");
        
        private final String name;
        private final String systemPrompt;
        
        Role(String name, String systemPrompt) {
            this.name = name;
            this.systemPrompt = systemPrompt;
        }
        
        public String getName() { return name; }
        public String getSystemPrompt() { return systemPrompt; }
    }

    public RoleBasedChat() {
        this.model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();
    }

    public String chatWithRole(Role role, String userMessage) {
        SystemMessage systemMessage = SystemMessage.from(role.getSystemPrompt());
        UserMessage userMsg = UserMessage.from(userMessage);
        
        ChatResponse response = model.chat(
            systemMessage,
            userMsg
        );
        
        return String.format("[%s]: %s", role.getName(), response.aiMessage().text());
    }

    public static void main(String[] args) {
        RoleBasedChat chat = new RoleBasedChat();
        
        String question = "什么是 Java？";
        
        System.out.println("=== 多角色对话系统 ===");
        System.out.println("问题: " + question);
        System.out.println();
        
        // 使用不同角色回答同一问题
        for (Role role : Role.values()) {
            System.out.println(chat.chatWithRole(role, question));
            System.out.println();
        }
    }
}
```

### 练习 2：消息历史优化器

实现一个智能的消息历史优化器：

```java
import dev.langchain4j.data.message.*;
import java.util.ArrayList;
import java.util.List;

public class MessageHistoryOptimizer {

    /**
     * 优化消息历史
     */
    public static List<ChatMessage> optimizeHistory(
            List<ChatMessage> history,
            int maxSize,
            int maxTokens) {
        
        List<ChatMessage> optimized = new ArrayList<>();
        
        // 1. 保留系统消息
        for (ChatMessage msg : history) {
            if (msg instanceof SystemMessage) {
                optimized.add(msg);
            }
        }
        
        // 2. 计算需要保留的消息数
        int tokenBudget = maxTokens;
        int currentTokens = 0;
        
        // 从最近的消息开始
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage msg = history.get(i);
            
            // 跳过系统消息（已添加）
            if (msg instanceof SystemMessage) {
                continue;
            }
            
            int msgTokens = estimateTokens(msg);
            
            if (currentTokens + msgTokens <= tokenBudget && 
                optimized.size() < maxSize) {
                optimized.add(0, msg);  // 插入到前面
                currentTokens += msgTokens;
            } else {
                break;
            }
        }
        
        return optimized;
    }

    /**
     * 简单的 Token 估算
     */
    private static int estimateTokens(ChatMessage message) {
        String text = "";
        
        if (message instanceof UserMessage) {
            text = ((UserMessage) message).text();
        } else if (message instanceof AiMessage) {
            text = ((AiMessage) message).text();
        } else if (message instanceof SystemMessage) {
            text = ((SystemMessage) message).text();
        }
        
        // 简单估算：中文字符约 0.5 token，英文单词约 1.33 tokens
        return (int) (text.length() * 0.7);
    }

    /**
     * 去除相似消息
     */
    public static List<ChatMessage> removeSimilarMessages(
            List<ChatMessage> history,
            double similarityThreshold) {
        
        List<ChatMessage> result = new ArrayList<>();
        
        for (ChatMessage msg : history) {
            boolean isSimilar = false;
            
            for (ChatMessage existing : result) {
                if (calculateSimilarity(msg, existing) > similarityThreshold) {
                    isSimilar = true;
                    break;
                }
            }
            
            if (!isSimilar) {
                result.add(msg);
            }
        }
        
        return result;
    }
    
    private static double calculateSimilarity(ChatMessage msg1, ChatMessage msg2) {
        String text1 = extractText(msg1);
        String text2 = extractText(msg2);
        
        // 简单的相似度计算（实际应用中应该使用更复杂的算法）
        int commonChars = 0;
        for (char c : text1.toCharArray()) {
            if (text2.indexOf(c) != -1) {
                commonChars++;
            }
        }
        
        return (double) commonChars / Math.max(text1.length(), text2.length());
    }
    
    private static String extractText(ChatMessage message) {
        if (message instanceof UserMessage) {
            return ((UserMessage) message).text();
        } else if (message instanceof AiMessage) {
            return ((AiMessage) message).text();
        } else if (message instanceof SystemMessage) {
            return ((SystemMessage) message).text();
        }
        return "";
    }

    public static void main(String[] args) {
        // 测试优化器
        List<ChatMessage> history = new ArrayList<>();
        history.add(SystemMessage.from("你是一个助手"));
        history.add(UserMessage.from("你好"));
        history.add(AiMessage.from("你好！有什么可以帮助你的吗？"));
        history.add(UserMessage.from("你好"));
        history.add(AiMessage.from("你好！有什么可以帮助你的吗？"));
        history.add(UserMessage.from("介绍 Java"));
        
        System.out.println("原始消息数: " + history.size());
        
        // 优化历史
        List<ChatMessage> optimized = optimizeHistory(history, 5, 1000);
        System.out.println("优化后消息数: " + optimized.size());
        
        // 去除相似消息
        List<ChatMessage> deduplicated = removeSimilarMessages(history, 0.8);
        System.out.println("去重后消息数: " + deduplicated.size());
    }
}
```

### 练习 3：消息序列化器

创建一个可以将对话历史序列化/反序列化的工具：

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.langchain4j.data.message.*;
import dev.langchain4j.data.message.tool.ToolExecutionRequest;
import java.io.File;
import java.util.List;

public class MessageSerializer {

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * 序列化消息历史到文件
     */
    public static void serializeToFile(
            List<ChatMessage> messages, 
            String filePath) throws Exception {
        
        mapper.writeValue(new File(filePath), messages);
        System.out.println("消息历史已保存到: " + filePath);
    }

    /**
     * 从文件反序列化消息历史
     */
    @SuppressWarnings("unchecked")
    public static List<ChatMessage> deserializeFromFile(
            String filePath) throws Exception {
        
        List<ChatMessage> messages = mapper.readValue(
            new File(filePath), 
            List.class
        );
        
        System.out.println("从文件加载了 " + messages.size() + " 条消息");
        return messages;
    }

    /**
     * 序列化为 JSON 字符串
     */
    public static String toJson(List<ChatMessage> messages) throws Exception {
        return mapper.writeValueAsString(messages);
    }

    /**
     * 从 JSON 字符串反序列化
     */
    @SuppressWarnings("unchecked")
    public static List<ChatMessage> fromJson(String json) throws Exception {
        return mapper.readValue(json, List.class);
    }

    /**
     * 格式化显示消息历史
     */
    public static void prettyPrint(List<ChatMessage> messages) {
        System.out.println("=== 对话历史 ===");
        System.out.println();
        
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            
            if (msg instanceof SystemMessage) {
                System.out.println("[系统] " + ((SystemMessage) msg).text());
            } else if (msg instanceof UserMessage) {
                UserMessage userMsg = (UserMessage) msg;
                System.out.println("[用户" + 
                    (userMsg.name() != null ? " " + userMsg.name() : "") + 
                    "] " + userMsg.text());
            } else if (msg instanceof AiMessage) {
                AiMessage aiMsg = (AiMessage) msg;
                System.out.println("[AI] " + aiMsg.text());
                
                if (aiMsg.hasToolExecutionRequests()) {
                    for (ToolExecutionRequest req : aiMsg.toolExecutionRequests()) {
                        System.out.println("  [工具] " + req.name() + 
                            "(" + req.arguments() + ")");
                    }
                }
            } else if (msg instanceof ToolExecutionResultMessage) {
                ToolExecutionResultMessage resultMsg = 
                    (ToolExecutionResultMessage) msg;
                System.out.println("[工具结果] " + resultMsg.text());
            }
            
            System.out.println();
        }
    }

    public static void main(String[] args) {
        try {
            // 创建示例对话
            List<ChatMessage> conversation = List.of(
                SystemMessage.from("你是一个助手"),
                UserMessage.from("你好"),
                AiMessage.from("你好！有什么可以帮助你的吗？"),
                UserMessage.from("2 + 2 等于几？"),
                AiMessage.from("2 + 2 等于 4")
            );
            
            // 序列化到文件
            serializeToFile(conversation, "conversation.json");
            
            // 反序列化
            List<ChatMessage> loaded = deserializeFromFile("conversation.json");
            
            // 格式化显示
            prettyPrint(loaded);
            
            // 序列化为 JSON
            String json = toJson(loaded);
            System.out.println("JSON 格式:\n" + json);
            
        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
        }
    }
}
```

## 总结

### 本章要点

1. **ChatMessage 类型**
   - `UserMessage` - 用户消息，支持多模态
   - `AiMessage` - AI 响应，包含工具请求
   - `SystemMessage` - 系统消息，设置行为规则
   - `ToolExecutionResultMessage` - 工具执行结果

2. **UserMessage 特性**
   - 基本文本和多模态内容
   - 可选的用户名称
   - 支持文本、图像、音频、视频、PDF

3. **AiMessage 特性**
   - 包含文本、思考内容、工具请求
   - 可以检查工具执行请求
   - 在多轮对话中保存上下文

4. **SystemMessage 最佳实践**
   - 定义角色和行为
   - 设置输出格式和约束
   - **重要**：不允许用户修改系统消息

5. **消息组合**
   - 消息顺序很重要
   - 系统消息应该在开始
   - 需要管理对话历史长度

6. **多模态消息**
   - 支持多种内容类型
   - 可设置图像详情级别
   - 适用于视觉、音频等场景

### 下一步

在下一章节中，我们将学习：
- ChatMemory 对话记忆管理
- 如何在多轮对话中保持上下文
- 不同类型的 ChatMemory 实现
- 持久化对话历史

### 常见问题

**Q1：SystemMessage 和 UserMessage 有什么区别？**

A：
- `SystemMessage` - 设置 LLM 的整体行为、角色和规则
- `UserMessage` - 具体的用户问题或请求
- LLM 对 SystemMessage 的关注度通常高于 UserMessage

**Q2：为什么多轮对话中 AI 不记得之前的内容？**

A：需要维护对话历史并在每次请求时发送。下一章将介绍 `ChatMemory` 来自动管理这个过程。

**Q3：消息历史太长怎么办？**

A：可以：
- 限制消息数量（如保留最近 10 条）
- 根据 Token 数量截断
- 总结早期对话
- 使用滑动窗口策略

**Q4：如何实现多模态？**

A：使用 `UserMessage.from()` 方法传入多个 `Content` 对象，包括 `TextContent`、`ImageContent`、`AudioContent` 等。

**Q5：ToolExecutionResultMessage 是什么时候创建的？**

A：在 LLM 请求调用工具后，我们手动或自动执行工具，然后创建 `ToolExecutionResultMessage` 将结果返回给 LLM。

## 参考资料

- [LangChain4j ChatMessage 文档](https://docs.langchain4j.dev/tutorials/chat-and-language-models)
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
