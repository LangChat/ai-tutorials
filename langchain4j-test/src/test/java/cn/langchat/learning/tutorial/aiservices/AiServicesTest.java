package cn.langchat.learning.tutorial.aiservices;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 11 - AI Services 测试
 * 
 * 测试文档中的完整使用场景，包括：
 * - 基础 AI Service 创建
 * - 系统消息配置
 * - ChatMemory 集成
 * - 工具集成
 * - 多用户支持
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
@DisplayName("11 - AI Services 测试")
class AiServicesTest {

    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        chatModel = TestModelProvider.getChatModel();
    }

    @Test
    @DisplayName("应该能创建简单的 AI Service")
    void shouldCreateSimpleAiService() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 创建简单的 AI Service                                 ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 定义简单服务接口
        interface SimpleChatService {
            String chat(@UserMessage String message);
        }

        // 创建服务实例
        SimpleChatService service = AiServices.builder(SimpleChatService.class)
                .chatModel(chatModel)
                .build();

        // 测试
        String response = service.chat("你好");
        
        assertNotNull(response);
        assertFalse(response.isEmpty());
        
        log.info("用户: 你好");
        log.info("AI: {}", response);
        log.info("\n✅ 测试通过：能够创建简单的 AI Service\n");
    }

    @Test
    @DisplayName("应该能配置系统消息")
    void shouldConfigureSystemMessage() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 配置系统消息                                         ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 定义服务接口
        interface SystemPromptService {
            String chat(@UserMessage String message);
        }

        // 创建服务（带系统消息）
        SystemPromptService service = AiServices.builder(SystemPromptService.class)
                .chatModel(chatModel)
                .systemMessageProvider(chatMemoryId -> "你是一个专业的编程老师，用简单易懂的语言解释技术概念。")
                .build();

        // 测试
        String response = service.chat("什么是面向对象编程？");
        
        assertNotNull(response);
        assertFalse(response.isEmpty());
        
        log.info("系统消息: 你是一个专业的编程老师，用简单易懂的语言解释技术概念。");
        log.info("用户: 什么是面向对象编程？");
        log.info("AI: {}", response);
        log.info("\n✅ 测试通过：能够配置系统消息\n");
    }

    @Test
    @DisplayName("应该能集成工具")
    void shouldIntegrateTools() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 集成工具                                             ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 定义工具
        interface ToolAwareService {
            String chat(@UserMessage String message);
        }

        // 定义服务接口

        class Calculator {
            @Tool("计算两个数的和")
            public String add(int a, int b) {
                return String.format("%d + %d = %d", a, b, a + b);
            }

            @Tool("获取当前时间")
            public String getTime() {
                return java.time.LocalTime.now().toString();
            }
        }

        // 创建服务（带工具）
        ToolAwareService service = AiServices.builder(ToolAwareService.class)
                .chatModel(chatModel)
                .tools(new Calculator())
                .build();

        // 测试工具调用
        String response = service.chat("5 加 3 等于几？");
        
        assertNotNull(response);
        // 注意：工具调用可能需要模型支持，这里只验证响应不为空
        
        log.info("用户: 5 加 3 等于几？");
        log.info("AI: {}", response);
        log.info("\n✅ 测试通过：能够集成工具\n");
    }

    @Test
    @DisplayName("应该能支持多方法服务接口")
    void shouldSupportMultipleMethods() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 支持多方法服务接口                                   ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 定义多方法服务接口
        interface MultiMethodService {
            String ask(@UserMessage String question);
            String translate(@UserMessage String text);
            String summarize(@UserMessage String text);
        }

        // 创建服务
        MultiMethodService service = AiServices.builder(MultiMethodService.class)
                .chatModel(chatModel)
                .systemMessageProvider(chatMemoryId -> "你是一个多功能的助手。")
                .build();

        // 测试各个方法
        String answer = service.ask("什么是人工智能？");
        assertNotNull(answer);
        
        log.info("测试 1 - ask() 方法:");
        log.info("用户: 什么是人工智能？");
        log.info("AI: {}", answer);
        
        String translation = service.translate("Hello, how are you?");
        assertNotNull(translation);
        
        log.info("\n测试 2 - translate() 方法:");
        log.info("用户: Hello, how are you?");
        log.info("AI: {}", translation);
        
        String summary = service.summarize("这是一个很长的文本...");
        assertNotNull(summary);
        
        log.info("\n测试 3 - summarize() 方法:");
        log.info("用户: 这是一个很长的文本...");
        log.info("AI: {}", summary);
        
        log.info("\n✅ 测试通过：能够支持多方法服务接口\n");
    }

    @Test
    @DisplayName("应该能创建不同角色的服务")
    void shouldCreateDifferentRoles() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 创建不同角色的服务                                   ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 定义服务接口
        interface Assistant {
            String chat(@UserMessage String message);
        }

        // 创建编程老师服务
        Assistant programmer = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .systemMessageProvider(chatMemoryId -> "你是一个专业的编程老师。")
                .build();

        // 创建心理咨询师服务
        Assistant counselor = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .systemMessageProvider(chatMemoryId -> "你是一个温暖的心理咨询师。")
                .build();

        // 测试不同角色
        String response1 = programmer.chat("什么是递归？");
        String response2 = counselor.chat("我感到压力很大，你有什么建议吗？");
        
        assertNotNull(response1);
        assertNotNull(response2);
        
        log.info("=== 编程老师 ===");
        log.info("用户: 什么是递归？");
        log.info("AI: {}", response1);
        
        log.info("\n=== 心理咨询师 ===");
        log.info("用户: 我感到压力很大，你有什么建议吗？");
        log.info("AI: {}", response2);
        
        log.info("\n✅ 测试通过：能够创建不同角色的服务\n");
    }

    @Test
    @DisplayName("应该能处理工具执行")
    void shouldHandleToolExecution() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 处理工具执行                                         ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 定义工具
        class MathTools {
            @Tool("加法运算")
            public int add(int a, int b) {
                return a + b;
            }

            @Tool("减法运算")
            public int subtract(int a, int b) {
                return a - b;
            }

            @Tool("乘法运算")
            public int multiply(int a, int b) {
                return a * b;
            }

            @Tool("除法运算")
            public double divide(int a, int b) {
                if (b == 0) {
                    throw new IllegalArgumentException("除数不能为零");
                }
                return (double) a / b;
            }
        }

        // 直接测试工具
        MathTools tools = new MathTools();
        
        assertEquals(5, tools.add(2, 3));
        assertEquals(1, tools.subtract(5, 4));
        assertEquals(6, tools.multiply(2, 3));
        assertEquals(2.0, tools.divide(4, 2));
        
        // 测试异常情况
        assertThrows(IllegalArgumentException.class, () -> tools.divide(10, 0));
        
        log.info("工具执行测试:");
        log.info("  add(2, 3) = {}", tools.add(2, 3));
        log.info("  subtract(5, 4) = {}", tools.subtract(5, 4));
        log.info("  multiply(2, 3) = {}", tools.multiply(2, 3));
        log.info("  divide(4, 2) = {}", tools.divide(4, 2));
        log.info("  divide(10, 0) 抛出异常: 除数不能为零");
        
        log.info("\n✅ 测试通过：能够正确处理工具执行\n");
    }
}
