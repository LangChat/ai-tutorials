package cn.langchat.learning.tutorial.introduction;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 01 Introduction - LangChain4j 基础测试
 * 
 * 不依赖 Spring Boot，直接使用 TestModelProvider
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
@DisplayName("01 Introduction - LangChain4j 基础测试")
class LangChain4jBasicTest {

    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        // 打印配置信息
        TestModelProvider.printConfig();
        
        // 获取 ChatModel
        chatModel = TestModelProvider.getChatModel();
        
        log.info("\n╔══════════════════════════════════════════════════════════════════╗");
        log.info("║              LangChain4j 基础功能测试                      ║");
        log.info("╠═══════════════════════════════════════════════════════════════════════╣");
        log.info("║ 测试内容: ChatModel 基础功能                                 ║");
        log.info("╚═════════════════════════════════════════════════════════════════════════════════════════════════════════╝\n");
    }

    @Test
    @DisplayName("应该能生成简单响应")
    void shouldGenerateSimpleResponse() {
        // Given - 准备测试数据
        String message = "What is 2 + 2?";

        // When - 执行操作
        String response = chatModel.chat(message);

        // Then - 验证结果
        assertNotNull(response, "响应不应为空");
        assertFalse(response.isEmpty(), "响应不应为空字符串");
        
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: shouldGenerateSimpleResponse                            ║");
        log.info("╠═══════════════════════════════════════════════════════════════════════╣");
        log.info("║ 问题: {}", message);
        log.info("╠═════════════════════════════════════════════════════════════════════════════════════════════════╣");
        log.info("║ 回答: ");
        log.info("║ {}", response);
        log.info("╚═════════════════════════════════════════════════════════════════════════════════════════════════════════╝");
    }

    @Test
    @DisplayName("应该能生成中文响应")
    void shouldGenerateChineseResponse() {
        // Given
        String message = "你好，请用中文回答：什么是 LangChain4j？";

        // When
        String response = chatModel.chat(message);

        // Then
        assertNotNull(response, "响应不应为空");
        assertFalse(response.isEmpty(), "响应不应为空字符串");
        
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: shouldGenerateChineseResponse                         ║");
        log.info("╠═══════════════════════════════════════════════════════════════════════╣");
        log.info("║ 问题: {}", message);
        log.info("╠═════════════════════════════════════════════════════════════════════════════════════════════════╣");
        log.info("║ 回答: ");
        log.info("║ {}", response);
        log.info("╚═════════════════════════════════════════════════════════════════════════════════════════════════════════╝");
    }

    @Test
    @DisplayName("应该能处理空消息")
    void shouldHandleEmptyMessage() {
        // Given
        String message = "";

        // When
        String response = chatModel.chat(message);

        // Then
        assertNotNull(response, "即使空消息也应返回响应");
        
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: shouldHandleEmptyMessage                            ║");
        log.info("╠═══════════════════════════════════════════════════════════════════════╣");
        log.info("║ 问题: [空消息]");
        log.info("╠═════════════════════════════════════════════════════════════════════════════════════════════════╣");
        log.info("║ 回答: ");
        log.info("║ {}", response);
        log.info("╚═════════════════════════════════════════════════════════════════════════════════════════════════════════╝");
    }

    @Test
    @DisplayName("应该能生成英文响应")
    void shouldGenerateEnglishResponse() {
        // Given
        String message = "Explain LangChain4j in simple terms.";

        // When
        String response = chatModel.chat(message);

        // Then
        assertNotNull(response, "响应不应为空");
        assertFalse(response.isEmpty(), "响应不应为空字符串");
        
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: shouldGenerateEnglishResponse                        ║");
        log.info("╠═══════════════════════════════════════════════════════════════════════╣");
        log.info("║ 问题: {}", message);
        log.info("╠═════════════════════════════════════════════════════════════════════════════════════════════════╣");
        log.info("║ 回答: ");
        log.info("║ {}", response);
        log.info("╚═════════════════════════════════════════════════════════════════════════════════════════════════════════╝");
    }
}
