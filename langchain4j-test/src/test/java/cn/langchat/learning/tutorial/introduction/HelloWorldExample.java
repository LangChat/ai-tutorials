package cn.langchat.learning.tutorial.introduction;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;

/**
 * 01 Introduction - Hello World 示例
 * 
 * 简单的 LangChain4j 使用示例，不依赖 Spring Boot
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
public class HelloWorldExample {

    public static void main(String[] args) {
        log.info("╔═════════════════════════════════════════════════════════════════════════════════════════════════════════╗");
        log.info("║          LangChain4j 学习路径 - 示例代码                                ║");
        log.info("╠══════════════════════════════════════════════════════════════════════════════════════════════════════════╣");
        log.info("║                                                               ║");
        log.info("║ 01 - Hello World 示例                                      ║");
        log.info("║                                                               ║");
        log.info("║ 本示例演示 LangChain4j 的基础使用：                       ║");
        log.info("║ 1. 获取 ChatModel                                         ║");
        log.info("║ 2. 发送消息给 LLM                                           ║");
        log.info("║ 3. 接收并显示响应                                           ║");
        log.info("║                                                               ║");
        log.info("╚═════════════════════════════════════════════════════════════════════════════════════════════════════════╝\n");

        // 1. 打印配置信息
        TestModelProvider.printConfig();

        // 2. 获取 ChatModel
        ChatModel chatModel = TestModelProvider.getChatModel();

        // 3. 发送消息给 LLM
        String message = "Hello, LangChain4j!";
        String response = chatModel.chat(message);

        // 4. 打印响应
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║              LangChain4j Hello World 示例                    ║");
        log.info("╠═════════════════════════════════════════════════════════════════════════╣");
        log.info("║ 用户: {}", message);
        log.info("╠═════════════════════════════════════════════════════════════════════════════════════════════════╣");
        log.info("║ AI:");
        log.info("║ {}", response);
        log.info("╚═════════════════════════════════════════════════════════════════════════════════════════════════════╝");
    }
}
