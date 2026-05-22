# App 热插拔原理

## 一、整体架构

```
AppWatcher (接口，可扩展)
  └─ JarFileWatcher (WatchService 实现)
       │ 监听 lib/*.jar + 新增 App 目录
       │ 500ms 去抖
       ▼ Consumer<String appId>
HotAppDeployer.onAppChanged(appId)
  └─ installApp(appDir)  ← 遍历 lib/*.jar，全部加载
  └─ uninstallApp(appId) ← 清理所有 Bean + 映射 + ClassLoader
```

三层职责分离：
- **AppWatcher** — 只负责"检测到变更"，不管怎么加载
- **HotAppDeployer** — 只负责"加载/卸载 Bean"，不管怎么检测
- 以后加数据库监听、页面配置监听时，只需新写一个 `implements AppWatcher`，HotAppDeployer 不用改

---

## 二、文件监听原理（WatchService）

### 2.1 从轮询到事件驱动

旧方案 10 秒轮询一次，盲等。新方案用 JDK 内置的 `WatchService`，操作系统文件系统有变动时直接通知 JVM，不需要自己轮询。

### 2.2 核心流程

```
1. 创建 WatchService
       │ FileSystems.getDefault().newWatchService()
       │ ▼
       Linux: inotify_init()       Windows: ReadDirectoryChangesW
       │                             异步 I/O 等待目录变更通知
       ▼
2. 注册要监听的目录
       │ libDir.register(watchService,
       │     ENTRY_CREATE,   // 文件被创建
       │     ENTRY_MODIFY,   // 文件被修改
       │     ENTRY_DELETE)   // 文件被删除
       │
       │ register() 告诉操作系统"我要监视这个目录"
       │ 返回 WatchKey —— 它是后续拿事件时的"凭证"
       ▼
3. 事件循环
       │ while (running) {
       │     WatchKey key = watchService.poll(60, SECONDS);  // 阻塞等待
       │     for (WatchEvent<?> event : key.pollEvents()) {  // 拉取事件
       │         Path filename = (Path) event.context();      // 哪个文件
       │         WatchEvent.Kind<?> kind = event.kind();      // 创建/修改/删除
       │         // 处理...
       │     }
       │     key.reset();  // 必须重置，否则后续事件收不到
       │ }
```

### 2.3 两种监听

```
安装目录本身
  注册事件：ENTRY_CREATE, ENTRY_DELETE
  作用：检测新增/删除了 App 子目录
  处理：新目录出现时注册它的 lib/

每个 App 的 lib/ 目录
  注册事件：ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE
  作用：检测 JAR 文件的增删改
  处理：只关心 *.jar 的变化
```

### 2.4 去抖（debounce）

Windows 上保存一个文件，文件系统可能触发 2~3 次 `ENTRY_MODIFY`。如果不处理，一个 JAR 落地会触发多次热重载。

去抖原理是一个"延迟取消"开关：

```
时间线：
  0ms  ── 事件1到达 ──→ 安排 500ms 后通知
100ms ── 事件2到达 ──→ 取消上一次安排，重新安排 500ms 后通知
200ms ── 事件3到达 ──→ 取消上一次安排，重新安排 500ms 后通知
700ms ── 安静了 500ms ──→ 真正触发通知（只触发一次！）
```

用 `ScheduledExecutorService` 延迟执行 + 每次新事件取消上一次的延迟任务。

---

## 三、JAR 扫描与 Bean 注册原理

### 3.1 扫描 JAR 中的类

```java
try (JarFile jar = new JarFile(jarFile)) {
    Enumeration<JarEntry> entries = jar.entries();
    while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        String name = entry.getName();
        // "com/yuncode/.../InterfaceQms0205.class"
        if (!name.endsWith(".class")) continue;

        String className = name.replace('/', '.').replace(".class", "");
        // "com.yuncode.user.apps.qms0205.InterfaceQms0205"

        Class<?> clazz = Class.forName(className, false, classLoader);
        //                                      ^^^^^ 不执行 static{} 块

        if (clazz.isAnnotationPresent(RestController.class) ||
            clazz.isAnnotationPresent(Service.class) ||
            clazz.isAnnotationPresent(Component.class)) {
            beanClasses.add(clazz);
        }
    }
}
```

- `JarFile` 本质是 `ZipFile`，遍历所有条目
- 过滤出 `.class` 文件，用 `Class.forName(..., false, classLoader)` 加载（不执行静态初始化）
- `isAnnotationPresent(RestController.class)` 查元数据判断是否 Spring Bean

### 3.2 ChildFirst 类加载

```java
URLClassLoader cl = new ChildFirstURLClassLoader(
    new URL[]{jar.toURI().toURL()},     // JAR 的 URL
    getClass().getClassLoader()         // 平台 ClassLoader 作为父加载器
);
```

标准 Java 是 parent-first（先问父加载器，找不到才自己找），但热插拔场景需要反过来：

```
标准 parent-first:
  平台有旧版 InterfaceQms0205.class（Maven 依赖）
  JAR 里有新版 InterfaceQms0205.class
  → ClassLoader 先问父加载器 → 找到旧版 → 返回旧版  ✗

child-first:
  → 先从自己的 JAR 找 → 找到新版 → 返回新版  ✓
```

`ChildFirstURLClassLoader` 重写了 `loadClass()`：

```java
protected Class<?> loadClass(String name) {
    if (name.startsWith("java.")) return super.loadClass(name);  // JDK 核心类
  
    Class<?> loaded = findLoadedClass(name);
    if (loaded != null) return loaded;

    try {
        return findClass(name);               // 优先从 JAR 找
    } catch (ClassNotFoundException e) {
        return super.loadClass(name);         // 找不到再委托父加载器
    }
}
```

### 3.3 注册到 Spring 容器

```java
// 1. 推断 bean 名称（类名首字母小写）
String beanName = "interfaceQms0205";

// 2. 检查是否已存在
if (beanFactory.containsBeanDefinition(beanName)) {
    log.warn("Bean already exists, skipping");
    return;
}

// 3. 构建 Bean 定义
BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(beanClass);
builder.setScope("singleton");
builder.getRawBeanDefinition().setAutowireMode(AUTOWIRE_CONSTRUCTOR);

// 4. 注册到 Spring 容器
beanFactory.registerBeanDefinition(beanName, builder.getBeanDefinition());

// 5. 强制实例化（触发依赖注入）
beanFactory.getBean(beanName);
```

### 3.4 注册 Controller URL 映射

动态注册的 Bean，Spring MVC 的 `RequestMappingHandlerMapping` 不知道它。需要手动注册 URL 映射：

```java
Method detectMethod = AbstractHandlerMethodMapping.class
    .getDeclaredMethod("detectHandlerMethods", Object.class);
detectMethod.setAccessible(true);
detectMethod.invoke(handlerMapping, beanName);
```

`detectHandlerMethods` 扫描 `InterfaceQms0205` 的所有方法，找到 `@GetMapping` 等注解，建立 URL → 方法的映射表。

### 3.5 刷新 API 文档缓存

SpringDoc 内部有三层缓存，热加载后必须全部清理，否则 API 文档仍显示旧的接口列表：

1. `OpenAPIService.cachedOpenAPI` — 缓存的 OpenAPI 对象
2. `OpenAPIService.mappingsMap` — 缓存的 Controller 映射
3. `SpringWebMvcProvider.handlerMethods` — 缓存的 HandlerMethod 映射

---

## 四、卸载原理

卸载比加载更容易出错，因为残留的 Bean 和映射会导致冲突。

```java
void uninstallApp(String appId) {
    // 1. 从 loadedApps 中移除
    LoadedApp app = loadedApps.remove(appId);

    // 2. 清除 Handler 映射（Spring MVC URL 路由）
    for (String beanName : app.beanNames) {
        removeHandlerMappings(beanName);
    }

    // 3. 销毁 Bean 实例，移除 Bean 定义
    for (String beanName : app.beanNames) {
        beanFactory.destroySingleton(beanName);
        beanFactory.removeBeanDefinition(beanName);
    }

    // 4. 关闭 ClassLoader（释放 JAR 文件锁）
    for (URLClassLoader cl : app.classLoaders) {
        cl.close();
    }
}
```

---

## 五、身份模型对比

| 维度 | 旧（JAR 文件名） | 新（App 目录名） |
|---|---|---|
| loadedApps key | `qms0205.jar` 或 `qms0205-1.0.0.jar` | `com.yuncode.user.apps.qms0205` |
| 加载粒度 | 单个 JAR | 整个 App 目录下所有 JAR |
| 卸载 | 按 JAR 名匹配，换个名就漏掉 | 按 App 目录名匹配，彻底清理 |
| 变更检测 | JAR 文件 lastModified 对比 | WatchService 事件通知 |

---

## 六、路径解析

安装目录（`install-dir`）不再依赖 `user.dir`，按优先级尝试多个候选路径，选第一个真正有 App 的：

```
优先级：
  1. 从 HotAppDeployer.class 的类路径反推 yuncode-lowcode-boot/apps/install/
  2. ./apps/install（user.dir = yuncode-lowcode-boot 时正确）
  3. ../apps/install（user.dir = yuncode-admin 时正确）
  4. 配置值（回退）
```

---

## 七、扩展

```java
// 以后加数据库监听时，只需写一个 DbConfigWatcher：
public class DbConfigWatcher implements AppWatcher {
    void start(Path installDir, Consumer<String> onAppChanged) {
        // 用数据库触发器/CDC监听配置表变更
        // 变更时调用 onAppChanged.accept(appId)
    }
    void close() { /* 清理资源 */ }
}

// 加到 HotAppDeployer 只需一行：
this.appWatcher = new JarFileWatcher();     // JAR 监听
// + new DbConfigWatcher(dataSource).start(...);  // 数据库监听
```

不需要改 HotAppDeployer 的加载/卸载逻辑。
