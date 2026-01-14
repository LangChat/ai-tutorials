---
title: '其他模型提供商'
description: '学习 LangChain4j 的 其他模型提供商 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 30 - 其他模型提供商

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 了解 LangChain4j 支持的其他模型提供商
- 掌握不同模型的集成方法
- 学会模型切换和策略选择
- 理解多模型部署架构
- 实现一个支持多模型的 LLM 应用

## 支持的模型提供商

| 提供商 | 模型类型 | 特点 |
|-------|---------|------|
| OpenAI | Chat、Embedding | 市场领导者，质量最高 |
| Anthropic | Chat | 长上下文，推理能力强 |
| Azure OpenAI | Chat、Embedding | 企业级，安全合规 |
| Google | Chat、Embedding | 多模态，搜索集成 |
| Cohere | Chat、Embedding | 高性价比，中文友好 |
| Hugging Face | 各种 | 开源模型，可定制 |
| Ollama | 各种 | 本地部署，隐私保护 |
| 百度 | Chat、Embedding | 中文支持，国内访问快 |

## Anthropic 集成

```java
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Anthropic 模型
 */
public class AnthropicIntegration {

    /**
     * 创建 Anthropic 模型
     */
    public static ChatModel createAnthropicModel(String apiKey) {
        return AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName(AnthropicChatModel.Models.CLAUDE_3_SONNET)
                .temperature(0.7)
                .maxTokens(1000)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    /**
     * Anthropic 模型列表
     */
    public static class AnthropicModels {
        public static final String CLAUDE_3_OPUS = "claude-3-opus";
        public static final String CLAUDE_3_SONNET = "claude-3-sonnet";
        public static final String CLAUDE_2_1 = "claude-2.1";
        public static final String CLAUDE_2 = "claude-2";
        public static final String CLAUDE_INSTANT_1_2 = "claude-instant-1.2";
    }
}
```

## Azure OpenAI 集成

```java
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Azure OpenAI 模型
 */
public class AzureOpenAiIntegration {

    /**
     * 创建 Azure OpenAI 模型
     */
    public static ChatModel createAzureModel(String endpoint, 
                                                    String apiKey, 
                                                    String deploymentName) {
        return AzureOpenAiChatModel.builder()
                .endpoint(endpoint)
                .apiKey(apiKey)
                .deploymentName(deploymentName)
                .modelName(deploymentName)
                .temperature(0.7)
                .maxTokens(1000)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * Azure 配置
     */
    public static class AzureConfig {
        private final String endpoint;
        private final String apiKey;
        private final String deploymentName;

        public AzureConfig(String endpoint, String apiKey, String deploymentName) {
            this.endpoint = endpoint;
            this.apiKey = apiKey;
            this.deploymentName = deploymentName;
        }

        public String getEndpoint() { return endpoint; }
        public String getApiKey() { return apiKey; }
        public String getDeploymentName() { return deploymentName; }
    }
}
```

## Hugging Face 集成

```java
import dev.langchain4j.model.huggingface.HuggingFaceChatModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;

/**
 * Hugging Face 模型
 */
public class HuggingFaceIntegration {

    /**
     * 创建 Hugging Face 聊天模型
     */
    public static HuggingFaceChatModel createChatModel(String modelId) {
        return HuggingFaceChatModel.builder()
                .modelId(modelId)
                .temperature(0.7)
                .maxNewTokens(256)
                .build();
    }

    /**
     * 创建 Hugging Face 嵌入模型
     */
    public static HuggingFaceEmbeddingModel createEmbeddingModel(String modelId) {
        return HuggingFaceEmbeddingModel.builder()
                .modelId(modelId)
                .build();
    }

    /**
     * 推荐的开源模型
     */
    public static class RecommendedModels {
        public static final String LAMA_2_7B = "meta-llama/Llama-2-7b-chat-hf";
        public static final String MISTRAL_7B = "mistralai/Mistral-7B-Instruct-v0.2";
        public static final String QWEN_7B = "Qwen/Qwen-7B-Chat";
        public static final String BAICHUAN_7B = "baichuan-inc/Baichuan-7B";
    }
}
```

## 总结

### 本章要点

1. **多模型支持**
   - 支持多个主流模型提供商
   - 统一的 API 接口
   - 灵活的模型切换

2. **选择策略**
   - 根据需求选择模型
   - 平衡质量和成本
   - 考虑访问速度

3. **集成方法**
   - 使用对应的 Builder
   - 配置必要的参数
   - 处理不同模型的特性

## 参考资料

- [LangChain4j 文档](https://docs.langchain4j.dev/)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
