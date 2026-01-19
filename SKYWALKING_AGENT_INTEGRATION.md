# SkyWalking Agent 集成指南

## 方式一：使用 Maven/Gradle（推荐）⭐

这种方式的优点是 **不需要手动下载 Agent**，Maven 会自动下载！

### 1. 添加依赖

```xml
<!-- pom.xml -->
<dependencies>
    <!-- SkyWalking Agent Toolkit -->
    <dependency>
        <groupId>org.apache.skywalking</groupId>
        <artifactId>apm-toolkit-trace</artifactId>
        <version>8.16.0</version>
    </dependency>
</dependencies>
```

### 2. 启动时自动追踪

**无需任何参数！**

只需添加依赖，SkyWalking Agent Toolkit 会自动工作。

---

## 方式二：使用 Docker（推荐用于测试）⭐⭐

### 1. 确保有 Docker

```bash
# 检查 Docker 是否运行
docker --version
```

### 2. 创建 docker-compose.yml

```yaml
version: '3.8'

services:
  # SkyWalking OAP Server
  oap:
    image: apache/skywalking-oap-server:8.16.0-es7
    ports:
      - "11800:11800"  # Agent 上报端口
      - "12800:12800"  # UI 访问端口
    environment:
      - SW_STORAGE=elasticsearch7
      - SW_ES_URL=jdbc:elasticsearch7://elasticsearch:9200
    depends_on:
      - elasticsearch
    ports:
      - "9200:9200"
    links:
      - elasticsearch
    restart: unless-stopped

  # Elasticsearch
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:7.17.0
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    volumes:
      - ./es-data:/usr/share/elasticsearch/data
    restart: unless-stopped

  # SkyWalking UI (可选)
  ui:
    image: apache/skywalking-ui:8.16.0
    depends_on:
      - oap
    ports:
      - "8088:8080"
    environment:
      - SW_OAP_ADDRESS=http://oap:12800
    restart: unless-stopped
```

### 3. 启动

```bash
docker-compose up -d
```

**访问 UI:** http://localhost:8088

---

## 方式三：使用 Java Agent（集成到应用启动）⭐⭐⭐

### 1. 下载 Agent

```bash
# 方式A: 从华为云下载（国内快）
wget https://repo.huaweicloud.com/repository/apache/skywalking/java-agent/8.16.0/apache-skywalking-java-agent-8.16.0.tgz

# 方式B: 使用备用镜像
curl -L https://dlcdn.apache.org/dist/skywalking/8.16.0/apache-skywalking-bin-linux.tar.gz -o skywalking-agent.tar.gz
```

### 2. 解压

```bash
tar -xzf apache-skywalking-java-agent-8.16.0.tgz
```

### 3. 启动应用时使用 Agent

```bash
java -javaagent:./skywalking-agent/skywalking-agent.jar \
     -Dskywalking.agent.service_name=yuncode-lowcode \
     -Dskywalking.collector.backend_service=localhost:11800 \
     -jar yuncode-lowcode.jar
```

---

## 🎯 推荐的方案

对于您的低代码平台：

### 当前开发阶段
```
✅ 不使用 Agent
→ 仅使用 MDC + 数据库
→ 减少复杂度
→ 降低资源占用
```

### 测试阶段（验证链路追踪）
```
✅ 使用 Docker Compose
→ 一键启动完整服务
→ 验证功能
→ 测试 Agent 连接
```

### 生产环境
```
✅ 使用 Java Agent（可选）
→ 内置到启动脚本
→ 无需手动操作
→ 自动上报监控数据
```

---

## 📝 快速测试步骤

### 测试 Agent 连接

1. **启动 OAP Server（通过 Docker）：**
```bash
docker run -d \
  --name skywalking-oap \
  -p 11800:11800 \
  -p 12800:12800 \
  -e SW_STORAGE=h2 \
  apache/skywalking-oap-server:8.16.0
```

2. **启动应用测试（带 Agent）：**
```bash
java -javaagent:./skywalking-agent/skywalking-agent.jar \
     -Dskywalking.agent.service_name=yuncode-test \
     -Dskywalking.collector.backend_service=localhost:11800 \
     -jar yuncode-lowcode.jar
```

3. **查看 SkyWalking UI：**
```
http://localhost:8088
```

---

## 🔧 如果必须二进制集成

我建议等待依赖安装完成后再配置 Agent。

现在的核心功能（MDC + 数据库）已经完全满足链路追踪需求，SkyWalking 只是锦上添花。
