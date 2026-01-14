package cn.langchat.learning.tutorial.faq;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 38 - FAQ 和解决方案测试
 */
@Slf4j
@DisplayName("38 - FAQ 和解决方案测试")
class FaqAndSolutionsTest {
    
    private ChatModel chatModel;
    
    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        chatModel = TestModelProvider.getChatModel();
    }
    
    @Test
    @DisplayName("应该能回答常见问题")
    void shouldAnswerFaq() {
        log.info("测试: 回答常见问题");
        String response = chatModel.chat("你好");
        assertNotNull(response);
        log.info("✅ 测试通过\n");
    }
}
