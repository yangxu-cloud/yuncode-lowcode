package com.yuncode.common.utils.web;

import cn.hutool.core.util.StrUtil;

/**
 * 用户代理工具类 - 用于解析浏览器和操作系统信息
 */
public class UserAgentUtils {

    /**
     * 解析浏览器类型
     */
    public static String parseBrowser(String userAgent) {
        if (StrUtil.isBlank(userAgent)) {
            return "未知";
        }

        if (userAgent.contains("Chrome") && !userAgent.contains("Edg")) {
            return "Chrome";
        } else if (userAgent.contains("Edg")) {
            return "Edge";
        } else if (userAgent.contains("Firefox")) {
            return "Firefox";
        } else if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) {
            return "Safari";
        } else if (userAgent.contains("Opera")) {
            return "Opera";
        } else if (userAgent.contains("Trident") || userAgent.contains("MSIE") || userAgent.contains("like Gecko")) {
            return "IE";
        } else {
            return "未知";
        }
    }

    /**
     * 解析操作系统
     */
    public static String parseOs(String userAgent) {
        if (StrUtil.isBlank(userAgent)) {
            return "未知";
        }

        if (userAgent.contains("Windows NT 10.0")) {
            return "Windows 10";
        } else if (userAgent.contains("Windows NT 6.3")) {
            return "Windows 8.1";
        } else if (userAgent.contains("Windows NT 6.2")) {
            return "Windows 8";
        } else if (userAgent.contains("Windows NT 6.1")) {
            return "Windows 7";
        } else if (userAgent.contains("Windows NT 6.0")) {
            return "Windows Vista";
        } else if (userAgent.contains("Windows NT 5.2") || userAgent.contains("Windows NT 5.1")) {
            return "Windows XP";
        } else if (userAgent.contains("Mac OS X")) {
            return "MacOS";
        } else if (userAgent.contains("Linux")) {
            return "Linux";
        } else if (userAgent.contains("Android")) {
            return "Android";
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            return "iOS";
        } else {
            return "未知";
        }
    }

    /**
     * 根据 IP 地址获取登录地点（简化版本）
     * 实际项目中可以调用 IP 定位服务
     */
    public static String getLocationByIP(String ip) {
        // TODO: 集成 IP 定位服务（如百度地图、高德地图等）
        // 这里暂时返回"内网IP"或"本地"
        if ("127.0.0.1".equals(ip) || "localhost".equals(ip) || "0:0:0:0:0:0:1".equals(ip)) {
            return "本地";
        } else if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
            return "内网";
        } else {
            return "未知";
        }
    }
}
