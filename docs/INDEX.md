# Yuncode LowCode - 文档中心

## 📖 最新文档

### 核心功能文档

1. **[平台日志处理方案](./功能文档/平台日志处理方案.md)** ⭐ 新增

   - 登录日志、操作日志、系统日志完整架构
   - AOP 切面自动记录机制
   - 链路追踪集成方案
   - 性能优化和监控告警策略

2. **[国际化语言切换方案](./功能文档/国际化语言切换方案.md)** ⭐ 新增

   - Vue I18n v11 完整配置
   - 中英文双语支持实现
   - 路由、菜单、表单国际化
   - 组件内翻译最佳实践

3. **[MDC 链路追踪实现](./MDC_TRACE_IMPLEMENTATION_COPY.md)**

   - TraceId 生成与传递机制
   - SpanId 和 ParentSpanId 管理
   - 与 SkyWalking Agent 集成准备
   - 完整的配置和使用示例

4. **[接口实现总结](./INTERFACE_IMPLEMENTATION_SUMMARY.md)**

   - 系统日志接口 (`/log/system/*`)
   - 在线用户接口 (`/system/online-users/*`)
   - 操作日志接口
   - API 测试集合和使用说明

5. **[用户缓存实现](./USER_CACHE_IMPLEMENTATION.md)**

   - Redis 缓存策略设计
   - Cache-Aside 缓存模式
   - 性能优化效果分析
   - 缓存失效和监控建议

### 配置和集成文档

1. **[Sa-Token JWT 集成](./SATOKEN_JWT_INTEGRATION.md)**

   - JWT 模式配置详解
   - 前后端分离集成
   - Token 生成和验证流程
   - 常见问题和解决方案

2. **[数据库设计](./DATABASE.md)**

   - 表结构设计
   - 索引优化
   - 多租户数据隔离
   - 初始化脚本说明

### 快速开始

1. **[项目概览](./PROJECT_SUMMARY.md)**

   - 项目架构说明
   - 技术栈介绍
   - 模块划分
   - 开发规范

2. **[快速启动指南](./QUICKSTART.md)**

   - 环境准备
   - 数据库初始化
   - 后端启动步骤
   - 前端启动步骤
   - 默认账号信息

### SkyWalking 集成

1. **[SkyWalking 配置指南](./SKYWALKING_SETUP.md)**

   - Agent 下载和安装
   - 配置文件说明
   - 应用监控设置
   - 链路追踪配置

## 🚀 快速导航

### 我想了解...

**如何快速启动项目？**

→ 查看 [快速启动指南](./QUICKSTART.md)

**如何实现国际化语言切换？**

→ 查看 [国际化语言切换方案](./功能文档/国际化语言切换方案.md)

**如何使用 MDC 链路追踪？**

→ 查看 [MDC 链路追踪实现](./MDC_TRACE_IMPLEMENTATION_COPY.md)

**如何调用后端接口？**

→ 查看 [接口实现总结](./INTERFACE_IMPLEMENTATION_SUMMARY.md)

**如何优化用户查询性能？**

→ 查看 [用户缓存实现](./USER_CACHE_IMPLEMENTATION.md)

**如何集成 SkyWalking？**

→ 查看 [SkyWalking 配置指南](./SKYWALKING_SETUP.md)

**如何配置 JWT 认证？**

→ 查看 [Sa-Token JWT 集成](./SATOKEN_JWT_INTEGRATION.md)

## 📊 文档统计

- 核心功能文档：5 篇
- 配置指南文档：2 篇
- 快速开始文档：2 篇
- 总文档数：10+ 篇

## 🔍 搜索关键词

- **日志、AOP、切面、记录** → [平台日志处理方案](./功能文档/平台日志处理方案.md)
- **国际化、i18n、多语言、中英文** → [国际化语言切换方案](./功能文档/国际化语言切换方案.md)
- **MDC、TraceId、链路追踪** → [MDC 链路追踪实现](./MDC_TRACE_IMPLEMENTATION_COPY.md)
- **API、接口、日志管理** → [接口实现总结](./INTERFACE_IMPLEMENTATION_SUMMARY.md)
- **缓存、Redis、性能优化** → [用户缓存实现](./USER_CACHE_IMPLEMENTATION.md)
- **JWT、认证、登录** → [Sa-Token JWT 集成](./SATOKEN_JWT_INTEGRATION.md)
- **数据库、表结构** → [数据库设计](./DATABASE.md)
- **SkyWalking、监控** → [SkyWalking 配置指南](./SKYWALKING_SETUP.md)

## 📝 更新记录

### 2025-01-22

- ✅ 添加国际化语言切换方案文档
- ✅ 更新系统运维为运维管理

### 2025-01-18

- ✅ 添加用户缓存实现文档
- ✅ 添加接口实现总结文档
- ✅ 整合所有文档到 docs 目录

### 2025-01-17

- ✅ 添加 MDC 链路追踪文档
- ✅ 添加 Sa-Token JWT 集成文档
- ✅ 添加数据库设计文档

---

**文档维护者：** Yuncode LowCode 开发团队
**最后更新：** 2025-01-22
