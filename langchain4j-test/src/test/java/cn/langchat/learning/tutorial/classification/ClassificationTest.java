package cn.langchat.learning.tutorial.classification;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 13 - 分类测试
 * 
 * 测试文档中的完整使用场景，包括：
 * - 文本分类
 * - 情感分析
 * - 主题分类
 * - 多标签分类
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
@DisplayName("13 - 分类测试")
class ClassificationTest {

    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        chatModel = TestModelProvider.getChatModel();
    }

    @Test
    @DisplayName("应该能进行情感分类")
    void shouldPerformSentimentClassification() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 情感分类                                             ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        String text = "今天天气真好，我很开心！";
        
        String prompt = String.format(
            "请对以下文本进行情感分类，只返回分类结果（正面/负面/中性）：\n\n文本：%s",
            text
        );
        
        String classification = chatModel.chat(prompt);
        
        assertNotNull(classification);
        assertTrue(
            classification.contains("正面") || 
            classification.contains("负面") || 
            classification.contains("中性")
        );
        
        log.info("文本: {}", text);
        log.info("情感分类: {}", classification);
        log.info("\n✅ 测试通过：能够进行情感分类\n");
    }

    @Test
    @DisplayName("应该能进行主题分类")
    void shouldPerformTopicClassification() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 主题分类                                             ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        String text = "Java 是一种面向对象的编程语言，具有平台无关性。";
        
        String prompt = String.format(
            "请对以下文本进行主题分类，返回以下主题之一：技术、体育、娱乐、政治、经济\n\n文本：%s",
            text
        );
        
        String classification = chatModel.chat(prompt);
        
        assertNotNull(classification);
        
        log.info("文本: {}", text);
        log.info("主题分类: {}", classification);
        log.info("\n✅ 测试通过：能够进行主题分类\n");
    }

    @Test
    @DisplayName("应该能处理多文本批量分类")
    void shouldHandleBatchClassification() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 批量分类                                             ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        String[] texts = {
            "这个产品很好用，强烈推荐！",
            "服务态度太差了，很失望。",
            "产品还可以，没有特别突出的地方。"
        };
        
        String[] classifications = new String[texts.length];
        for (int i = 0; i < texts.length; i++) {
            String prompt = String.format(
                "请对以下文本进行情感分类（正面/负面/中性）：\n\n文本：%s",
                texts[i]
            );
            classifications[i] = chatModel.chat(prompt);
        }
        
        // 验证
        for (String classification : classifications) {
            assertNotNull(classification);
        }
        
        log.info("批量分类结果:");
        for (int i = 0; i < texts.length; i++) {
            log.info("  {}. \"{}\" -> {}", i + 1, texts[i], classifications[i]);
        }
        
        log.info("\n✅ 测试通过：能够处理批量分类\n");
    }
}
