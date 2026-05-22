# Yuncode 低代码平台 - 网关方案设计

## 一、方案概述

### 1.1 项目背景
当前 Yuncode 低代码平台采用单体架构，为了满足未来的可扩展性、统一入口管理、事件统一处理等需求，需要引入统一网关层。

### 1.2 设计目标
- ✅ 统一入口：所有外部请求通过网关进入
- ✅ 灵活部署：开发环境嵌入式 Nacos，生产环境可切换独立部署
- ✅ 可扩展：预留微服务架构演进路径
- ✅ 事件驱动：系统所有事件通过网关统一处理

---

## 二、架构设计

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端应用层                                │
│  (yuncode-pure-admin / 自定义应用)                              │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    yuncode-gateway (网关层)                      │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Port: 9000                                              │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │  1. 路由转发                   ← 根据路径转发             │  │
│  │  2. 统一认证                   ← Token 校验、租户解析     │  │
│  │  3. 限流熔断                   ← 防止系统过载             │  │
│  │  4. 日志审计                   ← 记录所有请求             │  │
│  │  5. 事件处理                   ← 事件总线                 │  │
│  │  6. 嵌入式 Nacos (可选)        ← 开发环境无需单独部署     │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                             │
         ┌───────────────────┼───────────────────┐
         ▼                   ▼                   ▼
┌────────────────┐  ┌────────────────┐  ┌────────────────┐
│ yuncode-admin  │  │   应用1        │  │   应用2        │
│ Port: 8080     │  │ (动态加载)     │  │ (动态加载)     │
│ (注册到 Nacos) │  │ (注册到 Nacos) │  │ (注册到 Nacos) │
└────────────────┘  └────────────────┘  └────────────────┘
```

### 2.2 部署模式

#### 开发环境模式
```
┌─────────────────────────────────────┐
│  yuncode-gateway                    │
│  ┌───────────────────────────────┐  │
│  │  网关服务 (Port: 9000)        │  │
│  ├───────────────────────────────┤  │
│  │  嵌入式 Nacos (Port: 8848)    │  │
│  └───────────────────────────────┘  │
│  ↓ 管理                           │
│  ┌───────────────────────────────┐  │
│  │  yuncode-admin (Port: 8080)   │  │
│  │  (自动注册到嵌入式 Nacos)     │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

#### 生产环境模式
```
┌─────────────────────────────────────────────────┐
│           独立 Nacos 集群 (高可用)               │
│           Port: 8848                            │
└──────────────────────┬──────────────────────────┘
                       │
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ yuncode-     │ │ 应用服务1    │ │ 应用服务2    │
│ gateway      │ │             │ │             │
│ Port: 9000   │ │ Port: xxxx  │ │ Port: xxxx  │
└──────────────┘ └──────────────┘ └──────────────┘
```

---

## 三、功能模块设计

### 3.1 核心功能清单

| 功能模块 | 描述 | 优先级 |
|---------|------|-------|
| **路由转发** | 基于路径/断言的智能路由 | P0 |
| **统一认证** | Token 校验、租户解析、权限验证 | P0 |
| **服务注册发现** | 基于 Nacos 的服务管理 | P0 |
| **日志审计** | 请求/响应全链路日志记录 | P1 |
| **限流熔断** | 防止系统过载，保护下游服务 | P1 |
| **事件总线** | 系统事件统一处理和分发 | P1 |
| **灰度发布** | 支持应用平滑升级、A/B 测试 | P2 |

### 3.2 网关路由规则

```
路由规则示例：

/api/auth/**    → yuncode-auth (8080)
/api/system/**  → yuncode-admin (8080)
/api/tenant/**  → yuncode-admin (8080)
/api/apps/{appId}/**  → 动态路由到对应应用服务
```

### 3.3 统一认证流程

```
                    ┌─────────────────┐
                    │  前端发起请求   │
                    │  带 Token      │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │ 网关过滤器      │
                    └────────┬────────┘
                             │
         ┌───────────────────┼───────────────────┐
         ▼                   ▼                   ▼
  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
  │ Token 校验  │    │ 租户解析    │    │ 权限验证    │
  │ Sa-Token    │    │ 多租户隔离  │    │ 接口权限    │
  └──────┬──────┘    └──────┬──────┘    └──────┬──────┘
         │                   │                   │
         └───────────────────┼───────────────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │  转发到后端服务 │
                    └─────────────────┘
```

### 3.4 事件处理机制

```
系统事件类型：
- 用户登录/登出事件
- 应用创建/部署/卸载事件
- 数据变更事件
- 定时任务执行事件
- 告警事件

事件流程：
1. 各服务产生事件 → 2. 发送到网关事件总线 → 3. 事件分发到消费者
```

---

## 四、技术选型

### 4.1 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Cloud Gateway | 4.x | 网关框架 |
| Spring Cloud Alibaba Nacos | 2022.x | 服务注册发现/配置中心 |
| Spring Boot | 3.1.8 | 与项目保持一致 |
| Sa-Token | 1.38.0 | 统一认证 |
| Redis | - | 限流/缓存 |
| MySQL | - | Nacos 数据存储（可选） |

### 4.2 模块结构

```
yuncode-lowcode-boot/
├── yuncode-gateway/              # 新增：网关模块
│   ├── src/main/java/com/yuncode/gateway/
│   │   ├── YuncodeGatewayApplication.java    # 启动类
│   │   ├── config/
│   │   │   ├── GatewayConfig.java             # 网关配置
│   │   │   ├── NacosEmbeddedConfig.java       # 嵌入式 Nacos 配置
│   │   │   └── ServiceRegistryConfig.java     # 服务注册配置
│   │   ├── filter/
│   │   │   ├── AuthenticationFilter.java      # 认证过滤器
│   │   │   ├── TenantFilter.java              # 租户过滤器
│   │   │   ├── RequestLogFilter.java          # 日志过滤器
│   │   │   └── RateLimitFilter.java           # 限流过滤器
│   │   ├── handler/
│   │   │   ├── GlobalExceptionHandler.java    # 全局异常处理
│   │   │   └── JsonExceptionHandler.java      # JSON 异常处理
│   │   ├── event/
│   │   │   ├── GatewayEvent.java              # 事件基类
│   │   │   ├── EventPublisher.java            # 事件发布器
│   │   │   └── EventConsumer.java             # 事件消费者
│   │   └── route/
│   │       └── DynamicRouteLocator.java       # 动态路由定位器
│   ├── src/main/resources/
│   │   ├── application.yml                    # 配置文件
│   │   ├── application-dev.yml                # 开发环境
│   │   └── application-prod.yml               # 生产环境
│   └── pom.xml
│
├── yuncode-common/                  # 现有：通用模块
│   └── src/main/java/com/yuncode/common/
│       └── registry/               # 新增：服务注册接口
│           ├── ServiceRegistry.java
│           ├── ServiceInstance.java
│           └── NacosServiceRegistry.java
│
├── yuncode-auth/                    # 现有：认证模块
├── yuncode-system/                  # 现有：系统模块
├── yuncode-tenant/                  # 现有：租户模块
├── yuncode-admin/                   # 现有：管理模块（需要添加服务注册）
│   └── src/main/java/
│       └── com/yuncode/admin/config/
│           └── ServiceRegistryAutoConfiguration.java  # 新增
│
└── pom.xml
```

---

## 五、配置说明

### 5.1 网关配置 (application.yml)

```yaml
server:
  port: 9000

spring:
  application:
    name: yuncode-gateway
  profiles:
    active: dev

# Nacos 配置
nacos:
  embedded:
    enabled: true              # 是否启用嵌入式 Nacos（开发环境）
    port: 8848                # 嵌入式 Nacos 端口
  server-addr: localhost:8848 # Nacos 服务器地址
  discovery:
    enabled: true
    namespace: public
    group: YUNCODE_GROUP
  config:
    enabled: false            # 暂不启用配置中心

# Spring Cloud Gateway 配置
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true              # 开启从注册中心动态创建路由
          lower-case-service-id: true

      routes:
        # 管理后台路由
        - id: yuncode-admin
          uri: lb://yuncode-admin
          predicates:
            - Path=/api/admin/**,/api/system/**,/api/tenant/**,/api/auth/**
          filters:
            - StripPrefix=0

        # 应用动态路由
        - id: apps-dynamic
          uri: lb://yuncode-apps
          predicates:
            - Path=/api/apps/{appId}/**
          filters:
            - StripPrefix=0

# 限流配置
spring:
  cloud:
    gateway:
      redis-rate-limiter:
        replenishRate: 100    # 每秒允许请求数
        burstCapacity: 200     # 令牌桶容量
        requestedTokens: 1
```

### 5.2 开发环境 (application-dev.yml)

```yaml
nacos:
  embedded:
    enabled: true
    port: 8848
  server-addr: localhost:8848
```

### 5.3 生产环境 (application-prod.yml)

```yaml
nacos:
  embedded:
    enabled: false
  server-addr: nacos1:8848,nacos2:8848,nacos3:8848  # Nacos 集群地址
```

---

## 六、演进路线

### 阶段一：基础网关（当前）
- [ ] 创建 yuncode-gateway 模块
- [ ] 集成嵌入式 Nacos
- [ ] 实现基础路由转发
- [ ] 实现统一认证过滤器
- [ ] 集成 Sa-Token
- [ ] 实现日志审计

### 阶段二：服务注册发现
- [ ] yuncode-admin 注册到 Nacos
- [ ] 实现动态路由
- [ ] 实现限流熔断

### 阶段三：事件总线
- [ ] 实现事件发布机制
- [ ] 实现事件消费机制
- [ ] 事件持久化

### 阶段四：高级功能（可选）
- [ ] 灰度发布
- [ ] 统一配置中心
- [ ] 服务监控面板

---

## 七、实施建议

### 7.1 开发环境
1. 启动 yuncode-gateway（包含嵌入式 Nacos）
2. 启动 yuncode-admin（自动注册到 Nacos）
3. 前端请求访问网关 9000 端口

### 7.2 生产环境
1. 部署独立的 Nacos 集群（至少 3 节点）
2. 配置网关连接独立 Nacos
3. 部署各服务，注册到 Nacos

---

## 八、关键接口设计

### 8.1 服务注册接口

```java
public interface ServiceRegistry {

    /**
     * 注册服务
     */
    void register(ServiceInstance instance);

    /**
     * 注销服务
     */
    void deregister(String serviceId);

    /**
     * 发现服务
     */
    List<ServiceInstance> discover(String serviceName);

    /**
     * 获取所有服务实例
     */
    List<ServiceInstance> getAllInstances();
}
```

### 8.2 网关事件接口

```java
public abstract class GatewayEvent {
    private String eventId;
    private String eventType;
    private String source;
    private LocalDateTime timestamp;
    private Map<String, Object> data;
}

public interface EventPublisher {
    void publish(GatewayEvent event);
}

public interface EventConsumer {
    void onEvent(GatewayEvent event);
}
```

---

## 九、总结

本方案的核心优势：

1. **零额外部署**：开发环境使用嵌入式 Nacos，无需中间件
2. **平滑演进**：从单体到微服务的过渡路径清晰
3. **灵活切换**：通过配置即可在嵌入式/独立 Nacos 之间切换
4. **功能完整**：涵盖路由、认证、限流、事件等核心功能
