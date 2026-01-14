---
title: '部署和运维'
description: '学习 LangChain4j 的 部署和运维 功能和特性'
---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn

# 27 - 部署和运维

## 版本说明

本文档基于 **LangChain4j 1.10.0** 版本编写。

## 学习目标

通过本章节学习，你将能够：
- 理解 LangChain4j 应用的部署架构
- 掌握容器化部署（Docker）
- 学会 Kubernetes 集群部署
- 理解 CI/CD 流水线配置
- 掌握监控和日志收集
- 实现高可用和可扩展的部署

## 核心概念

### 部署架构

```
┌─────────────────────────────────────────────────┐
│              部署架构                              │
├─────────────────────────────────────────────────┤
│                                                           │
│  ┌──────────┐    ┌────────────┐    ┌───────────────┐  │
│  │ 用户     │    │ 负载均衡   │    │   服务集群     │  │
│  │ 访问     │◄──►│  (Nginx)  │◄──►│   (K8s)      │  │
│  └────┬─────┘    └────┬───────┘    └───────┬───────┘  │
│       │                 │                  │            │  │
│       │                 │                  │       ┌────▼────┐  │
│       │                 │                  └──────►│  数据库    │  │
│       └─────────────────┘                       │ (Redis)   │  │
│                      ┌────────────────────────────────┤  └──────────┘  │
│                      │            缓存层             │               │  │
│                      └────────────────────────────────┘               │  │
│                                                                   │  │
│                          监控和日志收集                         │  │
│                          (Prometheus + Grafana)                  │  │
│                                                                   │  │
└─────────────────────────────────────────────────────┘
```

## Docker 部署

### Dockerfile

```dockerfile
FROM openjdk:17-slim

LABEL maintainer="your-email@example.com"
LABEL version="1.0.0"
LABEL description="LangChain4j Demo Application"

# 设置工作目录
WORKDIR /app

# 复制 Maven 配置
COPY pom.xml .

# 复制源代码
COPY src ./src

# 构建应用（使用 Maven）
RUN mvn clean package -DskipTests

# 暴露端口
EXPOSE 8080

# 设置 JVM 参数
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:+UseStringDeduplication"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/target/langchat-demo.jar"]
```

### Docker Compose

```yaml
version: '3.8'

services:
  # LangChain4j 应用
  langchat-app:
    build: .
    container_name: langchat-app
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/langchat
      - SPRING_DATASOURCE_USERNAME=langchat
      - SPRING_DATASOURCE_PASSWORD=langchat123
      - REDIS_HOST=redis
      - REDIS_PORT=6379
    depends_on:
      - postgres
      - redis
    networks:
      - langchat-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"

  # PostgreSQL 数据库
  postgres:
    image: postgres:15-alpine
    container_name: langchat-postgres
    environment:
      - POSTGRES_DB=langchat
      - POSTGRES_USER=langchat
      - POSTGRES_PASSWORD=langchat123
      - POSTGRES_INITDB_ARGS=--encoding=UTF8
    volumes:
      - postgres-data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    networks:
      - langchat-network
    restart: unless-stopped

  # Redis 缓存
  redis:
    image: redis:7-alpine
    container_name: langchat-redis
    command: redis-server --appendonly yes
    volumes:
      - redis-data:/data
    ports:
      - "6379:6379"
    networks:
      - langchat-network
    restart: unless-stopped

  # Prometheus 监控
  prometheus:
    image: prom/prometheus:v2.45.0
    container_name: langchat-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/usr/share/prometheus/console_libraries'
      - '--web.console.templates=/usr/share/prometheus/console_libraries'
      - '--web.console.libraries=/usr/share/prometheus/console_libraries'
      - '--storage.tsdb.retention.time=200h'
      - '--web.enable-lifecycle'
    networks:
      - langchat-network
    restart: unless-stopped

  # Grafana 可视化
  grafana:
    image: grafana/grafana:10.0.0
    container_name: langchat-grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - grafana-data:/var/lib/grafana
    networks:
      - langchat-network
    restart: unless-stopped

networks:
  langchat-network:
    driver: bridge

volumes:
  postgres-data:
  redis-data:
  prometheus-data:
  grafana-data:
```

## Kubernetes 部署

### 应用部署

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: langchat-app
  namespace: default
spec:
  replicas: 3
  selector:
    matchLabels:
      app: langchat
  template:
    metadata:
      labels:
        app: langchat
        version: v1
    spec:
      containers:
      - name: langchat
        image: langchat/langchat-demo:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: openai-api-key
              key: api-key
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:postgresql://postgres:5432/langchat"
        - name: SPRING_DATASOURCE_USERNAME
          value: "langchat"
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: postgres-password
              key: password
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /api/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /api/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
---
apiVersion: v1
kind: Service
metadata:
  name: langchat-service
spec:
  selector:
    app: langchat
  ports:
    - protocol: TCP
      port: 8080
      targetPort: 8080
  type: LoadBalancer
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: langchat-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: langchat-app
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Pods
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

### 配置管理

```yaml
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: langchat-config
  namespace: default
data:
  application.yml: |
    server:
      port: 8080
      tomcat:
        max-threads: 200
        accept-count: 100
    spring:
      application:
        name: langchat
      profiles:
        active: ${SPRING_PROFILES_ACTIVE:prod}
    langchain4j:
      open-ai:
        chat-model:
          api-key: ${OPENAI_API_KEY}
          model-name: gpt-4o-mini
          temperature: 0.7
          max-tokens: 1000
          timeout: 60s
          max-retries: 3
        streaming-chat-model:
          api-key: ${OPENAI_API_KEY}
          model-name: gpt-4o-mini
          temperature: 0.7
          max-tokens: 1000
          timeout: 60s
        embedding-model:
          api-key: ${OPENAI_API_KEY}
          model-name: text-embedding-3-small
          timeout: 60s
    logging:
      level:
        root: INFO
        dev.langchain4j: DEBUG
      pattern:
        console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
        file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
      file:
        name: /app/logs/langchat-demo.log
        max-size: 100MB
        max-history: 30
```

## 监控配置

### Prometheus 配置

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'langchat'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    static_configs:
      - targets: ['langchat-service:8080']
```

### Grafana Dashboard

```json
{
  "dashboard": {
    "id": "langchat-dashboard",
    "title": "LangChain4j Dashboard",
    "tags": ["langchain4j", "llm"],
    "timezone": "browser",
    "refresh": "30s",
    "panels": [
      {
        "id": 1,
        "title": "JVM Memory",
        "type": "graph",
        "targets": [
          {
            "expr": "jvm_memory_max_bytes{application=\"langchat\"}",
            "legendFormat": "max bytes",
            "refId": "A"
          },
          {
            "expr": "jvm_memory_committed_bytes{application=\"langchat\"}",
            "legendFormat": "committed bytes",
            "refId": "B"
          },
          {
            "expr": "jvm_memory_used_bytes{application=\"langchat\"}",
            "legendFormat": "used bytes",
            "refId": "C"
          }
        ]
      },
      {
        "id": 2,
        "title": "HTTP Requests",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count{application=\"langchat\"}[1m])",
            "legendFormat": "{{method}} {{uri}}",
            "refId": "D"
          }
        ]
      },
      {
        "id": 3,
        "title": "Response Time",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_sum{application=\"langchat\"}[1m])",
            "legendFormat": "avg response time",
            "refId": "E"
          }
        ]
      }
    ]
  }
}
```

## CI/CD 配置

### GitHub Actions

```yaml
name: Build and Deploy

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

env:
  DOCKER_USERNAME: ${{ secrets.DOCKER_USERNAME }}
  DOCKER_PASSWORD: ${{ secrets.DOCKER_PASSWORD }}
  OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
  KUBECONFIG: ${{ secrets.KUBECONFIG }}

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'maven'

      - name: Build with Maven
        run: mvn clean package -DskipTests

      - name: Build Docker image
        run: |
          echo "${{ secrets.GITHUB_TOKEN }}" | docker login ghcr.io -u ${{ github.actor }} --password-stdin
          docker build -t ghcr.io/langchat/langchat-demo:${{ github.sha }} -t ghcr.io/langchat/langchat-demo:latest .

      - name: Push Docker image
        run: |
          echo "${{ secrets.GITHUB_TOKEN }}" | docker login ghcr.io -u ${{ github.actor }} --password-stdin
          docker push ghcr.io/langchat/langchat-demo:${{ github.sha }}
          docker push ghcr.io/langchat/langchat-demo:latest

  deploy-to-kubernetes:
    needs: build-and-push
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v3

      - name: Configure kubectl
        run: |
          mkdir -p $HOME/.kube
          echo "$KUBECONFIG" | base64 -d > $HOME/.kube/config
          chmod 600 $HOME/.kube/config

      - name: Deploy to Kubernetes
        run: |
          kubectl set image deployment/langchat-app langchat=ghcr.io/langchat/langchat-demo:${{ github.sha }}
          kubectl rollout status deployment/langchat-app

      - name: Verify deployment
        run: |
          kubectl rollout status deployment/langchat-app
          kubectl get pods -l app=langchat
```

## 运维最佳实践

### 日志收集

```yaml
# Logstash 配置
input {
  file {
    path => "/var/log/langchat/*.log"
    start_position => "beginning"
  }
}

filter {
  if [application] == "langchat" {
    grok {
      match => { "message" => "%{TIMESTAMP} \[%{THREAD}\] %{LOGLEVEL} %{LOGGER} - %{MESSAGE}" }
    }
    date {
      match => ["TIMESTAMP", "timestamp"]
      target => "@timestamp"
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "langchat-logs-%{+YYYY.MM.dd}"
  }
}
```

### 告警规则

```yaml
# alerting.yaml
groups:
  - name: langchat-alerts
    rules:
      - alert: HighErrorRate
        expr: |
          rate(http_server_requests_seconds_count{application="langchat",status="5xx"}[5m]) > 10
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "More than 10 errors in the last 5 minutes"

      - alert: HighResponseTime
        expr: |
          histogram_quantile(0.95, rate(http_server_requests_seconds_sum{application="langchat"}[5m])) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High response time detected"
          description: "95th percentile response time > 2s"

      - alert: PodRestarting
        expr: |
          increase(kube_pod_container_status_restarts_total{namespace="default",container="langchat"}[1h]) > 3
        for: 1h
        labels:
          severity: warning
        annotations:
          summary: "Pod is restarting frequently"
          description: "Container restarted more than 3 times in the last hour"
```

## 总结

### 本章要点

1. **部署方式**
   - Docker 容器化
   - Docker Compose 本地开发
   - Kubernetes 生产部署
   - Helm Chart 管理

2. **运维策略**
   - 监控和告警
   - 日志收集
   - 自动扩缩容
   - 配置管理

3. **最佳实践**
   - 使用环境变量管理密钥
   - 实施健康检查
   - 配置资源限制
   - 启用自动重启
   - 定期备份数据

4. **监控指标**
   - CPU 和内存使用率
   - 请求速率和响应时间
   - 错误率和重启次数
   - 数据库连接池状态

5. **CI/CD**
   - 自动构建和测试
   - 自动部署
   - 回滚机制
   - 环境隔离

## 参考资料

- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [Docker 官方文档](https://docs.docker.com/)
- [Kubernetes 官方文档](https://kubernetes.io/zh/docs/)
- [Prometheus 官方文档](https://prometheus.io/)
- [LangChat 官网](https://langchat.cn)

---

> 版权归属于 LangChat Team  
> 官网：https://langchat.cn
