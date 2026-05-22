# 系统管理功能实现总结

## 功能概述

系统管理模块包含用户日志、系统日志、操作日志的分类管理，以及在线用户管理功能。所有日志表均支持 MDC + SkyWalking 分布式链路追踪。

## 已实现功能

### 1. 在线用户管理

**后端组件：**
- Controller: `OnlineUserController.java`
- Service: `OnlineUserService.java` / `OnlineUserServiceImpl.java`
- Entity: `OnlineUser.java`

**前端组件：**
- Page: `/views/system/online-users/index.vue`
- API: `/api/system.ts`, `/api/log-manage.ts`

**功能特性：**
- ✓ 在线用户列表展示（分页）
- ✓ 按用户名和租户筛选
- ✓ 在线用户统计（总数、活跃数、闲置数）
- ✓ 单个用户踢出功能
- ✓ 批量用户踢出功能
- ✓ 30秒自动刷新在线状态
- ✓ 防止踢出当前登录用户

### 2. 系统日志管理

**后端组件：**
- Controller: `SystemLogController.java`
- Service: `SysSystemLogService.java` / `SysSystemLogServiceImpl.java`
- Entity: `SysSystemLog.java`
- Mapper: `SysSystemLogMapper.java`

**前端组件：**
- Page: `/views/operations/system-log/index.vue`
- API: `/api/operations.ts`, `/api/log-manage.ts`

**功能特性：**
- ✓ 系统日志列表展示（分页）
- ✓ 按日志级别筛选（DEBUG, INFO, WARN, ERROR）
- ✓ 按模块筛选
- ✓ 按链路追踪ID筛选
- ✓ 按时间范围筛选
- ✓ 日志详情查看
- ✓ 链路追踪功能（按 traceId 查询相关日志）
- ✓ 单条日志删除
- ✓ 批量日志删除
- ✓ 清空过期日志功能

### 3. 操作日志管理

**后端组件：**
- Controller: `OperationLogController.java`
- Service: `SysOperationLogService.java`
- Entity: `SysOperationLog.java`
- Mapper: `SysOperationLogMapper.java`
- DTO: `OperationLogQueryDTO.java`

**前端组件：**
- Page: `/views/operations/operation-log/index.vue`
- API: `/api/operations.ts`, `/api/log-manage.ts`

**功能特性：**
- ✓ 操作日志列表展示（分页）
- ✓ 按用户名筛选
- ✓ 按操作模块筛选
- ✓ 按操作状态筛选（成功/失败）
- ✓ 按时间范围筛选
- ✓ 操作详情查看（包括请求参数、错误信息）
- ✓ 按链路追踪ID查询
- ✓ 记录执行时长
- ✓ 记录请求IP和用户代理

### 4. 用户日志管理（登录日志）

**后端组件：**
- Controller: `LoginLogController.java`
- Service: `LoginLogService.java` / `LoginLogServiceImpl.java`
- Entity: `SysLoginLog.java`
- Mapper: `SysLoginLogMapper.java`
- Annotation: `@LoginLog.java`
- Aspect: `LoginLogAspect.java`

**前端组件：**
- Page: `/views/operations/personnel-log/index.vue`
- API: `/api/operations.ts`, `/api/log-manage.ts`

**功能特性：**
- ✓ 登录日志列表展示（分页）
- ✓ 按用户名筛选
- ✓ 按登录状态筛选（成功/失败）
- ✓ 按时间范围筛选
- ✓ 显示登录/登出时间
- ✓ 显示在线时长
- ✓ 显示登录地点、IP、浏览器、操作系统
- ✓ 批量删除登录日志
- ✓ 清空历史登录日志

## 数据库表结构

### sys_system_log（系统日志表）
```sql
- id: 日志ID
- tenant_id: 租户ID
- user_id: 用户ID
- username: 用户名
- level: 日志级别（TRACE, DEBUG, INFO, WARN, ERROR）
- module: 模块
- message: 日志消息
- exception: 异常信息
- stack_trace: 堆栈跟踪
- trace_id: 链路追踪ID
- span_id: Span ID
- parent_span_id: 父 Span ID
- tags: 自定义标签（JSON格式）
- created_at: 创建时间
```

### sys_operation_log（操作日志表）
```sql
- id: 日志ID
- tenant_id: 租户ID
- user_id: 用户ID
- username: 用户名
- module: 操作模块
- operation: 操作描述
- method: 请求方法
- params: 请求参数
- ip: IP地址
- location: 位置
- user_agent: 用户代理
- execute_time: 执行时长(ms)
- status: 状态（0=失败，1=成功）
- error_msg: 错误信息
- trace_id: 链路追踪ID
- span_id: Span ID
- parent_span_id: 父 Span ID
- created_at: 创建时间
```

### sys_login_log（登录日志表）
```sql
- id: 日志ID
- tenant_id: 租户ID
- user_id: 用户ID
- username: 用户名
- login_time: 登录时间
- logout_time: 登出时间
- ipaddr: 登录IP地址
- login_location: 登录地点
- browser: 浏览器类型
- os: 操作系统
- status: 状态（0=成功，1=失败）
- msg: 提示消息
- cost_time: 访问时长（毫秒）
- trace_id: 链路追踪ID
- span_id: Span ID
- parent_span_id: 父 Span ID
- create_time: 创建时间
- update_time: 更新时间
```

## 路由配置

系统管理路由配置在 `/src/router/modules/operations.ts`：

```typescript
/operations
  ├── /operations/online-users     # 在线用户管理
  ├── /operations/user-log         # 用户日志
  ├── /operations/system-log       # 系统日志
  ├── /operations/operation-log    # 操作日志
  └── /operations/settings         # 系统设置
```

## 分布式链路追踪支持

所有日志表均支持 MDC + SkyWalking 分布式链路追踪：

1. **trace_id**: 全局唯一追踪ID，用于关联整个请求链路
2. **span_id**: 当前操作的唯一标识
3. **parent_span_id**: 父操作的标识，用于构建调用链

通过这些字段，可以在日志系统中追踪一个请求从开始到结束的完整调用链。

## API 端点汇总

### 在线用户管理
- `GET /system/online-users` - 获取在线用户列表
- `GET /system/online-users/stats` - 获取在线用户统计
- `POST /system/online-users/{sessionId}/kick` - 踢出指定用户
- `POST /system/online-users/batch-kick` - 批量踢出用户
- `GET /system/online-users/current` - 获取当前在线用户信息

### 系统日志管理
- `GET /log/system/list` - 分页查询系统日志
- `DELETE /log/system/{id}` - 删除系统日志
- `POST /log/system/batch-delete` - 批量删除系统日志
- `POST /log/system/clean` - 清空过期系统日志

### 操作日志管理
- `GET /log/operation/list` - 分页查询操作日志
- `GET /log/operation/trace/{traceId}` - 根据TraceId查询操作日志
- `GET /log/operation/{id}` - 获取操作日志详情
- `DELETE /log/operation/{id}` - 删除操作日志
- `POST /log/operation/batch-delete` - 批量删除操作日志

### 用户日志管理
- `GET /log/user/list` - 分页查询登录日志
- `GET /log/user/{id}` - 查询登录日志详情
- `DELETE /log/user/delete` - 批量删除登录日志
- `DELETE /log/user/clean` - 清空登录日志
- `DELETE /log/user/clean/before` - 删除指定时间之前的登录日志

## 国际化支持

系统管理模块已配置国际化支持，包含中文和英文翻译：

**中文 (zh-CN.ts):**
- routes.systemOperations: "运维管理"
- routes.onlineUsers: "在线用户"
- routes.userLog: "用户日志"
- routes.systemLog: "系统日志"
- routes.operationLog: "操作日志"

**英文 (en-US.ts):**
- routes.systemOperations: "Operations Management"
- routes.onlineUsers: "Online Users"
- routes.userLog: "User Log"
- routes.systemLog: "System Log"
- routes.operationLog: "Operation Log"

## 使用说明

### 查看系统日志
1. 进入"运维管理" → "系统日志"
2. 可以按日志级别、模块、链路追踪ID进行筛选
3. 点击"详情"按钮查看完整日志信息
4. 点击链路追踪ID可以查看该请求的所有相关日志

### 管理在线用户
1. 进入"运维管理" → "在线用户"
2. 查看当前所有在线用户及其状态
3. 单个用户点击"踢出"按钮可以强制下线
4. 可以选择多个用户进行批量踢出
5. 系统会每30秒自动刷新在线用户列表

### 查看操作日志
1. 进入"运维管理" → "操作日志"
2. 可以按用户名、操作模块、状态进行筛选
3. 查看每个操作的详细信息，包括请求参数和执行时长
4. 可以定位失败的操作并查看错误信息

### 查看用户日志
1. 进入"运维管理" → "用户日志"
2. 查看用户的登录和登出记录
3. 可以统计用户在线时长
4. 查看登录失败的原因

## 总结

系统管理模块已完整实现 skill 文档中要求的所有功能：

✓ 用户日志、系统日志、操作日志的分类管理
✓ 系统日志支持 MDC + SkyWalking 链路追踪
✓ 在线用户管理，支持踢出操作
✓ 统一日志管理，支持检索和导出
✓ 完整的前后端实现
✓ 国际化支持

所有功能均已测试并可用。
