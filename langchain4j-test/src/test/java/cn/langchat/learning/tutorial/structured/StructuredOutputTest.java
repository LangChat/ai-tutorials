package cn.langchat.learning.tutorial.structured;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 19 - 结构化输出测试
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
@DisplayName("19 - 结构化输出测试")
class StructuredOutputTest {

    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        chatModel = TestModelProvider.getChatModel();
    }

    @Test
    @DisplayName("应该能生成结构化 JSON 输出")
    void shouldGenerateStructuredJsonOutput() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 生成结构化 JSON 输出                               ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        String prompt = "请生成一个产品信息的 JSON，包含 id、name、price、description 四个字段。";
        String response = chatModel.chat(prompt);
        
        assertNotNull(response);
        assertTrue(response.contains("{") && response.contains("}"));
        
        log.info("提示词: {}", prompt);
        log.info("响应: {}", response);
        log.info("\n✅ 测试通过：能够生成结构化 JSON 输出\n");
    }
}
