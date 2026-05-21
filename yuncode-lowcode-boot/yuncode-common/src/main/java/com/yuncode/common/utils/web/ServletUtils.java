package com.yuncode.common.utils.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Servlet 工具类
 */
public class ServletUtils {

    /**
     * 获取请求的 IP 地址（合并所有代理头检测，取第一个非内网 IP）
     */
    public static String getClientIP(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("x-forwarded-for");
        if (isEmptyOrUnknown(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isEmptyOrUnknown(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (isEmptyOrUnknown(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (isEmptyOrUnknown(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (isEmptyOrUnknown(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (isEmptyOrUnknown(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (isEmptyOrUnknown(ip)) {
            ip = request.getRemoteAddr();
        }

        // 对于多个代理的情况，取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.substring(0, ip.indexOf(",")).trim();
        }

        // IPv6 回环地址处理
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }

        return ip;
    }

    private static boolean isEmptyOrUnknown(String ip) {
        return ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip);
    }

    /**
     * 获取用户代理信息
     */
    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        return request.getHeader("User-Agent");
    }
}
