package cn.langchat.learning.tutorial.observability;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 16 - 可观测性测试
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
@DisplayName("16 - 可观测性测试")
class ObservabilityTest {

    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        chatModel = TestModelProvider.getChatModel();
    }

    @Test
    @DisplayName("应该能监控模型性能")
    void shouldMonitorPerformance() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 监控模型性能                                         ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        long startTime = System.currentTimeMillis();
        String response = chatModel.chat("你好");
        long endTime = System.currentTimeMillis();
        
        long responseTime = endTime - startTime;
        
        assertNotNull(response);
        assertTrue(responseTime > 0);
        
        log.info("响应时间: {} ms", responseTime);
        log.info("响应内容: {}", response);
        log.info("\n✅ 测试通过：能够监控模型性能\n");
    }
}
