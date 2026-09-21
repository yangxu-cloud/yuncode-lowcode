# Yuncode LowCode 平台架构审查报告

> 审查日期：2026-06-03
> 审查范围：后端 Spring Boot 模块 + 前端 Vue 架构 + 热部署插件系统 + 多租户实现

---

## 一、架构概览

### 后端模块依赖链

```
yuncode-platform-sdk  (纯 SPI 接口层，无 Spring 依赖)
        ↓
  yuncode-common      (Sa-Token、MyBatis-Plus、Utils)
       ↙ ↘
yuncode-system       (实体、服务、Mapper)
       ↓
yuncode-auth         (三级登录 + JWT)
       ↓
yuncode-admin        (Spring Boot 主入口 + HotAppDeployer)

yuncode-gateway      (Spring Cloud Gateway，独立进程)
```

### 前端结构

```
yuncode-pure-admin/
├── src/api/         # Axios API 模块
├── src/views/       # 页面组件
├── src/router/      # 路由配置
├── src/store/       # Pinia 状态管理
├── src/layout/      # 布局组件
└── src/components/  # 公共组件
```

---

## 二、问题清单

### P0 - 严重问题

| # | 问题 | 描述 | 影响 |
|---|------|------|------|
| 1 | **JWT 默认密钥硬编码** | `application.yml` 和 `application-gateway.yml` 中硬编码了默认 JWT 密钥 `yuncode-lowcode-sa-token-jwt-secret-key-2024-...` | 生产不覆盖则任意 token 可伪造 |
| 2 | **关键表租户过滤依赖人工** | `sys_user`、`sys_role`、`sys_org` 等表通过 `@InterceptorIgnore(tenantLine = "true")` 跳过了 MyBatis-Plus 自动拦截器，由 Service 层手动添加 `tenantId` 条件 | 遗漏即数据泄露 |

### P1 - 架构缺陷

| # | 问题 | 描述 | 建议 |
|---|------|------|------|
| 3 | **Gateway 层定位模糊** | 仅做 JWT 验证 + 路由转发到 `localhost:8080`，没有分流/限流/服务治理。Nacos 默认关闭。与后端 Sa-Token 重复验证 token | 评估是否保留 Gateway，或简化配置 |
| 4 | **`yuncode-system` 模块膨胀** | 包含用户、角色、权限、菜单、组织、公司、租户、应用、日志、BO 模型等 10+ 个业务领域 | 拆分为 `yuncode-iam`、`yuncode-tenant`、`yuncode-app` 等 |
| 5 | **App 热加载无安全沙箱** | `ChildFirstURLClassLoader` 只做了类隔离，没有 `SecurityManager` 或权限策略 | 恶意 JAR 可访问内部 API |
| 6 | **App JAR 硬编码依赖** | `yuncode-admin/pom.xml` 中硬编码了部分 app 为 `<dependency>`，与 HotAppDeployer 热加载冲突 | 卸载后类仍存在 |

### P2 - 可优化

| # | 问题 | 描述 | 建议 |
|---|------|------|------|
| 7 | **模块间事件耦合** | `ApplicationServiceImpl`(yuncode-system) ↔ `HotAppDeployer`(yuncode-admin) 通过 Spring Event 通信 | 字段变更无编译检查，建议定义事件接口 |
| 8 | **HotAppDeployer 内存泄漏风险** | 频繁 install/uninstall 可能残留类加载器引用 | 增加 Bean 销毁的断言/监控 |
| 9 | **三套 JWT 密钥同步** | Gateway 用 JJWT 独立验证 token，密钥推导逻辑 `_user`/`_tenant` 后缀与 Sa-Token 必须一致 | 提取为公共配置，避免不同步 |
| 10 | **前端 form-create designer 二开成本** | 已 fork 源码，但改动量大，后续升级困难 | 明确定义改动范围，或评估自研 |

### P3 - 建议

| # | 问题 | 描述 |
|---|------|------|
| 11 | SaaS + PaaS 定位清晰但实现未对齐 | BO 模型、表单设计器、流程引擎三者联动还不完善 |
| 12 | 前端代码存在多余的 DataSourceSwitcher | 组件定义但未挂载（已找回来并使用） |
| 13 | API 路径前缀不一致 | 部分 `/system/*`，部分 `/log/*`，部分 `/settings/*` |
| 14 | 表单设计器拖拽体验待优化 | 自动栅格、边框显示、主子表折叠等 |

---

## 三、关键架构决策点

```
┌────────────────────────────────────────────────────────┐
│                架构决策树                              │
├────────────────────────────────────────────────────────┤
│                                                        │
│  1. Gateway 层： 保留 ── 需要微服务扩展能力             │
│                 移除 ── 降低复杂度                      │
│                                                        │
│  2. 模块拆分：   yuncode-system ─→ yuncode-iam          │
│                                  ─→ yuncode-tenant      │
│                                  ─→ yuncode-app         │
│                                                        │
│  3. 前端策略：   继续 fork form-create ─→ 深度定制      │
│                 自研轻量表单引擎 ─→ 可控但成本高         │
│                                                        │
│  4. 多租户：     共享 DB + tenant_id ─→ 当前方式         │
│                 独立 schema ─→ 隔离更好但成本高          │
│                                                        │
└────────────────────────────────────────────────────────┘
```

---

## 四、建议的下一步

| 优先级 | 行动项 | 涉及模块 |
|--------|--------|----------|
| **P0** | 1. 检查生产 JWT 密钥配置 | `yuncode-auth`, `yuncode-gateway` |
| **P0** | 2. 关键表租户过滤引入 AOP 切面 | `yuncode-system` |
| **P1** | 3. 评估 Gateway 去留 | `yuncode-gateway` |
| **P1** | 4. 计划 `yuncode-system` 拆分 | `yuncode-system` |
| **P2** | 5. App 热加载安全加固 | `yuncode-admin/app` |
| **P2** | 6. 前端设计器策略定方向 | `form-create/packages/designer` |
