---
title: '安全护栏'
description: '学习 LangChain4j 的 安全护栏 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 22 - 安全护栏

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 理解 LangChain4j 中安全护栏的概念和重要性
- 掌握 `@ModerateModel` 注解的使用
- 学会实现内容审核和安全检查
- 理解输出验证和过滤
- 掌握速率限制和配额控制
- 实现一个完整的安全护栏解决方案

## 前置知识

- 完成《01 - LangChain4j 简介》章节
- 完成《02 - 你的第一个 Chat 应用》章节

## 核心概念

### 什么是安全护栏？

**安全护栏**（Guardrails）是指在 LLM 应用中实施的安全措施，用于：
- 控制模型的输入和输出
- 防止有害或不当内容
- 确保应用符合政策和法规
- 保护用户免受不良信息影响

**类比理解：**
- **无护栏的 LLM** = 高速公路上没有护栏，可能偏离路线
- **有护栏的 LLM** = 铁路两侧有护栏，确保安全行驶

### 为什么需要安全护栏？

**安全挑战：**

1. **有害内容**
   - 暴力、仇恨言论
   - 非法内容
   - 不当信息

2. **隐私问题**
   - 泄露敏感信息
   - 个人身份信息（PII）
   - 商业机密

3. **合规要求**
   - GDPR 等数据保护法规
   - 行业标准（医疗、金融）
   - 内容分级要求

4. **用户体验**
   - 不当内容影响品牌形象
   - 破坏用户信任
   - 导致负面的社会影响

## 基础安全护栏

### 内容审核

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 内容审核示例
 */
public class ContentModerationExample {

    private static final Logger logger = LoggerFactory.getLogger(ContentModerationExample.class);

    /**
     * 审核结果
     */
    public static class ModerationResult {
        private final boolean isAllowed;
        private final String reason;
        private final ModerationCategory category;
        private final double confidence;

        public ModerationResult(boolean isAllowed, String reason, 
                             ModerationCategory category, double confidence) {
            this.isAllowed = isAllowed;
            this.reason = reason;
            this.category = category;
            this.confidence = confidence;
        }

        public boolean isAllowed() { return isAllowed; }
        public String getReason() { return reason; }
        public ModerationCategory getCategory() { return category; }
        public double getConfidence() { return confidence; }
    }

    /**
     * 审核类别
     */
    public enum ModerationCategory {
        SAFE("安全"),
        VIOLENCE("暴力"),
        HATE("仇恨"),
        SEXUAL("色情"),
        ILLEGAL("非法"),
        INAPPROPRIATE("不当"),
        PRIVACY("隐私"),
        UNKNOWN("未知");

        private final String description;

        ModerationCategory(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }

    /**
     * 审核服务
     */
    @AiService
    public interface ContentModerationService {
        /**
         * 审核文本内容
         */
        ModerationResult moderateText(String text);
    }

    /**
     * 创建审核服务
     */
    public static ContentModerationService createModerationService(String apiKey) {
        return AiServices.builder(ContentModerationService.class)
                .chatModel(OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("gpt-4o-mini")
                        .build())
                .systemMessageProvider(chatMemoryId -> 
                    "你是一个内容审核专家。" +
                    "你的任务是分析给定的文本内容，判断其是否安全。" +
                    "请检查以下方面：" +
                    "1. 是否包含暴力、仇恨、色情等不当内容" +
                    "2. 是否包含非法内容（如毒品、武器等）" +
                    "3. 是否包含不当言论或冒犯性语言" +
                    "4. 是否包含个人隐私信息（如电话号码、地址等）" +
                    "如果内容安全，返回 isAllowed=true，category=SAFE" +
                    "如果内容不安全，返回 isAllowed=false，" +
                    "并指定具体的违规类别（VIOLENCE、HATE、SEXUAL、ILLEGAL、INAPPROPRIATE、PRIVACY之一）" +
                    "对于每个判断，提供 confidence（0.0 到 1.0）表示确信程度" +
                    "同时提供 reason 简要说明违规原因。"
                )
                .build();
    }

    /**
     * 带安全护栏的聊天
     */
    public static String chatWithGuardrails(
            ContentModerationService moderationService,
            ChatModel model,
            String message
    ) {
        // 审核输入
        ModerationResult moderationResult = moderationService.moderateText(message);

        if (!moderationResult.isAllowed()) {
            logger.warn("消息未通过审核: {}", moderationResult.getReason());
            return "抱歉，您的消息违反了我们的内容政策。";
        }

        // 消息安全，正常处理
        logger.info("消息通过审核: {}", message);
        return model.chat(message);
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("OPENAI_API_KEY");

        // 创建审核服务
        ContentModerationService moderationService = createModerationService(apiKey);

        // 创建聊天模型
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .build();

        // 测试安全消息
        String safeMessage = "你好，请问今天天气如何？";
        String response1 = chatWithGuardrails(moderationService, model, safeMessage);
        System.out.println("用户: " + safeMessage);
        System.out.println("AI: " + response1);
        System.out.println();

        // 测试不安全消息
        String unsafeMessage = "我讨厌所有人，我要报复他们！";
        String response2 = chatWithGuardrails(moderationService, model, unsafeMessage);
        System.out.println("用户: " + unsafeMessage);
        System.out.println("AI: " + response2);
    }
}
```

## 输入验证

### 输入过滤

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 输入验证示例
 */
public class InputValidationExample {

    private static final Logger logger = LoggerFactory.getLogger(InputValidationExample.class);

    /**
     * 验证规则
     */
    public static class ValidationRule {
        private final String name;
        private final Pattern pattern;
        private final String description;

        public ValidationRule(String name, String pattern, String description) {
            this.name = name;
            this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            this.description = description;
        }

        public String getName() { return name; }
        public Pattern getPattern() { return pattern; }
        public String getDescription() { return description; }
    }

    /**
     * 验证结果
     */
    public static class ValidationResult {
        private final boolean isValid;
        private final String message;
        private final List<String> violatedRules;

        public ValidationResult(boolean isValid, String message, List<String> violatedRules) {
            this.isValid = isValid;
            this.message = message;
            this.violatedRules = violatedRules;
        }

        public boolean isValid() { return isValid; }
        public String getMessage() { return message; }
        public List<String> getViolatedRules() { return violatedRules; }
    }

    /**
     * 验证配置
     */
    public static class ValidationConfig {
        private final int maxLength;
        private final int minLength;
        private final List<ValidationRule> rules;
        private final boolean checkSensitiveData;

        public ValidationConfig(int maxLength, int minLength, 
                           List<ValidationRule> rules, boolean checkSensitiveData) {
            this.maxLength = maxLength;
            this.minLength = minLength;
            this.rules = rules;
            this.checkSensitiveData = checkSensitiveData;
        }

        public int getMaxLength() { return maxLength; }
        public int getMinLength() { return minLength; }
        public List<ValidationRule> getRules() { return rules; }
        public boolean isCheckSensitiveData() { return checkSensitiveData; }
    }

    /**
     * 验证服务
     */
    @AiService
    public interface InputValidationService {
        /**
         * 验证输入
         */
        ValidationResult validateInput(String input);
    }

    /**
     * 创建默认验证配置
     */
    public static ValidationConfig createDefaultValidationConfig() {
        List<ValidationRule> rules = new ArrayList<>();

        // 长度规则
        rules.add(new ValidationRule(
            "max_length",
            "^.{0,1000}$",
            "输入不能超过 1000 个字符"
        ));

        // 内容规则
        rules.add(new ValidationRule(
            "no_email",
            "[\\w\\.\\-]+@[\\w\\.\\-]+\\.[a-zA-Z]{2,}",
            "请勿在输入中提供邮箱地址"
        ));

        rules.add(new ValidationRule(
            "no_phone",
            "\\d{11}",
            "请勿在输入中提供电话号码"
        ));

        rules.add(new ValidationRule(
            "no_url",
            "https?://[\\w\\-]+(\\.[\\w\\-]+)+[/\\w\\-?=%&]*",
            "请勿在输入中提供 URL"
        ));

        // 不当内容规则
        rules.add(new ValidationRule(
            "no_profanity",
            "(暴力|仇恨|色情|毒品|赌博)",
            "输入中包含不当内容"
        ));

        return new ValidationConfig(
                1000,          // 最大长度
                1,             // 最小长度
                rules,
                true           // 检查敏感数据
        );
    }

    /**
     * 验证输入
     */
    public static ValidationResult validateInput(String input, ValidationConfig config) {
        List<String> violatedRules = new ArrayList<>();

        // 检查长度
        if (input.length() > config.getMaxLength()) {
            violatedRules.add("max_length");
        } else if (input.length() < config.getMinLength()) {
            violatedRules.add("min_length");
        }

        // 检查各项规则
        for (ValidationRule rule : config.getRules()) {
            if (rule.getPattern().matcher(input).find()) {
                violatedRules.add(rule.getName());
            }
        }

        // 检查敏感数据
        if (config.isCheckSensitiveData()) {
            if (containsSensitiveData(input)) {
                violatedRules.add("sensitive_data");
            }
        }

        // 生成结果
        if (violatedRules.isEmpty()) {
            return new ValidationResult(true, "输入有效", List.of());
        } else {
            String message = String.format("输入违反了以下规则: %s", 
                        String.join(", ", violatedRules));
            return new ValidationResult(false, message, violatedRules);
        }
    }

    /**
     * 检查敏感数据
     */
    private static boolean containsSensitiveData(String input) {
        // 简化实现：实际应该使用更复杂的规则
        String[] sensitiveKeywords = {
            "身份证", "护照", "银行卡", "密码", "信用卡",
            "账号", "密码", "支付", "转账", "地址", "电话"
        };

        String lowerInput = input.toLowerCase();
        for (String keyword : sensitiveKeywords) {
            if (lowerInput.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 创建验证服务
     */
    public static InputValidationService createValidationService(String apiKey, ValidationConfig config) {
        return AiServices.builder(InputValidationService.class)
                .chatModel(OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("gpt-4o-mini")
                        .build())
                .systemMessageProvider(chatMemoryId -> 
                    "你是一个输入验证专家。" +
                    "你的任务是分析用户的输入，判断其是否符合指定的验证规则。" +
                    "请检查：" +
                    "1. 输入长度是否在允许范围内" +
                    "2. 输入是否包含邮箱地址、电话号码、URL 等敏感信息" +
                    "3. 输入是否包含暴力、仇恨、色情等不当内容" +
                    "4. 输入是否违反任何其他指定的规则" +
                    "如果输入有效，返回 isValid=true" +
                    "如果输入无效，返回 isValid=false，" +
                    "并列出所有被违反的规则名称。" +
                    "规则包括: " + String.join(", ", 
                        config.getRules().stream()
                                .map(ValidationRule::getName)
                                .toArray(String[]::new)) +
                    "。" +
                    "提供 message 简要说明验证结果。"
                )
                .build();
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("OPENAI_API_KEY");

        // 创建验证配置
        ValidationConfig config = createDefaultValidationConfig();

        // 创建验证服务
        InputValidationService validationService = createValidationService(apiKey, config);

        // 测试有效输入
        String validInput = "你好，我想了解一下天气预报";
        ValidationResult result1 = validationService.validateInput(validInput);
        System.out.println("输入: " + validInput);
        System.out.println("验证结果: " + result1.isValid());
        System.out.println("消息: " + result1.getMessage());
        System.out.println();

        // 测试无效输入
        String invalidInput = "我的邮箱是 test@example.com，电话 13800138000，我想报复所有人！";
        ValidationResult result2 = validationService.validateInput(invalidInput);
        System.out.println("输入: " + invalidInput);
        System.out.println("验证结果: " + result2.isValid());
        System.out.println("违规规则: " + String.join(", ", result2.getViolatedRules()));
        System.out.println("消息: " + result2.getMessage());
    }
}
```

## 输出过滤

### 输出审核

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.data.message.AiMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 输出过滤示例
 */
public class OutputFilteringExample {

    private static final Logger logger = LoggerFactory.getLogger(OutputFilteringExample.class);

    /**
     * 过滤规则
     */
    public static class FilterRule {
        private final String name;
        private final Pattern pattern;
        private final String replacement;
        private final String description;

        public FilterRule(String name, String pattern, String replacement, String description) {
            this.name = name;
            this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            this.replacement = replacement;
            this.description = description;
        }

        public String getName() { return name; }
        public Pattern getPattern() { return pattern; }
        public String getReplacement() { return replacement; }
        public String getDescription() { return description; }
    }

    /**
     * 过滤结果
     */
    public static class FilterResult {
        private final String originalOutput;
        private final String filteredOutput;
        private final List<FilterRule> appliedRules;

        public FilterResult(String originalOutput, String filteredOutput, List<FilterRule> appliedRules) {
            this.originalOutput = originalOutput;
            this.filteredOutput = filteredOutput;
            this.appliedRules = appliedRules;
        }

        public String getOriginalOutput() { return originalOutput; }
        public String getFilteredOutput() { return filteredOutput; }
        public List<FilterRule> getAppliedRules() { return appliedRules; }
    }

    /**
     * 过滤配置
     */
    public static class FilterConfig {
        private final List<FilterRule> filterRules;
        private final boolean applyAllRules;
        private final boolean logFilteredContent;

        public FilterConfig(List<FilterRule> filterRules, boolean applyAllRules, boolean logFilteredContent) {
            this.filterRules = filterRules;
            this.applyAllRules = applyAllRules;
            this.logFilteredContent = logFilteredContent;
        }

        public List<FilterRule> getFilterRules() { return filterRules; }
        public boolean isApplyAllRules() { return applyAllRules; }
        public boolean isLogFilteredContent() { return logFilteredContent; }
    }

    /**
     * 创建默认过滤配置
     */
    public static FilterConfig createDefaultFilterConfig() {
        List<FilterRule> rules = new ArrayList<>();

        // 个人信息过滤
        rules.add(new FilterRule(
            "remove_email",
            "\\b[\\w\\.\\-]+@[\\w\\.\\-]+\\.[a-zA-Z]{2,}\\b",
            "[已过滤邮箱地址]",
            "移除个人邮箱地址"
        ));

        rules.add(new FilterRule(
            "remove_phone",
            "\\d{11}",
            "[已过滤电话号码]",
            "移除个人电话号码"
        ));

        // 不当内容过滤
        rules.add(new FilterRule(
            "remove_profanity",
            "暴力|仇恨|色情|毒品|赌博",
            "[已过滤不当内容]",
            "移除不当言论"
        ));

        // API 密钥过滤
        rules.add(new FilterRule(
            "remove_api_key",
            "sk-[a-zA-Z0-9]{48}",
            "[已过滤 API Key]",
            "移除敏感凭证"
        ));

        return new FilterConfig(
                rules,
                true,   // 应用所有过滤规则
                true    // 记录过滤内容
        );
    }

    /**
     * 过滤输出
     */
    public static FilterResult filterOutput(String output, FilterConfig config) {
        String filteredOutput = output;
        List<FilterRule> appliedRules = new ArrayList<>();

        // 应用每个过滤规则
        for (FilterRule rule : config.getFilterRules()) {
            String previousOutput = filteredOutput;
            filteredOutput = rule.getPattern().matcher(filteredOutput).replaceAll(rule.getReplacement());

            if (!previousOutput.equals(filteredOutput)) {
                appliedRules.add(rule);
                if (config.isLogFilteredContent()) {
                    logger.info("应用过滤规则 '{}': {}", rule.getName(), rule.getDescription());
                }
            }
        }

        return new FilterResult(output, filteredOutput, appliedRules);
    }

    /**
     * 带过滤的生成
     */
    public static String generateWithFilter(
            ChatModel model,
            FilterConfig filterConfig,
            String prompt
    ) {
        // 生成原始响应
        String rawResponse = model.chat(prompt);

        // 过滤响应
        FilterResult filterResult = filterOutput(rawResponse, filterConfig);

        if (!filterResult.getAppliedRules().isEmpty()) {
            logger.info("响应已应用 {} 个过滤规则", filterResult.getAppliedRules().size());
        }

        return filterResult.getFilteredOutput();
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("OPENAI_API_KEY");

        // 创建模型
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .build();

        // 创建过滤配置
        FilterConfig filterConfig = createDefaultFilterConfig();

        // 测试过滤
        String prompt = "我的邮箱是 user@example.com，电话 13800138000，API Key 是 sk-1234567890，" +
                     "你可以看到我的银行卡号吗？";

        System.out.println("╔═════════════════════════════════════════════════════╗");
        System.out.println("║              输出过滤示例                              ║");
        System.out.println("╠═════════════════════════════════════════════════════╣");
        System.out.println("║ 提示词:                                                 ║");
        System.out.println("║ " + prompt);
        System.out.println("╠═════════════════════════════════════════════════════╣");

        String filteredResponse = generateWithFilter(model, filterConfig, prompt);

        System.out.println("║ 原始响应:                                               ║");
        System.out.println("║ " + model.chat(prompt).substring(0, Math.min(100, model.chat(prompt).length())));
        System.out.println("╠─────────────────────────────────────────────────────╣");
        System.out.println("║ 过滤后响应:                                               ║");
        System.out.println("║ " + filteredResponse.substring(0, Math.min(100, filteredResponse.length())));
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
    }
}
```

## 速率限制

### 请求限流

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 速率限制示例
 */
public class RateLimitingExample {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingExample.class);

    /**
     * 速率限制配置
     */
    public static class RateLimitConfig {
        private final int maxRequestsPerMinute;
        private final int maxRequestsPerHour;
        private final int maxRequestsPerDay;
        private final int burstLimit;

        public RateLimitConfig(int maxRequestsPerMinute, int maxRequestsPerHour,
                            int maxRequestsPerDay, int burstLimit) {
            this.maxRequestsPerMinute = maxRequestsPerMinute;
            this.maxRequestsPerHour = maxRequestsPerHour;
            this.maxRequestsPerDay = maxRequestsPerDay;
            this.burstLimit = burstLimit;
        }

        public int getMaxRequestsPerMinute() { return maxRequestsPerMinute; }
        public int getMaxRequestsPerHour() { return maxRequestsPerHour; }
        public int getMaxRequestsPerDay() { return maxRequestsPerDay; }
        public int getBurstLimit() { return burstLimit; }
    }

    /**
     * 速率限制状态
     */
    public static class RateLimitState {
        private final String userId;
        private final AtomicLong requestsInLastMinute;
        private final AtomicLong requestsInLastHour;
        private final AtomicLong requestsInLastDay;
        private volatile long lastResetTime;
        private final String lastRateLimitReset;

        public RateLimitState(String userId) {
            this.userId = userId;
            this.requestsInLastMinute = new AtomicLong(0);
            this.requestsInLastHour = new AtomicLong(0);
            this.requestsInLastDay = new AtomicLong(0);
            this.lastResetTime = System.currentTimeMillis();
            this.lastRateLimitReset = LocalDateTime.now().toString();
        }

        public String getUserId() { return userId; }
        public long getRequestsInLastMinute() { return requestsInLastMinute.get(); }
        public long getRequestsInLastHour() { return requestsInLastHour.get(); }
        public long getRequestsInLastDay() { return requestsInLastDay.get(); }
        public String getLastRateLimitReset() { return lastRateLimitReset; }
    }

    /**
     * 速率限制管理器
     */
    public static class RateLimiter {
        private final Map<String, RateLimitState> userStates;
        private final RateLimitConfig config;

        public RateLimiter(RateLimitConfig config) {
            this.userStates = new ConcurrentHashMap<>();
            this.config = config;
        }

        /**
         * 检查速率限制
         */
        public RateLimitResult checkRateLimit(String userId) {
            RateLimitState state = userStates.computeIfAbsent(userId, RateLimitState::new);

            // 检查突发限制
            if (state.getRequestsInLastMinute().get() >= config.getBurstLimit()) {
                logger.warn("用户 {} 触发突发限制", userId);
                return new RateLimitResult(false, "请求过于频繁", 
                                      state.getLastRateLimitReset());
            }

            // 检查每分钟限制
            if (state.getRequestsInLastMinute().get() >= config.getMaxRequestsPerMinute()) {
                logger.warn("用户 {} 超过每分钟请求限制", userId);
                return new RateLimitResult(false, "每分钟请求限制", 
                                      state.getLastRateLimitReset());
            }

            // 检查每小时限制
            if (state.getRequestsInLastHour().get() >= config.getMaxRequestsPerHour()) {
                logger.warn("用户 {} 超过每小时请求限制", userId);
                return new RateLimitResult(false, "每小时请求限制", 
                                      state.getLastRateLimitReset());
            }

            // 检查每天限制
            if (state.getRequestsInLastDay().get() >= config.getMaxRequestsPerDay()) {
                logger.warn("用户 {} 超过每天请求限制", userId);
                return new RateLimitResult(false, "每天请求限制", 
                                      state.getLastRateLimitReset());
            }

            // 通过检查，增加计数
            state.requestsInLastMinute.incrementAndGet();
            state.requestsInLastHour.incrementAndGet();
            state.requestsInLastDay.incrementAndGet();

            return new RateLimitResult(true, "允许", state.getLastRateLimitReset());
        }

        /**
         * 重置用户状态
         */
        public void resetUserState(String userId) {
            RateLimitState state = userStates.get(userId);
            if (state != null) {
                state.requestsInLastMinute.set(0);
                state.requestsInLastHour.set(0);
                state.requestsInLastDay.set(0);
                state.lastRateLimitReset = LocalDateTime.now().toString();
                logger.info("重置用户 {} 的速率限制状态", userId);
            }
        }

        /**
         * 获取用户状态
         */
        public RateLimitState getUserState(String userId) {
            return userStates.get(userId);
        }

        /**
         * 获取所有用户状态
         */
        public Map<String, RateLimitState> getAllUserStates() {
            return new ConcurrentHashMap<>(userStates);
        }
    }

    /**
     * 速率限制结果
     */
    public static class RateLimitResult {
        private final boolean isAllowed;
        private final String message;
        private final String resetTime;

        public RateLimitResult(boolean isAllowed, String message, String resetTime) {
            this.isAllowed = isAllowed;
            this.message = message;
            this.resetTime = resetTime;
        }

        public boolean isAllowed() { return isAllowed; }
        public String getMessage() { return message; }
        public String getResetTime() { return resetTime; }
    }

    /**
     * 创建默认速率限制配置
     */
    public static RateLimitConfig createDefaultRateLimitConfig() {
        return new RateLimitConfig(
                60,      // 每分钟最多 60 次请求
                1000,    // 每小时最多 1000 次请求
                10000,   // 每天最多 10000 次请求
                10       // 突发限制：每分钟最多 10 次请求
        );
    }

    public static void main(String[] args) {
        // 创建速率限制器
        RateLimiter limiter = new RateLimiter(createDefaultRateLimitConfig());

        String testUser = "user123";

        System.out.println("╔═════════════════════════════════════════════════════════════╗");
        System.out.println("║              速率限制示例                              ║");
        System.out.println("╠═══════════════════════════════════════════════════════════╣");
        System.out.println("║ 用户 ID: " + testUser);
        System.out.println("╠═══════════════════════════════════════════════════════════╣");

        // 模拟请求
        for (int i = 1; i <= 80; i++) {
            RateLimitResult result = limiter.checkRateLimit(testUser);

            String status = result.isAllowed() ? "✅ 允许" : "❌ 拒绝";
            String statusColor = result.isAllowed() ? "绿色" : "红色";

            System.out.printf("║ 请求 [%3d] %s %s - %s                            ║\n",
                        i,
                        status,
                        statusColor,
                        result.getMessage());

            if (!result.isAllowed() && i % 20 == 0) {
                System.out.println("╠─────────────────────────────────────────────────────────────╣");
                System.out.println("║ 重置用户状态                                         ║\n");
                limiter.resetUserState(testUser);
            }
        }

        System.out.println("╚═════════════════════════════════════════════════════════════════════╝");
    }
}
```

## 综合安全系统

### 完整的安全护栏

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 综合安全系统
 */
public class SecurityGuardrails {

    private static final Logger logger = LoggerFactory.getLogger(SecurityGuardrails.class);

    /**
     * 安全事件
     */
    public static class SecurityEvent {
        private final String eventId;
        private final String eventType;
        private final String userId;
        private final String message;
        private final Map<String, Object> details;
        private final long timestamp;

        public SecurityEvent(String eventType, String userId, String message, 
                            Map<String, Object> details) {
            this.eventId = generateEventId();
            this.eventType = eventType;
            this.userId = userId;
            this.message = message;
            this.details = details;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters
        public String getEventId() { return eventId; }
        public String getEventType() { return eventType; }
        public String getUserId() { return userId; }
        public String getMessage() { return message; }
        public Map<String, Object> getDetails() { return details; }
        public long getTimestamp() { return timestamp; }

        private static String generateEventId() {
            return "SEC-" + System.currentTimeMillis() + "-" + 
                   Integer.toHexString((int) (Math.random() * 10000));
        }
    }

    /**
     * 安全策略
     */
    public static class SecurityPolicy {
        private final String policyId;
        private final String policyName;
        private final String description;
        private final boolean isEnabled;
        private final String action;

        public SecurityPolicy(String policyId, String policyName, String description, 
                             boolean isEnabled, String action) {
            this.policyId = policyId;
            this.policyName = policyName;
            this.description = description;
            this.isEnabled = isEnabled;
            this.action = action;
        }

        // Getters
        public String getPolicyId() { return policyId; }
        public String getPolicyName() { return policyName; }
        public String getDescription() { return description; }
        public boolean isEnabled() { return isEnabled; }
        public String getAction() { return action; }
    }

    /**
     * 安全策略管理器
     */
    public static class SecurityPolicyManager {
        private final Map<String, SecurityPolicy> policies;
        private final List<SecurityEvent> securityEvents;

        public SecurityPolicyManager() {
            this.policies = new ConcurrentHashMap<>();
            this.securityEvents = new ArrayList<>();
        }

        /**
         * 添加安全策略
         */
        public void addPolicy(SecurityPolicy policy) {
            policies.put(policy.getPolicyId(), policy);
            logger.info("添加安全策略: {} - {}", policy.getPolicyName(), policy.getDescription());
        }

        /**
         * 检查策略违规
         */
        public List<SecurityPolicy> checkPolicyViolations(String input, String userId) {
            List<SecurityPolicy> violations = new ArrayList<>();

            for (SecurityPolicy policy : policies.values()) {
                if (!policy.isEnabled()) {
                    continue;
                }

                // 检查政策违规
                if (isPolicyViolation(input, policy)) {
                    violations.add(policy);

                    // 记录安全事件
                    recordSecurityEvent("POLICY_VIOLATION", userId, policy.getPolicyName(), policy.getPolicyId());
                }
            }

            return violations;
        }

        /**
         * 检查是否违反政策
         */
        private boolean isPolicyViolation(String input, SecurityPolicy policy) {
            // 简化：实际应该根据政策类型进行不同的检查
            return switch (policy.getPolicyId()) {
                case "content_moderation" -> false;  // 由专门的审核服务处理
                case "rate_limiting" -> false;  // 由速率限制器处理
                case "input_validation" -> false;  // 由输入验证处理
                default -> false;
            };
        }

        /**
         * 记录安全事件
         */
        public void recordSecurityEvent(String eventType, String userId, String message, String policyId) {
            Map<String, Object> details = new ConcurrentHashMap<>();
            details.put("policyId", policyId);
            details.put("timestamp", System.currentTimeMillis());

            SecurityEvent event = new SecurityEvent(eventType, userId, message, details);
            securityEvents.add(event);

            logger.warn("安全事件 - 类型: {}, 用户: {}, 策略: {}", 
                        eventType, userId, policyId);
        }

        /**
         * 获取安全事件
         */
        public List<SecurityEvent> getSecurityEvents() {
            return new ArrayList<>(securityEvents);
        }

        /**
         * 生成安全报告
         */
        public String generateSecurityReport() {
            StringBuilder report = new StringBuilder();
            report.append("╔═════════════════════════════════════════════════════════════════════╗\n");
            report.append("║              安全报告                                      ║\n");
            report.append("╠═════════════════════════════════════════════════════════════════════╣\n");
            report.append("║ 安全政策总数: ").append(policies.size()).append("\n");
            report.append("╠═════════════════════════════════════════════════════════════════════╣\n");
            report.append("║ 安全事件总数: ").append(securityEvents.size()).append("\n");
            report.append("╠═════════════════════════════════════════════════════════════════════╣\n");

            // 按事件类型分组
            Map<String, Integer> eventCounts = new HashMap<>();
            for (SecurityEvent event : securityEvents) {
                eventCounts.merge(event.getEventType(), 1, Integer::sum);
            }

            eventCounts.forEach((eventType, count) -> {
                report.append("║   ").append(eventType).append(": ").append(count).append(" 次\n");
            });

            report.append("╠═════════════════════════════════════════════════════════════════════╣\n");
            report.append("║ 时间: ").append(
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            ).append("\n");
            report.append("╚═════════════════════════════════════════════════════════════════════════════╝\n");

            return report.toString();
        }
    }

    /**
     * 安全护栏服务
     */
    @AiService
    public interface SecurityGuardrailsService {
        /**
         * 安全检查
         */
        SecurityCheckResult performSecurityCheck(String input, String userId);
    }

    /**
     * 安全检查结果
     */
    public static class SecurityCheckResult {
        private final boolean isSafe;
        private final List<SecurityPolicy> violations;
        private final String message;
        private final SecurityAction action;

        public SecurityCheckResult(boolean isSafe, List<SecurityPolicy> violations, 
                                   String message, SecurityAction action) {
            this.isSafe = isSafe;
            this.violations = violations;
            this.message = message;
            this.action = action;
        }

        public boolean isSafe() { return isSafe; }
        public List<SecurityPolicy> getViolations() { return violations; }
        public String getMessage() { return message; }
        public SecurityAction getAction() { return action; }
    }

    /**
     * 安全行动
     */
    public enum SecurityAction {
        ALLOW("允许"),
        BLOCK("阻止"),
        MODERATE("审核"),
        RATE_LIMIT("限流"),
        FILTER("过滤"),
        LOG_ONLY("仅记录")
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("OPENAI_API_KEY");

        // 创建安全政策管理器
        SecurityPolicyManager policyManager = new SecurityPolicyManager();

        // 添加安全政策
        policyManager.addPolicy(new SecurityPolicy(
            "content_moderation",
            "内容审核",
            "禁止有害、暴力、仇恨、色情等不当内容",
            true,
            "BLOCK"
        ));

        policyManager.addPolicy(new SecurityPolicy(
            "input_validation",
            "输入验证",
            "验证输入长度、格式和内容",
            true,
            "REJECT"
        ));

        policyManager.addPolicy(new SecurityPolicy(
            "rate_limiting",
            "速率限制",
            "限制用户请求频率",
            true,
            "RATE_LIMIT"
        ));

        policyManager.addPolicy(new SecurityPolicy(
            "output_filtering",
            "输出过滤",
            "过滤输出中的敏感信息",
            true,
            "FILTER"
        ));

        // 生成安全报告
        System.out.println(policyManager.generateSecurityReport());
    }
}
```

## 测试代码示例

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 安全护栏测试
 */
class GuardrailsTest {

    @Test
    void should_reject_harmful_content() {
        // 创建审核服务
        String apiKey = System.getenv("OPENAI_API_KEY");
        ContentModerationService service = ContentModerationExample.createModerationService(apiKey);

        // 测试有害内容
        ModerationResult result = service.moderateText("我要杀了所有的人");

        // 验证
        assertFalse(result.isAllowed());
        assertTrue(result.getCategory() == ContentModerationExample.ModerationCategory.VIOLENCE || 
                   result.getCategory() == ContentModerationExample.ModerationCategory.HATE);
    }

    @Test
    void should_allow_safe_content() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        ContentModerationService service = ContentModerationExample.createModerationService(apiKey);

        ModerationResult result = service.moderateText("今天天气真好");

        assertTrue(result.isAllowed());
        assertTrue(result.getCategory() == ContentModerationExample.ModerationCategory.SAFE);
    }

    @Test
    void should_reject_input_with_sensitive_data() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        ValidationConfig config = InputValidationExample.createDefaultValidationConfig();
        InputValidationService service = InputValidationExample.createValidationService(apiKey, config);

        ValidationResult result = service.validateInput("我的邮箱是 user@example.com");

        assertFalse(result.isValid());
        assertTrue(result.getViolatedRules().contains("no_email"));
    }

    @Test
    void should_apply_output_filter() {
        String output = "我的邮箱是 test@example.com，电话 13800138000";

        FilterConfig config = OutputFilteringExample.createDefaultFilterConfig();
        FilterResult result = OutputFilteringExample.filterOutput(output, config);

        assertFalse(result.getOriginalOutput().equals(result.getFilteredOutput()));
        assertTrue(result.getAppliedRules().size() > 0);
    }

    @Test
    void should_enforce_rate_limit() {
        RateLimitingExample.RateLimiter limiter = new RateLimitingExample.RateLimiter(
            new RateLimitingExample.RateLimitConfig(
                    5,      // 每分钟 5 次
                    10,     // 每小时 10 次
                    100,    // 每天 100 次
                    3       // 突发 3 次
            )
        );

        String userId = "test_user";

        // 前 5 个请求应该被允许
        for (int i = 1; i <= 5; i++) {
            RateLimitingExample.RateLimitResult result = limiter.checkRateLimit(userId);
            assertTrue(result.isAllowed(), "第 " + i + " 个请求应该被允许");
        }

        // 第 6 个请求应该被拒绝
        RateLimitingExample.RateLimitResult result = limiter.checkRateLimit(userId);
        assertFalse(result.isAllowed(), "第 6 个请求应该被拒绝");
        assertEquals("每分钟请求限制", result.getMessage());
    }

    @Test
    void should_reset_rate_limit() {
        RateLimitingExample.RateLimiter limiter = new RateLimitingExample.RateLimiter(
            new RateLimitingExample.RateLimitConfig(
                    3,      // 每分钟 3 次
                    10,     // 每小时 10 次
                    100,    // 每天 100 次
                    2       // 突发 2 次
            )
        );

        String userId = "test_user";

        // 使用所有请求
        for (int i = 0; i < 3; i++) {
            limiter.checkRateLimit(userId);
        }

        // 第 4 个请求应该被拒绝
        RateLimitingExample.RateLimitResult result = limiter.checkRateLimit(userId);
        assertFalse(result.isAllowed());

        // 重置
        limiter.resetUserState(userId);

        // 重置后的请求应该被允许
        result = limiter.checkRateLimit(userId);
        assertTrue(result.isAllowed());
    }
}
```

## 实践练习

### 练习 1：实现多层级审核

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 多层级审核
 */
public class MultiLevelModeration {

    private static final Logger logger = LoggerFactory.getLogger(MultiLevelModeration.class);

    /**
     * 审核级别
     */
    public enum ModerationLevel {
        LEVEL_1("第一层 - 基础关键词检查"),
        LEVEL_2("第二层 - 语义分析"),
        LEVEL_3("第三层 - 上下文分析"),
        LEVEL_4("第四层 - 专家审核");

        private final String description;

        ModerationLevel(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }

    /**
     * 审核结果
     */
    public static class MultiLevelResult {
        private final ModerationLevel passedAt;
        private final boolean isAllowed;
        private final String reason;
        private final double confidence;
        private final List<ModerationLevel> checkedLevels;

        public MultiLevelResult(ModerationLevel passedAt, boolean isAllowed, 
                            String reason, double confidence, 
                            List<ModerationLevel> checkedLevels) {
            this.passedAt = passedAt;
            this.isAllowed = isAllowed;
            this.reason = reason;
            this.confidence = confidence;
            this.checkedLevels = checkedLevels;
        }

        public ModerationLevel getPassedAt() { return passedAt; }
        public boolean isAllowed() { return isAllowed; }
        public String getReason() { return reason; }
        public double getConfidence() { return confidence; }
        public List<ModerationLevel> getCheckedLevels() { return checkedLevels; }
    }

    /**
     * 多层级审核服务
     */
    @AiService
    public interface MultiLevelModerationService {
        /**
         * 执行多层级审核
         */
        MultiLevelResult performModeration(String text);
    }

    /**
     * 创建多层级审核服务
     */
    public static MultiLevelModerationService createMultiLevelModerationService(String apiKey) {
        return AiServices.builder(MultiLevelModerationService.class)
                .chatModel(OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("gpt-4o-mini")
                        .build())
                .systemMessageProvider(chatMemoryId -> 
                    "你是一个多层级内容审核专家。" +
                    "你的任务是按照指定的层级顺序，对给定的文本进行审核。" +
                    "\n第一层审核（LEVEL_1 - 基础关键词检查）：" +
                    "快速检查文本中是否包含明显的违规关键词。" +
                    "如果有，直接拒绝，停止后续审核。" +
                    "\n第二层审核（LEVEL_2 - 语义分析）：" +
                    "分析文本的语义，判断其意图和含义是否安全。" +
                    "即使没有明显关键词，也要判断文本整体语境。" +
                    "\n第三层审核（LEVEL_3 - 上下文分析）：" +
                    "结合文本的上下文信息（如对话历史）进行判断。" +
                    "考虑文本的潜在隐含意义和可能的影响。" +
                    "\n第四层审核（LEVEL_4 - 专家审核）：" +
                    "对于复杂的边缘情况，进行深入的人工专家级分析。" +
                    "综合考虑所有因素，做出最终判断。" +
                    "\n请以 JSON 格式返回结果，包含：" +
                    "- passedAt: 通过审核的最低级别（LEVEL_1、LEVEL_2、LEVEL_3、LEVEL_4 或 FAILED）" +
                    "- isAllowed: 内容是否被允许" +
                    "- reason: 审核结果的简要说明" +
                    "- confidence: 审核确信度（0.0 到 1.0）" +
                    "- checkedLevels: 实际检查的所有级别列表"
                )
                .build();
    }

    /**
     * 执行审核
     */
    public static MultiLevelResult performModeration(String text, String apiKey) {
        MultiLevelModerationService service = createMultiLevelModerationService(apiKey);
        return service.performModeration(text);
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("OPENAI_API_KEY");

        // 测试安全内容
        String safeText = "请问今天天气如何？";
        MultiLevelResult result1 = performModeration(safeText, apiKey);
        System.out.println("╔═════════════════════════════════════════════════════════════╗");
        System.out.println("║              多层级审核示例                              ║");
        System.out.println("╠═══════════════════════════════════════════════════════════╣");
        System.out.println("║ 输入: " + safeText);
        System.out.println("╠═══════════════════════════════════════════════════════════╣");
        System.out.println("║ 审核级别: " + result1.getPassedAt().getDescription());
        System.out.println("║ 是否允许: " + (result1.isAllowed() ? "✅ 允许" : "❌ 拒绝"));
        System.out.println("║ 原因: " + result1.getReason());
        System.out.println("║ 确信度: " + String.format("%.2f", result1.getConfidence()));
        System.out.println("║ 检查级别: " + result1.getCheckedLevels());
        System.out.println("╚═══════════════════════════════════════════════════════════════════════╝");
        System.out.println();

        // 测试有害内容
        String harmfulText = "我讨厌所有人，我要报复他们，毁灭这个世界！";
        MultiLevelResult result2 = performModeration(harmfulText, apiKey);
        System.out.println("╔═════════════════════════════════════════════════════════════╗");
        System.out.println("║              多层级审核示例                              ║");
        System.out.println("╠═══════════════════════════════════════════════════════════╣");
        System.out.println("║ 输入: " + harmfulText);
        System.out.println("╠═════════════════════════════════════════════════════════════╣");
        System.out.println("║ 审核级别: " + result2.getPassedAt().getDescription());
        System.out.println("║ 是否允许: " + (result2.isAllowed() ? "✅ 允许" : "❌ 拒绝"));
        System.out.println("║ 原因: " + result2.getReason());
        System.out.println("║ 确信度: " + String.format("%.2f", result2.getConfidence()));
        System.out.println("║ 检查级别: " + result2.getCheckedLevels());
        System.out.println("╠─────────────────────────────────────────────────────────────╣");
        System.out.println("║ 审核详情:                                               ║");
        System.out.println("║ LEVEL_1: 基础关键词检查 - 检测到违规关键词               ║");
        System.out.println("║           仇恨言论、暴力内容                                 ║");
        System.out.println("║ LEVEL_2: 语义分析 - 文本表达了敌对情绪               ║");
        System.out.println("║           具有攻击性和威胁意图                               ║");
        System.out.println("║ LEVEL_3: 上下文分析 - 分析对话和场景             ║");
        System.out.println("║           明显的威胁和危险意图                               ║");
        System.out.println("║ LEVEL_4: 专家审核 - 专家判断                       ║");
        System.out.println("║           高风险，建议拒绝                                 ║");
        System.out.println("╠─────────────────────────────────────────────────────────────╣");
        System.out.println("║ 最终判断: 拒绝                                         ║");
        System.out.println("║ 原因: 内容包含多个违规关键词和敌对意图，属于高风险内容       ║");
        System.out.println("║ 建议: 立即阻止用户，并记录安全事件                       ║");
        System.out.println("╠─────────────────────────────────────────────────────────────╣");
        System.out.println("║ 审核级别: LEVEL_1                                        ║");
        System.out.println("║ 确信度: 0.95                                               ║");
        System.out.println("║ 检查级别: [LEVEL_1, LEVEL_2, LEVEL_3, LEVEL_4]            ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════════════════╝");
    }
}
```

## 总结

### 本章要点

1. **安全护栏重要性**
   - 防止有害内容
   - 保护用户隐私
   - 确保合规要求
   - 维护品牌形象

2. **审核类型**
   - 内容审核
   - 输入验证
   - 输出过滤
   - 速率限制

3. **实现方式**
   - 基于规则（关键词、正则表达式）
   - 基于模型（语义分析）
   - 混合方法（规则 + 模型）
   - 多层级审核

4. **最佳实践**
   - 立即阻止明显违规
   - 使用多层审核提高准确度
   - 记录所有安全事件
   - 定期审查和更新政策
   - 提供清晰的反馈

5. **应用场景**
   - 社交媒体平台
   - 即时通讯应用
   - 用户生成内容平台
   - 教育和儿童应用
   - 金融和医疗应用

### 下一步

在下一章节中，我们将学习：
- 其他模型提供商
- 自定义模型集成
- 本地模型部署
- 性能优化技巧
- 生产环境部署

### 常见问题

**Q1：如何平衡安全性和用户体验？**

A：平衡策略：
1. 第一层快速检查避免明显违规
2. 第二层语义分析减少误报
3. 对可疑内容使用人工审核
4. 提供清晰的政策说明
5. 允许用户申诉

**Q2：如何检测隐含的违规内容？**

A：检测方法：
1. 语义分析识别意图
2. 上下文分析了解场景
3. 情感分析判断语气
4. 隐喻和讽刺识别
5. 多模型交叉验证

**Q3：如何处理多语言内容审核？**

A：处理策略：
1. 使用多语言审核模型
2. 翻译后审核（成本高）
3. 针对每种语言制定规则
4. 使用通用语义模型
5. 组合多种方法

**Q4：如何减少误报？**

A：减少策略：
1. 调整审核阈值
2. 使用上下文信息
3. 允许用户申诉
4. 人工审核边缘案例
5. 定期审核和更新规则

**Q5：如何监控安全护栏效果？**

A：监控指标：
1. 违规内容拦截率
2. 误报率和假阴性率
3. 审核时间和延迟
4. 用户投诉率
5. 安全事件趋势

## 参考资料

- [LangChain4j 安全护栏文档](https://docs.langchain4j.dev/tutorials/guardrails)
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
