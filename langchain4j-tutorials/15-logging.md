---
title: '日志'
description: '学习 LangChain4j 的 日志 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 15 - 日志

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 理解 LangChain4j 日志的重要性
- 掌握日志级别和配置
- 学会使用 SLF4J 记录日志
- 理解请求和响应日志
- 掌握日志过滤和输出控制
- 实现一个完整的日志配置

## 前置知识

- 完成《01 - LangChain4j 简介》章节
- 完成《02 - 你的第一个 Chat 应用》章节
- Java 日志框架基础知识（SLF4J, Logback）

## 核心概念

### 为什么需要日志？

**日志**是任何应用的重要组件，特别是与 LLM 交互的应用：

1. **调试问题** - 追踪 API 请求和响应
2. **性能监控** - 测量响应时间和吞吐量
3. **合规审计** - 记录敏感操作和数据访问
4. **错误追踪** - 捕获和分析错误模式
5. **成本控制** - 监控 Token 使用量和 API 费用
6. **用户行为分析** - 了解用户如何与 AI 交互

### LangChain4j 日志功能

LangChain4j 提供了丰富的日志功能：

- **请求日志** - 记录发送给 LLM 的完整请求
- **响应日志** - 记录从 LLM 返回的完整响应
- **Token 使用日志** - 记录每个请求的 Token 消耗
- **错误日志** - 记录 API 错误和异常
- **性能日志** - 记录请求耗时和吞吐量

### 日志级别

```
ERROR   - 错误级别，应用程序无法继续运行
WARN    - 警告级别，潜在问题，但应用仍可运行
INFO    - 信息级别，重要的业务逻辑信息
DEBUG   - 调试级别，详细的诊断信息
TRACE   - 跟踪级别，最详细的信息
```

## 日志配置

### 基础日志配置

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基础日志示例
 */
public class BasicLoggingExample {

    private static final Logger logger = LoggerFactory.getLogger(BasicLoggingExample.class);

    public void doSomething() {
        logger.info("开始执行任务");

        try {
            // 业务逻辑
            String result = processData("输入数据");
            logger.info("任务执行成功: {}", result);
        } catch (Exception e) {
            logger.error("任务执行失败", e);
            throw new RuntimeException("任务失败", e);
        }
    }

    private String processData(String data) {
        logger.debug("处理数据: {}", data);
        return "处理结果: " + data;
    }

    public static void main(String[] args) {
        BasicLoggingExample example = new BasicLoggingExample();
        example.doSomething();
    }
}
```

### 配置请求和响应日志

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;

/**
 * 配置请求和响应日志
 */
public class RequestResponseLogging {

    private final ChatModel model;

    public RequestResponseLogging(String apiKey) {
        this.model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .logRequests(true)   // 启用请求日志
                .logResponses(true)  // 启用响应日志
                .build();
    }

    public void generateChat() {
        ChatResponse response = model.chat("你好");

        // 日志已自动记录
        System.out.println("响应: " + response.aiMessage().text());
    }

    public static void main(String[] args) {
        RequestResponseLogging logging = new RequestResponseLogging(
            System.getenv("OPENAI_API_KEY")
        );

        logging.generateChat();
    }
}
```

### 日志格式化

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日志格式化
 */
public class LoggingFormatter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFormatter.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 结构化日志方法
     */
    public static void logStructuredEvent(String eventName, Object eventData) {
        try {
            String json = mapper.writeValueAsString(eventData);
            logger.info("[{}] {}", eventName, json);
        } catch (Exception e) {
            logger.warn("无法序列化事件数据", e);
        }
    }

    /**
     * 格式化 Token 使用日志
     */
    public static void logTokenUsage(
            String modelId,
            int inputTokens,
            int outputTokens,
            int totalTokens
    ) {
        try {
            String json = mapper.writeValueAsString(Map.of(
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "model_id", modelId,
                "input_tokens", inputTokens,
                "output_tokens", outputTokens,
                "total_tokens", totalTokens,
                "estimated_cost", calculateCost(totalTokens, modelId)
            ));
            
            logger.info("Token 使用: {}", json);
        } catch (Exception e) {
            logger.warn("无法记录 Token 使用", e);
        }
    }

    /**
     * 计算成本
     */
    private static double calculateCost(int totalTokens, String modelId) {
        // 简化：假设每 1000 tokens $0.01
        return (double) totalTokens / 100000.0;
    }

    public static void main(String[] args) {
        // 记录 Token 使用
        logTokenUsage("gpt-4o-mini", 1500, 3000, 4500);

        // 记录结构化事件
        Map<String, Object> eventData = Map.of(
            "event_type", "chat",
            "user_id", "user123",
            "message_length", 100,
            "model", "gpt-4o-mini"
        );

        logStructuredEvent("user_chat", eventData);
    }
}
```

## 日志过滤和输出控制

### 配置日志级别

```xml
<!-- logback.xml -->
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 控制台日志级别 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
    </root>

    <!-- LangChain4j 日志级别 -->
    <logger name="dev.langchain4j" level="DEBUG" />

    <!-- 只记录警告和错误 -->
    <logger name="dev.langchain4j.model.chat.ChatModel" level="WARN" />
</configuration>
```

### 动态日志级别

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 * 动态日志级别
 */
public class DynamicLogLevel {

    private static final Logger logger = LoggerFactory.getLogger(DynamicLogLevel.class);

    /**
     * 根据配置设置日志级别
     */
    public static void setLogLevel(String loggerName, String level) {
        ch.qos.logback.classic.LoggerContext loggerContext = 
            (ch.qos.logback.classic.LoggerContext) LoggerFactory.getLogger(loggerName);

        Level targetLevel = Level.valueOf(level);
        loggerContext.setLevel(targetLevel);

        logger.info("已设置 {} 的日志级别为 {}", loggerName, level);
    }

    /**
     * 临时降低日志级别
     */
    public static void setTempLogLevel(String loggerName, String level, Runnable runnable) {
        ch.qos.logback.classic.LoggerContext loggerContext = 
            (ch.qos.logback.classic.LoggerContext) LoggerFactory.getLogger(loggerName);

        Level originalLevel = loggerContext.getLevel();
        Level targetLevel = Level.valueOf(level);

        try {
            loggerContext.setLevel(targetLevel);
            logger.info("临时降低 {} 的日志级别到 {}", loggerName, level);
            
            runnable.run();
        } finally {
            loggerContext.setLevel(originalLevel);
            logger.info("恢复 {} 的日志级别到 {}", loggerName, originalLevel);
        }
    }

    /**
     * 查询当前日志级别
     */
    public static String getLogLevel(String loggerName) {
        ch.qos.logback.classic.LoggerContext loggerContext = 
            (ch.qos.logback.classic.LoggerContext) LoggerFactory.getLogger(loggerName);

        return loggerContext.getLevel().toString();
    }

    public static void main(String[] args) {
        // 查询当前日志级别
        System.out.println("当前日志级别: " + getLogLevel("dev.langchain4j.model.chat"));

        // 设置新的日志级别
        setLogLevel("dev.langchain4j.model.chat", "TRACE");

        // 临时调试
        setTempLogLevel("dev.langchain4j", "DEBUG", () -> {
            logger.info("这是调试信息");
            logger.debug("这是调试信息");
        });
    }
}
```

## 日志与监控

### 性能监控日志

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

/**
 * 性能监控日志
 */
public class PerformanceMonitor {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitor.class);

    /**
     * 性能计时器
     */
    public static class PerformanceTimer {
        private final String operationName;
        private final Instant startTime;
        private final Logger logger;
        private final boolean logDuration;
        private final long thresholdMs;

        public PerformanceTimer(String operationName, Logger logger, boolean logDuration, long thresholdMs) {
            this.operationName = operationName;
            this.logger = logger;
            this.logDuration = logDuration;
            this.thresholdMs = thresholdMs;
            this.startTime = Instant.now();
        }

        public void close() {
            Duration duration = Duration.between(startTime, Instant.now());
            long durationMs = duration.toMillis();

            if (logDuration) {
                if (durationMs > thresholdMs) {
                    logger.warn("操作 {} 耗时 {}ms (超过阈值 {}ms)", 
                            operationName, durationMs, thresholdMs);
                } else {
                    logger.info("操作 {} 耗时 {}ms", operationName, durationMs);
                }
            }

            logger.debug("操作 {} 开始时间: {}, 结束时间: {}", 
                        operationName, startTime, Instant.now());
        }
    }

    /**
     * 记录性能指标
     */
    public static void logMetrics(
            String operation,
            long durationMs,
            int tokenCount,
            int tokensPerSecond
    ) {
        logger.info("性能指标 - 操作: {}, 耗时: {}ms, Token 数: {}, TPS: {}", 
                    operation, durationMs, tokenCount, tokensPerSecond);
    }

    /**
     * 记录慢查询
     */
    public static void logSlowQuery(
            String query,
            long durationMs,
            long thresholdMs
    ) {
        if (durationMs > thresholdMs) {
            logger.warn("慢查询检测 - 查询: {}, 耗时: {}ms (阈值: {}ms), SQL: {}", 
                        query.substring(0, Math.min(100, query.length())), 
                        durationMs, thresholdMs, query);
        }
    }

    public static void main(String[] args) throws Exception {
        // 使用性能计时器
        try (PerformanceTimer timer = new PerformanceTimer("generateChat", logger, true, 1000)) {
            // 模拟耗时操作
            Thread.sleep(500);
            logger.info("生成聊天响应...");
        }

        // 记录性能指标
        logMetrics("generateChat", 500, 1000, 2);

        // 慢查询警告
        String query = "SELECT * FROM users WHERE name LIKE '%Java%'";
        logSlowQuery(query, 2000, 1000);
    }
}
```

### Token 使用监控

```java
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.tool.ToolExecutionRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Token 使用监控
 */
public class TokenUsageMonitor {

    private static final Logger logger = LoggerFactory.getLogger(TokenUsageMonitor.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.of("yyyy-MM-dd HH:mm:ss");

    // 统计数据
    private static final AtomicInteger totalInputTokens = new AtomicInteger(0);
    private static final AtomicInteger totalOutputTokens = new AtomicInteger(0);
    private static final AtomicLong totalCost = new AtomicLong(0);
    private static final ConcurrentHashMap<String, ModelStats> modelStats = new ConcurrentHashMap<>();

    /**
     * 记录 Token 使用
     */
    public static void recordTokenUsage(
            String modelId,
            int inputTokens,
            int outputTokens
    ) {
        totalInputTokens.addAndGet(inputTokens);
        totalOutputTokens.addAndGet(outputTokens);
        int totalTokens = inputTokens + outputTokens;
        
        double cost = calculateCost(totalTokens, modelId);
        totalCost.addAndGet((long) (cost * 100));  // 转换为分

        // 记录详细日志
        logger.info("Token 使用 - 模型: {}, 输入: {}, 输出: {}, 总计: {}, 成本: ${}", 
                    modelId, inputTokens, outputTokens, totalTokens, String.format("%.4f", cost));

        // 更新模型统计
        modelStats.compute(modelId, (id, stats) -> {
            if (stats == null) {
                stats = new ModelStats();
            }
            stats.addUsage(inputTokens, outputTokens);
            return stats;
        });
    }

    /**
     * 获取统计摘要
     */
    public static String getStatsSummary() {
        int totalTokens = totalInputTokens.get() + totalOutputTokens.get();
        double totalCostDollars = totalCost.get() / 100.0;

        StringBuilder sb = new StringBuilder();
        sb.append("╔═══════════════════════════════════════════╗\n");
        sb.append("║           Token 使用统计                      ║\n");
        sb.append("╠═══════════════════════════════════════════╣\n");
        sb.append(String.format("║ 总输入 Token: %,10d                    ║\n", totalInputTokens.get()));
        sb.append(String.format("║ 总输出 Token: %,10d                    ║\n", totalOutputTokens.get()));
        sb.append(String.format("║ 总 Token: %,10d                            ║\n", totalTokens));
        sb.append(String.format("║ 总成本: $%.2f                           ║\n", totalCostDollars));
        sb.append("╠═══════════════════════════════════════════╣\n");

        // 按模型分组
        sb.append("║ 按模型统计:                                     ║\n");
        sb.append("╠═══════════════════════════════════════════╣\n");
        
        modelStats.forEach((modelId, stats) -> {
            sb.append(String.format("║ %s: %,10d tokens, $%.2f           ║\n",
                    modelId, stats.getTotalTokens(), stats.getCost()));
        });

        sb.append("╚═══════════════════════════════════════════╝\n");

        return sb.toString();
    }

    /**
     * 重置统计
     */
    public static void resetStats() {
        totalInputTokens.set(0);
        totalOutputTokens.set(0);
        totalCost.set(0);
        modelStats.clear();
        logger.info("Token 使用统计已重置");
    }

    /**
     * 导出统计数据
     */
    public static void exportStatsAsJson() {
        // 使用 JSON 处理将统计导出为 JSON
        // 这里简化实现
        logger.info("导出统计数据为 JSON");
    }

    /**
     * 计算成本
     */
    private static double calculateCost(int totalTokens, String modelId) {
        // 简化成本计算
        double pricePer1kTokens;

        if (modelId.contains("gpt-4")) {
            pricePer1kTokens = 0.03;  // $0.03 per 1K tokens
        } else if (modelId.contains("gpt-3.5")) {
            pricePer1kTokens = 0.002;  // $0.002 per 1K tokens
        } else {
            pricePer1kTokens = 0.001;  // 默认价格
        }

        return (totalTokens / 1000.0) * pricePer1kTokens;
    }

    /**
     * 模型统计
     */
    public static class ModelStats {
        private final AtomicInteger requestCount;
        private final AtomicInteger inputTokens;
        private final AtomicInteger outputTokens;

        public ModelStats() {
            this.requestCount = new AtomicInteger(0);
            this.inputTokens = new AtomicInteger(0);
            this.outputTokens = new AtomicInteger(0);
        }

        public void addUsage(int input, int output) {
            requestCount.incrementAndGet();
            inputTokens.addAndGet(input);
            outputTokens.addAndGet(output);
        }

        public int getRequestCount() { return requestCount.get(); }
        public int getInputTokens() { return inputTokens.get(); }
        public int getOutputTokens() { return outputTokens.get(); }
        public int getTotalTokens() { return getInputTokens() + getOutputTokens(); }
        public double getCost() {
            return calculateCost(getTotalTokens(), "gpt-4o");
        }
    }

    public static void main(String[] args) {
        // 模拟一些 Token 使用
        recordTokenUsage("gpt-4o-mini", 500, 300);
        recordTokenUsage("gpt-4o-mini", 800, 500);
        recordTokenUsage("gpt-3.5-turbo", 2000, 1500);

        // 显示统计
        System.out.println(getStatsSummary());

        // 定期输出统计
        logger.info("定期 Token 使用统计:\n{}", getStatsSummary());
    }
}
```

## 日志最佳实践

### 日志最佳实践

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志最佳实践
 */
public class LoggingBestPractices {

    private static final Logger logger = LoggerFactory.getLogger(LoggingBestPractices.class);

    /**
     * 最佳实践 1: 使用参数化日志
     */
    public void parameterizedLogging(String userId, String action, Object result) {
        // ✅ 好的做法
        logger.info("用户 {} 执行了操作 {}, 结果: {}", userId, action, result);

        // ❌ 不好的做法
        // logger.info("用户 " + userId + " 执行了操作 " + action + ", 结果: " + result);
    }

    /**
     * 最佳实践 2: 避免在循环中过度记录
     */
    public void loggingInLoops(List<String> items) {
        // ✅ 好的做法：记录汇总信息
        logger.info("处理 {} 个项目", items.size());
        
        for (String item : items) {
            processItem(item);
            // ❌ 不好的做法：每个循环都记录
            // logger.info("处理项目: " + item);
        }
        
        logger.info("所有项目处理完成");
    }

    /**
     * 最佳实践 3: 使用适当的日志级别
     */
    public void appropriateLogLevel(String message, Throwable error) {
        // ✅ 好的做法：根据情况选择级别
        if (error instanceof IllegalArgumentException) {
            logger.warn("参数错误: {}", message, error);
        } else if (error instanceof RuntimeException) {
            logger.error("运行时错误: {}", message, error);
        } else {
            logger.error("未知错误: {}", message, error);
        }
    }

    /**
     * 最佳实践 4: 结构化日志输出
     */
    public void structuredLogging(String eventType, String eventId, Object payload) {
        logger.info("{} | {} | {}", eventType, eventId, payload);
        // 输出: CHAT | 12345 | {"user": "张三", "message": "你好"}
    }

    /**
     * 最佳实践 5: 异常堆栈的合理使用
     */
    public void exceptionHandling(String context, Exception e) {
        // ✅ 好的做法：先记录消息，再记录异常
        logger.error("操作失败: {}", context, e);

        // ❌ 不好的做法：只记录异常
        // logger.error("操作失败", e);
    }

    /**
     * 最佳实践 6: 敏感信息的保护
     */
    public void sensitiveDataHandling(String password, String creditCard) {
        // ✅ 好的做法：不记录敏感信息
        logger.info("处理用户认证请求");
        // logger.info("密码: {}, 卡号: {}", password, creditCard);  // ❌

        // 如果必须记录，使用哈希
        logger.info("密码哈希: {}, 卡号哈希: {}", 
                    hashValue(password), hashValue(creditCard));
    }

    /**
     * 最佳实践 7: 性能关键路径
     */
    public void performanceCriticalPath(String operation) {
        long startTime = System.currentTimeMillis();
        
        // 执行操作
        String result = performOperation(operation);
        
        long duration = System.currentTimeMillis() - startTime;
        
        // 性能关键路径使用 DEBUG 或 INFO
        if (duration > 1000) {
            logger.warn("操作 {} 耗时 {}ms (性能警告)", operation, duration);
        } else {
            logger.debug("操作 {} 耗时 {}ms", operation, duration);
        }
    }

    /**
     * 最佳实践 8: 上下文信息
     */
    public void contextualLogging(String userId, String sessionId, String action) {
        // ✅ 好的做法：提供上下文
        logger.info("[用户: {}, 会话: {}] {}", userId, sessionId, action);
    }

    /**
     * 最佳实践 9: 审计日志
     */
    public void auditLogging(String action, String actor, String target, String result) {
        logger.info("审计日志 - 动作: {}, 执行者: {}, 目标: {}, 结果: {}", 
                    action, actor, target, result);
    }

    private String processItem(String item) {
        // 模拟处理
        return "处理结果: " + item;
    }

    private String performOperation(String operation) {
        // 模拟操作
        return "操作完成: " + operation;
    }

    private String hashValue(String value) {
        return "HASH_" + value.hashCode();
    }

    public static void main(String[] args) {
        LoggingBestPractices practices = new LoggingBestPractices();

        practices.parameterizedLogging("user123", "登录", "成功");
        System.out.println();

        practices.loggingInLoops(List.of("项目1", "项目2", "项目3", "项目4", "项目5"));
        System.out.println();

        try {
            practices.appropriateLogLevel("参数验证失败", new IllegalArgumentException("用户名不能为空"));
        } catch (IllegalArgumentException e) {
            logger.error("捕获异常: {}", e.getMessage());
        }
        System.out.println();

        practices.exceptionHandling("数据库连接", new RuntimeException("连接超时"));
        System.out.println();

        practices.sensitiveDataHandling("password123", "4111111111111111");
        System.out.println();

        practices.structuredLogging("CHAT", "event_123", Map.of(
            "user_id", "user123",
            "message", "你好"
        ));
        System.out.println();

        practices.auditLogging("删除用户", "admin", "user456", "成功");
    }
}
```

## 总结

### 本章要点

1. **日志重要性**
   - 调试和问题排查
   - 性能监控和优化
   - 合规审计和成本控制

2. **日志配置**
   - 请求和响应日志
   - Token 使用监控
   - 错误和警告日志

3. **日志级别**
   - DEBUG - 详细诊断信息
   - INFO - 重要业务逻辑
   - WARN - 潜在问题
   - ERROR - 应用错误

4. **最佳实践**
   - 使用参数化日志
   - 避免过度日志
   - 使用适当的日志级别
   - 保护敏感信息

5. **监控**
   - 性能监控
   - Token 使用统计
   - 成本计算

### 下一步

在下一章节中，我们将学习：
- 可观测性配置
- 分布式追踪
- 性能分析和优化
- 错误处理和重试
- 生产环境部署建议

### 常见问题

**Q1：如何启用 LangChain4j 的日志？**

A：
1. 配置 SLF4J 日志级别
2. 在构建模型时设置 `logRequests(true)` 和 `logResponses(true)`
3. 使用 `-Dorg.slf4j.simpleLogger.logLevel=debug` JVM 参数
4. 配置 logback.xml 文件

**Q2：如何减少日志的噪音？**

A：
1. 合理设置日志级别（生产环境使用 INFO 或 WARN）
2. 避免在循环中过度记录
3. 使用条件日志记录
4. 配置日志过滤器

**Q3：如何记录 Token 使用情况？**

A：
1. 启用响应日志（logResponses(true)）
2. 从 ChatResponse 中获取 Token 使用量
3. 创建自定义 Token 监控器
4. 定期统计和报告

**Q4：如何在生产环境中配置日志？**

A：生产环境建议：
1. 日志级别设置为 INFO 或 WARN
2. 使用异步日志追加器
3. 配置日志轮转和归档
4. 避免敏感信息记录
5. 集中日志到日志系统（如 ELK）

**Q5：如何监控 API 成本？**

A：
1. 记录每个请求的 Token 使用量
2. 计算每个模型的成本
3. 设置成本告警阈值
4. 定期生成成本报告
5. 与预算进行比较和控制

## 参考资料

- [LangChain4j 日志文档](https://docs.langchain4j.dev/tutorials/logging)
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
