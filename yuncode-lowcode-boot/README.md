# Yuncode LowCode Platform

云码低代码平台 - 后端服务

## 快速开始

### 方式一：一键启动（推荐）

#### Windows

双击运行：

```
start.bat
```

#### Linux/Mac

```bash
chmod +x start.sh stop.sh
./start.sh
```

### 方式二：手动启动

#### 1. 启动后端服务

```bash
cd yuncode-admin
mvn spring-boot:run
```

#### 2. 启动网关服务（新终端）

```bash
cd yuncode-gateway
mvn spring-boot:run
```

---

## 服务地址

启动成功后，可访问以下地址：

| 服务 | 地址 | 说明 |
|------|------|------|
| **后端服务** | http://localhost:8080 | API 后端 |
| | http://localhost:8080/api | API 根路径 |
| | http://localhost:8080/api/doc.html | API 文档 (Knife4j) |
| | http://localhost:8080/api/event/types | 事件类型列表 |
| **网关服务** | http://localhost:9000 | 统一网关 |
| | http://localhost:9000/gateway/status | 网关状态 |
| | http://localhost:9000/gateway/routes | 网关路由列表 |

---

## 停止服务

### Windows

双击运行：

```
stop.bat
```

### Linux/Mac

```bash
./stop.sh
```

---

## 前端配置

修改前端项目中的 API 地址配置：

```javascript
// 从直接访问后端
const API_BASE_URL = 'http://localhost:8080/api'

// 改为通过网关访问
const API_BASE_URL = 'http://localhost:9000/api'
```

---

## Nacos 配置（可选）

### 当前状态

默认不启用 Nacos，使用固定路由模式。

### 如何启用 Nacos

详细文档请查看：[Nacos切换指南](../docs/Nacos切换指南.md)

#### 快速切换

**方式一：使用 Profile（推荐）**

```bash
# 后端
cd yuncode-admin
mvn spring-boot:run -Dspring-boot.run.profiles=nacos

# 网关
cd yuncode-gateway
mvn spring-boot:run -Dspring-boot.run.profiles=nacos
```

**方式二：修改配置文件**

编辑 `application.yml`：

```yaml
nacos:
  discovery:
    enabled: true
```

### 检查 Nacos 状态

访问网关状态接口：

```bash
curl http://localhost:9000/gateway/status
```

查看返回值中的：

- `"nacosEnabled": true/false`
- `"mode": "Nacos Service Discovery"` 或 `"Fixed Routing"`

---

## 事件系统

详细文档请查看：[事件系统使用说明](../docs/事件系统使用说明.md)

### 快速示例

```java
@Autowired
private SimpleEventBus eventBus;

// 发布事件
eventBus.publish(EventTypes.APP_CREATE, "my-service", data);

// 注册消费者
eventBus.registerConsumer(EventTypes.APP_CREATE, event -> {
    // 处理事件
});
```

---

## 项目结构

```
yuncode-lowcode-boot/
├── yuncode-common/       # 通用模块（事件总线等）
├── yuncode-auth/         # 认证模块
├── yuncode-system/       # 系统模块
├── yuncode-tenant/       # 租户模块
├── yuncode-admin/        # 后端启动模块 (Port: 8080)
├── yuncode-gateway/      # 网关模块 (Port: 9000)
├── docs/                 # 文档目录
├── start.bat / start.sh  # 一键启动脚本
└── stop.bat / stop.sh    # 一键停止脚本
```

---

## 常见问题

### Q: 脚本启动失败怎么办？

A: 请确保已安装 Maven 和 Java 17+。如脚本无法使用，请使用手动启动方式。

### Q: 如何查看服务日志？

**Windows:**
- 直接查看弹出的两个窗口

**Linux/Mac:**
```bash
tail -f logs/admin.log    # 查看后端日志
tail -f logs/gateway.log  # 查看网关日志
```

### Q: Nacos 是必需的吗？

A: 不是必需的。默认不启用 Nacos，使用固定路由模式即可。需要时可随时切换。

### Q: 端口冲突怎么办？

A: 修改 `application.yml` 中的 `server.port` 配置项。

---

## 更多文档

- [网关方案设计](../docs/网关方案设计.md)
- [网关方案实现总结](../docs/网关方案实现总结.md)
- [事件系统使用说明](../docs/事件系统使用说明.md)
- [Nacos切换指南](../docs/Nacos切换指南.md)
