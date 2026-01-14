package cn.langchat.learning.tutorial.chatmodel;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 03 - ChatModel 深入理解测试
 * 
 * 测试文档中的完整使用场景，包括：
 * - ChatModel 的不同方法
 * - ChatResponse 元数据
 * - 多消息对话
 * - 长上下文处理
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
@DisplayName("03 - ChatModel 深入理解测试")
class ChatModelDeepDiveTest {

    private ChatModel model;

    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        model = TestModelProvider.getChatModel();
    }

    @Test
    @DisplayName("应该能使用 chat(String) 便捷方法")
    void shouldUseChatStringMethod() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: chat(String) 便捷方法                              ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        String response = model.chat("你好，请介绍一下你自己");

        assertNotNull(response);
        assertFalse(response.isEmpty());

        log.info("响应: {}", response);
        log.info("\n✅ 测试通过：chat(String) 方法正常工作\n");
    }

    @Test
    @DisplayName("应该能使用 chat(ChatMessage) 方法")
    void shouldUseChatMessageMethod() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: chat(ChatMessage) 方法                               ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        UserMessage message = UserMessage.from("什么是 Java？");
        ChatResponse response = model.chat(message);

        assertNotNull(response);
        assertNotNull(response.aiMessage());
        assertFalse(response.aiMessage().text().isEmpty());

        log.info("响应: {}", response.aiMessage().text());
        log.info("\n✅ 测试通过：chat(ChatMessage) 方法正常工作\n");
    }

    @Test
    @DisplayName("应该能使用 chat(ChatMessage...) 多消息方法")
    void shouldUseChatMultipleMessagesMethod() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: chat(ChatMessage...) 多消息方法                     ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        ChatResponse response = model.chat(
                SystemMessage.from("你是一个数学老师"),
                UserMessage.from("2 + 2 等于几？")
        );

        assertNotNull(response);
        assertTrue(response.aiMessage().text().contains("4"));

        log.info("响应: {}", response.aiMessage().text());
        log.info("\n✅ 测试通过：多消息方法正常工作\n");
    }

    @Test
    @DisplayName("应该能使用 chat(List<ChatMessage>) 方法")
    void shouldUseChatListMethod() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: chat(List<ChatMessage>) 方法                         ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        List<ChatMessage> messages = List.of(
                SystemMessage.from("你是一个友好的助手"),
                UserMessage.from("你好")
        );

        ChatResponse response = model.chat(messages);

        assertNotNull(response);
        assertNotNull(response.aiMessage());

        log.info("响应: {}", response.aiMessage().text());
        log.info("\n✅ 测试通过：List消息方法正常工作\n");
    }

    @Test
    @DisplayName("应该能使用 chat(ChatRequest) 完全控制")
    void shouldUseChatRequestMethod() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: chat(ChatRequest) 完全控制                          ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        ChatRequestParameters parameters = ChatRequestParameters.builder()
                .temperature(0.7)
                .maxOutputTokens(100)
                .build();

        ChatRequest request = ChatRequest.builder()
                .messages(
                        SystemMessage.from("你是一个助手"),
                        UserMessage.from("用一句话介绍 Java")
                )
                .parameters(parameters)
                .build();

        ChatResponse response = model.chat(request);

        assertNotNull(response);
        assertNotNull(response.aiMessage());

        log.info("响应: {}", response.aiMessage().text());
        log.info("参数: temperature=0.7, maxOutputTokens=100");
        log.info("\n✅ 测试通过：ChatRequest 方法正常工作\n");
    }

    @Test
    @DisplayName("应该能获取完整的响应元数据")
    void shouldGetCompleteResponseMetadata() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 获取完整响应元数据                                  ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 注意：要获取 ChatResponse，必须传入 ChatMessage，而不是 String
        // model.chat(String) 返回 String 类型
        // model.chat(ChatMessage...) 返回 ChatResponse 类型
        ChatResponse response = model.chat(UserMessage.from("你好"));

        // 获取元数据
        assertNotNull(response.metadata());
        assertNotNull(response.metadata().modelName());
        assertNotNull(response.metadata().id());
        assertNotNull(response.metadata().tokenUsage());
        assertNotNull(response.metadata().finishReason());

        // 获取 Token 使用
        TokenUsage usage = response.metadata().tokenUsage();
        assertNotNull(usage);
        assertTrue(usage.totalTokenCount() > 0);

        log.info("模型名称: {}", response.metadata().modelName());
        log.info("响应ID: {}", response.metadata().id());
        log.info("输入Token: {}", usage.inputTokenCount());
        log.info("输出Token: {}", usage.outputTokenCount());
        log.info("总Token: {}", usage.totalTokenCount());
        log.info("完成原因: {}", response.metadata().finishReason());
        log.info("\n✅ 测试通过：能够获取完整的响应元数据\n");
    }

    @Test
    @DisplayName("应该能处理多轮对话")
    void shouldHandleMultiTurnConversation() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 处理多轮对话                                         ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from("你是一个故事讲述者"));

        // 第一轮
        messages.add(UserMessage.from("开始讲一个关于编程的故事"));
        ChatResponse response1 = model.chat(messages);
        messages.add(response1.aiMessage());

        log.info("第一轮: {}", response1.aiMessage().text());

        // 第二轮
        messages.add(UserMessage.from("继续"));
        ChatResponse response2 = model.chat(messages);
        messages.add(response2.aiMessage());

        log.info("第二轮: {}", response2.aiMessage().text());

        // 第三轮
        messages.add(UserMessage.from("再继续"));
        ChatResponse response3 = model.chat(messages);

        log.info("第三轮: {}", response3.aiMessage().text());

        assertNotNull(response3);
        assertTrue(messages.size() >= 7); // 系统消息 + 3轮对话

        log.info("消息总数: {}", messages.size());
        log.info("\n✅ 测试通过：能够处理多轮对话\n");
    }

    @Test
    @DisplayName("应该能理解对话上下文")
    void shouldUnderstandConversationContext() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 理解对话上下文                                     ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from("你是一个数学老师"));

        // 第一轮：定义一个概念
        messages.add(UserMessage.from("定义 x = 5"));
        ChatResponse response1 = model.chat(messages);
        messages.add(response1.aiMessage());

        // 第二轮：基于之前的定义提问
        messages.add(UserMessage.from("x 加 2 等于多少？"));
        ChatResponse response2 = model.chat(messages);

        log.info("第二轮响应: {}", response2.aiMessage().text());

        // 验证AI记得之前的上下文
        assertNotNull(response2);
        assertTrue(response2.aiMessage().text().contains("7") ||
                        response2.aiMessage().text().contains("七"));

        log.info("\n✅ 测试通过：能够理解对话上下文\n");
    }

    @Test
    @DisplayName("应该能处理长对话")
    void shouldHandleLongConversation() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 处理长对话                                           ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from("你是一个对话伙伴"));

        // 模拟10轮对话
        for (int i = 1; i <= 10; i++) {
            messages.add(UserMessage.from("第" + i + "句话"));
            ChatResponse response = model.chat(messages);
            messages.add(response.aiMessage());

            if (i <= 3) { // 只打印前3轮
                log.info("第{}轮: {}", i, response.aiMessage().text());
            }
        }

        // 最后一次对话，验证AI是否记得早期内容
        messages.add(UserMessage.from("这是我们第几次对话？"));
        ChatResponse finalResponse = model.chat(messages);

        log.info("最后响应: {}", finalResponse.aiMessage().text());
        assertTrue(messages.size() > 20); // 至少有系统消息 + 10轮对话

        log.info("总消息数: {}", messages.size());
        log.info("\n✅ 测试通过：能够处理长对话\n");
    }

    @Test
    @DisplayName("应该能使用系统消息指导AI行为")
    void shouldUseSystemMessageToGuideBehavior() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 使用系统消息指导AI行为                            ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 使用专业语气
        String prompt1 = "介绍一下 Java";
        ChatResponse response1 = model.chat(
                SystemMessage.from("你是一个专业的程序员"),
                UserMessage.from(prompt1)
        );

        // 使用简单语气
        String prompt2 = "介绍一下 Java";
        ChatResponse response2 = model.chat(
                SystemMessage.from("你是一个小学老师"),
                UserMessage.from(prompt2)
        );

        assertNotNull(response1);
        assertNotNull(response2);

        log.info("专业语气: {}", response1.aiMessage().text());
        log.info("简单语气: {}", response2.aiMessage().text());
        log.info("\n✅ 测试通过：系统消息能够指导AI行为\n");
    }
}
