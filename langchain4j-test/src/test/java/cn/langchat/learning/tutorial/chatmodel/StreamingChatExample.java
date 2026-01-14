package cn.langchat.learning.tutorial.chatmodel;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 流式响应示例
 * 
 * 展示如何使用 StreamingChatModel 进行流式输出
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
public class StreamingChatExample {

    public static void main(String[] args) {
        TestModelProvider.printConfig();
        
        StreamingChatModel model = TestModelProvider.getStreamingChatModel();
        
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║          LangChain4j 流式响应示例                              ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        List<dev.langchain4j.data.message.ChatMessage> messages = 
                List.of(UserMessage.from("用一句话介绍 LangChain4j"));

        log.info("问题: {}", messages.get(0).toString());
        log.info("╠═══════════════════════════════════════════════════════════════════════════════════════════════════╣");
        log.info("流式响应:");
        log.info("╠═══════════════════════════════════════════════════════════════════════════════════════════════════╣");

        CompletableFuture<ChatResponse> future = new CompletableFuture<>();
        StringBuilder fullResponse = new StringBuilder();

        StreamingChatResponseHandler handler = new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                System.out.print(partialResponse);
                fullResponse.append(partialResponse);
            }
            
            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                System.out.println();
                log.info("\n生成完成！");
                log.info("总Token: {}", completeResponse.metadata().tokenUsage().totalTokenCount());
                future.complete(completeResponse);
            }
            
            @Override
            public void onError(Throwable error) {
                System.err.println("\n错误: " + error.getMessage());
                future.completeExceptionally(error);
            }
        };

        model.chat(messages, handler);

        try {
            future.join();
        } catch (Exception e) {
            log.error("流式响应失败: {}", e.getMessage());
        }
    }
}
