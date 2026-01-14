---
title: '图像模型'
description: '学习 LangChain4j 的 图像模型 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 17 - 图像模型

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 理解 LangChain4j 中图像模型的概念
- 掌握 `ImageModel` 的使用方法
- 学会处理图像输入和输出
- 理解 DALL-E、Midjourney 等多模态模型
- 掌握图像生成和编辑的操作
- 实现一个完整的图像处理应用

## 前置知识

- 完成《01 - LangChain4j 简介》章节
- 完成《02 - 你的第一个 Chat 应用》章节
- Java 基础知识

## 核心概念

### 什么是图像模型？

**图像模型**（Image Models）是能够理解和生成图像的多模态 AI 模型。

**类比理解：**
- **文本模型** = 只能"读"和"写"文字的作家
- **图像模型** = 既能"读"又能"画"的艺术家

**图像模型的类型：**

| 类型 | 说明 | 示例 |
|------|------|------|
| **文生图** | 根据文本描述生成图像 | "一只猫坐在窗台上" |
| **图生图** | 根据参考图像生成相似图像 | "生成与这张猫照片风格相似的艺术图" |
| **图像编辑** | 修改现有图像的某些部分 | "将这张照片中天空的颜色改为蓝色" |
| **图像描述** | 为图像生成文字描述 | "这张照片显示了一位在公园里跑步的女士" |

### 为什么需要图像模型？

1. **创意辅助** - 帮助设计师快速生成创意图像
2. **内容创作** - 自动生成营销图、插画等
3. **图像理解** - 分析图像内容，提取信息
4. **视觉搜索** - 基于图像内容进行搜索
5. **图像修复** - 提升低质量图像的分辨率和质量

## ImageModel 基础

### 创建图像模型实例

```java
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiImageModel;

/**
 * 图像模型示例
 */
public class ImageModelExample {

    private final ImageModel model;

    public ImageModelExample(String apiKey) {
        this.model = OpenAiImageModel.builder()
                .apiKey(apiKey)
                .modelName("dall-e-3")
                .build();
    }

    /**
     * 文生图
     */
    public String generateImage(String prompt) {
        return model.chat(prompt).content();
    }

    /**
     * 文生图（带详细选项）
     */
    public String generateImageWithDetails(
            String prompt,
            String size,
            String quality,
            int n
    ) {
        return model.chat(
                prompt,
                size,      // 图像尺寸
                quality,    // 图像质量
                n           // 生成数量
        ).content();
    }

    public static void main(String[] args) {
        ImageModelExample example = new ImageModelExample(
            System.getenv("OPENAI_API_KEY")
        );

        // 生成图像
        String imageUrl = example.generateImage("一只在海滩上玩耍的金色寻回犬，阳光明媚");

        System.out.println("生成的图像 URL: " + imageUrl);
    }
}
```

## 图像生成操作

### 基础图像生成

```java
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.image.request.ImageRequest;
import dev.langchain4j.model.image.request.ImageResponse;

/**
 * 图像生成示例
 */
public class ImageGenerationExample {

    private final ImageModel model;

    public ImageGenerationExample(String apiKey) {
        this.model = OpenAiImageModel.builder()
                .apiKey(apiKey)
                .modelName("dall-e-3")
                .build();
    }

    /**
     * 生成单个图像
     */
    public ImageResponse generateSingleImage(String prompt) {
        return model.chat(prompt);
    }

    /**
     * 生成多个图像
     */
    public ImageResponse generateMultipleImages(String prompt, int count) {
        return model.chat(prompt, count);
    }

    /**
     * 生成指定尺寸的图像
     */
    public ImageResponse generateImageWithSize(
            String prompt,
            String size,  // "256x256", "512x512", "1024x1024", "1792x1024"
            String format  // "url", "b64_json"
    ) {
        return model.chat(
                prompt,
                size,
                format
        );
    }

    /**
     * 生成高质量图像
     */
    public ImageResponse generateHighQualityImage(
            String prompt,
            String quality  // "standard", "hd"
    ) {
        return model.chat(prompt, quality);
    }

    /**
     * 获取图像 URL 列表
     */
    public List<String> getImageUrls(ImageResponse response) {
        return response.content()
                .revisions()
                .get(0)
                .b64Json() != null ? 
                    response.content().revisions().stream()
                        .map(r -> r.b64Json() != null ? 
                            r.b64Json().url() : r.url())
                        .toList() :
                    List.of(response.content().revisions().get(0).url());
    }

    public static void main(String[] args) {
        ImageGenerationExample example = new ImageGenerationExample(
            System.getenv("OPENAI_API_KEY")
        );

        // 生成图像
        System.out.println("=== 基础图像生成 ===");
        System.out.println();

        ImageResponse response1 = example.generateSingleImage(
            "一只穿着宇航服的猫在太空中探索"
        );

        System.out.println("生成的图像 URL: " + response1.content().revisions().get(0).url());
        System.out.println();

        // 生成多个图像
        System.out.println("=== 生成多个图像 ===");
        System.out.println();

        ImageResponse response2 = example.generateMultipleImages(
            "未来的城市景观，充满科技感",
            4
        );

        System.out.println("生成的图像数量: " + response2.content().revisions().size());
        for (int i = 0; i < response2.content().revisions().size(); i++) {
            System.out.println("图像 " + (i + 1) + ": " + 
                response2.content().revisions().get(i).url());
        }
    }
}
```

## 图像编辑操作

### 图像变体生成

```java
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiImageModel;

/**
 * 图像编辑示例
 */
public class ImageEditingExample {

    private final ImageModel model;

    public ImageEditingExample(String apiKey) {
        this.model = OpenAiImageModel.builder()
                .apiKey(apiKey)
                .modelName("dall-e-3")
                .build();
    }

    /**
     * 生成图像变体
     */
    public String createVariations(String imageUrl) {
        return model.edit(imageUrl, "创建这个图像的艺术风格变体")
                .n(2)  // 生成 2 个变体
                .size("1024x1024")
                .content();
    }

    /**
     * 生成多个变体
     */
    public String createMultipleVariations(String imageUrl, int count, String style) {
        return model.edit(
                imageUrl,
                "使用" + style + "风格重新生成这张图像",
                count
        ).content();
    }

    /**
     * 修改图像风格
     */
    public String changeStyle(String imageUrl, String newStyle) {
        return model.edit(
                imageUrl,
                "将这张图像改为" + newStyle + "风格"
        ).content();
    }

    /**
     * 生成不同视角的图像
     */
    public String generateDifferentAngles(String imageUrl) {
        return model.edit(
                imageUrl,
                "生成这张图像中物体从不同视角的外观"
        ).content();
    }

    public static void main(String[] args) {
        ImageEditingExample editor = new ImageEditingExample(
            System.getenv("OPENAI_API_KEY")
        );

        // 生成变体
        System.out.println("=== 图像变体生成 ===");
        System.out.println();

        String result = editor.createMultipleVariations(
            "https://example.com/image.jpg",
            3,
            "赛博朋克"
        );

        System.out.println("生成的变体图像: " + result);
        System.out.println();

        // 修改风格
        System.out.println("=== 修改图像风格 ===");
        System.out.println();

        String styleResult = editor.changeStyle(
            "https://example.com/image.jpg",
            "水彩画"
        );

        System.out.println("修改后的图像: " + styleResult);
    }
}
```

## 图像描述和理解

### 图像分析

```java
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 图像理解和描述
 */
public class ImageAnalysisExample {

    private static final Logger logger = LoggerFactory.getLogger(ImageAnalysisExample.class);

    private final ChatModel chatModel;

    public ImageAnalysisExample(String apiKey) {
        this.chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o")
                .build();
    }

    /**
     * 描述图像
     */
    public String describeImage(String imageUrl) {
        String prompt = String.format(
            "请详细描述这张图片的内容、风格、色彩和构图。" +
            "图片 URL: %s",
            imageUrl
        );

        return chatModel.chat(prompt).aiMessage().text();
    }

    /**
     * 生成图像标签
     */
    public String generateImageTags(String imageUrl) {
        String prompt = String.format(
            "分析这张图片并为其生成合适的标签（标签用逗号分隔）。" +
            "标签应该包括：物体、场景、颜色、风格、情绪等。" +
            "图片 URL: %s",
            imageUrl
        );

        return chatModel.chat(prompt).aiMessage().text();
    }

    /**
     * 提取图像中的文本
     */
    public String extractTextFromImage(String imageUrl) {
        String prompt = String.format(
            "请提取这张图片中所有可见的文本内容。" +
            "如果图片中没有文本，请说明'无文本'。" +
            "图片 URL: %s",
            imageUrl
        );

        return chatModel.chat(prompt).aiMessage().text();
    }

    /**
     * 回答关于图像的问题
     */
    public String askAboutImage(String imageUrl, String question) {
        String prompt = String.format(
            "请基于这张图片回答以下问题：\n" +
            "问题: %s\n" +
            "图片 URL: %s",
            question,
            imageUrl
        );

        return chatModel.chat(prompt).aiMessage().text();
    }

    /**
     * 对比多张图片
     */
    public String compareImages(List<String> imageUrls) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("请对比以下图片并说明它们的相似之处和不同之处：\n\n");

        for (int i = 0; i < imageUrls.size(); i++) {
            promptBuilder.append(String.format("图片 %d: %s\n", i + 1, imageUrls.get(i)));
        }

        String response = chatModel.chat(promptBuilder.toString()).aiMessage().text();

        return response;
    }

    /**
     * AI 图像分析服务接口
     */
    @AiService
    public interface ImageAnalysisService {
        String describeImage(@V String imageUrl);
        String generateTags(@V String imageUrl);
        String answerQuestion(String imageUrl, @V String question);
    }

    /**
     * 创建图像分析服务
     */
    public ImageAnalysisService createImageAnalysisService() {
        return AiServices.builder(ImageAnalysisService.class)
                .chatModel(chatModel)
                .build();
    }

    public static void main(String[] args) {
        ImageAnalysisExample analyzer = new ImageAnalysisExample(
            System.getenv("OPENAI_API_KEY")
        );

        // 创建服务
        ImageAnalysisService service = analyzer.createImageAnalysisService();

        // 描述图像
        System.out.println("=== 图像描述 ===");
        System.out.println();

        String description = service.describeImage("https://example.com/image.jpg");
        System.out.println("描述: " + description);
        System.out.println();

        // 生成标签
        System.out.println("=== 图像标签 ===");
        System.out.println();

        String tags = service.generateTags("https://example.com/image.jpg");
        System.out.println("标签: " + tags);
        System.out.println();

        // 回答问题
        System.out.println("=== 图像问答 ===");
        System.out.println();

        String answer = service.answerQuestion(
            "https://example.com/image.jpg",
            "图片中有什么动物？"
        );
        System.out.println("答案: " + answer);
    }
}
```

## 多模态应用

### 图文结合应用

```java
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 图文结合应用
 */
public class MultiModalApplication {

    private static final Logger logger = LoggerFactory.getLogger(MultiModalApplication.class);

    private final ImageModel imageModel;
    private final ChatModel chatModel;

    public MultiModalApplication(String apiKey) {
        this.imageModel = OpenAiImageModel.builder()
                .apiKey(apiKey)
                .modelName("dall-e-3")
                .build();

        this.chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o")
                .build();
    }

    /**
     * 文生图 + 图像描述
     */
    public ImageWithDescription generateImageAndDescribe(String prompt) {
        logger.info("生成图像: {}", prompt);

        // 生成图像
        String imageUrl = imageModel.chat(prompt).content();

        logger.info("图像已生成: {}", imageUrl);

        // 描述生成的图像
        String imageDescription = describeImage(imageUrl);

        return new ImageWithDescription(imageUrl, imageDescription);
    }

    /**
     * 根据产品描述生成产品图
     */
    public String generateProductImage(String productDescription) {
        logger.info("生成产品图: {}", productDescription);

        // 构建生成提示词
        String generationPrompt = String.format(
            "基于以下产品描述，生成一张专业的产品展示图：" +
            "\n产品描述: %s\n" +
            "要求：" +
            "- 使用明亮的背景\n" +
            "- 突出产品特点\n" +
            "- 专业摄影风格\n" +
            "- 高分辨率，细节清晰",
            productDescription
        );

        return imageModel.chat(generationPrompt).content();
    }

    /**
     * 图像风格迁移
     */
    public String transferStyle(String sourceImageUrl, String targetStyle, String content) {
        logger.info("风格迁移: 来源={}, 目标风格={}", sourceImageUrl, targetStyle);

        // 描述目标风格
        String styleDescription = describeStyle(targetStyle);

        // 构建提示词
        String prompt = String.format(
            "请为以下内容生成一张新图像：" +
            "\n内容: %s\n" +
            "使用以下艺术风格：\n%s\n" +
            "参考图片：",
            content,
            styleDescription,
            sourceImageUrl
        );

        return imageModel.chat(prompt).content();
    }

    /**
     * 描述图像
     */
    private String describeImage(String imageUrl) {
        String prompt = String.format(
            "请详细描述这张图片的视觉特点，包括：" +
            "- 主体内容\n" +
            "- 构图方式\n" +
            "- 色彩运用\n" +
            "- 艺术风格\n" +
            "图片 URL: %s",
            imageUrl
        );

        return chatModel.chat(prompt).aiMessage().text();
    }

    /**
     * 描述风格
     */
    private String describeStyle(String styleDescription) {
        String prompt = String.format(
            "请详细描述%s艺术风格的视觉特点，包括：" +
            "- 典型的构图方式\n" +
            "- 色彩使用习惯\n" +
            "- 笔触技法\n" +
            "- 整体美学特征",
            styleDescription
        );

        return chatModel.chat(prompt).aiMessage().text();
    }

    /**
     * 创建系列图像
     */
    public List<String> createImageSeries(String theme, String style, int count) {
        logger.info("创建图像系列: 主题={}, 风格={}, 数量={}", theme, style, count);

        List<String> imageUrls = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String prompt = String.format(
                "创建一张%s风格的%s主题图片，" +
                "这是系列中的第%d张图片。" +
                "保持与系列中其他图片的一致性。",
                style,
                theme,
                i + 1
            );

            String imageUrl = imageModel.chat(prompt).content();
            imageUrls.add(imageUrl);
        logger.info("已生成图像 {}/{}: {}", i + 1, count, imageUrl);
        }

        return imageUrls;
    }

    /**
     * 图像和描述的包装类
     */
    public static class ImageWithDescription {
        private final String imageUrl;
        private final String description;

        public ImageWithDescription(String imageUrl, String description) {
            this.imageUrl = imageUrl;
            this.description = description;
        }

        public String getImageUrl() { return imageUrl; }
        public String getDescription() { return description; }
    }

    /**
     * 图像生成服务接口
     */
    @AiService
    public interface ImageGenerationService {
        String generateImage(@V String prompt);
        String generateImageWithStyle(@V String prompt, String style);
    }

    /**
     * 创建图像生成服务
     */
    public ImageGenerationService createImageGenerationService() {
        return AiServices.builder(ImageGenerationService.class)
                .chatModel(chatModel)
                .systemMessageProvider(chatMemoryId -> 
                    "你是一个专业的图像生成助手，" +
                    "擅长根据用户的需求创建各种风格的图像。"
                )
                .build();
    }

    public static void main(String[] args) {
        MultiModalApplication app = new MultiModalApplication(
            System.getenv("OPENAI_API_KEY")
        );

        // 创建服务
        ImageGenerationService imageService = app.createImageGenerationService();

        // 文生图 + 描述
        System.out.println("=== 文生图 + 图像描述 ===");
        System.out.println();

        ImageWithDescription result = app.generateImageAndDescribe(
            "一个充满未来感的赛博朋克城市，霓虹灯闪烁，飞行汽车穿梭其中"
        );

        System.out.println("图像 URL: " + result.getImageUrl());
        System.out.println("图像描述: " + result.getDescription());
        System.out.println();

        // 产品图生成
        System.out.println("=== 产品图生成 ===");
        System.out.println();

        String productImage = app.generateProductImage(
            "一款智能手表，具有心率监测、GPS定位、防水等特性，" +
            "设计简约时尚，适合运动和日常佩戴"
        );

        System.out.println("产品图 URL: " + productImage);
        System.out.println();

        // 创建图像系列
        System.out.println("=== 创建图像系列 ===");
        System.out.println();

        List<String> seriesImages = app.createImageSeries(
            "春天花园",
            "水彩画印象派",
            4
        );

        System.out.println("生成的系列图像:");
        for (int i = 0; i < seriesImages.size(); i++) {
            System.out.println("  图像 " + (i + 1) + ": " + seriesImages.get(i));
        }
    }
}
```

## 测试代码示例

```java
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.image.request.ImageRequest;
import dev.langchain4j.model.image.request.ImageResponse;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 图像模型测试
 */
class ImageModelTest {

    private ImageModel model;

    @BeforeEach
    void setUp() {
        this.model = OpenAiImageModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("dall-e-3")
                .build();
    }

    @Test
    void should_generate_image() {
        // 生成图像
        ImageResponse response = model.chat("一只简单的猫");

        // 验证
        assertNotNull(response);
        assertNotNull(response.content());
        assertNotNull(response.content().revisions());
        assertTrue(response.content().revisions().size() > 0);

        System.out.println("生成的图像 URL: " + response.content().revisions().get(0).url());
    }

    @Test
    void should_generate_image_with_options() {
        // 带选项生成
        ImageResponse response = model.chat(
                "一个美丽的风景画",
                "1024x1024",    // 尺寸
                "hd",            // 高质量
                2                // 数量
        );

        // 验证
        assertNotNull(response);
        assertEquals(2, response.content().revisions().size());

        System.out.println("生成了 " + response.content().revisions().size() + " 个图像");
    }

    @Test
    void should_edit_image() {
        // 编辑图像（需要实际的图片 URL）
        String imageUrl = "https://example.com/test-image.jpg";
        
        // 注意：这个测试需要有效的图片 URL 和正确的 API 密钥
        // 这里只是演示代码结构
        
        // ImageResponse response = model.edit(imageUrl, "创建变体");
        
        // 验证
        // assertNotNull(response);
        // assertNotNull(response.content());
    }

    @Test
    void should_handle_errors() {
        // 测试错误处理
        
        // 模拟无效的 API 密钥
        ImageModel invalidModel = OpenAiImageModel.builder()
                .apiKey("invalid-key")
                .modelName("dall-e-3")
                .build();

        // 尝试生成（应该抛出异常）
        try {
            invalidModel.chat("测试图像");
            fail("应该抛出异常");
        } catch (Exception e) {
            // 预期失败
            assertNotNull(e);
        }
    }
}
```

## 实践练习

### 练习 1：实现图像生成器

```java
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiImageModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;

/**
 * 智能图像生成器
 */
public class SmartImageGenerator {

    private static final Logger logger = LoggerFactory.getLogger(SmartImageGenerator.class);

    private final ImageModel model;

    public SmartImageGenerator(String apiKey) {
        this.model = OpenAiImageModel.builder()
                .apiKey(apiKey)
                .modelName("dall-e-3")
                .build();
    }

    /**
     * 生成带有详细提示的图像
     */
    public String generateWithDetailedPrompt(
            String subject,
            String style,
            String mood,
            String additionalDetails
    ) {
        // 构建详细提示词
        String detailedPrompt = buildDetailedPrompt(
            subject,
            style,
            mood,
            additionalDetails
        );

        logger.info("生成图像: {}", detailedPrompt);

        return model.chat(detailedPrompt).content();
    }

    /**
     * 构建详细提示词
     */
    private String buildDetailedPrompt(
            String subject,
            String style,
            String mood,
            String additionalDetails
    ) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("创建一张");

        if (mood != null && !mood.isEmpty()) {
            prompt.append(mood).append("风格的");
        }

        if (style != null && !style.isEmpty()) {
            prompt.append(style).append("风格的");
        }

        prompt.append(subject);

        if (additionalDetails != null && !additionalDetails.isEmpty()) {
            prompt.append("，").append(additionalDetails);
        }

        prompt.append("的图像。");

        prompt.append("\n要求：");
        prompt.append("- 高质量，细节丰富\n");
        prompt.append("- 色彩鲜明，光影优美\n");
        prompt.append("- 专业摄影风格\n");
        prompt.append("- 构图平衡，主体突出");

        return prompt.toString();
    }

    /**
     * 生成多个变体
     */
    public List<String> generateVariations(
            String prompt,
            String baseStyle,
            int count,
            String[] styleVariations
    ) {
        List<String> imageUrls = new ArrayList<>();

        for (String styleVariant : styleVariations) {
            String fullPrompt = String.format("%s %s", baseStyle, styleVariant);
            String detailedPrompt = buildDetailedPrompt(prompt, fullStyle, null, null);

            logger.info("生成变体: {}", fullPrompt);

            String imageUrl = model.chat(detailedPrompt).content();
            imageUrls.add(imageUrl);

            if (imageUrls.size() >= count) {
                break;
            }
        }

        return imageUrls;
    }

    /**
     * 生成图像系列（主题变体）
     */
    public List<String> generateSeries(
            String baseSubject,
            String[] variations,
            String style
    ) {
        List<String> imageUrls = new ArrayList<>();

        for (String variation : variations) {
            String prompt = baseSubject + "的" + variation;
            String detailedPrompt = buildDetailedPrompt(prompt, style, null, null);

            logger.info("生成系列: {}", prompt);

            String imageUrl = model.chat(detailedPrompt).content();
            imageUrls.add(imageUrl);
        }

        return imageUrls;
    }

    /**
     * 从文本生成图像（提示词优化）
     */
    public String generateFromTextOptimized(String text) {
        // 提取关键信息
        String keywords = extractKeywords(text);
        String subject = extractSubject(text);

        // 构建优化的提示词
        String optimizedPrompt = String.format(
            "根据以下文本内容生成一张相关的图像：\n" +
            "原文: %s\n" +
            "提取的主题: %s\n" +
            "关键词: %s\n\n" +
            "生成要求：" +
            "- 图像应该准确反映原文的主题\n" +
            "- 使用关键词中的元素\n" +
            "- 视觉风格要与内容匹配\n" +
            "- 保持简洁和专业",
            text,
            subject,
            keywords
        );

        logger.info("优化提示词: {}", optimizedPrompt);

        return model.chat(optimizedPrompt).content();
    }

    /**
     * 提取关键词
     */
    private String extractKeywords(String text) {
        // 简化实现：提取名词和形容词
        String[] words = text.split("[\\s,。,！，？，；，：，""，（））]");
        
        // 这里应该使用更复杂的 NLP 技术
        return String.join(", ", words);
    }

    /**
     * 提取主题
     */
    private String extractSubject(String text) {
        // 简化实现：取第一个句子
        String[] sentences = text.split("[.。！？]");
        
        return sentences.length > 0 ? sentences[0] : text.substring(0, Math.min(20, text.length()));
    }

    public static void main(String[] args) {
        SmartImageGenerator generator = new SmartImageGenerator(
            System.getenv("OPENAI_API_KEY")
        );

        // 详细生成
        System.out.println("=== 详细图像生成 ===");
        System.out.println();

        String result1 = generator.generateWithDetailedPrompt(
            "一个现代化的咖啡店",
            "极简主义",
            "温馨",
            "大窗户，充足的自然光，木质家具，舒适的氛围"
        );

        System.out.println("生成的图像: " + result1);
        System.out.println();

        // 生成变体
        System.out.println("=== 生成变体 ===");
        System.out.println();

        String[] styleVariants = {
            "明亮色彩",
            "柔和色调",
            "黑白艺术"
        };

        List<String> variations = generator.generateVariations(
            "一只可爱的猫",
            "摄影",
            3,
            styleVariants
        );

        System.out.println("生成的变体:");
        for (int i = 0; i < variations.size(); i++) {
            System.out.println("  变体 " + (i + 1) + ": " + variations.get(i));
        }
    }
}
```

### 练习 2：实现视觉故事板

```java
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiImageModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;

/**
 * 视觉故事板
 */
public class VisualStoryboard {

    private static final Logger logger = LoggerFactory.getLogger(VisualStoryboard.class);

    private final ImageModel model;

    public VisualStoryboard(String apiKey) {
        this.model = OpenAiImageModel.builder()
                .apiKey(apiKey)
                .modelName("dall-e-3")
                .build();
    }

    /**
     * 故事板场景
     */
    public static class StoryboardScene {
        private final String description;
        private final String style;
        private final String mood;
        private final String composition;

        public StoryboardScene(String description, String style, String mood, String composition) {
            this.description = description;
            this.style = style;
            this.mood = mood;
            this.composition = composition;
        }

        public String getDescription() { return description; }
        public String getStyle() { return style; }
        public String getMood() { return mood; }
        public String getComposition() { return composition; }
    }

    /**
     * 故事板脚本
     */
    public static class StoryboardScript {
        private final String title;
        private final String genre;
        private final List<StoryboardScene> scenes;

        public StoryboardScript(String title, String genre, List<StoryboardScene> scenes) {
            this.title = title;
            this.genre = genre;
            this.scenes = scenes;
        }

        public String getTitle() { return title; }
        public String getGenre() { return genre; }
        public List<StoryboardScene> getScenes() { return scenes; }
    }

    /**
     * 创建故事板脚本
     */
    public StoryboardScript createScript(String storyConcept, String genre, List<StoryboardScene> scenes) {
        return new StoryboardScript(storyConcept, genre, scenes);
    }

    /**
     * 可视化故事板脚本
     */
    public void visualizeScript(StoryboardScript script) {
        System.out.println("╔═════════════════════════════════════════════════════════╗");
        System.out.println("║                    视觉故事板                               ║");
        System.out.println("╠════════════════════════════════════════════════════════╣");
        System.out.println("║ 标题: " + script.getTitle());
        System.out.println("║ 类型: " + script.getGenre());
        System.out.println("╠════════════════════════════════════════════════════════╣");
        System.out.println("║ 场景列表:                                               ║");
        System.out.println("╠════════════════════════════════════════════════════════╣");

        for (int i = 0; i < script.getScenes().size(); i++) {
            StoryboardScene scene = script.getScenes().get(i);

            System.out.printf("║ [%d] %s                                               ║\n",
                    i + 1,
                    scene.getDescription().substring(0, Math.min(50, scene.getDescription().length())));

            System.out.println("║     风格: " + scene.getStyle());
            System.out.println("║     情绪: " + scene.getMood());
            System.out.println("║     构图: " + scene.getComposition());
            System.out.println("╠─────────────────────────────────────────────────────────────╣");
        }

        System.out.println("╚═════════════════════════════════════════════════════════════╝");
    }

    /**
     * 生成故事板图像
     */
    public List<String> generateStoryboardImages(StoryboardScript script) {
        logger.info("生成故事板图像: {}", script.getTitle());

        List<String> imageUrls = new ArrayList<>();

        for (int i = 0; i < script.getScenes().size(); i++) {
            StoryboardScene scene = script.getScenes().get(i);

            // 构建场景提示词
            String scenePrompt = buildScenePrompt(scene, script.getGenre());

            logger.info("生成场景 {}: {}", i + 1, scene.getDescription());

            // 生成图像
            String imageUrl = model.chat(scenePrompt).content();
            imageUrls.add(imageUrl);
        }

        return imageUrls;
    }

    /**
     * 构建场景提示词
     */
    private String buildScenePrompt(StoryboardScene scene, String genre) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("创建一个");

        if (scene.getMood() != null) {
            prompt.append(scene.getMood()).append("情绪的");
        }

        prompt.append(genre).append("风格的");

        prompt.append("场景：").append(scene.getDescription());

        prompt.append("\n具体要求：");
        prompt.append("- 风格：").append(scene.getStyle()).append("\n");
        prompt.append("- 构图：").append(scene.getComposition()).append("\n");
        prompt.append("- 光影效果符合情绪\n");
        prompt.append("- 细节丰富，适合故事板");

        return prompt.toString();
    }

    /**
     * 创建示例故事板
     */
    public StoryboardScript createExampleScript() {
        // 定义场景
        List<StoryboardScene> scenes = new ArrayList<>();

        scenes.add(new StoryboardScene(
            "一个阳光明媚的早晨，女主角在小村庄的街道上行走，远处的山峦如画",
            "吉卜力动画风格",
            "宁静祥和",
            "广角镜头，三分法构图"
        ));

        scenes.add(new StoryboardScene(
            "女主角在古老的图书馆中发现一本神奇的书，书页发光",
            "奇幻冒险风格",
            "神秘震撼",
            "中景镜头，侧光照明"
        ));

        scenes.add(new StoryboardScene(
            "书中的魔法将女主角传送到一个魔法森林，周围是发光的植物",
            "奇幻风格",
            "奇幻梦幻",
            "大光圈效果，梦幻氛围"
        ));

        scenes.add(new StoryboardScene(
            "女主角在森林中遇到一个小精灵，精灵给她指引",
            "奇幻风格",
            "友好温暖",
            "平视镜头，主次分明"
        ));

        return createScript("魔法旅程", "奇幻", scenes);
    }

    public static void main(String[] args) {
        VisualStoryboard storyboard = new VisualStoryboard(
            System.getenv("OPENAI_API_KEY")
        );

        // 创建示例脚本
        StoryboardScript script = storyboard.createExampleScript();

        // 可视化脚本
        storyboard.visualizeScript(script);
        System.out.println();

        // 生成故事板图像
        System.out.println("=== 生成故事板图像 ===");
        System.out.println();

        // 注意：实际生成需要时间，这里只是演示
        // List<String> imageUrls = storyboard.generateStoryboardImages(script);
        
        System.out.println("故事板图像生成完成（实际应用中会生成 " + script.getScenes().size() + " 张图像）");
    }
}
```

## 总结

### 本章要点

1. **图像模型概念**
   - 文生图、图生图、图像编辑
   - 多模态 AI 模型
   - 图像理解和描述

2. **ImageModel 使用**
   - 创建图像模型实例
   - 配置参数和选项
   - 处理响应和结果

3. **图像操作**
   - 基础图像生成
   - 图像编辑和变体
   - 多种风格和尺寸
   - 批量生成

4. **多模态应用**
   - 图文结合
   - 图像描述和理解
   - 视觉故事板
   - 产品图生成

5. **最佳实践**
   - 合理构建提示词
   - 选择合适的风格和参数
   - 控制生成成本
   - 管理生成的图像

### 下一步

在下一章节中，我们将学习：
- 自定义 HTTP 客户端
- 异步操作和并发处理
- 错误处理和重试机制
- 性能优化技巧
- 生产环境部署建议

### 常见问题

**Q1：LangChain4j 支持哪些图像模型？**

A：LangChain4j 支持的图像模型包括：
- OpenAI DALL-E 系列（DALL-E 2, DALL-E 3）
- Midjourney（通过自定义 API）
- Stability AI
- 其他支持 OpenAI API 的图像生成服务

**Q2：如何控制生成图像的质量？**

A：控制方法：
1. 使用 `quality` 参数（"standard", "hd"）
2. 选择合适的模型（如 DALL-E 3 比 DALL-E 2 质量更高）
3. 在提示词中明确质量要求
4. 使用适当的 `size` 参数

**Q3：如何减少图像生成成本？**

A：优化策略：
1. 优化提示词，减少迭代
2. 合理选择质量参数（标准质量即可时不用高清）
3. 控制生成数量
4. 使用更经济的模型（如 DALL-E 2）
5. 缓存常用的图像

**Q4：如何处理图像生成的失败？**

A：失败处理策略：
1. 捕获并记录异常
2. 实现重试机制
3. 提供回退方案（使用更简单的模型）
4. 记录失败的提示词用于调试
5. 提供用户友好的错误消息

**Q5：图像生成 API 有哪些限制？**

A：常见限制：
1. 速率限制（每分钟请求数）
2. 并发限制（同时处理请求数）
3. 配额限制（每月生成的图像数）
4. 内容策略（不允许生成有害内容）
5. 生成时间限制（某些 API 有超时）

## 参考资料

- [LangChain4j 图像模型文档](https://docs.langchain4j.dev/tutorials/image-models)
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
