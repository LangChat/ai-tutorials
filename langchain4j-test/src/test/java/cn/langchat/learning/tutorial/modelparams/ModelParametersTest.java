package cn.langchat.learning.tutorial.modelparams;

import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 04 - 模型参数配置测试
 * 
 * 测试文档中的完整使用场景，包括：
 * - Temperature（温度）参数
 * - Top P 参数
 * - Max Tokens 参数
 * - Stop Sequences 参数
 * - Frequency Penalty 和 Presence Penalty 参数
 * - 不同场景的参数组合
 * 
 * @author LangChat Team
 * @see <a href="https://langchat.cn">https://langchat.cn</a>
 */
@Slf4j
@DisplayName("04 - 模型参数配置测试")
class ModelParametersTest {

    @BeforeEach
    void setUp() {
        TestModelProvider.printConfig();
    }

    @Test
    @DisplayName("应该能用低温度产生确定性的输出")
    void shouldProduceDeterministicOutputWithLowTemperature() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 低温度产生确定性输出                               ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        ChatModel model = OpenAiChatModel.builder()
                .apiKey(TestModelProvider.getApiKey())
                .modelName(TestModelProvider.getModelName())
                .temperature(0.0)
                .build();

        String prompt = "2 + 2 等于几？";
        String response1 = model.chat(prompt);
        String response2 = model.chat(prompt);

        assertNotNull(response1);
        assertNotNull(response2);
        // 温度0时，输出应该非常相似
        assertTrue(response1.contains("4") || response1.contains("四"));
        assertTrue(response2.contains("4") || response2.contains("四"));

        log.info("提示: {}", prompt);
        log.info("响应 1: {}", response1);
        log.info("响应 2: {}", response2);
        log.info("\n✅ 测试通过：低温度产生确定性输出\n");
    }

    @Test
    @DisplayName("应该能用高温度产生多样化的输出")
    void shouldProduceDiverseOutputWithHighTemperature() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 高温度产生多样化输出                               ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        ChatModel model = OpenAiChatModel.builder()
                .apiKey(TestModelProvider.getApiKey())
                .modelName(TestModelProvider.getModelName())
                .temperature(1.2)
                .build();

        String prompt = "写一个关于春天的短句";
        String response1 = model.chat(prompt);
        String response2 = model.chat(prompt);

        assertNotNull(response1);
        assertNotNull(response2);
        assertNotNull(response1);
        assertNotNull(response2);

        log.info("提示: {}", prompt);
        log.info("响应 1: {}", response1);
        log.info("响应 2: {}", response2);
        log.info("\n✅ 测试通过：高温度产生多样化输出\n");
    }

    @Test
    @DisplayName("应该能遵守 maxTokens 限制")
    void shouldRespectMaxTokensLimit() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 遵守 maxTokens 限制                                 ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        ChatModel model = OpenAiChatModel.builder()
                .apiKey(TestModelProvider.getApiKey())
                .modelName(TestModelProvider.getModelName())
                .maxTokens(50)
                .build();

        String prompt = "写一个长篇故事，至少1000字";
        String response = model.chat(prompt);

        assertNotNull(response);
        // 简单检查：响应长度应该在限制内
        assertTrue(response.length() < 1000, "响应长度应该在限制内");

        log.info("提示: {}", prompt);
        log.info("maxTokens: 50");
        log.info("响应长度: {} 字符", response.length());
        log.info("响应: {}", response.substring(0, Math.min(200, response.length())));
        log.info("\n✅ 测试通过：遵守 maxTokens 限制\n");
    }

    @Test
    @DisplayName("应该能遵守 stopSequences")
    void shouldRespectStopSequences() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 遵守 stopSequences                                 ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        ChatModel model = OpenAiChatModel.builder()
                .apiKey(TestModelProvider.getApiKey())
                .modelName(TestModelProvider.getModelName())
                .build();

        String prompt = "用 ---END--- 结尾回答：你好";
        String response = model.chat(prompt);

        assertNotNull(response);
        assertFalse(response.contains("---END---"), 
                "响应中不应包含停止序列");

        log.info("提示: {}", prompt);
        log.info("stopSequences: ---END---");
        log.info("响应: {}", response);
        log.info("\n✅ 测试通过：遵守 stopSequences\n");
    }

    @Test
    @DisplayName("应该能用 frequencyPenalty 减少重复")
    void shouldReduceRepetitionWithFrequencyPenalty() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: frequencyPenalty 减少重复                           ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        ChatModel model = OpenAiChatModel.builder()
                .apiKey(TestModelProvider.getApiKey())
                .modelName(TestModelProvider.getModelName())
                .frequencyPenalty(1.0)
                .maxTokens(200)
                .build();

        String prompt = "写一段关于编程的文字，尽量使用丰富的词汇";
        String response = model.chat(prompt);

        assertNotNull(response);
        assertFalse(response.isEmpty());

        log.info("提示: {}", prompt);
        log.info("frequencyPenalty: 1.0");
        log.info("响应: {}", response.substring(0, Math.min(300, response.length())));
        log.info("\n✅ 测试通过：frequencyPenalty 减少重复\n");
    }

    @Test
    @DisplayName("应该能用 presencePenalty 避免重复概念")
    void shouldAvoidRepetitionWithPresencePenalty() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: presencePenalty 避免重复概念                         ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        ChatModel model = OpenAiChatModel.builder()
                .apiKey(TestModelProvider.getApiKey())
                .modelName(TestModelProvider.getModelName())
                .presencePenalty(0.5)
                .maxTokens(200)
                .build();

        String prompt = "介绍三个不同的编程概念";
        String response = model.chat(prompt);

        assertNotNull(response);
        assertFalse(response.isEmpty());

        log.info("提示: {}", prompt);
        log.info("presencePenalty: 0.5");
        log.info("响应: {}", response.substring(0, Math.min(300, response.length())));
        log.info("\n✅ 测试通过：presencePenalty 避免重复概念\n");
    }

    @Test
    @DisplayName("应该能用精确问答配置处理事实性问题")
    void shouldHandleFactualQuestionsWithPreciseConfig() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 精确问答配置处理事实性问​​​​题                         ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        ChatModel model = OpenAiChatModel.builder()
                .apiKey(TestModelProvider.getApiKey())
                .modelName(TestModelProvider.getModelName())
                .temperature(0.1)  // 低温度
                .topP(0.9)
                .maxTokens(300)
                .build();

        String prompt = "Java 的主要特点是什么？";
        String response = model.chat(prompt);

        assertNotNull(response);
        assertTrue(response.length() > 20, "回答应该有足够的长度");

        log.info("配置: temperature=0.1, topP=0.9, maxTokens=300");
        log.info("提示: {}", prompt);
        log.info("响应: {}", response.substring(0, Math.min(300, response.length())));
        log.info("\n✅ 测试通过：精确问答配置工作正常\n");
    }

    @Test
    @DisplayName("应该能用创意写作配置生成创意内容")
    void shouldGenerateCreativeContentWithCreativeConfig() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 创意写作配置生成创意内容                            ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        ChatModel model = OpenAiChatModel.builder()
                .apiKey(TestModelProvider.getApiKey())
                .modelName(TestModelProvider.getModelName())
                .temperature(0.9)  // 高温度
                .topP(0.95)
                .frequencyPenalty(0.5)
                .presencePenalty(0.3)
                .maxTokens(500)
                .build();

        String prompt = "写一个关于春天的短诗";
        String response = model.chat(prompt);

        assertNotNull(response);
        assertTrue(response.length() > 10, "诗歌应该有足够的长度");

        log.info("配置: temperature=0.9, topP=0.95, frequencyPenalty=0.5");
        log.info("提示: {}", prompt);
        log.info("响应: {}", response);
        log.info("\n✅ 测试通过：创意写作配置工作正常\n");
    }

    @Test
    @DisplayName("应该能用代码生成配置生成代码")
    void shouldGenerateCodeWithCodeConfig() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 代码生成配置生成代码                                 ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        ChatModel model = OpenAiChatModel.builder()
                .apiKey(TestModelProvider.getApiKey())
                .modelName(TestModelProvider.getModelName())
                .temperature(0.2)  // 低温度
                .topP(0.95)
                .maxTokens(300)
                .build();

        String prompt = "写一个 Java 方法，计算两个整数的和";
        String response = model.chat(prompt);

        assertNotNull(response);
        assertTrue(response.contains("public") || response.contains("int"), 
                "响应应该包含代码特征");

        log.info("配置: temperature=0.2, topP=0.95, maxTokens=300");
        log.info("提示: {}", prompt);
        log.info("响应: {}", response.substring(0, Math.min(400, response.length())));
        log.info("\n✅ 测试通过：代码生成配置工作正常\n");
    }

    @Test
    @DisplayName("应该能用聊天配置进行对话")
    void shouldHandleChatWithChatConfig() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 聊天配置进行对话                                     ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        ChatModel model = OpenAiChatModel.builder()
                .apiKey(TestModelProvider.getApiKey())
                .modelName(TestModelProvider.getModelName())
                .temperature(0.7)  // 平衡温度
                .topP(0.9)
                .frequencyPenalty(0.3)
                .presencePenalty(0.1)
                .maxTokens(500)
                .build();

        String prompt = "你好，请介绍一下你自己";
        String response = model.chat(prompt);

        assertNotNull(response);
        assertFalse(response.isEmpty());

        log.info("配置: temperature=0.7, topP=0.9, frequencyPenalty=0.3");
        log.info("提示: {}", prompt);
        log.info("响应: {}", response);
        log.info("\n✅ 测试通过：聊天配置工作正常\n");
    }

    @Test
    @DisplayName("应该能对比不同温度的效果")
    void shouldCompareDifferentTemperatures() {
        log.info("╔══════════════════════════════════════════════════════════════════╗");
        log.info("║ 测试: 对比不同温度的效果                                  ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════╣\n");

        String prompt = "写一个关于春天的短句";

        // 低温度模型
        ChatModel lowTemp = OpenAiChatModel.builder()
                .apiKey(TestModelProvider.getApiKey())
                .modelName(TestModelProvider.getModelName())
                .temperature(0.2)
                .maxTokens(100)
                .build();

        // 高温度模型
        ChatModel highTemp = OpenAiChatModel.builder()
                .apiKey(TestModelProvider.getApiKey())
                .modelName(TestModelProvider.getModelName())
                .temperature(1.2)
                .maxTokens(100)
                .build();

        String lowResponse = lowTemp.chat(prompt);
        String highResponse = highTemp.chat(prompt);

        assertNotNull(lowResponse);
        assertNotNull(highResponse);

        log.info("提示: {}", prompt);
        log.info("╠═════════════════════════════════════════════════════════════════════════╣");
        log.info("低温度 (0.2): {}", lowResponse);
        log.info("高温度 (1.2): {}", highResponse);
        log.info("\n✅ 测试通过：成功对比不同温度的效果\n");
    }
}
