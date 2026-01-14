package cn.langchat.learning.tutorial.troubleshooting;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 29 - 故障排除测试
 */
@Slf4j
@DisplayName("29 - 故障排除测试")
class TroubleshootingTest {
    
    private ChatModel chatModel;
    
    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        chatModel = TestModelProvider.getChatModel();
    }
    
    @Test
    @DisplayName("应该能诊断问题")
    void shouldDiagnoseIssues() {
        log.info("测试: 诊断问题");
        String response = chatModel.chat("你好");
        assertNotNull(response);
        log.info("✅ 测试通过\n");
    }
}
