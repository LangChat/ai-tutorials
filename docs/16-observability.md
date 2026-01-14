---
title: '可观测性'
description: '学习 LangChain4j 的 可观测性 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 16 - 可观测性

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 理解可观测性的概念和重要性
- 掌握 LangChain4j 可观测性组件
- 学会配置和启用监控功能
- 理解分布式追踪的概念
- 掌握性能指标和监控
- 实现一个完整的可观测性解决方案

## 前置知识

- 完成《01 - LangChain4j 简介》章节
- 完成《02 - 你的第一个 Chat 应用》章节
- 完成《15 - 日志》章节（强烈推荐）

## 核心概念

### 什么是可观测性？

**可观测性**是指通过日志、指标、追踪三种信号，从系统外部观察和分析系统内部状态的能力。

**可观测性三支柱：**

```
┌─────────────────────────────────────────────────────────────────┐
│                        可观测性                        │
├─────────────────────────────────────────────────────────────────┤
│                                                             │
│  日志              │  指标              │  追踪              │
│  (Logs)             │  (Metrics)           │  (Tracing)           │
│                                                             │
│  • 发生了什么       │  • 发生了多少       │  • 在哪里发生       │
│  • 时间戳           │  • 统计数据         │  • 请求链路          │
│  • 错误消息         │  • 性能计数器       │  • 分布式上下文      │
│                                                             │
└─────────────────────────────────────────────────────────────────┘
```

### 为什么需要可观测性？

对于 LLM 应用，可观测性尤为重要：

1. **成本控制** - 监控 Token 使用量和 API 费用
2. **性能优化** - 识别慢查询和瓶颈
3. **问题排查** - 快速定位和解决错误
4. **用户体验** - 监控响应时间和可用性
5. **合规审计** - 记录敏感操作和数据访问
6. **容量规划** - 基于使用模式进行资源规划

## LangChain4j 可观测性组件

### 内置监控支持

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LangChain4j 可观测性示例
 */
public class LangChain4jObservability {

    private static final Logger logger = LoggerFactory.getLogger(LangChain4jObservability.class);

    /**
     * 启用内置监控的模型
     */
    public ChatModel createMonitoredModel(String apiKey) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .logRequests(true)      // 记录请求
                .logResponses(true)     // 记录响应
                .logStreamingResponses(true)  // 记录流式响应
                .maxRetries(3)         // 自动重试
                .maxTokens(1000)       // Token 限制
                .temperature(0.7)      // 温度
                .build();
    }

    /**
     * 生成带监控的聊天
     */
    public void chatWithMonitoring(ChatModel model, String userMessage) {
        logger.info("开始聊天，用户消息: {}", userMessage);

        // 生成响应（日志会自动记录）
        ChatResponse response = model.chat(userMessage);

        // 记录响应统计
        logger.info("聊天完成 - 耗时: {}ms, Token: {}", 
                    response.duration().toMillis(),
                    response.tokenUsage());

        System.out.println("AI: " + response.aiMessage().text());
    }

    public static void main(String[] args) {
        LangChain4jObservability observability = new LangChain4jObservability();
        ChatModel model = observability.createMonitoredModel(
            System.getenv("OPENAI_API_KEY")
        );

        observability.chatWithMonitoring(model, "你好，请介绍一下自己");
    }
}
```

## 指标收集

### 自定义指标收集器

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 指标收集器
 */
public class MetricsCollector {

    private static final Logger logger = LoggerFactory.getLogger(MetricsCollector.class);

    // 计数器
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong totalInputTokens = new AtomicLong(0);
    private final AtomicLong totalOutputTokens = new AtomicLong(0);

    // 直方图（响应时间分布）
    private final AtomicLong[] responseTimeHistogram = new AtomicLong[10];
    private final long[] histogramBuckets = {100, 200, 500, 1000, 2000, 5000, 10000, Long.MAX_VALUE};

    // 操作级统计
    private final ConcurrentHashMap<String, OperationStats> operationStats = new ConcurrentHashMap<>();

    /**
     * 记录请求
     */
    public void recordRequest(String operation) {
        totalRequests.incrementAndGet();

        OperationStats stats = operationStats.computeIfAbsent(operation, k -> new OperationStats());
        stats.recordRequest();

        logger.debug("记录请求: {}", operation);
    }

    /**
     * 记录成功响应
     */
    public void recordSuccess(String operation, long durationMs, int inputTokens, int outputTokens) {
        successfulRequests.incrementAndGet();
        totalInputTokens.addAndGet(inputTokens);
        totalOutputTokens.addAndGet(outputTokens);

        // 记录到直方图
        int histogramIndex = getHistogramIndex(durationMs);
        responseTimeHistogram[histogramIndex].incrementAndGet();

        // 记录操作统计
        OperationStats stats = operationStats.computeIfAbsent(operation, k -> new OperationStats());
        stats.recordSuccess(durationMs, inputTokens, outputTokens);

        logger.info("记录成功响应 - 操作: {}, 耗时: {}ms, 输入: {}, 输出: {}", 
                    operation, durationMs, inputTokens, outputTokens);
    }

    /**
     * 记录失败响应
     */
    public void recordFailure(String operation, long durationMs, String error) {
        failedRequests.incrementAndGet();

        OperationStats stats = operationStats.computeIfAbsent(operation, k -> new OperationStats());
        stats.recordFailure(durationMs);

        logger.warn("记录失败响应 - 操作: {}, 耗时: {}ms, 错误: {}", 
                    operation, durationMs, error);
    }

    /**
     * 获取指标摘要
     */
    public MetricsSummary getMetricsSummary() {
        return new MetricsSummary(
            totalRequests.get(),
            successfulRequests.get(),
            failedRequests.get(),
            totalInputTokens.get(),
            totalOutputTokens.get(),
            calculateSuccessRate(),
            calculateAverageResponseTime(),
            getResponseTimePercentiles()
        );
    }

    /**
     * 重置所有指标
     */
    public void reset() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        totalInputTokens.set(0);
        totalOutputTokens.set(0);
        
        for (AtomicLong counter : responseTimeHistogram) {
            counter.set(0);
        }
        
        operationStats.clear();
        
        logger.info("所有指标已重置");
    }

    /**
     * 获取直方图索引
     */
    private int getHistogramIndex(long durationMs) {
        for (int i = 0; i < histogramBuckets.length; i++) {
            if (durationMs < histogramBuckets[i]) {
                return i;
            }
        }
        return histogramBuckets.length - 1;
    }

    /**
     * 计算成功率
     */
    private double calculateSuccessRate() {
        long total = totalRequests.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) successfulRequests.get() / total;
    }

    /**
     * 计算平均响应时间
     */
    private double calculateAverageResponseTime() {
        long totalDuration = 0;
        long count = 0;
        
        for (OperationStats stats : operationStats.values()) {
            totalDuration += stats.getTotalDuration();
            count += stats.getRequestCount();
        }
        
        return count > 0 ? (double) totalDuration / count : 0.0;
    }

    /**
     * 获取响应时间百分位数
     */
    private Percentiles getResponseTimePercentiles() {
        return new Percentiles(
            calculatePercentile(50),
            calculatePercentile(90),
            calculatePercentile(95),
            calculatePercentile(99)
        );
    }

    /**
     * 计算百分位数
     */
    private long calculatePercentile(int percentile) {
        long[] allDurations = operationStats.values().stream()
                .mapToLong(stats -> stats.getAverageDuration())
                .sorted()
                .toArray();

        if (allDurations.length == 0) {
            return 0;
        }

        int index = (int) Math.ceil(percentile / 100.0 * allDurations.length) - 1;
        return allDurations[Math.min(index, allDurations.length - 1)];
    }

    /**
     * 操作统计
     */
    public static class OperationStats {
        private final AtomicLong requestCount = new AtomicLong(0);
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failureCount = new AtomicLong(0);
        private final AtomicLong totalDuration = new AtomicLong(0);
        private final AtomicLong totalInputTokens = new AtomicLong(0);
        private final AtomicLong totalOutputTokens = new AtomicLong(0);

        public void recordRequest() {
            requestCount.incrementAndGet();
        }

        public void recordSuccess(long durationMs, int inputTokens, int outputTokens) {
            successCount.incrementAndGet();
            totalDuration.addAndGet(durationMs);
            totalInputTokens.addAndGet(inputTokens);
            totalOutputTokens.addAndGet(outputTokens);
        }

        public void recordFailure(long durationMs) {
            failureCount.incrementAndGet();
            totalDuration.addAndGet(durationMs);
        }

        public long getRequestCount() { return requestCount.get(); }
        public long getSuccessCount() { return successCount.get(); }
        public long getFailureCount() { return failureCount.get(); }
        public long getTotalDuration() { return totalDuration.get(); }
        public long getTotalInputTokens() { return totalInputTokens.get(); }
        public long getTotalOutputTokens() { return totalOutputTokens.get(); }
        
        public long getAverageDuration() {
            long count = requestCount.get();
            return count > 0 ? totalDuration.get() / count : 0;
        }
    }

    /**
     * 指标摘要
     */
    public static class MetricsSummary {
        private final long totalRequests;
        private final long successfulRequests;
        private final long failedRequests;
        private final long totalInputTokens;
        private final long totalOutputTokens;
        private final double successRate;
        private final double averageResponseTime;
        private final Percentiles responseTimePercentiles;

        public MetricsSummary(
                long totalRequests,
                long successfulRequests,
                long failedRequests,
                long totalInputTokens,
                long totalOutputTokens,
                double successRate,
                double averageResponseTime,
                Percentiles responseTimePercentiles
        ) {
            this.totalRequests = totalRequests;
            this.successfulRequests = successfulRequests;
            this.failedRequests = failedRequests;
            this.totalInputTokens = totalInputTokens;
            this.totalOutputTokens = totalOutputTokens;
            this.successRate = successRate;
            this.averageResponseTime = averageResponseTime;
            this.responseTimePercentiles = responseTimePercentiles;
        }

        // Getters
        public long getTotalRequests() { return totalRequests; }
        public long getSuccessfulRequests() { return successfulRequests; }
        public long getFailedRequests() { return failedRequests; }
        public long getTotalTokens() { return totalInputTokens + totalOutputTokens; }
        public double getSuccessRate() { return successRate; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public Percentiles getResponseTimePercentiles() { return responseTimePercentiles; }
    }

    /**
     * 百分位数
     */
    public static class Percentiles {
        private final long p50;
        private final long p90;
        private final long p95;
        private final long p99;

        public Percentiles(long p50, long p90, long p95, long p99) {
            this.p50 = p50;
            this.p90 = p90;
            this.p95 = p95;
            this.p99 = p99;
        }

        public long getP50() { return p50; }
        public long getP90() { return p90; }
        public long getP95() { return p95; }
        public long getP99() { return p99; }
    }

    public static void main(String[] args) {
        MetricsCollector collector = new MetricsCollector();

        // 模拟一些请求
        for (int i = 0; i < 100; i++) {
            collector.recordRequest("generateChat");
            
            if (i % 10 != 0) {  // 10% 失败率
                collector.recordSuccess("generateChat", 500 + (int) (Math.random() * 1000), 100, 200);
            } else {
                collector.recordFailure("generateChat", 500, "模拟错误");
            }
        }

        // 获取指标摘要
        MetricsSummary summary = collector.getMetricsSummary();

        System.out.println("╔═══════════════════════════════════════════════════════╗");
        System.out.println("║                    指标摘要                           ║");
        System.out.println("╠═══════════════════════════════════════════════════════╣");
        System.out.println(String.format("║ 总请求数: %,8d                                    ║", summary.getTotalRequests()));
        System.out.println(String.format("║ 成功请求: %,8d                                    ║", summary.getSuccessfulRequests()));
        System.out.println(String.format("║ 失败请求: %,8d                                    ║", summary.getFailedRequests()));
        System.out.println(String.format("║ 成功率: %.2f%%                                    ║", summary.getSuccessRate() * 100));
        System.out.println(String.format("║ 平均响应时间: %.0fms                             ║", summary.getAverageResponseTime()));
        System.out.println("╠═══════════════════════════════════════════════════════╣");
        System.out.println("║ 响应时间百分位数:                                  ║");
        System.out.println(String.format("║   P50: %,5dms  (中位数)                           ║", summary.getResponseTimePercentiles().getP50()));
        System.out.println(String.format("║   P90: %,5dms                                    ║", summary.getResponseTimePercentiles().getP90()));
        System.out.println(String.format("║   P95: %,5dms                                    ║", summary.getResponseTimePercentiles().getP95()));
        System.out.println(String.format("║   P99: %,5dms                                    ║", summary.getResponseTimePercentiles().getP99()));
        System.out.println("╠═══════════════════════════════════════════════════════╣");
        System.out.println(String.format("║ 总 Token: %,8d                                    ║", summary.getTotalTokens()));
        System.out.println("╠═══════════════════════════════════════════════════════╣");
        System.out.println("║ 预估成本: $%.2f                                    ║", calculateEstimatedCost(summary.getTotalTokens()));
        System.out.println("╚═══════════════════════════════════════════════════════╝");
    }

    private static double calculateEstimatedCost(long totalTokens) {
        // 简化：假设每 1000 tokens $0.01
        return (double) totalTokens / 100000.0;
    }
}
```

## 分布式追踪

### 与 OpenTelemetry 集成

```java
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.Scope;
import io.opentelemetry.api.OpenTelemetry;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 分布式追踪示例
 */
public class DistributedTracing {

    private static final Logger logger = LoggerFactory.getLogger(DistributedTracing.class);

    /**
     * OpenTelemetry Tracer
     */
    private final Tracer tracer;

    public DistributedTracing(OpenTelemetry openTelemetry) {
        this.tracer = openTelemetry.getTracer("langchain4j");
    }

    /**
     * 带追踪的聊天服务
     */
    public interface TracedChatService {
        String chat(String userId, String message);
    }

    /**
     * 创建带追踪的服务
     */
    public TracedChatService createTracedService(ChatModel model) {
        return AiServices.builder(TracedChatService.class)
                .chatModel(model)
                .build();
    }

    /**
     * 执行带追踪的操作
     */
    public <T> T executeWithTracing(
            String operationName,
            String spanKind,
            TracedOperation<T> operation
    ) {
        Span span = tracer.spanBuilder(operationName)
                .setSpanKind(SpanKind.valueOf(spanKind))
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            logger.debug("开始追踪: {}", operationName);
            
            // 添加属性
            span.setAttribute("application.name", "langchain4j-app");
            span.setAttribute("operation.type", operationName);
            span.setAttribute("start.time", System.currentTimeMillis());

            // 执行操作
            T result = operation.execute(span);

            // 记录成功
            span.setStatus(io.opentelemetry.api.trace.StatusCode.OK);
            
            logger.debug("追踪完成: {}, 结果: {}", operationName, result);
            
            return result;
        } catch (Exception e) {
            // 记录错误
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            
            logger.error("追踪失败: {}", operationName, e);
            
            throw new RuntimeException("操作失败", e);
        } finally {
            span.end();
        }
    }

    /**
     * 追踪操作接口
     */
    public interface TracedOperation<T> {
        T execute(Span span);
    }

    /**
     * 记录 Token 使用
     */
    public void recordTokenUsage(
            Span span,
            String model,
            int inputTokens,
            int outputTokens
    ) {
        int totalTokens = inputTokens + outputTokens;

        span.setAttribute("llm.model", model);
        span.setAttribute("llm.input.tokens", inputTokens);
        span.setAttribute("llm.output.tokens", outputTokens);
        span.setAttribute("llm.total.tokens", totalTokens);
        span.setAttribute("llm.estimated.cost", calculateCost(totalTokens, model));

        span.addEvent("token.usage", System.currentTimeMillis());
    }

    /**
     * 记录性能指标
     */
    public void recordPerformanceMetrics(
            Span span,
            long durationMs,
            int tokensPerSecond
    ) {
        span.setAttribute("performance.duration.ms", durationMs);
        span.setAttribute("performance.tps", tokensPerSecond);
        span.setAttribute("performance.efficiency", calculateEfficiency(durationMs, tokensPerSecond));

        span.addEvent("performance.metrics", System.currentTimeMillis());
    }

    /**
     * 计算成本
     */
    private double calculateCost(int tokens, String model) {
        // 简化成本计算
        double pricePer1kTokens;

        if (model.contains("gpt-4")) {
            pricePer1kTokens = 0.03;
        } else {
            pricePer1kTokens = 0.002;
        }

        return (double) tokens / 1000.0 * pricePer1kTokens;
    }

    /**
     * 计算效率
     */
    private double calculateEfficiency(long durationMs, int tokensPerSecond) {
        // 效率 = tokens / (duration / 1000) = tokensPerSecond * 1000 / duration
        return (double) tokensPerSecond * 1000 / durationMs;
    }

    public static void main(String[] args) {
        // 简化：创建 OpenTelemetry
        OpenTelemetry openTelemetry = OpenTelemetry.noop();  // 实际应该使用真实的实现
        
        DistributedTracing tracing = new DistributedTracing(openTelemetry);

        // 带追踪执行操作
        String result = tracing.executeWithTracing("generateChat", "INTERNAL", span -> {
            span.setAttribute("user.id", "user123");
            span.setAttribute("model", "gpt-4o-mini");
            
            try {
                Thread.sleep(500);  // 模拟操作
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return "操作完成";
        });

        System.out.println("结果: " + result);
    }
}
```

## 健康检查

### 健康检查端点

```java
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * 健康检查
 */
public class LangChain4jHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(LangChain4jHealthIndicator.class);

    private final ChatModel model;
    private final String apiKey;

    public LangChain4jHealthIndicator(String apiKey) {
        this.apiKey = apiKey;
        this.model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .timeout(java.time.Duration.ofSeconds(10))
                .build();
    }

    @Override
    public Health health() {
        try {
            // 检查 API Key
            if (apiKey == null || apiKey.isEmpty()) {
                return Health.down()
                        .withDetail("api_key", "未配置")
                        .withDetail("model", model.getModelName());
            }

            // 测试连接
            long startTime = System.currentTimeMillis();
            model.chat("健康检查");
            long duration = System.currentTimeMillis() - startTime;

            // 检查响应时间
            if (duration > 5000) {  // 超过 5 秒
                return Health.down()
                        .withDetail("connection", "响应时间过慢")
                        .withDetail("duration_ms", duration)
                        .withDetail("model", model.getModelName());
            }

            // 健康
            return Health.up()
                        .withDetail("connection", "正常")
                        .withDetail("duration_ms", duration)
                        .withDetail("model", model.getModelName())
                        .withDetail("check_time", Instant.now())
                        .withDetail("status", "API 可用");

        } catch (Exception e) {
            logger.error("健康检查失败", e);

            return Health.down()
                        .withDetail("error", e.getMessage())
                        .withDetail("model", model.getModelName());
        }
    }

    /**
     * 详细的健康检查
     */
    public DetailedHealth detailedHealth() {
        Health basicHealth = health();

        if (basicHealth.getStatus() == Status.DOWN) {
            return new DetailedHealth(
                Status.DOWN,
                "基础健康检查失败",
                basicHealth.getDetails()
            );
        }

        // 执行详细检查
        Map<String, Object> details = new HashMap<>();
        details.putAll(basicHealth.getDetails());

        try {
            // 检查模型配置
            details.put("model.name", model.getModelName());
            details.put("model.temperature", model.temperature());
            details.put("model.max_tokens", model.maxTokens());
            details.put("model.timeout", model.timeout());

            // 测试简单生成
            long startTime = System.currentTimeMillis();
            ChatResponse response = model.chat("test");
            long duration = System.currentTimeMillis() - startTime;

            details.put("test.duration_ms", duration);
            details.put("test.tokens", response.tokenUsage());
            details.put("test.model", response.model());

            // 性能检查
            if (duration < 1000) {
                details.put("performance.rating", "excellent");
            } else if (duration < 2000) {
                details.put("performance.rating", "good");
            } else if (duration < 5000) {
                details.put("performance.rating", "fair");
            } else {
                details.put("performance.rating", "poor");
            }

            return new DetailedHealth(
                Status.UP,
                "所有检查通过",
                details
            );

        } catch (Exception e) {
            logger.error("详细健康检查失败", e);

            details.put("error", e.getMessage());
            
            return new DetailedHealth(
                Status.DOWN,
                "详细检查失败",
                details
            );
        }
    }

    /**
     * 详细健康结果
     */
    public static class DetailedHealth extends Health {
        private final String message;

        public DetailedHealth(Status status, String message, Map<String, Object> details) {
            super(status, details);
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public static void main(String[] args) {
        LangChain4jHealthIndicator health = new LangChain4jHealthIndicator(
            System.getenv("OPENAI_API_KEY")
        );

        // 基础健康检查
        Health basicHealth = health.health();
        System.out.println("基础健康检查:");
        System.out.println("状态: " + basicHealth.getStatus());
        System.out.println("详情: " + basicHealth.getDetails());
        System.out.println();

        // 详细健康检查
        DetailedHealth detailedHealth = health.detailedHealth();
        System.out.println("详细健康检查:");
        System.out.println("状态: " + detailedHealth.getStatus());
        System.out.println("消息: " + detailedHealth.getMessage());
        System.out.println("详情: " + detailedHealth.getDetails());
    }
}
```

## 告警系统

### 指标告警配置

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * 告警系统
 */
public class AlertingSystem {

    private static final Logger logger = LoggerFactory.getLogger(AlertingSystem.class);

    // 告警规则
    private final ConcurrentHashMap<String, AlertRule> alertRules = new ConcurrentHashMap<>();

    // 调度器
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * 添加告警规则
     */
    public void addAlertRule(String ruleId, AlertRule rule) {
        alertRules.put(ruleId, rule);
        logger.info("添加告警规则: {} - {}", ruleId, rule);
    }

    /**
     * 添加阈值告警
     */
    public void addThresholdAlert(
            String ruleId,
            String metricName,
            Function<Double, Double> valueSupplier,
            double threshold,
            String comparison,
            long checkIntervalMs
    ) {
        AlertRule rule = new ThresholdAlertRule(
            metricName,
            valueSupplier,
            threshold,
            comparison,
            checkIntervalMs
        );

        addAlertRule(ruleId, rule);

        // 定期检查
        scheduler.scheduleAtFixedRate(
            () -> checkAndAlert(ruleId, rule),
            0,
            checkIntervalMs,
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * 添加速率告警
     */
    public void addRateAlert(
            String ruleId,
            String metricName,
            Function<Double, Double> valueSupplier,
            int timeWindowMs,
            int threshold
    ) {
        AlertRule rule = new RateAlertRule(
            metricName,
            valueSupplier,
            timeWindowMs,
            threshold
        );

        addAlertRule(ruleId, rule);

        // 定期检查
        scheduler.scheduleAtFixedRate(
            () -> checkAndAlert(ruleId, rule),
            0,
            1000,  // 每秒检查
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * 检查并告警
     */
    private void checkAndAlert(String ruleId, AlertRule rule) {
        try {
            AlertResult result = rule.check();

            if (result.isAlertTriggered()) {
                triggerAlert(ruleId, rule, result);
            }
        } catch (Exception e) {
            logger.error("检查告警规则 {} 失败", ruleId, e);
        }
    }

    /**
     * 触发告警
     */
    private void triggerAlert(String ruleId, AlertRule rule, AlertResult result) {
        // 记录告警日志
        logger.warn("告警触发 - 规则: {}, 结果: {}", ruleId, result);

        // 发送通知（邮件、短信、Slack 等）
        sendNotification(ruleId, rule, result);

        // 记录指标
        recordAlertMetric(ruleId, result);
    }

    /**
     * 发送通知
     */
    private void sendNotification(String ruleId, AlertRule rule, AlertResult result) {
        // 实际实现中，这里会调用通知服务
        // 例如：发送邮件、Slack 消息、PagerDuty 告警等
        logger.info("发送通知: {}", result.getMessage());
    }

    /**
     * 记录告警指标
     */
    private void recordAlertMetric(String ruleId, AlertResult result) {
        // 记录到指标系统
        logger.info("告警指标: rule={}, severity={}", ruleId, result.getSeverity());
    }

    /**
     * 告警规则接口
     */
    public interface AlertRule {
        AlertResult check();
        String getDescription();
        String getSeverity();
    }

    /**
     * 告警结果
     */
    public static class AlertResult {
        private final boolean alertTriggered;
        private final String message;
        private final double currentValue;
        private final double threshold;
        private final String severity;

        public AlertResult(boolean alertTriggered, String message, double currentValue, double threshold, String severity) {
            this.alertTriggered = alertTriggered;
            this.message = message;
            this.currentValue = currentValue;
            this.threshold = threshold;
            this.severity = severity;
        }

        public boolean isAlertTriggered() { return alertTriggered; }
        public String getMessage() { return message; }
        public String getSeverity() { return severity; }
    }

    /**
     * 阈值告警规则
     */
    public static class ThresholdAlertRule implements AlertRule {
        private final String metricName;
        private final Function<Double, Double> valueSupplier;
        private final double threshold;
        private final String comparison;

        public ThresholdAlertRule(
                String metricName,
                Function<Double, Double> valueSupplier,
                double threshold,
                String comparison
        ) {
            this.metricName = metricName;
            this.valueSupplier = valueSupplier;
            this.threshold = threshold;
            this.comparison = comparison;
        }

        @Override
        public AlertResult check() {
            double currentValue = valueSupplier.apply(0.0);

            boolean alertTriggered = switch (comparison) {
                case ">" -> currentValue > threshold;
                case ">=" -> currentValue >= threshold;
                case "<" -> currentValue < threshold;
                case "<=" -> currentValue <= threshold;
                default -> false;
            };

            String message = String.format("%s = %.2f, 阈值 = %.2f (%s)", 
                    metricName, currentValue, threshold, comparison);

            return new AlertResult(
                alertTriggered,
                message,
                currentValue,
                threshold,
                alertTriggered ? "warning" : "info"
            );
        }

        @Override
        public String getDescription() {
            return String.format("阈值告警 - %s %s %.2f", comparison, metricName, threshold);
        }

        @Override
        public String getSeverity() {
            return "threshold";
        }
    }

    /**
     * 速率告警规则
     */
    public static class RateAlertRule implements AlertRule {
        private final String metricName;
        private final Function<Double, Double> valueSupplier;
        private final int timeWindowMs;
        private final int threshold;

        public RateAlertRule(
                String metricName,
                Function<Double, Double> valueSupplier,
                int timeWindowMs,
                int threshold
        ) {
            this.metricName = metricName;
            this.valueSupplier = valueSupplier;
            this.timeWindowMs = timeWindowMs;
            this.threshold = threshold;
        }

        @Override
        public AlertResult check() {
            // 简化：实际实现应该记录时间窗口内的值
            double currentValue = valueSupplier.apply(0.0);
            double rate = currentValue;  // 简化：应该是 值/时间

            boolean alertTriggered = rate > threshold;

            String message = String.format("%s = %.2f/min, 阈值 = %d/min", 
                    metricName, rate, threshold);

            return new AlertResult(
                alertTriggered,
                message,
                rate,
                threshold,
                alertTriggered ? "critical" : "info"
            );
        }

        @Override
        public String getDescription() {
            return String.format("速率告警 - %s 每 %d 分钟 %d 次", metricName, timeWindowMs/60000, threshold);
        }

        @Override
        public String getSeverity() {
            return "rate";
        }
    }

    /**
     * 关闭调度器
     */
    public void shutdown() {
        scheduler.shutdown();
        logger.info("告警系统已关闭");
    }

    public static void main(String[] args) {
        AlertingSystem alerting = new AlertingSystem();

        // 添加响应时间告警
        alerting.addThresholdAlert(
            "response_time_high",
            "平均响应时间",
            () -> System.currentTimeMillis(),  // 应该从指标系统获取
            5000,  // 5 秒阈值
            ">",
            10000  // 每 10 秒检查
        );

        // 添加错误率告警
        alerting.addThresholdAlert(
            "error_rate_high",
            "错误率",
            () -> 0.05,  // 应该从指标系统获取
            0.1,   // 10% 阈值
            ">",
            30000  // 每 30 秒检查
        );

        // 保持运行
        Scanner scanner = new Scanner(System.in);
        System.out.println("告警系统已启动，按回车键停止");
        scanner.nextLine();
        
        alerting.shutdown();
        scanner.close();
    }
}
```

## 测试代码示例

```java
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 可观测性测试
 */
class ObservabilityTest {

    private MetricsCollector metrics;

    @BeforeEach
    void setUp() {
        this.metrics = new MetricsCollector();
    }

    @Test
    void should_record_success_metrics() {
        metrics.recordRequest("test_operation");
        metrics.recordSuccess("test_operation", 500, 100, 200);

        MetricsSummary summary = metrics.getMetricsSummary();

        assertEquals(1, summary.getTotalRequests());
        assertEquals(1, summary.getSuccessfulRequests());
        assertEquals(300, summary.getTotalTokens());
        assertTrue(summary.getSuccessRate() > 0.99);
    }

    @Test
    void should_record_failure_metrics() {
        metrics.recordRequest("test_operation");
        metrics.recordFailure("test_operation", 500, "模拟错误");

        MetricsSummary summary = metrics.getMetricsSummary();

        assertEquals(1, summary.getTotalRequests());
        assertEquals(1, summary.getFailedRequests());
        assertEquals(0, summary.getSuccessCount());
        assertTrue(summary.getSuccessRate() < 0.01);
    }

    @Test
    void should_calculate_percentiles() {
        // 添加一些响应时间
        metrics.recordRequest("test");
        metrics.recordSuccess("test", 100, 50, 50);
        metrics.recordSuccess("test", 200, 100, 100);
        metrics.recordSuccess("test", 300, 150, 150);
        metrics.recordSuccess("test", 400, 200, 200);
        metrics.recordSuccess("test", 500, 250, 250);
        metrics.recordSuccess("test", 600, 300, 300);
        metrics.recordSuccess("test", 700, 350, 350);
        metrics.recordSuccess("test", 800, 400, 400);
        metrics.recordSuccess("test", 900, 450, 450);
        metrics.recordSuccess("test", 1000, 500, 500);
        metrics.recordSuccess("test", 2000, 1000, 1000);
        metrics.recordSuccess("test", 3000, 1500, 1500);
        metrics.recordSuccess("test", 5000, 2500, 2500);

        MetricsSummary summary = metrics.getMetricsSummary();
        Percentiles percentiles = summary.getResponseTimePercentiles();

        assertNotNull(percentiles);
        assertTrue(percentiles.getP50() > 0);
        assertTrue(percentiles.getP90() > percentiles.getP50());
        assertTrue(percentiles.getP95() > percentiles.getP90());
        assertTrue(percentiles.getP99() > percentiles.getP95());
    }

    @Test
    void should_reset_metrics() {
        metrics.recordRequest("test");
        metrics.recordSuccess("test", 500, 100, 200);

        metrics.reset();

        MetricsSummary summary = metrics.getMetricsSummary();

        assertEquals(0, summary.getTotalRequests());
        assertEquals(0, summary.getTotalRequests());
    }
}
```

## 实践练习

### 练习 1：实现实时监控面板

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 实时监控面板
 */
public class RealTimeMonitoringDashboard {

    private static final Logger logger = LoggerFactory.getLogger(RealTimeMonitoringDashboard.class);

    private final MetricsCollector metrics;
    private final ScheduledExecutorService scheduler;

    public RealTimeMonitoringDashboard(MetricsCollector metrics) {
        this.metrics = metrics;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * 启动监控
     */
    public void start() {
        logger.info("启动实时监控面板");

        // 每秒更新一次显示
        scheduler.scheduleAtFixedRate(this::updateDisplay, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * 停止监控
     */
    public void stop() {
        scheduler.shutdown();
        logger.info("监控面板已停止");
    }

    /**
     * 更新显示
     */
    private void updateDisplay() {
        MetricsSummary summary = metrics.getMetricsSummary();

        // 清屏
        System.out.print("\033[H");  // ANSI 清屏

        // 显示标题
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║            实时监控面板                          ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");

        // 显示指标
        displaySection("请求统计", getRequestsStats(summary));
        displaySection("性能指标", getPerformanceStats(summary));
        displaySection("Token 使用", getTokenStats(summary));
        displaySection("成本估算", getCostStats(summary));

        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  更新时间: " + LocalDateTime.now().format(DateTimeFormatter.of("HH:mm:ss")));
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
    }

    /**
     * 显示部分
     */
    private void displaySection(String title, String content) {
        System.out.println("║ " + String.format("%-18s", title));
        System.out.println("╠─────────────────────────────────────────────────────────────────╣");
        System.out.println("║ " + content);
        System.out.println("╠─────────────────────────────────────────────────────────────────╣");
    }

    /**
     * 获取请求统计
     */
    private String getRequestsStats(MetricsSummary summary) {
        return String.format(
            "总请求: %,5d | 成功: %,5d | 失败: %,5d | 成功率: %.1f%%",
            summary.getTotalRequests(),
            summary.getSuccessfulRequests(),
            summary.getFailedRequests(),
            summary.getSuccessRate() * 100
        );
    }

    /**
     * 获取性能指标
     */
    private String getPerformanceStats(MetricsSummary summary) {
        double avgResponseTime = summary.getAverageResponseTime();
        Percentiles percentiles = summary.getResponseTimePercentiles();

        return String.format(
            "平均: %.0fms | P50: %,4dms | P90: %,4dms | P95: %,4dms | P99: %,4dms",
            avgResponseTime,
            percentiles.getP50(),
            percentiles.getP90(),
            percentiles.getP95(),
            percentiles.getP99()
        );
    }

    /**
     * 获取 Token 统计
     */
    private String getTokenStats(MetricsSummary summary) {
        return String.format(
            "总 Token: %,8d | 输入: %,8d | 输出: %,8d",
            summary.getTotalTokens(),
            summary.getTotalInputTokens(),
            summary.getTotalOutputTokens()
        );
    }

    /**
     * 获取成本估算
     */
    private String getCostStats(MetricsSummary summary) {
        double estimatedCost = calculateEstimatedCost(summary.getTotalTokens());

        return String.format(
            "总成本: $%.2f | 平均每请求: $%.4f",
            estimatedCost,
            summary.getTotalRequests() > 0 ? estimatedCost / summary.getTotalRequests() : 0
        );
    }

    /**
     * 计算估算成本
     */
    private double calculateEstimatedCost(long totalTokens) {
        // 简化：假设每 1000 tokens $0.01
        return (double) totalTokens / 100000.0;
    }

    public static void main(String[] args) throws InterruptedException {
        MetricsCollector metrics = new MetricsCollector();
        RealTimeMonitoringDashboard dashboard = new RealTimeMonitoringDashboard(metrics);

        // 模拟一些请求
        for (int i = 0; i < 50; i++) {
            metrics.recordRequest("generateChat");
            
            if (i % 10 != 0) {
                metrics.recordSuccess("generateChat", 500 + (int) (Math.random() * 1000), 100, 200);
            } else {
                metrics.recordFailure("generateChat", 500, "模拟错误");
            }
            
            Thread.sleep(100);  // 模拟请求间隔
        }

        // 启动监控
        dashboard.start();

        System.out.println("监控面板已启动，按 Ctrl+C 停止");

        // 保持运行
        Thread.currentThread().join();
    }
}
```

### 练习 2：实现 SLA 监控

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

/**
 * SLA (Service Level Agreement) 监控
 */
public class SlaMonitor {

    private static final Logger logger = LoggerFactory.getLogger(SlaMonitor.class);

    // SLA 定义
    private final double maxResponseTimeMs;
    private final double maxErrorRate;
    private final double maxTokenCost;
    private final long monitoringWindowMs;

    // SLA 违规记录
    private final List<SlaViolation> violations;

    public SlaMonitor(
            double maxResponseTimeMs,
            double maxErrorRate,
            double maxTokenCost,
            long monitoringWindowMs
    ) {
        this.maxResponseTimeMs = maxResponseTimeMs;
        this.maxErrorRate = maxErrorRate;
        this.maxTokenCost = maxTokenCost;
        this.monitoringWindowMs = monitoringWindowMs;
        this.violations = new ArrayList<>();
    }

    /**
     * 检查 SLA 合规性
     */
    public SlaComplianceReport checkCompliance(MetricsSummary metrics) {
        List<SlaViolation> newViolations = new ArrayList<>();

        // 检查响应时间
        if (metrics.getAverageResponseTime() > maxResponseTimeMs) {
            newViolations.add(new SlaViolation(
                "response_time",
                "平均响应时间",
                metrics.getAverageResponseTime(),
                maxResponseTimeMs,
                maxResponseTimeMs / metrics.getAverageResponseTime(),
                "高"
            ));
        }

        // 检查错误率
        double errorRate = 1.0 - metrics.getSuccessRate();
        if (errorRate > maxErrorRate) {
            newViolations.add(new SlaViolation(
                "error_rate",
                "错误率",
                errorRate * 100,
                maxErrorRate * 100,
                errorRate / maxErrorRate,
                "高"
            ));
        }

        // 检查 P95 响应时间
        if (metrics.getResponseTimePercentiles().getP95() > maxResponseTimeMs) {
            newViolations.add(new SlaViolation(
                "p95_response_time",
                "P95 响应时间",
                metrics.getResponseTimePercentiles().getP95(),
                maxResponseTimeMs,
                metrics.getResponseTimePercentiles().getP95() / maxResponseTimeMs,
                "中"
            ));
        }

        return new SlaComplianceReport(newViolations);
    }

    /**
     * 获取 SLA 合规分数
     */
    public double getComplianceScore(SlaComplianceReport report) {
        if (report.getViolations().isEmpty()) {
            return 100.0;  // 完全合规
        }

        // 计算违规影响
        double totalImpact = 0;
        for (SlaViolation violation : report.getViolations()) {
            totalImpact += violation.getSeverity().equals("高") ? 10 : 5;
        }

        return Math.max(0, 100 - totalImpact);
    }

    /**
     * 获取违规详情
     */
    public List<SlaViolation> getViolations() {
        return new ArrayList<>(violations);
    }

    /**
     * 清空违规记录
     */
    public void clearViolations() {
        violations.clear();
        logger.info("SLA 违规记录已清空");
    }

    /**
     * SLA 违规
     */
    public static class SlaViolation {
        private final String metric;
        private final String name;
        private final double actualValue;
        private final double threshold;
        private final double ratio;
        private final String severity;

        public SlaViolation(String metric, String name, double actualValue, double threshold, double ratio, String severity) {
            this.metric = metric;
            this.name = name;
            this.actualValue = actualValue;
            this.threshold = threshold;
            this.ratio = ratio;
            this.severity = severity;
        }

        public String getMetric() { return metric; }
        public String getName() { return name; }
        public double getActualValue() { return actualValue; }
        public double getThreshold() { return threshold; }
        public double getRatio() { return ratio; }
        public String getSeverity() { return severity; }
    }

    /**
     * SLA 合规报告
     */
    public static class SlaComplianceReport {
        private final List<SlaViolation> violations;

        public SlaComplianceReport(List<SlaViolation> violations) {
            this.violations = violations;
        }

        public List<SlaViolation> getViolations() {
            return violations;
        }

        public boolean isCompliant() {
            return violations.isEmpty();
        }

        public int getViolationCount() {
            return violations.size();
        }
    }

    public static void main(String[] args) {
        SlaMonitor sla = new SlaMonitor(
            3000,    // 最大响应时间 3 秒
            0.05,     // 最大错误率 5%
            10.0,     // 最大成本 $10
            60000    // 监控窗口 1 分钟
        );

        MetricsCollector metrics = new MetricsCollector();

        // 模拟一些请求
        for (int i = 0; i < 100; i++) {
            metrics.recordRequest("generateChat");
            metrics.recordSuccess("generateChat", 500 + (int) (Math.random() * 2000), 100, 200);
        }

        MetricsSummary summary = metrics.getMetricsSummary();
        SlaComplianceReport report = sla.checkCompliance(summary);

        // 显示报告
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║              SLA 合规报告                           ║");
        System.out.println("╠═══════════════════════════════════════════════════════════╣");
        System.out.println("║ 合规状态: " + (report.isCompliant() ? "✅ 合规" : "❌ 违规"));
        System.out.println("║ 违规数量: " + report.getViolationCount());
        System.out.println("║ 合规分数: " + String.format("%.1f/100", sla.getComplianceScore(report)));
        System.out.println("╠═════════════════════════════════════════════════════════╣");

        if (!report.getViolations().isEmpty()) {
            System.out.println("║ 违规详情:                                             ║");
            System.out.println("╠═══════════════════════════════════════════════════════════╣");

            for (SlaViolation violation : report.getViolations()) {
                System.out.printf("║ %s: %.2f (阈值: %.2f, 比例: %.2fx) [%s] %s%n",
                        violation.getName(),
                        violation.getActualValue(),
                        violation.getThreshold(),
                        violation.getRatio(),
                        violation.getSeverity(),
                        violation.getSeverity()
                );
            }

            System.out.println("╠═════════════════════════════════════════════════════════╣");
        }

        System.out.println("╚═════════════════════════════════════════════════════════════════╝");
    }
}
```

## 总结

### 本章要点

1. **可观测性概念**
   - 日志、指标、追踪三支柱
   - 从外部观察系统内部状态
   - 对于 LLM 应用尤其重要

2. **LangChain4j 可观测性**
   - 内置监控支持
   - 请求和响应日志
   - Token 使用追踪
   - 性能指标收集

3. **指标收集**
   - 请求/成功/失败计数
   - 响应时间分布
   - Token 使用统计
   - 成本计算

4. **分布式追踪**
   - OpenTelemetry 集成
   - 请求链路追踪
   - 跨服务上下文传播

5. **监控和告警**
   - 健康检查
   - 实时监控面板
   - SLA 监控
   - 智能告警

### 下一步

在下一章节中，我们将学习：
- 测试和评估策略
- 集成测试框架
- 性能基准测试
- A/B 测试
- 错误注入测试

### 常见问题

**Q1：可观测性与日志有什么区别？**

A：
- **日志** - 离散的事件记录，主要用于调试
- **可观测性** - 结构化的指标和追踪，用于监控和分析
- 可观测性包括日志，但更加全面和系统化

**Q2：如何选择监控哪些指标？**

A：选择标准：
1. 业务关键指标（响应时间、错误率）
2. 资源指标（Token 使用、API 调用次数）
3. 性能指标（吞吐量、延迟）
4. 成本指标（预算、实际花费）

**Q3：如何处理监控数据？**

A：
1. 实时计算和展示
2. 存储到时序数据库（如 Prometheus）
3. 定期聚合和归档
4. 配置数据保留策略

**Q4：如何设置告警阈值？**

A：
1. 基于 SLA 要求设置
2. 使用历史数据确定合理阈值
3. 配置分级告警（info/warning/critical）
4. 避免告警疲劳（限流、冷却时间）

**Q5：如何保护监控系统的性能？**

A：
1. 使用异步日志和指标收集
2. 考虑采样策略（如 10% 采样）
3. 避免在热路径上执行复杂计算
4. 使用高效的数据结构和序列化

## 参考资料

- [LangChain4j 可观测性文档](https://docs.langchain4j.dev/tutorials/observability)
- [OpenTelemetry 官方文档](https://opentelemetry.io/)
- [Prometheus 官方文档](https://prometheus.io/)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
