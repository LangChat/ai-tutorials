package cn.langchat.learning.tutorial.json;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 14 - JSON 处理测试
 * 
 * 测试文档中的完整使用场景，包括：
 * - JSON 生成
 * - JSON 解析
 * - 结构化数据提取
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
@DisplayName("14 - JSON 处理测试")
class JsonHandlingTest {

    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        chatModel = TestModelProvider.getChatModel();
    }

    @Test
    @DisplayName("应该能生成 JSON 格式数据")
    void shouldGenerateJsonFormat() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 生成 JSON 格式数据                                   ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        String prompt = "请生成一个用户信息的 JSON，包含 name、age、email 三个字段。";
        
        String response = chatModel.chat(prompt);
        
        assertNotNull(response);
        assertTrue(
            response.contains("{") && response.contains("}"),
            "响应应该包含 JSON 格式"
        );
        
        log.info("提示词: {}", prompt);
        log.info("响应: {}", response);
        log.info("\n✅ 测试通过：能够生成 JSON 格式数据\n");
    }

    @Test
    @DisplayName("应该能提取结构化数据")
    void shouldExtractStructuredData() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 提取结构化数据                                       ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        String text = "张三，25岁，zhangsan@example.com，软件工程师";
        String prompt = String.format(
            "请从以下文本中提取信息，并以 JSON 格式返回：\n\n文本：%s\n\n提取字段：name、age、email、job",
            text
        );
        
        String response = chatModel.chat(prompt);
        
        assertNotNull(response);
        assertTrue(response.contains("张三"));
        
        log.info("文本: {}", text);
        log.info("提取的 JSON: {}", response);
        log.info("\n✅ 测试通过：能够提取结构化数据\n");
    }
}
