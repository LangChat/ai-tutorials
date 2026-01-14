---
title: '项目介绍'
description: '项目介绍'
---

# LangChain4j 学习路径测试代码库

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

## 项目介绍

这是一个独立的 Java 项目，用于存放 LangChain4j 学习路径中所有文档涉及的测试代码和示例，让开发人员能够快速运行和测试 LangChain4j 的各种功能。

### 项目定位

- **可运行性** - 所有代码都是完整的 Maven 项目，可以直接运行
- **测试覆盖** - 每个学习文档都有对应的测试代码
- **配置统一** - 使用 .env 文件统一管理配置
- **快速上手** - 开发人员可以快速克隆并运行测试
- **模块化设计** - 每个学习主题对应独立的测试模块

## 快速开始

### 前置要求

- JDK 17+
- Maven 3.9+

### ⚠️ 配置 API Key

在运行测试之前，**必须**先配置 API Key。本项目使用 .env 文件管理配置。

#### 步骤 1：复制配置文件

```bash
# 从模板复制配置文件
cp .env.example .env
```

#### 步骤 2：编辑 .env 文件

```env
# API 配置
LANGCHAT_API_KEY=your-api-key-here
LANGCHAT_BASE_URL=https://api.openai.com/v1

# 模型配置
LANGCHAT_MODEL_NAME=gpt-4o-mini
LANGCHAT_EMBEDDING_MODEL_NAME=text-embedding-ada-002

# 模型参数
LANGCHAT_TEMPERATURE=0.7
LANGCHAT_MAX_TOKENS=1000
```

#### 配置优先级

配置按以下优先级加载（从高到低）：
1. **系统环境变量**（最高优先级）
2. **.env.local** 文件
3. **.env** 文件
4. **默认值**（最低优先级）

#### 使用系统环境变量

```bash
# Mac/Linux
export LANGCHAT_API_KEY="your-api-key"
export LANGCHAT_BASE_URL="https://api.openai.com/v1"

# Windows CMD
set LANGCHAT_API_KEY=your-api-key
set LANGCHAT_BASE_URL=https://api.openai.com/v1

# Windows PowerShell
$env:LANGCHAT_API_KEY="your-api-key"
$env:LANGCHAT_BASE_URL="https://api.openai.com/v1"
```

### 运行测试

```bash
# 编译项目
mvn clean compile

# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=LangChain4jBasicTest

# 运行特定测试方法
mvn test -Dtest=LangChain4jBasicTest#shouldGenerateSimpleResponse
```

### 运行示例

在 IntelliJ IDEA 中直接运行示例类的 main 方法，例如：
- `src/test/java/cn/langchat/learning/tutorial/introduction/HelloWorldExample.java`

### 打印配置信息

```bash
# 查看当前配置（已遮盖敏感信息）
mvn test -Dtest=PrintConfigTest
```

## 项目结构

```
langchain4j-learning/
├── .env                      # 环境配置文件（已忽略）
├── .env.example              # 环境配置模板
├── .env.local               # 本地覆盖配置（已忽略）
├── .gitignore               # Git 忽略配置
├── pom.xml                 # Maven 配置文件
├── README.md               # 项目说明文档（本文件）
│
├── docs/                   # 学习文档
│   ├── 01-introduction-to-langchain4j.md
│   ├── 02-your-first-chat-application.md
│   └── ... (40 篇文档)
│
├── src/
│   ├── main/
│   │   └── java/
│   │       └── cn/langchat/learning/
│   │           └── LearningTestsApplication.java    # 主应用类
│   │
│   └── test/
│       └── java/
│           └── cn/langchat/learning/
│               ├── util/                   # 工具类
│               │   ├── EnvConfig.java    # 配置加载器
│               │   └── TestModelProvider.java  # 模型提供者
│               │
│               └── tutorial/              # 教程测试包
│                   ├── introduction/      # 01 - 简介
│                   │   ├── HelloWorldExample.java
│                   │   ├── LangChain4jBasicTest.java
│                   │   └── VersionTest.java
│                   ├── firstchat/        # 02 - 第一个聊天应用
│                   ├── chatmodel/        # 03 - ChatModel 深入
│                   ├── embedding/        # 07 - Embedding 模型
│                   ├── embeddingstore/  # 08 - 向量存储
│                   ├── rag/             # 10 - RAG
│                   ├── aiservices/      # 11 - AI Services
│                   └── ... (其他主题)
```

## 技术栈

### 核心框架

| 技术 | 版本 | 用途 |
|------|------|------|
| **Java** | 17 | 开发语言 |
| **Maven** | 3.9+ | 构建工具 |
| **JUnit Jupiter** | 5.10.0 | 测试库 |
| **Lombok** | 1.18.24 | 简化代码 |

### LangChain4j 依赖

| 依赖 | 版本 | 说明 |
|------|------|------|
| **langchain4j** | 1.10.0 | 核心框架 |
| **langchain4j-open-ai** | - | OpenAI 集成 |

### 可选依赖

| 依赖 | 版本 | 说明 |
|------|------|------|
| **SLF4J** | 2.0.9 | 日志框架 |

## 配置说明

### 环境变量配置

支持以下配置项（通过 .env 文件或系统环境变量）：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `LANGCHAT_API_KEY` | API Key | 必需 |
| `LANGCHAT_BASE_URL` | API 地址 | https://api.openai.com/v1 |
| `LANGCHAT_MODEL_NAME` | 模型名称 | gpt-4o-mini |
| `LANGCHAT_EMBEDDING_MODEL_NAME` | 嵌入模型名称 | text-embedding-ada-002 |
| `LANGCHAT_TEMPERATURE` | 温度参数 | 0.7 |
| `LANGCHAT_MAX_TOKENS` | 最大 Token 数 | 1000 |

### 配置加载顺序

1. **系统环境变量** - 优先级最高，用于 CI/CD 或特殊场景
2. **.env.local** - 本地开发时覆盖 .env 配置
3. **.env** - 默认配置文件
4. **默认值** - 代码中的默认值

### 安全性

- ✅ `.env` 和 `.env.local` 已在 `.gitignore` 中
- ✅ API Key 等敏感信息不会提交到版本控制
- ✅ 日志输出自动遮盖敏感信息
- ✅ 支持通过系统环境变量覆盖配置

## 测试覆盖

| 文档编号 | 文档主题 | 测试包 | 测试文件 |
|---------|---------|--------|---------|
| 01 | Introduction | introduction/ | HelloWorldExample, LangChain4jBasicTest, VersionTest |
| 02 | First Chat | firstchat/ | FirstChatApplicationTest, SimpleChatApp |
| 03 | ChatModel | chatmodel/ | ChatModelDeepDiveTest, StreamingChatExample |
| 07 | Embedding Model | embedding/ | EmbeddingModelTest, TextSimilarityExample |
| 08 | Embedding Store | embeddingstore/ | EmbeddingStoreTest |
| 10 | RAG | rag/ | RagTest |
| 11 | AI Services | aiservices/ | AiServicesTest |
| ... | ... | ... | ... |

完整的文档列表请参考 `docs/` 目录。

## 开发指南

### 添加新的测试

1. 在 `src/test/java/cn/langchat/learning/tutorial/<文档编号>/` 创建测试包
2. 使用 JUnit 5 和 Lombok 编写测试
3. 使用 `TestModelProvider` 获取配置好的模型

### 代码规范

- 使用 Lombok 注解减少样板代码（@Slf4j, @Data, @Builder 等）
- 遵循 Java 命名规范
- 测试方法使用 `@DisplayName` 添加中文描述
- 使用 `TestModelProvider.getChatModel()` 获取模型实例

### 测试代码示例

```java
import cn.langchat.learning.util.TestModelProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class YourTest {

    @Test
    @DisplayName("应该生成正确的响应")
    void shouldGenerateCorrectResponse() {
        // 使用 TestModelProvider 获取配置好的模型
        ChatLanguageModel chatModel = TestModelProvider.getChatModel();
        
        // 测试代码
        String response = chatModel.generate("测试输入");
        
        log.info("响应: {}", response);
    }
}
```

### 配置获取示例

```java
import cn.langchat.learning.util.EnvConfig;

// 获取 API Key
String apiKey = EnvConfig.getApiKey();

// 获取模型名称
String modelName = EnvConfig.getModelName();

// 获取 Base URL
String baseUrl = EnvConfig.getBaseUrl();

// 获取温度参数
Double temperature = EnvConfig.getTemperature();

// 打印配置信息
EnvConfig.printConfig();
```

## 常见问题

### Q1: 如何获取 API Key？

访问相应模型提供商的官网注册并创建 API Key：
- **OpenAI**: https://platform.openai.com/api-keys
- **通义千问**: https://dashscope.aliyun.com/
- **其他兼容 OpenAI API 的服务**

### Q2: 测试失败怎么办？

检查以下项目：
1. ✅ API Key 是否在 `.env` 文件中正确配置
2. ✅ 网络连接是否正常
3. ✅ API 额度是否充足
4. ✅ 查看日志中的错误信息

### Q3: 如何切换到其他模型？

编辑 `.env` 文件中的模型配置：

```env
LANGCHAT_BASE_URL=https://api.siliconflow.cn/v1
LANGCHAT_MODEL_NAME=Qwen/Qwen2.5-72B-Instruct
```

### Q4: 如何在本地覆盖配置？

创建 `.env.local` 文件，设置需要覆盖的配置项：

```env
# .env.local 会覆盖 .env 中的对应配置
LANGCHAT_API_KEY=your-local-api-key
LANGCHAT_MODEL_NAME=your-local-model
```

### Q5: 为什么使用 .env 文件而不是系统环境变量？

使用 `.env` 文件的优点：
- ✅ 配置集中管理，易于维护
- ✅ 支持本地覆盖（.env.local）
- ✅ 不同环境可以有不同的 .env 文件
- ✅ 避免在 CI/CD 中硬编码配置
- ✅ .env 文件被 .gitignore 忽略，安全可靠

## 项目特点

### 1. 环境配置管理

- 使用 `.env` 文件统一管理配置
- 支持多级配置覆盖（环境变量 > .env.local > .env）
- 自动加载和类型转换
- 敏感信息遮盖

### 2. 统一的模型提供者

- `TestModelProvider` 提供统一的模型获取接口
- `EnvConfig` 负责配置加载和管理
- 测试代码无需关心配置细节

### 3. 模块化设计

- 每个学习主题对应独立的测试包
- 测试类和示例类分离
- 工具类独立管理

### 4. 安全性

- API Key 通过环境变量或 .env 文件管理
- 敏感配置文件被 .gitignore 忽略
- 日志输出自动遮盖敏感信息

## 参考资料

- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChain4j GitHub](https://github.com/langchain4j/langchain4j)
- [LangChain4j Tutorials](./langchain4j-tutorials/)
- [LangChat 官网](https://langchat.cn)

## 许可证

本项目版权归 LangChat Team 所有。

## 贡献指南

欢迎贡献代码、报告问题或提出改进建议！

---

**Happy Learning! 🚀**
