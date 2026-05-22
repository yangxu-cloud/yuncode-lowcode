# 事件系统使用说明

## 概述

系统所有事件通过 `SimpleEventBus` 统一处理。

## 架构设计

```
┌──────────────────────────────────────────┐
│         SimpleEventBus (事件总线)         │
│  • 发布事件 (publish/publishAsync)        │
│  • 注册消费者 (registerConsumer)          │
└──────────────┬───────────────────────────┘
               │
       ┌───────┴────────┐
       │                │
       ▼                ▼
┌──────────────┐  ┌─────────────┐
│  同步消费者  │  │ 异步消费者  │
└──────────────┘  └─────────────┘
```

## 快速开始

### 1. 注入事件总线

```java
@Autowired
private SimpleEventBus eventBus;
```

### 2. 发布事件

```java
// 方式 1：构建完整事件对象
GatewayEvent event = GatewayEvent.builder()
    .eventType(EventTypes.USER_LOGIN)
    .source("auth-service")
    .build();
event.addData("userId", "1001");
event.addData("username", "test-user");
eventBus.publish(event);

// 方式 2：便捷方法
Map<String, Object> data = new HashMap<>();
data.put("userId", "1001");
data.put("username", "test-user");
eventBus.publish(EventTypes.USER_LOGIN, "auth-service", data);

// 方式 3：异步发布
eventBus.publishAsync(EventTypes.USER_LOGIN, "auth-service", data);
```

### 3. 注册消费者

```java
// Lambda 方式
eventBus.registerConsumer(EventTypes.USER_LOGIN, event -> {
    String userId = event.getData("userId");
    String username = event.getData("username");
    log.info("User logged in: {} ({})", username, userId);
});

// 实现接口方式
eventBus.registerConsumer(EventTypes.USER_LOGIN, new EventConsumer() {
    @Override
    public void onEvent(GatewayEvent event) {
        // 处理事件
    }
});

// 通配符方式：监听所有事件
eventBus.registerConsumer("*", event -> {
    log.info("Received event: {}", event.getEventType());
});
```

## 事件类型

### 用户相关

| 事件类型 | 说明 |
|---------|------|
| `user.login` | 用户登录 |
| `user.logout` | 用户登出 |
| `user.register` | 用户注册 |

### 应用相关

| 事件类型 | 说明 |
|---------|------|
| `app.create` | 应用创建 |
| `app.update` | 应用更新 |
| `app.delete` | 应用删除 |
| `app.deploy` | 应用部署 |
| `app.undeploy` | 应用卸载 |
| `app.start` | 应用启动 |
| `app.stop` | 应用停止 |

### 系统相关

| 事件类型 | 说明 |
|---------|------|
| `system.startup` | 系统启动 |
| `system.shutdown` | 系统关闭 |
| `system.alert` | 系统告警 |

### 定时任务相关

| 事件类型 | 说明 |
|---------|------|
| `job.execute` | 任务执行 |
| `job.success` | 任务成功 |
| `job.fail` | 任务失败 |

## API 接口

### 发布测试事件

```bash
POST /api/event/test
Content-Type: application/json

{
  "eventType": "app.create",
  "source": "test",
  "data": {
    "appId": "com.yuncode.demo",
    "appName": "Demo App"
  }
}
```

### 演示登录事件

```bash
POST /api/event/demo/login?userId=1001&username=test-user
```

### 演示应用创建事件

```bash
POST /api/event/demo/app?appId=com.yuncode.demo&appName=Demo%20App&creator=admin
```

### 获取事件类型列表

```bash
GET /api/event/types
```

## 使用示例

### 示例 1：在登录时发布事件

```java
@Service
public class AuthService {

    @Autowired
    private SimpleEventBus eventBus;

    public LoginResult login(String username, String password) {
        // 登录逻辑...
        User user = validateUser(username, password);

        // 发布登录事件
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("loginTime", System.currentTimeMillis());

        eventBus.publish(EventTypes.USER_LOGIN, "auth-service", data);

        return new LoginResult(user);
    }
}
```

### 示例 2：监听应用创建事件

```java
@Component
public class AppCreateListener {

    @Autowired
    private SimpleEventBus eventBus;

    @Autowired
    private ApplicationDirectoryService appDirService;

    @PostConstruct
    public void init() {
        // 注册应用创建事件消费者
        eventBus.registerConsumer(EventTypes.APP_CREATE, event -> {
            String appId = event.getData("appId");
            String appName = event.getData("appName");
            String creator = event.getData("creator");

            log.info("Received app create event: {} ({})", appName, appId);

            // 自动创建应用目录
            try {
                String path = appDirService.createApplicationDirectory(
                    appId, appName, null, null, null
                );
                log.info("Created app directory: {}", path);
            } catch (Exception e) {
                log.error("Failed to create app directory", e);
            }
        });
    }
}
```

### 示例 3：审计日志消费者

```java
@Component
public class AuditLogListener {

    @Autowired
    private SimpleEventBus eventBus;

    @PostConstruct
    public void init() {
        // 监听所有事件，记录审计日志
        eventBus.registerConsumer("*", event -> {
            log.info("[Audit] [{}] [{}] from: {}",
                event.getTimestamp(), event.getEventType(), event.getSource());
            log.info("[Audit] Data: {}", event.getData());

            // 保存到数据库或发送到其他系统
            // auditLogService.save(event);
        });
    }
}
```

## 高级功能

### 异步执行

```java
// 使用 publishAsync 异步发布事件
eventBus.publishAsync(EventTypes.APP_DEPLOY, "app-service", data);
// 事件会在后台线程处理，不会阻塞主线程
```

### 事件数据操作

```java
// 添加数据
event.addData("key1", "value1");
event.addData("key2", 123);

// 获取数据（支持泛型）
String value = event.getData("key1");
Integer number = event.getData("key2");

// 获取完整数据
Map<String, Object> allData = event.getData();
```

## 完整示例

参考 `EventBusDemo` 类中的完整使用示例。

## 注意事项

1. 消费者处理异常不会影响事件发布
2. 异步消费者使用线程池，注意线程安全
3. 事件数据应尽量简单，避免传输大对象
4. 建议所有事件通过事件总线发布，便于统一管理

## 未来扩展

- [ ] 事件持久化（存储到数据库）
- [ ] 事件回放机制
- [ ] 事件订阅管理（支持动态订阅/取消）
- [ ] 事件优先级
- [ ] 分布式事件支持（Redis/消息队列）
