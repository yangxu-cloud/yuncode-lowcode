# Yuncode Gateway

云码低代码平台统一网关服务

## 当前实现

### 已实现功能

| 功能 | 说明 |
|------|------|
| 统一入口 | 所有请求通过网关（9000端口）进入 |
| 路由转发 | 将 `/api/**` 请求转发到后端（8080端口） |
| 请求日志 | 记录所有请求的方法、路径、状态码、耗时 |
| 认证过滤 | Token 验证和白名单处理 |
| 事件总线 | 系统事件统一处理 |
| 跨域支持 | CORS 配置 |
| **Nacos 开关** | 通过配置切换服务发现模式 |

### 架构

```
┌─────────────────┐
│  前端应用       │
└────────┬────────┘
         │
         ▼
┌──────────────────────────────────┐
│  yuncode-gateway:9000           │
│  • 请求日志                     │
│  • 认证过滤                     │
│  • 事件总线                     │
│  • Nacos: OFF/ON (可配置)       │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│  yuncode-admin:8080             │
│  • Nacos: OFF/ON (可配置)       │
└──────────────────────────────────┘
```

## 快速开始

### 方式一：不使用 Nacos（默认）

这是最简单的方式，不需要额外部署任何中间件。

#### 1. 启动后端服务

```bash
cd yuncode-admin
mvn spring-boot:run
```

#### 2. 启动网关

```bash
cd yuncode-gateway
mvn spring-boot:run
```

#### 3. 验证

```bash
# 查看网关状态
curl http://localhost:9000/gateway/status
```

响应中 `"nacosEnabled": false`，表示当前使用固定路由模式。

---

### 方式二：启用 Nacos 服务发现

当需要支持微服务时，可以启用 Nacos。

#### 1. 部署 Nacos

```bash
# 下载并启动 Nacos (单机模式)
# Windows:
startup.cmd -m standalone

# Linux/Mac:
sh startup.sh -m standalone
```

#### 2. 配置并启动后端

修改 `yuncode-admin/src/main/resources/application.yml`：

```yaml
nacos:
  discovery:
    enabled: true
```

或者使用 profile：

```bash
cd yuncode-admin
mvn spring-boot:run -Dspring-boot.run.profiles=nacos
```

#### 3. 配置并启动网关

修改 `yuncode-gateway/src/main/resources/application.yml`：

```yaml
nacos:
  discovery:
    enabled: true
```

或者使用 profile：

```bash
cd yuncode-gateway
mvn spring-boot:run -Dspring-boot.run.profiles=nacos
```

#### 4. 验证

```bash
# 查看网关状态
curl http://localhost:9000/gateway/status
```

响应中 `"nacosEnabled": true`，表示当前使用 Nacos 服务发现模式。

---

## Nacos 切换指南

### 从固定路由切换到 Nacos

#### 1. 部署 Nacos Server

下载地址：https://github.com/alibaba/nacos/releases

#### 2. 修改网关配置

编辑 `yuncode-gateway/src/main/resources/application.yml`：

```yaml
nacos:
  discovery:
    enabled: true
    server-addr: localhost:8848

spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      routes:
        - id: yuncode-admin
          uri: lb://yuncode-admin  # 使用服务名而非固定地址
          predicates:
            - Path=/api/**
```

或者使用现成的 profile 配置：

```yaml
spring:
  profiles:
    active: nacos  # 使用 application-nacos.yml 配置
```

#### 3. 修改后端配置

编辑 `yuncode-admin/src/main/resources/application.yml`：

```yaml
nacos:
  discovery:
    enabled: true
    server-addr: localhost:8848
```

或者使用 profile：

```yaml
spring:
  profiles:
    active: nacos
```

#### 4. 重启服务

```bash
# 重启后端
# 重启网关
```

---

### 从 Nacos 切换回固定路由

#### 1. 修改网关配置

```yaml
nacos:
  discovery:
    enabled: false

spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: false
      routes:
        - id: yuncode-admin
          uri: http://localhost:8080  # 改回固定地址
          predicates:
            - Path=/api/**
```

或者修改 profile：

```yaml
spring:
  profiles:
    active: dev
```

#### 2. 修改后端配置

```yaml
nacos:
  discovery:
    enabled: false
```

#### 3. 重启服务

---

## 配置说明

### 网关配置

| 配置项 | 说明 | 默认值 |
|--------|------|-------|
| `nacos.discovery.enabled` | 是否启用 Nacos | false |
| `nacos.discovery.server-addr` | Nacos 地址 | localhost:8848 |
| `nacos.discovery.namespace` | Nacos 命名空间 | public |
| `nacos.discovery.group` | Nacos 分组 | YUNCODE_GROUP |
| `gateway.backend.admin-url` | 后端地址（固定路由模式） | http://localhost:8080 |

### 后端配置

| 配置项 | 说明 | 默认值 |
|--------|------|-------|
| `nacos.discovery.enabled` | 是否启用 Nacos | false |
| `nacos.discovery.server-addr` | Nacos 地址 | localhost:8848 |
| `nacos.discovery.namespace` | Nacos 命名空间 | public |
| `nacos.discovery.group` | Nacos 分组 | YUNCODE_GROUP |

---

## 访问地址

| 服务 | 地址 |
|------|------|
| 网关 | http://localhost:9000 |
| 后端 | http://localhost:8080 |
| Nacos 控制台 | http://localhost:8848/nacos (启用时) |
| 网关状态 | http://localhost:9000/gateway/status |
| 网关路由 | http://localhost:9000/gateway/routes |

---

## 前端配置

修改前端 API 地址为网关地址：

```javascript
const API_BASE_URL = 'http://localhost:9000/api';
```

---

## 网关管理接口

### 获取网关状态

```bash
GET /gateway/status
```

响应示例：

```json
{
  "success": true,
  "status": "UP",
  "service": "yuncode-gateway",
  "port": 9000,
  "backendUrl": "http://localhost:8080",
  "nacosEnabled": false,
  "mode": "Fixed Routing"
}
```

### 获取路由列表

```bash
GET /gateway/routes
```

### 发布测试事件

```bash
POST /gateway/event/test
Content-Type: application/json

{
  "eventType": "app.create",
  "source": "gateway",
  "data": {
    "appId": "com.yuncode.demo",
    "appName": "Demo App"
  }
}
```

---

## 过滤器

### RequestLogFilter

记录所有请求和响应信息。

### AuthenticationFilter

简单的 Token 验证和白名单处理，详细认证由后端服务处理。

---

## 事件系统

事件系统已集成到网关中，使用方式参考 `事件系统使用说明.md`。

---

## 未来扩展（可选）

- [ ] 动态路由配置
- [ ] 限流熔断
- [ ] 灰度发布
- [ ] 统一配置中心
- [ ] 服务监控面板
