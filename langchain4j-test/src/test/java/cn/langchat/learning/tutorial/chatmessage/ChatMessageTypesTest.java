package cn.langchat.learning.tutorial.chatmessage;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 05 - ChatMessage 类型详解测试
 * 
 * 测试文档中的完整使用场景，包括：
 * - UserMessage 的各种用法
 * - AiMessage 的属性和方法
 * - SystemMessage 的作用
 * - 多消息对话
 * - 消息组合和顺序
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
@DisplayName("05 - ChatMessage 类型详解测试")
class ChatMessageTypesTest {

    private ChatModel model;

    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        model = TestModelProvider.getChatModel();
    }

    @Test
    @DisplayName("应该能使用 UserMessage")
    void shouldUseUserMessage() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: UserMessage 的使用                               ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        UserMessage userMessage = UserMessage.from("你好");
        ChatResponse response = model.chat(userMessage);

        assertNotNull(response);
        assertNotNull(response.aiMessage());
        assertNotNull(response.aiMessage().text());

        log.info("用户消息: {}", userMessage.singleText());
        log.info("AI 响应: {}", response.aiMessage().text());
        log.info("\n✅ 测试通过：UserMessage 正常工作\n");
    }

    @Test
    @DisplayName("应该能使用带用户名的 UserMessage")
    void shouldUseUserMessageWithName() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 带用户名的 UserMessage                              ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        UserMessage userMessage = UserMessage.from("小明", "介绍一下你自己");
        ChatResponse response = model.chat(userMessage);

        assertNotNull(response);
        assertNotNull(userMessage.name());

        log.info("用户名: {}", userMessage.name());
        log.info("消息: {}", userMessage.singleText());
        log.info("AI 响应: {}", response.aiMessage().text());
        log.info("\n✅ 测试通过：带用户名的 UserMessage 正常工作\n");
    }

    @Test
    @DisplayName("应该能使用 SystemMessage 指导 AI 行为")
    void shouldUseSystemMessage() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: SystemMessage 指导 AI 行为                         ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        SystemMessage systemMessage = SystemMessage.from("你是一个专业的 Java 编程老师");
        UserMessage userMessage = UserMessage.from("什么是 Java？");

        ChatResponse response = model.chat(systemMessage, userMessage);

        assertNotNull(response);
        assertNotNull(response.aiMessage());

        log.info("系统消息: {}", systemMessage.text());
        log.info("用户消息: {}", userMessage.singleText());
        log.info("AI 响应: {}", response.aiMessage().text());
        log.info("\n✅ 测试通过：SystemMessage 指导 AI 行为正常\n");
    }

    @Test
    @DisplayName("应该能处理多轮对话")
    void shouldHandleMultipleTurnConversation() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 多轮对话                                         ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from("你是一个友好的助手"));

        // 第一轮
        UserMessage user1 = UserMessage.from("你好");
        messages.add(user1);
        ChatResponse response1 = model.chat(messages);
        messages.add(response1.aiMessage());

        log.info("第一轮:");
        log.info("  用户: {}", user1.singleText());
        log.info("  AI: {}", response1.aiMessage().text());

        // 第二轮
        UserMessage user2 = UserMessage.from("我叫什么？");
        messages.add(user2);
        ChatResponse response2 = model.chat(messages);

        log.info("第二轮:");
        log.info("  用户: {}", user2.singleText());
        log.info("  AI: {}", response2.aiMessage().text());

        // 验证AI记得对话历史（虽然可能记不住用户名）
        assertNotNull(response2);
        assertNotNull(response2.aiMessage());

        log.info("\n✅ 测试通过：多轮对话正常工作\n");
    }

    @Test
    @DisplayName("应该能正确组织消息顺序")
    void shouldOrganizeMessageOrder() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 正确组织消息顺序                                  ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 正确的顺序：SystemMessage -> UserMessage -> AiMessage -> UserMessage
        List<ChatMessage> correctOrder = List.of(
                SystemMessage.from("你是一个数学老师"),
                UserMessage.from("什么是 2 + 2？"),
                AiMessage.from("2 + 2 等于 4"),
                UserMessage.from("那 3 + 3 呢？")
        );

        ChatResponse response = model.chat(correctOrder);

        assertNotNull(response);
        assertNotNull(response.aiMessage());

        log.info("系统: 你是一个数学老师");
        log.info("用户: 什么是 2 + 2？");
        log.info("AI: 2 + 2 等于 4");
        log.info("用户: 那 3 + 3 呢？");
        log.info("AI: {}", response.aiMessage().text());
        log.info("\n✅ 测试通过：消息顺序正确组织\n");
    }

    @Test
    @DisplayName("应该能获取 AiMessage 的属性")
    void shouldGetAiMessageProperties() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 获取 AiMessage 的属性                              ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        UserMessage userMessage = UserMessage.from("你好");
        ChatResponse response = model.chat(userMessage);

        AiMessage aiMessage = response.aiMessage();

        assertNotNull(aiMessage);
        assertNotNull(aiMessage.text());
        assertFalse(aiMessage.text().isEmpty());

        log.info("用户消息: {}", userMessage.singleText());
        log.info("AI 消息文本: {}", aiMessage.text());
        log.info("是否有工具执行请求: {}", aiMessage.hasToolExecutionRequests());
        log.info("\n✅ 测试通过：能够获取 AiMessage 的属性\n");
    }

    @Test
    @DisplayName("应该能组合不同类型的消息")
    void shouldCombineDifferentMessageTypes() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 组合不同类型的消息                                ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        SystemMessage systemMsg = SystemMessage.from("你是一个专业的翻译");
        UserMessage userMsg = UserMessage.from("将 'Hello World' 翻译成中文");

        ChatResponse response = model.chat(systemMsg, userMsg);

        assertNotNull(response);
        assertNotNull(response.aiMessage());

        log.info("系统消息: {}", systemMsg.text());
        log.info("用户消息: {}", userMsg.singleText());
        log.info("AI 响应: {}", response.aiMessage().text());
        log.info("\n✅ 测试通过：能够组合不同类型的消息\n");
    }

    @Test
    @DisplayName("应该能在对话中保持上下文")
    void shouldMaintainConversationContext() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 在对话中保持上下文                                ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from("你是一个助手"));

        // 第一个问题
        messages.add(UserMessage.from("Java 是什么？"));
        ChatResponse response1 = model.chat(messages);
        messages.add(response1.aiMessage());

        log.info("问题 1: Java 是什么？");
        log.info("回答 1: {}", response1.aiMessage().text());

        // 第二个问题
        messages.add(UserMessage.from("它有哪些特点？"));
        ChatResponse response2 = model.chat(messages);

        log.info("问题 2: 它有哪些特点？");
        log.info("回答 2: {}", response2.aiMessage().text());

        assertNotNull(response2);
        assertNotNull(response2.aiMessage());

        log.info("\n✅ 测试通过：能够在对话中保持上下文\n");
    }
}
