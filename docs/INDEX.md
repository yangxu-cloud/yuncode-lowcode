# 文档索引

> docs/ 目录结构总览，按分类索引所有文档。

```
docs/
├── INDEX.md            ← 本文件，文档索引
├── requirements/       ← 需求文档（原始需求，源自 skills/）
├── design/             ← 架构设计（why + how）
├── spec/               ← 实现总结（what）
├── guide/              ← 操作指南（how to）
└── sql/                ← 数据库脚本
```

---

## requirements/ — 需求文档

各功能模块的原始需求定义。供开发时对照。

| 文档 | 说明 |
|------|------|
| [平台架构要求.md](requirements/平台架构要求.md) | 平台架构要求：技术选型、安全规范、监控要求 |
| [用户登录.md](requirements/用户登录.md) | 用户登录：多账号体系、登录日志 |
| [导航管理.md](requirements/导航管理.md) | 导航管理：左右面板、菜单树、权限分配、组件选择器 |
| [角色管理.md](requirements/角色管理.md) | 角色管理：角色树、分类、人员关联、权限分配 |
| [组织服务.md](requirements/组织服务.md) | 组织服务：组织树、公司/部门/人员、兼职管理、租户关联 |
| [界面样式.md](requirements/界面样式.md) | 界面样式：主题配置、PureAdmin 组件需求 |
| [应用开发.md](requirements/应用开发.md) | 应用开发：应用创建、安装升级、目录结构 |

---

## design/ — 架构设计

系统架构、技术选型和设计决策。适合新成员了解系统全貌。

| 文档 | 说明 |
|------|------|
| [app-hot-reload.md](design/app-hot-reload.md) | App 热插拔原理：WatchService、ClassLoader、Bean 注册/卸载 |
| [app-plugin-architecture.md](design/app-plugin-architecture.md) | App 插件系统架构设计：双模式加载、SPI 接口、未来规划 |
| [auth.md](design/auth.md) | 认证授权架构：Sa-Token + JWT + Redis + 多账号体系 |
| [database.md](design/database.md) | 数据库设计：表结构、字段说明 |
| [event-system.md](design/event-system.md) | 事件系统：EventPublisher/Consumer、使用说明 |
| [exception-handling.md](design/exception-handling.md) | 异常处理方案：全局异常、业务异常、错误码 |
| [gateway.md](design/gateway.md) | 网关方案设计：路由、鉴权、限流 |
| [i18n.md](design/i18n.md) | 国际化方案：Vue I18n 集成、语言切换 |
| [logging.md](design/logging.md) | 日志体系：三类日志、AOP 切面、链路追踪 |
| [multi-account-login.md](design/multi-account-login.md) | 多账户登录：用户踢出、SSE 消息机制 |
| [multi-tenancy.md](design/multi-tenancy.md) | 多租户设计：数据隔离、租户上下文 |
| [multi-user-online.md](design/multi-user-online.md) | 多用户在线管理：会话管理、在线列表 |
| [org-data-api.md](design/org-data-api.md) | 组织架构树数据接口 |
| [user-cache.md](design/user-cache.md) | 用户缓存设计：Redis 缓存、缓存策略 |

---

## spec/ — 实现总结

各功能模块的实现说明、接口定义和技术总结。

| 文档 | 说明 |
|------|------|
| [application-management.md](spec/application-management.md) | 应用管理功能实现总结 |
| [system-management.md](spec/system-management.md) | 系统管理功能实现总结 |
| [menu-structure.md](spec/menu-structure.md) | 菜单结构调整记录 |
| [app-dev-menu.md](spec/app-dev-menu.md) | 应用开发菜单设置 |
| [menu-diagnosis.md](spec/menu-diagnosis.md) | PureAdmin 公共设施菜单诊断 |
| [org-management.md](spec/org-management.md) | 组织服务功能实现 |
| [org-menu.md](spec/org-menu.md) | 组织管理菜单实现 |
| [company-management.md](spec/company-management.md) | 公司管理功能实现 |
| [interface-summary.md](spec/interface-summary.md) | 接口实现总结 |
| [gateway-summary.md](spec/gateway-summary.md) | 网关方案实现总结 |
| [frontend-integration.md](spec/frontend-integration.md) | 前端集成完整说明 |
| [multi-tenant-summary.md](spec/multi-tenant-summary.md) | 多租户用户管理系统实现 |
| [exception-summary.md](spec/exception-summary.md) | 异常处理改造总结 |
| [kickout-summary.md](spec/kickout-summary.md) | 踢出通知功能完整实现总结 |
| [multi-tab-login-test.md](spec/multi-tab-login-test.md) | 多标签页登录测试 |
| [multi-account-test-steps.md](spec/multi-account-test-steps.md) | 多账号测试步骤 |

---

## guide/ — 操作指南

环境配置、构建部署、故障排查。适合开发者日常查阅。

| 文档 | 说明 |
|------|------|
| [quickstart.md](guide/quickstart.md) | 快速启动指南（数据库初始化 → 后端 → 前端） |
| [build.md](guide/build.md) | 构建说明 |
| [compilation-issue.md](guide/compilation-issue.md) | 编译问题排查 |
| [reload-dependencies.md](guide/reload-dependencies.md) | 依赖重载说明 |
| [troubleshooting.md](guide/troubleshooting.md) | 通用故障排查 |
| [nacos.md](guide/nacos.md) | Nacos 切换指南 |
| [skywalking.md](guide/skywalking.md) | Skywalking 集成配置 |
| [skywalking-agent.md](guide/skywalking-agent.md) | Skywalking Agent 集成说明 |
| [maven-fix.md](guide/maven-fix.md) | Maven 依赖问题修复 |
| [sql-guide.md](guide/sql-guide.md) | SQL 脚本使用指南 |
| [jdk17-setup.md](guide/jdk17-setup.md) | JDK 17 环境配置 |
| [sse-debug.md](guide/sse-debug.md) | SSE 调试指南 |
| [clear-token.md](guide/clear-token.md) | Token 清理说明 |
| [mdc-trace.md](guide/mdc-trace.md) | MDC 链路追踪实现 |
| [kickout-guide.md](guide/kickout-guide.md) | 踢出通知前端集成指南 |
| [facility-menu-guide.md](guide/facility-menu-guide.md) | 公共设施菜单启动指南 |

---

## sql/ — 数据库脚本

| 脚本 | 说明 |
|------|------|
| `init_database.sql` | 数据库初始化（完整建表） |
| `init_system_role.sql` | 系统角色初始化 |
| `role_management.sql` | 角色管理相关 |
| `create_login_log_table.sql` | 登录日志表 |
| `create_oper_log_table.sql` | 操作日志表 |
| `create_system_log_table.sql` | 系统日志表 |
| `create_sys_settings.sql` | 系统设置表 |
| 其余迁移/修复脚本 | 按功能命名，按需执行 |
