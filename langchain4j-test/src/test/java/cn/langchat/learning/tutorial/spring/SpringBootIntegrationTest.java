package cn.langchat.learning.tutorial.spring;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 23 - Spring Boot 集成测试
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
@DisplayName("23 - Spring Boot 集成测试")
class SpringBootIntegrationTest {

    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        chatModel = TestModelProvider.getChatModel();
    }

    @Test
    @DisplayName("应该能在测试环境中使用 ChatModel")
    void shouldUseChatModelInTestEnvironment() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 在测试环境中使用 ChatModel                          ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        String response = chatModel.chat("你好");
        
        assertNotNull(response);
        assertFalse(response.isEmpty());
        
        log.info("响应: {}", response);
        log.info("\n✅ 测试通过：能够在测试环境中使用 ChatModel\n");
    }
}
