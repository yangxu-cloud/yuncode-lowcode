package com.yuncode.common.utils;

import cn.dev33.satoken.stp.StpUtil;
import com.yuncode.common.exception.BaseException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 异常日志记录工具类
 * 统一记录异常日志，包含请求信息、用户信息、堆栈跟踪等
 *
 * @author yuncode
 */
@Slf4j
public class ExceptionLogUtil {

    /**
     * 记录异常日志（完整版）
     */
    public static void logException(Exception exception) {
        logException(exception, null);
    }

    /**
     * 记录异常日志（带自定义消息）
     */
    public static void logException(Exception exception, String message) {
        HttpServletRequest request = getRequest();
        Map<String, Object> requestInfo = buildRequestInfo(request);
        Map<String, Object> userInfo = buildUserInfo();

        StringBuilder logMessage = new StringBuilder();
        if (message != null) {
            logMessage.append(message).append(" | ");
        }

        logMessage.append("异常类型: ").append(exception.getClass().getSimpleName());
        logMessage.append(", 异常消息: ").append(exception.getMessage());

        if (exception instanceof BaseException) {
            BaseException baseException = (BaseException) exception;
            logMessage.append(", 错误码: ").append(baseException.getCode());
            if (baseException.getErrorCode() != null) {
                logMessage.append(" (").append(baseException.getErrorCode().name()).append(")");
            }
        }

        log.error(buildLogString(logMessage.toString(), requestInfo, userInfo), exception);
    }

    /**
     * 记录业务异常日志（不记录堆栈）
     */
    public static void logBusinessException(BaseException exception) {
        logBusinessException(exception, null);
    }

    /**
     * 记录业务异常日志（不记录堆栈，带自定义消息）
     */
    public static void logBusinessException(BaseException exception, String message) {
        HttpServletRequest request = getRequest();
        Map<String, Object> requestInfo = buildRequestInfo(request);
        Map<String, Object> userInfo = buildUserInfo();

        StringBuilder logMessage = new StringBuilder();
        if (message != null) {
            logMessage.append(message).append(" | ");
        }

        logMessage.append("业务异常");
        logMessage.append(", 错误码: ").append(exception.getCode());
        logMessage.append(", 错误消息: ").append(exception.getMessage());

        if (exception.getErrorCode() != null) {
            logMessage.append(" (").append(exception.getErrorCode().name()).append(")");
        }

        if (exception.getDetail() != null) {
            logMessage.append(", 详情: ").append(exception.getDetail());
        }

        log.warn(buildLogString(logMessage.toString(), requestInfo, userInfo));
    }

    /**
     * 记录参数校验异常日志
     */
    public static void logParamError(String fieldName, Object fieldValue, String errorMessage) {
        HttpServletRequest request = getRequest();
        Map<String, Object> requestInfo = buildRequestInfo(request);
        Map<String, Object> userInfo = buildUserInfo();

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("参数校验失败");
        logMessage.append(", 字段: ").append(fieldName);
        if (fieldValue != null) {
            logMessage.append(", 值: ").append(fieldValue);
        }
        logMessage.append(", 错误: ").append(errorMessage);

        log.warn(buildLogString(logMessage.toString(), requestInfo, userInfo));
    }

    /**
     * 记录系统异常日志（完整堆栈）
     */
    public static void logSystemException(Exception exception) {
        logSystemException(exception, "系统异常");
    }

    /**
     * 记录系统异常日志（完整堆栈，带自定义消息）
     */
    public static void logSystemException(Exception exception, String message) {
        HttpServletRequest request = getRequest();
        Map<String, Object> requestInfo = buildRequestInfo(request);
        Map<String, Object> userInfo = buildUserInfo();

        String logMessage = message + " | " + exception.getClass().getSimpleName() + ": " + exception.getMessage();

        log.error(buildLogString(logMessage, requestInfo, userInfo), exception);
    }

    // ========== 私有辅助方法 ==========

    private static HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Map<String, Object> buildRequestInfo(HttpServletRequest request) {
        Map<String, Object> requestInfo = new HashMap<>();

        if (request == null) {
            return requestInfo;
        }

        requestInfo.put("method", request.getMethod());
        requestInfo.put("uri", request.getRequestURI());
        requestInfo.put("queryString", request.getQueryString());
        requestInfo.put("remoteAddr", request.getRemoteAddr());
        requestInfo.put("userAgent", request.getHeader("User-Agent"));

        return requestInfo;
    }

    public static Map<String, String> getRequestHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!headerName.toLowerCase().contains("authorization") &&
                !headerName.toLowerCase().contains("token")) {
                headers.put(headerName, request.getHeader(headerName));
            }
        }
        return headers;
    }

    private static Map<String, Object> buildUserInfo() {
        Map<String, Object> userInfo = new HashMap<>();

        try {
            if (StpUtil.isLogin()) {
                Object loginId = StpUtil.getLoginIdDefaultNull();
                userInfo.put("loginId", loginId != null ? loginId.toString() : null);

                try {
                    String username = StpUtil.getSession().get("username") != null ?
                            StpUtil.getSession().get("username").toString() : null;
                    userInfo.put("username", username);
                } catch (Exception e) {
                    // 忽略获取用户名失败
                }

                try {
                    Object tenantId = StpUtil.getSession().get("tenantId");
                    userInfo.put("tenantId", tenantId != null ? tenantId.toString() : null);
                } catch (Exception e) {
                    // 忽略获取租户ID失败
                }
            }
        } catch (Exception e) {
            // 未登录或获取用户信息失败
        }

        return userInfo;
    }

    private static String buildLogString(String message, Map<String, Object> requestInfo, Map<String, Object> userInfo) {
        StringBuilder sb = new StringBuilder();

        sb.append("【异常日志】");

        sb.append(" ").append(message);

        if (!userInfo.isEmpty()) {
            sb.append(" | 用户信息: ").append(userInfo);
        }

        if (!requestInfo.isEmpty()) {
            sb.append(" | 请求信息: ");
            sb.append(requestInfo.get("method")).append(" ");
            sb.append(requestInfo.get("uri"));
            if (requestInfo.get("queryString") != null) {
                sb.append("?").append(requestInfo.get("queryString"));
            }
            sb.append(" | 来源IP: ").append(requestInfo.get("remoteAddr"));
        }

        return sb.toString();
    }
}
