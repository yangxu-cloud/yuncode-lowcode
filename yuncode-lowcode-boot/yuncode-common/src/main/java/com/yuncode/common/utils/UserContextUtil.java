package com.yuncode.common.utils;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户上下文工具类
 * 用于从 Sa-Token Token Extra 中获取当前用户信息
 *
 * 优势：
 * 1. 无需查询 Session 或数据库，直接从 JWT Token 获取
 * 2. 性能更高，减少 Redis 查询
 * 3. 适合微服务和分布式场景
 */
@Slf4j
public class UserContextUtil {

    /**
     * 用户类型常量
     */
    public static final String LOGIN_TYPE_ADMIN = "admin";
    public static final String LOGIN_TYPE_TENANT = "tenant";
    public static final String LOGIN_TYPE_USER = "user";

    /**
     * 获取当前用户ID
     *
     * @return 用户ID
     */
    public static Long getUserId() {
        try {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            return loginId != null ? Long.valueOf(loginId.toString()) : null;
        } catch (Exception e) {
            log.debug("获取用户ID失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前用户类型（从 Token Extra）
     *
     * @return 用户类型：admin/tenant/user
     */
    public static String getLoginType() {
        try {
            // 优先从 TokenSession（JWT Token Extra）获取
            Object loginTypeObj = StpUtil.getTokenSession().get("loginType");
            if (loginTypeObj != null) {
                return loginTypeObj.toString();
            }

            // 降级方案：从 Session 获取
            return StpUtil.getSession().get("loginType", "");
        } catch (Exception e) {
            log.debug("获取用户类型失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 获取当前租户ID（从 Token Extra）
     *
     * @return 租户ID
     */
    public static Long getTenantId() {
        try {
            // 优先从 TokenSession（JWT Token Extra）获取
            Object tenantIdObj = StpUtil.getTokenSession().get("tenantId");
            if (tenantIdObj != null) {
                return Long.valueOf(tenantIdObj.toString());
            }

            // 降级方案：从 Session 获取
            return StpUtil.getSession().get("tenantId", 0L);
        } catch (Exception e) {
            log.debug("获取租户ID失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前用户名（从 Token Extra）
     *
     * @return 用户名
     */
    public static String getUsername() {
        try {
            // 优先从 TokenSession（JWT Token Extra）获取
            Object usernameObj = StpUtil.getTokenSession().get("username");
            if (usernameObj != null) {
                return usernameObj.toString();
            }

            // 降级方案：从 Session 获取
            return StpUtil.getSession().get("username", "");
        } catch (Exception e) {
            log.debug("获取用户名失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 获取当前用户昵称（从 Token Extra）
     *
     * @return 用户昵称
     */
    public static String getNickname() {
        try {
            // 优先从 TokenSession（JWT Token Extra）获取
            Object nicknameObj = StpUtil.getTokenSession().get("nickname");
            if (nicknameObj != null) {
                return nicknameObj.toString();
            }

            // 降级方案：从 Session 获取
            return StpUtil.getSession().get("nickname", "");
        } catch (Exception e) {
            log.debug("获取用户昵称失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 判断当前用户是否是管理员
     *
     * @return true=是管理员, false=不是管理员
     */
    public static boolean isAdmin() {
        return LOGIN_TYPE_ADMIN.equals(getLoginType());
    }

    /**
     * 判断当前用户是否是租户管理员
     *
     * @return true=是租户管理员, false=不是租户管理员
     */
    public static boolean isTenant() {
        return LOGIN_TYPE_TENANT.equals(getLoginType());
    }

    /**
     * 判断当前用户是否是普通用户
     *
     * @return true=是普通用户, false=不是普通用户
     */
    public static boolean isUser() {
        return LOGIN_TYPE_USER.equals(getLoginType());
    }

    /**
     * 获取用户类型显示名称
     *
     * @return 用户类型显示名称
     */
    public static String getLoginTypeName() {
        String loginType = getLoginType();
        return switch (loginType) {
            case LOGIN_TYPE_ADMIN -> "平台管理员";
            case LOGIN_TYPE_TENANT -> "租户管理员";
            case LOGIN_TYPE_USER -> "普通用户";
            default -> "未知";
        };
    }
}
