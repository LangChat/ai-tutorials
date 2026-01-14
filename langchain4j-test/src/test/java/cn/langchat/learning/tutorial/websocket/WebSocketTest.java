package cn.langchat.learning.tutorial.websocket;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 24 - WebSocket 实时通信测试
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
@DisplayName("24 - WebSocket 实时通信测试")
class WebSocketTest {

    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        chatModel = TestModelProvider.getChatModel();
    }

    @Test
    @DisplayName("应该能模拟实时对话")
    void shouldSimulateRealTimeConversation() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 模拟实时对话                                         ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        String response = chatModel.chat("你好");
        
        assertNotNull(response);
        assertFalse(response.isEmpty());
        
        log.info("响应: {}", response);
        log.info("\n✅ 测试通过：能够模拟实时对话\n");
    }
}
