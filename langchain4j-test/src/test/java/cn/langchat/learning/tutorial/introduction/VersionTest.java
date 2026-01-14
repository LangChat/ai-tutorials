package cn.langchat.learning.tutorial.introduction;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 01 Introduction - 版本和环境信息测试
 * 
 * 不依赖 Spring Boot，直接使用 TestModelProvider
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
@DisplayName("01 Introduction - 版本和环境信息测试")
class VersionTest {

    @Test
    @DisplayName("显示版本和环境信息")
    void shouldDisplayVersionAndEnvironment() {
        // 打印配置信息
        TestModelProvider.printConfig();

        // 获取 ChatModel 验证配置正确
        ChatModel chatModel = TestModelProvider.getChatModel();
        assertNotNull(chatModel, "ChatModel 应该成功创建");

        // 显示环境信息
        log.info("\n╔══════════════════════════════════════════════════════════════════╗");
        log.info("║              运行环境信息                                          ║");
        log.info("╠═════════════════════════════════════════════════════════════════════╣");
        log.info("║ Java 版本: {}", System.getProperty("java.version"));
        log.info("║ Java Home: {}", System.getProperty("java.home"));
        log.info("║ 操作系统: {} {}", System.getProperty("os.name"), System.getProperty("os.version"));
        log.info("║ 用户名称: {}", System.getProperty("user.name"));
        log.info("║ 工作目录: {}", System.getProperty("user.dir"));
        log.info("╚═════════════════════════════════════════════════════════════════════════════════════════════════════════╝\n");
    }
}
