package cn.langchat.learning.tutorial.imagemodel;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 17 - 图像模型测试
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
@DisplayName("17 - 图像模型测试")
class ImageModelTest {

    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        chatModel = TestModelProvider.getChatModel();
    }

    @Test
    @DisplayName("应该能描述图片（文本模拟）")
    void shouldDescribeImage() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 描述图片（文本模拟）                               ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        String prompt = "请描述一张包含蓝天和白云的风景画。";
        String response = chatModel.chat(prompt);
        
        assertNotNull(response);
        assertFalse(response.isEmpty());
        
        log.info("提示词: {}", prompt);
        log.info("响应: {}", response);
        log.info("\n✅ 测试通过：能够描述图片（文本模拟）\n");
    }
}
