package cn.langchat.learning.tutorial.othermodels;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 30 - 其他模型提供商测试
 */
@Slf4j
@DisplayName("30 - 其他模型提供商测试")
class OtherModelsTest {
    
    private ChatModel chatModel;
    
    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        chatModel = TestModelProvider.getChatModel();
    }
    
    @Test
    @DisplayName("应该能使用不同模型")
    void shouldUseDifferentModels() {
        log.info("测试: 使用不同模型");
        String response = chatModel.chat("你好");
        assertNotNull(response);
        log.info("✅ 测试通过\n");
    }
}
