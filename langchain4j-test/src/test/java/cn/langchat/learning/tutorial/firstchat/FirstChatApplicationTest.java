package cn.langchat.learning.tutorial.firstchat;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 02 - 第一个 Chat 应用测试
 * 
 * 测试文档中的完整使用场景，包括：
 * - 简单字符串消息
 * - 使用 ChatMessage
 * - 多消息对话
 * - 使用 ChatRequest 完全控制
 * - 获取完整响应信息
 * - Token 使用统计
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
@DisplayName("02 - 第一个 Chat 应用测试")
class FirstChatApplicationTest {

    private ChatModel model;

    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        model = TestModelProvider.getChatModel();
    }

    @Test
    @DisplayName("应该能够发送简单的字符串消息")
    void shouldSendSimpleStringMessage() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 简单字符串消息                                       ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // Given - 准备测试数据
        String userMessage = "你好，请介绍一下你自己";

        // When - 发送消息
        String response = model.chat(userMessage);

        // Then - 验证响应
        assertNotNull(response, "响应不应为空");
        assertFalse(response.isEmpty(), "响应不应为空字符串");

        log.info("用户: {}", userMessage);
        log.info("AI: {}", response);
        log.info("\n✅ 测试通过：能够发送简单的字符串消息\n");
    }

    @Test
    @DisplayName("应该能够使用 ChatMessage 发送消息")
    void shouldSendChatMessage() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 使用 ChatMessage 发送消息                            ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // Given
        UserMessage userMessage = UserMessage.from("你好，请介绍一下你自己");

        // When
        ChatResponse response = model.chat(userMessage);
        AiMessage aiMessage = response.aiMessage();

        // Then
        assertNotNull(response, "响应不应为空");
        assertNotNull(aiMessage, "AI消息不应为空");
        assertFalse(aiMessage.text().isEmpty(), "AI消息内容不应为空");

        log.info("用户: {}", userMessage.contents());
        log.info("AI: {}", aiMessage.text());
        log.info("\n✅ 测试通过：能够使用 ChatMessage 发送消息\n");
    }

    @Test
    @DisplayName("应该能够处理多消息对话")
    void shouldHandleMultipleMessages() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 多消息对话                                             ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // Given - 构建对话历史
        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from("你是一个专业的 Java 编程助手"));
        messages.add(UserMessage.from("什么是 Java？"));

        // When - 第一轮对话
        log.info("--- 第一轮对话 ---");
        ChatResponse response1 = model.chat(messages);
        messages.add(response1.aiMessage());
        log.info("用户: 什么是 Java？");
        log.info("AI: {}\n", response1.aiMessage().text());

        // When - 第二轮对话
        messages.add(UserMessage.from("Java 有哪些特点？"));
        log.info("--- 第二轮对话 ---");
        ChatResponse response2 = model.chat(messages);
        log.info("用户: Java 有哪些特点？");
        log.info("AI: {}", response2.aiMessage().text());

        // Then
        assertNotNull(response1, "第一轮响应不应为空");
        assertNotNull(response2, "第二轮响应不应为空");
        assertFalse(response2.aiMessage().text().isEmpty(), "第二轮响应内容不应为空");

        log.info("\n✅ 测试通过：能够处理多消息对话\n");
    }

    @Test
    @DisplayName("应该能够使用 ChatRequest 完全控制")
    void shouldUseChatRequest() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 使用 ChatRequest 完全控制                            ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // Given - 构建请求参数
        ChatRequestParameters parameters = ChatRequestParameters.builder()
                .temperature(0.7)
                .topP(0.9)
                .maxOutputTokens(500)
                .build();

        // 构建完整请求
        ChatRequest request = ChatRequest.builder()
                .messages(
                        SystemMessage.from("你是一个专业的助手"),
                        UserMessage.from("什么是机器学习？")
                )
                .parameters(parameters)
                .build();

        // When - 发送请求
        ChatResponse response = model.chat(request);

        // Then
        assertNotNull(response, "响应不应为空");
        assertNotNull(response.aiMessage(), "AI消息不应为空");
        assertFalse(response.aiMessage().text().isEmpty(), "响应内容不应为空");

        log.info("系统提示: 你是一个专业的助手");
        log.info("用户: 什么是机器学习？");
        log.info("AI: {}", response.aiMessage().text());
        log.info("参数: temperature=0.7, topP=0.9, maxOutputTokens=500");
        log.info("\n✅ 测试通过：能够使用 ChatRequest 完全控制\n");
    }

    @Test
    @DisplayName("应该能够获取完整响应信息")
    void shouldGetCompleteResponse() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 获取完整响应信息                                       ║");
        log.info("╚═════════════════════════════════════════════════════════════════════╣\n");

        // When - 注意：要获取 ChatResponse，必须传入 ChatMessage
        ChatResponse response = model.chat(UserMessage.from("请简单介绍一下 LangChain4j"));

        // Then - 获取 AI 消息
        AiMessage aiMessage = response.aiMessage();
        String text = aiMessage.text();

        // 获取响应元数据
        ChatResponseMetadata metadata = response.metadata();
        String modelId = metadata.modelName();
        String responseId = metadata.id();
        TokenUsage tokenUsage = metadata.tokenUsage();
        FinishReason finishReason = metadata.finishReason();

        // Token 使用详情
        int inputTokens = tokenUsage.inputTokenCount();
        int outputTokens = tokenUsage.outputTokenCount();
        int totalTokens = tokenUsage.totalTokenCount();

        // 验证
        assertNotNull(response, "响应不应为空");
        assertNotNull(aiMessage, "AI消息不应为空");
        assertNotNull(metadata, "元数据不应为空");
        assertNotNull(modelId, "模型ID不应为空");
        assertNotNull(tokenUsage, "Token使用信息不应为空");

        log.info("响应文本: {}", text);
        log.info("╠═══════════════════════════════════════════════════════════════════════╣");
        log.info("元数据:");
        log.info("  模型: {}", modelId);
        log.info("  响应ID: {}", responseId);
        log.info("  完成原因: {}", finishReason);
        log.info("╠═══════════════════════════════════════════════════════════════════════╣");
        log.info("Token 使用:");
        log.info("  输入 Token: {}", inputTokens);
        log.info("  输出 Token: {}", outputTokens);
        log.info("  总 Token: {}", totalTokens);
        log.info("\n✅ 测试通过：能够获取完整响应信息\n");
    }

    @Test
    @DisplayName("应该能够回答简单问题")
    void shouldAnswerSimpleQuestion() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 回答简单问题                                           ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // Given
        String question = "2 + 2 等于几？";

        // When
        String response = model.chat(question);

        // Then
        assertNotNull(response, "响应不应为空");
        assertTrue(response.contains("4") || response.contains("四"), 
                "响应应该包含 4 或 四");

        log.info("问题: {}", question);
        log.info("回答: {}", response);
        log.info("\n✅ 测试通过：能够回答简单问题\n");
    }

    @Test
    @DisplayName("应该能够生成代码")
    void shouldGenerateCode() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 生成代码                                                ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // Given
        String request = "写一个 Java 的 Hello World 程序";

        // When
        String response = model.chat(request);

        // Then
        assertNotNull(response, "响应不应为空");
        assertTrue(response.contains("public") || response.contains("class"), 
                "响应应该包含 public 或 class");
        assertTrue(response.contains("System.out.println"), 
                "响应应该包含 System.out.println");

        log.info("请求: {}", request);
        log.info("生成的代码:\n{}", response);
        log.info("\n✅ 测试通过：能够生成代码\n");
    }

    @Test
    @DisplayName("应该能够遵循指令")
    void shouldFollowInstructions() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 遵循指令                                               ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // Given
        String instruction = "请用一句话介绍 Java";

        // When
        String response = model.chat(instruction);

        // Then
        assertNotNull(response, "响应不应为空");
        // 检查是否是一句话（简单检查句号数量）
        long sentenceCount = response.chars()
                .filter(ch -> ch == '。' || ch == '.' || ch == '!')
                .count();
        assertTrue(sentenceCount <= 2, 
                "响应应该包含最多2个句子，实际: " + sentenceCount);

        log.info("指令: {}", instruction);
        log.info("回答: {}", response);
        log.info("句子数: {}", sentenceCount);
        log.info("\n✅ 测试通过：能够遵循指令\n");
    }

    @Test
    @DisplayName("应该能够处理空消息")
    void shouldHandleEmptyMessage() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 处理空消息                                             ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // Given
        String emptyMessage = "";

        // When
        String response = model.chat(emptyMessage);

        // Then
        assertNotNull(response, "即使空消息也应返回响应");

        log.info("空消息: [空]");
        log.info("响应: {}", response);
        log.info("\n✅ 测试通过：能够处理空消息\n");
    }

    @Test
    @DisplayName("应该能够用中文响应中文请求")
    void shouldRespondInChinese() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 中文响应                                               ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // Given
        String message = "你是谁？";

        // When
        String response = model.chat(message);

        // Then
        assertNotNull(response, "响应不应为空");
        // 验证响应中包含中文字符
        assertTrue(response.matches(".*[\\u4e00-\\u9fa5].*"), 
                "响应应该包含中文字符");

        log.info("问题: {}", message);
        log.info("回答: {}", response);
        log.info("\n✅ 测试通过：能够用中文响应中文请求\n");
    }

    @Test
    @DisplayName("应该能够理解完成原因")
    void shouldUnderstandFinishReason() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 理解完成原因                                           ║");
        log.info("╚═════════════════════════════════════════════════════════════════════╣\n");

        // When - 注意：要获取 ChatResponse，必须传入 ChatMessage
        ChatResponse response = model.chat(UserMessage.from("讲一个简短的故事"));
        FinishReason finishReason = response.metadata().finishReason();

        // Then
        assertNotNull(finishReason, "完成原因不应为空");

        log.info("请求: 讲一个简短的故事");
        log.info("响应: {}", response.aiMessage().text());
        log.info("完成原因: {}", finishReason);

        switch (finishReason) {
            case STOP:
                log.info("说明: 正常结束");
                break;
            case LENGTH:
                log.info("说明: 达到最大 Token 限制");
                break;
            case CONTENT_FILTER:
                log.info("说明: 内容被过滤");
                break;
            default:
                log.info("说明: 其他原因");
        }

        log.info("\n✅ 测试通过：能够理解完成原因\n");
    }
}
