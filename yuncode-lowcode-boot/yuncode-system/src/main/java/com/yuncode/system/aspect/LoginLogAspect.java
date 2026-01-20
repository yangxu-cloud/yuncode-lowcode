package com.yuncode.system.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.yuncode.common.exception.BusinessException;
import com.yuncode.system.annotation.LoginLog;
import com.yuncode.system.enums.LoginStatus;
import com.yuncode.system.service.SysLoginLogService;
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

import java.lang.reflect.Method;

/**
 * 登录日志切面
 * 自动记录带有 @LoginLog 注解的方法调用
 *
 * @author yuncode
 */
@Aspect
@Component
public class LoginLogAspect {

    private static final Logger log = LoggerFactory.getLogger(LoginLogAspect.class);

    @Autowired
    private SysLoginLogService loginLogService;

    /**
     * 配置织入点
     */
    @Pointcut("@annotation(com.yuncode.system.annotation.LoginLog)")
    public void loginLogPointcut() {
    }

    /**
     * 环绕通知：记录登录日志
     */
    @Around("loginLogPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LoginLog loginLogAnnotation = method.getAnnotation(LoginLog.class);

        // 获取方法参数
        Object[] args = joinPoint.getArgs();
        HttpServletRequest request = null;
        String username = "unknown";

        // 解析参数（假设第一个参数是 LoginDTO 或包含 username，第二个参数是 HttpServletRequest）
        if (args != null && args.length > 0) {
            // 尝试从第一个参数获取用户名
            try {
                Object firstArg = args[0];
                if (firstArg != null) {
                    // 使用反射获取 username 字段
                    try {
                        java.lang.reflect.Field usernameField = firstArg.getClass().getDeclaredField("username");
                        usernameField.setAccessible(true);
                        username = (String) usernameField.get(firstArg);
                    } catch (Exception e) {
                        log.debug("无法从第一个参数获取用户名");
                    }
                }
            } catch (Exception e) {
                log.debug("解析登录参数失败", e);
            }

            // 获取 HttpServletRequest
            if (args.length > 1 && args[1] instanceof HttpServletRequest) {
                request = (HttpServletRequest) args[1];
            }
        }

        // 初始化登录状态
        Integer status = LoginStatus.SUCCESS.getCode();
        String msg = "登录成功";
        Long tenantId = null;

        // 尝试从当前上下文获取租户ID（登录成功后）
        try {
            if (StpUtil.isLogin()) {
                Object tenantIdObj = StpUtil.getSession().get("tenantId");
                if (tenantIdObj != null) {
                    tenantId = Long.valueOf(tenantIdObj.toString());
                }
            }
        } catch (Exception e) {
            // 登录失败时忽略
        }

        try {
            // 执行方法
            Object result = joinPoint.proceed();
            return result;

        } catch (BusinessException e) {
            // 业务异常（登录失败）
            status = LoginStatus.FAIL.getCode();
            msg = e.getMessage();
            throw e;

        } catch (Exception e) {
            // 系统异常
            status = LoginStatus.FAIL.getCode();
            msg = "系统异常：" + e.getMessage();
            log.error("登录系统异常: loginType={}, username={}",
                    loginLogAnnotation.loginType(), username, e);
            throw new BusinessException(e.getMessage());

        } finally {
            // 记录登录日志（无论成功或失败）
            long costTime = System.currentTimeMillis() - startTime;

            try {
                loginLogService.recordLoginLog(
                        tenantId,
                        username,
                        status,
                        msg,
                        request,
                        costTime
                );

                log.debug("登录日志记录成功: loginType={}, username={}, status={}, costTime={}ms",
                        loginLogAnnotation.loginType(), username,
                        status == 0 ? "成功" : "失败", costTime);

            } catch (Exception e) {
                log.error("记录登录日志失败: loginType={}, username={}",
                        loginLogAnnotation.loginType(), username, e);
            }
        }
    }
}
