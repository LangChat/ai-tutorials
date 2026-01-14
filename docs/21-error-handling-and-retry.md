---
title: '错误处理和重试'
description: '学习 LangChain4j 的 错误处理和重试 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 21 - 错误处理和重试

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 理解 LangChain4j 中错误处理的重要性
- 掌握常见的错误类型和处理策略
- 学会实现自动重试机制
- 理解超时和网络错误的处理
- 掌握回退策略和错误恢复
- 实现一个完整的错误处理解决方案

## 前置知识

- 完成《01 - LangChain4j 简介》章节
- 完成《02 - 你的第一个 Chat 应用》章节
- Java 异常处理基础

## 核心概念

### 为什么需要错误处理？

**LLM 应用的错误类型：**

1. **网络错误**
   - 连接超时
   - DNS 解析失败
   - SSL/TLS 握手错误

2. **API 错误**
   - 认证失败（401）
   - 速率限制（429）
   - 服务不可用（503）
   - 服务器错误（500）

3. **模型错误**
   - 模型不存在
   - Token 超限
   - 上下文超长

4. **客户端错误**
   - 请求格式错误
   - 参数验证失败
   - JSON 解析错误

### 错误处理策略

```
┌─────────────────────────────────────────────────────────────────┐
│                    错误处理策略                            │
├─────────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 立即失败          2. 简单重试          3. 指数退避          │
│     (严重错误)           (临时错误)           (网络错误)          │
│                                                             │
│  4. 熔断器               5. 熔断重试           6. 熔断恢复          │
│  (保护服务)             (保护服务)           (自动恢复)           │
│                                                             │
└─────────────────────────────────────────────────────────────────┘
```

## 基础错误处理

### try-catch 示例

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基础错误处理示例
 */
public class BasicErrorHandling {

    private static final Logger logger = LoggerFactory.getLogger(BasicErrorHandling.class);

    /**
     * 简单的对话方法（带错误处理）
     */
    public static String chatWithErrorHandling(ChatModel model, String message) {
        try {
            // 尝试生成响应
            logger.info("开始聊天，消息: {}", message);
            String response = model.chat(message);
            logger.info("聊天成功");
            return response;

        } catch (java.net.SocketTimeoutException e) {
            // 处理超时错误
            logger.error("请求超时: {}", e.getMessage());
            throw new RuntimeException("请求超时，请稍后重试", e);

        } catch (java.net.ConnectException e) {
            // 处理连接错误
            logger.error("连接失败: {}", e.getMessage());
            throw new RuntimeException("无法连接到服务器", e);

        } catch (java.io.IOException e) {
            // 处理 IO 错误
            logger.error("IO 错误: {}", e.getMessage());
            throw new RuntimeException("网络通信错误", e);

        } catch (Exception e) {
            // 处理未知错误
            logger.error("未知错误: {}", e.getMessage());
            throw new RuntimeException("发生未知错误", e);
        }
    }

    /**
     * 创建带错误处理的模型
     */
    public static ChatModel createModelWithErrorHandling(String apiKey) {
        try {
            return OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName("gpt-4o-mini")
                    .timeout(Duration.ofSeconds(30))
                    .build();
        } catch (Exception e) {
            logger.error("创建模型失败: {}", e.getMessage());
            throw new RuntimeException("无法创建模型", e);
        }
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("OPENAI_API_KEY");

        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("未设置 OPENAI_API_KEY 环境变量");
            return;
        }

        // 创建模型
        ChatModel model = createModelWithErrorHandling(apiKey);

        // 带错误处理的聊天
        try {
            String response = chatWithErrorHandling(model, "你好，请介绍一下自己");
            System.out.println("AI: " + response);
        } catch (RuntimeException e) {
            System.err.println("错误: " + e.getMessage());
        }
    }
}
```

## 自动重试机制

### 指数退避重试

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * 指数退避重试示例
 */
public class ExponentialBackoffRetry {

    private static final Logger logger = LoggerFactory.getLogger(ExponentialBackoffRetry.class);

    /**
     * 重试配置
     */
    public static class RetryConfig {
        private final int maxRetries;
        private final Duration initialDelay;
        private final Duration maxDelay;
        private final double backoffFactor;
        private final Class<? extends Exception>[] retryableExceptions;
        private final Class<? extends Exception>[] nonRetryableExceptions;

        public RetryConfig(
                int maxRetries,
                Duration initialDelay,
                Duration maxDelay,
                double backoffFactor,
                Class<? extends Exception>[] retryableExceptions,
                Class<? extends Exception>[] nonRetryableExceptions
        ) {
            this.maxRetries = maxRetries;
            this.initialDelay = initialDelay;
            this.maxDelay = maxDelay;
            this.backoffFactor = backoffFactor;
            this.retryableExceptions = retryableExceptions;
            this.nonRetryableExceptions = nonRetryableExceptions;
        }

        // Getters
        public int getMaxRetries() { return maxRetries; }
        public Duration getInitialDelay() { return initialDelay; }
        public Duration getMaxDelay() { return maxDelay; }
        public double getBackoffFactor() { return backoffFactor; }
        public Class<? extends Exception>[] getRetryableExceptions() { return retryableExceptions; }
        public Class<? extends Exception>[] getNonRetryableExceptions() { return nonRetryableExceptions; }
    }

    /**
     * 重试结果
     */
    public static class RetryResult<T> {
        private final T result;
        private final int attemptCount;
        private final long totalDelayMs;
        private final Exception lastException;

        public RetryResult(T result, int attemptCount, long totalDelayMs, Exception lastException) {
            this.result = result;
            this.attemptCount = attemptCount;
            this.totalDelayMs = totalDelayMs;
            this.lastException = lastException;
        }

        public T getResult() { return result; }
        public int getAttemptCount() { return attemptCount; }
        public long getTotalDelayMs() { return totalDelayMs; }
        public Exception getLastException() { return lastException; }
    }

    /**
     * 创建默认重试配置
     */
    public static RetryConfig createDefaultRetryConfig() {
        return new RetryConfig(
                3,                                      // 最多重试 3 次
                Duration.ofMillis(1000),              // 初始延迟 1 秒
                Duration.ofMillis(10000),             // 最大延迟 10 秒
                2.0,                                    // 指数退避因子
                new Class[]{
                    java.net.SocketTimeoutException.class,
                    java.net.ConnectException.class,
                    java.io.IOException.class
                },
                new Class[]{
                    IllegalArgumentException.class,
                    NullPointerException.class
                }
        );
    }

    /**
     * 带重试的方法执行
     */
    public static <T> T executeWithRetry(
            String operationName,
            RetryOperation<T> operation,
            RetryConfig config
    ) throws Exception {
        int attempt = 0;
        Exception lastException = null;
        long totalDelayMs = 0;

        while (attempt <= config.getMaxRetries()) {
            attempt++;

            try {
                logger.info("执行 '{}', 尝试: {}/{}", operationName, attempt, config.getMaxRetries());

                // 执行操作
                T result = operation.execute();
                
                logger.info("操作 '{}' 成功，尝试次数: {}", operationName, attempt);
                return result;

            } catch (Exception e) {
                lastException = e;

                // 检查是否为不可重试的错误
                if (isNonRetryableException(e, config)) {
                    logger.error("操作 '{}' 失败，错误不可重试: {}", operationName, e.getMessage());
                    throw new RuntimeException("操作失败: " + operationName, e);
                }

                // 检查是否已达到最大重试次数
                if (attempt > config.getMaxRetries()) {
                    logger.error("操作 '{}' 失败，已达到最大重试次数: {}", 
                                operationName, config.getMaxRetries());
                    throw new RuntimeException(
                        String.format("操作 '%s' 失败，已重试 %d 次", operationName, config.getMaxRetries()),
                        lastException
                    );
                }

                // 计算延迟
                long delayMs = calculateDelayMs(attempt, config);
                totalDelayMs += delayMs;

                logger.warn("操作 '{}' 失败（尝试 {}/{}），{} ms 后重试: {}", 
                            operationName, attempt, config.getMaxRetries(),
                            delayMs, e.getMessage());

                // 等待重试
                Thread.sleep(delayMs);
            }
        }

        // 不应该到达这里
        throw new RuntimeException("重试逻辑错误", lastException);
    }

    /**
     * 计算延迟
     */
    private static long calculateDelayMs(int attempt, RetryConfig config) {
        double delayMs = config.getInitialDelay().toMillis() * 
                       Math.pow(config.getBackoffFactor(), attempt - 1);
        long delay = (long) delayMs;

        // 限制最大延迟
        return Math.min(delay, config.getMaxDelay().toMillis());
    }

    /**
     * 判断是否为不可重试的错误
     */
    private static boolean isNonRetryableException(Exception e, RetryConfig config) {
        for (Class<? extends Exception> exceptionClass : config.getNonRetryableExceptions()) {
            if (exceptionClass.isInstance(e)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 重试操作接口
     */
    public interface RetryOperation<T> {
        T execute() throws Exception;
    }

    /**
     * 使用示例
     */
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
                .timeout(Duration.ofSeconds(5))  // 短超时以触发重试
                .build();

        // 创建重试配置
        RetryConfig config = createDefaultRetryConfig();

        // 创建重试操作
        RetryOperation<String> operation = () -> {
            return model.chat("你好，请介绍一下自己");
        };

        // 带重试执行
        try {
            String result = executeWithRetry("聊天", operation, config);
            System.out.println("AI: " + result);
        } catch (RuntimeException e) {
            System.err.println("错误: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("根本原因: " + e.getCause().getMessage());
            }
        }
    }
}
```

## 断路器模式

### 熔断器实现

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 断路器模式示例
 */
public class CircuitBreakerExample {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerExample.class);

    /**
     * 断路器状态
     */
    public enum CircuitState {
        CLOSED,    // 正常状态，允许请求
        OPEN,       // 熔断状态，拒绝请求
        HALF_OPEN   // 半开状态，允许少量请求以测试恢复
    }

    /**
     * 断路器配置
     */
    public static class CircuitBreakerConfig {
        private final int failureThreshold;
        private final Duration timeout;
        private final Duration resetTimeout;
        private final int halfOpenMaxCalls;

        public CircuitBreakerConfig(
                int failureThreshold,
                Duration timeout,
                Duration resetTimeout,
                int halfOpenMaxCalls
        ) {
            this.failureThreshold = failureThreshold;
            this.timeout = timeout;
            this.resetTimeout = resetTimeout;
            this.halfOpenMaxCalls = halfOpenMaxCalls;
        }

        // Getters
        public int getFailureThreshold() { return failureThreshold; }
        public Duration getTimeout() { return timeout; }
        public Duration getResetTimeout() { return resetTimeout; }
        public int getHalfOpenMaxCalls() { return halfOpenMaxCalls; }
    }

    /**
     * 断路器
     */
    public static class CircuitBreaker {
        private final CircuitBreakerConfig config;
        private final AtomicInteger failureCount;
        private final AtomicInteger successCount;
        private volatile CircuitState state;
        private volatile long lastFailureTime;

        public CircuitBreaker(CircuitBreakerConfig config) {
            this.config = config;
            this.failureCount = new AtomicInteger(0);
            this.successCount = new AtomicInteger(0);
            this.state = CircuitState.CLOSED;
        }

        /**
         * 获取当前状态
         */
        public CircuitState getState() {
            return state;
        }

        /**
         * 允许请求
         */
        public synchronized boolean allowRequest() {
            switch (state) {
                case OPEN:
                    long timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime;
                    if (timeSinceLastFailure > config.getResetTimeout().toMillis()) {
                        state = CircuitState.HALF_OPEN;
                        successCount.set(0);
                        logger.info("断路器从 OPEN 转换到 HALF_OPEN");
                    } else {
                        logger.warn("断路器状态 OPEN，拒绝请求");
                    }
                    return state == CircuitState.CLOSED || state == CircuitState.HALF_OPEN;

                case HALF_OPEN:
                    logger.info("断路器状态 HALF_OPEN，允许测试请求");
                    return successCount.get() < config.getHalfOpenMaxCalls();

                case CLOSED:
                default:
                    return true;
            }
        }

        /**
         * 记录成功
         */
        public synchronized void recordSuccess() {
            if (state == CircuitState.HALF_OPEN) {
                int currentSuccess = successCount.incrementAndGet();
                logger.info("断路器 HALF_OPEN 状态，成功次数: {}", currentSuccess);

                if (currentSuccess >= config.getHalfOpenMaxCalls()) {
                    state = CircuitState.CLOSED;
                    failureCount.set(0);
                    logger.info("断路器从 HALF_OPEN 转换到 CLOSED");
                }
            } else {
                logger.info("断路器状态 CLOSED，记录成功");
                successCount.incrementAndGet();
            }
        }

        /**
         * 记录失败
         */
        public synchronized void recordFailure() {
            int currentFailures = failureCount.incrementAndGet();
            lastFailureTime = System.currentTimeMillis();

            logger.warn("断路器记录失败，当前失败数: {}, 阈值: {}", 
                        currentFailures, config.getFailureThreshold());

            if (currentFailures >= config.getFailureThreshold()) {
                state = CircuitState.OPEN;
                logger.error("断路器从 {} 转换到 OPEN", state);
            }

            if (state == CircuitState.HALF_OPEN) {
                state = CircuitState.OPEN;
                logger.error("断路器从 HALF_OPEN 转换到 OPEN");
            }
        }

        /**
         * 重置断路器
         */
        public synchronized void reset() {
            state = CircuitState.CLOSED;
            failureCount.set(0);
            successCount.set(0);
            logger.info("断路器已重置");
        }
    }

    /**
     * 带断路器的方法执行
     */
    public static <T> T executeWithCircuitBreaker(
            String operationName,
            RetryOperation<T> operation,
            CircuitBreaker circuitBreaker
    ) throws Exception {
        // 检查是否允许请求
        if (!circuitBreaker.allowRequest()) {
            throw new RuntimeException(
                String.format("断路器状态 %s，拒绝操作 '%s'", 
                            circuitBreaker.getState(), operationName)
            );
        }

        try {
            // 执行操作
            T result = operation.execute();
            
            // 记录成功
            circuitBreaker.recordSuccess();
            
            logger.info("操作 '{}' 成功", operationName);
            return result;

        } catch (Exception e) {
            // 记录失败
            circuitBreaker.recordFailure();
            
            logger.error("操作 '{}' 失败: {}", operationName, e.getMessage());
            throw e;
        }
    }

    /**
     * 重试操作接口
     */
    public interface RetryOperation<T> {
        T execute() throws Exception;
    }

    /**
     * 使用示例
     */
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
                .timeout(Duration.ofSeconds(5))
                .build();

        // 创建断路器配置
        CircuitBreakerConfig config = new CircuitBreakerConfig(
                5,                      // 失败阈值
                Duration.ofSeconds(10),  // 超时
                Duration.ofSeconds(30),  // 重置时间
                2                       // 半开最大调用
        );

        // 创建断路器
        CircuitBreaker circuitBreaker = new CircuitBreaker(config);

        // 创建重试操作
        RetryOperation<String> operation = () -> {
            return model.chat("测试消息");
        };

        // 执行多次请求
        for (int i = 0; i < 10; i++) {
            try {
                String result = executeWithCircuitBreaker("聊天", operation, circuitBreaker);
                System.out.printf("[请求 %d] 成功: %s\n", i + 1, result);
                System.out.println();

            } catch (Exception e) {
                System.err.printf("[请求 %d] 失败: %s\n\n", i + 1, e.getMessage());
            }
        }

        // 重置断路器
        System.out.println("重置断路器");
        circuitBreaker.reset();
    }
}
```

## 错误恢复

### 优雅降级

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 优雅降级示例
 */
public class GracefulDegradation {

    private static final Logger logger = LoggerFactory.getLogger(GracefulDegradation.class);

    /**
     * 响应类型
     */
    public enum ResponseType {
        FULL,           // 完整响应
        CACHED,         // 缓存响应
        SIMPLIFIED,     // 简化响应
        FALLBACK        // 降级响应
    }

    /**
     * 响应结果
     */
    public static class ResponseResult<T> {
        private final T response;
        private final ResponseType responseType;
        private final long latencyMs;
        private final String message;

        public ResponseResult(T response, ResponseType responseType, 
                            long latencyMs, String message) {
            this.response = response;
            this.responseType = responseType;
            this.latencyMs = latencyMs;
            this.message = message;
        }

        public T getResponse() { return response; }
        public ResponseType getResponseType() { return responseType; }
        public long getLatencyMs() { return latencyMs; }
        public String getMessage() { return message; }
    }

    /**
     * 缓存
     */
    public static class ResponseCache {
        private final Map<String, String> cache;
        private final long maxAgeMs;

        public ResponseCache(long maxAgeMs) {
            this.cache = new HashMap<>();
            this.maxAgeMs = maxAgeMs;
        }

        /**
         * 获取缓存响应
         */
        public synchronized String get(String key) {
            // 简化实现：实际应该考虑缓存过期
            return cache.get(key);
        }

        /**
         * 添加缓存响应
         */
        public synchronized void put(String key, String value) {
            cache.put(key, value);
        }

        /**
         * 清除缓存
         */
        public synchronized void clear() {
            cache.clear();
        }
    }

    /**
     * 带降级的聊天
     */
    public static <T> ResponseResult<T> chatWithDegradation(
            ChatModel model,
            String message,
            ResponseCache cache,
            long cacheKey,
            long maxLatencyMs,
            String fallbackMessage
    ) throws Exception {
        long startTime = System.currentTimeMillis();

        try {
            // 尝试从缓存获取
            String cachedResponse = cache.get(String.valueOf(cacheKey));
            if (cachedResponse != null) {
                long latency = System.currentTimeMillis() - startTime;
                logger.info("使用缓存响应，延迟: {}ms", latency);
                return new ResponseResult<>(
                    (T) cachedResponse,
                    ResponseType.CACHED,
                    latency,
                    "从缓存返回"
                );
            }

            // 执行请求
            String response = model.chat(message);
            long latency = System.currentTimeMillis() - startTime;

            // 检查是否超过最大延迟
            if (latency > maxLatencyMs) {
                logger.warn("响应时间 {}ms 超过阈值 {}ms", latency, maxLatencyMs);
                
                // 返回简化响应
                return new ResponseResult<>(
                    (T) response.substring(0, Math.min(100, response.length())),
                    ResponseType.SIMPLIFIED,
                    latency,
                    "响应超时，返回简化版本"
                );
            }

            // 缓存响应
            cache.put(String.valueOf(cacheKey), response);

            return new ResponseResult<>(
                (T) response,
                ResponseType.FULL,
                latency,
                "完整响应"
            );

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            
            logger.error("请求失败: {}", e.getMessage());

            // 返回降级响应
            return new ResponseResult<>(
                (T) fallbackMessage,
                ResponseType.FALLBACK,
                latency,
                "请求失败，使用降级响应"
            );
        }
    }

    /**
     * 使用示例
     */
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
                .timeout(Duration.ofSeconds(10))
                .build();

        // 创建缓存
        ResponseCache cache = new ResponseCache(30000);  // 30 秒缓存

        // 多次请求
        for (int i = 0; i < 5; i++) {
            try {
                ResponseResult<String> result = chatWithDegradation(
                    model,
                    "你好，第 " + (i + 1) + " 次问候",
                    cache,
                    i,
                    5000,  // 5 秒阈值
                    "抱歉，我现在无法处理您的请求，请稍后再试。"
                );

                System.out.println("╔═════════════════════════════════════════════════════════╗");
                System.out.println("║ 请求 " + (i + 1) + " 响应                                  ║");
                System.out.println("╠═════════════════════════════════════════════════════════╣");
                System.out.println("║ 类型: " + result.getResponseType());
                System.out.println("║ 延迟: " + result.getLatencyMs() + "ms");
                System.out.println("║ 消息: " + result.getMessage());
                System.out.println("║ 响应: " + 
                    result.getResponse().substring(0, Math.min(80, result.getResponse().length())));
                System.out.println("╠───────────────────────────────────────────────────────────╣");
                System.out.println("║ 完整响应:                                              ║");
                System.out.println("║ " + result.getResponse());
                System.out.println("╚═════════════════════════════════════════════════════════╝");
                System.out.println();

            } catch (Exception e) {
                System.err.println("错误: " + e.getMessage());
            }
        }
    }
}
```

## 错误监控和告警

### 错误监控

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 错误监控和告警
 */
public class ErrorMonitoring {

    private static final Logger logger = LoggerFactory.getLogger(ErrorMonitoring.class);

    /**
     * 错误统计
     */
    public static class ErrorStatistics {
        private final String errorType;
        private final String errorType;
        private final AtomicLong count;
        private final AtomicLong lastOccurrence;
        private final Map<String, Long> errorDetails;

        public ErrorStatistics(String errorType, String errorType) {
            this.errorType = errorType;
            this.errorType = errorType;
            this.count = new AtomicLong(0);
            this.lastOccurrence = new AtomicLong(0);
            this.errorDetails = new ConcurrentHashMap<>();
        }

        /**
         * 记录错误
         */
        public void recordError(String details) {
            count.incrementAndGet();
            lastOccurrence.set(System.currentTimeMillis());
            errorDetails.merge(details, 1L, Long::sum);
        }

        /**
         * 获取统计
         */
        public long getCount() { return count.get(); }
        public long getLastOccurrence() { return lastOccurrence.get(); }
        public Map<String, Long> getErrorDetails() { return new ConcurrentHashMap<>(errorDetails); }
    }

    /**
     * 告警配置
     */
    public static class AlertConfig {
        private final long errorThreshold;
        private final long timeWindowMs;
        private final boolean enableEmailAlert;
        private final boolean enableSlackAlert;

        public AlertConfig(
                long errorThreshold,
                long timeWindowMs,
                boolean enableEmailAlert,
                boolean enableSlackAlert
        ) {
            this.errorThreshold = errorThreshold;
            this.timeWindowMs = timeWindowMs;
            this.enableEmailAlert = enableEmailAlert;
            this.enableSlackAlert = enableSlackAlert;
        }

        // Getters
        public long getErrorThreshold() { return errorThreshold; }
        public long getTimeWindowMs() { return timeWindowMs; }
        public boolean isEnableEmailAlert() { return enableEmailAlert; }
        public boolean isEnableSlackAlert() { return enableSlackAlert; }
    }

    /**
     * 告警管理器
     */
    public static class AlertManager {
        private final AlertConfig config;
        private final Map<String, ErrorStatistics> errorStats;

        public AlertManager(AlertConfig config) {
            this.config = config;
            this.errorStats = new ConcurrentHashMap<>();
        }

        /**
         * 记录错误
         */
        public void recordError(String errorType, String errorType, String details) {
            ErrorStatistics stats = errorStats.computeIfAbsent(errorType, 
                    k -> new ErrorStatistics(errorType, errorType));

            stats.recordError(details);

            // 检查是否需要告警
            checkAlert(errorType, stats);
        }

        /**
         * 检查告警
         */
        private void checkAlert(String errorType, ErrorStatistics stats) {
            long timeSinceLastOccurrence = System.currentTimeMillis() - stats.getLastOccurrence();

            // 检查错误数量阈值
            if (stats.getCount() >= config.getErrorThreshold()) {
                logger.error("错误告警: 错误类型 '{}' 在过去 {}ms 内发生了 {} 次",
                            errorType, config.getTimeWindowMs(), stats.getCount());

                // 发送告警
                sendAlert(errorType, stats.getCount(), stats.getErrorDetails());
            }
        }

        /**
         * 发送告警
         */
        private void sendAlert(String errorType, long count, Map<String, Long> details) {
            String message = String.format(
                "错误告警\n" +
                "错误类型: %s\n" +
                "发生次数: %d\n" +
                "时间: %s\n" +
                "详细信息: %s",
                errorType,
                count,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                details
            );

            logger.error("告警消息: {}", message);

            // 发送邮件
            if (config.isEnableEmailAlert()) {
                sendEmailAlert(message);
            }

            // 发送 Slack
            if (config.isEnableSlackAlert()) {
                sendSlackAlert(message);
            }
        }

        /**
         * 发送邮件告警
         */
        private void sendEmailAlert(String message) {
            // 简化：实际应该使用 JavaMail 或其他邮件库
            logger.info("发送邮件告警: {}", message);
        }

        /**
         * 发送 Slack 告警
         */
        private void sendSlackAlert(String message) {
            // 简化：实际应该使用 Slack Web API
            logger.info("发送 Slack 告警: {}", message);
        }

        /**
         * 获取错误统计
         */
        public Map<String, ErrorStatistics> getErrorStatistics() {
            return new ConcurrentHashMap<>(errorStats);
        }

        /**
         * 生成报告
         */
        public String generateReport() {
            StringBuilder report = new StringBuilder();
            report.append("╔═════════════════════════════════════════════════════════╗\n");
            report.append("║              错误监控报告                          ║\n");
            report.append("╠═════════════════════════════════════════════════════════╣\n");
            report.append("║ 错误类型总数: ").append(errorStats.size()).append("\n");

            errorStats.forEach((errorType, stats) -> {
                report.append("╠─────────────────────────────────────────────────────────╣\n");
                report.append("║ 错误类型: ").append(errorType).append("\n");
                report.append("║ 发生次数: ").append(stats.getCount()).append("\n");
                report.append("║ 最后发生: ").append(
                    LocalDateTime.ofEpochMilli(stats.getLastOccurrence())
                            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                ).append("\n");
                report.append("║ 详细统计:                                            ║\n");
                
                stats.getErrorDetails().forEach((detail, count) -> {
                    report.append("║   - ").append(detail).append(": ")
                          .append(count).append(" 次\n");
                });
            });

            report.append("╠═════════════════════════════════════════════════════════╣\n");
            report.append("║ 时间: ").append(
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ).append("\n");
            report.append("╚═════════════════════════════════════════════════════════╝\n");

            return report.toString();
        }
    }

    /**
     * 使用示例
     */
    public static void main(String[] args) {
        // 创建告警配置
        AlertConfig config = new AlertConfig(
                10,          // 10 次错误
                60000,       // 1 分钟内
                true,         // 启用邮件告警
                true          // 启用 Slack 告警
        );

        // 创建告警管理器
        AlertManager alertManager = new AlertManager(config);

        // 模拟错误
        for (int i = 0; i < 15; i++) {
            alertManager.recordError("API错误", "超时", "请求超时 " + i);
            Thread.sleep(1000);  // 1 秒间隔
        }

        // 生成报告
        System.out.println(alertManager.generateReport());
    }
}
```

## 测试代码示例

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import static org.mockito.Mockito.*;

/**
 * 错误处理测试
 */
class ErrorHandlingTest {

    @Test
    void should_retry_on_timeout() {
        // 创建 Mock 模型，模拟超时
        ChatModel mockModel = mock(ChatModel.class);
        
        int[] attempts = {0};
        when(mockModel.chat(anyString()))
                .thenAnswer(invocation -> {
                    attempts[0]++;
                    if (attempts[0] < 3) {
                        throw new java.net.SocketTimeoutException("Timeout");
                    }
                    return "Success";
                });

        // 执行重试
        String result = ExponentialBackoffRetry.executeWithRetry(
            "测试",
            () -> mockModel.chat("test"),
            ExponentialBackoffRetry.createDefaultRetryConfig()
        );

        // 验证
        assertEquals(3, attempts[0]);
        assertEquals("Success", result);
    }

    @Test
    void should_not_retry_on_validation_error() {
        // 创建 Mock 模型，模拟验证错误
        ChatModel mockModel = mock(ChatModel.class);
        
        when(mockModel.chat(anyString()))
                .thenThrow(new IllegalArgumentException("Invalid input"));

        // 执行重试
        Exception exception = assertThrows(RuntimeException.class, () -> {
            ExponentialBackoffRetry.executeWithRetry(
                "测试",
                () -> mockModel.chat("test"),
                ExponentialBackoffRetry.createDefaultRetryConfig()
            );
        });

        // 验证没有重试
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void should_circuit_break_after_failures() {
        CircuitBreaker circuitBreaker = new CircuitBreaker(
            new CircuitBreakerConfig(
                3,
                Duration.ofSeconds(10),
                Duration.ofSeconds(30),
                2
            )
        );

        CircuitBreaker.RetryOperation<String> operation = mock(CircuitBreaker.RetryOperation.class);
        when(operation.execute())
                .thenThrow(new RuntimeException("Failed"));

        // 执行多次请求
        for (int i = 0; i < 5; i++) {
            try {
                executeWithCircuitBreaker("测试", operation, circuitBreaker);
                fail("应该抛出异常");
            } catch (RuntimeException e) {
                // 预期失败
            }
        }

        // 验证断路器状态
        assertEquals(CircuitBreaker.CircuitState.OPEN, circuitBreaker.getState());
    }

    @Test
    void should_reset_after_success() {
        CircuitBreaker circuitBreaker = new CircuitBreaker(
            new CircuitBreakerConfig(
                3,
                Duration.ofSeconds(10),
                Duration.ofSeconds(30),
                2
            )
        );

        // 先触发断路器
        CircuitBreaker.RetryOperation<String> failOperation = mock(CircuitBreaker.RetryOperation.class);
        when(failOperation.execute()).thenThrow(new RuntimeException("Failed"));

        for (int i = 0; i < 3; i++) {
            try {
                executeWithCircuitBreaker("失败", failOperation, circuitBreaker);
            } catch (Exception e) {
                // 预期失败
            }
        }

        assertEquals(CircuitBreaker.CircuitState.OPEN, circuitBreaker.getState());

        // 重置
        circuitBreaker.reset();
        assertEquals(CircuitBreaker.CircuitState.CLOSED, circuitBreaker.getState());

        // 成功请求
        CircuitBreaker.RetryOperation<String> successOperation = mock(CircuitBreaker.RetryOperation.class);
        when(successOperation.execute()).thenReturn("Success");

        String result = executeWithCircuitBreaker("成功", successOperation, circuitBreaker);
        assertEquals("Success", result);
        assertEquals(CircuitBreaker.CircuitState.CLOSED, circuitBreaker.getState());
    }
}
```

## 实践练习

### 练习 1：实现完整的错误处理系统

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 完整的错误处理系统
 */
public class ErrorHandlingSystem {

    private static final Logger logger = LoggerFactory.getLogger(ErrorHandlingSystem.class);

    /**
     * 错误类型枚举
     */
    public enum ErrorType {
        NETWORK_ERROR("网络错误"),
        API_ERROR("API 错误"),
        VALIDATION_ERROR("验证错误"),
        TIMEOUT_ERROR("超时错误"),
        UNKNOWN_ERROR("未知错误");

        private final String description;

        ErrorType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 错误处理器接口
     */
    public interface ErrorHandler {
        boolean canHandle(Exception e);
        ErrorResponse handle(Exception e);
    }

    /**
     * 错误响应
     */
    public static class ErrorResponse {
        private final String code;
        private final String message;
        private final String type;
        private final Map<String, Object> details;
        private final long timestamp;

        public ErrorResponse(String code, String message, ErrorType type, Map<String, Object> details) {
            this.code = code;
            this.message = message;
            this.type = type.name();
            this.details = details;
            this.timestamp = System.currentTimeMillis();
        }

        public String getCode() { return code; }
        public String getMessage() { return message; }
        public String getType() { return type; }
        public Map<String, Object> getDetails() { return details; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * 网络错误处理器
     */
    public static class NetworkErrorHandler implements ErrorHandler {
        @Override
        public boolean canHandle(Exception e) {
            return e instanceof java.net.SocketTimeoutException ||
                   e instanceof java.net.ConnectException ||
                   e instanceof java.io.IOException;
        }

        @Override
        public ErrorResponse handle(Exception e) {
            Map<String, Object> details = new ConcurrentHashMap<>();
            details.put("original_message", e.getMessage());

            if (e instanceof java.net.SocketTimeoutException) {
                return new ErrorResponse(
                    "NETWORK_TIMEOUT",
                    "请求超时",
                    ErrorType.TIMEOUT_ERROR,
                    details
                );
            } else if (e instanceof java.net.ConnectException) {
                return new ErrorResponse(
                    "CONNECTION_FAILED",
                    "无法连接到服务器",
                    ErrorType.NETWORK_ERROR,
                    details
                );
            } else {
                return new ErrorResponse(
                    "NETWORK_ERROR",
                    "网络通信错误",
                    ErrorType.NETWORK_ERROR,
                    details
                );
            }
        }
    }

    /**
     * API 错误处理器
     */
    public static class ApiErrorHandler implements ErrorHandler {
        @Override
        public boolean canHandle(Exception e) {
            String message = e.getMessage();
            return message != null && (
                message.contains("401") ||
                message.contains("429") ||
                message.contains("500") ||
                message.contains("503")
            );
        }

        @Override
        public ErrorResponse handle(Exception e) {
            String message = e.getMessage();

            if (message.contains("401")) {
                return new ErrorResponse(
                        "AUTHENTICATION_FAILED",
                        "认证失败",
                        ErrorType.API_ERROR,
                        Map.of("status_code", 401)
                );
            } else if (message.contains("429")) {
                return new ErrorResponse(
                        "RATE_LIMIT_EXCEEDED",
                        "请求频率过高",
                        ErrorType.API_ERROR,
                        Map.of("status_code", 429)
                );
            } else if (message.contains("503")) {
                return new ErrorResponse(
                        "SERVICE_UNAVAILABLE",
                        "服务暂时不可用",
                        ErrorType.API_ERROR,
                        Map.of("status_code", 503)
                );
            } else if (message.contains("500")) {
                return new ErrorResponse(
                        "INTERNAL_SERVER_ERROR",
                        "服务器内部错误",
                        ErrorType.API_ERROR,
                        Map.of("status_code", 500)
                );
            }

            return new ErrorResponse(
                "UNKNOWN_API_ERROR",
                "未知的 API 错误",
                ErrorType.API_ERROR,
                Map.of("message", message)
            );
        }
    }

    /**
     * 验证错误处理器
     */
    public static class ValidationErrorHandler implements ErrorHandler {
        @Override
        public boolean canHandle(Exception e) {
            return e instanceof IllegalArgumentException ||
                   e instanceof NullPointerException;
        }

        @Override
        public ErrorResponse handle(Exception e) {
            return new ErrorResponse(
                    "VALIDATION_ERROR",
                    e.getMessage(),
                    ErrorType.VALIDATION_ERROR,
                    Map.of("exception_type", e.getClass().getSimpleName())
            );
        }
    }

    /**
     * 默认错误处理器
     */
    public static class DefaultErrorHandler implements ErrorHandler {
        @Override
        public boolean canHandle(Exception e) {
            return true;
        }

        @Override
        public ErrorResponse handle(Exception e) {
            Map<String, Object> details = new ConcurrentHashMap<>();
            details.put("exception_type", e.getClass().getName());
            details.put("message", e.getMessage());
            details.put("cause", e.getCause() != null ? e.getCause().getMessage() : null);

            return new ErrorResponse(
                    "INTERNAL_ERROR",
                    "内部错误",
                    ErrorType.UNKNOWN_ERROR,
                    details
            );
        }
    }

    /**
     * 错误处理管理器
     */
    public static class ErrorHandlingManager {
        private final Map<ErrorType, ErrorHandler> handlers;

        public ErrorHandlingManager() {
            this.handlers = new ConcurrentHashMap<>();

            // 注册处理器
            registerHandler(ErrorType.NETWORK_ERROR, new NetworkErrorHandler());
            registerHandler(ErrorType.API_ERROR, new ApiErrorHandler());
            registerHandler(ErrorType.VALIDATION_ERROR, new ValidationErrorHandler());
            registerHandler(ErrorType.TIMEOUT_ERROR, new NetworkErrorHandler());  // 复用
            registerHandler(ErrorType.UNKNOWN_ERROR, new DefaultErrorHandler());
        }

        /**
         * 注册错误处理器
         */
        public void registerHandler(ErrorType type, ErrorHandler handler) {
            handlers.put(type, handler);
            logger.info("注册错误处理器: {} -> {}", type, handler.getClass().getSimpleName());
        }

        /**
         * 处理错误
         */
        public ErrorResponse handleError(Exception e) {
            for (ErrorHandler handler : handlers.values()) {
                if (handler.canHandle(e)) {
                    logger.error("使用处理器处理错误: {}", handler.getClass().getSimpleName());
                    return handler.handle(e);
                }
            }

            logger.warn("未找到合适的错误处理器，使用默认处理器");
            return new DefaultErrorHandler().handle(e);
        }
    }

    /**
     * 使用示例
     */
    public static void main(String[] args) {
        String apiKey = System.getenv("OPENAI_API_KEY");

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("未设置 OPENAI_API_KEY 环境变量");
            return;
        }

        // 创建错误处理管理器
        ErrorHandlingManager manager = new ErrorHandlingManager();

        // 测试各种错误类型
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║              错误处理系统示例                              ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println();

        // 测试 1：网络错误
        System.out.println("=== 测试 1: 网络错误 ===");
        Exception networkError = new java.net.SocketTimeoutException("Connection timeout");
        ErrorResponse networkResponse = manager.handleError(networkError);
        displayErrorResponse(networkResponse);
        System.out.println();

        // 测试 2：验证错误
        System.out.println("=== 测试 2: 验证错误 ===");
        Exception validationError = new IllegalArgumentException("参数不能为空");
        ErrorResponse validationResponse = manager.handleError(validationError);
        displayErrorResponse(validationResponse);
        System.out.println();

        // 测试 3：API 错误
        System.out.println("=== 测试 3: API 错误 ===");
        Exception apiError = new RuntimeException("API returned 429 Too Many Requests");
        ErrorResponse apiErrorResponse = manager.handleError(apiError);
        displayErrorResponse(apiErrorResponse);
        System.out.println();

        // 测试 4：未知错误
        System.out.println("=== 测试 4: 未知错误 ===");
        Exception unknownError = new RuntimeException("Something went wrong");
        ErrorResponse unknownErrorResponse = manager.handleError(unknownError);
        displayErrorResponse(unknownErrorResponse);
    }

    /**
     * 显示错误响应
     */
    private static void displayErrorResponse(ErrorResponse response) {
        System.out.printf("║ 代码: %s                                               ║\n", response.getCode());
        System.out.printf("║ 类型: %s                                                   ║\n", response.getType());
        System.out.printf("║ 消息: %s                                             ║\n", response.getMessage());
        System.out.printf("║ 时间: %s                                             ║\n",
                java.time.LocalDateTime.ofEpochMilli(response.getTimestamp())
                        .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        System.out.printf("║ 详情:                                                    ║\n");
        response.getDetails().forEach((key, value) -> {
            System.out.printf("║   %s: %s                                         ║\n", key, value);
        });
        System.out.println("╠─────────────────────────────────────────────────────────────╣");
    }
}
```

## 总结

### 本章要点

1. **错误处理重要性**
   - 提高应用可靠性
   - 改善用户体验
   - 便于问题排查

2. **错误类型**
   - 网络错误
   - API 错误
   - 验证错误
   - 超时错误

3. **重试策略**
   - 指数退避
   - 线性退避
   - 固定延迟
   - 随机抖动

4. **断路器模式**
   - 保护服务免受过载
   - 快速失败
   - 自动恢复
   - 半开状态测试

5. **最佳实践**
   - 合理配置重试次数和延迟
   - 正确设置断路器阈值
   - 记录错误和发送告警
   - 实现优雅降级

### 下一步

在下一章节中，我们将学习：
- Spring Boot 集成
- Web 应用开发
- REST API 创建
- WebSocket 实时通信
- 安全和认证

### 常见问题

**Q1：如何确定重试次数？**

A：考虑因素：
1. 错误类型（网络错误需要更多重试）
2. 业务要求（重要操作可以重试更多次）
3. 成本控制（每次重试都消耗 Token）
4. 用户容忍度（重试过多次会影响体验）

**Q2：断路器何时打开和关闭？**

A：触发条件：
- 打开：失败次数达到阈值
- 半开：重置时间过后
- 关闭：半开状态下多次成功后

**Q3：如何实现优雅降级？**

A：降级策略：
1. 使用缓存结果
2. 返回简化响应
3. 返回默认响应
4. 使用备用服务

**Q4：如何监控错误？**

A：监控方法：
1. 记录所有错误
2. 统计错误频率
3. 设置错误告警
4. 分析错误趋势
5. 生成错误报告

**Q5：如何处理未知错误？**

A：处理策略：
1. 记录详细信息
2. 返回通用错误消息
3. 通知开发团队
4. 收集错误上下文
5. 提供技术支持信息

## 参考资料

- [LangChain4j 错误处理文档](https://docs.langchain4j.dev/tutorials/error-handling-and-retry)
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
