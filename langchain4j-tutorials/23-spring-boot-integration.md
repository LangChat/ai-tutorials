---
title: 'Spring Boot 集成'
description: '学习 LangChain4j 的 Spring Boot 集成 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 23 - Spring Boot 集成

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 理解 LangChain4j 与 Spring Boot 的集成方式
- 掌握 LangChain4j Spring Boot Starter 的使用
- 学会创建基于 Spring Boot 的 LLM 应用
- 理解依赖注入和配置管理
- 掌握 REST API 的创建
- 实现一个完整的 Spring Boot + LangChain4j 应用

## 前置知识

- 完成《01 - LangChain4j 简介》章节
- Spring Boot 基础知识
- Maven/Gradle 构建工具基础

## 核心概念

### Spring Boot Starter

**LangChain4j Spring Boot Starter** 是官方提供的 Spring Boot 集成模块，提供：

1. **自动配置**
   - 自动配置 ChatModel
   - 自动配置 EmbeddingModel
   - 自动配置 TokenStream

2. **依赖注入**
   - 通过 @Autowired 注入模型
   - 通过 @Bean 自定义配置

3. **属性配置**
   - application.yml/application.properties 配置
   - 环境变量和配置文件支持

4. **Starter 优势**
   - 简化集成
   - 统一配置管理
   - 自动依赖管理
   - Spring 生态系统集成

### 项目结构

```
langchat-demo/
├── src/main/java/
│   └── com/example/langchat/
│       ├── LangchatDemoApplication.java     # 主应用类
│       ├── config/
│       │   ├── ModelConfig.java          # 模型配置
│       │   └── ChatConfig.java           # 聊天配置
│       ├── controller/
│       │   ├── ChatController.java        # 聊天控制器
│       │   └── ChatWebSocketHandler.java  # WebSocket 处理器
│       ├── service/
│       │   ├── ChatService.java          # 聊天服务
│       │   └── StreamingChatService.java # 流式聊天服务
│       └── dto/
│           ├── ChatRequest.java           # 聊天请求 DTO
│           └── ChatResponse.java          # 聊天响应 DTO
├── src/main/resources/
│   ├── application.yml                 # 配置文件
│   └── static/                       # 静态资源
└── pom.xml                            # Maven 配置
```

## 项目配置

### pom.xml 依赖

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>langchat-demo</artifactId>
    <version>1.0.0</version>
    <name>LangChat Demo</name>
    <description>LangChain4j Spring Boot 集成示例</description>

    <properties>
        <java.version>17</java.version>
        <langchain4j.version>1.10.0</langchain4j.version>
        <maven.compiler.source>${java.version}</maven.compiler.source>
        <maven.compiler.target>${java.version}</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- LangChain4j Spring Boot Starter -->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-open-ai-spring-boot-starter</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>

        <!-- Spring Boot WebSocket (可选）-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Lombok (可选）-->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test Dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### application.yml 配置

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  application:
    name: langchat-demo

# OpenAI 配置
langchain4j:
  open-ai:
    chat-model:
      api-key: ${OPENAI_API_KEY}
      model-name: gpt-4o-mini
      temperature: 0.7
      max-tokens: 1000
      timeout: 60s
      max-retries: 3
      log-requests: true
      log-responses: true

    streaming-chat-model:
      api-key: ${OPENAI_API_KEY}
      model-name: gpt-4o-mini
      temperature: 0.7
      max-tokens: 1000
      timeout: 60s

    embedding-model:
      api-key: ${OPENAI_API_KEY}
      model-name: text-embedding-3-small
      timeout: 60s

# 日志配置
logging:
  level:
    root: INFO
    dev.langchain4j: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/langchat-demo.log
```

## 核心代码实现

### 主应用类

```java
package com.example.langchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 主应用类
 */
@SpringBootApplication
public class LangchatDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(LangchatDemoApplication.class, args);
    }
}
```

### 模型配置类

```java
package com.example.langchat.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 模型配置
 */
@Configuration
public class ModelConfig {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String chatApiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name}")
    private String chatModelName;

    @Value("${langchain4j.open-ai.chat-model.temperature}")
    private Double temperature;

    @Value("${langchain4j.open-ai.chat-model.max-tokens}")
    private Integer maxTokens;

    @Value("${langchain4j.open-ai.chat-model.timeout}")
    private Duration timeout;

    /**
     * ChatModel Bean
     */
    @Bean
    public ChatModel chatModel() {
        return OpenAiChatModel.builder()
                .apiKey(chatApiKey)
                .modelName(chatModelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(timeout)
                .build();
    }

    /**
     * StreamingChatModel Bean
     */
    @Bean
    public StreamingChatModel streamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(chatApiKey)
                .modelName(chatModelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(timeout)
                .build();
    }

    /**
     * EmbeddingModel Bean
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(chatApiKey)
                .modelName("text-embedding-3-small")
                .build();
    }

    /**
     * 聊天模型接口
     */
    public interface StreamingChatModel extends dev.langchain4j.model.chat.StreamingChatModel {
    }
}
```

### 聊天服务

```java
package com.example.langchat.service;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 聊天服务
 */
@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final ChatModel chatModel;

    /**
     * AI 服务接口
     */
    @AiService
    public interface Assistant {
        String chat(@UserMessage String message);
    }

    /**
     * AI 助手实例
     */
    private Assistant assistant;

    /**
     * 构造函数
     */
    @Autowired
    public ChatService(ChatModel chatModel) {
        this.chatModel = chatModel;

        // 创建 AI 助手
        this.assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .systemMessageProvider(chatMemoryId -> 
                    "你是一个专业、友好且乐于助人的 AI 助手。" +
                    "你的任务是回答用户的问题，提供准确、有用的信息。" +
                    "请保持回答简洁明了，避免冗长的解释。" +
                    "如果不确定答案，请诚实告知用户。" +
                    "请使用自然的语言风格，避免过于正式或生硬的表达。"
                )
                .build();
    }

    /**
     * 发送消息
     */
    public String sendMessage(String message) {
        logger.info("收到消息: {}", message);
        return assistant.chat(message);
    }
}
```

### 聊天控制器

```java
package com.example.langchat.controller;

import com.example.langchat.dto.ChatRequest;
import com.example.langchat.dto.ChatResponse;
import com.example.langchat.service.ChatService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;

/**
 * 聊天控制器
 */
@RestController
@RequestMapping("/chat")
@CrossOrigin(origins = "*")  // 生产环境应该指定具体域名
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    /**
     * 构造函数
     */
    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 发送聊天消息
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(@Valid @RequestBody ChatRequest request) {
        logger.info("收到聊天请求: {}", request.getMessage());

        try {
            String response = chatService.sendMessage(request.getMessage());

            ChatResponse chatResponse = new ChatResponse();
            chatResponse.setMessage(response);
            chatResponse.setTimestamp(System.currentTimeMillis());

            return ResponseEntity.ok(chatResponse);

        } catch (Exception e) {
            logger.error("处理聊天请求失败", e);

            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setMessage("处理失败: " + e.getMessage());
            errorResponse.setTimestamp(System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Chat 服务正常运行");
    }

    /**
     * 获取服务信息
     */
    @GetMapping("/info")
    public ResponseEntity<ChatServiceInfo> getInfo() {
        ChatServiceInfo info = new ChatServiceInfo();
        info.setName("Chat Service");
        info.setVersion("1.0.0");
        info.setUptime(System.currentTimeMillis());
        return ResponseEntity.ok(info);
    }

    /**
     * 聊天服务信息
     */
    private static class ChatServiceInfo {
        private String name;
        private String version;
        private long uptime;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public long getUptime() { return uptime; }
        public void setUptime(long uptime) { this.uptime = uptime; }
    }
}
```

### 流式聊天控制器

```java
package com.example.langchat.controller;

import com.example.langchat.service.StreamingChatService;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 流式聊天控制器
 */
@RestController
@RequestMapping("/chat/stream")
public class StreamingChatController {

    private static final Logger logger = LoggerFactory.getLogger(StreamingChatController.class);

    private final StreamingChatModel streamingChatModel;

    /**
     * 构造函数
     */
    @Autowired
    public StreamingChatController(StreamingChatService streamingChatService) {
        this.streamingChatModel = streamingChatService.getModel();
    }

    /**
     * 流式发送消息
     */
    @GetMapping(value = "/message", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void streamMessage(@RequestParam("message") String message,
                            @RequestParam(value = "temperature", defaultValue = "0.7") double temperature,
                            @RequestParam(value = "maxTokens", defaultValue = "1000") int maxTokens) throws IOException {
        
        logger.info("收到流式聊天请求: {}", message);

        // 设置响应
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        
        try (java.io.PrintWriter writer = response.getWriter()) {
            // 发送开始事件
            writer.write("event: start\n");
            writer.write("data: {}\n\n");
            writer.flush();

            // 流式生成
            AtomicReference<StringBuilder> fullResponse = new AtomicReference<>(new StringBuilder());
            
            streamingchatModel.chat(message, nextToken -> {
                // 发送 Token 事件
                try {
                    writer.write("event: token\n");
                    writer.write("data: ");
                    writer.write(nextToken);
                    writer.write("\n\n");
                    writer.flush();

                    fullResponse.get().append(nextToken);
                } catch (IOException e) {
                    logger.error("发送流数据失败", e);
                }
            });

            // 发送完成事件
            writer.write("event: end\n");
            writer.write("data: ");
            writer.write(fullResponse.get().toString());
            writer.write("\n\n");
            writer.flush();

        } catch (Exception e) {
            logger.error("流式聊天处理失败", e);
            // 发送错误事件
            response.sendError(500, "处理失败: " + e.getMessage());
        }
    }

    /**
     * 获取 HttpServletResponse
     */
    private jakarta.servlet.http.HttpServletResponse response;

    /**
     * 注入 HttpServletResponse
     */
    @Autowired
    public void setResponse(jakarta.servlet.http.HttpServletResponse response) {
        this.response = response;
    }
}
```

### DTO 类

```java
package com.example.langchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 聊天请求
 */
public class ChatRequest {

    @NotBlank(message = "消息不能为空")
    @Size(min = 1, max = 2000, message = "消息长度必须在 1-2000 字符之间")
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

/**
 * 聊天响应
 */
public class ChatResponse {

    private String message;
    private long timestamp;
    private String error;

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
```

## REST API 示例

### 创建完整 API

```java
package com.example.langchat.controller;

import com.example.langchat.dto.ChatRequest;
import com.example.langchat.dto.ChatResponse;
import com.example.langchat.service.ChatService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 高级聊天 API
 */
@RestController
@RequestMapping("/api/v1/chat")
public class AdvancedChatController {

    private final ChatService chatService;

    @Autowired
    public AdvancedChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 批量聊天
     */
    @PostMapping("/batch")
    public ResponseEntity<List<ChatResponse>> batchChat(@RequestBody List<ChatRequest> requests) {
        List<ChatResponse> responses = new java.util.ArrayList<>();

        for (ChatRequest request : requests) {
            try {
                String responseMessage = chatService.sendMessage(request.getMessage());

                ChatResponse response = new ChatResponse();
                response.setMessage(responseMessage);
                response.setTimestamp(System.currentTimeMillis());
                responses.add(response);

            } catch (Exception e) {
                ChatResponse errorResponse = new ChatResponse();
                errorResponse.setMessage("失败: " + e.getMessage());
                errorResponse.setTimestamp(System.currentTimeMillis());
                responses.add(errorResponse);
            }
        }

        return ResponseEntity.ok(responses);
    }

    /**
     * 带上下文的聊天
     */
    @PostMapping("/context")
    public ResponseEntity<ChatResponse> chatWithContext(@RequestBody ChatContextRequest request) {
        String fullMessage = request.getContext() + "\n" + request.getMessage();
        String response = chatService.sendMessage(fullMessage);

        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setMessage(response);
        chatResponse.setTimestamp(System.currentTimeMillis());

        return ResponseEntity.ok(chatResponse);
    }

    /**
     * 多轮对话
     */
    @PostMapping("/conversation")
    public ResponseEntity<ChatResponse> conversation(@RequestBody ConversationRequest request) {
        StringBuilder context = new StringBuilder();

        // 处理历史消息
        for (String message : request.getHistory()) {
            context.append(message).append("\n");
        }

        // 添加当前消息
        context.append(request.getMessage());

        // 发送
        String response = chatService.sendMessage(context.toString());

        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setMessage(response);
        chatResponse.setTimestamp(System.currentTimeMillis());

        return ResponseEntity.ok(chatResponse);
    }

    /**
     * 带系统提示的聊天
     */
    @PostMapping("/custom-system")
    public ResponseEntity<ChatResponse> chatWithSystemMessage(@RequestBody SystemMessageRequest request) {
        // 使用自定义系统提示发送
        // 这里可以创建一个专门的服务方法

        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setMessage("自定义系统提示功能");
        chatResponse.setTimestamp(System.currentTimeMillis());

        return ResponseEntity.ok(chatResponse);
    }

    /**
     * 聊天上下文请求
     */
    private static class ChatContextRequest {
        private String context;
        private String message;

        // Getters and Setters
        public String getContext() { return context; }
        public void setContext(String context) { this.context = context; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * 对话请求
     */
    private static class ConversationRequest {
        private List<String> history;
        private String message;

        // Getters and Setters
        public List<String> getHistory() { return history; }
        public void setHistory(List<String> history) { this.history = history; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * 系统消息请求
     */
    private static class SystemMessageRequest {
        private String systemMessage;
        private String userMessage;

        // Getters and Setters
        public String getSystemMessage() { return systemMessage; }
        public void setSystemMessage(String systemMessage) { this.systemMessage = systemMessage; }
        public String getUserMessage() { return userMessage; }
        public void setUserMessage(String userMessage) { this.userMessage = userMessage; }
    }
}
```

## 测试代码

### Spring Boot 测试

```java
package com.example.langchat;

import com.example.langchat.dto.ChatRequest;
import com.example.langchat.dto.ChatResponse;
import com.example.langchat.service.ChatService;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.langchain4j.data.message.AiMessage;

/**
 * 聊天服务测试
 */
@SpringBootTest
@TestPropertySource(properties = {
    "langchain4j.open-ai.chat-model.api-key=test-key",
    "langchain4j.open-ai.chat-model.model-name=gpt-4o-mini",
    "langchain4j.open-ai.chat-model.temperature=0.7",
    "langchain4j.open-ai.chat-model.max-tokens=1000"
})
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @Test
    void should_send_message_successfully() throws Exception {
        // 准备测试数据
        ChatRequest request = new ChatRequest();
        request.setMessage("你好");

        // Mock 服务
        when(chatService.sendMessage(anyString())).thenReturn("你好！有什么可以帮助你的？");

        // 执行请求
        mockMvc.perform(MockMvcRequestBuilders.post("/api/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"你好\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("你好！有什么可以帮助你的？"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void should_validate_empty_message() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_validate_long_message() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("a".repeat(2001));  // 超过 2000 字符限制

        mockMvc.perform(MockMvcRequestBuilders.post("/api/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"" + "a".repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_handle_service_error() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("测试");

        // Mock 服务异常
        when(chatService.sendMessage(anyString()))
                .thenThrow(new RuntimeException("服务错误"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"测试\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void should_return_health_check() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/chat/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Chat 服务正常运行"));
    }

    @Test
    void should_return_service_info() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/chat/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Chat Service"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.uptime").exists());
    }
}
```

## 实践练习

### 练习 1：实现完整的聊天应用

```java
package com.example.langchat;

import com.example.langchat.dto.ChatRequest;
import com.example.langchat.dto.ChatResponse;
import com.example.langchat.service.ChatService;

import org.springframework.web.bind.annotation.*;

/**
 * 完整的聊天应用
 */
@RestController
@RequestMapping("/api/v2/chat")
public class CompleteChatController {

    private final ChatService chatService;

    public CompleteChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 发送消息
     */
    @PostMapping("/send")
    public ResponseEntity<ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        String response = chatService.sendMessage(request.getMessage());

        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setMessage(response);
        chatResponse.setTimestamp(System.currentTimeMillis());

        return ResponseEntity.ok(chatResponse);
    }

    /**
     * 批量处理消息
     */
    @PostMapping("/batch")
    public ResponseEntity<List<ChatResponse>> batchProcess(@RequestBody List<ChatRequest> requests) {
        // 实现批量处理逻辑
        return ResponseEntity.ok(new java.util.ArrayList<>());
    }

    /**
     * 清除会话
     */
    @Delete("/session")
    public ResponseEntity<String> clearSession() {
        // 实现会话清除逻辑
        return ResponseEntity.ok("会话已清除");
    }

    /**
     * 获取会话历史
     */
    @GetMapping("/history")
    public ResponseEntity<List<String>> getSessionHistory() {
        // 实现历史记录逻辑
        return ResponseEntity.ok(new java.util.ArrayList<>());
    }

    /**
     * 设置参数
     */
    @PostMapping("/settings")
    public ResponseEntity<String> updateSettings(@RequestBody ChatSettings settings) {
        // 实现参数更新逻辑
        return ResponseEntity.ok("设置已更新");
    }

    /**
     * 获取参数
     */
    @GetMapping("/settings")
    public ResponseEntity<ChatSettings> getSettings() {
        return ResponseEntity.ok(new ChatSettings());
    }

    /**
     * 聊天设置
     */
    private static class ChatSettings {
        private double temperature;
        private int maxTokens;
        private String modelName;

        // Getters and Setters
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
    }
}
```

## 总结

### 本章要点

1. **Spring Boot Starter**
   - 简化 LangChain4j 集成
   - 自动配置模型
   - 统一依赖管理

2. **配置管理**
   - application.yml 配置
   - @Value 属性注入
   - @Bean 模型定义

3. **服务层**
   - ChatService 封装
   - AI 服务使用
   - 业务逻辑分离

4. **REST API**
   - 控制器设计
   - 请求验证
   - 错误处理

5. **测试策略**
   - 单元测试
   - 集成测试
   - Mock 测试

### 下一步

在下一章节中，我们将学习：
- WebSocket 实时通信
- 高级流式处理
- 安全和认证
- 性能优化
- 部署和运维

### 常见问题

**Q1：如何配置不同的模型？**

A：配置方法：
1. 在 application.yml 中指定不同的模型名称
2. 为不同模型创建不同的 Bean
3. 使用 @Profile 区分环境
4. 动态切换模型

**Q2：如何处理并发请求？**

A：处理策略：
1. 使用线程池配置
2. 设置连接池大小
3. 使用异步处理
4. 实现请求队列
5. 限流和熔断

**Q3：如何实现多租户？**

A：实现方案：
1. 为每个租户配置不同的 API Key
2. 动态创建模型 Bean
3. 租户级别的限流
4. 租户配置隔离
5. 使用 AOP 实现租户拦截

**Q4：如何监控 Spring Boot 应用？**

A：监控方法：
1. Spring Boot Actuator
2. 自定义健康检查
3. Prometheus 指标暴露
4. 日志聚合
5. APM 工具集成

**Q5：如何部署到生产环境？**

A：部署方案：
1. 打包为 JAR 或 WAR
2. Docker 容器化
3. Kubernetes 部署
4. 环境变量配置
5. 反向代理配置

## 参考资料

- [LangChain4j Spring Boot Starter 文档](https://docs.langchain4j.dev/tutorials/spring-boot-integration)
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
