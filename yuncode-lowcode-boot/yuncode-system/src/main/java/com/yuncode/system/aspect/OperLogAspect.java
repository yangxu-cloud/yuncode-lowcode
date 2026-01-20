package com.yuncode.system.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuncode.system.annotation.OperLog;
import com.yuncode.system.entity.SysOperationLog;
import com.yuncode.system.service.SysOperationLogService;
import com.yuncode.common.utils.web.TraceIdContext;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志切面
 * 自动记录带有 @OperLog 注解的方法调用
 */
@Aspect
@Component
public class OperLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(OperLogAspect.class);

    @Autowired
    private SysOperationLogService operationLogService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 配置织入点
     */
    @Pointcut("@annotation(com.yuncode.system.annotation.OperLog)")
    public void operLogPointcut() {
    }

    /**
     * 环绕通知：记录操作日志
     */
    @Around("operLogPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取请求属性
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = null;
        if (attributes != null) {
            request = attributes.getRequest();
        }

        // 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperLog operLogAnnotation = method.getAnnotation(OperLog.class);

        // 创建操作日志对象
        SysOperationLog operationLog = new SysOperationLog();
        operationLog.setModule(operLogAnnotation.module());
        operationLog.setOperation(getBusinessTypeName(operLogAnnotation.businessType()) + ":" + operLogAnnotation.description());
        operationLog.setMethod(signature.getDeclaringType().getName() + "." + method.getName());

        if (request != null) {
            operationLog.setIp(getIpAddr(request));
        }

        // 设置用户信息
        try {
            if (StpUtil.isLogin()) {
                operationLog.setUserId(StpUtil.getLoginIdAsLong());
                operationLog.setUsername(StpUtil.getLoginIdAsString());

                // 获取租户信息
                Object tenantIdObj = StpUtil.getSession().get("tenantId");
                if (tenantIdObj != null) {
                    operationLog.setTenantId(Long.valueOf(tenantIdObj.toString()));
                }
                Object tenantNameObj = StpUtil.getSession().get("tenantName");
                if (tenantNameObj != null) {
                    operationLog.setTenantName(tenantNameObj.toString());
                }
            }
        } catch (Exception e) {
            // 未登录状态
        }

        // 设置链路追踪信息
        operationLog.setTraceId(TraceIdContext.getTraceId());
        operationLog.setSpanId(TraceIdContext.getSpanId());
        operationLog.setParentSpanId(TraceIdContext.getParentSpanId());

        // 记录请求参数
        Object[] args = joinPoint.getArgs();
        try {
            if (args != null && args.length > 0) {
                String params = objectMapper.writeValueAsString(args);
                operationLog.setParams(params.length() > 2000 ? params.substring(0, 2000) : params);
            }
        } catch (Exception e) {
            logger.error("操作日志参数序列化失败", e);
        }

        Object result = null;
        try {
            // 执行方法
            result = joinPoint.proceed();

            // 操作成功
            operationLog.setStatus("success");

        } catch (Exception e) {
            // 操作失败
            operationLog.setStatus("error");
            operationLog.setErrorMsg(e.getMessage());
            logger.error("方法执行异常: {}", method.getName(), e);
            throw e;
        } finally {
            // 设置执行时间
            long executeTime = System.currentTimeMillis() - startTime;
            operationLog.setExecuteTime(executeTime);
            operationLog.setCreatedAt(LocalDateTime.now());

            // 异步保存日志
            try {
                operationLogService.recordOperationLog(operationLog);
            } catch (Exception e) {
                logger.error("保存操作日志失败", e);
            }

            logger.debug("操作日志记录完成，耗时: {}ms", executeTime);
        }

        return result;
    }

    /**
     * 获取客户端IP地址
     */
    private String getIpAddr(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多个IP的情况，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }

    /**
     * 获取业务类型名称
     */
    private String getBusinessTypeName(int businessType) {
        return switch (businessType) {
            case 1 -> "新增";
            case 2 -> "修改";
            case 3 -> "删除";
            case 4 -> "授权";
            case 5 -> "导出";
            case 6 -> "导入";
            default -> "其它";
        };
    }
}
