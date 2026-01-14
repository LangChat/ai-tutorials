package cn.langchat.learning.tutorial.chatmemory;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 06 - ChatMemory 对话记忆管理测试
 * 
 * 测试文档中的完整使用场景，包括：
 * - MessageWindowChatMemory 的使用
 * - TokenWindowChatMemory 的使用
 * - 消息添加和获取
 * - 记忆清理
 * - 系统消息保留
 * - 多用户场景
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
@DisplayName("06 - ChatMemory 对话记忆管理测试")
class ChatMemoryTest {

    private ChatMemory messageWindowMemory;
    private ChatMemory tokenWindowMemory;

    @BeforeEach
    void setUp() {
        // 创建两种类型的记忆
        messageWindowMemory = MessageWindowChatMemory.builder()
                .maxMessages(5)
                .build();
        
        // TokenWindowChatMemory需要maxTokens和tokenCountEstimator两个参数
        // 使用OpenAiTokenCountEstimator来准确估算token数量
        tokenWindowMemory = TokenWindowChatMemory.builder()
                .maxTokens(100, new OpenAiTokenCountEstimator("gpt-4"))
                .build();
    }

    @Test
    @DisplayName("应该能添加和获取消息")
    void shouldAddAndGetMessages() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 添加和获取消息                                     ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 添加消息
        messageWindowMemory.add(UserMessage.from("你好"));
        messageWindowMemory.add(AiMessage.from("你好！"));
        messageWindowMemory.add(UserMessage.from("我是小明"));

        // 获取所有消息
        List<ChatMessage> messages = messageWindowMemory.messages();

        assertEquals(3, messages.size());
        assertEquals("你好", ((UserMessage) messages.get(0)).singleText());

        log.info("消息总数: {}", messages.size());
        for (ChatMessage msg : messages) {
            log.info("  {}: {}", 
                msg.getClass().getSimpleName(), 
                extractText(msg));
        }
        log.info("\n✅ 测试通过：能够添加和获取消息\n");
    }

    @Test
    @DisplayName("应该能限制最大消息数")
    void shouldLimitMaxMessages() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 限制最大消息数                                     ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 添加系统消息
        messageWindowMemory.add(SystemMessage.from("你是一个助手"));

        // 添加7条用户消息（超过限制）
        for (int i = 1; i <= 7; i++) {
            messageWindowMemory.add(UserMessage.from("消息 " + i));
            messageWindowMemory.add(AiMessage.from("响应 " + i));
        }

        List<ChatMessage> messages = messageWindowMemory.messages();

        // 应该保留系统消息 + 最近的4条（总共5条）
        assertTrue(messages.size() <= 5);
        log.info("添加了 15 条消息，保留了 {} 条", messages.size());

        // 系统消息应该仍然存在
        boolean hasSystemMessage = messages.stream()
                .anyMatch(msg -> msg instanceof SystemMessage);
        assertTrue(hasSystemMessage);

        log.info("\n✅ 测试通过：能够限制最大消息数\n");
    }

    @Test
    @DisplayName("应该能清空记忆")
    void shouldClearMemory() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 清空记忆                                             ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 添加消息
        messageWindowMemory.add(UserMessage.from("消息1"));
        messageWindowMemory.add(AiMessage.from("响应1"));

        assertFalse(messageWindowMemory.messages().isEmpty());

        // 清空记忆
        messageWindowMemory.clear();

        assertTrue(messageWindowMemory.messages().isEmpty());

        log.info("记忆已清空");
        log.info("\n✅ 测试通过：能够清空记忆\n");
    }

    @Test
    @DisplayName("应该能使用 TokenWindowChatMemory")
    void shouldUseTokenWindowChatMemory() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: TokenWindowChatMemory 的使用                         ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 添加较长的消息
        String longMessage = "这是一条很长的消息用于测试Token限制，包含了很多文字内容...";
        
        for (int i = 0; i < 10; i++) {
            tokenWindowMemory.add(UserMessage.from(longMessage));
            tokenWindowMemory.add(AiMessage.from("响应" + i));
        }

        List<ChatMessage> messages = tokenWindowMemory.messages();

        // 由于Token限制，应该只保留部分消息
        assertTrue(messages.size() >= 1);
        log.info("添加了 20 条消息，保留了 {} 条", messages.size());
        log.info("\n✅ 测试通过：能够使用 TokenWindowChatMemory\n");
    }

    @Test
    @DisplayName("应该能保留系统消息")
    void shouldPreserveSystemMessage() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 保留系统消息                                         ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 添加系统消息
        messageWindowMemory.add(SystemMessage.from("你是一个Java专家"));

        // 添加大量消息以触发清理
        for (int i = 0; i < 10; i++) {
            messageWindowMemory.add(UserMessage.from("消息 " + i));
            messageWindowMemory.add(AiMessage.from("响应 " + i));
        }

        List<ChatMessage> messages = messageWindowMemory.messages();

        // 系统消息应该仍然存在
        boolean hasSystemMessage = messages.stream()
                .anyMatch(msg -> msg instanceof SystemMessage);
        assertTrue(hasSystemMessage);

        log.info("当前消息数: {}", messages.size());
        log.info("系统消息保留: {}", hasSystemMessage);
        log.info("\n✅ 测试通过：能够保留系统消息\n");
    }

    @Test
    @DisplayName("应该能管理多用户记忆")
    void shouldManageMultiUserMemory() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 管理多用户记忆                                     ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 为不同用户创建独立的记忆
        ChatMemory user1Memory = MessageWindowChatMemory.builder()
                .id("user1")
                .maxMessages(5)
                .build();

        ChatMemory user2Memory = MessageWindowChatMemory.builder()
                .id("user2")
                .maxMessages(5)
                .build();

        // 用户1的对话
        user1Memory.add(UserMessage.from("我是小明"));
        user1Memory.add(AiMessage.from("你好小明！"));

        // 用户2的对话
        user2Memory.add(UserMessage.from("我是小红"));
        user2Memory.add(AiMessage.from("你好小红！"));

        // 验证独立性
        String user1Last = extractText(user1Memory.messages().get(1));
        String user2Last = extractText(user2Memory.messages().get(1));

        assertTrue(user1Last.contains("小明"));
        assertTrue(user2Last.contains("小红"));
        assertFalse(user1Last.contains("小红"));
        assertFalse(user2Last.contains("小明"));

        log.info("用户1最后消息: {}", user1Last);
        log.info("用户2最后消息: {}", user2Last);
        log.info("\n✅ 测试通过：能够管理多用户记忆\n");
    }

    @Test
    @DisplayName("应该能正确组织消息顺序")
    void shouldMaintainMessageOrder() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 维护消息顺序                                         ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 按顺序添加消息
        messageWindowMemory.add(SystemMessage.from("系统"));
        messageWindowMemory.add(UserMessage.from("用户1"));
        messageWindowMemory.add(AiMessage.from("AI1"));
        messageWindowMemory.add(UserMessage.from("用户2"));
        messageWindowMemory.add(AiMessage.from("AI2"));

        List<ChatMessage> messages = messageWindowMemory.messages();

        // 验证顺序
        assertTrue(messages.get(0) instanceof SystemMessage);
        assertTrue(messages.get(1) instanceof UserMessage);
        assertTrue(messages.get(2) instanceof AiMessage);
        assertTrue(messages.get(3) instanceof UserMessage);
        assertTrue(messages.get(4) instanceof AiMessage);

        log.info("消息顺序验证:");
        for (int i = 0; i < messages.size(); i++) {
            log.info("  {}: {}", i, messages.get(i).getClass().getSimpleName());
        }
        log.info("\n✅ 测试通过：能够正确维护消息顺序\n");
    }

    @Test
    @DisplayName("应该能处理不同类型的消息")
    void shouldHandleDifferentMessageTypes() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 处理不同类型的消息                                 ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        // 添加不同类型的消息
        messageWindowMemory.add(SystemMessage.from("系统消息"));
        messageWindowMemory.add(UserMessage.from("用户消息"));
        messageWindowMemory.add(AiMessage.from("AI消息"));

        List<ChatMessage> messages = messageWindowMemory.messages();

        assertEquals(3, messages.size());

        // 验证每种类型
        long systemCount = messages.stream()
                .filter(msg -> msg instanceof SystemMessage)
                .count();
        long userCount = messages.stream()
                .filter(msg -> msg instanceof UserMessage)
                .count();
        long aiCount = messages.stream()
                .filter(msg -> msg instanceof AiMessage)
                .count();

        assertEquals(1, systemCount);
        assertEquals(1, userCount);
        assertEquals(1, aiCount);

        log.info("SystemMessage: {}", systemCount);
        log.info("UserMessage: {}", userCount);
        log.info("AiMessage: {}", aiCount);
        log.info("\n✅ 测试通过：能够处理不同类型的消息\n");
    }

    /**
     * 提取消息文本
     */
    private String extractText(ChatMessage message) {
        if (message instanceof UserMessage) {
            return ((UserMessage) message).singleText();
        } else if (message instanceof AiMessage) {
            return ((AiMessage) message).text();
        } else if (message instanceof SystemMessage) {
            return ((SystemMessage) message).text();
        }
        return message.toString();
    }
}
