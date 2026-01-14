package cn.langchat.learning.tutorial.bestpractices;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 37 - 最佳实践测试
 */
@Slf4j
@DisplayName("37 - 最佳实践测试")
class BestPracticesTest {
    
    private ChatModel chatModel;
    
    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        chatModel = TestModelProvider.getChatModel();
    }
    
    @Test
    @DisplayName("应该能遵循最佳实践")
    void shouldFollowBestPractices() {
        log.info("测试: 遵循最佳实践");
        String response = chatModel.chat("你好");
        assertNotNull(response);
        log.info("✅ 测试通过\n");
    }
}
