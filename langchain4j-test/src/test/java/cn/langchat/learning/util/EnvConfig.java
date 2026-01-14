package cn.langchat.learning.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 环境配置加载器
 * 
 * 按优先级顺序加载配置：
 * 1. 系统环境变量（优先级最高）
 * 2. .env.local 文件
 * 3. .env 文件
 * 4. 默认值
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
public final class EnvConfig {

    // 配置键名
    public static final String API_KEY = "LANGCHAT_API_KEY";
    public static final String BASE_URL = "LANGCHAT_BASE_URL";
    public static final String MODEL_NAME = "LANGCHAT_MODEL_NAME";
    public static final String EMBEDDING_MODEL_NAME = "LANGCHAT_EMBEDDING_MODEL_NAME";
    public static final String TEMPERATURE = "LANGCHAT_TEMPERATURE";
    public static final String MAX_TOKENS = "LANGCHAT_MAX_TOKENS";
    private static final Properties properties = new Properties();
    // 默认值
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final String DEFAULT_MODEL_NAME = "gpt-3.5-turbo";
    private static final String DEFAULT_EMBEDDING_MODEL_NAME = "text-embedding-ada-002";
    private static final Double DEFAULT_TEMPERATURE = 0.7;
    private static final Integer DEFAULT_MAX_TOKENS = 1000;
    private static boolean initialized = false;

    private EnvConfig() {
        // 工具类，不允许实例化
    }

    /**
     * 初始化配置（懒加载）
     */
    private static synchronized void init() {
        if (initialized) {
            return;
        }

        // 1. 尝试从项目根目录加载 .env 文件
        loadEnvFile(".env");
        
        // 2. 尝试从项目根目录加载 .env.local 文件（会覆盖 .env 中的配置）
        loadEnvFile(".env.local");

        initialized = true;
        log.debug("环境配置加载完成");
    }

    /**
     * 加载指定的 .env 文件
     */
    private static void loadEnvFile(String filename) {
        Path envPath = Path.of(filename);
        if (!Files.exists(envPath)) {
            log.debug("配置文件 {} 不存在，跳过", filename);
            return;
        }

        try {
            log.info("正在加载配置文件: {}", filename);
            Properties fileProperties = new Properties();
            
            // 读取 .env 文件（支持注释和空行）
            Files.lines(envPath)
                .filter(line -> !line.trim().isEmpty() && !line.trim().startsWith("#"))
                .filter(line -> line.contains("="))
                .forEach(line -> {
                    int separatorIndex = line.indexOf("=");
                    String key = line.substring(0, separatorIndex).trim();
                    String value = line.substring(separatorIndex + 1).trim();
                    fileProperties.setProperty(key, value);
                });

            // 将文件中的配置合并到 properties 中
            properties.putAll(fileProperties);
            log.info("已从 {} 加载 {} 个配置项", filename, fileProperties.size());
            
        } catch (IOException e) {
            log.warn("加载配置文件 {} 失败: {}", filename, e.getMessage());
        }
    }

    /**
     * 获取配置值（优先使用系统环境变量）
     */
    private static String get(String key) {
        init();
        
        // 1. 优先使用系统环境变量
        String systemValue = System.getenv(key);
        if (systemValue != null && !systemValue.isEmpty()) {
            log.debug("使用系统环境变量: {} = {}", key, maskValue(key, systemValue));
            return systemValue;
        }
        
        // 2. 使用 .env 文件中的配置
        String envValue = properties.getProperty(key);
        if (envValue != null) {
            log.debug("使用 .env 配置: {} = {}", key, maskValue(key, envValue));
            return envValue;
        }
        
        log.debug("配置 {} 未设置", key);
        return null;
    }

    /**
     * 获取字符串配置
     */
    public static String getString(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取整数配置
     */
    public static Integer getInt(String key, Integer defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("配置 {} 的值 '{}' 不是有效的整数，使用默认值: {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 获取浮点数配置
     */
    public static Double getDouble(String key, Double defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.warn("配置 {} 的值 '{}' 不是有效的数字，使用默认值: {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 获取 API Key
     */
    public static String getApiKey() {
        String apiKey = getString(API_KEY, null);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("API Key 未配置！请在 .env 文件中设置 LANGCHAT_API_KEY");
        }
        return apiKey;
    }

    /**
     * 获取 Base URL
     */
    public static String getBaseUrl() {
        return getString(BASE_URL, DEFAULT_BASE_URL);
    }

    /**
     * 获取 Model Name
     */
    public static String getModelName() {
        return getString(MODEL_NAME, DEFAULT_MODEL_NAME);
    }

    /**
     * 获取 Embedding Model Name
     */
    public static String getEmbeddingModelName() {
        return getString(EMBEDDING_MODEL_NAME, DEFAULT_EMBEDDING_MODEL_NAME);
    }

    /**
     * 获取 Temperature
     */
    public static Double getTemperature() {
        return getDouble(TEMPERATURE, DEFAULT_TEMPERATURE);
    }

    /**
     * 获取 Max Tokens
     */
    public static Integer getMaxTokens() {
        return getInt(MAX_TOKENS, DEFAULT_MAX_TOKENS);
    }

    /**
     * 遮盖敏感信息用于日志输出
     */
    private static String maskValue(String key, String value) {
        if (key.contains("KEY") || key.contains("SECRET") || key.contains("TOKEN")) {
            if (value.length() <= 8) {
                return "***";
            }
            return value.substring(0, 8) + "***";
        }
        return value;
    }

    /**
     * 打印配置信息（用于调试）
     */
    public static void printConfig() {
        init();
        log.info("╔═════════════════════════════════════════════════════════════════════════════════════════════════════════╗");
        log.info("║                         LangChain4j 环境配置                                                    ║");
        log.info("╠════════════════════════════════════════════════════════════════════════════════════════════════════════╣");
        log.info("║ API Key:           {}", maskValue(API_KEY, getApiKey()));
        log.info("║ Base URL:          {}", getBaseUrl());
        log.info("║ Model Name:        {}", getModelName());
        log.info("║ Embedding Model:   {}", getEmbeddingModelName());
        log.info("║ Temperature:       {}", getTemperature());
        log.info("║ Max Tokens:       {}", getMaxTokens());
        log.info("╚═════════════════════════════════════════════════════════════════════════════════════════════════════════╝\n");
    }
}
