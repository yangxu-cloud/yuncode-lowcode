# 日志体系设计

> 合并自：功能文档/平台日志处理方案.md + 功能文档/统一业务日志方案.md

## 日志分类

| 类型 | 表名 | 方式 | 内容 |
|------|------|------|------|
| 登录日志 | `sys_login_log` | @LoginLog 切面 | 登录/登出、IP、浏览器、耗时 |
| 操作日志 | `sys_operation_log` | @OperLog 切面 | CRUD、模块、参数、状态 |
| 系统日志 | `sys_system_log` | @SystemLog 切面 | 异常、性能警告、TraceId |

## 链路追踪

- `TraceIdContext` — ThreadLocal 持有 TraceId / SpanId / ParentSpanId
- `TraceIdAspect` — 自动为 Service/Controller/Mapper 创建子 Span
- `TraceIdFilter` — 从请求头提取或生成 TraceId，传播到日志和响应头
- 所有日志表均包含 `trace_id` / `span_id` / `parent_span_id` 字段

## 核心组件

| 组件 | 位置 | 职责 |
|------|------|------|
| LoginLogAspect | `com.yuncode.system.aspect` | 拦截 @LoginLog |
| OperLogAspect | `com.yuncode.system.aspect` | 拦截 @OperLog |
| SystemLogAspect | `com.yuncode.system.aspect` | 拦截 @SystemLog + Controller 异常 |
| TraceIdAspect | `com.yuncode.common.aspect` | 方法级链路追踪 |
| GlobalExceptionHandler | `com.yuncode.common.exception` | 全局异常处理 |
