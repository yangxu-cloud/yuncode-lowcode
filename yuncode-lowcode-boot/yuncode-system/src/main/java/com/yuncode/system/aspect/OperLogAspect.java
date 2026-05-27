package com.yuncode.system.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuncode.system.annotation.OperLog;
import com.yuncode.system.entity.SysOperationLog;
import com.yuncode.system.service.SysOperationLogService;
import com.yuncode.common.utils.web.ServletUtils;
import com.yuncode.common.utils.web.TraceIdContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志切面
 * 自动记录带有 @OperLog 注解的方法调用
 */
@RequiredArgsConstructor
@Aspect
@Component
public class OperLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(OperLogAspect.class);

    private final SysOperationLogService operationLogService;
    private final ObjectMapper objectMapper;

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
        operationLog.setOperation(getBusinessTypeName(operLogAnnotation.businessType()) + " " + operLogAnnotation.description());

        // 设置方法信息：请求方式 + URL + Java方法
        if (request != null) {
            String requestMethod = request.getMethod();
            String requestUri = request.getRequestURI();
            String javaMethod = signature.getDeclaringType().getSimpleName() + "." + method.getName();
            operationLog.setMethod(requestMethod + " " + requestUri + " -> " + javaMethod);
            operationLog.setIp(ServletUtils.getClientIP(request));
            operationLog.setUserAgent(request.getHeader("User-Agent"));  // 浏览器和操作系统信息
        } else {
            operationLog.setMethod(signature.getDeclaringType().getSimpleName() + "." + method.getName());
        }

        // 设置用户信息
        try {
            if (StpUtil.isLogin()) {
                operationLog.setUserId(StpUtil.getLoginIdAsLong());
                // 获取真实用户名（优先使用 nickname，其次 username）
                Object nicknameObj = StpUtil.getSession().get("nickname");
                Object usernameObj = StpUtil.getSession().get("username");
                if (nicknameObj != null) {
                    operationLog.setUsername(nicknameObj.toString());
                } else if (usernameObj != null) {
                    operationLog.setUsername(usernameObj.toString());
                } else {
                    operationLog.setUsername(StpUtil.getLoginIdAsString());
                }

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

        // 记录请求参数（过滤 MultipartFile 避免序列化失败）
        Object[] args = joinPoint.getArgs();
        try {
            if (args != null && args.length > 0) {
                Object[] loggableArgs = new Object[args.length];
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof MultipartFile) {
                        loggableArgs[i] = ((MultipartFile) args[i]).getOriginalFilename();
                    } else if (args[i] instanceof MultipartFile[]) {
                        MultipartFile[] files = (MultipartFile[]) args[i];
                        String[] names = new String[files.length];
                        for (int j = 0; j < files.length; j++) {
                            names[j] = files[j].getOriginalFilename();
                        }
                        loggableArgs[i] = names;
                    } else {
                        loggableArgs[i] = args[i];
                    }
                }
                String params = objectMapper.writeValueAsString(loggableArgs);
                operationLog.setParams(params.length() > 2000 ? params.substring(0, 2000) : params);
            }
        } catch (Throwable e) {
            // 忽略序列化异常（如 StackOverflowError），不影响业务逻辑
            if (!(e instanceof StackOverflowError)) {
                logger.error("操作日志参数序列化失败", e);
            }
        }

        Object result = null;
        try {
            // 执行方法
            result = joinPoint.proceed();

            // 操作成功
            operationLog.setStatus(1);  // 1=成功

        } catch (Exception e) {
            // 操作失败
            operationLog.setStatus(0);  // 0=失败
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
