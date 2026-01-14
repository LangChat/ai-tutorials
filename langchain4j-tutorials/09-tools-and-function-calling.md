---
title: '工具和函数调用'
description: '学习 LangChain4j 的 工具和函数调用 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 09 - Tools 工具调用

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 理解工具调用（Function Calling）的概念
- 掌握 `@Tool` 注解的使用
- 学会定义和创建自定义工具
- 理解工具参数和返回值
- 掌握工具执行和错误处理
- 实现一个带工具调用的聊天机器人

## 前置知识

- 完成《01 - LangChain4j 简介》章节
- 完成《02 - 你的第一个 Chat 应用》章节
- 完成《03 - 深入理解 ChatModel》章节

## 核心概念

### 什么是工具调用（Function Calling）？

**工具调用**是允许 LLM 在需要时调用外部函数或 API 的机制。

**类比理解：**
如果把 LLM 比作一个智能助手，那么工具就像是助手的手机：
- LLM 可以打电话（调用工具）获取实时信息
- LLM 可以查天气（调用天气 API）
- LLM 可以搜索数据库（调用数据库查询）

**为什么需要工具调用？**

1. **实时信息** - 获取最新天气、股票价格等
2. **数据访问** - 查询数据库、文件系统等
3. **外部服务** - 调用第三方 API（如支付、邮件等）
4. **计算能力** - 执行数学计算、数据分析等
5. **系统操作** - 读写文件、发送通知等

### 工具调用的流程

```
1. 用户发送查询
      ↓
2. LLM 分析查询
      ↓
3. LLM 决定是否需要调用工具
   ↓
   ├─ 不需要 → 直接回答
   │
   └─ 需要 → 生成工具调用请求
             ↓
           4. 执行工具
             ↓
           5. 返回工具执行结果
             ↓
           6. LLM 根据结果生成最终回答
```

### LangChain4j 中的工具调用

LangChain4j 通过以下机制支持工具调用：

1. **`@Tool` 注解** - 标记工具方法
2. **自动调用** - LLM 决定何时调用哪个工具
3. **参数提取** - 自动从用户消息中提取工具参数
4. **结果返回** - 将工具执行结果返回给 LLM

## 定义工具

### 使用 @Tool 注解

使用 `@Tool` 注解标记一个方法为工具：

```java
import dev.langchain4j.agent.tool.Tool;

/**
 * 计算器工具
 */
public class Calculator {

    @Tool("计算两个数的和")
    String add(int a, int b) {
        return String.valueOf(a + b);
    }

    @Tool("计算两个数的差")
    String subtract(int a, int b) {
        return String.valueOf(a - b);
    }

    @Tool("计算两个数的积")
    String multiply(int a, int b) {
        return String.valueOf(a * b);
    }

    @Tool("计算两个数的商")
    String divide(int a, int b) {
        if (b == 0) {
            return "除数不能为零";
        }
        return String.valueOf((double) a / b);
    }
}
```

### 工具方法的要求

**工具方法需要满足：**

1. **返回值** - 必须有返回值（通常是 String）
2. **参数** - 可以有零个或多个参数
3. **参数类型** - 支持：
   - 基本类型（String、int、double、boolean 等）
   - 复杂类型（自定义对象、Map 等）
4. **注解** - 使用 `@Tool` 注解，可提供描述

### 工具描述

```java
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolMemoryId;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.util.function.Function;

/**
 * 带详细描述的工具
 */
public class AdvancedTools {

    /**
     * 简单工具 - 只需要基本类型参数
     */
    @Tool("获取当前日期和时间")
    String getCurrentDateTime() {
        return java.time.LocalDateTime.now().toString();
    }

    /**
     * 带参数描述的工具
     */
    @Tool(
        name = "getWeather",
        description = "根据城市名称获取天气信息"
    )
    String getWeather(
        @ToolParam("城市名称") String city,
        @ToolParam("温度单位 (celsius 或 fahrenheit)", defaultValue = "celsius") String unit
    ) {
        // 模拟天气查询
        if ("北京".equals(city)) {
            if ("celsius".equals(unit)) {
                return "北京今天晴，温度 25°C";
            } else {
                return "北京今天晴，温度 77°F";
            }
        } else if ("上海".equals(city)) {
            return "上海今天多云，温度 22°C";
        } else {
            return "抱歉，暂无 " + city + " 的天气信息";
        }
    }

    /**
     * 复杂参数 - 使用自定义对象
     */
    @Tool("发送邮件")
    String sendEmail(EmailRequest request) {
        // 模拟发送邮件
        return "邮件已发送给 " + request.getTo() + 
               "，主题: " + request.getSubject();
    }

    public static class EmailRequest {
        private String to;
        private String subject;
        private String content;

        public EmailRequest(String to, String subject, String content) {
            this.to = to;
            this.subject = subject;
            this.content = content;
        }

        public String getTo() { return to; }
        public String getSubject() { return subject; }
        public String getContent() { return content; }
    }
}
```

## 工具参数

### 基本类型参数

```java
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolParam;

public class BasicToolParameters {

    /**
     * String 参数
     */
    @Tool("查询用户信息")
    String getUserInfo(
        @ToolParam("用户 ID") String userId
    ) {
        // 模拟数据库查询
        return "用户 " + userId + " 的信息：张三，25岁，软件工程师";
    }

    /**
     * int 参数
     */
    @Tool("计算平方")
    String square(
        @ToolParam("数字") int number
    ) {
        return number + " 的平方是 " + (number * number);
    }

    /**
     * double 参数
     */
    @Tool("计算平方根")
    String sqrt(
        @ToolParam("数字") double number
    ) {
        if (number < 0) {
            return "不能计算负数的平方根";
        }
        return number + " 的平方根是 " + Math.sqrt(number);
    }

    /**
     * boolean 参数
     */
    @Tool("检查数字奇偶性")
    String isOddOrEven(
        @ToolParam("数字") int number
    ) {
        return number + " 是" + (number % 2 == 0 ? "偶数" : "奇数");
    }

    /**
     * 多个参数
     */
    @Tool("计算矩形面积")
    String calculateRectangleArea(
        @ToolParam("长度") double length,
        @ToolParam("宽度") double width
    ) {
        return "矩形面积是 " + (length * width);
    }
}
```

### 复杂类型参数

```java
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolParam;
import java.util.List;
import java.util.Map;

public class ComplexToolParameters {

    /**
     * List 参数
     */
    @Tool("计算数字列表的平均值")
    String calculateAverage(
        @ToolParam("数字列表") List<Integer> numbers
    ) {
        if (numbers == null || numbers.isEmpty()) {
            return "列表不能为空";
        }

        double sum = 0;
        for (int num : numbers) {
            sum += num;
        }

        return "平均值是 " + (sum / numbers.size());
    }

    /**
     * Map 参数
     */
    @Tool("创建订单")
    String createOrder(
        @ToolParam("订单信息") Map<String, Object> orderInfo
    ) {
        String productId = (String) orderInfo.get("productId");
        int quantity = (Integer) orderInfo.getOrDefault("quantity", 1);
        String shippingAddress = (String) orderInfo.get("shippingAddress");

        // 模拟订单创建
        return "订单已创建：产品 ID " + productId + 
               ", 数量 " + quantity + 
               ", 配送地址 " + shippingAddress;
    }

    /**
     * 自定义对象参数
     */
    @Tool("搜索图书")
    String searchBooks(BookSearchParams params) {
        return "搜索条件：" + params.toString() + 
               "\n模拟搜索结果...";
    }

    public static class BookSearchParams {
        private String title;
        private String author;
        private String category;
        private Integer minPrice;
        private Integer maxPrice;

        // 构造器
        public BookSearchParams() {}

        public BookSearchParams title(String title) {
            this.title = title;
            return this;
        }

        public BookSearchParams author(String author) {
            this.author = author;
            return this;
        }

        public BookSearchParams category(String category) {
            this.category = category;
            return this;
        }

        public BookSearchParams minPrice(Integer minPrice) {
            this.minPrice = minPrice;
            return this;
        }

        public BookSearchParams maxPrice(Integer maxPrice) {
            this.maxPrice = maxPrice;
            return this;
        }

        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public String getCategory() { return category; }
        public Integer getMinPrice() { return minPrice; }
        public Integer getMaxPrice() { return maxPrice; }

        @Override
        public String toString() {
            return "BookSearchParams{" +
                   "title='" + title + '\'' +
                   ", author='" + author + '\'' +
                   ", category='" + category + '\'' +
                   ", minPrice=" + minPrice +
                   ", maxPrice=" + maxPrice +
                   '}';
        }
    }
}
```

## 工具执行

### AI Services 集成

使用 `AiServices` 创建带工具的服务：

```java
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.AiMessage;

/**
 * AI 助手服务接口
 */
public interface Assistant {

    String chat(@UserMessage String message);
}

/**
 * 带工具的 AI 助手
 */
public class ToolAssistant {

    private final ChatModel model;
    private final Calculator calculator;

    public ToolAssistant() {
        // 创建模型
        this.model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();
        
        // 创建工具实例
        this.calculator = new Calculator();
    }

    /**
     * 创建 AI 助手
     */
    public Assistant createAssistant() {
        return AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(calculator)  // 注册工具
                .build();
    }

    /**
     * 工具类 - 计算器
     */
    public static class Calculator {

        @Tool("计算两个数的和")
        public String add(int a, int b) {
            return String.valueOf(a + b);
        }

        @Tool("计算两个数的差")
        public String subtract(int a, int b) {
            return String.valueOf(a - b);
        }

        @Tool("计算两个数的积")
        public String multiply(int a, int b) {
            return String.valueOf(a * b);
        }

        public static void main(String[] args) {
            ToolAssistant assistant = new ToolAssistant();
            Assistant aiAssistant = assistant.createAssistant();

            // 测试工具调用
            System.out.println("=== 工具调用测试 ===");
            System.out.println();

            String response1 = aiAssistant.chat("2 + 2 等于几？");
            System.out.println("用户: 2 + 2 等于几？");
            System.out.println("AI: " + response1);
            System.out.println();

            String response2 = aiAssistant.chat("100 减 50 等于几？");
            System.out.println("用户: 100 减 50 等于几？");
            System.out.println("AI: " + response2);
            System.out.println();

            String response3 = aiAssistant.chat("5 乘以 8 等于几？");
            System.out.println("用户: 5 乘以 8 等于几？");
            System.out.println("AI: " + response3);
        }
    }
}
```

### 工具执行流程

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.*;
import dev.langchain4j.data.message.tool.ToolExecutionRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * 工具执行流程示例
 */
public class ToolExecutionFlow {

    private final ChatModel model;
    private final WeatherService weatherService;

    public ToolExecutionFlow() {
        this.model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .tools(new WeatherService())
                .build();
        
        this.weatherService = new WeatherService();
    }

    /**
     * 对话并自动处理工具调用
     */
    public String chat(String userMessage) {
        // 1. 发送消息给 LLM
        ChatResponse response = model.chat(userMessage);

        // 2. 检查是否有工具调用请求
        if (response.aiMessage().hasToolExecutionRequests()) {
            List<ToolExecutionRequest> toolRequests = 
                response.aiMessage().toolExecutionRequests();

            System.out.println("LLM 请求调用工具: " + toolRequests.size());
            
            // 3. 执行工具
            List<ToolExecutionResultMessage> toolResults = new ArrayList<>();
            for (ToolExecutionRequest request : toolRequests) {
                System.out.println("  执行工具: " + request.name() + 
                               "(" + request.arguments() + ")");
                
                String result = executeTool(request);
                
                toolResults.add(ToolExecutionResultMessage.from(
                    request.id(),
                    request.name(),
                    result
                ));
            }

            // 4. 将工具结果返回给 LLM
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(UserMessage.from(userMessage));
            messages.add(response.aiMessage());
            messages.addAll(toolResults);

            ChatResponse finalResponse = model.chat(messages);
            return finalResponse.aiMessage().text();
        }

        // 没有工具调用，直接返回
        return response.aiMessage().text();
    }

    /**
     * 执行工具
     */
    private String executeTool(ToolExecutionRequest request) {
        return switch (request.name()) {
            case "getWeather" -> weatherService.getWeather(
                extractCity(request.arguments())
            );
            case "getCurrentDate" -> weatherService.getCurrentDate();
            case "getTemperature" -> weatherService.getTemperature(
                extractCity(request.arguments())
            );
            default -> "未知的工具: " + request.name();
        };
    }

    private String extractCity(String arguments) {
        // 简化：从 JSON 中提取城市名称
        // 实际应用中应该使用 JSON 解析库
        if (arguments.contains("北京")) {
            return "北京";
        } else if (arguments.contains("上海")) {
            return "上海";
        }
        return "未知城市";
    }

    /**
     * 天气服务
     */
    public static class WeatherService {

        @Tool("获取当前日期")
        public String getCurrentDate() {
            return java.time.LocalDate.now().toString();
        }

        @Tool("获取天气信息")
        public String getWeather(String city) {
            return switch (city) {
                case "北京" -> "北京今天晴，温度 25°C";
                case "上海" -> "上海今天多云，温度 22°C";
                case "广州" -> "广州今天阴，温度 26°C";
                case "深圳" -> "深圳今天小雨，温度 24°C";
                default -> "抱歉，暂无 " + city + " 的天气信息";
            };
        }

        @Tool("获取温度")
        public String getTemperature(String city) {
            return switch (city) {
                case "北京" -> "25°C";
                case "上海" -> "22°C";
                case "广州" -> "26°C";
                case "深圳" -> "24°C";
                default -> "未知城市";
            };
        }
    }

    public static void main(String[] args) {
        ToolExecutionFlow chat = new ToolExecutionFlow();

        // 测试 1：不需要工具调用
        System.out.println("=== 测试 1：普通对话 ===");
        String response1 = chat.chat("你好");
        System.out.println("用户: 你好");
        System.out.println("AI: " + response1);
        System.out.println();

        // 测试 2：需要工具调用
        System.out.println("=== 测试 2：天气查询 ===");
        String response2 = chat.chat("北京今天天气怎么样？");
        System.out.println("用户: 北京今天天气怎么样？");
        System.out.println("AI: " + response2);
        System.out.println();

        // 测试 3：多工具调用
        System.out.println("=== 测试 3：多工具调用 ===");
        String response3 = chat.chat("现在几点了？北京温度多少？");
        System.out.println("用户: 现在几点了？北京温度多少？");
        System.out.println("AI: " + response3);
    }
}
```

## 完整示例：智能客服机器人

```java
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.data.message.*;
import dev.langchain4j.data.message.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.tool.ToolExecutionResultMessage;

import java.util.*;

/**
 * 智能客服机器人
 */
public class SmartCustomerServiceBot {

    private final ChatModel model;
    private final ProductCatalog productCatalog;
    private final OrderService orderService;

    public SmartCustomerServiceBot() {
        this.model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .tools(new ProductCatalog(), new OrderService())
                .build();
        
        this.productCatalog = new ProductCatalog();
        this.orderService = new OrderService();
    }

    /**
     * 创建 AI 助手
     */
    public Assistant createAssistant() {
        return AiServices.builder(Assistant.class)
                .chatModel(model)
                .systemMessageProvider(chatMemoryId -> "你是一个友好的客服助手，可以帮助用户查询产品信息和创建订单")
                .build();
    }

    /**
     * AI 助手接口
     */
    public interface Assistant {
        String chat(@UserMessage String message);
    }

    /**
     * 产品目录工具
     */
    public static class ProductCatalog {

        private final Map<String, Product> products = new HashMap<>();

        public ProductCatalog() {
            // 初始化产品数据
            products.put("p001", new Product("p001", "iPhone 15", 6999.0, "智能手机"));
            products.put("p002", new Product("p002", "MacBook Pro", 14999.0, "笔记本电脑"));
            products.put("p003", new Product("p003", "AirPods Pro", 1999.0, "耳机"));
            products.put("p004", new Product("p004", "Apple Watch", 3299.0, "智能手表"));
            products.put("p005", new Product("p005", "iPad Air", 4799.0, "平板"));
        }

        @Tool("搜索产品")
        public String searchProducts(String keyword) {
            System.out.println("[工具] 搜索产品: " + keyword);
            
            List<Product> results = new ArrayList<>();
            for (Product product : products.values()) {
                if (product.getName().toLowerCase().contains(keyword.toLowerCase()) ||
                    product.getCategory().toLowerCase().contains(keyword.toLowerCase())) {
                    results.add(product);
                }
            }

            if (results.isEmpty()) {
                return "未找到匹配的产品";
            }

            StringBuilder sb = new StringBuilder("找到 " + results.size() + " 个产品:\n");
            for (Product product : results) {
                sb.append("- ").append(product.getName())
                      .append(" (¥").append(product.getPrice()).append(")")
                      .append("\n");
            }
            return sb.toString();
        }

        @Tool("获取产品详情")
        public String getProductDetails(String productId) {
            System.out.println("[工具] 获取产品详情: " + productId);
            
            Product product = products.get(productId);
            if (product == null) {
                return "产品 ID " + productId + " 不存在";
            }

            return String.format("产品名称: %s\n价格: ¥%.2f\n分类: %s",
                    product.getName(),
                    product.getPrice(),
                    product.getCategory());
        }

        @Tool("按分类获取产品")
        public String getProductsByCategory(String category) {
            System.out.println("[工具] 获取分类产品: " + category);
            
            List<Product> results = new ArrayList<>();
            for (Product product : products.values()) {
                if (category.equalsIgnoreCase(product.getCategory())) {
                    results.add(product);
                }
            }

            if (results.isEmpty()) {
                return "分类 " + category + " 下没有产品";
            }

            StringBuilder sb = new StringBuilder(category + " 分类下有 " + results.size() + " 个产品:\n");
            for (Product product : results) {
                sb.append("- ").append(product.getName())
                      .append(" (¥").append(product.getPrice()).append(")")
                      .append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * 订单服务工具
     */
    public static class OrderService {

        private final Map<String, Order> orders = new HashMap<>();

        @Tool("创建订单")
        public String createOrder(String productId, int quantity, String address) {
            System.out.println("[工具] 创建订单: " + productId + ", 数量: " + quantity + ", 地址: " + address);

            // 简化：实际应用中应该有更复杂的验证
            String orderId = "order_" + System.currentTimeMillis();
            Order order = new Order(orderId, productId, quantity, address, "已创建");
            orders.put(orderId, order);

            return "订单创建成功！\n订单号: " + orderId + 
                   "\n产品 ID: " + productId + 
                   "\n数量: " + quantity + 
                   "\n配送地址: " + address;
        }

        @Tool("查询订单状态")
        public String getOrderStatus(String orderId) {
            System.out.println("[工具] 查询订单状态: " + orderId);

            Order order = orders.get(orderId);
            if (order == null) {
                return "订单 " + orderId + " 不存在";
            }

            return "订单 " + orderId + " 的状态是: " + order.getStatus();
        }

        @Tool("取消订单")
        public String cancelOrder(String orderId) {
            System.out.println("[工具] 取消订单: " + orderId);

            Order order = orders.get(orderId);
            if (order == null) {
                return "订单 " + orderId + " 不存在";
            }

            if ("已取消".equals(order.getStatus())) {
                return "订单 " + orderId + " 已经取消";
            }

            order.setStatus("已取消");
            return "订单 " + orderId + " 已取消";
        }
    }

    /**
     * 数据类
     */
    public static class Product {
        private final String id;
        private final String name;
        private final double price;
        private final String category;

        public Product(String id, String name, double price, String category) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.category = category;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public double getPrice() { return price; }
        public String getCategory() { return category; }
    }

    public static class Order {
        private final String id;
        private final String productId;
        private final int quantity;
        private final String address;
        private String status;

        public Order(String id, String productId, int quantity, String address, String status) {
            this.id = id;
            this.productId = productId;
            this.quantity = quantity;
            this.address = address;
            this.status = status;
        }

        public String getId() { return id; }
        public String getProductId() { return productId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static void main(String[] args) {
        SmartCustomerServiceBot bot = new SmartCustomerServiceBot();
        Assistant assistant = bot.createAssistant();

        System.out.println("═══════════════════════════════════════════════╗");
        System.out.println("║       智能客服机器人                        ║");
        System.out.println("╚═══════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("使用提示:");
        System.out.println("  - 可以搜索产品（如：搜索手机）");
        System.out.println("  - 可以查看产品详情（如：详情 p001）");
        System.out.println("  - 可以创建订单（如：订单 p001 1 北京）");
        System.out.println("  - 可以查询订单状态（如：订单 order_xxx）");
        System.out.println("  - 可以取消订单（如：取消 order_xxx）");
        System.out.println("  - 输入 'exit' 退出");
        System.out.println();

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("用户: ");
            String input = scanner.nextLine().trim();

            if ("exit".equalsIgnoreCase(input)) {
                System.out.println("\n感谢使用，再见！");
                break;
            }

            if (input.isEmpty()) {
                continue;
            }

            String response = assistant.chat(input);
            System.out.println("客服: " + response);
            System.out.println();
        }

        scanner.close();
    }
}
```

## 测试代码示例

```java
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tool 调用测试
 */
class ToolCallingTest {

    private ChatModel model;

    @BeforeEach
    void setUp() {
        this.model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();
    }

    @Test
    void should_call_tool_when_necessary() {
        // 创建简单的工具
        TestTool tool = new TestTool();
        
        ChatModel modelWithTool = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .tools(tool)
                .build();

        // 测试需要工具调用的情况
        String query = "5 加 3 等于几？";
        ChatResponse response = modelWithTool.chat(query);

        assertNotNull(response);
        assertTrue(response.aiMessage().hasToolExecutionRequests() || 
                   response.aiMessage().text().contains("8"));
        
        System.out.println("查询: " + query);
        if (response.aiMessage().hasToolExecutionRequests()) {
            System.out.println("工具调用: " + 
                response.aiMessage().toolExecutionRequests().get(0).name());
        }
        System.out.println("响应: " + response.aiMessage().text());
    }

    @Test
    void should_not_call_tool_when_not_needed() {
        TestTool tool = new TestTool();
        
        ChatModel modelWithTool = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .tools(tool)
                .build();

        // 测试不需要工具调用的情况
        String query = "你好";
        ChatResponse response = modelWithTool.chat(query);

        assertNotNull(response);
        assertFalse(response.aiMessage().hasToolExecutionRequests());
        
        System.out.println("查询: " + query);
        System.out.println("响应: " + response.aiMessage().text());
    }

    @Test
    void should_handle_tool_execution() {
        TestTool tool = new TestTool();
        
        ChatModel modelWithTool = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .tools(tool)
                .logRequests(true)
                .logResponses(true)
                .build();

        // 测试工具执行
        String query = "计算 2 加 3";
        ChatResponse response1 = modelWithTool.chat(query);

        if (response1.aiMessage().hasToolExecutionRequests()) {
            // 执行工具并返回结果
            ToolExecutionResultMessage toolResult = 
                ToolExecutionResultMessage.from(
                    response1.aiMessage().toolExecutionRequests().get(0).id(),
                    response1.aiMessage().toolExecutionRequests().get(0).name(),
                    "5"  // 工具执行结果
                );
            
            // 发送工具结果给 LLM
            ChatResponse response2 = modelWithTool.chat(
                UserMessage.from(query),
                response1.aiMessage(),
                toolResult
            );
            
            assertNotNull(response2);
            assertTrue(response2.aiMessage().text().contains("5"));
            
            System.out.println("查询: " + query);
            System.out.println("工具执行结果: " + toolResult.text());
            System.out.println("最终响应: " + response2.aiMessage().text());
        }
    }

    @Test
    void should_support_multiple_tools() {
        // 创建多个工具
        Tool tool1 = new TestTool();
        Tool tool2 = new DateTool();
        
        ChatModel modelWithTools = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .tools(tool1, tool2)
                .build();

        String query = "5 加 3 等于几？";
        ChatResponse response = modelWithTools.chat(query);

        assertNotNull(response);
        assertTrue(response.aiMessage().hasToolExecutionRequests());
        
        System.out.println("查询: " + query);
        System.out.println("工具调用: " + 
            response.aiMessage().toolExecutionRequests().get(0).name());
        System.out.println("响应: " + response.aiMessage().text());
    }

    /**
     * 测试工具
     */
    public static class TestTool {

        @Tool("计算两个数的和")
        public String add(int a, int b) {
            return String.valueOf(a + b);
        }
    }

    /**
     * 日期工具
     */
    public static class DateTool {

        @Tool("获取当前日期")
        public String getCurrentDate() {
            return java.time.LocalDate.now().toString();
        }
    }
}
```

## 实践练习

### 练习 1：创建数据库查询工具

实现一个查询数据库的工具：

```java
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolParam;

import java.util.*;
import java.sql.*;

/**
 * 数据库查询工具
 */
public class DatabaseQueryTool {

    private final Connection dbConnection;

    public DatabaseQueryTool(String dbUrl, String username, String password) {
        try {
            this.dbConnection = DriverManager.getConnection(dbUrl, username, password);
            System.out.println("数据库连接成功");
            createTableIfNotExists();
            insertSampleData();
            System.out.println("数据库初始化完成");
        } catch (SQLException e) {
            throw new RuntimeException("数据库连接失败", e);
        }
    }

    @Tool("查询用户信息")
    public String getUserInfo(
            @ToolParam("用户 ID") String userId
    ) {
        System.out.println("[工具] 查询用户: " + userId);
        
        try {
            String sql = "SELECT * FROM users WHERE id = ?";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setString(1, userId);
                
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    return String.format(
                        "用户 ID: %s\n姓名: %s\n年龄: %d\n邮箱: %s\n部门: %s",
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getInt("age"),
                        rs.getString("email"),
                        rs.getString("department")
                    );
                } else {
                    return "用户 ID " + userId + " 不存在";
                }
            }
        } catch (SQLException e) {
            return "查询失败: " + e.getMessage();
        }
    }

    @Tool("查询部门用户")
    public String getUsersByDepartment(
            @ToolParam("部门名称") String department
    ) {
        System.out.println("[工具] 查询部门用户: " + department);
        
        try {
            String sql = "SELECT * FROM users WHERE department = ? ORDER BY age DESC";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setString(1, department);
                
                ResultSet rs = stmt.executeQuery();
                
                List<String> users = new ArrayList<>();
                while (rs.next()) {
                    users.add(String.format("%s (年龄: %d)", 
                        rs.getString("name"),
                        rs.getInt("age")));
                }
                
                if (users.isEmpty()) {
                    return "部门 " + department + " 没有用户";
                }
                
                return "部门 " + department + " 有 " + users.size() + " 位用户:\n" + 
                       String.join("\n", users);
            }
        } catch (SQLException e) {
            return "查询失败: " + e.getMessage();
        }
    }

    @Tool("添加新用户")
    public String addUser(
            @ToolParam("用户 ID") String id,
            @ToolParam("姓名") String name,
            @ToolParam("年龄") int age,
            @ToolParam("邮箱") String email,
            @ToolParam("部门") String department
    ) {
        System.out.println("[工具] 添加用户: " + name);
        
        try {
            String sql = "INSERT INTO users (id, name, age, email, department) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setString(1, id);
                stmt.setString(2, name);
                stmt.setInt(3, age);
                stmt.setString(4, email);
                stmt.setString(5, department);
                
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    return "用户 " + name + " 添加成功";
                } else {
                    return "用户添加失败";
                }
            }
        } catch (SQLException e) {
            return "添加失败: " + e.getMessage();
        }
    }

    @Tool("获取用户统计")
    public String getUserStats() {
        System.out.println("[工具] 获取用户统计");
        
        try {
            String sql = "SELECT department, COUNT(*) as count FROM users GROUP BY department";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                
                Map<String, Integer> stats = new LinkedHashMap<>();
                int totalCount = 0;
                
                while (rs.next()) {
                    String dept = rs.getString("department");
                    int count = rs.getInt("count");
                    stats.put(dept, count);
                    totalCount += count;
                }
                
                StringBuilder sb = new StringBuilder();
                sb.append("用户统计（总计 ").append(totalCount).append(" 人）:\n");
                
                for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                    sb.append("  ").append(entry.getKey())
                          .append(": ").append(entry.getValue()).append(" 人\n");
                }
                
                return sb.toString();
            }
        } catch (SQLException e) {
            return "查询失败: " + e.getMessage();
        }
    }

    private void createTableIfNotExists() {
        try {
            String sql = "CREATE TABLE IF NOT EXISTS users (" +
                           "id VARCHAR(20) PRIMARY KEY," +
                           "name VARCHAR(50)," +
                           "age INT," +
                           "email VARCHAR(100)," +
                           "department VARCHAR(50))";
            
            try (Statement stmt = dbConnection.createStatement()) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            System.err.println("创建表失败: " + e.getMessage());
        }
    }

    private void insertSampleData() {
        String[] users = {
            "u001,张三,25,zhangsan@example.com,技术部",
            "u002,李四,30,lisi@example.com,技术部",
            "u003,王五,28,wangwu@example.com,市场部",
            "u004,赵六,32,zhaoliu@example.com,市场部",
            "u005,钱七,27,qianqi@example.com,人事部"
        };

        for (String user : users) {
            String[] parts = user.split(",");
            if (parts.length == 5) {
                try {
                    String sql = "INSERT INTO users (id, name, age, email, department) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                        stmt.setString(1, parts[0]);
                        stmt.setString(2, parts[1]);
                        stmt.setInt(3, Integer.parseInt(parts[2]));
                        stmt.setString(4, parts[3]);
                        stmt.setString(5, parts[4]);
                        stmt.executeUpdate();
                    }
                } catch (SQLException e) {
                    System.err.println("插入数据失败: " + e.getMessage());
                }
            }
        }
    }

    public void close() {
        try {
            if (dbConnection != null && !dbConnection.isClosed()) {
                dbConnection.close();
            }
        } catch (SQLException e) {
            System.err.println("关闭数据库失败: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // 使用 H2 内存数据库进行测试
        DatabaseQueryTool tool = new DatabaseQueryTool(
            "jdbc:h2:~/test", "sa", ""
        );

        // 测试工具方法
        System.out.println(tool.getUserInfo("u001"));
        System.out.println();
        System.out.println(tool.getUsersByDepartment("技术部"));
        System.out.println();
        System.out.println(tool.getUserStats());

        tool.close();
    }
}
```

## 总结

### 本章要点

1. **工具调用概念**
   - 允许 LLM 调用外部函数或 API
   - LLM 自动决策何时调用工具
   - 工具执行结果返回给 LLM

2. **@Tool 注解**
   - 标记方法为工具
   - 可以提供名称和描述
   - 支持 `@ToolParam` 标记参数

3. **工具参数**
   - 支持基本类型和复杂类型
   - 参数可以有默认值
   - 自动提取和验证参数

4. **工具执行**
   - LLM 生成工具调用请求
   - 执行工具并获取结果
   - 将结果返回给 LLM

5. **AI Services 集成**
   - 使用 `tools()` 方法注册工具
   - 自动处理工具调用流程
   - 简化工具使用

### 下一步

在下一章节中，我们将学习：
- AI Services 高级特性
- 多轮对话中的工具调用
- 并行工具调用
- 工具的错误处理和重试
- 自定义工具调用逻辑

### 常见问题

**Q1：@Tool 和 @ToolParam 有什么区别？**

A：
- `@Tool` - 标记方法为工具，可以提供名称和描述
- `@ToolParam` - 标记方法参数，可以提供名称和默认值

**Q2：工具可以调用其他工具吗？**

A：可以，但需要谨慎设计以避免无限循环。建议将工具设计为独立的、原子性的操作。

**Q3：如果工具执行失败会怎样？**

A：工具执行的错误会返回给 LLM，LLM 可以根据错误生成适当的响应或尝试其他方法。建议在工具中实现良好的错误处理和验证。

**Q4：工具可以有多个返回值吗？**

A：工具方法必须返回单个值（通常是 String），但这个值可以包含复杂的数据结构（如 JSON）。

**Q5：如何测试工具？**

A：
1. 单元测试工具方法本身
2. 集成测试整个工具调用流程
3. 手动模拟 LLM 的工具调用请求
4. 使用模拟数据进行测试

## 参考资料

- [LangChain4j Tools 文档](https://docs.langchain4j.dev/tutorials/tools)
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
