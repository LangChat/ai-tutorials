package cn.langchat.learning.util;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;

/**
 * 测试模型提供者
 * 
 * 提供静态方法获取配置好的 ChatModel 和 StreamingChatModel
 * 用于所有测试类和示例类
 * 
 * 配置从以下来源加载（按优先级）：
 * 1. 系统环境变量
 * 2. .env.local 文件
 * 3. .env 文件
 * 4. 默认值
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
public final class TestModelProvider {

    private TestModelProvider() {
        // 工具类，不允许实例化
    }

    /**
     * 获取 ChatModel
     *
     * @return 配置好的 ChatModel
     */
    public static ChatModel getChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(EnvConfig.getApiKey())
                .baseUrl(EnvConfig.getBaseUrl())
                .modelName(EnvConfig.getModelName())
                .temperature(EnvConfig.getTemperature())
                .maxTokens(EnvConfig.getMaxTokens())
                .build();
    }

    /**
     * 获取 StreamingChatModel
     *
     * @return 配置好的 StreamingChatModel
     */
    public static StreamingChatModel getStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(EnvConfig.getApiKey())
                .baseUrl(EnvConfig.getBaseUrl())
                .modelName(EnvConfig.getModelName())
                .temperature(EnvConfig.getTemperature())
                .maxTokens(EnvConfig.getMaxTokens())
                .build();
    }

    /**
     * 获取 API Key（供测试类使用）
     */
    public static String getApiKey() {
        return EnvConfig.getApiKey();
    }

    /**
     * 获取 Model Name（供测试类使用）
     */
    public static String getModelName() {
        return EnvConfig.getModelName();
    }

    /**
     * 获取 Base URL（供测试类使用）
     */
    public static String getBaseUrl() {
        return EnvConfig.getBaseUrl();
    }

    /**
     * 获取 EmbeddingModel
     *
     * @return 配置好的 EmbeddingModel
     */
    public static EmbeddingModel getEmbeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(EnvConfig.getApiKey())
                .baseUrl(EnvConfig.getBaseUrl())
                .modelName(EnvConfig.getEmbeddingModelName())
                .build();
    }

    /**
     * 获取 Embedding Model Name（供测试类使用）
     */
    public static String getEmbeddingModelName() {
        return EnvConfig.getEmbeddingModelName();
    }

    /**
     * 打印配置信息
     */
    public static void printConfig() {
        EnvConfig.printConfig();
    }
}
