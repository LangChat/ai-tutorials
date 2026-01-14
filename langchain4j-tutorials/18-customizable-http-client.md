---
title: '可自定义 HTTP 客户端'
description: '学习 LangChain4j 的 可自定义 HTTP 客户端 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 18 - 自定义 HTTP 客户端

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 理解 LangChain4j 的 HTTP 客户端机制
- 掌握自定义 HTTP 客户端的配置方法
- 学会实现自定义请求拦截器和响应处理器
- 理解 HTTP 客户端的高级特性（超时、代理、重试等）
- 掌握不同模型提供商的 HTTP 客户端配置
- 实现一个完整的自定义 HTTP 客户端解决方案

## 前置知识

- 完成《01 - LangChain4j 简介》章节
- HTTP 协议基础知识
- Java HTTP 客户端经验
## 核心概念

### 什么是自定义 HTTP 客户端？

**自定义 HTTP 客户端**是 LangChain4j 提供的高级功能，允许开发者：

1. **自定义请求行为**
   - 修改请求头
   - 添加认证信息
   - 实现请求签名

2. **自定义响应处理**
   - 记录请求和响应
   - 处理错误和异常
   - 解析自定义响应格式

3. **网络配置**
   - 配置超时时间
   - 设置代理服务器
   - 实现 SSL/TLS 配置

4. **高级功能**
   - 自动重试机制
   - 连接池配置
   - 请求限流

### 为什么需要自定义 HTTP 客户端？

**场景 1：企业内网部署**
- 需要通过公司代理访问 OpenAI API
- 需要使用自定义的证书
- 需要记录所有 API 调用

**场景 2：安全合规要求**
- 需要记录所有 API 请求和响应（用于审计）
- 需要对敏感数据进行加密
- 需要实现请求签名（防重放攻击）

**场景 3：性能优化**
- 需要配置合适的超时时间
- 需要使用连接池减少连接开销
- 需要实现智能重试策略

## 自定义 HTTP 客户端基础

### 基础配置

```java
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.http.client.ClientProvider;
import dev.langchain4j.http.client.HttpClient;

import java.time.Duration;
import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * 自定义 HTTP 客户端示例
 */
public class CustomHttpClientExample {

    /**
     * 创建带有自定义 HTTP 客户端的模型
     */
    public static OpenAiChatModel createModelWithCustomHttpClient(
            String apiKey,
            String baseUrl,
            Duration timeout,
            Proxy proxy
    ) {
        OpenAiChatModel.Builder builder = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini");

        // 设置自定义基础 URL
        if (baseUrl != null && !baseUrl.isEmpty()) {
            builder.baseUrl(baseUrl);
        }

        // 设置超时
        if (timeout != null) {
            builder.timeout(timeout);
        }

        // 设置代理
        if (proxy != null) {
            builder.httpClient(createHttpClientWithProxy(proxy));
        }

        return builder.build();
    }

    /**
     * 创建带有代理的 HTTP 客户端
     */
    private static HttpClient createHttpClientWithProxy(Proxy proxy) {
        // 这里简化实现，实际中应该使用具体的 HTTP 客户端库
        // LangChain4j 支持多种 HTTP 客户端实现
        
        System.out.println("创建带有代理的 HTTP 客户端");
        System.out.println("代理类型: " + proxy.type());
        System.out.println("代理地址: " + proxy.address());
        
        // 返回 null 表示使用默认配置
        return null;
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("OPENAI_API_KEY");

        // 创建带有自定义配置的模型
        OpenAiChatModel model = createModelWithCustomHttpClient(
            apiKey,
            "https://api.openai.com/v1",  // 自定义基础 URL
            Duration.ofSeconds(30),            // 30 秒超时
            new Proxy(
                Proxy.Type.HTTP,
                new InetSocketAddress("proxy.example.com", 8080)
            )
        );

        System.out.println("=== 测试模型 ===");
        System.out.println();

        // 测试模型
        try {
            String response = model.chat("你好，请介绍一下自己");
            System.out.println("响应: " + response);
        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
        }
    }
}
```

## 高级配置

### 超时和重试

```java
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 超时和重试配置
 */
public class TimeoutAndRetryConfig {

    /**
     * 创建带有超时和重试的模型
     */
    public static OpenAiChatModel createModelWithTimeoutAndRetry(
            String apiKey,
            Duration connectTimeout,
            Duration readTimeout,
            int maxRetries,
            Duration retryDelay
    ) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .timeout(readTimeout)
                .maxRetries(maxRetries)
                .build();
    }

    /**
     * 创建带有指数退避重试的模型
     */
    public static OpenAiChatModel createModelWithExponentialBackoff(
            String apiKey,
            int maxRetries,
            Duration initialDelay,
            double backoffFactor
    ) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .maxRetries(maxRetries)
                .build();
    }

    /**
     * 创建带有固定延迟重试的模型
     */
    public static OpenAiChatModel createModelWithFixedDelayRetry(
            String apiKey,
            int maxRetries,
            Duration retryDelay
    ) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .maxRetries(maxRetries)
                .build();
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("OPENAI_API_KEY");

        // 创建带有超时的模型
        System.out.println("=== 超时配置 ===");
        System.out.println();

        OpenAiChatModel model1 = createModelWithTimeoutAndRetry(
            apiKey,
            Duration.ofSeconds(10),  // 连接超时 10 秒
            Duration.ofSeconds(30),  // 读取超时 30 秒
            3,                       // 最大重试次数
            Duration.ofMillis(1000) // 重试延迟 1 秒
        );

        // 测试超时
        try {
            String response = model1.chat("你好");
            System.out.println("响应: " + response);
        } catch (Exception e) {
            System.err.println("超时错误: " + e.getMessage());
        }

        System.out.println();

        // 创建带有重试的模型
        System.out.println("=== 重试配置 ===");
        System.out.println();

        OpenAiChatModel model2 = createModelWithFixedDelayRetry(
            apiKey,
            3,                      // 最多重试 3 次
            Duration.ofSeconds(2) // 重试间隔 2 秒
        );

        // 测试重试
        try {
            String response = model2.chat("你好");
            System.out.println("响应: " + response);
        } catch (Exception e) {
            System.err.println("重试错误: " + e.getMessage());
        }
    }
}
```

### 请求和响应日志

```java
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 请求和响应日志
 */
public class RequestResponseLogging {

    private static final Logger logger = LoggerFactory.getLogger(RequestResponseLogging.class);

    /**
     * 创建带有日志的模型
     */
    public static OpenAiChatModel createModelWithLogging(
            String apiKey,
            boolean logRequests,
            boolean logResponses,
            boolean logStreamingResponses
    ) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .logRequests(logRequests)
                .logResponses(logResponses)
                .logStreamingResponses(logStreamingResponses)
                .build();
    }

    /**
     * 自定义请求日志
     */
    public static void logRequest(String endpoint, String method, Map<String, String> headers, String body) {
        logger.info("HTTP 请求 - {} {}", method, endpoint);
        logger.info("请求头: {}", headers);
        logger.info("请求体: {}", body);
        logger.info("时间戳: {}", System.currentTimeMillis());
    }

    /**
     * 自定义响应日志
     */
    public static void logResponse(
            String endpoint,
            int statusCode,
            Map<String, String> headers,
            String body,
            long durationMs
    ) {
        logger.info("HTTP 响应 - {} {} ({}ms)", endpoint, statusCode, durationMs);
        logger.info("响应头: {}", headers);
        logger.info("响应体: {}", body);
        logger.info("时间戳: {}", System.currentTimeMillis());

        // 性能警告
        if (durationMs > 5000) {
            logger.warn("慢响应检测 - {} 响应时间: {}ms (阈值: 5000ms)", endpoint, durationMs);
        }
    }

    /**
     * 自定义错误日志
     */
    public static void logError(
            String endpoint,
            String method,
            Exception error,
            long durationMs
    ) {
        logger.error("HTTP 错误 - {} {}", method, endpoint);
        logger.error("异常: {}", error.getMessage());
        logger.error("耗时: {}ms", durationMs);
        logger.error("时间戳: {}", System.currentTimeMillis());
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("OPENAI_API_KEY");

        // 创建带有日志的模型
        System.out.println("=== 请求和响应日志 ===");
        System.out.println();

        OpenAiChatModel model = createModelWithLogging(
            apiKey,
            true,   // 记录请求
            true,   // 记录响应
            true    // 记录流式响应
        );

        // 测试日志
        long startTime = System.currentTimeMillis();
        try {
            String response = model.chat("你好，请介绍一下自己");
            long duration = System.currentTimeMillis() - startTime;

            System.out.println("响应: " + response);
            System.out.println("耗时: " + duration + "ms");
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            logError("chat/completions", "POST", e, duration);
        }
    }
}
```

## 认证和安全

### API Key 管理

```java
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API Key 管理
 */
public class ApiKeyManager {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyManager.class);

    // API Key 存储和轮换
    private final Map<String, String> apiKeys;
    private final String defaultApiKey;

    public ApiKeyManager(String defaultApiKey) {
        this.apiKeys = new ConcurrentHashMap<>();
        this.defaultApiKey = defaultApiKey;
        this.apiKeys.put("default", defaultApiKey);
    }

    /**
     * 获取 API Key（支持轮换）
     */
    public String getApiKey() {
        return defaultApiKey;
    }

    /**
     * 获取指定的 API Key
     */
    public String getApiKey(String keyName) {
        return apiKeys.get(keyName);
    }

    /**
     * 添加 API Key
     */
    public void addApiKey(String keyName, String apiKey) {
        apiKeys.put(keyName, apiKey);
        logger.info("添加 API Key: {}", keyName);
    }

    /**
     * 移除 API Key
     */
    public void removeApiKey(String keyName) {
        apiKeys.remove(keyName);
        logger.info("移除 API Key: {}", keyName);
    }

    /**
     * 创建使用指定 API Key 的模型
     */
    public OpenAiChatModel createModelWithApiKey(String keyName) {
        String apiKey = getApiKey(keyName);
        
        if (apiKey == null) {
            throw new IllegalArgumentException("API Key '" + keyName + "' 不存在");
        }

        logger.info("创建模型，使用 API Key: {}", keyName);

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .build();
    }

    /**
     * 创建带有 API Key 轮换的模型
     */
    public OpenAiChatModel createModelWithKeyRotation(
            String[] keyNames,
            String rotationStrategy  // "round-robin", "random", "least-used"
    ) {
        String currentKey = selectApiKey(keyNames, rotationStrategy);
        
        return OpenAiChatModel.builder()
                .apiKey(currentKey)
                .modelName("gpt-4o-mini")
                .build();
    }

    /**
     * 选择 API Key
     */
    private String selectApiKey(String[] keyNames, String strategy) {
        if (keyNames == null || keyNames.length == 0) {
            return defaultApiKey;
        }

        return switch (strategy) {
            case "round-robin" -> {
                int index = (int) (System.currentTimeMillis() / 1000) % keyNames.length;
                return keyNames[index];
            }
            case "random" -> {
                int index = (int) (Math.random() * keyNames.length);
                return keyNames[index];
            }
            case "least-used" -> {
                // 简化：实际应用中应该跟踪使用计数
                int index = (int) (Math.random() * keyNames.length);
                return keyNames[index];
            }
            default -> keyNames[0];
        };
    }

    /**
     * 验证 API Key
     */
    public boolean validateApiKey(String apiKey) {
        try {
            OpenAiChatModel model = OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName("gpt-4o-mini")
                    .timeout(Duration.ofSeconds(10))
                    .build();

            // 测试请求
            model.chat("test");

            logger.info("API Key 验证成功");
            return true;
        } catch (Exception e) {
            logger.warn("API Key 验证失败: {}", e.getMessage());
            return false;
        }
    }

    public static void main(String[] args) {
        ApiKeyManager manager = new ApiKeyManager(
            System.getenv("OPENAI_API_KEY")
        );

        // 添加多个 API Key
        manager.addApiKey("key1", "sk-xxxxx");
        manager.addApiKey("key2", "sk-yyyyy");
        manager.addApiKey("key3", "sk-zzzzz");

        // 使用指定 API Key 创建模型
        System.out.println("=== 使用指定 API Key ===");
        System.out.println();

        OpenAiChatModel model1 = manager.createModelWithApiKey("key1");

        // 使用 API Key 轮换
        System.out.println("=== API Key 轮换 ===");
        System.out.println();

        String[] keyNames = {"key1", "key2", "key3"};
        OpenAiChatModel model2 = manager.createModelWithKeyRotation(
            keyNames,
            "round-robin"
        );

        // 验证 API Key
        System.out.println("=== API Key 验证 ===");
        System.out.println();

        boolean isValid = manager.validateApiKey(
            System.getenv("OPENAI_API_KEY")
        );

        System.out.println("API Key 是否有效: " + isValid);
    }
}
```

## 自定义拦截器

### 请求拦截器

```java
import dev.langchain4j.http.client.sse.ServerSentEventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 请求拦截器
 */
public class RequestInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RequestInterceptor.class);

    /**
     * 请求前拦截
     */
    public void beforeRequest(
            String url,
            String method,
            Map<String, String> headers,
            String body
    ) {
        logger.info("========== 请求开始 ==========");
        logger.info("URL: {}", url);
        logger.info("方法: {}", method);
        logger.info("请求头: {}", headers);
        logger.info("请求体: {}", body);

        // 自定义处理
        addCustomHeaders(headers);
        logRequestMetrics(body);

        logger.info("============================");
    }

    /**
     * 添加自定义请求头
     */
    private void addCustomHeaders(Map<String, String> headers) {
        headers.put("X-Request-ID", generateRequestId());
        headers.put("X-Application-Name", "LangChain4j-App");
        headers.put("X-Timestamp", String.valueOf(System.currentTimeMillis()));

        logger.debug("已添加自定义请求头");
    }

    /**
     * 记录请求指标
     */
    private void logRequestMetrics(String body) {
        if (body != null) {
            logger.info("请求体大小: {} bytes", body.getBytes().length);
            logger.info("请求体哈希: {}", Integer.toHexString(body.hashCode()));
        }
    }

    /**
     * 请求后拦截
     */
    public void afterRequest(
            String url,
            int statusCode,
            Map<String, String> headers,
            String body,
            long durationMs
    ) {
        logger.info("========== 请求完成 ==========");
        logger.info("URL: {}", url);
        logger.info("状态码: {}", statusCode);
        logger.info("响应头: {}", headers);
        logger.info("耗时: {}ms", durationMs);

        // 自定义处理
        logPerformanceMetrics(durationMs, body);
        checkRateLimit(statusCode, headers);

        logger.info("============================");
    }

    /**
     * 请求错误拦截
     */
    public void onRequestError(
            String url,
            Exception error,
            long durationMs
    ) {
        logger.error("========== 请求错误 ==========");
        logger.error("URL: {}", url);
        logger.error("异常: {}", error.getClass().getName());
        logger.error("消息: {}", error.getMessage());
        logger.error("耗时: {}ms", durationMs);

        // 自定义处理
        logErrorMetrics(error);
        handleError(error);

        logger.error("============================");
    }

    /**
     * 记录性能指标
     */
    private void logPerformanceMetrics(long durationMs, String body) {
        long bytesPerSecond = body != null && durationMs > 0 ? 
            body.getBytes().length * 1000 / durationMs : 0;

        logger.info("性能指标 - 吞吐量: {} bytes/s", bytesPerSecond);

        if (durationMs > 10000) {
            logger.warn("性能警告 - 请求耗时 {}ms (超过阈值 10000ms)", durationMs);
        }
    }

    /**
     * 检查速率限制
     */
    private void checkRateLimit(int statusCode, Map<String, String> headers) {
        if (statusCode == 429) {
            logger.warn("速率限制触发 - 状态码: {}", statusCode);
            String retryAfter = headers.get("Retry-After");
            if (retryAfter != null) {
                logger.warn("建议重试时间: {} 秒", retryAfter);
            }
        }
    }

    /**
     * 记录错误指标
     */
    private void logErrorMetrics(Exception error) {
        logger.error("错误类型: {}", error.getClass().getName());
        logger.error("错误消息: {}", error.getMessage());

        if (error.getCause() != null) {
            logger.error("根本原因: {}", error.getCause().getMessage());
        }

        StackTraceElement[] stackTrace = error.getStackTrace();
        if (stackTrace.length > 0) {
            logger.error("错误位置: {}:{}",
                stackTrace[0].getClassName(),
                stackTrace[0].getMethodName());
        }
    }

    /**
     * 处理错误
     */
    private void handleError(Exception error) {
        // 根据错误类型采取不同的处理策略
        if (error instanceof java.net.SocketTimeoutException) {
            logger.error("连接超时，建议增加超时时间");
        } else if (error instanceof java.net.ConnectException) {
            logger.error("连接失败，建议检查网络和代理设置");
        } else if (error instanceof java.io.IOException) {
            logger.error("IO 错误，建议检查网络连接");
        }

        // 可以添加自定义的错误处理逻辑
        // 例如：发送告警通知、记录到监控系统等
    }

    /**
     * 生成请求 ID
     */
    private String generateRequestId() {
        return String.format("%s-%d",
                System.currentTimeMillis(),
                (int) (Math.random() * 10000)
        );
    }

    public static void main(String[] args) {
        RequestInterceptor interceptor = new RequestInterceptor();

        // 测试请求前拦截
        interceptor.beforeRequest(
            "https://api.openai.com/v1/chat/completions",
            "POST",
            Map.of(
                "Content-Type", "application/json",
                "Authorization", "Bearer sk-xxxxx"
            ),
            "{\"model\":\"gpt-4o-mini\",\"messages\":[{\"role\":\"user\",\"content\":\"你好\"}]}"
        );

        System.out.println();

        // 测试请求后拦截
        interceptor.afterRequest(
            "https://api.openai.com/v1/chat/completions",
            200,
            Map.of(
                "Content-Type", "application/json",
                "x-request-id", "1234567890"
            ),
            "{\"id\":\"chatcmpl-123\",\"choices\":[{\"message\":{\"content\":\"你好\"}]}]}",
            1500
        );

        System.out.println();

        // 测试请求错误拦截
        interceptor.onRequestError(
            "https://api.openai.com/v1/chat/completions",
            new RuntimeException("模拟错误"),
            500
        );
    }
}
```

## 流式事件监听

### SSE 事件监听

```java
import dev.langchain4j.http.client.sse.ServerSentEventListener;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SSE 事件监听
 */
public class SseEventListenerExample {

    private static final Logger logger = LoggerFactory.getLogger(SseEventListenerExample.class);

    /**
     * 创建带有事件监听器的流式模型
     */
    public static OpenAiStreamingChatModel createModelWithEventListener(
            String apiKey,
            ServerSentEventListener listener
    ) {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .streamResponseListener(listener)
                .build();
    }

    /**
     * 自定义 SSE 事件监听器
     */
    public static class CustomSseEventListener implements ServerSentEventListener {

        private final StringBuilder fullResponse;
        private final long startTime;
        private int tokenCount;

        public CustomSseEventListener() {
            this.fullResponse = new StringBuilder();
            this.startTime = System.currentTimeMillis();
            this.tokenCount = 0;
        }

        @Override
        public void onEvent(String event, String data) {
            if (event == null) {
                return;  // 忽略注释行
            }

            logger.debug("SSE 事件: {} = {}", event, data);

            if ("data".equals(event)) {
                handleData(data);
            } else if ("done".equals(event)) {
                handleDone();
            } else if ("error".equals(event)) {
                handleError(data);
            }
        }

        @Override
        public void onOpen() {
            logger.info("========== SSE 连接打开 ==========");
            logger.info("时间戳: {}", System.currentTimeMillis());
            logger.info("============================");
        }

        @Override
        public void onClose() {
            long duration = System.currentTimeMillis() - startTime;

            logger.info("========== SSE 连接关闭 ==========");
            logger.info("完整响应长度: {} 字符", fullResponse.length());
            logger.info("Token 数量: {}", tokenCount);
            logger.info("总耗时: {}ms", duration);
            logger.info("平均 Token 时间: {}ms/token", tokenCount > 0 ? duration / tokenCount : 0);
            logger.info("============================");
        }

        @Override
        public void onError(Throwable error) {
            logger.error("========== SSE 错误 ==========");
            logger.error("异常类型: {}", error.getClass().getName());
            logger.error("错误消息: {}", error.getMessage());
            
            if (error.getCause() != null) {
                logger.error("根本原因: {}", error.getCause().getMessage());
            }
            
            logger.error("============================");
        }

        /**
         * 处理数据事件
         */
        private void handleData(String data) {
            fullResponse.append(data);
            
            // 统计 Token 数（简化实现）
            if (data.contains(" ")) {
                tokenCount++;
            }
            
            logger.info("接收数据: {} (总长度: {})", 
                        data.substring(0, Math.min(100, data.length())), 
                        fullResponse.length());
        }

        /**
         * 处理完成事件
         */
        private void handleDone() {
            logger.info("SSE 传输完成");
            logger.info("接收完整响应");
        }

        /**
         * 处理错误事件
         */
        private void handleError(String data) {
            logger.error("SSE 错误: {}", data);
        }

        /**
         * 获取完整响应
         */
        public String getFullResponse() {
            return fullResponse.toString();
        }

        /**
         * 获取 Token 统计
         */
        public int getTokenCount() {
            return tokenCount;
        }
    }

    public static void main(String[] args) {
        // 创建自定义事件监听器
        CustomSseEventListener listener = new CustomSseEventListener();

        // 创建带有事件监听器的模型
        OpenAiStreamingChatModel model = createModelWithEventListener(
            System.getenv("OPENAI_API_KEY"),
            listener
        );

        System.out.println("╔═════════════════════════════════════════════════════╗");
        System.out.println("║           SSE 事件监听                          ║");
        System.out.println("╠═════════════════════════════════════════════════════╣");
        System.out.println("║ 模型名称: gpt-4o-mini                          ║");
        System.out.println("║ 监听器类型: CustomSseEventListener          ║");
        System.out.println("╚═════════════════════════════════════════════════════╝");
        System.out.println();

        System.out.println("开始流式对话...");
        System.out.println("输入 'exit' 退出");
        System.out.println();

        Scanner scanner = new Scanner(System.in);

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

            System.out.print("AI: ");

            // 流式生成（SSE 事件会被监听器捕获）
            String response = model.chat(input);

            System.out.println();
            System.out.println("完整响应: " + listener.getFullResponse());
            System.out.println("Token 数量: " + listener.getTokenCount());
            System.out.println();
        }

        scanner.close();
    }
}
```

## 测试代码示例

```java
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

/**
 * 自定义 HTTP 客户端测试
 */
class CustomHttpClientTest {

    private String apiKey;

    @BeforeEach
    void setUp() {
        this.apiKey = System.getenv("OPENAI_API_KEY");
        assertNotNull(apiKey, "OPENAI_API_KEY 环境变量未设置");
    }

    @Test
    void should_create_model_with_custom_timeout() {
        // 创建带有自定义超时的模型
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .timeout(Duration.ofSeconds(30))
                .build();

        assertNotNull(model);
        assertEquals("gpt-4o-mini", model.getModelName());
    }

    @Test
    void should_create_model_with_retry_config() {
        // 创建带有重试配置的模型
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .maxRetries(3)
                .build();

        assertNotNull(model);
        assertNotNull(model);  // 验证模型创建成功
    }

    @Test
    void should_handle_timeout_gracefully() {
        // 创建带有超时的模型
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .timeout(Duration.ofMillis(100))  // 非常短的超时
                .build();

        // 注意：这个测试可能因为超时而失败
        // 实际应用中应该使用合理的超时时间
        try {
            String response = model.chat("测试超时");
            assertNotNull(response);
        } catch (Exception e) {
            // 验证错误类型
            assertTrue(e.getCause() instanceof java.util.concurrent.TimeoutException 
                || e.getCause() instanceof java.net.SocketTimeoutException
                || e.getCause() instanceof java.net.SocketTimeoutException);
        }
    }

    @Test
    void should_log_request_and_response() {
        // 创建带有日志的模型
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .logRequests(true)
                .logResponses(true)
                .build();

        assertNotNull(model);
        assertNotNull(model);  // 验证模型创建成功

        // 实际日志会在测试输出中显示
        String response = model.chat("你好");
        assertNotNull(response);
    }

    @Test
    void should_validate_api_key() {
        // 使用无效 API Key
        String invalidKey = "sk-invalid";

        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(invalidKey)
                .modelName("gpt-4o-mini")
                .timeout(Duration.ofSeconds(10))
                .build();

        // 尝试生成请求
        try {
            model.chat("test");
            fail("应该抛出异常");
        } catch (Exception e) {
            // 验证错误
            assertNotNull(e);
            // 实际应该包含 API Key 无效的信息
        }
    }
}
```

## 实践练习

### 练习 1：实现智能重试策略

```java
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.AiMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 智能重试策略
 */
public class SmartRetryStrategy {

    private static final Logger logger = LoggerFactory.getLogger(SmartRetryStrategy.class);

    /**
     * 重试配置
     */
    public static class RetryConfig {
        private final int maxRetries;
        private final Duration initialDelay;
        private final Duration maxDelay;
        private final double backoffFactor;
        private final String retryStrategy;  // "fixed", "exponential", "linear"

        public RetryConfig(
                int maxRetries,
                Duration initialDelay,
                Duration maxDelay,
                double backoffFactor,
                String retryStrategy
        ) {
            this.maxRetries = maxRetries;
            this.initialDelay = initialDelay;
            this.maxDelay = maxDelay;
            this.backoffFactor = backoffFactor;
            this.retryStrategy = retryStrategy;
        }

        // Getters
        public int getMaxRetries() { return maxRetries; }
        public Duration getInitialDelay() { return initialDelay; }
        public Duration getMaxDelay() { return maxDelay; }
        public double getBackoffFactor() { return backoffFactor; }
        public String getRetryStrategy() { return retryStrategy; }
    }

    /**
     * 重试记录
     */
    public static class RetryRecord {
        private final int attempt;
        private final long timestamp;
        private final String error;
        private final long delayMs;

        public RetryRecord(int attempt, String error, long delayMs) {
            this.attempt = attempt;
            this.timestamp = System.currentTimeMillis();
            this.error = error;
            this.delayMs = delayMs;
        }

        public int getAttempt() { return attempt; }
        public long getTimestamp() { return timestamp; }
        public String getError() { return error; }
        public long getDelayMs() { return delayMs; }
    }

    /**
     * 重试上下文
     */
    public static class RetryContext {
        private final String originalRequest;
        private final List<RetryRecord> attempts;
        private final RetryConfig config;

        public RetryContext(String originalRequest, RetryConfig config) {
            this.originalRequest = originalRequest;
            this.attempts = new ArrayList<>();
            this.config = config;
        }

        /**
         * 记录重试
         */
        public void recordRetry(String error, long delayMs) {
            int attempt = attempts.size() + 1;
            RetryRecord record = new RetryRecord(attempt, error, delayMs);
            attempts.add(record);

            logger.info("记录重试 - 尝试 {}: 错误: {}, 延迟: {}ms",
                        attempt, error, delayMs);
        }

        /**
         * 计算下一次重试延迟
         */
        public long calculateNextDelay() {
            return switch (config.getRetryStrategy()) {
                case "fixed" -> config.getInitialDelay().toMillis();
                case "exponential" -> calculateExponentialDelay();
                case "linear" -> calculateLinearDelay();
                default -> config.getInitialDelay().toMillis();
            };
        }

        /**
         * 计算指数退避延迟
         */
        private long calculateExponentialDelay() {
            int attempt = attempts.size();
            long delay = (long) (config.getInitialDelay().toMillis() * 
                            Math.pow(config.getBackoffFactor(), attempt));
            return Math.min(delay, config.getMaxDelay().toMillis());
        }

        /**
         * 计算线性退避延迟
         */
        private long calculateLinearDelay() {
            int attempt = attempts.size();
            long delay = config.getInitialDelay().toMillis() + 
                         (attempt * 1000);  // 每次增加 1 秒
            return Math.min(delay, config.getMaxDelay().toMillis());
        }

        /**
         * 判断是否应该重试
         */
        public boolean shouldRetry() {
            return attempts.size() < config.getMaxRetries();
        }

        /**
         * 获取重试次数
         */
        public int getRetryCount() {
            return attempts.size();
        }

        /**
         * 获取所有重试记录
         */
        public List<RetryRecord> getAttempts() {
            return new ArrayList<>(attempts);
        }

        /**
         * 获取重试摘要
         */
        public String getRetrySummary() {
            if (attempts.isEmpty()) {
                return "无重试";
            }

            return String.format(
                "总重试次数: %d\n" +
                "最后一次错误: %s\n" +
                "总耗时: %dms",
                attempts.size(),
                attempts.get(attempts.size() - 1).getError(),
                attempts.stream().mapToLong(RetryRecord::getDelayMs).sum()
            );
        }
    }

    /**
     * 智能重试处理器
     */
    public static class SmartRetryHandler {

        private static final Logger logger = LoggerFactory.getLogger(SmartRetryHandler.class);

        /**
         * 带重试的请求执行
         */
        public String executeWithRetry(
                OpenAiChatModel model,
                String prompt,
                RetryConfig config
        ) {
            RetryContext context = new RetryContext(prompt, config);
            Exception lastError = null;

            while (context.shouldRetry()) {
                try {
                    // 执行请求
                    logger.info("执行请求（尝试 {}）", context.getRetryCount() + 1);
                    String response = model.chat(prompt);

                    logger.info("请求成功");
                    return response;
                } catch (Exception e) {
                    lastError = e;

                    // 计算延迟
                    long delayMs = context.calculateNextDelay();

                    // 记录重试
                    context.recordRetry(e.getMessage(), delayMs);

                    // 等待重试延迟
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试被中断", ie);
                    }
                }
            }

            throw new RuntimeException(
                "重试失败，已达到最大重试次数: " + config.getMaxRetries() + "\n" +
                "重试摘要:\n" + context.getRetrySummary(),
                lastError
            );
        }

        /**
         * 分析失败原因
         */
        public String analyzeFailure(RetryContext context) {
            if (context.getRetryCount() == 0) {
                return "首次请求失败";
            }

            // 分析失败模式
            List<RetryRecord> attempts = context.getAttempts();
            
            if (attempts.size() < 2) {
                return "数据不足，无法分析";
            }

            // 检查错误是否一致
            String lastError = attempts.get(attempts.size() - 1).getError();
            boolean allSameError = attempts.stream()
                    .allMatch(record -> record.getError().equals(lastError));

            if (allSameError) {
                return "所有重试都失败于相同错误: " + lastError +
                       "\n建议: 检查 API Key、请求格式或网络连接";
            } else {
                return "重试失败原因不同：" + lastError +
                       "\n建议: 检查请求参数和服务器状态";
            }
        }

        /**
         * 获取建议的解决方案
         */
        public String getSuggestedSolution(RetryContext context) {
            String analysis = analyzeFailure(context);

            StringBuilder suggestions = new StringBuilder();
            suggestions.append("失败分析:\n").append(analysis).append("\n\n");
            suggestions.append("建议的解决方案:\n");
            suggestions.append("1. 检查网络连接\n");
            suggestions.append("2. 验证 API Key 有效性\n");
            suggestions.append("3. 检查请求格式和参数\n");
            suggestions.append("4. 检查服务器状态和可用性\n");
            suggestions.append("5. 考虑增加超时时间\n");

            return suggestions.toString();
        }

        public static void main(String[] args) {
            // 创建重试配置
            RetryConfig config = new RetryConfig(
                    3,                      // 最多重试 3 次
                    Duration.ofSeconds(1),    // 初始延迟 1 秒
                    Duration.ofSeconds(10),   // 最大延迟 10 秒
                    2.0,                    // 指数退避因子
                    "exponential"           // 指数退避策略
            );

            SmartRetryHandler handler = new SmartRetryHandler();

            // 创建模型
            OpenAiChatModel model = OpenAiChatModel.builder()
                    .apiKey(System.getenv("OPENAI_API_KEY"))
                    .modelName("gpt-4o-mini")
                    .build();

            // 执行带重试的请求
            System.out.println("╔════════════════════════════════════════════════════════════╗");
            System.out.println("║               智能重试演示                              ║");
            System.out.println("╠════════════════════════════════════════════════════════════╣");
            System.out.println("║ 最大重试次数: " + config.getMaxRetries() + "                                ║");
            System.out.println("║ 初始延迟: " + config.getInitialDelay().getSeconds() + " 秒                         ║");
            System.out.println("║ 最大延迟: " + config.getMaxDelay().getSeconds() + " 秒                         ║");
            System.out.println("║ 退避因子: " + config.getBackoffFactor() + "                               ║");
            System.out.println("║ 退避策略: " + config.getRetryStrategy() + "                           ║");
            System.out.println("╚════════════════════════════════════════════════════════════╝");
            System.out.println();

            try {
                String response = handler.executeWithRetry(model, "你好", config);
                System.out.println("成功响应: " + response);
            } catch (RuntimeException e) {
                System.err.println("请求失败: " + e.getMessage());
            }
        }
    }
}
```

### 练习 2：实现连接池配置

```java
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 连接池配置
 */
public class ConnectionPoolConfig {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionPoolConfig.class);

    /**
     * 连接池配置
     */
    public static class PoolConfig {
        private final int maxConnections;
        private final int maxConnectionsPerRoute;
        private final int maxIdleConnections;
        private final long keepAliveDuration;
        private final long connectionTimeout;
        private final long readTimeout;

        public PoolConfig(
                int maxConnections,
                int maxConnectionsPerRoute,
                int maxIdleConnections,
                long keepAliveDuration,
                long connectionTimeout,
                long readTimeout
        ) {
            this.maxConnections = maxConnections;
            this.maxConnectionsPerRoute = maxConnectionsPerRoute;
            this.maxIdleConnections = maxIdleConnections;
            this.keepAliveDuration = keepAliveDuration;
            this.connectionTimeout = connectionTimeout;
            this.readTimeout = readTimeout;
        }

        // Getters
        public int getMaxConnections() { return maxConnections; }
        public int getMaxConnectionsPerRoute() { return maxConnectionsPerRoute; }
        public int getMaxIdleConnections() { return maxIdleConnections; }
        public long getKeepAliveDuration() { return keepAliveDuration; }
        public long getConnectionTimeout() { return connectionTimeout; }
        public long getReadTimeout() { return readTimeout; }
    }

    /**
     * 创建带有连接池配置的模型
     */
    public static OpenAiChatModel createModelWithConnectionPool(
            String apiKey,
            PoolConfig config
    ) {
        logger.info("创建带有连接池配置的模型");

        // 注意：LangChain4j 的连接池配置可能需要通过特定的 HTTP 客户端库实现
        // 这里展示概念和配置方法

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .timeout(Duration.ofMillis(config.getReadTimeout()))
                .build();
    }

    /**
     * 连接池统计
     */
    public static class PoolStatistics {
        private final PoolConfig config;
        private final int activeConnections;
        private final int idleConnections;
        private final long totalRequests;
        private final long totalBytesTransferred;

        public PoolStatistics(
                PoolConfig config,
                int activeConnections,
                int idleConnections,
                long totalRequests,
                long totalBytesTransferred
        ) {
            this.config = config;
            this.activeConnections = activeConnections;
            this.idleConnections = idleConnections;
            this.totalRequests = totalRequests;
            this.totalBytesTransferred = totalBytesTransferred;
        }

        /**
         * 获取连接池状态
         */
        public String getStatus() {
            return String.format(
                "总连接: %d\n" +
                "活跃连接: %d\n" +
                "空闲连接: %d\n" +
                "总请求数: %d\n" +
                "总传输字节: %d\n" +
                "平均每连接请求数: %.2f",
                activeConnections + idleConnections,
                activeConnections,
                idleConnections,
                totalRequests,
                totalBytesTransferred,
                totalRequests > 0 ? (double) totalRequests / (activeConnections + idleConnections) : 0
            );
        }
    }

    /**
         * 获取配置建议
         */
    public static String getConfigSuggestions(PoolConfig config, PoolStatistics stats) {
            StringBuilder suggestions = new StringBuilder();
            
            suggestions.append("连接池配置建议:\n");
            
            // 检查最大连接数
            if (stats.getActiveConnections() > config.getMaxConnections() * 0.9) {
                suggestions.append("建议: 考虑增加 maxConnections，当前连接数接近上限\n");
            } else if (stats.getActiveConnections() < config.getMaxConnections() * 0.3) {
                suggestions.append("建议: 考虑减少 maxConnections以节省资源\n");
            }
            
            // 检查空闲连接
            if (stats.getIdleConnections() > config.getMaxIdleConnections() * 0.8) {
                suggestions.append("建议: 考虑减少 maxIdleConnections以节省资源\n");
            }
            
            // 检查超时配置
            if (config.getConnectionTimeout() > 30000) {
                suggestions.append("建议: connectionTimeout 过长，可能导致响应慢\n");
            }
            
            // 检查 KeepAlive
            if (config.getKeepAliveDuration() > 300000) {
                suggestions.append("建议: keepAliveDuration 过长，可能导致连接浪费\n");
            }
            
            if (suggestions.length() == 0) {
                suggestions.append("当前配置合理，无需调整\n");
            }
            
            return suggestions.toString();
        }

    public static void main(String[] args) {
        // 创建连接池配置
        PoolConfig config = new PoolConfig(
                50,                       // 最大连接数
                10,                       // 每个路由的最大连接数
                20,                       // 最大空闲连接数
                60000,                    // Keep-Alive 60 秒
                30000,                    // 连接超时 30 秒
                60000                     // 读取超时 60 秒
        );

        // 创建带有连接池配置的模型
        OpenAiChatModel model = createModelWithConnectionPool(
            System.getenv("OPENAI_API_KEY"),
            config
        );

        // 模拟连接池统计
        PoolStatistics stats = new PoolStatistics(
            config,
            15,                       // 活跃连接数
            20,                       // 空闲连接数
            1000,                     // 总请求数
            10240000                  // 总传输字节数
        );

        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║               连接池配置                              ║");
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.println("║ 最大连接数: " + config.getMaxConnections() + "                                ║");
        System.out.println("║ 每路由最大连接: " + config.getMaxConnectionsPerRoute() + "                              ║");
        System.out.println("║ 最大空闲连接: " + config.getMaxIdleConnections() + "                              ║");
        System.out.println("║ Keep-Alive: " + (config.getKeepAliveDuration() / 1000) + " 秒                       ║");
        System.out.println("║ 连接超时: " + (config.getConnectionTimeout() / 1000) + " 秒                      ║");
        System.out.println("║ 读取超时: " + (config.getReadTimeout() / 1000) + " 秒                      ║");
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.println("║ " + stats.getStatus());
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.println("║ 配置建议:                                               ║");
        System.out.println("║ " + getConfigSuggestions(config, stats).replace("\n", "\n║ "));
        System.out.println("╚═══════════════════════════════════════════════════════════════════════╝");
    }
}
```

## 总结

### 本章要点

1. **自定义 HTTP 客户端概念**
   - 理解 LangChain4j 的 HTTP 客户端机制
   - 掌握自定义配置的方法
   - 认识高级特性（超时、重试、代理等）

2. **基础配置**
   - 基础 URL 和认证配置
   - 超时和重试设置
   - 日志记录

3. **高级特性**
   - 请求和响应拦截
   - 自定义事件监听
   - 连接池配置
   - 代理和安全设置

4. **最佳实践**
   - 合理配置超时时间
   - 实现智能重试策略
   - 记录和监控请求
   - 优化连接池参数

5. **应用场景**
   - 企业内网部署
   - 安全合规要求
   - 性能优化
   - 故障排查

### 下一步

在下一章节中，我们将学习：
- 测试和评估
- 性能基准测试
- A/B 测试
- 错误注入测试
- 生产环境部署

### 常见问题

**Q1：如何配置代理？**

A：配置方法：
1. 使用 Java 的 `Proxy` 类
2. 配置系统属性 `http.proxyHost` 和 `http.proxyPort`
3. 在 LangChain4j 模型构建器中设置 `httpClient`
4. 环境变量配置

**Q2：如何设置合适的超时时间？**

A：建议策略：
1. 连接超时：5-10 秒（网络往返时间）
2. 读取超时：根据模型响应时间，通常 30-60 秒
3. 考虑网络延迟和服务器负载
4. 测试不同值找到最佳配置

**Q3：如何实现智能重试？**

A：重试策略：
1. 指数退避：延迟随重试次数指数增长
2. 线性退避：延迟线性增长
3. 随机抖动：随机延迟，避免重试风暴
4. 最大重试次数：通常 3-5 次

**Q4：如何监控 HTTP 客户端？**

A：监控指标：
1. 请求/响应时间
2. 错误率和重试次数
3. 连接池使用情况
4. 网络吞吐量
5. Token 使用和成本

**Q5：如何优化连接池？**

A：优化技巧：
1. 设置合理的最大连接数
2. 配置适当的 KeepAlive 时间
3. 调整连接超时和读取超时
4. 监控连接使用情况
5. 根据负载动态调整

## 参考资料

- [LangChain4j 自定义 HTTP 客户端文档](https://docs.langchain4j.dev/tutorials/customizable-http-client)
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
