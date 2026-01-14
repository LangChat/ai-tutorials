package cn.langchat.learning.tutorial.tools;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 09 - Tools 和 Function Calling 测试
 * 
 * 测试文档中的完整使用场景，包括：
 * - Tool 的基本定义和使用
 * - Function Calling 功能
 * - 自定义 Tool 创建
 * - Tool 参数和返回值
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
@DisplayName("09 - Tools 和 Function Calling 测试")
class ToolsTest {

    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        // 注意：Tools 功能可能需要特定模型支持
        this.chatModel = OpenAiChatModel.builder()
                .apiKey(TestModelProvider.getApiKey())
                .baseUrl(TestModelProvider.getBaseUrl())
                .modelName(TestModelProvider.getModelName())
                .build();
    }

    @Test
    @DisplayName("应该能定义和使用 Tool")
    void shouldDefineAndUseTool() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 定义和使用 Tool                                         ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 定义一个简单的 Tool 类
        class DateTool {
            @Tool("获取当前日期")
            public String getCurrentDate() {
                return LocalDate.now().toString();
            }
        }

        // 验证 Tool 方法存在
        DateTool tool = new DateTool();
        String date = tool.getCurrentDate();
        assertNotNull(date);
        assertFalse(date.isEmpty());
        
        log.info("Tool 执行成功，返回: {}", date);
        log.info("\n✅ 测试通过：能够定义和使用 Tool\n");
    }

    @Test
    @DisplayName("应该能定义带参数的 Tool")
    void shouldDefineToolWithParameters() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 定义带参数的 Tool                                   ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 定义带参数的 Tool 类
        class MathTool {
            @Tool("计算两个数的和")
            public int add(int a, int b) {
                return a + b;
            }
        }

        // 验证 Tool 方法
        MathTool tool = new MathTool();
        int result = tool.add(3, 5);
        assertEquals(8, result);
        
        log.info("Tool 执行成功: add(3, 5) = {}", result);
        log.info("\n✅ 测试通过：能够定义带参数的 Tool\n");
    }

    @Test
    @DisplayName("应该能创建 ToolSpecification")
    void shouldCreateToolSpecification() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 创建 ToolSpecification                                   ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 创建 ToolSpecification
        ToolSpecification spec = ToolSpecification.builder()
                .name("get_weather")
                .description("获取指定城市的天气信息")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("city", "城市名称")
                        .build())
                .build();

        assertNotNull(spec);
        assertEquals("get_weather", spec.name());
        assertEquals("获取指定城市的天气信息", spec.description());
        
        log.info("ToolSpecification 创建成功:");
        log.info("  名称: {}", spec.name());
        log.info("  描述: {}", spec.description());
        log.info("  参数: {}", spec.parameters());
        log.info("\n✅ 测试通过：能够创建 ToolSpecification\n");
    }

    @Test
    @DisplayName("应该能执行 Tool 并处理返回值")
    void shouldExecuteToolAndHandleReturnValue() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 执行 Tool 并处理返回值                                ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 定义 Tool 类
        class UserTool {
            @Tool("获取用户信息")
            public String getUserInfo(String userId) {
                // 模拟数据库查询
                return "用户ID: " + userId + ", 姓名: 张三, 邮箱: zhangsan@example.com";
            }
        }

        // 执行 Tool
        UserTool tool = new UserTool();
        String result = tool.getUserInfo("user123");
        
        assertNotNull(result);
        assertTrue(result.contains("user123"));
        assertTrue(result.contains("张三"));
        
        log.info("Tool 执行结果:");
        log.info("  {}", result);
        log.info("\n✅ 测试通过：能够执行 Tool 并处理返回值\n");
    }

    @Test
    @DisplayName("应该能处理多个 Tool")
    void shouldHandleMultipleTools() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 处理多个 Tool                                          ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 定义多个 Tool 类
        class DateTimeTool {
            @Tool("获取日期")
            public String getDate() {
                return LocalDate.now().toString();
            }

            @Tool("获取时间")
            public String getTime() {
                return java.time.LocalTime.now().toString();
            }
        }

        class MathTool {
            @Tool("计算平方")
            public int square(int x) {
                return x * x;
            }
        }

        // 执行多个 Tool
        DateTimeTool dateTimeTool = new DateTimeTool();
        MathTool mathTool = new MathTool();
        
        String date = dateTimeTool.getDate();
        String time = dateTimeTool.getTime();
        int squared = mathTool.square(5);

        log.info("多个 Tool 执行结果:");
        log.info("  getDate(): {}", date);
        log.info("  getTime(): {}", time);
        log.info("  square(5): {}", squared);
        log.info("\n✅ 测试通过：能够处理多个 Tool\n");
    }

    @Test
    @DisplayName("应该能处理 Tool 执行异常")
    void shouldHandleToolExecutionException() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 处理 Tool 执行异常                                      ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 定义可能抛出异常的 Tool 类
        class DivisionTool {
            @Tool("除法运算")
            public double divide(double a, double b) {
                if (b == 0) {
                    throw new IllegalArgumentException("除数不能为零");
                }
                return a / b;
            }
        }

        DivisionTool tool = new DivisionTool();
        
        // 正常情况
        double result1 = tool.divide(10, 2);
        assertEquals(5.0, result1);
        log.info("divide(10, 2) = {}", result1);

        // 异常情况
        assertThrows(IllegalArgumentException.class, () -> {
            tool.divide(10, 0);
        });
        log.info("正确捕获异常: 除数不能为零");

        log.info("\n✅ 测试通过：能够正确处理 Tool 执行异常\n");
    }
}
