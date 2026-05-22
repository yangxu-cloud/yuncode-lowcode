# Nacos 切换指南

## 概述

网关和后端服务都支持通过配置文件切换是否启用 Nacos 服务发现。

- **前期**：不启用 Nacos，使用固定路由（简单，无需额外中间件）
- **后期**：启用 Nacos，使用服务发现（支持微服务架构）

---

## 一、当前状态（不启用 Nacos）

### 配置说明

网关和后端默认都设置了：

```yaml
nacos:
  discovery:
    enabled: false
```

### 网关路由方式

使用固定地址转发：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: yuncode-admin
          uri: http://localhost:8080  # 固定地址
          predicates:
            - Path=/api/**
```

### 启动方式

```bash
# 终端 1 - 启动后端
cd yuncode-admin
mvn spring-boot:run

# 终端 2 - 启动网关
cd yuncode-gateway
mvn spring-boot:run
```

---

## 二、切换到 Nacos 模式

### 步骤 1：部署 Nacos

#### 下载 Nacos

访问：https://github.com/alibaba/nacos/releases

下载最新版本（如 nacos-server-2.x.x.zip）

#### 启动 Nacos（单机模式）

**Windows：**

```bash
cd nacos/bin
startup.cmd -m standalone
```

**Linux/Mac：**

```bash
cd nacos/bin
sh startup.sh -m standalone
```

#### 验证 Nacos

访问：http://localhost:8848/nacos

默认账号密码：`nacos/nacos`

---

### 步骤 2：配置后端服务

#### 方式 A：修改主配置文件

编辑 `yuncode-admin/src/main/resources/application.yml`：

```yaml
nacos:
  discovery:
    enabled: true
    server-addr: localhost:8848
    namespace: public
    group: YUNCODE_GROUP
```

#### 方式 B：使用 Nacos Profile（推荐）

直接使用现成的配置文件，无需修改：

```bash
cd yuncode-admin
mvn spring-boot:run -Dspring-boot.run.profiles=nacos
```

或在 `application.yml` 中设置：

```yaml
spring:
  profiles:
    active: nacos
```

#### 启动后端

```bash
cd yuncode-admin
mvn spring-boot:run
```

启动后，在 Nacos 控制台的「服务管理」→「服务列表」中可以看到 `yuncode-admin` 已注册。

---

### 步骤 3：配置网关

#### 方式 A：修改主配置文件

编辑 `yuncode-gateway/src/main/resources/application.yml`：

```yaml
nacos:
  discovery:
    enabled: true
    server-addr: localhost:8848
    namespace: public
    group: YUNCODE_GROUP

spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      routes:
        - id: yuncode-admin
          uri: lb://yuncode-admin  # 使用服务名，而非固定地址
          predicates:
            - Path=/api/**
          filters:
            - StripPrefix=0
```

#### 方式 B：使用 Nacos Profile（推荐）

直接使用现成的配置文件：

```bash
cd yuncode-gateway
mvn spring-boot:run -Dspring-boot.run.profiles=nacos
```

或在 `application.yml` 中设置：

```yaml
spring:
  profiles:
    active: nacos
```

#### 启动网关

```bash
cd yuncode-gateway
mvn spring-boot:run
```

---

### 步骤 4：验证

#### 检查网关状态

```bash
curl http://localhost:9000/gateway/status
```

期望响应：

```json
{
  "success": true,
  "status": "UP",
  "nacosEnabled": true,
  "mode": "Nacos Service Discovery"
}
```

#### 检查 Nacos 控制台

访问 http://localhost:8848/nacos

在「服务管理」→「服务列表」中应能看到：

- `yuncode-gateway`
- `yuncode-admin`

#### 测试 API

通过网关访问后端 API 应正常工作：

```bash
curl http://localhost:9000/gateway/routes
```

---

## 三、从 Nacos 切换回固定路由

### 方式 A：修改配置文件

网关和后端都修改为：

```yaml
nacos:
  discovery:
    enabled: false
```

网关还需要改回固定路由：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: yuncode-admin
          uri: http://localhost:8080
          predicates:
            - Path=/api/**
```

### 方式 B：使用 Dev Profile

更简单的方式，直接切换 profile：

```bash
# 后端
cd yuncode-admin
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 网关
cd yuncode-gateway
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## 四、配置文件说明

### 网关模块的配置文件

| 配置文件 | 说明 |
|---------|------|
| `application.yml` | 主配置，默认不启用 Nacos |
| `application-dev.yml` | 开发环境，不启用 Nacos |
| `application-nacos.yml` | Nacos 模式，启用服务发现 |

### 后端模块的配置文件

| 配置文件 | 说明 |
|---------|------|
| `application.yml` | 主配置，默认不启用 Nacos |
| `application-nacos.yml` | Nacos 模式，启用服务发现 |

---

## 五、配置项详解

### Nacos 配置项

| 配置项 | 说明 | 默认值 |
|--------|------|-------|
| `nacos.discovery.enabled` | 是否启用 Nacos | false |
| `nacos.discovery.server-addr` | Nacos 服务器地址 | localhost:8848 |
| `nacos.discovery.namespace` | 命名空间 ID | public |
| `nacos.discovery.group` | 分组名称 | YUNCODE_GROUP |
| `nacos.discovery.heart-beat-interval` | 心跳间隔（毫秒） | 5000 |
| `nacos.discovery.heart-beat-timeout` | 心跳超时（毫秒） | 15000 |
| `nacos.discovery.ip-delete-timeout` | IP 删除超时（毫秒） | 30000 |

### 网关路由配置

| 配置项 | 说明 |
|--------|------|
| `spring.cloud.gateway.discovery.locator.enabled` | 是否从注册中心创建路由 |
| `spring.cloud.gateway.routes[*].uri` | `http://...` = 固定地址<br>`lb://...` = 服务发现 |

---

## 六、生产环境建议

### 1. Nacos 集群部署

生产环境建议部署 Nacos 集群：

```yaml
nacos:
  discovery:
    server-addr: nacos1:8848,nacos2:8848,nacos3:8848
```

### 2. 使用独立的 Profile

生产环境创建 `application-prod.yml`：

```yaml
spring:
  profiles:
    active: prod
```

### 3. Nacos 命名空间隔离

- dev：开发环境
- test：测试环境
- prod：生产环境

---

## 七、常见问题

### Q：启动时提示找不到 Nacos 类？

A：确保 pom.xml 中的 Nacos 依赖不是 `optional` 或者 scope 没有问题。

### Q：启用 Nacos 后网关无法转发请求？

A：检查：

1. 后端服务是否已注册到 Nacos
2. 网关的路由 URI 是否使用 `lb://服务名` 格式
3. 服务名称是否一致（大小写）

### Q：如何查看当前是否启用了 Nacos？

A：访问网关状态接口：

```bash
curl http://localhost:9000/gateway/status
```

查看 `nacosEnabled` 和 `mode` 字段。

---

## 八、总结

| 场景 | 配置 | Profile |
|------|------|---------|
| 前期开发 | 固定路由 | `dev` (默认) |
| 微服务架构 | Nacos 服务发现 | `nacos` |
| 生产环境 | Nacos 集群 | `prod` (自定义) |

切换只需修改配置或 profile，无需代码改动！
