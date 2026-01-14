---
title: '故障排查'
description: '学习 LangChain4j 的 故障排查 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 29 - 故障排查

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 诊断常见的 LangChain4j 应用问题
- 理解错误日志的分析方法
- 掌握性能问题的排查技巧
- 学会调试工具的使用
- 掌握日志和监控的应用

## 常见问题

### 连接问题

```
问题：API 连接超时
原因：网络问题、API Key 错误、速率限制
解决：
1. 检查网络连接
2. 验证 API Key
3. 检查速率限制
4. 增加 timeout 时间
```

### 内存问题

```
问题：OOM 错误
原因：响应过大、内存泄漏
解决：
1. 减少 maxTokens
2. 使用流式输出
3. 检查内存泄漏
4. 增加 JVM 堆内存
```

### Token 问题

```
问题：Token 计数不准确
原因：估算错误、模型响应格式
解决：
1. 使用官方 Token 计数接口
2. 检查模型响应
3. 调整 Prompt 长度
```

## 调试工具

### 日志配置

```yaml
logging:
  level:
    dev.langchain4j: DEBUG
    okhttp3: DEBUG
  pattern:
    console: "%d{HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 监控端点

```java
// 应用信息
http://localhost:8080/actuator/health

// 指标
http://localhost:8080/actuator/metrics

// 信息
http://localhost:8080/actuator/info
```

## 参考资料

- [LangChain4j 文档](https://docs.langchain4j.dev/)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
