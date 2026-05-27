package com.yuncode.common.utils;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.yuncode.common.exception.BusinessException;
import com.yuncode.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

/**
 * 安全工具类 — 统一的认证、授权、用户上下文工具。
 * 合并了原 SecurityUtil、SaTokenUtil、UserContextUtil 的功能。
 *
 * @author Yuncode
 */
@Slf4j
public final class SecurityUtil {

    private SecurityUtil() {}

    // ======================== 角色常量 ========================

    public static final String ROLE_PLATFORM_ADMIN = "PLATFORM_ADMIN";
    public static final String ROLE_TENANT_ADMIN = "TENANT_ADMIN";
    public static final String ROLE_NORMAL = "NORMAL";

    // ======================== 登录类型常量 ========================

    public static final String LOGIN_TYPE_ADMIN = "admin";
    public static final String LOGIN_TYPE_TENANT = "tenant";
    public static final String LOGIN_TYPE_USER = "user";

    // ======================== 用户信息获取（token-first, session fallback） ========================

    /**
     * 获取当前用户ID，未登录抛异常
     */
    public static Long getUserId() {
        Long userId = getUserIdOrNull();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    /**
     * 获取当前用户ID，未登录返回 null
     */
    public static Long getUserIdOrNull() {
        try {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            return loginId != null ? Long.valueOf(loginId.toString()) : null;
        } catch (Exception e) {
            log.debug("获取用户ID失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前用户名
     */
    public static String getUsername() {
        return getTokenOrSessionValue("username", "");
    }

    /**
     * 获取当前用户昵称
     */
    public static String getNickname() {
        return getTokenOrSessionValue("nickname", "");
    }

    /**
     * 获取当前租户ID，未登录抛异常
     */
    public static Long getTenantId() {
        Long tenantId = getTenantIdOrNull();
        if (tenantId == null || tenantId == 0L) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return tenantId;
    }

    /**
     * 获取当前租户ID，未登录返回 null
     */
    public static Long getTenantIdOrNull() {
        try {
            Object obj = StpUtil.getTokenSession().get("tenantId");
            if (obj != null) {
                return Long.valueOf(obj.toString());
            }
            return StpUtil.getSession().get("tenantId", null);
        } catch (Exception e) {
            log.debug("获取租户ID失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前租户编码
     */
    public static String getTenantCode() {
        return getTokenOrSessionValue("tenantCode", "");
    }

    /**
     * 获取当前角色编码，未登录抛异常
     */
    public static String getRoleCode() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try {
            Object obj = StpUtil.getTokenSession().get("roleCode");
            if (obj != null) {
                return obj.toString();
            }
            return StpUtil.getSession().get("roleCode", ROLE_NORMAL);
        } catch (Exception e) {
            log.debug("获取角色编码失败: {}", e.getMessage());
            return ROLE_NORMAL;
        }
    }

    /**
     * 获取当前登录类型（admin/tenant/user）
     */
    public static String getLoginType() {
        return getTokenOrSessionValue("loginType", "");
    }

    /**
     * 获取登录类型显示名称
     */
    public static String getLoginTypeName() {
        return switch (getLoginType()) {
            case LOGIN_TYPE_ADMIN -> "平台管理员";
            case LOGIN_TYPE_TENANT -> "租户管理员";
            case LOGIN_TYPE_USER -> "普通用户";
            default -> "未知";
        };
    }

    // ======================== 角色判断 ========================

    public static boolean isPlatformAdmin() {
        return ROLE_PLATFORM_ADMIN.equals(getRoleCode());
    }

    public static boolean isTenantAdmin() {
        return ROLE_TENANT_ADMIN.equals(getRoleCode());
    }

    public static boolean isAdmin() {
        String roleCode = getRoleCode();
        return ROLE_PLATFORM_ADMIN.equals(roleCode) || ROLE_TENANT_ADMIN.equals(roleCode);
    }

    // ======================== 基于登录类型的判断 ========================

    public static boolean isAdminType() {
        return LOGIN_TYPE_ADMIN.equals(getLoginType());
    }

    public static boolean isTenantType() {
        return LOGIN_TYPE_TENANT.equals(getLoginType());
    }

    public static boolean isUserType() {
        return LOGIN_TYPE_USER.equals(getLoginType());
    }

    // ======================== 权限校验（抛异常） ========================

    public static void checkPlatformAdmin() {
        if (!isPlatformAdmin()) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "需要平台管理员权限");
        }
    }

    public static void checkTenantAdmin() {
        if (!isTenantAdmin() && !isPlatformAdmin()) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "需要租户管理员权限");
        }
    }

    public static void checkAdmin() {
        if (!isAdmin()) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "需要管理员权限");
        }
    }

    // ======================== Sa-Token 权限/角色 ========================

    public static boolean isLogin() {
        return StpUtil.isLogin();
    }

    public static boolean hasPermission(String permission) {
        return StpUtil.hasPermission(permission);
    }

    public static boolean hasRole(String role) {
        return StpUtil.hasRole(role);
    }

    public static void checkPermission(String permission) {
        StpUtil.checkPermission(permission);
    }

    public static void checkRole(String role) {
        StpUtil.checkRole(role);
    }

    public static void kickout(Long userId) {
        StpUtil.kickout(userId);
    }

    public static String getTokenValue() {
        return StpUtil.getTokenValue();
    }

    public static void logout() {
        StpUtil.logout();
    }

    // ======================== 密码工具 ========================

    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    public static boolean checkPassword(String password, String hashed) {
        return BCrypt.checkpw(password, hashed);
    }

    // ======================== 内部工具 ========================

    private static String getTokenOrSessionValue(String key, String defaultValue) {
        try {
            Object obj = StpUtil.getTokenSession().get(key);
            if (obj != null) {
                return obj.toString();
            }
            return StpUtil.getSession().get(key, defaultValue);
        } catch (Exception e) {
            log.debug("获取 {} 失败: {}", key, e.getMessage());
            return defaultValue;
        }
    }
}
