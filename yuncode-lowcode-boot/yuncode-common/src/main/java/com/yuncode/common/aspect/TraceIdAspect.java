package com.yuncode.common.aspect;

import com.yuncode.common.utils.web.TraceIdContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 链路追踪 AOP 切面
 * 自动为 Service、Controller 层的方法调用创建子 Span
 *
 * 作用：
 * 1. 在方法调用时创建子 Span
 * 2. 记录方法执行时间
 * 3. 记录方法参数和返回值（可选）
 */
@Aspect
@Component
public class TraceIdAspect {

    private static final Logger log = LoggerFactory.getLogger(TraceIdAspect.class);

    /**
     * 切入点：所有 Service 层方法
     */
    @Pointcut("execution(* com.yuncode..service..*.*(..))")
    public void serviceLayer() {}

    /**
     * 切入点：所有 Controller 层方法
     */
    @Pointcut("execution(* com.yuncode..controller..*.*(..))")
    public void controllerLayer() {}

    /**
     * 切入点：所有 Mapper 层方法（数据库访问）
     */
    @Pointcut("execution(* com.yuncode..mapper..*.*(..))")
    public void mapperLayer() {}

    /**
     * 环绕通知：为每个方法调用创建子 Span
     */
    @Around("serviceLayer() || controllerLayer() || mapperLayer()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 创建子 Span
        TraceIdContext.createChildSpan();

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String traceId = TraceIdContext.getTraceId();
        String spanId = TraceIdContext.getSpanId();

        long startTime = System.currentTimeMillis();

        try {
            if (log.isTraceEnabled()) {
                log.trace("方法开始: {}.{} | TraceId: {} | SpanId: {}",
                        className, methodName, traceId, spanId);
            }

            // 执行方法
            Object result = joinPoint.proceed();

            long costTime = System.currentTimeMillis() - startTime;

            if (log.isTraceEnabled()) {
                log.trace("方法结束: {}.{} | 耗时: {}ms | TraceId: {} | SpanId: {}",
                        className, methodName, costTime, traceId, spanId);
            }

            // 如果方法执行时间过长，记录警告日志
            if (costTime > 3000) {
                log.warn("方法执行过慢: {}.{} | 耗时: {}ms | TraceId: {} | SpanId: {}",
                        className, methodName, costTime, traceId, spanId);
            }

            return result;

        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;

            log.error("方法异常: {}.{} | 耗时: {}ms | TraceId: {} | SpanId: {} | 异常: {}",
                    className, methodName, costTime, traceId, spanId, e.getMessage(), e);

            throw e;
        } finally {
            // 恢复父 Span（可选，看具体需求）
            // TraceIdContext.restoreParentSpan();
        }
    }
}
