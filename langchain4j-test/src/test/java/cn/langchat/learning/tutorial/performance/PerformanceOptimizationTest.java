package cn.langchat.learning.tutorial.performance;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 25 - 性能优化测试
 */
@Slf4j
@DisplayName("25 - 性能优化测试")
class PerformanceOptimizationTest {
    
    private ChatModel chatModel;
    
    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        chatModel = TestModelProvider.getChatModel();
    }
    
    @Test
    @DisplayName("应该能测量响应时间")
    void shouldMeasureResponseTime() {
        log.info("测试: 测量响应时间");
        
        long startTime = System.currentTimeMillis();
        String response = chatModel.chat("你好");
        long endTime = System.currentTimeMillis();
        
        assertNotNull(response);
        assertTrue(endTime - startTime > 0);
        
        log.info("响应时间: {} ms", endTime - startTime);
        log.info("✅ 测试通过\n");
    }
}
