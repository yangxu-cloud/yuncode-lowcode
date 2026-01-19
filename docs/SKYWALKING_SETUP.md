# SkyWalking Agent 集成指南

## 1. 添加依赖

```xml
<!-- pom.xml -->
<properties>
    <skywalking.version>8.16.0</skywalking.version>
</properties>

<dependencies>
    <!-- SkyWalking ToolKit -->
    <dependency>
        <groupId>org.apache.skywalking</groupId>
        <artifactId>apm-toolkit-trace</artifactId>
        <version>${skywalking.version}</version>
    </dependency>

    <!-- SkyWalking Logback 集成 -->
    <dependency>
        <groupId>org.apache.skywalking</groupId>
        <artifactId>apm-toolkit-logback-1.x</artifactId>
        <version>${skywalking.version}</version>
    </dependency>
</dependencies>
```

## 2. 配置 Logback（集成 SkyWalking + MDC）

```xml
<!-- logback-spring.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 控制台输出（带 MDC） -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId:-NO_TRACE}] [%X{spanId:-NO_SPAN}] [%X{tenantId:-NO_TENANT}] [%thread] %-5level %logger{50} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 文件输出 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/yuncode-lowcode.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/yuncode-lowcode.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>100MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId:-NO_TRACE}] [%X{spanId:-NO_SPAN}] [%X{tenantId:-NO_TENANT}] [%thread] %-5level %logger{50} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- SkyWalking gRPC 日志上报 -->
    <appender name="SKYWALKING_GRPC" class="org.apache.skywalking.apm.toolkit.log.logback.v1.x.log.GRPCLogClientAppender">
        <encoder class="ch.qos.logback.core.encoder.LogstashEncoder">
            <providers>
                <provider class="org.apache.skywalking.apm.toolkit.log.logback.v1.x.log.LogstashJsonProvider">
                    <!-- 增加自定义字段 -->
                    <pattern>
                        {
                        "traceId": "%mdc{traceId}",
                        "spanId": "%mdc{spanId}",
                        "tenantId": "%mdc{tenantId}",
                        "userId": "%mdc{userId}",
                        "level": "%level",
                        "logger": "%logger",
                        "message": "%message",
                        "exception": "%ex"
                        }
                    </pattern>
                </provider>
            </providers>
        </encoder>
    </appender>

    <!-- 异步日志（推荐生产环境） -->
    <appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="FILE" />
        <discardingThreshold>0</discardingThreshold>
        <queueSize>512</queueSize>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="ASYNC_FILE" />
        <appender-ref ref="SKYWALKING_GRPC" />
    </root>
</configuration>
```

## 3. 创建 TraceContext 管理

```java
package com.yuncode.common.trace;

import org.slf4j.MDC;

/**
 * 链路追踪上下文管理
 * 支持 MDC 和 SkyWalking
 */
public class TraceContext {

    private static final String TRACE_ID = "traceId";
    private static final String SPAN_ID = "spanId";
    private static final String PARENT_SPAN_ID = "parentSpanId";
    private static final String TENANT_ID = "tenantId";
    private static final String USER_ID = "userId";

    /**
     * 设置 TraceId
     * 优先使用 SkyWalking 的 TraceId，如果没有则自己生成
     */
    public static void setTraceId(String traceId) {
        if (traceId != null && !traceId.isEmpty()) {
            MDC.put(TRACE_ID, traceId);
        }
    }

    /**
     * 获取 TraceId
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    /**
     * 设置 SpanId
     */
    public static void setSpanId(String spanId) {
        if (spanId != null && !spanId.isEmpty()) {
            MDC.put(SPAN_ID, spanId);
        }
    }

    /**
     * 获取 SpanId
     */
    public static String getSpanId() {
        return MDC.get(SPAN_ID);
    }

    /**
     * 设置父 SpanId
     */
    public static void setParentSpanId(String parentSpanId) {
        if (parentSpanId != null && !parentSpanId.isEmpty()) {
            MDC.put(PARENT_SPAN_ID, parentSpanId);
        }
    }

    /**
     * 设置租户ID
     */
    public static void setTenantId(Long tenantId) {
        if (tenantId != null) {
            MDC.put(TENANT_ID, String.valueOf(tenantId));
        }
    }

    /**
     * 获取租户ID
     */
    public static Long getTenantId() {
        String tenantId = MDC.get(TENANT_ID);
        return tenantId != null ? Long.parseLong(tenantId) : null;
    }

    /**
     * 设置用户ID
     */
    public static void setUserId(Long userId) {
        if (userId != null) {
            MDC.put(USER_ID, String.valueOf(userId));
        }
    }

    /**
     * 获取用户ID
     */
    public static Long getUserId() {
        String userId = MDC.get(USER_ID);
        return userId != null ? Long.parseLong(userId) : null;
    }

    /**
     * 清理 MDC
     * 必须在请求结束时调用，避免内存泄漏
     */
    public static void clear() {
        MDC.clear();
    }

    /**
     * 生成 TraceId
     * 格式: {timestamp}-{processId}-{sequence}
     */
    public static String generateTraceId() {
        long timestamp = System.currentTimeMillis();
        String processId = getProcessId();
        String sequence = getSequence();
        return String.format("%d-%s-%s", timestamp, processId, sequence);
    }

    /**
     * 生成 SpanId
     */
    public static String generateSpanId() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private static java.util.concurrent.atomic.AtomicInteger sequence = new java.util.concurrent.atomic.AtomicInt(0);

    private static String getSequence() {
        return String.format("%04d", sequence.incrementAndGet() % 10000);
    }

    private static String getProcessId() {
        String pid = System.getenv().get("PID");
        if (pid == null) {
            pid = String.valueOf(ProcessHandle.current().pid());
        }
        return pid;
    }
}
```

## 4. 创建拦截器（同步 SkyWalking TraceId）

```java
package com.yuncode.common.interceptor;

import com.yuncode.common.trace.TraceContext;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 链路追踪拦截器
 * 同步 SkyWalking TraceId 到 MDC
 */
@Component
public class TraceInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler) throws Exception {

        // 1. 获取 SkyWalking 生成的 TraceId（如果有 Agent）
        String swTraceId = org.apache.skywalking.apm.toolkit.trace.TraceContext.traceId();

        // 2. 如果没有 SkyWalking TraceId，自己生成
        if (swTraceId == null || swTraceId.isEmpty()) {
            swTraceId = TraceContext.generateTraceId();
        }

        // 3. 设置到 MDC（供日志使用）
        TraceContext.setTraceId(swTraceId);

        // 4. 生成 SpanId
        TraceContext.setSpanId(TraceContext.generateSpanId());

        // 5. 设置租户和用户信息（从认证上下文获取）
        Long tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId != null) {
            TraceContext.setTenantId(tenantId);
        }
        if (userId != null) {
            TraceContext.setUserId(userId);
        }

        // 6. 将 TraceId 添加到响应头（前端可以获取）
        response.setHeader("X-Trace-Id", swTraceId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                              HttpServletResponse response,
                              Object handler,
                              Exception ex) throws Exception {
        // 清理 MDC，避免内存泄漏
        TraceContext.clear();
    }
}
```

## 5. 注册拦截器

```java
package com.yuncode.common.config;

import com.yuncode.common.interceptor.TraceInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private TraceInterceptor traceInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(traceInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/health", "/actuator/**", "/error");
    }
}
```

## 6. 使用注解进行链路追踪

```java
package com.yuncode.common.annotation;

import java.lang.annotation.*;

/**
 * 自定义链路追踪注解
 * 配合 SkyWalking 使用
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Traced {

    /**
     * 操作名称
     */
    String operation() default "";

    /**
     * 模块名称
     */
    String module() default "";

    /**
     * 是否记录参数
     */
    boolean logParams() default true;

    /**
     * 是否记录返回值
     */
    boolean logResult() default false;

    /**
     * 是否记录异常
     */
    boolean logException() default true;
}
```

## 7. 使用示例

```java
@RestController
@RequestMapping("/api/form")
public class FormController {

    private static final Logger logger = LoggerFactory.getLogger(FormController.class);

    /**
     * 创建表单
     * 自动记录链路追踪日志
     */
    @PostMapping("/create")
    @Traced(operation = "创建表单", module = "表单管理", logParams = true)
    public Result<Form> createForm(@RequestBody FormDTO dto) {
        logger.info("开始创建表单: {}", dto.getName());

        Form form = formService.create(dto);

        logger.info("表单创建成功: id={}", form.getId());
        return Result.success(form);
    }
}
```

## 8. 集成测试

```java
@SpringBootTest
public class TraceIntegrationTest {

    @Test
    public void testTraceContext() {
        // 模拟请求
        String traceId = TraceContext.generateTraceId();
        TraceContext.setTraceId(traceId);
        TraceContext.setTenantId(1001L);
        TraceContext.setUserId(1L);

        // 验证
        assertEquals(traceId, TraceContext.getTraceId());
        assertEquals(1001L, TraceContext.getTenantId());
        assertEquals(1L, TraceContext.getUserId());

        // 清理
        TraceContext.clear();
        assertNull(TraceContext.getTraceId());
    }
}
```

## 9. 启动参数配置

### 开发环境（不使用 Agent）

```bash
java -jar yuncode-lowcode.jar
```

### 测试环境（使用 Agent）

```bash
java -javaagent:/path/to/skywalking-agent.jar \
     -Dskywalking.agent.service_name=yuncode-lowcode-test \
     -Dskywalking.agent.authentication=your-token \
     -Dskywalking.collector.backend_service=localhost:11800 \
     -Dskywalking.plugin.springmvc.collect_http_params=true \
     -Dskywalking.plugin.springmvc.collect_http_body=true \
     -jar yuncode-lowcode.jar
```

### 生产环境

```bash
java -javaagent:/opt/skywalking/agent/skywalking-agent.jar \
     -Dskywalking.agent.service_name=yuncode-lowcode-prod \
     -Dskywalking.collector.backend_service=skywalking-server:11800 \
     -Xms2g -Xmx2g \
     -jar yuncode-lowcode.jar
```

## 10. Agent 配置文件

```properties
# /path/to/skywalking-agent/config/agent.config

# Agent 服务名称
agent.service_name=yuncode-lowcode

# 认证
agent.authentication=your-auth-token

# Collector 地址
collector.backend_service=localhost:11800

# 日志级别
logging.level=INFO

# 插件配置
plugin.springmvc.collect_http_params=true
plugin.springmvc.collect_http_body=true
plugin.springmvc.collect_http_headers=true

# 采样率（生产环境建议降低）
agent.sample_rate=1

# 忽略的路径（静态资源等）
agent.ignore_suffix=.jpg,.jpeg,.png,.gif,.bmp,.ico,.css,.js,.woff,.woff2

# 实例名称（支持多实例）
agent.instance_name=server-1

# 租户标签（多租户支持）
agent.namespace=yuncode
```

## 11. 故障排查

### 问题1：Agent 未生效

```bash
# 检查 Agent 是否加载
java -javaagent:skywalking-agent.jar \
     -Dskywalking.logging.level=DEBUG \
     -jar app.jar

# 查看日志中是否包含：
# SkyWalking Agent class isPresent
```

### 问题2：TraceId 不同步

```java
// 添加调试日志
@Component
public class TraceInterceptor implements HandlerInterceptor {
    public boolean preHandle(...) {
        String swTraceId = TraceContext.traceId();
        logger.info("SkyWalking TraceId: {}", swTraceId);

        String mdcTraceId = TraceContext.getTraceId();
        logger.info("MDC TraceId: {}", mdcTraceId);

        // 两者应该相同
    }
}
```

### 问题3：性能影响

```properties
# Agent 配置优化
agent.sample_rate=0.1          # 10% 采样率
agent.cool_down_threshold=10000 # 冷却时间
plugin.springmvc.enhance_sql_limit=10 # SQL 采集限制
```

## 12. 监控指标

访问 SkyWalking UI (http://localhost:8088)：

1. **服务列表**：查看所有应用
2. **拓扑图**：查看服务依赖关系
3. **链路追踪**：查看请求调用链路
4. **仪表盘**：查看性能指标
5. **日志**：查看关联的业务日志
6. **告警**：配置告警规则

## 13. 前端集成

```typescript
// 前端可以跳转到 SkyWalking UI
function openSkyWalkingTrace(traceId: string) {
  const url = `http://localhost:8088/trace/${traceId}`;
  window.open(url, '_blank');
}

// 在日志列表中使用
<el-link @click="openSkyWalkingTrace(row.traceId)">
  {{ row.traceId }}
</el-link>
```

## 14. 多租户支持

```java
/**
 * 多租户拦截器
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, ...) {
        // 从 Header 或 Token 获取租户ID
        String tenantId = request.getHeader("X-TenantId");
        if (tenantId != null) {
            TraceContext.setTenantId(Long.parseLong(tenantId));

            // 添加 SkyWalking Tag
            ActiveSpan.tag("tenant", tenantId);
        }

        return true;
    }
}
```

## 15. 最佳实践

1. **开发环境**：不使用 Agent，仅使用 MDC
2. **测试环境**：使用 Agent，100% 采样
3. **生产环境**：使用 Agent，10% 采样，降低开销
4. **日志保留**：SkyWalking 保留 7 天，数据库保留 30 天
5. **告警配置**：配置慢查询、异常、错误率告警
6. **定期清理**：定期清理过期数据，避免磁盘占满
