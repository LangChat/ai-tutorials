---
title: 'JSON 处理'
description: '学习 LangChain4j 的 JSON 处理 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 14 - JSON 处理

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 理解 LangChain4j 中的 JSON 序列化机制
- 掌握 `@Jackson` 注解的使用方法
- 学会配置 JSON 对象映射策略
- 理解自定义 JSON 序列化器
- 掌握 JSON 工具的使用和最佳实践
- 实现一个完整的 JSON 处理示例

## 前置知识

- 完成《01 - LangChain4j 简介》章节
- 完成《02 - 你的第一个 Chat 应用》章节
- Java 基础知识（熟悉 Jackson 库）

## 核心概念

### 什么是 JSON 处理？

在 LangChain4j 中，**JSON 处理**主要涉及：
1. 将对象序列化为 JSON 格式（用于传递给 LLM）
2. 将 LLM 返回的 JSON 字符串反序列化为对象
3. 处理复杂的嵌套 JSON 结构

### 为什么需要特殊处理？

**LangChain4j 默认使用 Jackson**，但有时需要自定义：
- 控制日期格式
- 忽略空值或未知属性
- 自定义字段命名策略（camelCase ↔ snake_case）
- 处理复杂的继承结构

### Jackson 配置要点

```java
// Jackson 常用配置
String json = """
    {
        "userName": "张三",
        "userAge": 25,
        "isVip": true,
        "lastLogin": "2024-01-15T10:30:00"
    }
    """;

ObjectMapper mapper = new ObjectMapper()
    .registerModule(new JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .setProperty(PropertyNamingStrategies.SNAKE_CASE, mapper.getSerializationConfig().getPropertyNamingStrategy());
```

## @Jackson 注解

### 使用 @Jackson 禁用序列化

```java
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 使用 @Jackson 注解控制序列化
 */
public class User {

    @JsonProperty("user_name")
    private String name;

    @JsonProperty("user_age")
    private int age;

    @JsonProperty("is_vip")
    private boolean vip;

    @JsonIgnore
    private String password;  // 不会序列化到 JSON

    @JsonProperty("last_login")
    private String lastLogin;

    @JsonProperty("preferences")
    private Map<String, String> preferences;

    // 构造器
    public User() {}

    public User(String name, int age, boolean vip, String password, String lastLogin) {
        this.name = name;
        this.age = age;
        this.vip = vip;
        this.password = password;
        this.lastLogin = lastLogin;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public boolean isVip() { return vip; }
    public void setVip(boolean vip) { this.vip = vip; }

    public String getPassword() { return password; }

    public String getLastLogin() { return lastLogin; }
    public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }

    public Map<String, String> getPreferences() { return preferences; }
    public void setPreferences(Map<String, String> preferences) { this.preferences = preferences; }

    @Override
    public String toString() {
        return "User{" +
               "name='" + name + '\'' +
               ", age=" + age +
               ", vip=" + vip +
               ", preferences=" + preferences +
               '}';
    }
}
```

### 使用 @JsonAlias 设置别名

```java
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 使用 @JsonAlias 设置 JSON 字段别名
 */
public class Product {

    @JsonProperty("product_id")
    @JsonAlias({"id", "productId", "ID"})  // 多个可能的 JSON 字段名
    private String id;

    @JsonProperty("product_name")
    @JsonAlias({"name", "productName", "product_name"})
    private String name;

    @JsonProperty("product_price")
    @JsonAlias({"price", "productPrice", "cost"})
    private double price;

    @JsonProperty("in_stock")
    @JsonAlias({"available", "hasStock", "isInStock"})
    private boolean inStock;

    // 构造器
    public Product() {}

    public Product(String id, String name, double price, boolean inStock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.inStock = inStock;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public boolean isInStock() { return inStock; }
    public void setInStock(boolean inStock) { this.inStock = inStock; }

    @Override
    public String toString() {
        return "Product{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", price=" + price +
               ", inStock=" + inStock +
               '}';
    }
}
```

## 自定义序列化器

### 日期格式化

```java
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 自定义日期序列化器
 */
@JsonSerialize(using = CustomDateSerializer.class)
public class CustomDateSerializer extends JsonSerializer<Date> {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @Override
    public void serialize(Date date, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (date == null) {
            gen.writeNull();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        String formattedDate = sdf.format(date);

        gen.writeString(formattedDate);
    }
}
```

### 枚举序列化

```java
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;

/**
 * 用户状态枚举
 */
@JsonSerialize(using = UserStatusSerializer.class)
public enum UserStatus {

    ACTIVE("active", 1),
    INACTIVE("inactive", 0),
    SUSPENDED("suspended", -1),
    DELETED("deleted", -2);

    private final String status;
    private final int code;

    UserStatus(String status, int code) {
        this.status = status;
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public int getCode() {
        return code;
    }
}

/**
 * 用户状态序列化器
 */
class UserStatusSerializer extends JsonSerializer<UserStatus> {

    @Override
    public void serialize(UserStatus value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("status", value.getStatus());
        gen.writeNumberField("code", value.getCode());
        gen.writeEndObject();
    }
}
```

## JSON 工具方法

### 创建 ObjectMapper

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * JSON 工具类
 */
public class JsonUtils {

    private static final ObjectMapper DEFAULT_MAPPER;

    static {
        DEFAULT_MAPPER = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * 获取默认 ObjectMapper
     */
    public static ObjectMapper getDefaultMapper() {
        return DEFAULT_MAPPER;
    }

    /**
     * 创建自定义 ObjectMapper
     */
    public static ObjectMapper createCustomMapper(JsonConfiguration config) {
        ObjectMapper mapper = new ObjectMapper();

        // 注册 JavaTimeModule
        mapper.registerModule(new JavaTimeModule());

        // 配置日期格式
        if (config != null && config.getDateFormat() != null) {
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.setDateFormat(new SimpleDateFormat(config.getDateFormat()));
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            // 配置命名策略
            if (config != null && config.getPropertyNamingStrategy() != null) {
                mapper.setProperty(
                        PropertyNamingStrategies.SNAKE_CASE,
                        mapper.getSerializationConfig().getPropertyNamingStrategy()
                    );
            }
        }

        return mapper;
    }

    /**
     * 将对象序列化为 JSON 字符串
     */
    public static String toJson(Object object) throws Exception {
        return DEFAULT_MAPPER.writeValueAsString(object);
    }

    /**
     * 将对象序列化为格式化的 JSON 字符串
     */
    public static String toPrettyJson(Object object) throws Exception {
        return DEFAULT_MAPPER.writerWithDefaultPrettyPrinter()
                .writeValueAsString(object);
    }

    /**
     * 将 JSON 字符串反序列化为对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return DEFAULT_MAPPER.readValue(json, clazz);
    }

    /**
     * 将 JSON 字符串反序列化为对象（带类型引用）
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) throws Exception {
        return DEFAULT_MAPPER.readValue(json, typeReference);
    }
}

/**
 * JSON 配置
 */
class JsonConfiguration {
    private String dateFormat;
    private PropertyNamingStrategy propertyNamingStrategy;

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public PropertyNamingStrategy getPropertyNamingStrategy() {
        return propertyNamingStrategy;
    }

    public void setPropertyNamingStrategy(PropertyNamingStrategy propertyNamingStrategy) {
        this.propertyNamingStrategy = propertyNamingStrategy;
    }
}
```

## JSON 与 LLM 集成

### 传递 JSON 给 LLM

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;

/**
 * JSON 与 LLM 集成
 */
public class JsonWithLLM {

    private final ChatModel model;
    private final ObjectMapper mapper;

    public JsonWithLLM(String apiKey) {
        this.model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .build();
        this.mapper = new ObjectMapper();
    }

    /**
     * 要求 LLM 返回 JSON 格式
     */
    public String requestJson(String prompt, String instructions) {
        String fullPrompt = String.format(
            "%s\n" +
            "请按照以下 JSON 格式返回响应：\n" +
            "%s\n\n" +
            "用户问题：%s",
            instructions,
            prompt
        );

        AiMessage response = model.chat(fullPrompt);
        return response.text();
    }

    /**
     * 将 LLM 返回的 JSON 转换为对象
     */
    public <T> T parseLlmResponse(String jsonResponse, Class<T> clazz) throws Exception {
        return mapper.readValue(jsonResponse, clazz);
    }

    /**
     * 使用提示词模板构建 JSON 请求
     */
    public String buildJsonPrompt(String prompt, Map<String, Object> context) {
        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("请基于以下信息回答问题，并以 JSON 格式返回：\n\n");

        promptBuilder.append("上下文信息：\n");
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            promptBuilder.append(String.format("- %s: %s\n", 
                entry.getKey(), entry.getValue()));
        }

        promptBuilder.append(String.format("\n用户问题：%s", prompt));

        return promptBuilder.toString();
    }
}
```

## 测试代码示例

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

/**
 * JSON 处理测试
 */
class JsonHandlingTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        this.mapper = new ObjectMapper();
    }

    @Test
    void should_serialize_to_json() throws Exception {
        Map<String, Object> user = Map.of(
            "name", "张三",
            "age", 25,
            "vip", true
        );

        String json = mapper.writeValueAsString(user);

        // 验证
        assertNotNull(json);
        assertTrue(json.contains("\"name\":\"张三\""));
        assertTrue(json.contains("\"age\":25"));
        assertTrue(json.contains("\"vip\":true"));

        System.out.println("JSON: " + json);
    }

    @Test
    void should_deserialize_from_json() throws Exception {
        String json = "{\"name\":\"张三\",\"age\":25,\"vip\":true}";

        Map<String, Object> user = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});

        // 验证
        assertNotNull(user);
        assertEquals("张三", user.get("name"));
        assertEquals(25, user.get("age"));
        assertEquals(true, user.get("vip"));

        System.out.println("User: " + user);
    }

    @Test
    void should_handle_nested_json() throws Exception {
        String json = """
            {
                "user": {
                    "name": "张三",
                    "profile": {
                        "age": 25,
                        "vip": true
                    }
                }
            }
            """;

        Map<String, Object> result = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});

        // 验证
        assertNotNull(result);
        Map<String, Object> user = (Map<String, Object>) result.get("user");
        assertNotNull(user);

        Map<String, Object> profile = (Map<String, Object>) user.get("profile");
        assertNotNull(profile);

        assertEquals(25, profile.get("age"));
        assertEquals(true, profile.get("vip"));

        System.out.println("Result: " + result);
    }

    @Test
    void should_format_json_pretty() throws Exception {
        Map<String, Object> user = Map.of(
            "name", "张三",
            "age", 25,
            "vip", true,
            "preferences", Map.of(
                "language", "Java",
                "framework", "Spring"
            )
        );

        String json = new ObjectMapper().writerWithDefaultPrettyPrinter()
                .writeValueAsString(user);

        // 验证
        assertNotNull(json);
        assertTrue(json.contains("\n"));  // 确保是多行格式
        assertTrue(json.contains("language"));

        System.out.println("Pretty JSON: " + json);
    }
}
```

## 实践练习

### 练习 1：实现 API 响应 JSON 工具

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * API 响应 JSON 工具
 */
public class ApiResponseJsonUtil {

    private static final ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * API 响应包装器
     */
    public static class ApiResponse<T> {
        private final int code;
        private final String message;
        private final T data;
        private final long timestamp;

        public ApiResponse(int code, String message, T data) {
            this.code = code;
            this.message = message;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        public int getCode() { return code; }
        public String getMessage() { return message; }
        public T getData() { return data; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "成功", data);
    }

    /**
     * 错误响应
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null);
    }

    /**
     * 错误响应（带错误代码）
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    /**
     * 将 API 响应序列化为 JSON
     */
    public static String toJson(ApiResponse<?> response) throws Exception {
        return MAPPER.writeValueAsString(response);
    }

    /**
     * 格式化的 JSON 响应
     */
    public static String toPrettyJson(ApiResponse<?> response) throws Exception {
        return MAPPER.writerWithDefaultPrettyPrinter()
                .writeValueAsString(response);
    }

    /**
     * 从 JSON 解析 API 响应
     */
    public static <T> ApiResponse<T> fromJson(String json, Class<T> dataClass) throws Exception {
        return MAPPER.readValue(json, 
            new TypeReference<ApiResponse<T>>() {}.getGenericSuperclass());
    }

    public static void main(String[] args) throws Exception {
        // 测试成功响应
        Map<String, Object> data = Map.of(
            "userId", 12345,
            "username", "张三",
            "lastLogin", new Date()
        );

        ApiResponse<Map<String, Object>> successResponse = success(data);
        System.out.println("=== 成功响应 ===");
        System.out.println(toPrettyJson(successResponse));
        System.out.println();

        // 测试错误响应
        ApiResponse<Object> errorResponse = error("用户不存在");
        System.out.println("=== 错误响应 ===");
        System.out.println(toPrettyJson(errorResponse));
        System.out.println();

        // 测试错误响应（带错误代码）
        ApiResponse<Object> errorCodeResponse = error(404, "资源未找到");
        System.out.println("=== 错误响应（带代码） ===");
        System.out.println(toPrettyJson(errorCodeResponse));
    }
}
```

### 练习 2：实现配置化 JSON 序列化

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.text.SimpleDateFormat;
import java.util.Map;

/**
 * 配置化 JSON 序列化
 */
public class ConfigurableJsonSerializer {

    private final ObjectMapper mapper;

    public ConfigurableJsonSerializer(JsonConfig config) {
        this.mapper = createMapper(config);
    }

    /**
     * 创建配置的 ObjectMapper
     */
    private static ObjectMapper createMapper(JsonConfig config) {
        ObjectMapper mapper = new ObjectMapper();

        // 注册 JavaTimeModule
        mapper.registerModule(new JavaTimeModule());

        // 配置日期格式
        if (config.getDateFormat() != null) {
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.setDateFormat(new SimpleDateFormat(config.getDateFormat()));
        }

        // 配置序列化包含
        if (config.getSerializationInclusion() != null) {
            mapper.setSerializationInclusion(config.getSerializationInclusion());
        }

        // 配置命名策略
        if (config.getPropertyNamingStrategy() != null) {
            mapper.setProperty(config.getPropertyNamingStrategy(), 
                mapper.getSerializationConfig().getPropertyNamingStrategy());
        }

        // 配置缩进
        if (config.isPrettyPrint()) {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setDefaultPrettyPrinter(new ObjectMapper().getSerializationConfig().getDefaultPrettyPrinter());
        } else {
            mapper.disable(SerializationFeature.INDENT_OUTPUT);
        }

        return mapper;
    }

    /**
     * 序列化为 JSON
     */
    public String toJson(Object object) throws Exception {
        return mapper.writeValueAsString(object);
    }

    /**
     * 序列化为格式化 JSON
     */
    public String toPrettyJson(Object object) throws Exception {
        return mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(object);
    }

    /**
     * 从 JSON 反序列化
     */
    public <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return mapper.readValue(json, clazz);
    }

    /**
     * 获取配置的 mapper
     */
    public ObjectMapper getMapper() {
        return mapper;
    }

    /**
     * JSON 配置
     */
    public static class JsonConfig {
        private String dateFormat;
        private JsonInclude serializationInclusion;
        private PropertyNamingStrategy propertyNamingStrategy;
        private boolean prettyPrint = true;
        private boolean ignoreNullValues = true;

        public String getDateFormat() {
            return dateFormat;
        }

        public void setDateFormat(String dateFormat) {
            this.dateFormat = dateFormat;
        }

        public JsonInclude getSerializationInclusion() {
            return serializationInclusion;
        }

        public void setSerializationInclusion(JsonInclude serializationInclusion) {
            this.serializationInclusion = serializationInclusion;
        }

        public PropertyNamingStrategy getPropertyNamingStrategy() {
            return propertyNamingStrategy;
        }

        public void setPropertyNamingStrategy(PropertyNamingStrategy propertyNamingStrategy) {
            this.propertyNamingStrategy = propertyNamingStrategy;
        }

        public boolean isPrettyPrint() {
            return prettyPrint;
        }

        public void setPrettyPrint(boolean prettyPrint) {
            this.prettyPrint = prettyPrint;
        }

        public boolean isIgnoreNullValues() {
            return ignoreNullValues;
        }

        public void setIgnoreNullValues(boolean ignoreNullValues) {
            this.ignoreNullValues = ignoreNullValues;
        }
    }

    public static void main(String[] args) throws Exception {
        // 测试默认配置
        System.out.println("=== 默认配置 ===");
        JsonConfig defaultConfig = new JsonConfig();
        ConfigurableJsonSerializer defaultSerializer = new ConfigurableJsonSerializer(defaultConfig);

        Map<String, Object> data = Map.of(
            "name", "张三",
            "age", 25,
            "vip", true,
            "nullField", null
        );

        String defaultJson = defaultSerializer.toPrettyJson(data);
        System.out.println(defaultJson);
        System.out.println();

        // 测试自定义配置
        System.out.println("=== 自定义配置 ===");
        JsonConfig customConfig = new JsonConfig();
        customConfig.setDateFormat("yyyy年MM月dd日 HH时mm分ss秒");
        customConfig.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        customConfig.setPrettyPrint(true);
        customConfig.setIgnoreNullValues(true);

        ConfigurableJsonSerializer customSerializer = new ConfigurableJsonSerializer(customConfig);

        String customJson = customSerializer.toPrettyJson(data);
        System.out.println(customJson);
    }
}
```

### 练习 3：实现动态 JSON 生成器

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

/**
 * 动态 JSON 生成器
 */
public class DynamicJsonBuilder {

    private final ObjectMapper mapper;
    private final ObjectNode rootNode;

    public DynamicJsonBuilder() {
        this.mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);
                .setDefaultPrettyPrinter(new ObjectMapper().getSerializationConfig().getDefaultPrettyPrinter());
        this.rootNode = mapper.createObjectNode();
    }

    /**
     * 添加字符串字段
     */
    public DynamicJsonBuilder put(String key, String value) {
        rootNode.put(key, value);
        return this;
    }

    /**
     * 添加数字字段
     */
    public DynamicJsonBuilder put(String key, Number value) {
        rootNode.put(key, value);
        return this;
    }

    /**
     * 添加布尔字段
     */
    public DynamicJsonBuilder put(String key, Boolean value) {
        rootNode.put(key, value);
        return this;
    }

    /**
     * 添加对象字段
     */
    public DynamicJsonBuilder put(String key, Object value) {
        rootNode.putPOJO(key, value);
        return this;
    }

    /**
     * 批量添加字段
     */
    public DynamicJsonBuilder putAll(Map<String, Object> fields) {
        if (fields != null) {
            fields.forEach(rootNode::putPOJO);
        }
        return this;
    }

    /**
     * 添加嵌套对象
     */
    public DynamicJsonBuilder putNested(String key, Map<String, Object> nestedFields) {
        ObjectNode nestedNode = mapper.createObjectNode();
        if (nestedFields != null) {
            nestedFields.forEach(nestedNode::putPOJO);
        }
        rootNode.set(key, nestedNode);
        return this;
    }

    /**
     * 移除字段
     */
    public DynamicJsonBuilder remove(String key) {
        rootNode.remove(key);
        return this;
    }

    /**
     * 构建 JSON 字符串
     */
    public String build() throws Exception {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
    }

    /**
     * 构建 JSON 树
     */
    public JsonNode buildNode() {
        return rootNode;
    }

    /**
     * 清空所有字段
     */
    public DynamicJsonBuilder clear() {
        List<String> fieldNames = new java.util.ArrayList<>(rootNode.fieldNames());
        fieldNames.forEach(rootNode::remove);
        return this;
    }

    /**
     * 合并另一个 JSON 对象
     */
    public DynamicJsonBuilder merge(JsonNode otherNode) {
        rootNode.setAll(otherNode);
        return this;
    }

    /**
     * 将字符串转换为 JSON 节点
     */
    public JsonNode parseJson(String json) throws Exception {
        return mapper.readTree(json);
    }

    public static void main(String[] args) throws Exception {
        // 测试动态构建
        System.out.println("=== 动态 JSON 构建 ===");
        System.out.println();

        DynamicJsonBuilder builder = new DynamicJsonBuilder()
                .put("name", "张三")
                .put("age", 25)
                .put("vip", true)
                .put("preferences", Map.of(
                    "language", "Java",
                    "framework", "Spring"
                ))
                .putNested("profile", Map.of(
                    "country", "中国",
                    "city", "北京"
                ));

        String json = builder.build();
        System.out.println(json);
        System.out.println();

        // 测试批量添加
        System.out.println("=== 批量添加字段 ===");
        System.out.println();

        DynamicJsonBuilder builder2 = new DynamicJsonBuilder();
        builder2.putAll(Map.of(
            "field1", "value1",
            "field2", "value2",
            "field3", "value3"
        ));

        System.out.println(builder2.build());
        System.out.println();

        // 测试合并
        System.out.println("=== 合并 JSON ===");
        System.out.println();

        DynamicJsonBuilder builder3 = new DynamicJsonBuilder()
                .put("name", "李四")
                .put("age", 30);

        JsonNode otherJson = builder3.parseJson("{\"city\":\"上海\",\"email\":\"lisi@example.com\"}");
        builder3.merge(otherJson);

        System.out.println(builder3.build());
        System.out.println();

        // 测试清空和重建
        System.out.println("=== 清空和重建 ===");
        System.out.println();

        DynamicJsonBuilder builder4 = new DynamicJsonBuilder()
                .put("temp1", "value1")
                .put("temp2", "value2")
                .put("temp3", "value3");

        System.out.println("原始 JSON:");
        System.out.println(builder4.build());
        System.out.println();

        builder4.clear();

        System.out.println("清空后:");
        System.out.println(builder4.buildNode());
        System.out.println();

        builder4.put("name", "王五")
                .put("age", 28);

        System.out.println("重建后:");
        System.out.println(builder4.build());
    }
}
```

## 总结

### 本章要点

1. **JSON 处理概念**
   - 序列化和反序列化
   - LangChain4j 中的 JSON 处理
   - 与 LLM 的集成

2. **@Jackson 注解**
   - `@JsonProperty` - 设置 JSON 字段名
   - `@JsonAlias` - 设置字段别名
   - `@JsonIgnore` - 忽略字段
   - `@JsonSerialize` - 自定义序列化

3. **自定义序列化器**
   - 日期格式化
   - 枚举序列化
   - 复杂对象序列化

4. **JSON 工具**
   - ObjectMapper 创建
   - 序列化和反序列化
   - 格式化输出
   - 错误处理

5. **最佳实践**
   - 合理配置 ObjectMapper
   - 使用注解简化代码
   - 处理日期和时间格式
   - 避免循环引用

### 下一步

在下一章节中，我们将学习：
- 日志和监控
- 可观测性配置
- 性能分析和优化
- 错误处理和重试
- 测试和评估

### 常见问题

**Q1：LangChain4j 使用哪个 JSON 库？**

A：LangChain4j 默认使用 Jackson，这是 Java 中最流行的 JSON 处理库之一。

**Q2：如何配置日期格式？**

A：可以通过以下方式配置：
1. 注册 `JavaTimeModule`
2. 设置 `DateFormat`
3. 禁用 `WRITE_DATES_AS_TIMESTAMPS`
4. 使用 `@JsonFormat` 注解

**Q3：如何处理循环引用？**

A：
1. 使用 `@JsonIgnore` 忽略循环引用字段
2. 使用 `@JsonIdentityInfo` 标识对象
3. 配置 `ObjectMapper` 处理循环引用
4. 使用 `@JsonManagedReference` 自定义引用处理

**Q4：如何控制 null 值的序列化？**

A：
1. 全局配置：`mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)`
2. 局部注解：`@JsonInclude(JsonInclude.Include.NON_NULL)`
3. 自定义序列化器：在序列化器中检查并处理

**Q5：如何优化 JSON 性能？**

A：优化技巧：
1. 重用 ObjectMapper 实例
2. 避免不必要的序列化和反序列化
3. 使用树模型而非对象模型
4. 配置适当的序列化特性
5. 考虑使用更快的 JSON 库（如 Gson）在性能关键场景

## 参考资料

- [LangChain4j JSON 文档](https://docs.langchain4j.dev/tutorials/json)
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
