package cn.langchat.learning.tutorial.logging;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 15 - 日志测试
 * 
 * 测试文档中的完整使用场景，包括：
 * - 请求日志记录
 * - 响应日志记录
 * - 调试日志
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
@DisplayName("15 - 日志测试")
class LoggingTest {

    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        chatModel = TestModelProvider.getChatModel();
    }

    @Test
    @DisplayName("应该能记录请求日志")
    void shouldLogRequest() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 记录请求日志                                         ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        String prompt = "你好";
        log.info("发送请求: {}", prompt);
        
        String response = chatModel.chat(prompt);
        
        log.info("收到响应: {}", response);
        assertNotNull(response);
        
        log.info("\n✅ 测试通过：能够记录请求和响应日志\n");
    }
}
