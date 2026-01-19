package com.yuncode.system.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuncode.system.annotation.OperLog;
import com.yuncode.system.entity.SysOperLog;
import com.yuncode.system.service.OperLogService;
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
    private OperLogService operLogService;

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
        SysOperLog operLog = new SysOperLog();
        operLog.setModule(operLogAnnotation.module());
        operLog.setBusinessType(operLogAnnotation.businessType());
        operLog.setMethod(signature.getDeclaringType().getName() + "." + method.getName());

        if (request != null) {
            operLog.setRequestMethod(request.getMethod());
            operLog.setOperUrl(request.getRequestURI());
            operLog.setOperIp(getIpAddr(request));
        }

        try {
            if (StpUtil.isLogin()) {
                operLog.setOperName(StpUtil.getLoginIdAsString());
            }
        } catch (Exception e) {
            // 未登录状态
        }

        // 设置链路追踪信息
        operLog.setTraceId(TraceIdContext.getTraceId());
        operLog.setSpanId(TraceIdContext.getSpanId());
        operLog.setParentSpanId(TraceIdContext.getParentSpanId());

        // 记录请求参数
        Object[] args = joinPoint.getArgs();
        try {
            if (args != null && args.length > 0) {
                String params = objectMapper.writeValueAsString(args);
                operLog.setOperParam(params.length() > 2000 ? params.substring(0, 2000) : params);
            }
        } catch (Exception e) {
            logger.error("操作日志参数序列化失败", e);
        }

        Object result = null;
        try {
            // 执行方法
            result = joinPoint.proceed();

            // 操作成功
            operLog.setStatus(0);

            // 记录返回结果
            try {
                if (result != null) {
                    String jsonResult = objectMapper.writeValueAsString(result);
                    operLog.setJsonResult(jsonResult.length() > 2000 ? jsonResult.substring(0, 2000) : jsonResult);
                }
            } catch (Exception e) {
                logger.error("操作日志返回结果序列化失败", e);
            }

        } catch (Exception e) {
            // 操作失败
            operLog.setStatus(1);
            operLog.setErrorMsg(e.getMessage());
            logger.error("方法执行异常: {}", method.getName(), e);
            throw e;
        } finally {
            // 设置操作时间
            operLog.setOperTime(LocalDateTime.now());

            // 异步保存日志
            try {
                operLogService.saveOperLog(operLog);
            } catch (Exception e) {
                logger.error("保存操作日志失败", e);
            }

            long endTime = System.currentTimeMillis();
            logger.debug("操作日志记录完成，耗时: {}ms", endTime - startTime);
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
}
