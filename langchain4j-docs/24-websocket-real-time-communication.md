---
title: 'WebSocket 实时通信'
description: '学习 LangChain4j 的 WebSocket 实时通信 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 24 - WebSocket 实时通信

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 理解 WebSocket 在 LLM 应用中的作用
- 掌握 LangChain4j 与 Spring WebSocket 的集成
- 学会实现实时流式聊天
- 理解事件驱动架构和 SSE 流
- 掌握连接管理和消息广播
- 实现一个完整的实时聊天应用

## 前置知识

- 完成《01 - LangChain4j 简介》章节
- 完成《12 - 流式输出详解》章节
- WebSocket 基础知识
- Spring Boot WebSocket 基础

## 核心概念

### WebSocket 在 LLM 应用中的优势

**传统 REST API vs WebSocket：**

| 特性 | REST API | WebSocket |
|------|-----------|-----------|
| **通信模式** | 请求-响应 | 全双工实时 |
| **延迟** | 较高（每次请求） | 极低（持久连接）|
| **资源消耗** | HTTP 开销 | 初始握手后开销极小 |
| **流式输出** | 需要轮询或 SSE | 原生支持 |
| **实时性** | 差 | 优秀 |
| **连接数** | 无状态 | 有状态连接 |

**LLM 应用场景：**
- 实时对话（ChatGPT 风格体验）
- 实时代码补全和生成
- 实时协作（多人同时编辑）
- 实时翻译和字幕
- 实时语音转文字

### 架构设计

```
┌─────────────────────────────────────────────────────────┐
│                  WebSocket 架构                          │
├─────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────┐    ┌────────────┐    ┌───────────┐  │
│  │  Client │    │  WebSocket │    │  Handler   │  │
│  │ Browser │◄──►│  Endpoint    │◄──►│  & Service │  │
│  └─────────┘    └────────────┘    └─────┬─────┘  │
│                                   │              │       │
│                                   │         ┌───┘       │
│                                   │         │            │
│                                   │     ┌─────┐       │
│                                   │     │Chat  │       │
│                                   └────►│Model │       │
│                                         └─────┘       │
│                                       │
│                                   ┌───────┐   │
│                                   │Memory │   │
│                                   └───────┘   │
│                                               │
└─────────────────────────────────────────────────────┘
```

## Spring WebSocket 配置

### Maven 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### WebSocket 配置类

```java
package com.example.langchat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册 WebSocket 处理器
        registry.addHandler(new ChatWebSocketHandler(), "/ws/chat")
                .setAllowedOrigins("*");  // 生产环境应该指定具体域名
    }
}
```

## WebSocket 处理器

### 基础 WebSocket 处理器

```java
package com.example.langchat.websocket;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 处理器
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    // 会话存储：Session ID -> Session 信息
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    // 用户会话：User ID -> Session ID
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();

    // AI 服务
    private ChatModel chatModel;

    /**
     * 构造函数
     */
    @Autowired
    public ChatWebSocketHandler(ChatModel chatModel) {
        this.chatModel = chatModel;
        
        // 创建 AI 助手
        this.assistant = createAssistant();
    }

    /**
     * AI 助手
     */
    @AiService
    public interface Assistant {
        String chat(@UserMessage String message);
    }

    private Assistant assistant;

    /**
     * 创建 AI 助手
     */
    private Assistant createAssistant() {
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .systemMessageProvider(chatMemoryId -> 
                    "你是一个友好、乐于助人的 AI 助手。" +
                    "请用简洁、自然的语言回答用户的问题。" +
                    "保持对话的上下文连贯性。"
                )
                .build();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session, 
                                             Map<String, Object> attributes) throws Exception {
        logger.info("WebSocket 连接建立: Session ID = {}", session.getId());

        // 创建会话信息
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setSessionId(session.getId());
        sessionInfo.setUserId((String) attributes.get("userId"));
        sessionInfo.setConnectTime(System.currentTimeMillis());
        sessionInfo.setLastActivityTime(System.currentTimeMillis());

        // 存储会话
        sessions.put(session.getId(), sessionInfo);

        // 存储用户会话
        String userId = sessionInfo.getUserId();
        if (userId != null) {
            userSessions.put(userId, session.getId());
        }

        // 发送欢迎消息
        session.sendMessage(new TextMessage("欢迎连接！你可以开始聊天了。"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, 
                                         TextMessage message) throws Exception {
        String sessionId = session.getId();
        String userId = sessions.get(sessionId).getUserId();
        String text = message.getPayload();

        logger.info("收到消息: Session ID = {}, User ID = {}, Text = {}", 
                    sessionId, userId, text);

        try {
            // 更新活动时间
            sessions.get(sessionId).setLastActivityTime(System.currentTimeMillis());

            // 使用 AI 助手生成响应
            String response = assistant.chat(text);

            // 发送响应
            session.sendMessage(new TextMessage(response));

            logger.info("发送响应: {}", response);

        } catch (Exception e) {
            logger.error("处理消息失败: {}", e.getMessage());
            
            // 发送错误消息
            session.sendMessage(new TextMessage("抱歉，处理您的消息时出错了。"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, 
                                       CloseStatus closeStatus, 
                                       Map<String, Object> attributes) throws Exception {
        String sessionId = session.getId();
        String userId = sessions.get(sessionId).getUserId();

        logger.info("WebSocket 连接关闭: Session ID = {}, User ID = {}, Status = {}", 
                    sessionId, userId, closeStatus);

        // 移除会话
        sessions.remove(sessionId);

        // 移除用户会话
        if (userId != null) {
            userSessions.remove(userId);
        }

        // 记录会话时长
        SessionInfo sessionInfo = sessions.get(sessionId);
        if (sessionInfo != null) {
            long duration = System.currentTimeMillis() - sessionInfo.getConnectTime();
            logger.info("会话时长: {} 秒", duration / 1000);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, 
                                       Throwable exception) throws Exception {
        logger.error("WebSocket 传输错误: Session ID = {}, Error: {}", 
                    session.getId(), exception.getMessage());

        // 发送错误消息（如果可能）
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage("连接出现错误，正在重连..."));
            }
        } catch (IOException e) {
            logger.error("发送错误消息失败", e);
        }
    }

    /**
     * 获取会话信息
     */
    public SessionInfo getSessionInfo(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 获取用户会话
     */
    public String getUserSessionId(String userId) {
        return userSessions.get(userId);
    }

    /**
     * 获取所有活跃会话
     */
    public Map<String, SessionInfo> getAllSessions() {
        return new ConcurrentHashMap<>(sessions);
    }

    /**
     * 发送广播消息
     */
    public void broadcastMessage(String message) {
        for (WebSocketSession session : sessions.values()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                logger.error("发送广播消息失败: Session ID = {}", 
                            session.getSessionId(), e);
            }
        }
    }

    /**
     * 发送给特定用户
     */
    public void sendToUser(String userId, String message) {
        String sessionId = userSessions.get(userId);
        if (sessionId != null) {
            SessionInfo sessionInfo = sessions.get(sessionId);
            if (sessionInfo != null && sessionInfo.getSession().isOpen()) {
                try {
                    sessionInfo.getSession().sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    logger.error("发送用户消息失败: User ID = {}, Session ID = {}", 
                                userId, sessionId, e);
                }
            }
        }
    }

    /**
     * 会话信息
     */
    public static class SessionInfo {
        private String sessionId;
        private String userId;
        private long connectTime;
        private long lastActivityTime;
        private WebSocketSession session;

        // Getters and Setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public long getConnectTime() { return connectTime; }
        public void setConnectTime(long connectTime) { this.connectTime = connectTime; }
        
        public long getLastActivityTime() { return lastActivityTime; }
        public void setLastActivityTime(long lastActivityTime) { 
            this.lastActivityTime = lastActivityTime; 
        }
        
        public WebSocketSession getSession() { return session; }
        public void setSession(WebSocketSession session) { this.session = session; }
    }
}
```

## 流式响应集成

### 流式 WebSocket 处理器

```java
package com.example.langchat.websocket;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 流式 WebSocket 处理器
 */
@Component
public class StreamingChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(StreamingChatWebSocketHandler.class);

    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();

    private StreamingChatModel streamingChatModel;

    /**
     * AI 助手接口
     */
    @AiService
    public interface StreamingAssistant {
        String chat(@UserMessage String message);
    }

    private StreamingAssistant streamingAssistant;

    @Autowired
    public StreamingChatWebSocketHandler(StreamingChatModel model) {
        this.streamingChatModel = model;
        this.streamingAssistant = createAssistant();
    }

    /**
     * 创建流式 AI 助手
     */
    private StreamingAssistant createAssistant() {
        return AiServices.builder(StreamingAssistant.class)
                .streamingChatModel(streamingChatModel)
                .systemMessageProvider(chatMemoryId -> 
                    "你是一个流式 AI 助手。" +
                    "请以流式方式生成响应，" +
                    "逐个 Token 发送内容。" +
                    "保持自然、流畅的输出。"
                )
                .build();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session, 
                                             Map<String, Object> attributes) {
        logger.info("流式 WebSocket 连接建立: {}", session.getId());

        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setSessionId(session.getId());
        sessionInfo.setUserId((String) attributes.get("userId"));
        sessionInfo.setConnectTime(System.currentTimeMillis());

        sessions.put(session.getId(), sessionInfo);

        String userId = sessionInfo.getUserId();
        if (userId != null) {
            userSessions.put(userId, session.getId());
        }

        session.sendMessage(new TextMessage("已连接到流式聊天服务！"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, 
                                         TextMessage message) {
        String sessionId = session.getId();
        String text = message.getPayload();

        logger.info("收到流式消息: Session ID = {}, Text = {}", sessionId, text);

        try {
            // 开始流式生成
            session.sendMessage(new TextMessage("AI: "));

            StringBuilder fullResponse = new StringBuilder();

            streamingAssistant.chat(text);

        } catch (Exception e) {
            logger.error("处理流式消息失败", e);
            try {
                session.sendMessage(new TextMessage("处理失败，请重试。"));
            } catch (IOException io) {
                logger.error("发送错误消息失败", io);
            }
        }
    }

    /**
     * 发送 Token 到客户端
     */
    private void sendToken(WebSocketSession session, String token) {
        try {
            session.sendMessage(new TextMessage(token));
        } catch (IOException e) {
            logger.error("发送 Token 失败: {}", e.getMessage());
        }
    }

    /**
     * 发送流式响应（使用 TokenStream）
     */
    public void streamResponse(WebSocketSession session, String prompt) {
        logger.info("开始流式生成: Session ID = {}, Prompt = {}", 
                    session.getId(), prompt);

        streamingchatModel.chat(prompt, new Consumer<String>() {
            @Override
            public void accept(String token) {
                sendToken(session, token);
            }
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, 
                                       CloseStatus closeStatus, 
                                       Map<String, Object> attributes) {
        String sessionId = session.getId();
        String userId = sessions.get(sessionId).getUserId();

        logger.info("流式 WebSocket 连接关闭: Session ID = {}, User ID = {}", 
                    sessionId, userId);

        sessions.remove(sessionId);
        if (userId != null) {
            userSessions.remove(userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, 
                                       Throwable exception) {
        logger.error("流式 WebSocket 传输错误: {}", exception.getMessage());
    }

    /**
     * 发送广播消息
     */
    public void broadcastMessage(String message) {
        for (SessionInfo sessionInfo : sessions.values()) {
            if (sessionInfo.getSession().isOpen()) {
                try {
                    sessionInfo.getSession().sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    logger.error("发送广播失败: {}", sessionInfo.getSessionId());
                }
            }
        }
    }

    /**
     * 发送给特定用户
     */
    public void sendToUser(String userId, String message) {
        String sessionId = userSessions.get(userId);
        if (sessionId != null) {
            SessionInfo sessionInfo = sessions.get(sessionId);
            if (sessionInfo != null && sessionInfo.getSession().isOpen()) {
                try {
                    sessionInfo.getSession().sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    logger.error("发送用户消息失败: User ID = {}, Session ID = {}", 
                                userId, sessionId, e);
                }
            }
        }
    }

    /**
     * 获取会话信息
     */
    public SessionInfo getSessionInfo(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 会话信息
     */
    public static class SessionInfo {
        private String sessionId;
        private String userId;
        private long connectTime;
        private long lastActivityTime;
        private WebSocketSession session;

        // Getters and Setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public long getConnectTime() { return connectTime; }
        public void setConnectTime(long connectTime) { this.connectTime = connectTime; }
        public long getLastActivityTime() { return lastActivityTime; }
        public void setLastActivityTime(long lastActivityTime) { 
            this.lastActivityTime = lastActivityTime; 
        }
        public WebSocketSession getSession() { return session; }
        public void setSession(WebSocketSession session) { this.session = session; }
    }
}
```

## 连接管理

### 会话管理器

```java
package com.example.langchat.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 会话管理器
 */
@Component
public class SessionManager {

    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

    // 所有会话
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    // 用户到会话的映射
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();

    // 房间（Group Chat）
    private final Map<String, List<String>> rooms = new ConcurrentHashMap<>();

    /**
     * 添加会话
     */
    public void addSession(String sessionId, String userId, String username) {
        SessionInfo info = new SessionInfo();
        info.setSessionId(sessionId);
        info.setUserId(userId);
        info.setUsername(username);
        info.setConnectTime(System.currentTimeMillis());
        info.setLastActivityTime(System.currentTimeMillis());

        sessions.put(sessionId, info);
        userSessions.put(userId, sessionId);

        logger.info("添加会话: Session ID = {}, User ID = {}, Username = {}", 
                    sessionId, userId, username);
    }

    /**
     * 移除会话
     */
    public void removeSession(String sessionId) {
        SessionInfo info = sessions.get(sessionId);
        if (info != null) {
            userSessions.remove(info.getUserId());
            sessions.remove(sessionId);

            logger.info("移除会话: Session ID = {}, User ID = {}", 
                        sessionId, info.getUserId());
        }
    }

    /**
     * 更新用户会话
     */
    public void updateUserSession(String userId, String newSessionId) {
        String oldSessionId = userSessions.get(userId);
        if (oldSessionId != null) {
            // 移除旧会话
            sessions.remove(oldSessionId);
        }

        userSessions.put(userId, newSessionId);
        logger.info("更新用户会话: User ID = {}, 新 Session ID = {}", 
                    userId, newSessionId);
    }

    /**
     * 获取会话信息
     */
    public SessionInfo getSessionInfo(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 获取用户会话
     */
    public String getUserSessionId(String userId) {
        return userSessions.get(userId);
    }

    /**
     * 获取所有会话
     */
    public Map<String, SessionInfo> getAllSessions() {
        return new ConcurrentHashMap<>(sessions);
    }

    /**
     * 获取所有在线用户
     */
    public List<String> getOnlineUsers() {
        return new CopyOnWriteArrayList<>(userSessions.keySet());
    }

    /**
     * 获取会话数量
     */
    public int getSessionCount() {
        return sessions.size();
    }

    /**
     * 获取用户数量
     */
    public int getUserCount() {
        return userSessions.size();
    }

    /**
     * 添加到房间
     */
    public void joinRoom(String userId, String roomId) {
        if (!rooms.containsKey(roomId)) {
            rooms.put(roomId, new CopyOnWriteArrayList<>());
        }

        List<String> roomUsers = rooms.get(roomId);
        if (!roomUsers.contains(userId)) {
            roomUsers.add(userId);
            logger.info("用户 {} 加入房间 {}", userId, roomId);
        }
    }

    /**
     * 离开房间
     */
    public void leaveRoom(String userId, String roomId) {
        List<String> roomUsers = rooms.get(roomId);
        if (roomUsers != null) {
            roomUsers.remove(userId);
            logger.info("用户 {} 离开房间 {}", userId, roomId);
        }
    }

    /**
     * 获取房间用户
     */
    public List<String> getRoomUsers(String roomId) {
        return rooms.get(roomId);
    }

    /**
     * 广播消息到房间
     */
    public void broadcastToRoom(String roomId, String message) {
        List<String> roomUsers = rooms.get(roomId);
        if (roomUsers != null) {
            for (String userId : roomUsers) {
                String sessionId = userSessions.get(userId);
                if (sessionId != null) {
                    SessionInfo info = sessions.get(sessionId);
                    if (info != null && info.getSession().isOpen()) {
                        try {
                            info.getSession().sendMessage(new TextMessage(message));
                        } catch (IOException e) {
                            logger.error("发送房间消息失败: Room ID = {}, User ID = {}", 
                                        roomId, userId, e);
                        }
                    }
                }
            }
        }
    }

    /**
     * 获取会话统计
     */
    public SessionStats getStats() {
        return new SessionStats(
            getSessionCount(),
            getUserCount(),
            rooms.size()
        );
    }

    /**
     * 会话统计
     */
    public static class SessionStats {
        private final int sessionCount;
        private final int userCount;
        private final int roomCount;

        public SessionStats(int sessionCount, int userCount, int roomCount) {
            this.sessionCount = sessionCount;
            this.userCount = userCount;
            this.roomCount = roomCount;
        }

        public int getSessionCount() { return sessionCount; }
        public int getUserCount() { return userCount; }
        public int getRoomCount() { return roomCount; }

        @Override
        public String toString() {
            return String.format("SessionStats{sessions=%d, users=%d, rooms=%d}",
                        sessionCount, userCount, roomCount);
        }
    }

    /**
     * 会话信息
     */
    public static class SessionInfo {
        private String sessionId;
        private String userId;
        private String username;
        private long connectTime;
        private long lastActivityTime;
        private WebSocketSession session;

        // Getters and Setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public long getConnectTime() { return connectTime; }
        public void setConnectTime(long connectTime) { this.connectTime = connectTime; }
        public long getLastActivityTime() { return lastActivityTime; }
        public void setLastActivityTime(long lastActivityTime) { 
            this.lastActivityTime = lastActivityTime; 
        }
        public WebSocketSession getSession() { return session; }
        public void setSession(WebSocketSession session) { this.session = session; }
    }
}
```

## 前端集成

### JavaScript 客户端

```javascript
// WebSocket 客户端
class ChatWebSocket {
    constructor(url) {
        this.url = url;
        this.ws = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectInterval = 5000; // 5 秒
    }

    /**
     * 连接 WebSocket
     */
    connect() {
        try {
            console.log(`连接到 WebSocket: ${this.url}`);
            
            this.ws = new WebSocket(this.url);
            
            this.ws.onopen = (event) => {
                console.log('WebSocket 连接已建立');
                this.reconnectAttempts = 0;
                this.onConnected(event);
            };
            
            this.ws.onmessage = (event) => {
                console.log(`收到消息: ${event.data}`);
                this.onMessage(event);
            };
            
            this.ws.onerror = (event) => {
                console.error('WebSocket 错误:', event);
                this.onError(event);
                this.attemptReconnect();
            };
            
            this.ws.onclose = (event) => {
                console.log('WebSocket 连接已关闭');
                this.onClosed(event);
                
                // 如果不是正常关闭，尝试重连
                if (!event.wasClean) {
                    this.attemptReconnect();
                }
            };

        } catch (error) {
            console.error('连接 WebSocket 失败:', error);
            this.attemptReconnect();
        }
    }

    /**
     * 发送消息
     */
    sendMessage(message) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            console.log(`发送消息: ${message}`);
            this.ws.send(message);
        } else {
            console.warn('WebSocket 未连接，无法发送消息');
            this.attemptReconnect();
        }
    }

    /**
     * 断开连接
     */
    disconnect() {
        if (this.ws) {
            console.log('断开 WebSocket 连接');
            this.ws.close();
            this.ws = null;
        }
    }

    /**
     * 尝试重连
     */
    attemptReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            const delay = this.reconnectInterval * this.reconnectAttempts;
            
            console.log(`尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts}), 延迟: ${delay}ms`);
            
            setTimeout(() => {
                this.connect();
            }, delay);
        } else {
            console.error('已达到最大重试次数，停止重连');
            this.onMaxReconnectAttemptsReached();
        }
    }

    /**
     * 重置重连计数
     */
    resetReconnectAttempts() {
        this.reconnectAttempts = 0;
    }

    /**
     * 连接回调（可重写）
     */
    onConnected(event) {
        // 触发自定义事件
        this.triggerEvent('connected', event);
    }

    onMessage(event) {
        // 解析消息类型
        try {
            const data = JSON.parse(event.data);
            
            if (data.type === 'chat') {
                this.onChatMessage(data.message, data.sender);
            } else if (data.type === 'stream') {
                this.onStreamToken(data.token);
            } else if (data.type === 'error') {
                this.onError(data.message);
            } else if (data.type === 'system') {
                this.onSystemMessage(data.message);
            }
        } catch (error) {
            // 不是 JSON，当作普通文本处理
            this.onTextMessage(event.data);
        }
    }

    onError(event) {
        console.error('WebSocket 错误:', event);
        this.triggerEvent('error', event);
    }

    onClosed(event) {
        console.log('WebSocket 已关闭:', event);
        this.triggerEvent('closed', event);
    }

    onMaxReconnectAttemptsReached() {
        console.warn('已达到最大重试次数');
        this.triggerEvent('maxReconnectAttempts', {});
    }

    onChatMessage(message, sender) {
        console.log(`聊天消息: ${sender}: ${message}`);
        this.triggerEvent('chat', { message, sender });
    }

    onStreamToken(token) {
        console.log(`流式 Token: ${token}`);
        this.triggerEvent('token', { token });
    }

    onTextMessage(text) {
        console.log(`文本消息: ${text}`);
        this.triggerEvent('text', { text });
    }

    onSystemMessage(message) {
        console.log(`系统消息: ${message}`);
        this.triggerEvent('system', { message });
    }

    /**
     * 触发自定义事件
     */
    triggerEvent(eventName, data) {
        const event = new CustomEvent(eventName, data);
        window.dispatchEvent(event);
    }
}

/**
 * 自定义事件
 */
class CustomEvent extends Event {
    constructor(eventName, detail) {
        super(eventName, { detail });
        this.detail = detail;
    }

    get detail() {
        return this.detail;
    }
}

// 使用示例
const chatWebSocket = new ChatWebSocket('ws://localhost:8080/ws/chat');

// 连接
chatWebSocket.connect();

// 发送消息
chatWebSocket.sendMessage('你好，请介绍一下自己');

// 监听消息
window.addEventListener('chat', (event) => {
    console.log(`收到聊天消息: ${event.detail.message}`);
    displayMessage(event.detail.sender, event.detail.message);
});

window.addEventListener('token', (event) => {
    console.log(`收到 Token: ${event.detail.token}`);
    appendToken(event.detail.token);
});

window.addEventListener('system', (event) => {
    console.log(`系统消息: ${event.detail.message}`);
    showSystemMessage(event.detail.message);
});

window.addEventListener('error', (event) => {
    console.error(`错误: ${event.detail}`);
    showErrorMessage(event.detail);
});

window.addEventListener('closed', (event) => {
    console.log('连接已关闭');
    onConnectionClosed();
});

// 断开连接
// chatWebSocket.disconnect();

function displayMessage(sender, message) {
    const chatContainer = document.getElementById('chat-container');
    const messageDiv = document.createElement('div');
    messageDiv.className = 'message';
    messageDiv.innerHTML = `
        <div class="sender">${sender}</div>
        <div class="content">${message}</div>
    `;
    chatContainer.appendChild(messageDiv);
}

function appendToken(token) {
    const currentMessage = document.querySelector('.message:last-child .content');
    if (currentMessage) {
        currentMessage.textContent += token;
    }
}

function showSystemMessage(message) {
    const chatContainer = document.getElementById('chat-container');
    const messageDiv = document.createElement('div');
    messageDiv.className = 'message system';
    messageDiv.textContent = message;
    chatContainer.appendChild(messageDiv);
}

function showErrorMessage(error) {
    const errorDiv = document.getElementById('error-message');
    errorDiv.textContent = error.message || '连接错误';
    errorDiv.style.display = 'block';
}

function onConnectionClosed() {
    const statusDiv = document.getElementById('connection-status');
    statusDiv.textContent = '未连接';
    statusDiv.className = 'status disconnected';
}

// 页面加载时自动连接
window.onload = () => {
    console.log('页面已加载，连接 WebSocket...');
    chatWebSocket.connect();
};
```

### HTML 页面

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>LangChain4j WebSocket 聊天</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 0;
            padding: 0;
            display: flex;
            flex-direction: column;
            height: 100vh;
        }

        .header {
            background-color: #2c3e50;
            color: white;
            padding: 20px;
            text-align: center;
            box-shadow: 0 2px 5px rgba(0,0,0,0.1);
        }

        .header h1 {
            margin: 0;
            font-size: 24px;
        }

        .status {
            margin-top: 10px;
            font-size: 14px;
        }

        .status.connected {
            color: #27ae60;
        }

        .status.disconnected {
            color: #e74c3c;
        }

        .chat-container {
            flex: 1;
            overflow-y: auto;
            padding: 20px;
            background-color: #f9f9f9;
        }

        .message {
            margin-bottom: 15px;
            padding: 10px;
            background-color: white;
            border-radius: 8px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
        }

        .message.system {
            background-color: #ecf0f1;
            color: #7f8c8d;
            font-style: italic;
            text-align: center;
        }

        .message .sender {
            font-weight: bold;
            color: #2c3e50;
            margin-bottom: 5px;
        }

        .message .content {
            line-height: 1.6;
        }

        .input-area {
            padding: 20px;
            background-color: white;
            box-shadow: 0 -2px 5px rgba(0,0,0,0.1);
            display: flex;
            gap: 10px;
        }

        .input-area input {
            flex: 1;
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 4px;
            font-size: 14px;
        }

        .input-area button {
            padding: 10px 25px;
            background-color: #3498db;
            color: white;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 14px;
            font-weight: bold;
            transition: background-color 0.3s;
        }

        .input-area button:hover {
            background-color: #2980b9;
        }

        .error-message {
            display: none;
            padding: 15px;
            background-color: #f8d7da;
            color: #f2dede;
            text-align: center;
            margin: 20px;
            border-radius: 4px;
        }

        .typing-indicator {
            padding: 10px 20px;
            font-style: italic;
            color: #7f8c8d;
            display: none;
        }

        .typing-indicator.show {
            display: block;
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>🤖 LangChain4j WebSocket 聊天</h1>
        <div class="status" id="connection-status">
            <span id="status-text">未连接</span>
        </div>
    </div>

    <div class="chat-container" id="chat-container">
        <!-- 消息将在这里显示 -->
    </div>

    <div class="typing-indicator" id="typing-indicator">
        AI 正在输入...
    </div>

    <div class="input-area">
        <input type="text" id="message-input" placeholder="输入消息..." autocomplete="off">
        <button id="send-button">发送</button>
        <button id="disconnect-button">断开</button>
    </div>

    <div class="error-message" id="error-message"></div>

    <script src="chat-websocket.js"></script>
    <script>
        const messageInput = document.getElementById('message-input');
        const sendButton = document.getElementById('send-button');
        const disconnectButton = document.getElementById('disconnect-button');
        const typingIndicator = document.getElementById('typing-indicator');
        const statusText = document.getElementById('status-text');
        const statusDiv = document.getElementById('connection-status');

        // 更新连接状态
        window.addEventListener('connected', () => {
            statusText.textContent = '已连接';
            statusDiv.className = 'status connected';
        });

        window.addEventListener('closed', () => {
            statusText.textContent = '未连接';
            statusDiv.className = 'status disconnected';
        });

        // 显示/隐藏输入提示
        window.addEventListener('token', () => {
            typingIndicator.classList.add('show');
        });

        window.addEventListener('chat', () => {
            typingIndicator.classList.remove('show');
        });

        // 发送消息
        function sendMessage() {
            const message = messageInput.value.trim();
            if (message) {
                chatWebSocket.sendMessage(message);
                messageInput.value = '';
            }
        }

        // 按钮点击事件
        sendButton.addEventListener('click', sendMessage);
        messageInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                sendMessage();
            }
        });

        // 断开连接
        disconnectButton.addEventListener('click', () => {
            chatWebSocket.disconnect();
        });
    </script>
</body>
</html>
```

## 测试代码

### WebSocket 测试

```java
package com.example.langchat.websocket;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 测试
 */
@SpringBootTest
class WebSocketHandlerTest {

    @Autowired
    private ChatWebSocketHandler chatHandler;

    @Autowired
    private SessionManager sessionManager;

    @Test
    void should_handle_connection() throws Exception {
        // 创建 Mock Session
        WebSocketSession mockSession = mock(WebSocketSession.class);
        when(mockSession.getId()).thenReturn("test-session-123");
        when(mockSession.isOpen()).thenReturn(true);

        // 模拟连接建立
        chatHandler.afterConnectionEstablished(mockSession, Map.of("userId", "user123"));

        // 验证会话已创建
        assertEquals(1, sessionManager.getSessionCount());
        assertNotNull(sessionManager.getSessionInfo("test-session-123"));
        assertEquals("user123", sessionManager.getSessionInfo("test-session-123").getUserId());
    }

    @Test
    void should_handle_message() throws Exception {
        WebSocketSession mockSession = mock(WebSocketSession.class);
        when(mockSession.getId()).thenReturn("test-session-456");
        when(mockSession.isOpen()).thenReturn(true);

        // 模拟消息
        TextMessage message = new TextMessage("测试消息");
        chatHandler.handleTextMessage(mockSession, message);

        // 验证消息已处理
        SessionInfo info = sessionManager.getSessionInfo("test-session-456");
        assertNotNull(info);
        assertTrue(System.currentTimeMillis() - info.getLastActivityTime() < 1000);
    }

    @Test
    void should_handle_disconnect() throws Exception {
        WebSocketSession mockSession = mock(WebSocketSession.class);
        when(mockSession.getId()).thenReturn("test-session-789");
        when(mockSession.isOpen()).thenReturn(false);

        // 模拟关闭
        chatHandler.afterConnectionClosed(
            mockSession,
            CloseStatus.NORMAL,
            Map.of()
        );

        // 验证会话已移除
        assertNull(sessionManager.getSessionInfo("test-session-789"));
    }

    @Test
    void should_broadcast_message() throws Exception {
        // 创建多个会话
        for (int i = 0; i < 5; i++) {
            WebSocketSession mockSession = mock(WebSocketSession.class);
            when(mockSession.getId()).thenReturn("session-" + i);
            when(mockSession.isOpen()).thenReturn(true);
            sessionManager.addSession("session-" + i, "user-" + i, "用户" + i);
        }

        // 广播消息
        chatHandler.broadcastMessage("大家好！");

        // 验证所有会话都收到消息
        for (int i = 0; i < 5; i++) {
            WebSocketSession session = sessionManager.getSessionInfo("session-" + i).getSession();
            verify(session).sendMessage(any(TextMessage.class));
        }
    }

    @Test
    void should_send_to_specific_user() throws Exception {
        // 创建两个会话
        WebSocketSession mockSession1 = mock(WebSocketSession.class);
        WebSocketSession mockSession2 = mock(WebSocketSession.class);

        when(mockSession1.getId()).thenReturn("session-1");
        when(mockSession1.isOpen()).thenReturn(true);
        sessionManager.addSession("session-1", "user-1", "用户1");

        when(mockSession2.getId()).thenReturn("session-2");
        when(mockSession2.isOpen()).thenReturn(true);
        sessionManager.addSession("session-2", "user-2", "用户2");

        // 发送给特定用户
        chatHandler.sendToUser("user-1", "这是给用户1的消息");

        // 验证只有用户1 的会话收到消息
        verify(mockSession1).sendMessage(any(TextMessage.class));
        verify(mockSession2, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void should_handle_room_messages() throws Exception {
        // 创建房间
        String roomId = "room-123";
        
        // 添加用户到房间
        sessionManager.joinRoom("user-1", roomId);
        sessionManager.joinRoom("user-2", roomId);
        sessionManager.joinRoom("user-3", roomId);

        // 创建 Mock 会话
        for (String userId : List.of("user-1", "user-2", "user-3")) {
            String sessionId = sessionManager.getUserSessionId(userId);
            WebSocketSession mockSession = mock(WebSocketSession.class);
            when(mockSession.getId()).thenReturn(sessionId);
            when(mockSession.isOpen()).thenReturn(true);
            sessionManager.getSessionInfo(sessionId).setSession(mockSession);
        }

        // 广播到房间
        sessionManager.broadcastToRoom(roomId, "大家好！");

        // 验证房间内所有用户都收到消息
        verify(sessionManager.getSessionInfo(sessionManager.getUserSessionId("user-1")).getSession())
            .sendMessage(any(TextMessage.class));
        verify(sessionManager.getSessionInfo(sessionManager.getUserSessionId("user-2")).getSession())
            .sendMessage(any(TextMessage.class));
        verify(sessionManager.getSessionInfo(sessionManager.getUserSessionId("user-3")).getSession())
            .sendMessage(any(TextMessage.class));
    }

    @Test
    void should_handle_multiple_users_same_user() throws Exception {
        // 用户1 先登录
        sessionManager.addSession("session-1", "user-1", "用户1");

        // 用户1 再次登录（不同会话）
        WebSocketSession mockSession1 = mock(WebSocketSession.class);
        WebSocketSession mockSession2 = mock(WebSocketSession.class);

        when(mockSession1.getId()).thenReturn("session-1");
        when(mockSession1.isOpen()).thenReturn(true);

        when(mockSession2.getId()).thenReturn("session-2");
        when(mockSession2.isOpen()).thenReturn(true);

        sessionManager.updateUserSession("user-1", "session-2");
        sessionManager.getSessionInfo("session-2").setSession(mockSession2);

        // 发送消息给用户1
        chatHandler.sendToUser("user-1", "测试消息");

        // 验证只有新会话收到消息
        verify(mockSession1, never()).sendMessage(any(TextMessage.class));
        verify(mockSession2).sendMessage(any(TextMessage.class));
    }

    @Test
    void should_return_stats() {
        // 添加一些会话
        for (int i = 0; i < 10; i++) {
            sessionManager.addSession("session-" + i, "user-" + i, "用户" + i);
        sessionManager.joinRoom("user-" + i, "common-room");
        sessionManager.joinRoom("user-" + (i % 2), "private-room");
        sessionManager.joinRoom("user-" + (i % 3), "group-room");
        sessionManager.joinRoom("user-" + (i % 5), "special-room");
        sessionManager.joinRoom("user-" + i, "unique-room-" + i);
        sessionManager.joinRoom("user-" + i, "shared-room");
        sessionManager.joinRoom("user-" + i, "team-room");
        sessionManager.joinRoom("user-" + i, "work-room");
        sessionManager.joinRoom("user-" + i, "meeting-room");
        sessionManager.joinRoom("user-" + i, "project-room");
    }

    // 简化：实际测试中不会添加这么多房间
    SessionManager.SessionStats stats = sessionManager.getStats();

    assertEquals(10, stats.getSessionCount());
    assertEquals(10, stats.getUserCount());
    assertTrue(stats.getRoomCount() > 0);
}
```

## 实践练习

### 练习 1：实现多房间聊天

```java
package com.example.langchat.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 房间聊天
 */
public class RoomChat {

    private static final Logger logger = LoggerFactory.getLogger(RoomChat.class);

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, String> userRooms = new ConcurrentHashMap<>();

    /**
     * 创建房间
     */
    public String createRoom(String creatorId, String roomName, String description, int maxUsers) {
        String roomId = "room-" + System.currentTimeMillis() + "-" + Integer.toHexString((int) (Math.random() * 10000));

        Room room = new Room();
        room.setRoomId(roomId);
        room.setName(roomName);
        room.setDescription(description);
        room.setCreatorId(creatorId);
        room.setMaxUsers(maxUsers);
        room.setCreatedTime(System.currentTimeMillis());

        rooms.put(roomId, room);

        logger.info("创建房间: ID = {}, 名称 = {}, 创建者 = {}", 
                    roomId, roomName, creatorId);

        // 创建者自动加入
        joinRoom(creatorId, roomId);

        return roomId;
    }

    /**
     * 加入房间
     */
    public boolean joinRoom(String userId, String roomId) {
        Room room = rooms.get(roomId);

        if (room == null) {
            logger.warn("房间不存在: {}", roomId);
            return false;
        }

        if (room.getUsers().contains(userId)) {
            logger.info("用户 {} 已在房间 {}", userId, roomId);
            return true;
        }

        if (room.getUsers().size() >= room.getMaxUsers()) {
            logger.warn("房间 {} 已满", roomId);
            return false;
        }

        room.addUser(userId);
        userRooms.put(userId, roomId);

        logger.info("用户 {} 加入房间 {}", userId, roomId);

        // 通知房间其他用户
        broadcastToRoom(roomId, String.format("用户 %s 加入了聊天室", userId));

        return true;
    }

    /**
     * 离开房间
     */
    public boolean leaveRoom(String userId, String roomId) {
        Room room = rooms.get(roomId);

        if (room == null) {
            logger.warn("房间不存在: {}", roomId);
            return false;
        }

        room.removeUser(userId);
        userRooms.remove(userId);

        logger.info("用户 {} 离开房间 {}", userId, roomId);

        // 通知房间其他用户
        broadcastToRoom(roomId, String.format("用户 %s 离开了聊天室", userId));

        return true;
    }

    /**
     * 发送房间消息
     */
    public void sendToRoom(String userId, String roomId, String message) {
        Room room = rooms.get(roomId);

        if (room == null) {
            logger.warn("房间不存在: {}", roomId);
            return;
        }

        if (!room.getUsers().contains(userId)) {
            logger.warn("用户 {} 不在房间 {}", userId, roomId);
            return;
        }

        // 构建消息对象
        RoomMessage roomMessage = new RoomMessage();
        roomMessage.setUserId(userId);
        roomMessage.setRoomId(roomId);
        roomMessage.setMessage(message);
        roomMessage.setTimestamp(System.currentTimeMillis());

        // 广播到房间
        for (String roomUserId : room.getUsers()) {
            String sessionId = sessionManager.getUserSessionId(roomUserId);
            if (sessionId != null) {
                SessionInfo info = sessionManager.getSessionInfo(sessionId);
                if (info != null && info.getSession().isOpen()) {
                    try {
                        info.getSession().sendMessage(new TextMessage(roomMessage.toJson()));
                    } catch (IOException e) {
                        logger.error("发送房间消息失败: Room ID = {}, User ID = {}", 
                                    roomId, roomUserId, e);
                    }
                }
            }
        }
    }

    /**
     * 获取房间列表
     */
    public List<Room> getRoomList() {
        return new CopyOnWriteArrayList<>(rooms.values());
    }

    /**
     * 获取房间信息
     */
    public Room getRoomInfo(String roomId) {
        return rooms.get(roomId);
    }

    /**
     * 获取用户所在的房间
     */
    public String getUserRoomId(String userId) {
        return userRooms.get(userId);
    }

    /**
     * 广播到房间
     */
    private void broadcastToRoom(String roomId, String message) {
        Room room = rooms.get(roomId);

        if (room != null) {
            for (String userId : room.getUsers()) {
                String sessionId = sessionManager.getUserSessionId(userId);
                if (sessionId != null) {
                    SessionInfo info = sessionManager.getSessionInfo(sessionId);
                    if (info != null && info.getSession().isOpen()) {
                        try {
                            info.getSession().sendMessage(new TextMessage(message));
                        } catch (IOException e) {
                            logger.error("发送房间消息失败: Room ID = {}, User ID = {}", 
                                        roomId, userId, e);
                        }
                    }
                }
            }
        }
    }

    /**
     * 房间
     */
    public static class Room {
        private String roomId;
        private String name;
        private String description;
        private String creatorId;
        private int maxUsers;
        private long createdTime;
        private final List<String> users;
        private final List<RoomMessage> messages;

        public Room(String roomId, String name, String description, 
                      String creatorId, int maxUsers) {
            this.roomId = roomId;
            this.name = name;
            this.description = description;
            this.creatorId = creatorId;
            this.maxUsers = maxUsers;
            this.createdTime = System.currentTimeMillis();
            this.users = new CopyOnWriteArrayList<>();
            this.messages = new CopyOnWriteArrayList<>();
        }

        public void addUser(String userId) {
            users.add(userId);
        }

        public void removeUser(String userId) {
            users.remove(userId);
        }

        public void addMessage(RoomMessage message) {
            messages.add(message);
        }

        // Getters
        public String getRoomId() { return roomId; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getCreatorId() { return creatorId; }
        public int getMaxUsers() { return maxUsers; }
        public long getCreatedTime() { return createdTime; }
        public List<String> getUsers() { return new CopyOnWriteArrayList<>(users); }
        public List<RoomMessage> getMessages() { return new CopyOnWriteArrayList<>(messages); }
    }

    /**
     * 房间消息
     */
    public static class RoomMessage {
        private String messageId;
        private String userId;
        private String roomId;
        private String message;
        private long timestamp;

        public RoomMessage(String userId, String roomId, String message) {
            this.userId = userId;
            this.roomId = roomId;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters and Setters
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getRoomId() { return roomId; }
        public void setRoomId(String roomId) { this.roomId = roomId; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public String toJson() {
            return String.format(
                "{\"messageId\":\"%s\",\"userId\":\"%s\",\"roomId\":\"%s\",\"message\":\"%s\",\"timestamp\":%d}",
                messageId,
                userId,
                roomId,
                message,
                timestamp
            );
        }
    }

    // 注入 SessionManager（需要修改 SessionManager 以支持此接口）
    private SessionManager sessionManager;

    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public static void main(String[] args) {
        RoomChat roomChat = new RoomChat();

        // 创建房间
        String roomId = roomChat.createRoom(
            "user-123",
            "LangChain4j 爱好者",
            "讨论 LangChain4j 框架的使用和最佳实践",
            50
        );

        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║              房间聊天                              ║");
        System.out.println("╠═════════════════════════════════════════════════════════════════╣");
        System.out.println("║ 房间 ID: " + roomId);
        System.out.println("╠═══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ 名称: LangChain4j 爱好者                               ║");
        System.out.println("║ 描述: 讨论 LangChain4j 框架的使用和最佳实践                       ║");
        System.out.println("║ 创建者: user-123                                          ║");
        System.out.println("║ 最大人数: 50                                                ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ 用户列表:                                                ║");
        System.out.println("║   - user-123 (创建者)                                    ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ 消息历史:                                                ║");
        System.out.println("║   暂无消息                                                  ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ 创建时间: " + java.time.Instant.ofEpochMilli(rooms.get(roomId).getCreatedTime()) +
                          "                                                  ║");
        System.out.println("╚═════════════════════════════════════════════════════════════════════════════╝");
    }
}
```

## 总结

### 本章要点

1. **WebSocket 优势**
   - 实时双向通信
   - 低延迟连接
   - 持久连接节省资源
   - 原生支持流式输出

2. **Spring WebSocket**
   - 注解配置
   - 自动端点注册
   - 消息处理器
   - 拦截器支持

3. **连接管理**
   - 会话状态跟踪
   - 用户会话映射
   - 心跳检测
   - 断线重连

4. **流式响应**
   - 逐 Token 输出
   - 流式事件监听
   - 进度反馈
   - 错误恢复

5. **最佳实践**
   - 实现心跳机制
   - 限制消息频率
   - 处理大量并发连接
   - 实现消息队列
   - 提供重连机制

### 下一步

在下一章节中，我们将学习：
- 高级流式处理
- 异步消息处理
- 消息队列集成
- 性能优化
- 安全和认证

### 常见问题

**Q1：WebSocket 和 SSE 有什么区别？**

A：主要区别：
1. WebSocket 是全双工，SSE 是单向
2. WebSocket 需要特殊握手，SSE 基于标准 HTTP
3. WebSocket 更复杂，SSE 更简单
4. WebSocket 适合实时对话，SSE 适合单向数据流
5. LLM 应用通常 WebSocket 更好，但 SSE 也可以

**Q2：如何处理大量并发连接？**

A：优化策略：
1. 使用连接池
2. 限制单用户连接数
3. 实现负载均衡
4. 使用异步处理
5. 优化消息队列

**Q3：如何实现消息持久化？**

A：持久化方案：
1. 数据库存储消息历史
2. Redis 缓存在线消息
3. 消息队列处理离线消息
4. 消息过期机制
5. 分页加载历史

**Q4：如何保证消息顺序？**

A：保证方案：
1. 单线程处理每个会话
2. 使用有序消息队列
3. 消息 ID 顺序递增
4. 时间戳排序
5. 客户端排序显示

**Q5：如何处理网络中断？**

A：处理策略：
1. 实现心跳检测
2. 客户端自动重连
3. 服务器会话恢复
4. 消息重发机制
5. 离线消息存储

## 参考资料

- [LangChain4j 文档](https://docs.langchain4j.dev/tutorials/websocket-real-time-communication)
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
