package cn.langchat.learning.tutorial.firstchat;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;

import java.util.Scanner;

/**
 * 简单的聊天应用示例
 * 
 * 对应文档02的第一个完整示例，展示如何构建一个可运行的聊天应用
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
public class SimpleChatApp {

    private final ChatModel model;
    private final Scanner scanner;

    public SimpleChatApp() {
        this.model = TestModelProvider.getChatModel();
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        // 打印配置信息
        TestModelProvider.printConfig();

        SimpleChatApp app = new SimpleChatApp();
        app.start();
    }

    public void start() {
        log.info("╔══════════════════════════════════════╗");
        log.info("║   LangChain4j 简单聊天应用 v1.0    ║");
        log.info("╚══════════════════════════════════════╝");
        log.info("");
        log.info("使用提示:");
        log.info("  - 直接输入消息与 AI 对话");
        log.info("  - 输入 'clear' 清屏");
        log.info("  - 输入 'exit' 退出");
        log.info("");

        int messageCount = 0;

        while (true) {
            log.info("[{}] 你: ", ++messageCount);
            String input = scanner.nextLine().trim();

            // 处理命令
            if ("exit".equalsIgnoreCase(input)) {
                log.info("\n再见！感谢使用。");
                break;
            } else if ("clear".equalsIgnoreCase(input)) {
                for (int i = 0; i < 50; i++) {
                    log.info("");
                }
                continue;
            } else if (input.isEmpty()) {
                continue;
            }

            // 发送消息
            try {
                long startTime = System.currentTimeMillis();
                String response = model.chat(input);
                long endTime = System.currentTimeMillis();

                log.info("[{}] AI: {}", messageCount, response);
                log.info("        (耗时: {}ms)", (endTime - startTime));
                log.info("");
            } catch (Exception e) {
                log.error("错误: {}", e.getMessage());
                log.info("");
            }
        }

        scanner.close();
    }
}
