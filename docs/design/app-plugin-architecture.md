# Yuncode LowCode App 插件系统架构设计

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2026-05-20 | 初稿，定义双模式插件架构与第一阶段实施 |

---

## 一、背景与目标

### 1.1 问题

- 用户创建的应用（如 qms0805）位于 `apps/install/` 目录下
- 应用有自己的 `pom.xml`，但不在父 pom 的 `<modules>` 中，Maven 不识别
- 开发环境需要源码级集成，生产环境需要 JAR 动态加载
- 缺乏统一的 App SPI 接口和生命周期管理

### 1.2 目标

1. **Dev 模式**：App 作为 Maven 子模块，Spring 直接扫描，开发体验流畅
2. **Prod 模式**：App 打包为独立 JAR，通过独立 ClassLoader 动态加载
3. **统一 SPI**：定义 `YuncodeApp` 接口，所有 App 遵循相同生命周期
4. **隔离性**：App 之间互不干扰，一个 App 崩溃不影响平台

---

## 二、总体架构

```
┌──────────────────────────────────────────────────────────┐
│                   Yuncode LowCode Platform                 │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │                App Loader Layer                   │   │
│  │  ┌─────────────────┐  ┌──────────────────────┐   │   │
│  │  │  DevAppLoader    │  │  ProdAppLoader       │   │   │
│  │  │  - Maven模块     │  │  - JAR 动态加载      │   │   │
│  │  │  - Spring 扫描   │  │  - 独立 ClassLoader  │   │   │
│  │  │  - 直接注入      │  │  - SPI 发现          │   │   │
│  │  └────────┬────────┘  └──────────┬───────────┘   │   │
│  │           │                      │               │   │
│  │           └──────┬───────────────┘               │   │
│  │                  ▼                               │   │
│  │         ┌────────────────┐                       │   │
│  │         │  AppRegistry   │                       │   │
│  │         │  (运行时注册表) │                       │   │
│  │         └────────────────┘                       │   │
│  └──────────────────────────────────────────────────┘   │
│                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  App A        │  │  App B       │  │  App C       │  │
│  │  (qms0805)   │  │  (其他应用)  │  │  (其他应用)  │  │
│  │  YuncodeApp  │  │  YuncodeApp  │  │  YuncodeApp  │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │               Platform Core                       │   │
│  │  yuncode-common │ yuncode-system │ yuncode-auth   │   │
│  └──────────────────────────────────────────────────┘   │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

### 2.1 分层说明

| 层级 | 职责 |
|------|------|
| **Platform Core** | yuncode-common（SPI接口）、yuncode-system（应用管理）、yuncode-auth（权限）|
| **App Loader Layer** | 负责加载 App，Dev 模式走 Maven，Prod 模式走 ClassLoader |
| **App Registry** | 运行时注册表，管理所有已加载 App 的实例和元数据 |
| **App Layer** | 具体业务 App，实现 `YuncodeApp` 接口 |

---

## 三、双模式加载策略

### 3.1 Dev 模式

适用于本地开发、调试阶段。

```
┌──────────────────────────────────────────────┐
│                  Dev 模式                      │
│                                               │
│  pom.xml (parent)                             │
│    └── <module>apps/install/qms0805</module>  │
│                                               │
│  yuncode-admin/pom.xml                        │
│    └── <dependency>qms0805</dependency>       │
│                                               │
│  Spring 启动 → 扫描包路径 → 注册 Bean        │
│  支持热重载（spring-boot-devtools/JRebel）    │
└──────────────────────────────────────────────┘
```

**优点**：
- 开发简单，IDE 直接识别
- Spring 全功能支持（AOP、事务、事件）
- 热重载支持

**缺点**：
- App 需要随主项目一起发布
- 无法热插拔

### 3.2 Prod 模式

适用于生产环境。

```
┌──────────────────────────────────────────────┐
│                  Prod 模式                     │
│                                               │
│  apps/lib/                                    │
│    ├── qms0805-1.0.0.jar                     │
│    └── qms0805-1.0.1.jar  (热替换)           │
│                                               │
│  平台启动 → AppLoader 扫描 apps/lib/          │
│    → 创建独立 ClassLoader                    │
│    → SPI 加载 YuncodeApp 实现                │
│    → 调用 onStartup(ctx)                     │
│    → 注册到 AppRegistry                      │
└──────────────────────────────────────────────┘
```

**优点**：
- 独立部署，不依赖主项目
- 热替换，无需重启
- ClassLoader 隔离

**缺点**：
- 无法使用部分 Spring 功能
- 需要自行管理 Controller 注册

---

## 四、SPI 接口定义

### 4.1 YuncodeApp — App 插件核心接口

```java
package com.yuncode.common.app;

/**
 * App 插件核心 SPI 接口。
 * 所有动态应用必须实现此接口。
 *
 * 平台通过 ServiceLoader 或 Spring 扫描发现实现类，
 * 按照生命周期方法管理 App 的启动和停止。
 */
public interface YuncodeApp {

    /** 应用唯一标识（如 qms0805） */
    String getAppId();

    /** 应用展示名称 */
    String getAppName();

    /** 应用版本 */
    String getVersion();

    /** 安装时调用 */
    default void onInstall(AppContext ctx) {}

    /** 卸载时调用 */
    default void onUninstall(AppContext ctx) {}

    /** 启动时调用 */
    default void onStartup(AppContext ctx) {}

    /** 停止时调用 */
    default void onShutdown(AppContext ctx) {}
}
```

### 4.2 AppContext — 运行时上下文

```java
package com.yuncode.common.app;

import java.nio.file.Path;
import java.util.Properties;

/**
 * App 运行上下文。提供平台能力给 App 使用。
 */
public interface AppContext {

    /** 获取应用数据目录（apps/install/{appId}/） */
    Path getAppDirectory();

    /** 获取应用 lib 目录（apps/install/{appId}/lib/） */
    Path getLibDirectory();

    /** 获取应用静态资源目录（apps/install/{appId}/web/） */
    Path getWebDirectory();

    /** 获取应用配置（来自 manifest.xml） */
    AppManifest getManifest();

    /** 获取自定义属性 */
    Properties getProperties();

    /** 注册 Controller 实例（Prod 模式使用） */
    void registerController(Object controller);

    /** 注册 Service 实例（Prod 模式使用） */
    void registerService(Object service);

    /** 获取事件发布器 */
    EventPublisher getEventPublisher();

    /** 获取租户 ID */
    Long getTenantId();
}
```

### 4.3 AppManifest — 应用元数据模型

```java
package com.yuncode.common.app;

import java.time.LocalDateTime;

/**
 * App 发布清单元数据（对应 manifest.xml）。
 */
public class AppManifest {

    private String name;
    private String version;
    private Integer buildNo;
    private String developer;
    private Boolean reloadable;
    private Boolean allowStartup;
    private String depends;
    private LocalDateTime installDate;
    private LocalDateTime releaseDate;

    // getters & setters...
}
```

### 4.4 AppStatus — 应用状态枚举

```java
package com.yuncode.common.app;

/**
 * App 运行时状态。
 */
public enum AppStatus {
    INSTALLED(0, "已安装"),
    RUNNING(1, "运行中"),
    STOPPED(2, "已停止"),
    ERROR(3, "异常");

    private final int code;
    private final String description;

    // constructor & getters...
}
```

---

## 五、类加载隔离机制

### 5.1 ClassLoader 层次结构

```
                    ┌──────────────────────────┐
                    │   Bootstrap ClassLoader   │
                    │   (JVM 核心类)            │
                    └──────────┬───────────────┘
                               │
                    ┌──────────▼───────────────┐
                    │   Platform ClassLoader    │
                    │   (yuncode-admin 类路径)  │
                    │   - yuncode-common       │
                    │   - yuncode-system       │
                    │   - Spring Boot          │
                    │   - 所有第三方依赖        │
                    └──────────┬───────────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
    ┌─────────▼──────┐  ┌─────▼───────┐  ┌─────▼───────┐
    │ AppClassLoader │  │ AppCL       │  │ AppCL       │
    │ (qms0805)     │  │ (App B)    │  │ (App C)    │
    │ parent-first  │  │ parent-first│  │ parent-first │
    └────────────────┘  └─────────────┘  └─────────────┘
```

### 5.2 加载规则

1. **parent-first 委托**：AppClassLoader 先委托 Platform CL 加载类，加载不到再从自己的 JAR 加载
2. **共享类**：yuncode-common 中的接口和类由 Platform CL 加载，App 和平台共享
3. **隔离类**：每个 App 的自身实现类由各自的 AppClassLoader 加载，互不可见
4. **资源隔离**：每个 App 的配置文件、静态资源互不干扰

### 5.3 AppClassLoader 实现要点

```java
public class AppClassLoader extends URLClassLoader {
    
    public AppClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) 
            throws ClassNotFoundException {
        // parent-first 委托
        // 但 yuncode.common.app 包由 Platform CL 加载
        // App 自身类由本加载器加载
        return super.loadClass(name, resolve);
    }
}
```

---

## 六、热部署机制（第三阶段）

### 6.1 文件监听

```
FileWatcher (apps/lib/ 目录)
    ↓ 检测到 JAR 变化
    ├── 新增 JAR → 加载新 App
    ├── 更新 JAR → 热替换 App
    └── 删除 JAR → 卸载 App
```

### 6.2 热替换流程

```
1. FileWatcher 检测到 qms0805-1.0.1.jar
2. 创建新 AppClassLoader (newCL)
3. newCL 加载新版 YuncodeApp 实现类
4. 调用 newApp.onStartup(ctx)
5. 注册新 Controller/Service
6. 调用 oldApp.onShutdown(ctx)
7. 注销旧 Controller/Service
8. AppRegistry 替换为新实例
9. 旧 ClassLoader 无引用后 GC
```

### 6.3 注意事项

- **状态迁移**：App 需要自行处理业务状态迁移
- **会话保持**：正在处理的请求完成后才切换
- **版本回滚**：保留上一版 JAR，支持快速回退

---

## 七、App 通信机制

### 7.1 通信方式

| 方式 | 适用场景 | 说明 |
|------|---------|------|
| **EventBus** | 跨 App 通信 | 发布/订阅模式，松耦合 |
| **AppContext API** | 访问平台能力 | 通过接口调用，不依赖实现 |
| **REST API** | 外部调用 | 通过 Gateway 路由到 App |

### 7.2 EventBus 集成

平台已有 `EventPublisher` / `EventConsumer` 接口和 `SimpleEventBus` 实现，
App 可通过 `AppContext.getEventPublisher()` 获取：

```java
// App A 发布事件
ctx.getEventPublisher().publish(
    GatewayEvent.builder()
        .eventType("app.qms0805.weight.overlimit")
        .source("qms0805")
        .data(Map.of("orderId", "123"))
        .build()
);

// App B 监听事件（或平台模块监听）
```

---

## 八、应用目录规范

### 8.1 目录结构

```
apps/
├── install/                         # 已安装应用
│   └── {appId}/                     # 应用 ID（如 com.yuncode.user.apps.qms0805）
│       ├── pom.xml                  # Maven 构建文件
│       ├── manifest.xml             # 发布清单
│       ├── icon.png                 # 应用图标
│       ├── lib/                     # 私有依赖 JAR
│       ├── repository/              # 数据库表、表单、台账定义
│       ├── template/                # 扩展 Controller 页面模板
│       └── web/                     # 静态资源（HTML/CSS/JS）
├── uninstall/                       # 已卸载应用归档
└── history/                         # 历史版本归档
```

### 8.2 Dev 模式类路径

```
YuncodeApp 实现类:
  apps/install/{appId}/src/main/java/com/yuncode/user/apps/{appId}/xxx.java

Spring 扫描:
  yuncode-admin 通过 @SpringBootApplication scanBasePackages
  配置扫描 com.yuncode.user.apps 包路径
```

---

## 九、实施方案

### 9.1 第一阶段（当前）

| 任务 | 说明 |
|------|------|
| SPI 接口定义 | YuncodeApp、AppContext、AppManifest、AppStatus |
| Maven 集成 | App 作为子模块加入父 pom |
| App 骨架 | qms0805 实现 YuncodeApp，包含示例代码 |
| 验证编译 | Dev 模式构建成功 |

### 9.2 第二阶段

| 任务 | 说明 |
|------|------|
| AppClassLoader | 实现独立 ClassLoader，支持 JAR 加载 |
| ProdAppLoader | 扫描 apps/lib/，SPI 发现 YuncodeApp |
| AppRegistry | 运行时注册表，管理 App 实例 |
| App 打包 | Maven 配置打包为可分发的 JAR |

### 9.3 第三阶段

| 任务 | 说明 |
|------|------|
| FileWatcher | 监听 JAR 变化，自动加载/卸载 |
| 热替换 | 无缝切换 App 版本 |
| 管理界面 | 前端可视化 App 管理 |
| 监控 | App 运行状态监控与告警 |

---

## 十、设计决策记录

| 决策 | 选项 | 选择理由 |
|------|------|---------|
| 接口语言 | Java Interface + default | 兼容性好，Prod 模式通过 SPI 加载 |
| ClassLoader 策略 | parent-first | 优先共享平台类，减少冲突 |
| App 通信 | EventBus | 已有实现，松耦合 |
| 配置格式 | XML (manifest.xml) | 项目已有定义，兼容现有数据 |
| 包扫描 | component-scan + SPI | Dev 用扫描，Prod 用 SPI，互不干扰 |
