package cn.langchat.learning.tutorial.httpclient;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 18 - 自定义 HTTP 客户端测试
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
@DisplayName("18 - 自定义 HTTP 客户端测试")
class CustomHttpClientTest {

    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        chatModel = TestModelProvider.getChatModel();
    }

    @Test
    @DisplayName("应该能使用默认 HTTP 客户端")
    void shouldUseDefaultHttpClient() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 使用默认 HTTP 客户端                             ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        String response = chatModel.chat("你好");
        
        assertNotNull(response);
        assertFalse(response.isEmpty());
        
        log.info("响应: {}", response);
        log.info("\n✅ 测试通过：能够使用默认 HTTP 客户端\n");
    }
}
