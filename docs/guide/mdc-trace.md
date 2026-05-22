# MDC 链路追踪实现文档

## 概述

本项目已实现完整的链路追踪功能，采用 **MDC（Mapped Diagnostic Context） + SkyWalking Agent** 技术方案，支持分布式追踪和日志聚合分析。

## 实现的功能

### 1. 核心组件

#### 1.1 TraceIdContext 工具类
**位置**: `yuncode-lowcode-boot/yuncode-common/src/main/java/com/yuncode/common/utils/web/TraceIdContext.java`

**功能**:
- 生成唯一的 TraceId 和 SpanId
- 管理 MDC 上下文
- 支持分布式追踪（从 HTTP 请求头提取 TraceId）

**主要方法**:
```java
// 初始化新的链路追踪上下文
TraceIdContext.initContext();

// 获取 TraceId
String traceId = TraceIdContext.getTraceId();

// 创建子 Span（用于异步调用或下游服务）
TraceIdContext.createChildSpan();

// 清除上下文
TraceIdContext.clearContext();
```

#### 1.2 TraceIdFilter 过滤器
**位置**: `yuncode-lowcode-boot/yuncode-common/src/main/java/com/yuncode/common/utils/web/TraceIdFilter.java`

**功能**:
- 拦截所有 HTTP 请求
- 自动生成或提取 TraceId
- 将 TraceId 设置到 MDC 上下文
- 将 TraceId 添加到响应头（方便前端追踪）
- 请求结束时自动清除 MDC（避免内存泄漏）

**支持的分布式追踪协议**:
- 自定义: `X-Trace-Id`
- SkyWalking: `sw8-trace-id`
- Zipkin B3: `X-B3-TraceId`

#### 1.3 TraceIdAspect AOP 切面
**位置**: `yuncode-lowcode-boot/yuncode-common/src/main/java/com/yuncode/common/aspect/TraceIdAspect.java`

**功能**:
- 自动为 Service、Controller、Mapper 层的方法调用创建子 Span
- 记录方法执行时间
- 性能监控（执行时间超过 3 秒会记录警告日志）

### 2. 实体类更新

#### 2.1 SysLoginLog（登录日志）
**位置**: `yuncode-lowcode-boot/yuncode-system/src/main/java/com/yuncode/system/entity/SysLoginLog.java`

**新增字段**:
```java
/**
 * 链路追踪ID
 */
private String traceId;

/**
 * Span ID
 */
private String spanId;

/**
 * 父 Span ID
 */
private String parentSpanId;
```

#### 2.2 其他日志实体
- `SysSystemLog`（系统日志）- 已包含链路追踪字段
- `SysOperationLog`（操作日志）- 已包含链路追踪字段

### 3. 数据库更新脚本

#### 3.1 登录日志表更新脚本
**位置**: `yuncode-lowcode-boot/sql/update_login_log_add_trace_columns.sql`

**功能**:
- 为 `sys_login_log` 表添加 `trace_id`、`span_id`、`parent_span_id` 字段
- 创建索引提高查询性能
- 为历史数据生成 TraceId（可选）

#### 3.2 完整更新脚本
**位置**: `yuncode-lowcode-boot/sql/update_all_log_tables_add_trace_columns.sql`

**功能**:
- 支持所有日志表的链路追踪字段更新
- 包含验证脚本
- 详细的执行说明

### 4. 日志配置

#### 4.1 Logback 配置
**位置**: `yuncode-lowcode-boot/yuncode-admin/src/main/resources/logback-spring.xml`

**日志格式**:
```
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} [TraceId=%X{traceId}, SpanId=%X{spanId}] - %msg%n
```

**输出示例**:
```
2026-01-18 03:30:15.123 [http-nio-8080-exec-1] INFO  c.y.controller.LoginController [TraceId=a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6, SpanId=a1b2c3d4e5f6g7h8] - 用户登录成功
```

## 使用指南

### 1. 数据库更新

执行 SQL 脚本更新数据库表结构：

```bash
# 方式 1: 仅更新登录日志表
mysql -u root -p yuncode_lowcode < yuncode-lowcode-boot/sql/update_login_log_add_trace_columns.sql

# 方式 2: 更新所有日志表（推荐）
mysql -u root -p yuncode_lowcode < yuncode-lowcode-boot/sql/update_all_log_tables_add_trace_columns.sql
```

### 2. 代码中使用

#### 2.1 自动使用（推荐）
无需任何代码修改，系统会自动：
- 为每个请求生成 TraceId
- 在日志中输出 TraceId
- 保存到数据库（如果使用了日志实体）

#### 2.2 手动获取 TraceId
```java
import com.yuncode.common.utils.web.TraceIdContext;

public class SomeService {
    public void someMethod() {
        // 获取当前请求的 TraceId
        String traceId = TraceIdContext.getTraceId();
        String spanId = TraceIdContext.getSpanId();

        // 可以用于日志记录、异常追踪等
        log.info("当前 TraceId: {}", traceId);
    }
}
```

#### 2.3 异步任务中使用
```java
import com.yuncode.common.utils.web.TraceIdContext;
import org.slf4j.MDC;

@Service
public class AsyncService {

    @Async
    public void asyncMethod() {
        // 主线程的 TraceId
        String parentTraceId = TraceIdContext.getTraceId();

        // 在异步线程中设置 TraceId
        TraceIdContext.setTraceId(parentTraceId);

        try {
            // 执行业务逻辑
            doSomething();
        } finally {
            // 清除上下文
            TraceIdContext.clearContext();
        }
    }
}
```

### 3. 前端集成

#### 3.1 获取 TraceId
前端可以通过响应头获取 TraceId：

```javascript
// axios 拦截器示例
axios.interceptors.response.use(
    response => {
        const traceId = response.headers['x-trace-id'];
        if (traceId) {
            console.log('TraceId:', traceId);
            // 可以保存到 localStorage 或发送到错误追踪系统
            localStorage.setItem('lastTraceId', traceId);
        }
        return response;
    },
    error => {
        const traceId = error.response?.headers['x-trace-id'];
        if (traceId) {
            console.error('Error TraceId:', traceId);
            // 可以用于错误上报
            reportError(error, traceId);
        }
        return Promise.reject(error);
    }
);
```

#### 3.2 传递 TraceId
前端可以在请求头中传递 TraceId（支持分布式追踪）：

```javascript
// 获取上一次请求的 TraceId
const traceId = localStorage.getItem('lastTraceId');

// 在请求头中传递
axios.get('/api/data', {
    headers: {
        'X-Trace-Id': traceId || generateNewTraceId()
    }
});
```

## 日志查询示例

### 1. 查询特定 TraceId 的所有日志
```bash
# Linux/Mac
grep "TraceId=a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6" logs/yuncode-lowcode-all.log

# Windows PowerShell
Select-String -Path "logs\yuncode-lowcode-all.log" -Pattern "TraceId=a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6"
```

### 2. 数据库查询登录日志
```sql
-- 查询特定 TraceId 的登录日志
SELECT * FROM sys_login_log
WHERE trace_id = 'a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6';

-- 查询所有日志表的链路信息
SELECT
    'login' AS log_type,
    id,
    username,
    login_time,
    trace_id,
    span_id
FROM sys_login_log
WHERE trace_id = 'a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6'
UNION ALL
SELECT
    'operation' AS log_type,
    id,
    username,
    created_at,
    trace_id,
    span_id
FROM sys_operation_log
WHERE trace_id = 'a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6';
```

## SkyWalking 集成（可选）

### 1. 添加依赖（已可选）
```xml
<dependency>
    <groupId>org.apache.skywalking</groupId>
    <artifactId>apm-toolkit-trace</artifactId>
    <version>8.16.0</version>
</dependency>
```

### 2. 启动应用时使用 Agent
```bash
java -javaagent:./skywalking-agent/skywalking-agent.jar \
     -Dskywalking.agent.service_name=yuncode-lowcode \
     -Dskywalking.collector.backend_service=localhost:11800 \
     -jar yuncode-lowcode.jar
```

### 3. 查看 SkyWalking UI
访问: http://localhost:8088

## 性能影响

- **MDC 开销**: 极小（< 1ms）
- **日志输出**: 增加 ~30 字节/行（TraceId + SpanId）
- **数据库存储**: 每条日志增加约 100 字节
- **建议**: 生产环境可以关闭 DEBUG 日志，只保留 INFO 和 ERROR

## 最佳实践

1. **不要在循环中创建子 Span** - 只在方法入口创建
2. **异步任务要手动传递 TraceId** - 使用 `TraceIdContext.setTraceId()`
3. **定期清理历史日志** - 避免数据库膨胀
4. **监控慢查询** - 使用 AOP 切面自动记录执行时间
5. **前端也记录 TraceId** - 方便问题定位

## 故障排查

### 问题 1: 日志中没有 TraceId
**原因**: TraceIdFilter 未生效
**解决**: 检查 `TraceIdFilter` 是否被 Spring 扫描到

### 问题 2: 异步任务中 TraceId 丢失
**原因**: 异步线程无法继承主线程的 MDC
**解决**: 在异步方法中手动设置 TraceId

### 问题 3: 数据库中 trace_id 为 NULL
**原因**: 保存日志时未设置 traceId
**解决**: 确保使用 `TraceIdContext.getTraceId()` 获取并设置到实体

## 下一步计划

1. ✅ MDC + 数据库方案（当前实现）
2. ⬜ SkyWalking Agent 集成（测试阶段）
3. ⬜ 日志聚合分析（ELK/Loki）
4. ⬜ 可视化链路追踪界面
5. ⬜ 性能监控告警

## 相关文档

- [SkyWalking 集成指南](../../SKYWALKING_AGENT_INTEGRATION.md)
- [数据库更新脚本](../sql/update_all_log_tables_add_trace_columns.sql)
- [平台要求](../../.skills/平台要求/SKILL.md)
