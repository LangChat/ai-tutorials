---
title: 'LangChain4j 简介'
description: '学习 LangChain4j 的 LangChain4j 简介 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 01 - LangChain4j 简介

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 理解 LangChain4j 是什么及其核心价值
- 了解 LangChain4j 的应用场景
- 掌握 LangChain4j 的核心特性
- 搭建 LangChain4j 开发环境
- 运行第一个 LangChain4j Hello World 示例

## 前置知识

- Java 基础知识（Java 17+）
- Maven 或 Gradle 基础
- 了解基本的大语言模型（LLM）概念

## 核心概念

### 什么是 LangChain4j？

LangChain4j 是一个用于构建 LLM（大语言模型）应用的 Java 开源库。它提供了一套完整的工具和组件，帮助开发者轻松地将大语言模型集成到 Java 应用程序中。

**类比理解：**
如果把大语言模型（如 GPT-4）比作一个强大的引擎，那么 LangChain4j 就是帮助你在 Java 应用中安装、配置和使用这个引擎的工具箱。你不需要从头开始编写所有与引擎交互的代码，LangChain4j 已经为你准备好了。

### 为什么选择 LangChain4j？

#### 1. **专为 Java 设计**
LangChain4j 是专门为 Java 生态系统设计的，充分利用了 Java 的类型安全性和面向对象特性。与其他 Python 语言的 LangChain 相比，它更适合 Java 开发者使用。

#### 2. **简单易用**
LangChain4j 提供了简单直观的 API，无论是低级 API（如 `ChatModel`）还是高级 API（如 `AI Services`），都能快速上手。

#### 3. **功能完整**
LangChain4j 提供了构建 LLM 应用所需的所有核心功能：
- 与多种 LLM 提供商集成（OpenAI、Anthropic、Google 等）
- 对话记忆管理
- 工具调用（Function Calling）
- 检索增强生成（RAG）
- 结构化输出
- 流式响应
- 智能体（Agent）
- 多模态支持

#### 4. **生产就绪**
LangChain4j 被许多企业用于生产环境，具有良好的性能、稳定性和可扩展性。

#### 5. **活跃的社区**
LangChain4j 拥有活跃的开源社区，持续更新和完善，提供及时的技术支持。

### 应用场景

LangChain4j 可以用于构建各种类型的应用：

#### 1. **聊天机器人**
构建智能客服、虚拟助手等对话系统。

#### 2. **知识库问答**
基于企业文档、FAQ 等知识库的智能问答系统。

#### 3. **代码助手**
帮助开发者编写、理解、调试代码的 AI 助手。

#### 4. **数据分析助手**
通过自然语言查询和分析数据。

#### 5. **文档处理**
智能文档摘要、提取、分类等。

#### 6. **工作流自动化**
使用 LLM 自动化复杂的工作流程。

### 核心特性

#### 1. **多模型支持**
LangChain4j 支持多种大语言模型提供商：
- OpenAI（GPT-4、GPT-3.5 等）
- Anthropic（Claude 系列）
- Google（Gemini 系列）
- Mistral AI
- Ollama（本地模型）
- 等等

#### 2. **双层级 API**

**低级 API：**
提供最大的灵活性和控制力，包括：
- `ChatModel` - 聊天模型接口
- `EmbeddingModel` - 向量模型接口
- `ChatMemory` - 对话记忆管理
- `Tool` - 工具调用
- `EmbeddingStore` - 向量存储

**高级 API（AI Services）：**
提供声明式、简化的接口，类似 Spring Data JPA：
```java
// 定义接口
interface Assistant {
    String chat(String message);
}

// 自动实现
Assistant assistant = AiServices.create(Assistant.class, chatModel);
String response = assistant.chat("你好");
```

#### 3. **对话记忆**
LLM 本身是无状态的，LangChain4j 提供了多种对话记忆管理方案：
- `MessageWindowChatMemory` - 基于消息数量的记忆
- `TokenWindowChatMemory` - 基于 Token 数量的记忆
- 持久化记忆存储

#### 4. **工具调用（Function Calling）**
允许 LLM 调用外部工具和 API，扩展其能力：
```java
class WeatherService {
    @Tool
    String getWeather(String city) {
        // 调用天气 API
        return "北京今天晴，25°C";
    }
}
```

#### 5. **检索增强生成（RAG）**
结合外部知识库提高回答准确性：
- 文档向量化存储
- 语义检索
- 相关内容注入

#### 6. **流式响应**
实时流式输出 LLM 的生成内容，提升用户体验。

#### 7. **结构化输出**
让 LLM 输出结构化的数据（JSON、POJO 等）：
```java
Person person = assistant.extractPerson("John, 30 years old");
```

#### 8. **智能体（Agent）**
构建具有自主规划和执行能力的智能体。

#### 9. **多模态支持**
支持文本、图像、音频、视频等多种模态。

#### 10. **可观测性**
提供完善的监控和日志功能，便于调试和优化。

#### 11. **框架集成**
完美集成主流 Java 框架：
- Spring Boot
- Quarkus
- Micronaut

## 开发环境准备

### 系统要求

- **Java 17 或更高版本**
- **Maven 3.6+ 或 Gradle 7+**
- **IDE**：IntelliJ IDEA（推荐）或 Eclipse

### Maven 项目配置

创建一个 Maven 项目，在 `pom.xml` 中添加 LangChain4j 依赖：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>langchain4j-learning</artifactId>
    <version>1.0.0</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <langchain4j.version>1.0.0-alpha1</langchain4j.version>
    </properties>

    <dependencies>
        <!-- LangChain4j 核心 -->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>

        <!-- OpenAI 模型集成 -->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-open-ai</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>

        <!-- 日志框架（SLF4J + Logback） -->
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.5.8</version>
        </dependency>

        <!-- JUnit 5 测试框架 -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

</project>
```

### 获取 API Key

大多数 LLM 提供商都需要 API Key，以 OpenAI 为例：

1. 访问 https://platform.openai.com/
2. 注册/登录账户
3. 进入 API Keys 页面
4. 创建新的 API Key
5. 复制并保存 API Key

**安全提示：** 不要将 API Key 提交到代码仓库，使用环境变量存储。

设置环境变量：
```bash
# Linux/Mac
export OPENAI_API_KEY="your-api-key-here"

# Windows
set OPENAI_API_KEY=your-api-key-here
```

## Hello World 示例

现在让我们创建第一个 LangChain4j 程序，实现一个简单的 "Hello World"。

### 完整代码示例

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * LangChain4j Hello World 示例
 */
public class HelloWorldExample {

    public static void main(String[] args) {
        // 1. 创建 ChatModel 实例
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))  // 从环境变量读取 API Key
                .modelName("gpt-4o-mini")                  // 使用 GPT-4o-mini 模型
                .build();

        // 2. 发送消息给 LLM
        String message = "Hello, LangChain4j!";
        String response = model.chat(message);

        // 3. 打印响应
        System.out.println("用户: " + message);
        System.out.println("AI: " + response);
    }
}
```

### 代码详解

#### 步骤 1：创建 ChatModel

```java
ChatModel model = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .build();
```

- `OpenAiChatModel`：OpenAI 提供的聊天模型实现
- `.apiKey()`：设置 OpenAI API Key
- `.modelName()`：指定使用的模型
  - `gpt-4o-mini`：性价比高，适合学习和测试
  - `gpt-4o`：更强大的模型
  - `gpt-3.5-turbo`：较老但便宜的模型
- `.build()`：构建模型实例

#### 步骤 2：发送消息

```java
String response = model.chat(message);
```

- `.chat()` 方法将用户消息发送给 LLM
- 返回 LLM 生成的响应文本

#### 步骤 3：打印结果

```java
System.out.println("用户: " + message);
System.out.println("AI: " + response);
```

打印对话内容。

### 运行程序

编译并运行程序：

```bash
# 编译
mvn clean compile

# 运行
mvn exec:java -Dexec.mainClass="com.example.HelloWorldExample"
```

**预期输出：**
```
用户: Hello, LangChain4j!
AI: 你好！很高兴认识你。LangChain4j 是一个强大的 Java LLM 框架，有什么我可以帮助你的吗？
```

## 测试代码示例

使用 JUnit 5 编写测试用例：

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * LangChain4j 基础测试
 */
class LangChain4jBasicTest {

    private ChatModel model;

    @BeforeEach
    void setUp() {
        // 每个测试前创建模型实例
        model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();
    }

    @Test
    void should_generate_response_when_given_simple_message() {
        // Given - 准备测试数据
        String message = "What is 2 + 2?";

        // When - 执行操作
        String response = model.chat(message);

        // Then - 验证结果
        assertNotNull(response);
        assertFalse(response.isEmpty());
        System.out.println("Response: " + response);
    }

    @Test
    void should_generate_chinese_response_when_message_in_chinese() {
        // Given
        String message = "你好，请用中文回答：什么是 LangChain4j？";

        // When
        String response = model.chat(message);

        // Then
        assertNotNull(response);
        // 验证响应中包含中文（简单检查）
        assertTrue(response.matches(".*[\\u4e00-\\u9fa5].*"));
        System.out.println("中文响应: " + response);
    }

    @Test
    void should_handle_empty_message() {
        // Given
        String message = "";

        // When
        String response = model.chat(message);

        // Then
        assertNotNull(response);
        System.out.println("空消息响应: " + response);
    }
}
```

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=LangChain4jBasicTest
```

## 实践练习

### 练习 1：多轮对话

修改 Hello World 程序，实现一个简单的多轮对话：

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.util.Scanner;

public class SimpleChatBot {

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== 简单聊天机器人 ===");
        System.out.println("输入 'exit' 退出");
        System.out.println();

        while (true) {
            System.out.print("用户: ");
            String input = scanner.nextLine();

            if ("exit".equalsIgnoreCase(input.trim())) {
                System.out.println("再见！");
                break;
            }

            String response = model.chat(input);
            System.out.println("AI: " + response);
            System.out.println();
        }

        scanner.close();
    }
}
```

**任务：**
1. 运行这个聊天机器人程序
2. 尝试与它进行多轮对话
3. 注意观察：每轮对话之间 AI 是否"记得"之前的对话内容？
4. 思考：为什么会出现这种情况？

### 练习 2：探索不同模型

尝试使用不同的 OpenAI 模型，观察响应速度和质量的变化：

```java
ChatModel modelGpt35 = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-3.5-turbo")
        .build();

ChatModel modelGpt4o = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .build();

// 比较两个模型的响应
String message = "写一首关于春天的诗";
long start = System.currentTimeMillis();
String response1 = modelGpt35.chat(message);
long end = System.currentTimeMillis();
System.out.println("GPT-3.5 Turbo (" + (end - start) + "ms):");
System.out.println(response1);
System.out.println();

start = System.currentTimeMillis();
String response2 = modelGpt4o.chat(message);
end = System.currentTimeMillis();
System.out.println("GPT-4o Mini (" + (end - start) + "ms):");
System.out.println(response2);
```

### 练习 3：自定义提示词

尝试不同的提示词，观察 LLM 的响应变化：

```java
// 提示词 1：简单提问
String prompt1 = "什么是 Java？";
System.out.println(model.chat(prompt1));

// 提示词 2：指定回答格式
String prompt2 = "请用 3 点说明什么是 Java？";
System.out.println(model.chat(prompt2));

// 提示词 3：角色扮演
String prompt3 = "你是一位 Java 专家，请用简单易懂的语言向初学者解释什么是 Java。";
System.out.println(model.chat(prompt3));
```

## 总结

### 本章要点

1. **LangChain4j 是什么**
   - 专为 Java 设计的 LLM 应用开发框架
   - 提供完整的工具链和组件

2. **核心优势**
   - 简单易用的 API
   - 功能完整（对话记忆、工具调用、RAG 等）
   - 生产就绪
   - 活跃的社区

3. **双层级 API 设计**
   - 低级 API：灵活控制
   - 高级 API（AI Services）：声明式简化

4. **环境搭建**
   - 添加 Maven 依赖
   - 获取 API Key
   - 运行 Hello World

5. **Hello World 示例**
   - 创建 ChatModel
   - 发送消息
   - 接收响应

### 下一步

在下一章节中，我们将深入学习：
- ChatModel 的详细用法
- 不同模型类型的选择
- 异常处理
- 更多实用示例

### 常见问题

**Q1：LangChain4j 与 Python 版 LangChain 有什么区别？**

A：LangChain4j 是专门为 Java 生态系统设计的，充分利用了 Java 的类型安全性和面向对象特性。虽然概念相似，但 API 设计更适合 Java 开发者使用。

**Q2：我必须使用 OpenAI 吗？**

A：不，LangChain4j 支持多种 LLM 提供商，包括 Anthropic、Google、Mistral 等，还可以使用 Ollama 运行本地模型。

**Q3：API Key 安全吗？**

A：永远不要将 API Key 提交到代码仓库。建议使用环境变量、配置文件或密钥管理服务来存储。

**Q4：LLM 响应速度很慢怎么办？**

A：可以尝试：
- 使用更快的模型（如 gpt-4o-mini）
- 使用流式响应
- 优化提示词长度
- 考虑缓存常见查询

**Q5：如何控制 LLM 的输出长度？**

A：可以通过 `maxTokens` 参数控制，我们将在下一章节详细介绍。

## 参考资料

- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChain4j GitHub](https://github.com/langchain4j/langchain4j)
- [OpenAI 官方文档](https://platform.openai.com/docs)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
