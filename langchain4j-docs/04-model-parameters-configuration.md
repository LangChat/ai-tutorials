---
title: '模型参数配置'
description: '学习 LangChain4j 的 模型参数配置 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 04 - 模型参数配置

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 理解核心模型参数的含义和作用
- 掌握 temperature、topP、topK 等参数的配置
- 学会使用停止序列和输出长度限制
- 掌握频率惩罚和存在惩罚的使用
- 了解如何优化参数以获得最佳效果

## 前置知识

- 完成《01 - LangChain4j 简介》章节
- 完成《02 - 你的第一个 Chat 应用》章节
- 完成《03 - 深入理解 ChatModel》章节
- 了解基本的概率概念

## 核心概念

### 为什么需要参数配置？

大语言模型（LLM）的输出质量和风格可以通过各种参数来控制。合理的参数配置可以让模型：

1. **更符合预期** - 控制输出的风格和确定性
2. **更高效** - 避免不必要的 Token 消耗
3. **更安全** - 限制输出长度和内容
4. **更有创意** - 或者更确定，根据需求调整

**类比理解：**
如果把 LLM 比作一个画家，参数就像是画家的画笔和颜料配置：
- 粗画笔（低 temperature）→ 精确、确定
- 细画笔（高 temperature）→ 有创意、多样化
- 限制画布大小（maxTokens）→ 控制作品尺寸
- 添加特殊颜料（其他参数）→ 改变风格

## 核心参数详解

### 1. Temperature（温度）

**什么是 Temperature？**

Temperature 控制模型输出的随机性和创造性。

```java
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.ChatModel;

ChatModel model = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .temperature(0.7)  // 设置温度
        .build();
```

**取值范围：0.0 - 2.0**

| Temperature 值 | 效果 | 适用场景 |
|----------------|------|----------|
| 0.0 | 完全确定，相同输入总是相同输出 | 数学计算、代码生成、需要精确答案的场景 |
| 0.1 - 0.3 | 高度确定，几乎无随机性 | 技术文档、数据分析、API 调用 |
| 0.4 - 0.6 | 适度确定，轻微变化 | 大多数量应用、问答系统 |
| 0.7 | 平衡（默认） | 平衡创造性和一致性 |
| 0.8 - 1.0 | 适度创造性 | 创意写作、故事生成、头脑风暴 |
| 1.0 - 2.0 | 高创造性 | 诗歌创作、创意写作、实验性应用 |

**示例对比：**

```java
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.ChatModel;

public class TemperatureExample {

    public static void main(String[] args) {
        String prompt = "写一个关于春天的短句";
        
        // 低温度 - 确定
        ChatModel lowTempModel = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .temperature(0.2)
                .build();
        
        // 高温度 - 创造性
        ChatModel highTempModel = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .temperature(1.2)
                .build();
        
        System.out.println("=== 温度 0.2（确定）===");
        for (int i = 0; i < 3; i++) {
            System.out.println(lowTempModel.chat(prompt));
        }
        
        System.out.println("\n=== 温度 1.2（创造性）===");
        for (int i = 0; i < 3; i++) {
            System.out.println(highTempModel.chat(prompt));
        }
    }
}
```

**注意事项：**
- 温度设置为 0 时，模型会贪婪采样，总是选择概率最高的词
- 温度越高，输出越不可预测
- 对于需要一致性的任务（如代码生成），使用低温度
- 对于需要创造性的任务（如创意写作），使用高温度

### 2. Top P（核采样）

**什么是 Top P？**

Top P（Nucleus Sampling）限制模型只从累积概率达到某个阈值的词汇中选择。

```java
ChatModel model = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .topP(0.9)  // 核采样
        .build();
```

**取值范围：0.0 - 1.0**

| Top P 值 | 效果 |
|-----------|------|
| 0.1 | 只从概率最高的 10% 词汇中选择，非常确定 |
| 0.5 | 从概率最高的 50% 词汇中选择，适度确定 |
| 0.9（推荐）| 从概率最高的 90% 词汇中选择，平衡多样性 |
| 1.0 | 从所有词汇中选择，完全开放 |

**Top P vs Temperature：**

- **Temperature**：调整整个概率分布的平滑程度
- **Top P**：只考虑概率最高的部分词汇

**使用建议：**
- 通常 Top P 设置为 0.9 或 1.0
- 可以与 Temperature 结合使用
- 对于需要精确输出的场景，可以降低 Top P

### 3. Top K（Top K 采样）

**什么是 Top K？**

Top K 只从概率最高的 K 个词汇中选择。

```java
ChatModel model = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .topK(40)  // 从概率最高的 40 个词中选择
        .build();
```

**使用建议：**
- OpenAI 模型通常使用 Top P 而不是 Top K
- 其他模型提供商可能支持 Top K
- 典型值：40、50、100

**Top P vs Top K：**

- **Top P**：基于累积概率，更灵活
- **Top K**：固定数量词汇，更简单

### 4. Max Tokens（最大输出 Token 数）

**什么是 Max Tokens？**

限制模型输出的最大 Token 数量。

```java
import dev.langchain4j.model.openai.OpenAiChatModel;

ChatModel model = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .maxTokens(500)  // 最多输出 500 个 Token
        .build();
```

**为什么使用 Max Tokens？**

1. **成本控制** - 限制输出 Token 数，控制费用
2. **响应时间** - 减少生成时间
3. **输出长度** - 确保输出不会过长
4. **API 限制** - 某些 API 有 Token 限制

**不同场景的建议值：**

| 场景 | 建议值 | 说明 |
|------|---------|------|
| 简短问答 | 100 - 300 | 短答案即可 |
| 代码片段 | 200 - 500 | 代码通常不长 |
| 技术文档 | 300 - 800 | 中等长度 |
| 文章段落 | 500 - 1000 | 需要详细内容 |
| 长篇创作 | 1000 - 4000 | 需要完整内容 |

**示例：**

```java
// 简短回答
ChatModel shortAnswer = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .maxTokens(100)
        .build();

// 详细解释
ChatModel detailedAnswer = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .maxTokens(2000)
        .build();

String question = "什么是 Java？";

System.out.println("简短回答:");
System.out.println(shortAnswer.chat(question));

System.out.println("\n详细解释:");
System.out.println(detailedAnswer.chat(question));
```

### 5. Stop Sequences（停止序列）

**什么是 Stop Sequences？**

指定某些字符串，当模型输出这些字符串时停止生成。

```java
ChatModel model = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .stopSequences("\n\n", "---", "END")
        .build();
```

**使用场景：**

1. **结构化输出** - 在特定标记处停止
2. **问答格式** - 在答案后停止
3. **防止冗长** - 避免不必要的内容

**示例：**

```java
ChatModel model = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .stopSequences("---END---")
        .build();

String response = model.chat(
    "用以下格式回答：\n" +
    "问题：什么是 Java？\n" +
    "答案：[你的答案]\n" +
    "---END---"
);

System.out.println(response);
// 模型会在输出 ---END--- 后停止
```

### 6. Frequency Penalty（频率惩罚）

**什么是 Frequency Penalty？**

频率惩罚降低重复词语出现的概率，鼓励模型使用更多样化的词汇。

```java
ChatModel model = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .frequencyPenalty(0.5)  // 频率惩罚
        .build();
```

**取值范围：-2.0 到 2.0**

| Frequency Penalty 值 | 效果 |
|---------------------|------|
| -2.0 | 鼓励重复，增加词语出现频率 |
| 0.0 | 无惩罚（默认） |
| 0.1 - 0.5 | 轻微惩罚，减少轻微重复 |
| 0.6 - 1.0 | 中等惩罚，适度减少重复 |
| 1.1 - 2.0 | 强惩罚，显著减少重复 |

**使用场景：**
- 避免模型重复相同内容
- 鼓励更丰富的词汇表达
- 创意写作时通常使用较高值

**示例：**

```java
// 无频率惩罚
ChatModel noPenalty = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .frequencyPenalty(0.0)
        .build();

// 有频率惩罚
ChatModel withPenalty = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .frequencyPenalty(1.0)
        .build();

String prompt = "写一段关于编程的文字";

System.out.println("无频率惩罚:");
System.out.println(noPenalty.chat(prompt));

System.out.println("\n有频率惩罚:");
System.out.println(withPenalty.chat(prompt));
```

### 7. Presence Penalty（存在惩罚）

**什么是 Presence Penalty？**

存在惩罚降低已经出现过的词再次出现的概率，无论它们出现了多少次。

```java
ChatModel model = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .presencePenalty(0.5)  // 存在惩罚
        .build();
```

**取值范围：-2.0 到 2.0**

**Frequency Penalty vs Presence Penalty：**

| 特性 | Frequency Penalty | Presence Penalty |
|------|------------------|-----------------|
| 重复惩罚 | 与出现次数成比例 | 与出现次数无关 |
| 效果 | 减少频繁重复 | 避免重复提及 |
| 适用 | 避免过度使用某些词 | 避免重复相同概念 |

**使用建议：**
- 通常 Presence Penalty 设置为 0.1 - 0.5
- 对于需要多样性的场景使用
- 可以与 Frequency Penalty 结合使用

### 8. Logit Bias（Logit 偏置）

**什么是 Logit Bias？**

Logit Bias 允置特定 token 的出现概率。

```java
import java.util.Map;

ChatModel model = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .logitBias(Map.of(
            1000, 5.0,   // 增加词 ID 1000 的概率
            2000, -10.0  // 减少词 ID 2000 的概率
        ))
        .build();
```

**取值范围：-100 到 100**

| Logit Bias 值 | 效果 |
|---------------|------|
| -100 | 完全禁止该 token 出现 |
| -10 到 -1 | 显著降低出现概率 |
| -1 到 1 | 轻微影响 |
| 1 到 10 | 显著提高出现概率 |
| 100 | 强制该 token 出现 |

**使用场景：**
- 避免某些词汇出现
- 鼓励特定风格或术语
- 内容过滤和合规

**注意：** 需要知道 token 的 ID，这通常需要通过 tokenizer 工具获取。

## 参数组合策略

### 场景一：精确问答

```java
ChatModel factualQAModel = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .temperature(0.1)          // 低温度，确保确定性
        .topP(0.9)                 // 标准核采样
        .maxTokens(300)             // 适度长度
        .build();
```

**适用：**
- 事实性问题
- 技术文档
- API 调用
- 数据分析

### 场景二：创意写作

```java
ChatModel creativeModel = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .temperature(0.9)          // 高温度，增加创造性
        .topP(0.95)                // 允许更多样化
        .frequencyPenalty(0.5)      // 减少重复
        .presencePenalty(0.3)       // 避免重复概念
        .maxTokens(1000)            // 充足长度
        .build();
```

**适用：**
- 故事创作
- 诗歌写作
- 创意头脑风暴
- 艺术生成

### 场景三：代码生成

```java
ChatModel codeModel = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .temperature(0.2)          // 低温度，确保正确性
        .topP(0.95)                // 允许适当的词汇选择
        .maxTokens(800)             // 代码通常需要一定长度
        .stopSequences("\n\n", "###")  // 在特定标记停止
        .build();
```

**适用：**
- 代码生成
- 函数编写
- 算法实现
- 代码解释

### 场景四：聊天机器人

```java
ChatModel chatModel = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .temperature(0.7)          // 平衡温度
        .topP(0.9)                 // 标准核采样
        .frequencyPenalty(0.3)      // 轻微减少重复
        .presencePenalty(0.1)       // 避免过度重复
        .maxTokens(500)             // 适度长度
        .build();
```

**适用：**
- 客服聊天
- 虚拟助手
- 日常对话
- 咨询问答

### 场景五：摘要生成

```java
ChatModel summaryModel = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4o-mini")
        .temperature(0.3)          // 较低温度，确保准确性
        .topP(0.9)                 // 标准核采样
        .maxTokens(200)             // 摘要通常较短
        .build();
```

**适用：**
- 文章摘要
- 文档总结
- 会议纪要
- 关键信息提取

## 完整示例：参数对比工具

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * 参数对比工具 - 比较不同参数配置的效果
 */
public class ParameterComparisonTool {

    public static void main(String[] args) {
        String prompt = "写一个关于编程的短句";
        
        // 配置 1：确定模式
        ChatModel deterministic = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .temperature(0.1)
                .topP(0.9)
                .maxTokens(100)
                .build();
        
        // 配置 2：平衡模式
        ChatModel balanced = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .temperature(0.7)
                .topP(0.9)
                .maxTokens(100)
                .build();
        
        // 配置 3：创意模式
        ChatModel creative = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .temperature(1.2)
                .topP(0.95)
                .frequencyPenalty(0.5)
                .maxTokens(100)
                .build();
        
        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║       LLM 参数配置对比工具                 ║");
        System.out.println("╚════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("提示词: " + prompt);
        System.out.println();
        
        // 测试每种配置 3 次
        for (int i = 1; i <= 3; i++) {
            System.out.println("═══ 第 " + i + " 次测试 ═");
            System.out.println();
            
            System.out.println("【确定模式】Temperature=0.1");
            System.out.println(deterministic.chat(prompt));
            System.out.println();
            
            System.out.println("【平衡模式】Temperature=0.7");
            System.out.println(balanced.chat(prompt));
            System.out.println();
            
            System.out.println("【创意模式】Temperature=1.2");
            System.out.println(creative.chat(prompt));
            System.out.println();
            System.out.println("─────────────────────────────────────────────────");
            System.out.println();
        }
    }
}
```

## 测试代码示例

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelParametersTest {

    @Test
    void should_respond_deterministically_with_low_temperature() {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .temperature(0.0)
                .build();
        
        String prompt = "2 + 2 等于几？";
        String response1 = model.chat(prompt);
        String response2 = model.chat(prompt);
        
        assertEquals(response1, response2, 
            "温度 0 时，相同输入应该产生相同输出");
        System.out.println("确定性输出: " + response1);
    }

    @Test
    void should_have_different_outputs_with_high_temperature() {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .temperature(1.0)
                .build();
        
        String prompt = "写一个关于春天的词";
        String response1 = model.chat(prompt);
        String response2 = model.chat(prompt);
        
        assertNotEquals(response1, response2, 
            "高温度时，相同输入应该产生不同输出");
        System.out.println("输出 1: " + response1);
        System.out.println("输出 2: " + response2);
    }

    @Test
    void should_respect_max_tokens_limit() {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .maxTokens(50)
                .build();
        
        String prompt = "写一个关于编程的故事，至少 500 字";
        String response = model.chat(prompt);
        
        // 简单检查：响应不应该特别长
        assertTrue(response.length() < 500, 
            "响应长度应该在 maxTokens 限制内");
        System.out.println("长度限制响应 (" + response.length() + " chars): " + 
            response.substring(0, Math.min(100, response.length())));
    }

    @Test
    void should_respect_stop_sequences() {
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .stopSequences("---END---")
                .build();
        
        String prompt = "以 ---END--- 结尾的句子";
        String response = model.chat(prompt);
        
        assertFalse(response.contains("---END---"), 
            "响应中不应包含停止序列");
        System.out.println("停止序列响应: " + response);
    }
}
```

## 实践练习

### 练习 1：创建参数配置工具

创建一个工具类，封装常用配置：

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * 模型配置工厂 - 提供常用配置
 */
public class ModelConfigFactory {

    /**
     * 创建精确问答模型
     */
    public static ChatModel createFactualModel() {
        return OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .temperature(0.1)
                .topP(0.9)
                .maxTokens(300)
                .build();
    }

    /**
     * 创建聊天模型
     */
    public static ChatModel createChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .temperature(0.7)
                .topP(0.9)
                .frequencyPenalty(0.3)
                .presencePenalty(0.1)
                .maxTokens(500)
                .build();
    }

    /**
     * 创建创意写作模型
     */
    public static ChatModel createCreativeModel() {
        return OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .temperature(0.9)
                .topP(0.95)
                .frequencyPenalty(0.5)
                .presencePenalty(0.3)
                .maxTokens(1000)
                .build();
    }

    /**
     * 创建代码生成模型
     */
    public static ChatModel createCodeModel() {
        return OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .temperature(0.2)
                .topP(0.95)
                .maxTokens(800)
                .stopSequences("\n\n", "###")
                .build();
    }

    /**
     * 创建摘要模型
     */
    public static ChatModel createSummaryModel() {
        return OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .temperature(0.3)
                .topP(0.9)
                .maxTokens(200)
                .build();
    }

    /**
     * 自定义模型配置
     */
    public static class CustomModelBuilder {
        private double temperature = 0.7;
        private double topP = 0.9;
        private int maxTokens = 500;
        private double frequencyPenalty = 0.0;
        private double presencePenalty = 0.0;

        public CustomModelBuilder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public CustomModelBuilder topP(double topP) {
            this.topP = topP;
            return this;
        }

        public CustomModelBuilder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public CustomModelBuilder frequencyPenalty(double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public CustomModelBuilder presencePenalty(double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public ChatModel build() {
            return OpenAiChatModel.builder()
                    .apiKey(System.getenv("OPENAI_API_KEY"))
                    .modelName("gpt-4o-mini")
                    .temperature(temperature)
                    .topP(topP)
                    .maxTokens(maxTokens)
                    .frequencyPenalty(frequencyPenalty)
                    .presencePenalty(presencePenalty)
                    .build();
        }
    }

    public static void main(String[] args) {
        // 使用预设配置
        ChatModel chatModel = ModelConfigFactory.createChatModel();
        System.out.println(chatModel.chat("你好"));

        // 使用自定义配置
        ChatModel customModel = new CustomModelBuilder()
                .temperature(0.5)
                .maxTokens(300)
                .frequencyPenalty(0.2)
                .build();
        System.out.println(customModel.chat("你好"));
    }
}
```

### 练习 2：参数优化实验

创建实验工具，测试不同参数组合：

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.util.HashMap;
import java.util.Map;

/**
 * 参数优化实验
 */
public class ParameterOptimizationExperiment {

    public static void main(String[] args) {
        String prompt = "用一句话介绍 Java";
        int iterations = 5;
        
        Map<String, ChatModel> models = new HashMap<>();
        
        // 实验 1：不同温度
        models.put("Temp 0.1", createModel(0.1, 0.9, 200));
        models.put("Temp 0.5", createModel(0.5, 0.9, 200));
        models.put("Temp 0.9", createModel(0.9, 0.9, 200));
        
        // 实验 2：不同 Max Tokens
        models.put("Max 100", createModel(0.7, 0.9, 100));
        models.put("Max 300", createModel(0.7, 0.9, 300));
        models.put("Max 500", createModel(0.7, 0.9, 500));
        
        // 运行实验
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║           参数优化实验工具                       ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("提示词: " + prompt);
        System.out.println("迭代次数: " + iterations);
        System.out.println();
        
        for (Map.Entry<String, ChatModel> entry : models.entrySet()) {
            System.out.println("═══ 配置: " + entry.getKey() + " ═");
            System.out.println();
            
            for (int i = 0; i < iterations; i++) {
                String response = entry.getValue().chat(prompt);
                System.out.println((i + 1) + ". " + response);
            }
            System.out.println();
        }
    }
    
    private static ChatModel createModel(double temperature, 
                                            double topP, 
                                            int maxTokens) {
        return OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .temperature(temperature)
                .topP(topP)
                .maxTokens(maxTokens)
                .build();
    }
}
```

### 练习 3：动态参数调整

创建一个根据任务类型动态调整参数的服务：

```java
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.util.HashMap;
import java.util.Map;

/**
 * 智能参数选择服务
 */
public class SmartParameterSelector {

    private final Map<TaskType, ChatModel> modelCache = new HashMap<>();
    private final String apiKey;
    private final String modelName;

    public SmartParameterSelector(String apiKey, String modelName) {
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    public enum TaskType {
        FACTUAL_QA,      // 事实问答
        CREATIVE_WRITING, // 创意写作
        CODE_GENERATION,  // 代码生成
        CHAT,            // 聊天
        SUMMARY,         // 摘要
        TRANSLATION       // 翻译
    }

    /**
     * 根据任务类型获取优化的模型
     */
    public ChatModel getModelForTask(TaskType taskType) {
        return modelCache.computeIfAbsent(taskType, this::createModelForTask);
    }

    /**
     * 使用优化的模型完成任务
     */
    public String executeTask(TaskType taskType, String prompt) {
        ChatModel model = getModelForTask(taskType);
        return model.chat(prompt);
    }

    private ChatModel createModelForTask(TaskType taskType) {
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName);

        switch (taskType) {
            case FACTUAL_QA:
                return builder
                        .temperature(0.1)
                        .topP(0.9)
                        .maxTokens(300)
                        .build();

            case CREATIVE_WRITING:
                return builder
                        .temperature(0.9)
                        .topP(0.95)
                        .frequencyPenalty(0.5)
                        .presencePenalty(0.3)
                        .maxTokens(1000)
                        .build();

            case CODE_GENERATION:
                return builder
                        .temperature(0.2)
                        .topP(0.95)
                        .maxTokens(800)
                        .stopSequences("\n\n", "###")
                        .build();

            case CHAT:
                return builder
                        .temperature(0.7)
                        .topP(0.9)
                        .frequencyPenalty(0.3)
                        .presencePenalty(0.1)
                        .maxTokens(500)
                        .build();

            case SUMMARY:
                return builder
                        .temperature(0.3)
                        .topP(0.9)
                        .maxTokens(200)
                        .build();

            case TRANSLATION:
                return builder
                        .temperature(0.2)
                        .topP(0.9)
                        .maxTokens(400)
                        .build();

            default:
                return builder
                        .temperature(0.7)
                        .build();
        }
    }

    public static void main(String[] args) {
        SmartParameterSelector selector = new SmartParameterSelector(
                System.getenv("OPENAI_API_KEY"),
                "gpt-4o-mini"
        );

        // 测试不同任务类型
        System.out.println("=== 事实问答 ===");
        System.out.println(selector.executeTask(
                TaskType.FACTUAL_QA, 
                "Java 的主要特点是什么？"
        ));
        System.out.println();

        System.out.println("=== 创意写作 ===");
        System.out.println(selector.executeTask(
                TaskType.CREATIVE_WRITING, 
                "写一个关于编程的短诗"
        ));
        System.out.println();

        System.out.println("=== 代码生成 ===");
        System.out.println(selector.executeTask(
                TaskType.CODE_GENERATION, 
                "写一个 Java 方法计算两个数的和"
        ));
        System.out.println();

        System.out.println("=== 摘要 ===");
        String longText = "LangChain4j 是一个用于构建 LLM 应用的 Java 库..." +
                        "它提供了完整的工具链和组件，帮助开发者..." +
                        "支持多种 LLM 提供商，包括 OpenAI、Anthropic 等...";
        System.out.println(selector.executeTask(
                TaskType.SUMMARY, 
                "请用一句话总结以下内容：" + longText
        ));
    }
}
```

## 总结

### 本章要点

1. **核心参数**
   - **Temperature**：控制随机性和创造性（0.0-2.0）
   - **Top P**：核采样，限制候选词范围（0.0-1.0）
   - **Top K**：从概率最高的 K 个词中选择
   - **Max Tokens**：限制最大输出长度
   - **Stop Sequences**：指定停止标记

2. **惩罚参数**
   - **Frequency Penalty**：降低重复词概率（-2.0 到 2.0）
   - **Presence Penalty**：降低已出现词的再出现概率
   - **Logit Bias**：偏置特定 token 的概率

3. **参数组合策略**
   - 精确问答：低温度 + 标准采样
   - 创意写作：高温度 + 惩罚
   - 代码生成：低温度 + 停止序列
   - 聊天：平衡温度 + 轻微惩罚

4. **最佳实践**
   - 根据任务类型选择合适的参数
   - 通过实验优化参数组合
   - 封装常用配置便于复用
   - 动态调整参数适应不同场景

### 下一步

在下一章节中，我们将学习：
- ChatMessage 类型的详细介绍
- UserMessage、SystemMessage、AiMessage 的使用
- 消息组合和对话上下文管理

### 常见问题

**Q1：Temperature 和 Top P 应该同时使用吗？**

A：通常同时使用可以获得更好的效果。Temperature 控制整体分布，Top P 限制候选范围。但也可以只使用其中一个。

**Q2：如何选择合适的 Max Tokens 值？**

A：考虑：
- 任务类型（问答用小值，创作用大值）
- 成本控制（Token 数量与费用成正比）
- 用户体验（太短可能信息不足，太长可能冗余）

**Q3：Frequency Penalty 和 Presence Penalty 有什么区别？**

A：
- Frequency Penalty：与词的出现次数成比例
- Presence Penalty：无论出现多少次，只要出现过就惩罚
- 通常可以结合使用，建议值：Frequency 0.1-0.5，Presence 0.1-0.3

**Q4：Temperature 设置为 0 会完全确定吗？**

A：理论上是的，但不同模型可能有微小差异。对于完全确定的行为，建议使用 temperature < 0.1。

**Q5：如何通过实验找到最佳参数？**

A：
1. 定义评估指标（如相关性、多样性等）
2. 测试不同参数组合
3. 对比结果
4. 选择最佳配置
5. 在新数据上验证

## 参考资料

- [OpenAI 官方文档 - Temperature](https://platform.openai.com/docs/api-reference/creating-completions-completions)
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
