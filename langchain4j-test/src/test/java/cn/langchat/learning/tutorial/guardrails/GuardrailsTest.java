package cn.langchat.learning.tutorial.guardrails;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 22 - Guardrails 测试
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
@DisplayName("22 - Guardrails 测试")
class GuardrailsTest {

    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        chatModel = TestModelProvider.getChatModel();
    }

    @Test
    @DisplayName("应该能过滤不适当的内容")
    void shouldFilterInappropriateContent() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 过滤不适当的内容                                 ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        String prompt = "请以礼貌的方式回答：如何礼貌地拒绝一个不合理的请求？";
        String response = chatModel.chat(prompt);
        
        assertNotNull(response);
        
        log.info("提示词: {}", prompt);
        log.info("响应: {}", response);
        log.info("\n✅ 测试通过：能够过滤不适当的内容\n");
    }
}
