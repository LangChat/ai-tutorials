---
title: '成本优化'
description: '学习 LangChain4j 的 成本优化 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 28 - 成本优化

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 理解 LLM 应用的成本构成
- 掌握 Token 使用优化策略
- 学会选择合适的模型和参数
- 理解缓存和批量处理的成本效益
- 掌握成本监控和分析方法
- 实现一个成本优化的 LLM 应用

## 核心概念

### 成本构成

```
总成本 = Token 成本 + API 调用成本 + 基础设施成本
```

| 成本类型 | 描述 | 占比 |
|---------|------|------|
| **Token 成本** | 输入/输出 Token 计费 | 60-80% |
| **API 调用成本** | 请求次数 × 单价 | 10-20% |
| **基础设施** | 服务器、存储、带宽 | 5-15% |
| **其他** | 监控、日志、备份 | 1-5% |

## 模型选择策略

### 成本对比

| 模型 | Token 成本/1K | 质量 | 适用场景 |
|------|-------------|------|---------|
| gpt-4o-mini | $0.001 | 高 | 日常对话、简单任务 |
| gpt-4o | $0.015 | 很高 | 复杂推理、代码生成 |
| gpt-3.5-turbo | $0.002 | 高 | 平衡质量和成本 |
| gpt-3.5 | $0.004 | 中 | 中等复杂度任务 |

### 模型选择建议

```java
import org.springframework.stereotype.Component;

/**
 * 成本优化策略
 */
@Component
public class CostOptimization {

    /**
     * 模型类型
     */
    public enum ModelType {
        MINI("gpt-4o-mini", 0.001, "低成本，高质量"),
        STANDARD("gpt-4o", 0.015, "高质量，高成本"),
        TURBO("gpt-3.5-turbo", 0.002, "平衡质量和成本"),
        BASE("gpt-3.5", 0.004, "中等质量，中等成本");

        private final String modelName;
        private final double costPer1kTokens;
        private final String description;

        ModelType(String modelName, double costPer1kTokens, String description) {
            this.modelName = modelName;
            this.costPer1kTokens = costPer1kTokens;
            this.description = description;
        }

        public String getModelName() { return modelName; }
        public double getCostPer1kTokens() { return costPer1kTokens; }
        public String getDescription() { return description; }
    }

    /**
     * 任务类型
     */
    public enum TaskType {
        SIMPLE_CHAT("简单对话", ModelType.MINI),
        COMPLEX_REASONING("复杂推理", ModelType.STANDARD),
        CODE_GENERATION("代码生成", ModelType.STANDARD),
        TRANSLATION("翻译", ModelType.MINI),
        SUMMARY("摘要", ModelType.MINI),
        QUESTION_ANSWERING("问答", ModelType.TURBO);

        private final String description;
        private final ModelType recommendedModel;

        TaskType(String description, ModelType recommendedModel) {
            this.description = description;
            this.recommendedModel = recommendedModel;
        }

        public String getDescription() { return description; }
        public ModelType getRecommendedModel() { return recommendedModel; }
    }

    /**
     * 成本计算
     */
    public static class CostCalculator {
        private final double costPer1kTokens;

        public CostCalculator(double costPer1kTokens) {
            this.costPer1kTokens = costPer1kTokens;
        }

        /**
         * 计算成本
         */
        public CostResult calculateCost(int inputTokens, int outputTokens) {
            int totalTokens = inputTokens + outputTokens;
            double cost = (totalTokens / 1000.0) * costPer1kTokens;

            return new CostResult(
                inputTokens,
                outputTokens,
                totalTokens,
                cost,
                costPer1kTokens
            );
        }

        /**
         * 成本结果
         */
        public static class CostResult {
            private final int inputTokens;
            private final int outputTokens;
            private final int totalTokens;
            private final double cost;
            private final double costPer1kTokens;

            public CostResult(int inputTokens, int outputTokens, 
                            int totalTokens, double cost, double costPer1kTokens) {
                this.inputTokens = inputTokens;
                this.outputTokens = outputTokens;
                this.totalTokens = totalTokens;
                this.cost = cost;
                this.costPer1kTokens = costPer1kTokens;
            }

            // Getters
            public int getInputTokens() { return inputTokens; }
            public int getOutputTokens() { return outputTokens; }
            public int getTotalTokens() { return totalTokens; }
            public double getCost() { return cost; }
            public double getCostPer1kTokens() { return costPer1kTokens; }

            @Override
            public String toString() {
                return String.format(
                    "CostResult{input=%d, output=%d, total=%d, cost=$%.4f, rate=$%.5f/1K}",
                    inputTokens, outputTokens, totalTokens, cost, costPer1kTokens
                );
            }
        }
    }

    /**
     * 成本优化分析
     */
    public static class CostAnalysis {
        private final Map<TaskType, CostResult> taskCosts;
        private final CostSavings potentialSavings;

        public CostAnalysis(Map<TaskType, CostResult> taskCosts, CostSavings potentialSavings) {
            this.taskCosts = taskCosts;
            this.potentialSavings = potentialSavings;
        }

        public CostSavings getPotentialSavings() { return potentialSavings; }
        public Map<TaskType, CostResult> getTaskCosts() { return taskCosts; }

        /**
         * 生成成本报告
         */
        public CostReport generateReport() {
            return new CostReport(taskCosts, potentialSavings);
        }
    }

    /**
     * 成本节省
     */
    public static class CostSavings {
        private final double originalCost;
        private final double optimizedCost;
        private final double savings;
        private final double savingsPercentage;

        public CostSavings(double originalCost, double optimizedCost) {
            this.originalCost = originalCost;
            this.optimizedCost = optimizedCost;
            this.savings = originalCost - optimizedCost;
            this.savingsPercentage = originalCost > 0 ? (savings / originalCost) * 100 : 0;
        }

        public double getOriginalCost() { return originalCost; }
        public double getOptimizedCost() { return optimizedCost; }
        public double getSavings() { return savings; }
        public double getSavingsPercentage() { return savingsPercentage; }
    }

    /**
     * 成本报告
     */
    public static class CostReport {
        private final Map<TaskType, CostResult> taskCosts;
        private final CostSavings totalSavings;
        private final String date;

        public CostReport(Map<TaskType, CostResult> taskCosts, CostSavings totalSavings) {
            this.taskCosts = taskCosts;
            this.totalSavings = totalSavings;
            this.date = java.time.LocalDateTime.now().toString();
        }

        public Map<TaskType, CostResult> getTaskCosts() { return taskCosts; }
        public CostSavings getTotalSavings() { return totalSavings; }
        public String getDate() { return date; }
    }

    /**
     * 成本优化配置
     */
    public static class CostOptimizationConfig {
        private final ModelType defaultModel;
        private final int maxTokens;
        private final double temperature;
        private final int maxHistoryTokens;
        private final boolean enableCaching;

        public CostOptimizationConfig(ModelType defaultModel, int maxTokens, 
                                   double temperature, int maxHistoryTokens, boolean enableCaching) {
            this.defaultModel = defaultModel;
            this.maxTokens = maxTokens;
            this.temperature = temperature;
            this.maxHistoryTokens = maxHistoryTokens;
            this.enableCaching = enableCaching;
        }

        public static CostOptimizationConfig createDefaultConfig() {
            return new CostOptimizationConfig(
                    ModelType.MINI,      // 默认使用 mini
                    500,                   // 最大 500 Token
                    0.7,                   // 温度 0.7
                    2000,                  // 最大历史 2000 Token
                    true                    // 启用缓存
            );
        }

        public static CostOptimizationConfig createAggressiveConfig() {
            return new CostOptimizationConfig(
                    ModelType.MINI,
                    200,                   // 最大 200 Token
                    0.7,
                    1000,                  // 最大历史 1000 Token
                    true
            );
        }

        public ModelType getDefaultModel() { return defaultModel; }
        public int getMaxTokens() { return maxTokens; }
        public double getTemperature() { return temperature; }
        public int getMaxHistoryTokens() { return maxHistoryTokens; }
        public boolean isEnableCaching() { return enableCaching; }
    }

    /**
     * 成本监控
     */
    @Component
    public static class CostMonitor {
        private final CostCalculator calculator;
        private final Map<String, List<CostResult>> userCosts = new java.util.concurrent.ConcurrentHashMap<>();

        public CostMonitor() {
            this.calculator = new CostCalculator(0.001);  // gpt-4o-mini 价格
        }

        /**
         * 记录用户成本
         */
        public void recordUserCost(String userId, int inputTokens, int outputTokens) {
            userCosts.computeIfAbsent(userId, k -> new java.util.ArrayList<>())
                    .add(calculator.calculateCost(inputTokens, outputTokens));
        }

        /**
         * 获取用户总成本
         */
        public double getUserTotalCost(String userId) {
            return userCosts.getOrDefault(userId, new java.util.ArrayList<>())
                    .stream()
                    .mapToDouble(CostResult::getCost)
                    .sum();
        }

        /**
         * 获取用户 Token 使用
         */
        public CostUsage getUserTokenUsage(String userId) {
            List<CostResult> costs = userCosts.get(userId);
            if (costs == null || costs.isEmpty()) {
                return null;
            }

            int totalInput = costs.stream().mapToInt(CostResult::getInputTokens).sum();
            int totalOutput = costs.stream().mapToInt(CostResult::getOutputTokens).sum();

            return new CostUsage(totalInput, totalOutput, totalInput + totalOutput, getUserTotalCost(userId));
        }

        /**
         * Token 使用
         */
        public static class CostUsage {
            private final int totalInputTokens;
            private final int totalOutputTokens;
            private final int totalTokens;
            private final double totalCost;

            public CostUsage(int totalInputTokens, int totalOutputTokens, 
                            int totalTokens, double totalCost) {
                this.totalInputTokens = totalInputTokens;
                this.totalOutputTokens = totalOutputTokens;
                this.totalTokens = totalTokens;
                this.totalCost = totalCost;
            }

            public int getTotalInputTokens() { return totalInputTokens; }
            public int getTotalOutputTokens() { return totalOutputTokens; }
            public int getTotalTokens() { return totalTokens; }
            public double getTotalCost() { return totalCost; }

            @Override
            public String toString() {
                return String.format("CostUsage{input=%d, output=%d, total=%d, cost=$%.4f}",
                            totalInputTokens, totalOutputTokens, totalTokens, totalCost);
            }
        }
    }

    /**
     * 主方法示例
     */
    public static void main(String[] args) {
        // 成本计算器
        CostCalculator calculator = new CostCalculator(0.001);

        // 计算不同任务的成本
        Map<TaskType, CostResult> taskCosts = new java.util.HashMap<>();

        taskCosts.put(TaskType.SIMPLE_CHAT, calculator.calculateCost(100, 200));
        taskCosts.put(TaskType.COMPLEX_REASONING, calculator.calculateCost(300, 1000));
        taskCosts.put(TaskType.CODE_GENERATION, calculator.calculateCost(500, 1500));
        taskCosts.put(TaskType.TRANSLATION, calculator.calculateCost(150, 300));
        taskCosts.put(TaskType.SUMMARY, calculator.calculateCost(50, 100));

        // 计算总成本
        double totalOriginalCost = taskCosts.values().stream().mapToDouble(CostResult::getCost).sum();

        // 使用 mini 模型的优化后成本
        Map<TaskType, CostResult> optimizedCosts = new java.util.HashMap<>();
        CostCalculator miniCalculator = new CostCalculator(0.001);

        optimizedCosts.put(TaskType.SIMPLE_CHAT, miniCalculator.calculateCost(80, 150));
        optimizedCosts.put(TaskType.COMPLEX_REASONING, miniCalculator.calculateCost(250, 800));
        optimizedCosts.put(TaskType.CODE_GENERATION, miniCalculator.calculateCost(400, 1200));
        optimizedCosts.put(TaskType.TRANSLATION, miniCalculator.calculateCost(120, 250));
        optimizedCosts.put(TaskType.SUMMARY, miniCalculator.calculateCost(40, 80));

        double totalOptimizedCost = optimizedCosts.values().stream().mapToDouble(CostResult::getCost).sum();

        // 成本节省
        CostSavings savings = new CostSavings(totalOriginalCost, totalOptimizedCost);

        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║              成本优化分析                               ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════╣");
        System.out.printf("║ 原始成本: $%.4f                                 ║\n", totalOriginalCost);
        System.out.printf("║ 优化后成本: $%.4f                                 ║\n", totalOptimizedCost);
        System.out.printf("║ 节省: $%.4f (%.1f%%)                           ║\n", 
                    savings.getSavings(), savings.getSavingsPercentage());
        System.out.println("╠═══════════════════════════════════════════════════════════════╣");
        System.out.println("║ 任务成本对比:                                            ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════╣");

        taskCosts.forEach((taskType, originalCost) -> {
            CostResult optimized = optimizedCosts.get(taskType);
            double saving = originalCost.getCost() - optimized.getCost();
            double savingPercent = (saving / originalCost.getCost()) * 100;

            System.out.printf("║ %-20s 原始: $%.4f 优化: $%.4f 节省: $%.4f (%.1f%%)    ║\n",
                        taskType.getDescription(),
                        originalCost.getCost(),
                        optimized.getCost(),
                        saving,
                        savingPercent
            );
        });

        System.out.println("╚═════════════════════════════════════════════════════════════════════════════════════════════════════════════╝");

        // 优化建议
        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║              成本优化建议                               ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════╣");
        System.out.println("║ 1. 使用 gpt-4o-mini 处理简单任务（对话、翻译、摘要）     ║");
        System.out.println("║ 2. 优化 Prompt 以减少输入 Token                          ║");
        System.out.println("║ 3. 限制输出长度（设置 max_tokens）                        ║");
        System.out.println("║ 4. 使用缓存减少重复请求成本                           ║");
        System.out.println("║ 5. 使用批量处理提高 Token 效率                         ║");
        System.out.println("║ 6. 根据任务类型选择合适的模型                        ║");
        System.out.println("║ 7. 定期审查成本使用情况                                    ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════════════════════════════════════════════╝");
    }
}
```

## 总结

### 本章要点

1. **成本构成**
   - Token 成本（60-80%）
   - API 调用成本（10-20%）
   - 基础设施成本（5-15%）

2. **优化策略**
   - 选择合适的模型
   - 优化 Token 使用
   - 实施缓存策略
   - 批量处理请求

3. **最佳实践**
   - 定期成本审计
   - 设置成本预算
   - 监控成本指标
   - 优化 Prompt
   - 使用缓存

4. **工具**
   - 成本计算器
   - Token 使用分析
   - 成本报告生成

5. **参考价格**
   - gpt-4o-mini: $0.001/1K tokens
   - gpt-4o: $0.015/1K tokens
   - gpt-3.5-turbo: $0.002/1K tokens

## 参考资料

- [LangChain4j 文档](https://docs.langchain4j.dev/)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
