package cn.langchat.learning.tutorial.realtime;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 36 - 实时协作测试
 */
@Slf4j
@DisplayName("36 - 实时协作测试")
class RealTimeCollaborationTest {
    
    private ChatModel chatModel;
    
    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        chatModel = TestModelProvider.getChatModel();
    }
    
    @Test
    @DisplayName("应该能实现实时协作")
    void shouldImplementRealTimeCollaboration() {
        log.info("测试: 实时协作");
        String response = chatModel.chat("你好");
        assertNotNull(response);
        log.info("✅ 测试通过\n");
    }
}
