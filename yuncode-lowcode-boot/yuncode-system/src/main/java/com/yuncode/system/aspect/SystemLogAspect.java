package com.yuncode.system.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.yuncode.system.entity.SysSystemLog;
import com.yuncode.system.service.SysSystemLogService;
import com.yuncode.common.utils.web.TraceIdContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 系统日志切面
 * 自动捕获并记录异常到系统日志表
 */
@Slf4j
@Aspect
@Component
public class SystemLogAspect {

    private final SysSystemLogService sysSystemLogService;

    public SystemLogAspect(SysSystemLogService sysSystemLogService) {
        this.sysSystemLogService = sysSystemLogService;
    }

    /**
     * 配置织入点：拦截所有 Controller 方法
     */
    @Pointcut("execution(* com.yuncode..controller..*.*(..))")
    public void controllerPointcut() {
    }

    /**
     * 环绕通知：捕获异常并记录到系统日志
     */
    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = null;
        String requestMethod = "";
        String requestUrl = "";
        String requestIp = "";

        if (attributes != null) {
            request = attributes.getRequest();
            requestMethod = request.getMethod();
            requestUrl = request.getRequestURI();
            requestIp = getClientIP(request);
        }

        Object result = null;

        try {
            // 执行方法
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            // 记录异常到系统日志
            recordSystemLog(method, requestMethod, requestUrl, requestIp, e, startTime);
            // 重新抛出异常，让全局异常处理器处理
            throw e;
        }
    }

    /**
     * 记录系统日志
     */
    private void recordSystemLog(Method method, String requestMethod, String requestUrl,
                                 String requestIp, Throwable exception, long startTime) {
        try {
            // 不记录 StackOverflowError，避免递归
            if (exception instanceof StackOverflowError) {
                log.warn("检测到 StackOverflowError: {}.{}, 跳过记录日志",
                        method.getDeclaringClass().getSimpleName(), method.getName());
                return;
            }

            SysSystemLog systemLog = new SysSystemLog();

            // 设置日志级别
            systemLog.setLevel("ERROR");

            // 设置模块（从类名获取）
            String className = method.getDeclaringClass().getSimpleName();
            systemLog.setModule(className);

            // 设置日志消息
            systemLog.setMessage("方法执行异常: " + method.getName());

            // 设置异常信息
            systemLog.setException(exception.getClass().getName() + ": " + exception.getMessage());

            // 设置堆栈跟踪（限制长度）
            String stackTrace = getStackTrace(exception);
            systemLog.setStackTrace(stackTrace.length() > 5000 ? stackTrace.substring(0, 5000) : stackTrace);

            // 设置请求信息
            if (requestUrl != null) {
                String requestInfo = String.format("%s %s", requestMethod, requestUrl);
                systemLog.setTags("{\"request\":\"" + requestInfo + "\",\"ip\":\"" + requestIp + "\"}");
            }

            // 设置链路追踪信息
            systemLog.setTraceId(TraceIdContext.getTraceId());
            systemLog.setSpanId(TraceIdContext.getSpanId());
            systemLog.setParentSpanId(TraceIdContext.getParentSpanId());

            // 设置创建时间
            systemLog.setCreatedAt(LocalDateTime.now());

            // 获取当前登录用户信息
            try {
                setUserInfo(systemLog);
            } catch (Exception e) {
                log.debug("获取用户信息失败: {}", e.getMessage());
            }

            // 异步保存到数据库
            final SysSystemLog logToSave = systemLog;
            new Thread(() -> {
                try {
                    sysSystemLogService.save(logToSave);
                    log.info("系统错误日志已保存: module={}, method={}, message={}",
                            systemLog.getModule(), method.getName(), exception.getMessage());
                } catch (Exception e) {
                    log.error("保存系统错误日志失败: {}", e.getMessage());
                }
            }).start();

        } catch (Throwable e) {
            // 避免递归，不打印异常堆栈
            if (!(e instanceof StackOverflowError)) {
                log.error("记录系统日志失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 设置用户信息
     */
    private void setUserInfo(SysSystemLog systemLog) {
        try {
            // 使用 StpUtil 获取登录用户信息
            if (StpUtil.isLogin()) {
                Object loginId = StpUtil.getLoginId();
                if (loginId != null) {
                    systemLog.setUserId(Long.valueOf(loginId.toString()));

                    // 从 Session 获取用户名
                    String username = StpUtil.getSession().get("username", "");
                    systemLog.setUsername(username);

                    // 获取租户信息
                    Long tenantId = StpUtil.getSession().get("tenantId", 0L);
                    if (tenantId != null && tenantId > 0) {
                        systemLog.setTenantId(tenantId);

                        String tenantName = StpUtil.getSession().get("tenantName", "");
                        systemLog.setTenantName(tenantName);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("获取用户信息失败: {}", e.getMessage());
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIP(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 获取异常堆栈跟踪
     */
    private String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}
