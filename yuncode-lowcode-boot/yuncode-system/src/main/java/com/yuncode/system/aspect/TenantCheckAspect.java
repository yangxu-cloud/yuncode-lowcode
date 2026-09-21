package com.yuncode.system.aspect;

import com.yuncode.common.annotation.RequireTenant;
import com.yuncode.common.annotation.RequireTenant.Isolation;
import com.yuncode.common.filter.GatewayAuthFilter;
import com.yuncode.common.utils.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 租户访问控制 AOP 切面
 *
 * 拦截 Service 层的方法调用，检查当前用户是否有权访问目标租户的数据。
 * 支持两级校验：
 * 1. 登录态校验（STRICT 默认）
 * 2. 数据级校验：如果方法参数包含 tenantId 字段，自动对比当前用户租户
 */
@Slf4j
@Aspect
@Order(2)
@Component
public class TenantCheckAspect {

    private static final String TENANT_ID_FIELD = "tenantId";

    /**
     * 拦截所有 Service 层方法
     */
    @Before("execution(* com.yuncode.*.service..*.*(..))")
    public void checkTenantAccess(JoinPoint joinPoint) {
        // 获取方法上的 @RequireTenant 注解
        Isolation isolation = getIsolationLevel(joinPoint);

        switch (isolation) {
            case BYPASS:
                return;
            case PERMISSIVE:
                Long tenantId = SecurityUtil.getTenantIdOrNull();
                log.debug("租户访问（宽松）: method={}, tenantId={}",
                    joinPoint.getSignature().toShortString(), tenantId);
                return;
            case STRICT:
                // ===== 1. 登录态校验 =====
                if (isGatewayAuthRequest()) {
                    break; // Gateway 已验证，直接放行
                }

                try {
                    if (!SecurityUtil.isLogin()) break;
                } catch (Exception e) {
                    log.debug("登录态检查跳过: {}", e.getMessage());
                    break;
                }

                // 平台管理员放行
                if (SecurityUtil.isPlatformAdmin()) break;

                // ===== 2. 数据级校验：检查方法参数中的 tenantId =====
                Object[] args = joinPoint.getArgs();
                if (args == null || args.length == 0) break;

                Long currentTenantId = SecurityUtil.getTenantIdOrNull();
                if (currentTenantId == null) break;

                for (Object arg : args) {
                    if (arg == null) continue;
                    Long paramTenantId = extractTenantId(arg);
                    if (paramTenantId != null && !currentTenantId.equals(paramTenantId)) {
                        log.warn("租户数据访问拒绝: 当前租户={}, 参数租户={}, method={}",
                            currentTenantId, paramTenantId,
                            joinPoint.getSignature().toShortString());
                        throw new com.yuncode.common.exception.BusinessException(
                            com.yuncode.common.exception.ErrorCode.NO_PERMISSION,
                            "无权访问其他租户的数据");
                    }
                }
                break;
        }
    }

    /**
     * 从方法参数中提取 tenantId
     * 支持三种类型：
     * 1. 参数本身就是 Long/Integer 类型的 tenantId
     * 2. 参数是实体对象，包含 tenantId 字段
     * 3. 参数是 DTO，包含 tenantId 字段
     */
    private Long extractTenantId(Object arg) {
        // 类型1: 参数本身就是 tenantId 值
        if (arg instanceof Long) {
            Long val = (Long) arg;
            if (val > 0) return val;
            return null;
        }
        if (arg instanceof Integer) {
            Integer val = (Integer) arg;
            if (val > 0) return val.longValue();
            return null;
        }

        // 类型2/3: 从对象中反射读取 tenantId 字段
        try {
            Field field = findField(arg.getClass(), TENANT_ID_FIELD);
            if (field == null) return null;
            field.setAccessible(true);
            Object val = field.get(arg);
            if (val instanceof Long) return (Long) val;
            if (val instanceof Integer) return ((Integer) val).longValue();
            // 支持 String 类型的 tenantId
            if (val instanceof String && !((String) val).isEmpty()) {
                return Long.parseLong((String) val);
            }
        } catch (NumberFormatException e) {
            log.debug("tenantId 解析失败: {}", e.getMessage());
        } catch (Exception e) {
            // 反射失败，忽略
        }
        return null;
    }

    /**
     * 递归查找字段（包括父类）
     */
    private Field findField(Class<?> clazz, String fieldName) {
        if (clazz == null || clazz == Object.class) return null;
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return findField(clazz.getSuperclass(), fieldName);
        }
    }

    private Isolation getIsolationLevel(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RequireTenant annotation = method.getAnnotation(RequireTenant.class);
        if (annotation != null) return annotation.isolation();

        Class<?> targetClass = joinPoint.getTarget().getClass();
        annotation = targetClass.getAnnotation(RequireTenant.class);
        if (annotation != null) return annotation.isolation();

        return Isolation.STRICT;
    }

    private boolean isGatewayAuthRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return false;
            HttpServletRequest request = attrs.getRequest();
            return "true".equals(request.getHeader(GatewayAuthFilter.GATEWAY_AUTH_HEADER))
                || "true".equals(request.getAttribute(GatewayAuthFilter.GATEWAY_AUTH_HEADER));
        } catch (Exception e) {
            return false;
        }
    }
}
