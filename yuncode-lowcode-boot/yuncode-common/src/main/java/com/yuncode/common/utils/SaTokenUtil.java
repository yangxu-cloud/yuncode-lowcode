package com.yuncode.common.utils;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;

/**
 * Sa-Token 工具类
 * 封装常用的认证和授权操作
 */
public class SaTokenUtil {

    /**
     * 获取当前登录用户 ID
     */
    public static Long getUserId() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null) {
            return null;
        }
        return Long.parseLong(loginId.toString());
    }

    /**
     * 获取当前登录用户名
     */
    public static String getUsername() {
        return (String) StpUtil.getSession().get("username");
    }

    /**
     * 获取当前昵称
     */
    public static String getNickname() {
        return (String) StpUtil.getSession().get("nickname");
    }

    /**
     * 获取当前租户 ID
     */
    public static Long getTenantId() {
        return (Long) StpUtil.getSession().get("tenantId");
    }

    /**
     * 获取当前租户编码
     */
    public static String getTenantCode() {
        return (String) StpUtil.getSession().get("tenantCode");
    }

    /**
     * 获取当前登录类型（admin/tenant/user）
     */
    public static String getLoginType() {
        return (String) StpUtil.getSession().get("loginType");
    }

    /**
     * 检查是否登录
     */
    public static boolean isLogin() {
        return StpUtil.isLogin();
    }

    /**
     * 检查是否拥有指定权限
     */
    public static boolean hasPermission(String permission) {
        return StpUtil.hasPermission(permission);
    }

    /**
     * 检查是否拥有指定角色
     */
    public static boolean hasRole(String role) {
        return StpUtil.hasRole(role);
    }

    /**
     * 校验权限，如果不通过则抛出异常
     */
    public static void checkPermission(String permission) {
        StpUtil.checkPermission(permission);
    }

    /**
     * 校验角色，如果不通过则抛出异常
     */
    public static void checkRole(String role) {
        StpUtil.checkRole(role);
    }

    /**
     * 踢人下线
     */
    public static void kickout(Long userId) {
        StpUtil.kickout(userId);
    }

    /**
     * 获取当前会话的 Token
     */
    public static String getTokenValue() {
        return StpUtil.getTokenValue();
    }

    /**
     * 密码加密（使用 BCrypt）
     */
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password);
    }

    /**
     * 密码校验（使用 BCrypt）
     */
    public static boolean checkPassword(String password, String hashed) {
        return BCrypt.checkpw(password, hashed);
    }

    /**
     * 登出
     */
    public static void logout() {
        StpUtil.logout();
    }
}
