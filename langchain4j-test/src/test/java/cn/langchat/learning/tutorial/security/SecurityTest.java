package cn.langchat.learning.tutorial.security;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 26 - 安全和认证测试
 */
@Slf4j
@DisplayName("26 - 安全和认证测试")
class SecurityTest {
    
    private ChatModel chatModel;
    
    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        chatModel = TestModelProvider.getChatModel();
    }
    
    @Test
    @DisplayName("应该能安全使用 API Key")
    void shouldUseApiKeySecurely() {
        log.info("测试: 安全使用 API Key");
        
        String response = chatModel.chat("你好");
        assertNotNull(response);
        
        log.info("✅ 测试通过\n");
    }
}
