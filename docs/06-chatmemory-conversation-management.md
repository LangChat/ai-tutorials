---
title: 'ChatMemory 对话管理'
description: '学习 LangChain4j 的 ChatMemory 对话管理 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 06 - ChatMemory 对话记忆管理

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 理解为什么需要 ChatMemory
- 掌握 MessageWindowChatMemory 的使用
- 了解 TokenWindowChatMemory 的区别
- 学会管理多用户场景的对话记忆
- 理解 ChatMemoryProvider 的作用
- 实现对话记忆的持久化存储

## 前置知识

- 完成《01 - LangChain4j 简介》章节
- 完成《02 - 你的第一个 Chat 应用》章节
- 完成《03 - 深入理解 ChatModel》章节
- 完成《04 - 模型参数配置》章节
- 完成《05 - ChatMessage 类型详解》章节

## 核心概念

### 为什么需要 ChatMemory？

**问题：** LLM（大语言模型）本身是无状态的，它不会"记住"之前的对话内容。

**示例：**
```
用户: 你好，我是小明
AI: 你好小明！很高兴认识你。
用户: 我叫什么？
AI: 对不起，我不知道你的名字。
```

**原因：**
- LLM 每次请求都是独立的
- 没有内置的机制在多个请求之间共享状态
- 这就是为什么多轮对话需要手动传递历史

**解决方案：**
`ChatMemory` 是 LangChain4j 提供的机制，用于管理对话历史，使 LLM 能够"记住"之前的对话。

**类比理解：**
如果把 LLM 比作一个健忘的人，那么 `ChatMemory` 就像是他的笔记本，记录了他说过的所有话。

### ChatMemory 接口

```java
public interface ChatMemory {
    
    /**
     * 添加消息到记忆中
     * @param message 要添加的消息
     */
    void add(ChatMessage message);
    
    /**
     * 获取所有消息
     * @return 消息列表（不可变）
     */
    List<ChatMessage> messages();
    
    /**
     * 清空记忆
     */
    void clear();
}
```

### ChatMemory 实现类型

LangChain4j 提供了多种 `ChatMemory` 实现：

| 实现类型 | 管理方式 | 使用场景 |
|---------|---------|---------|
| `MessageWindowChatMemory` | 保留最近的 N 条消息 | 通用场景，简单直接 |
| `TokenWindowChatMemory` | 保留最近的 N 个 Token | 需要 Token 限制的场景 |
| `ChatMemoryRemover` | 装饰器，自动清理旧消息 | 与其他 ChatMemory 组合使用 |

## MessageWindowChatMemory

### 基本用法

`MessageWindowChatMemory` 保留最近固定数量的消息，超过限制时会自动删除最旧的消息。

```java
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.data.message.*;

// 创建一个保留最近 10 条消息的记忆
ChatMemory memory = MessageWindowChatMemory.builder()
        .maxMessages(10)
        .build();
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.data.message.*;

// 创建一个保留最近 10 条消息的记忆
ChatMemory memory = MessageWindowChatMemory.withMaxMessages(10);

// 添加消息
memory.add(UserMessage.from("你好"));
memory.add(AiMessage.from("你好！"));
memory.add(UserMessage.from("我是小明"));

// 获取所有消息
List<ChatMessage> messages = memory.messages();
System.out.println("消息数: " + messages.size());
```

### 完整示例

```java
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.*;
import java.util.Scanner;

/**
 * 使用 ChatMemory 的聊天机器人
 */
public class ChatMemoryExample {

    private final ChatModel model;
    private final ChatMemory memory;
    private final Scanner scanner;

    public ChatMemoryExample() {
        this.model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();
        
        this.memory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();
        this.scanner = new Scanner(System.in);
        
        // 添加系统消息
        memory.add(SystemMessage.from("你是一个友好的助手"));
    }

    public void start() {
        System.out.println("=== 带 ChatMemory 的聊天机器人 ===");
        System.out.println("（能够记住最近 " + memory.maxMessages() + " 条消息）");
        System.out.println();

        while (true) {
            System.out.print("你: ");
            String input = scanner.nextLine().trim();

            if ("exit".equalsIgnoreCase(input)) {
                System.out.println("\n再见！");
                break;
            } else if (input.isEmpty()) {
                continue;
            }

            // 添加用户消息到记忆
            memory.add(UserMessage.from(input));

            // 从记忆获取所有消息并发送给 LLM
            String response = model.chat(memory.messages());

            // 添加 AI 响应到记忆
            memory.add(AiMessage.from(response));

            System.out.println("AI: " + response);
            System.out.println();
            System.out.println("当前记忆: " + memory.messages().size() + " 条消息");
            System.out.println();
        }

        scanner.close();
    }

    public static void main(String[] args) {
        ChatMemoryExample chat = new ChatMemoryExample();
        chat.start();
    }
}
```

### 运行示例

```
=== 带 ChatMemory 的聊天机器人 ===
（能够记住最近 10 条消息）

你: 你好，我是小明
AI: 你好小明！很高兴认识你。
当前记忆: 3 条消息

你: 我叫什么？
AI: 你叫小明。
当前记忆: 5 条消息

你: 我喜欢什么颜色？
AI: 你还没有告诉我你喜欢什么颜色呢。
当前记忆: 7 条消息

你: 我喜欢蓝色
AI: 好的，蓝色是很不错的颜色选择！
当前记忆: 9 条消息

你: 我喜欢什么颜色？
AI: 你喜欢蓝色。
当前记忆: 11 条消息
```

**注意：** 当记忆超过 10 条时，最旧的消息会被自动删除。

### 配置选项

```java
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

// 使用构建器模式
ChatMemory memory1 = MessageWindowChatMemory.builder()
        .maxMessages(10)
        .build();

// 可选：设置用户ID
ChatMemory memory2 = MessageWindowChatMemory.builder()
        .maxMessages(10)
        .id("user123")
        .build();

// 3. 预填充消息
ChatMemory memory3 = MessageWindowChatMemory.builder()
        .maxMessages(10)
        .id("user123")  // 为多用户场景设置 ID
        .build();

// 添加初始消息
memory3.add(SystemMessage.from("你是一个助手"));
memory3.add(UserMessage.from("上一轮对话..."));
```

### 消息保留策略

```java
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.data.message.*;

ChatMemory memory = MessageWindowChatMemory.withMaxMessages(5);

// 添加消息
memory.add(SystemMessage.from("系统消息"));
memory.add(UserMessage.from("消息1"));
memory.add(AiMessage.from("响应1"));
memory.add(UserMessage.from("消息2"));
memory.add(AiMessage.from("响应2"));
memory.add(UserMessage.from("消息3"));  // 超过限制
memory.add(AiMessage.from("响应3"));

// 检查保留的消息
List<ChatMessage> messages = memory.messages();

// 保留：系统消息 + 最近 4 条（总共 5 条）
// 最早的 AI 消息被删除
System.out.println("总消息数: " + messages.size());
for (ChatMessage msg : messages) {
    System.out.println(msg.getClass().getSimpleName());
}
```

**输出：**
```
总消息数: 5
SystemMessage
UserMessage
AiMessage
UserMessage
AiMessage
```

## TokenWindowChatMemory

### 与 MessageWindowChatMemory 的区别

`TokenWindowChatMemory` 根据而不是消息数量来管理记忆。

**为什么使用 TokenWindow？**
- 更精确的成本控制
- 确保 Token 数量不超模型限制
- 适应不同长度的消息

### 基本用法

```java
import dev.langchain4j.memory.chat.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.data.message.*;

// 创建一个保留最近 1000 个 Token 的记忆
// TokenWindowChatMemory需要maxTokens和TokenCountEstimator两个参数
ChatMemory memory = TokenWindowChatMemory.builder()
        .maxTokens(1000, new OpenAiTokenCountEstimator("gpt-4"))
        .build();

// 添加消息
memory.add(UserMessage.from("你好"));
memory.add(AiMessage.from("你好！"));

// 获取所有消息
List<ChatMessage> messages = memory.messages();
```

### 完整示例

```java
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.data.message.*;

/**
 * 使用 TokenWindowChatMemory 的聊天机器人
 */
public class TokenWindowMemoryExample {

    private final ChatModel model;
    private final ChatMemory memory;

    public TokenWindowMemoryExample() {
        this.model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();
        
        // 限制为 1000 个 Token，使用OpenAiTokenCountEstimator估算
        this.memory = TokenWindowChatMemory.builder()
                .maxTokens(1000, new OpenAiTokenCountEstimator("gpt-4"))
                .build();
        
        // 添加系统消息（通常只计算一次）
        memory.add(SystemMessage.from("你是一个友好的助手"));
    }

    public void chat(String userMessage) {
        // 添加用户消息
        memory.add(UserMessage.from(userMessage));

        // 从记忆获取消息并发送给 LLM
        String response = model.chat(memory.messages());

        // 添加 AI 响应到记忆
        memory.add(AiMessage.from(response));

        // 显示统计信息
        System.out.println("用户: " + userMessage);
        System.out.println("AI: " + response);
        System.out.println("当前消息数: " + memory.messages().size());
        System.out.println("估算 Token 数: ~" + estimateTokens(memory.messages()));
        System.out.println();
    }

    private int estimateTokens(List<ChatMessage> messages) {
        int totalTokens = 0;
        for (ChatMessage msg : messages) {
            if (msg instanceof SystemMessage) {
                totalTokens += ((SystemMessage) msg).text().length() / 4; // 估算
            } else if (msg instanceof UserMessage) {
                totalTokens += ((UserMessage) msg).text().length() / 4;
            } else if (msg instanceof AiMessage) {
                totalTokens += ((AiMessage) msg).text().length() / 4;
            }
        }
        return totalTokens;
    }

    public static void main(String[] args) {
        TokenWindowMemoryExample chat = new TokenWindowMemoryExample();
        
        chat.chat("你好");
        chat.chat("我叫小明");
        chat.chat("我喜欢编程");
        chat.chat("Java 是什么？");
        chat.chat("如何学习 Java？");
    }
}
```

### Token 估算

`TokenWindowChatMemory` 需要一个 `TokenCountEstimator` 来估算每条消息的 Token 数量。LangChain4j 提供了 `OpenAiTokenCountEstimator`：

```java
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;

// 使用OpenAiTokenCountEstimator
ChatMemory memory = TokenWindowChatMemory.builder()
        .maxTokens(1000, new OpenAiTokenCountEstimator("gpt-4"))
        .build();
```

**TokenCountEstimator 说明：**
- `OpenAiTokenCountEstimator` - 使用OpenAI的算法精确估算Token数
- 构造函数参数指定模型名称（如 "gpt-4", "gpt-3.5-turbo"）
- 不同模型的Token计算方式可能略有不同

**为什么需要TokenCountEstimator？**
- 消息是不可分割的整体（indivisible）
- 如果一条消息超过Token限制，会被完全删除
- 准确的Token估算可以避免意外删除重要消息

## 多用户场景

### ChatMemoryProvider

对于多用户应用（如聊天机器人服务），每个用户需要独立的记忆。

`ChatMemoryProvider` 接口用于为每个用户创建独立的 `ChatMemory` 实例。

```java
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;

// 创建 ChatMemoryProvider
ChatMemoryProvider memoryProvider = memoryId -> {
    // 为每个用户创建独立的记忆
    return MessageWindowChatMemory.builder()
            .id(memoryId.toString())  // 使用用户 ID
            .maxMessages(10)
            .build();
};

// 定义 AI Service 接口
interface MultiUserAssistant {
    String chat(@dev.langchain4j.service.MemoryId String userId, 
               @UserMessage String message);
}

// 构建多用户 AI Service
MultiUserAssistant assistant = AiServices.builder(MultiUserAssistant.class)
        .chatModel(model)
        .chatMemoryProvider(memoryProvider)  // 设置记忆提供者
        .build();

// 不同用户使用独立的记忆
String response1 = assistant.chat("user123", "你好");
String response2 = assistant.chat("user456", "你好");
```

### 多用户完整示例

```java
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;

/**
 * 多用户聊天机器人
 */
public class MultiUserChatBot {

    private final MultiUserAssistant assistant;

    public MultiUserChatBot() {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        // 为每个用户创建独立的记忆
        ChatMemoryProvider memoryProvider = userId -> {
            ChatMemory memory = MessageWindowChatMemory.builder()
                    .id(userId)
                    .maxMessages(10)
                    .build();
            
            // 添加用户特定的系统消息
            memory.add(dev.langchain4j.data.message.SystemMessage.from(
                "你是一个友好的助手，正在与用户 " + userId + " 对话"
            ));
            
            return memory;
        };

        this.assistant = AiServices.builder(MultiUserAssistant.class)
                .chatModel(model)
                .chatMemoryProvider(memoryProvider)
                .build();
    }

    /**
     * 用户聊天
     * @param userId 用户 ID
     * @param message 用户消息
     * @return AI 响应
     */
    public String chat(String userId, String message) {
        return assistant.chat(userId, message);
    }

    /**
     * 获取用户的对话历史
     * @param userId 用户 ID
     * @return 对话历史
     */
    public List<dev.langchain4j.data.message.ChatMessage> getUserHistory(String userId) {
        // 注意：实际应用中需要暴露获取 ChatMemory 的方法
        // 这里简化了示例
        return List.of();
    }

    public interface MultiUserAssistant {
        String chat(@MemoryId String userId, 
                   @UserMessage String message);
    }

    public static void main(String[] args) {
        MultiUserChatBot bot = new MultiUserChatBot();

        // 用户 1 的对话
        System.out.println("=== 用户 1 (小明）===");
        System.out.println(bot.chat("user1", "你好，我是小明"));
        System.out.println(bot.chat("user1", "我叫什么？"));
        System.out.println(bot.chat("user1", "我喜欢什么颜色？"));
        System.out.println();

        // 用户 2 的对话
        System.out.println("=== 用户 2 (小红）===");
        System.out.println(bot.chat("user2", "你好，我是小红"));
        System.out.println(bot.chat("user2", "我叫什么？"));
        System.out.println(bot.chat("user2", "我喜欢编程"));
        System.out.println();

        // 验证独立性：用户 1 和用户 2 的记忆是独立的
        // 用户 1 的 AI 应该记得小明，但不应该知道小红
        // 用户 2 的 AI 应该记得小红，但不应该知道小明
    }
}
```

## 持久化 ChatMemory

### 为什么需要持久化？

内存中的 `ChatMemory` 在应用重启后会丢失。对于生产环境，需要将对话历史持久化存储到数据库。

### 持久化策略

**常用方案：**
1. **数据库存储** - MySQL、PostgreSQL、MongoDB 等
2. **Redis 缓存** - 高性能，适合会话数据
3. **文件存储** - JSON、序列化对象

### 使用数据库持久化

```java
import dev.langchain4j.memory.chat.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.data.message.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库持久化的 ChatMemory
 */
public class PersistentChatMemory {

    private final ChatMemory memory;
    private final String userId;
    private final Connection dbConnection;

    public PersistentChatMemory(String userId, Connection dbConnection) {
        this.userId = userId;
        this.dbConnection = dbConnection;
        this.memory = MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
        this.memory = MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();

        // 从数据库加载历史
        loadFromDatabase();
    }

    /**
     * 从数据库加载对话历史
     */
    private void loadFromDatabase() {
        try {
            List<dev.langchain4j.data.message.ChatMessage> history = 
                loadConversationHistory(userId);
            
            // 添加到内存
            for (dev.langchain4j.data.message.ChatMessage msg : history) {
                memory.add(msg);
            }
            
            System.out.println("从数据库加载了 " + history.size() + " 条历史消息");
        } catch (Exception e) {
            System.err.println("加载历史失败: " + e.getMessage());
        }
    }

    /**
     * 保存对话历史到数据库
     */
    public void saveToDatabase() {
        try {
            List<dev.langchain4j.data.message.ChatMessage> messages = memory.messages();
            saveConversationHistory(userId, messages);
            System.out.println("保存了 " + messages.size() + " 条消息到数据库");
        } catch (Exception e) {
            System.err.println("保存历史失败: " + e.getMessage());
        }
    }

    /**
     * 添加消息
     */
    public void addMessage(ChatMessage message) {
        memory.add(message);
    }

    /**
     * 获取所有消息
     */
    public List<ChatMessage> getMessages() {
        return memory.messages();
    }

    /**
     * 从数据库加载历史（简化版）
     */
    private List<dev.langchain4j.data.message.ChatMessage> loadConversationHistory(
            String userId) throws SQLException {
        
        List<dev.langchain4j.data.message.ChatMessage> history = new ArrayList<>();
        
        String sql = "SELECT message_data FROM conversation_history " +
                    "WHERE user_id = ? ORDER BY created_at DESC LIMIT 20";
        
        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setString(1, userId);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                // 简化：实际应该反序列化完整的消息对象
                String messageData = rs.getString("message_data");
                
                // 这里应该解析 JSON 并创建 ChatMessage
                // 简化示例：
                if (messageData.startsWith("User:")) {
                    history.add(UserMessage.from(messageData.substring(5)));
                } else if (messageData.startsWith("AI:")) {
                    history.add(AiMessage.from(messageData.substring(3)));
                }
            }
        }
        
        return history;
    }

    /**
     * 保存历史到数据库（简化版）
     */
    private void saveConversationHistory(
            String userId, 
            List<dev.langchain4j.data.message.ChatMessage> messages) throws SQLException {
        
        // 先删除旧历史
        String deleteSql = "DELETE FROM conversation_history WHERE user_id = ?";
        try (PreparedStatement stmt = dbConnection.prepareStatement(deleteSql)) {
            stmt.setString(1, userId);
            stmt.executeUpdate();
        }
        
        // 插入新历史
        String insertSql = "INSERT INTO conversation_history " +
                             "(user_id, message_data, created_at) " +
                             "VALUES (?, ?, NOW())";
        
        try (PreparedStatement stmt = dbConnection.prepareStatement(insertSql)) {
            for (dev.langchain4j.data.message.ChatMessage msg : messages) {
                String messageData = serializeMessage(msg);
                stmt.setString(1, userId);
                stmt.setString(2, messageData);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private String serializeMessage(dev.langchain4j.data.message.ChatMessage msg) {
        // 简化：实际应该使用 JSON 序列化
        if (msg instanceof UserMessage) {
            return "User: " + ((UserMessage) msg).singleText();
        } else if (msg instanceof AiMessage) {
            return "AI: " + ((AiMessage) msg).text();
        }
        return msg.getClass().getSimpleName() + ": " + msg.toString();
    }

    /**
     * 聊天方法
     */
    public String chat(String userMessage) {
        addMessage(UserMessage.from(userMessage));
        
        // 使用模型生成响应
        // 这里简化了模型调用
        String aiResponse = "AI 响应: " + userMessage;
        
        addMessage(AiMessage.from(aiResponse));
        
        // 保存到数据库
        saveToDatabase();
        
        return aiResponse;
    }
}
```

### 使用 Redis 持久化

```java
import dev.langchain4j.memory.chat.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.data.message.*;
import redis.clients.jedis.Jedis;

/**
 * Redis 持久化的 ChatMemory
 */
public class RedisChatMemory {

    private final ChatMemory memory;
    private final String userId;
    private final Jedis jedis;
    private final String redisKeyPrefix;

    public RedisChatMemory(String userId, String redisHost) {
        this.userId = userId;
        this.jedis = new Jedis(redisHost);
        this.redisKeyPrefix = "chat_memory:" + userId + ":";
        this.memory = MessageWindowChatMemory.withMaxMessages(20);

        // 从 Redis 加载历史
        loadFromRedis();
    }

    /**
     * 从 Redis 加载历史
     */
    private void loadFromRedis() {
        try {
            List<dev.langchain4j.data.message.ChatMessage> history = new ArrayList<>();
            
            // 获取所有历史消息的 key
            Set<String> keys = jedis.keys(redisKeyPrefix + "*");
            
            for (String key : keys) {
                String messageData = jedis.get(key);
                if (messageData != null) {
                    // 反序列化消息（简化）
                    ChatMessage msg = deserializeMessage(messageData);
                    history.add(msg);
                }
            }
            
            // 按时间排序（简化：这里假设 key 包含时间戳）
            history.sort((a, b) -> b.getId().compareTo(a.getId()));
            
            // 添加到内存（最多 20 条）
            for (int i = 0; i < Math.min(20, history.size()); i++) {
                memory.add(history.get(i));
            }
            
            System.out.println("从 Redis 加载了 " + memory.messages().size() + " 条消息");
        } catch (Exception e) {
            System.err.println("加载历史失败: " + e.getMessage());
        }
    }

    /**
     * 保存到 Redis
     */
    public void saveToRedis() {
        try {
            // 先删除旧历史
            jedis.del(redisKeyPrefix + "*");
            
            // 保存新历史
            long timestamp = System.currentTimeMillis();
            List<ChatMessage> messages = memory.messages();
            
            for (int i = 0; i < messages.size(); i++) {
                ChatMessage msg = messages.get(i);
                String key = redisKeyPrefix + timestamp + ":" + i;
                String value = serializeMessage(msg);
                jedis.set(key, value);
            }
            
            System.out.println("保存了 " + messages.size() + " 条消息到 Redis");
        } catch (Exception e) {
            System.err.println("保存历史失败: " + e.getMessage());
        }
    }

    /**
     * 添加消息
     */
    public void addMessage(ChatMessage message) {
        memory.add(message);
    }

    /**
     * 获取所有消息
     */
    public List<ChatMessage> getMessages() {
        return memory.messages();
    }

    /**
     * 序列化消息（简化）
     */
    private String serializeMessage(ChatMessage msg) {
        // 实际应用中应该使用 JSON 序列化
        if (msg instanceof UserMessage) {
            return "User:" + ((UserMessage) msg).singleText();
        } else if (msg instanceof AiMessage) {
            return "AI:" + ((AiMessage) msg).text();
        }
        return msg.getClass().getSimpleName();
    }

    /**
     * 反序列化消息（简化）
     */
    private ChatMessage deserializeMessage(String data) {
        if (data.startsWith("User:")) {
            return UserMessage.from(data.substring(4));
        } else if (data.startsWith("AI:")) {
            return AiMessage.from(data.substring(3));
        }
        return UserMessage.from(data);
    }
}
```

## 完整示例：智能记忆管理器

```java
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.*;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能记忆管理器
 */
public class SmartMemoryManager {

    private final Map<String, ChatMemory> userMemories;
    private final ChatModel model;

    public SmartMemoryManager(ChatModel model) {
        this.model = model;
        this.userMemories = new HashMap<>();
    }

    /**
     * 获取或创建用户的记忆
     */
    public ChatMemory getOrCreateUserMemory(String userId) {
        return userMemories.computeIfAbsent(userId, id -> {
            ChatMemory memory = MessageWindowChatMemory.builder()
                    .id(id)
                    .maxMessages(10)
                    .build();
            
            // 添加欢迎消息
            memory.add(SystemMessage.from(
                "你是一个友好的助手，很高兴为你服务！"
            ));
            
            System.out.println("为用户 " + userId + " 创建了新记忆");
            return memory;
        });
    }

    /**
     * 用户聊天
     */
    public String chat(String userId, String message) {
        ChatMemory memory = getOrCreateUserMemory(userId);
        
        // 添加用户消息
        memory.add(UserMessage.from(message));
        
        // 生成响应
        String response = model.chat(memory.messages());
        
        // 添加 AI 响应
        memory.add(AiMessage.from(response));
        
        // 如果记忆满了，清理旧消息
        if (memory.messages().size() > memory.maxMessages()) {
            // MessageWindowChatMemory 会自动清理
            System.out.println("用户 " + userId + " 的记忆已清理");
        }
        
        return response;
    }

    /**
     * 获取用户的对话历史
     */
    public List<ChatMessage> getUserHistory(String userId) {
        ChatMemory memory = userMemories.get(userId);
        return memory != null ? memory.messages() : new ArrayList<>();
    }

    /**
     * 清除用户的记忆
     */
    public void clearUserMemory(String userId) {
        ChatMemory memory = userMemories.get(userId);
        if (memory != null) {
            memory.clear();
            System.out.println("用户 " + userId + " 的记忆已清空");
        }
    }

    /**
     * 获取所有用户的统计
     */
    public void printStatistics() {
        System.out.println("=== 记忆统计 ===");
        System.out.println("总用户数: " + userMemories.size());
        
        int totalMessages = 0;
        for (ChatMemory memory : userMemories.values()) {
            totalMessages += memory.messages().size();
        }
        
        System.out.println("总消息数: " + totalMessages);
        System.out.println("平均每用户: " + 
            (double) totalMessages / userMemories.size());
    }

    /**
     * 保存所有用户记忆（可持久化）
     */
    public void saveAllMemories() {
        // 在实际应用中，这里会将所有用户的对话历史
        // 保存到数据库或文件
        System.out.println("保存了 " + userMemories.size() + " 个用户的记忆");
    }

    /**
     * 加载所有用户记忆（可持久化）
     */
    public void loadAllMemories() {
        // 在实际应用中，这里会从数据库或文件
        // 加载所有用户的对话历史
        System.out.println("加载了用户记忆");
    }

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        SmartMemoryManager manager = new SmartMemoryManager(model);

        // 多用户对话
        System.out.println("=== 用户 1 ===");
        System.out.println(manager.chat("user1", "你好，我是小明"));
        System.out.println(manager.chat("user1", "我叫什么？"));
        System.out.println();

        System.out.println("=== 用户 2 ===");
        System.out.println(manager.chat("user2", "你好，我是小红"));
        System.out.println(manager.chat("user2", "我叫什么？"));
        System.out.println();

        // 打印历史
        System.out.println("用户 1 的历史:");
        manager.getUserHistory("user1").forEach(msg -> {
            System.out.println("  " + msg.getClass().getSimpleName() + ": " + 
                extractText(msg));
        });
        System.out.println();

        System.out.println("用户 2 的历史:");
        manager.getUserHistory("user2").forEach(msg -> {
            System.out.println("  " + msg.getClass().getSimpleName() + ": " + 
                extractText(msg));
        });
        System.out.println();

        // 打印统计
        manager.printStatistics();
    }

    private static String extractText(ChatMessage msg) {
        if (msg instanceof UserMessage) {
            return ((UserMessage) msg).text();
        } else if (msg instanceof AiMessage) {
            return ((AiMessage) msg).text();
        } else if (msg instanceof SystemMessage) {
            return ((SystemMessage) msg).text();
        }
        return msg.toString();
    }
}
```

## 测试代码示例

```java
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.data.message.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class ChatMemoryTest {

    private ChatMemory messageWindowMemory;
    private ChatMemory tokenWindowMemory;

    @BeforeEach
    void setUp() {
        messageWindowMemory = MessageWindowChatMemory.builder()
                .maxMessages(5)
                .build();
        
        tokenWindowMemory = TokenWindowChatMemory.builder()
                .maxTokens(100, new dev.langchain4j.model.openai.OpenAiTokenCountEstimator("gpt-4"))
                .build();
    }

    @Test
    void should_keep_max_messages() {
        // 添加 7 条消息
        for (int i = 0; i < 7; i++) {
            messageWindowMemory.add(UserMessage.from("消息 " + i));
        }
        
        // 应该只保留最近的 5 条（系统消息 + 4 条用户消息）
        // 注意：这里简化了示例，实际会保留系统消息
        List<ChatMessage> messages = messageWindowMemory.messages();
        assertTrue(messages.size() <= 5);
        
        System.out.println("保留的消息数: " + messages.size());
    }

    @Test
    void should_clear_memory() {
        messageWindowMemory.add(UserMessage.from("消息1"));
        messageWindowMemory.add(UserMessage.from("消息2"));
        
        assertFalse(messageWindowMemory.messages().isEmpty());
        
        messageWindowMemory.clear();
        
        assertTrue(messageWindowMemory.messages().isEmpty());
        System.out.println("记忆已清空");
    }

    @Test
    void should_honor_token_limit() {
        // 添加多条消息
        String longMessage = "这是一条很长的消息用于测试 Token 限制...";
        tokenWindowMemory.add(UserMessage.from(longMessage));
        tokenWindowMemory.add(UserMessage.from(longMessage));
        tokenWindowMemory.add(UserMessage.from(longMessage));
        tokenWindowMemory.add(UserMessage.from(longMessage));
        
        // 由于 Token 限制，可能只保留部分消息
        int messageCount = tokenWindowMemory.messages().size();
        assertTrue(messageCount >= 1);
        
        System.out.println("保留的消息数: " + messageCount);
    }

    @Test
    void should_preserve_system_message() {
        ChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(3)
                .build();
        
        memory.add(SystemMessage.from("系统消息"));
        memory.add(UserMessage.from("用户消息1"));
        memory.add(UserMessage.from("用户消息2"));
        memory.add(UserMessage.from("用户消息3")); // 超过限制
        
        List<ChatMessage> messages = memory.messages();
        
        // 系统消息应该总是保留
        boolean hasSystemMessage = messages.stream()
                .anyMatch(msg -> msg instanceof SystemMessage);
        assertTrue(hasSystemMessage);
        
        System.out.println("消息: " + messages);
    }
}
```

## 实践练习

### 练习 1：实现对话清理器

创建一个智能的对话历史清理器：

```java
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.data.message.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 对话清理器 - 智能清理对话历史
 */
public class ConversationCleaner {

    /**
     * 清理相似的消息
     */
    public static void removeSimilarMessages(ChatMemory memory, double similarityThreshold) {
        List<ChatMessage> messages = new ArrayList<>(memory.messages());
        memory.clear();
        
        for (ChatMessage msg : messages) {
            boolean isDuplicate = false;
            
            for (ChatMessage existing : memory.messages()) {
                if (calculateSimilarity(msg, existing) > similarityThreshold) {
                    isDuplicate = true;
                    break;
                }
            }
            
            if (!isDuplicate) {
                memory.add(msg);
            }
        }
        
        System.out.println("清理后剩余 " + memory.messages().size() + " 条消息");
    }

    /**
     * 总结旧消息
     */
    public static void summarizeOldMessages(ChatMemory memory, int keepRecent) {
        List<ChatMessage> messages = new ArrayList<>(memory.messages());
        memory.clear();
        
        // 保留最近的消息
        for (int i = Math.max(0, messages.size() - keepRecent); 
             i < messages.size(); i++) {
            memory.add(messages.get(i));
        }
        
        // 旧消息用一条摘要替代
        if (messages.size() > keepRecent) {
            List<ChatMessage> oldMessages = messages.subList(
                0, 
                Math.max(0, messages.size() - keepRecent)
            );
            
            String summary = generateSummary(oldMessages);
            memory.add(0, AiMessage.from(
                "[对话总结] " + summary
            ));
        }
        
        System.out.println("总结后总消息数: " + memory.messages().size());
    }

    /**
     * 清理系统消息（只保留一个）
     */
    public static void cleanSystemMessages(ChatMemory memory) {
        List<ChatMessage> messages = new ArrayList<>();
        boolean hasSystemMessage = false;
        
        for (ChatMessage msg : memory.messages()) {
            if (msg instanceof SystemMessage && !hasSystemMessage) {
                messages.add(msg);
                hasSystemMessage = true;
            } else if (!(msg instanceof SystemMessage)) {
                messages.add(msg);
            }
        }
        
        memory.clear();
        memory.addAll(messages);
        
        System.out.println("清理后剩余 " + memory.messages().size() + " 条消息");
    }

    private static double calculateSimilarity(ChatMessage msg1, ChatMessage msg2) {
        String text1 = extractText(msg1);
        String text2 = extractText(msg2);
        
        // 简化的相似度计算（使用 Jaccard 相似度）
        // 实际应用中应该使用更复杂的算法
        if (text1.isEmpty() || text2.isEmpty()) {
            return 0.0;
        }
        
        String[] words1 = text1.toLowerCase().split("\\s+");
        String[] words2 = text2.toLowerCase().split("\\s+");
        
        Set<String> set1 = new java.util.HashSet<>(java.util.Arrays.asList(words1));
        Set<String> set2 = new java.util.HashSet<>(java.util.Arrays.asList(words2));
        
        Set<String> intersection = new java.util.HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new java.util.HashSet<>(set1);
        union.addAll(set2);
        
        return (double) intersection.size() / union.size();
    }

    private static String extractText(ChatMessage msg) {
        if (msg instanceof UserMessage) {
            return ((UserMessage) msg).singleText();
        } else if (msg instanceof AiMessage) {
            return ((AiMessage) msg).text();
        } else if (msg instanceof SystemMessage) {
            return ((SystemMessage) msg).text();
        }
        return "";
    }
    private static String extractText(ChatMessage msg) {
        if (msg instanceof UserMessage) {
            return ((UserMessage) msg).singleText();
        } else if (msg instanceof AiMessage) {
            return ((AiMessage) msg).text();
        } else if (msg instanceof SystemMessage) {
            return ((SystemMessage) msg).text();
        }
        return "";
    }

    private static String generateSummary(List<ChatMessage> messages) {
        // 简化：实际应用中应该使用 LLM 生成摘要
        int userMessages = 0;
        int aiMessages = 0;
        
        for (ChatMessage msg : messages) {
            if (msg instanceof UserMessage) {
                userMessages++;
            } else if (msg instanceof AiMessage) {
                aiMessages++;
            }
        }
        
        return String.format(
            "之前的对话包含 %d 条用户消息和 %d 条 AI 消息。",
            userMessages, aiMessages
        );
    }

    public static void main(String[] args) {
        ChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();
        
        // 添加一些消息
        memory.add(SystemMessage.from("你是一个助手"));
        memory.add(UserMessage.from("你好"));
        memory.add(AiMessage.from("你好！"));
        memory.add(UserMessage.from("你好"));
        memory.add(AiMessage.from("你好！"));
        memory.add(UserMessage.from("今天天气怎么样？"));
        memory.add(AiMessage.from("今天天气很好！"));
        memory.add(UserMessage.from("你好"));
        memory.add(AiMessage.from("你好！"));
        memory.add(UserMessage.from("明天天气怎么样？"));
        
        System.out.println("清理前: " + memory.messages().size() + " 条消息");
        
        // 清理相似消息
        removeSimilarMessages(memory, 0.7);
        
        // 清理系统消息
        cleanSystemMessages(memory);
        
        // 显示最终结果
        System.out.println("\n最终消息:");
        memory.messages().forEach(msg -> {
            System.out.println("  " + msg.getClass().getSimpleName() + ": " + extractText(msg));
        });
    }
}
```

### 练习 2：实现带过期时间的记忆

创建一个会话有过期时间的记忆系统：

```java
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.data.message.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 带过期时间的记忆管理
 */
public class ExpirableMemoryManager {

    private final Map<String, ConversationContext> conversations;
    private final long sessionTimeoutMillis;
    private final long cleanupIntervalMillis;

    public ExpirableMemoryManager(long sessionTimeoutMinutes, long cleanupIntervalMinutes) {
        this.conversations = new HashMap<>();
        this.sessionTimeoutMillis = sessionTimeoutMinutes * 60 * 1000;
        this.cleanupIntervalMillis = cleanupIntervalMinutes * 60 * 1000;
        
        // 启动清理线程
        startCleanupThread();
    }

    /**
     * 获取或创建对话上下文
     */
    private ConversationContext getOrCreateContext(String userId) {
        return conversations.computeIfAbsent(userId, id -> {
            ChatMemory memory = MessageWindowChatMemory.builder()
                    .maxMessages(20)
                    .build();
            
            memory.add(SystemMessage.from("你是一个友好的助手"));
            
            return new ConversationContext(memory, System.currentTimeMillis());
        });
    }

    /**
     * 聊天
     */
    public String chat(String userId, String message) {
        ConversationContext context = getOrCreateContext(userId);
        context.updateLastActivity();
        
        context.memory.add(UserMessage.from(message));
        
        String response = simulateResponse(message);
        context.memory.add(AiMessage.from(response));
        
        return response;
    }

    /**
     * 获取对话历史
     */
    public List<ChatMessage> getHistory(String userId) {
        ConversationContext context = conversations.get(userId);
        return context != null ? context.memory.messages() : null;
    }

    /**
     * 清除对话
     */
    public void clearConversation(String userId) {
        conversations.remove(userId);
        System.out.println("用户 " + userId + " 的对话已清除");
    }

    /**
     * 清理过期会话
     */
    public void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        int expiredCount = 0;
        
        for (Map.Entry<String, ConversationContext> entry : conversations.entrySet()) {
            if (now - entry.getValue().lastActivityTime > sessionTimeoutMillis) {
                conversations.remove(entry.getKey());
                expiredCount++;
            }
        }
        
        if (expiredCount > 0) {
            System.out.println("清理了 " + expiredCount + " 个过期会话");
        }
    }

    /**
     * 启动清理线程
     */
    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(cleanupIntervalMillis);
                    cleanupExpiredSessions();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "SessionCleanupThread");
        
        cleanupThread.setDaemon(true);
        cleanupThread.start();
        
        System.out.println("会话清理线程已启动（间隔: " + 
            (cleanupIntervalMillis / 60000) + " 分钟）");
    }

    private static String simulateResponse(String message) {
        // 简化：实际应用中应该使用 LLM
        return "AI 响应: " + message;
    }

    /**
     * 对话上下文
     */
    private static class ConversationContext {
        final ChatMemory memory;
        long lastActivityTime;

        ConversationContext(ChatMemory memory, long lastActivityTime) {
            this.memory = memory;
            this.lastActivityTime = lastActivityTime;
        }

        void updateLastActivity() {
            this.lastActivityTime = System.currentTimeMillis();
        }
    }

    public static void main(String[] args) {
        ExpirableMemoryManager manager = new ExpirableMemoryManager(30, 5); // 30分钟超时，5分钟清理

        String user1 = "user123";
        String user2 = "user456";

        // 用户 1 对话
        System.out.println("=== 用户 1 对话 ===");
        System.out.println(manager.chat(user1, "你好"));
        System.out.println(manager.chat(user1, "我是谁？"));
        
        // 用户 2 对话
        System.out.println("\n=== 用户 2 对话 ===");
        System.out.println(manager.chat(user2, "你好"));
        System.out.println(manager.chat(user2, "我喜欢什么？"));

        // 获取历史
        System.out.println("\n用户 1 历史: " + manager.getHistory(user1));
        System.out.println("用户 2 历史: " + manager.getHistory(user2));

        // 等待过期...
        System.out.println("\n等待会话过期（模拟）...");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 清理过期会话
        manager.cleanupExpiredSessions();

        // 检查历史
        System.out.println("\n清理后:");
        System.out.println("用户 1 历史: " + manager.getHistory(user1));
        System.out.println("用户 2 历史: " + manager.getHistory(user2));
    }
}
```

### 练习 3：实现记忆压缩

创建一个可以压缩旧对话历史的系统：

```java
import dev.langchain4j.memory.chat.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.data.message.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 对话历史压缩器
 */
public class ConversationCompressor {

    /**
     * 压缩旧消息
     */
    public static void compressOldMessages(
            ChatMemory memory, 
            int keepRecent, 
            int maxCompressedMessages) {
        
        List<ChatMessage> messages = new ArrayList<>(memory.messages());
        if (messages.size() <= keepRecent) {
            System.out.println("消息数未超过阈值，无需压缩");
            return;
        }
        
        // 分为旧消息和最近消息
        List<ChatMessage> oldMessages = messages.subList(
            0, 
            messages.size() - keepRecent
        );
        List<ChatMessage> recentMessages = messages.subList(
            messages.size() - keepRecent, 
            messages.size()
        );
        
        // 压缩旧消息
        List<ChatMessage> compressedOldMessages = compressMessages(
            oldMessages, 
            maxCompressedMessages
        );
        
        // 重建记忆
        memory.clear();
        for (ChatMessage msg : compressedOldMessages) {
            memory.add(msg);
        }
        for (ChatMessage msg : recentMessages) {
            memory.add(msg);
        }
        
        System.out.println("压缩前: " + messages.size() + " 条消息");
        System.out.println("压缩后: " + memory.messages().size() + " 条消息");
    }

    /**
     * 压缩消息列表
     */
    private static List<ChatMessage> compressMessages(
            List<ChatMessage> messages, 
            int maxCompressed) {
        
        List<ChatMessage> compressed = new ArrayList<>();
        
        // 1. 保留系统消息
        for (ChatMessage msg : messages) {
            if (msg instanceof SystemMessage) {
                compressed.add(msg);
            }
        }
        
        // 2. 提取关键信息
        List<String> keyPoints = extractKeyPoints(messages, maxCompressed - compressed.size());
        
        // 3. 创建压缩消息
        String compressedText = "[对话摘要] ";
        compressedText += String.join("; ", keyPoints);
        compressed.add(AiMessage.from(compressedText));
        
        return compressed;
    }

    /**
     * 提取对话关键点
     */
    private static List<String> extractKeyPoints(
            List<ChatMessage> messages, 
            int maxPoints) {
        
        // 简化：实际应用中应该使用 LLM 提取关键点
        List<String> points = new ArrayList<>();
        
        for (int i = 0; i < Math.min(maxPoints, messages.size()); i++) {
            ChatMessage msg = messages.get(i);
            String text = extractText(msg);
            
            // 简化的关键点提取
            if (text.length() > 5 && text.length() < 50) {
                points.add(text.substring(0, Math.min(20, text.length())) + "...");
            }
        }
        
        return points;
    }

    private static String extractText(ChatMessage msg) {
        if (msg instanceof UserMessage) {
            return ((UserMessage) msg).singleText();
        } else if (msg instanceof AiMessage) {
            return ((AiMessage) msg).text();
        } else if (msg instanceof SystemMessage) {
            return ((SystemMessage) msg).text();
        }
        return msg.toString();
    }

    public static void main(String[] args) {
        ChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(15)
                .build();
        
        // 添加对话历史
        memory.add(SystemMessage.from("你是一个关于 Java 的助手"));
        memory.add(UserMessage.from("Java 是什么？"));
        memory.add(AiMessage.from("Java 是一种面向对象的编程语言..."));
        memory.add(UserMessage.from("Java 的特点是什么？"));
        memory.add(AiMessage.from("Java 的特点包括：\n1. 平台无关性\n2. 面向对象\n3. 简单易学\n4. 安全性\n5. 高性能"));
        memory.add(UserMessage.from("如何在 Java 中创建线程？"));
        memory.add(AiMessage.from("在 Java 中创建线程有两种方式..."));
        memory.add(UserMessage.from("什么是 Spring 框架？"));
        memory.add(AiMessage.from("Spring 是一个开源的应用框架..."));
        memory.add(UserMessage.from("如何使用 Spring？"));
        memory.add(AiMessage.from("使用 Spring 需要添加依赖..."));
        memory.add(UserMessage.from("什么是 Lombok？"));
        memory.add(AiMessage.from("Lombok 是一个 Java 库..."));
        memory.add(UserMessage.from("如何学习 Java？"));
        memory.add(AiMessage.from("学习 Java 的路径..."));
        
        System.out.println("=== 压缩前 ===");
        System.out.println("总消息数: " + memory.messages().size());
        
        // 压缩旧消息，保留最近 5 条
        compressOldMessages(memory, 5, 3);
        
        System.out.println("\n=== 压缩后 ===");
        System.out.println("总消息数: " + memory.messages().size());
        
        System.out.println("\n当前消息:");
        memory.messages().forEach(msg -> {
            System.out.println("  " + msg.getClass().getSimpleName() + ": " + extractText(msg));
        });
    }
}
```

## 总结

### 本章要点

1. **ChatMemory 的必要性**
   - LLM 本身是无状态的
   - 需要手动传递对话历史
   - `ChatMemory` 自动管理对话历史

2. **MessageWindowChatMemory**
   - 基于消息数量的管理
   - 简单直接，易于使用
   - 适合大多数场景

3. **TokenWindowChatMemory**
   - 基于 Token 数量的管理
   - 更精确的成本控制
   - 适合需要 Token 限制的场景

4. **多用户场景**
   - 使用 `ChatMemoryProvider` 为每个用户创建独立记忆
   - 确保用户间的对话隔离
   - 适合聊天机器人等应用

5. **持久化**
   - 数据库存储（MySQL、PostgreSQL）
   - Redis 缓存（高性能）
   - 文件存储（简单直接）

6. **记忆管理最佳实践**
   - 定期清理和压缩旧消息
   - 实现会话过期机制
   - 合理设置记忆大小限制

### 下一步

在下一章节中，我们将学习：
- EmbeddingModel 和向量化
- 如何将文本转换为向量
- 向量相似度计算
- EmbeddingStore 向量存储

### 常见问题

**Q1：MessageWindowChatMemory 和 TokenWindowChatMemory 应该选择哪个？**

A：
- **MessageWindowChatMemory**：简单场景，不需要精确的 Token 控制
- **TokenWindowChatMemory**：需要精确控制成本，或消息长度差异较大的场景

**Q2：ChatMemory 中的系统消息会被删除吗？**

A：取决于实现。通常 `MessageWindowChatMemory` 会保留系统消息，即使超过限制。但可以通过自定义配置改变此行为。

**Q3：如何实现多用户的对话隔离？**

A：使用 `ChatMemoryProvider`，为每个用户 ID 返回独立的 `ChatMemory` 实例。

**Q4：持久化 ChatMemory 有什么好的方案？**

A：
- **Redis**：适合实时会话，高性能
- **数据库**：适合长期存储，便于查询和分析
- **组合方案**：Redis 用于会话期间，数据库用于归档

**Q5：如何避免记忆泄漏？**

A：
- 实现会话过期机制
- 定期清理不活跃用户的记忆
- 限制每个用户的记忆大小
- 使用滑动窗口策略

## 参考资料

- [LangChain4j ChatMemory 文档](https://docs.langchain4j.dev/tutorials/chat-memory)
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
