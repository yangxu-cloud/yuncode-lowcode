# 架构优化与代码审查报告

> 审查日期：2026-06-03
> 涉及范围：后端 Spring Boot 模块 + 前端架构 + 热部署 + 安全

---

## 一、审查发现的问题（按优先级）

### P0 - 严重问题（已修复）

| 问题 | 描述 | 修复 |
|------|------|------|
| **JWT 密钥硬编码** | application.yml 中硬编码默认密钥，生产不覆盖则任意 token 可伪造 | `yuncode-admin/application.yml`、`yuncode-gateway/application.yml` 移除默认值，改为 `${JWT_SECRET_KEY}`；开发环境密钥放 `application-dev.yml`；`SaTokenJwtConfig` 和 `AuthenticationFilter` 启动时检查密钥安全性，不满足则中断启动 |
| **密钥推导不一致** | Gateway 的 JJWT 和 Sa-Token 各自推导 `_user`/`_tenant` 后缀，不一致则 token 无法验证 | 新建 `JwtKeyUtil`（yuncode-common），统一管理密钥推导逻辑，Gateway 和 Sa-Token 都调用同一方法 |

### P1 - 架构缺陷（部分修复）

| 问题 | 描述 | 修复 |
|------|------|------|
| **Gateway 定位模糊** | 仅 JWT 验证+路由转发，无 IP 黑白名单、限流、完整监控 | 新增 `IpAccessFilter`（IP 黑白名单，支持 CIDR）、`MetricsFilter`（请求计数/成功率/响应时间）、增强 `RequestLogFilter`（X-Request-Id 跟踪/慢请求告警）、新增 `/gateway/metrics` 和 `/gateway/actuator/prometheus` 端点 |
| **App 热加载重复 JWT 验证** | Gateway 验证 JWT 后，后端 Sa-Token 又重新解析一次 | `AuthenticationFilter` 传递 `X-Gateway-Auth: true` 标记头给后端；新增 `GatewayAuthFilter`（yuncode-common）读取标记头直接构建登录态，避免后端重复解析 |
| **App 热加载安全沙箱** | ChildFirstURLClassLoader 无安全限制，恶意 JAR 可破坏平台 | `ChildFirstURLClassLoader` 新增安全沙箱，禁止反射 API、文件 IO、网络访问、Spring 内部容器操作 |
| **App 硬编码依赖** | pom.xml 中 app 模块作为 Maven dependency 编译进 admin JAR，卸载后类仍存在 | 改为 `<scope>provided</scope>`，打包时不包含；添加注释说明由 HotAppDeployer 运行时热加载 |
| **HotAppDeployer 卸载不完整** | 卸载时不清理 SpringDoc 缓存，已注册的 bean 失败不回滚 | 卸载时也调用 `refreshOpenApiCache()`；新增 `AppStatus` 追踪（LOADING/LOADED/FAILED/UNLOADING）；新增 `rollbackBeans()` 失败回滚 |

### P2 - 可优化（部分修复）

| 问题 | 描述 | 修复 |
|------|------|------|
| **模块间事件耦合** | yuncode-system 发布 `AppLifecycleEvent`，yuncode-admin 消费 | `AppLifecycleEvent` 移到 `yuncode-common.event`，新增模块只需依赖 `yuncode-common` 即可监听 |
| **关键表租户过滤依赖人工** | sys_user/sys_role/sys_org 等表跳过 MyBatis-Plus 自动拦截器，Service 层忘记加 tenantId 则数据泄露 | 新建 `@RequireTenant` 注解（STRICT/PERMISSIVE/BYPASS 三级）；新建 `TenantCheckAspect` AOP 拦截所有 Service 方法；`SecurityUtil.checkTenantAccess()` 自动反射检查参数中的 tenantId 是否匹配当前用户 |
| **API 路径前缀不一致** | `/settings`、`/menu`、`/org`、`/log/user` 等路径不在 `/system/*` 下 | 统一为 `/system/settings`、`/system/menu`、`/system/org`、`/system/log/user` 等，前端 API 路径同步更新 |
| **前端 designer 二开成本** | form-create designer 被直接引用，改动困难 | 已 fork 到本地 `form-create/packages/designer`，源码可直接修改 |

### P3 - 建议（已处理）

| 问题 | 描述 | 修复 |
|------|------|------|
| DataSourceSwitcher 未挂载 | 组件定义但没用到 | 集成到设置面板（右上角齿轮） |
| 表单设计器边框 | 蓝色边框默认可见 | 默认隐藏，hover 时显示 |
| BO 模型折叠 | 字段列表展开折叠功能缺失 | 主子表折叠，主表默认展开，子表默认折叠 |

---

## 二、模块拆分结论

经过实践对比，结论：**不拆 Maven 模块，用子包组织**。

```
yuncode-system (单模块)
├── com.yuncode.system.log/      ← 日志（controller/service/entity/mapper/aspect）
├── com.yuncode.system.iam/      ← 用户/角色/权限/菜单/组织/公司
├── com.yuncode.system.app/      ← 应用管理/BO模型/分类
└── com.yuncode.system.*/        ← 剩余：设置/通知/在线用户/适配器
```

理由：Maven 模块拆分导致大量跨模块引用问题，子包隔离已足够。

---

## 三、当前模块结构

```
yuncode-lowcode-boot/
├── yuncode-platform-sdk/    (14 文件) 对外 SDK 接口，零依赖
├── yuncode-common/          (25 文件) 工具/Utils/SDK 实现/事件/JwtKeyUtil
├── yuncode-auth/            (16 文件) 三级登录 + Sa-Token JWT 配置
├── yuncode-system/          (90+ 文件) 业务模块：log/iam/app/系统
├── yuncode-admin/           (20 文件) 启动入口 + HotAppDeployer + 监控 API
├── yuncode-gateway/         (15 文件) 网关：JWT 验证/IP黑名单/监控/Prometheus
└── apps/install/*/          App 热加载 JAR
```

---

## 四、SDK 设计（新增）

面向 App 开发者的统一 SDK 入口，只需依赖 `yuncode-platform-sdk`：

```java
SDK.getUser().currentUserId();      // 当前用户
SDK.getApp().currentAppId();        // 当前应用
SDK.getTenant().currentTenantId();  // 当前租户
SDK.getDB().query("SELECT ...");    // 数据库操作
```

平台内部通过 `SDKInitializer`（yuncode-common）在启动时自动注册实现，App 开发者零配置。

---

## 五、生产部署检查清单

- [ ] 设置 `JWT_SECRET_KEY` 环境变量（至少 32 字节随机值）
- [ ] Gateway 配置 IP 白名单（`gateway.ip-whitelist`）
- [ ] 检查 `yuncode-common` 的 filter 包是否被 `@ComponentScan` 扫描
- [ ] 删除 `yuncode-log`、`yuncode-iam`、`yuncode-app` 残留目录

---

## 六、关键文件变更汇总

| 领域 | 文件 | 操作 |
|------|------|------|
| JWT | `yuncode-admin/application.yml` | 移除默认密钥 |
| JWT | `yuncode-gateway/application.yml` | 移除默认密钥 |
| JWT | `SaTokenJwtConfig.java` | 启动时密钥安全检查 |
| JWT | `AuthenticationFilter.java` | 启动时密钥安全检查 + 传递认证头 |
| JWT | `JwtKeyUtil.java` | **新建** - 密钥推导工具类 |
| Gateway | `IpAccessFilter.java` | **新建** - IP 黑白名单 |
| Gateway | `MetricsFilter.java` | **新建** - 请求指标 |
| Gateway | `RequestLogFilter.java` | 增强 - X-Request-Id + 慢请求 |
| Gateway | `GatewayController.java` | 增强 - 添加 metrics/prometheus 端点 |
| Gateway | `GatewayAuthFilter.java` | **新建** - 后端信任 Gateway 认证 |
| AOP | `@RequireTenant.java` | **新建** - 租户隔离注解 |
| AOP | `TenantCheckAspect.java` | **新建** - 租户隔离 AOP |
| AOP | `SecurityUtil.java` | 增强 - add checkTenantAccess() |
| HotApp | `HotAppDeployer.java` | 增强 - 状态追踪/回滚/SpringDoc 清理/安全沙箱 |
| HotApp | `yuncode-admin/pom.xml` | App 模块改为 provided |
| Log | `yuncode-log/*` | 回移到 yuncode-system/log/ 子包 |
| IAM | `yuncode-iam/*` | 回移到 yuncode-system/iam/ 子包 |
| App | `yuncode-app/*` | 回移到 yuncode-system/app/ 子包 |
| API | 多个 Controller | 路径统一为 /system/* 前缀 |
| SDK | `SDK.java`, `*SDK.java` | **新建** - 5 个 SDK 接口 |
| SDK | `Default*SDK.java`, `SDKInitializer.java` | **新建** - 4 个实现 + 1 个初始化器 |
| Front | `vite.config.ts` | 新增 Gateway 代理 |
| Front | `monitor.ts`, `system-monitor/` | **新建** - 系统监控 API + 页面 |
| Front | 多个 API 文件 | 路径更新 /log/ → /system/log/ |
