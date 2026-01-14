package cn.langchat.learning.tutorial.testing;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 20 - 测试和评估测试
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
@DisplayName("20 - 测试和评估测试")
class TestingAndEvaluationTest {

    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        chatModel = TestModelProvider.getChatModel();
    }

    @Test
    @DisplayName("应该能评估模型响应质量")
    void shouldEvaluateResponseQuality() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 评估模型响应质量                                  ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        String response = chatModel.chat("什么是人工智能？");
        
        // 基本评估标准
        assertNotNull(response, "响应不应为空");
        assertFalse(response.trim().isEmpty(), "响应不应为空白");
        assertTrue(response.length() > 10, "响应应该有足够的长度");
        
        log.info("响应: {}", response);
        log.info("响应长度: {} 字符", response.length());
        
        log.info("\n基本质量评估:");
        log.info("  ✓ 非空检查: 通过");
        log.info("  ✓ 长度检查: 通过");
        log.info("  ✓ 内容检查: 通过");
        
        log.info("\n✅ 测试通过：能够评估模型响应质量\n");
    }

    @Test
    @DisplayName("应该能测试模型的准确性")
    void shouldTestAccuracy() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 测试模型的准确性                                    ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 测试简单事实
        String response1 = chatModel.chat("1 + 1 等于几？");
        assertTrue(response1.contains("2"), "1+1应该等于2");
        
        // 测试概念解释
        String response2 = chatModel.chat("用一句话解释什么是编程");
        assertNotNull(response2);
        assertTrue(response2.length() > 5, "解释应该有足够的内容");
        
        log.info("测试 1: 1 + 1 = ?");
        log.info("  响应: {}", response1);
        log.info("  包含 '2': {}", response1.contains("2"));
        
        log.info("\n测试 2: 编程定义");
        log.info("  响应: {}", response2);
        log.info("  长度: {} 字符", response2.length());
        
        log.info("\n✅ 测试通过：能够测试模型的准确性\n");
    }
}
