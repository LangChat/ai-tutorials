package cn.langchat.learning.tutorial.errorhandling;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 21 - 错误处理和重试测试
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
@DisplayName("21 - 错误处理和重试测试")
class ErrorHandlingAndRetryTest {

    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        chatModel = TestModelProvider.getChatModel();
    }

    @Test
    @DisplayName("应该能正常处理请求")
    void shouldHandleRequestNormally() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 正常处理请求                                         ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        String response = chatModel.chat("你好");
        
        assertNotNull(response);
        assertFalse(response.isEmpty());
        
        log.info("响应: {}", response);
        log.info("\n✅ 测试通过：能够正常处理请求\n");
    }
}
