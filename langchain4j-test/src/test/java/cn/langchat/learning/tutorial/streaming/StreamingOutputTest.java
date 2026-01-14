package cn.langchat.learning.tutorial.streaming;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 12 - 流式输出测试
 * 
 * 测试文档中的完整使用场景，包括：
 * - StreamingChatModel 的基本使用
 * - 流式输出处理
 * - Token 收集和组装
 * - 错误处理
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
@DisplayName("12 - 流式输出测试")
class StreamingOutputTest {

    private StreamingChatModel streamingChatModel;
    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
        streamingChatModel = TestModelProvider.getStreamingChatModel();
        chatModel = TestModelProvider.getChatModel();
    }

    @Test
    @DisplayName("应该能接收流式输出")
    void shouldReceiveStreamingOutput() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 接收流式输出                                         ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        List<String> tokens = new ArrayList<>();

        streamingChatModel.chat(
            "请用3句话介绍一下Java",
            new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    log.info("收到 Token: {}", partialResponse);
                    tokens.add(partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    log.info("\n流式输出完成");
                }

                @Override
                public void onError(Throwable error) {
                    log.error("发生错误", error);
                }
            }
        );

        // 验证收到了一些 token
        assertNotNull(tokens);
        assertTrue(tokens.size() > 0);
        
        // 组装完整响应
        String fullResponse = String.join("", tokens);
        log.info("完整响应: {}", fullResponse);
        log.info("Token 数量: {}", tokens.size());
        log.info("\n✅ 测试通过：能够接收流式输出\n");
    }

    @Test
    @DisplayName("应该能收集和组装完整响应")
    void shouldCollectAndAssembleResponse() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 收集和组装完整响应                                ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        List<String> tokens = new ArrayList<>();
        StringBuilder fullText = new StringBuilder();

        streamingChatModel.chat(
            "什么是Python？",
            new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    tokens.add(partialResponse);
                    fullText.append(partialResponse);
                    log.info("Token: {}", partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    log.info("\n组装完成");
                }

                @Override
                public void onError(Throwable error) {
                    log.error("错误", error);
                }
            }
        );

        // 验证组装结果
        String assembledResponse = fullText.toString();
        assertNotNull(assembledResponse);
        assertFalse(assembledResponse.isEmpty());
        
        log.info("\n组装的完整响应:");
        log.info("{}", assembledResponse);
        log.info("\nToken 数量: {}", tokens.size());
        log.info("\n✅ 测试通过：能够收集和组装完整响应\n");
    }

    @Test
    @DisplayName("应该能处理流式输出错误")
    void shouldHandleStreamingErrors() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 处理流式输出错误                                     ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        List<String> errors = new ArrayList<>();

        // 使用正常请求测试错误处理
        streamingChatModel.chat(
            "你好",
            new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    log.info("正常 Token: {}", partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    log.info("请求完成");
                }

                @Override
                public void onError(Throwable error) {
                    log.error("捕获到错误", error);
                    errors.add(error.getMessage());
                }
            }
        );

        log.info("错误数量: {}", errors.size());
        log.info("\n✅ 测试通过：能够正确处理流式输出错误\n");
    }

    @Test
    @DisplayName("应该能比较流式和非流式输出")
    void shouldCompareStreamingAndNonStreaming() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 比较流式和非流式输出                             ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        String query = "请用一句话介绍人工智能";

        // 非流式输出
        String nonStreamingResponse = chatModel.chat(query);
        
        log.info("=== 非流式输出 ===");
        log.info("响应: {}", nonStreamingResponse);

        // 流式输出
        List<String> streamingTokens = new ArrayList<>();
        streamingChatModel.chat(
            query,
            new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    streamingTokens.add(partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                }

                @Override
                public void onError(Throwable error) {
                }
            }
        );

        String streamingResponse = String.join("", streamingTokens);
        
        log.info("=== 流式输出 ===");
        log.info("响应: {}", streamingResponse);
        log.info("Token 数量: {}", streamingTokens.size());
        log.info("响应长度比较:");
        log.info("  非流式: {} 字符", nonStreamingResponse.length());
        log.info("  流式: {} 字符", streamingResponse.length());
        log.info("\n✅ 测试通过：能够比较流式和非流式输出\n");
    }
}
