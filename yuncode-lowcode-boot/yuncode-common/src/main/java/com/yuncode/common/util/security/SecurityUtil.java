package com.yuncode.common.util.security;

import cn.dev33.satoken.stp.StpUtil;
import com.yuncode.common.exception.BusinessException;
import com.yuncode.common.exception.ErrorCode;

/**
 * 安全工具类
 * 用于获取当前登录用户信息和权限校验
 *
 * @author Yuncode
 * @since 2025-01-28
 */
public class SecurityUtil {

    /**
     * 角色编码常量
     */
    public static final String ROLE_PLATFORM_ADMIN = "PLATFORM_ADMIN"; // 平台管理员
    public static final String ROLE_TENANT_ADMIN = "TENANT_ADMIN";     // 租户管理员
    public static final String ROLE_NORMAL = "NORMAL";                 // 普通用户

    /**
     * 获取当前用户ID
     *
     * @return 用户ID
     */
    public static Long getUserId() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return StpUtil.getSession().get("userId", 0L);
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名
     */
    public static String getUsername() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return StpUtil.getSession().get("username", "");
    }

    /**
     * 获取当前租户ID
     *
     * @return 租户ID
     */
    public static Long getTenantId() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return StpUtil.getSession().get("tenantId", 0L);
    }

    /**
     * 获取当前用户角色编码
     *
     * @return 角色编码（PLATFORM_ADMIN/TENANT_ADMIN/NORMAL）
     */
    public static String getRoleCode() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return StpUtil.getSession().get("roleCode", ROLE_NORMAL);
    }

    /**
     * 判断当前用户是否是平台管理员
     *
     * @return true=平台管理员
     */
    public static boolean isPlatformAdmin() {
        return ROLE_PLATFORM_ADMIN.equals(getRoleCode());
    }

    /**
     * 判断当前用户是否是租户管理员
     *
     * @return true=租户管理员
     */
    public static boolean isTenantAdmin() {
        return ROLE_TENANT_ADMIN.equals(getRoleCode());
    }

    /**
     * 判断当前用户是否是管理员（平台管理员或租户管理员）
     *
     * @return true=管理员
     */
    public static boolean isAdmin() {
        String roleCode = getRoleCode();
        return ROLE_PLATFORM_ADMIN.equals(roleCode) || ROLE_TENANT_ADMIN.equals(roleCode);
    }

    /**
     * 校验是否是平台管理员，如果不是则抛出异常
     */
    public static void checkPlatformAdmin() {
        if (!isPlatformAdmin()) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "需要平台管理员权限");
        }
    }

    /**
     * 校验是否是租户管理员，如果不是则抛出异常
     */
    public static void checkTenantAdmin() {
        if (!isTenantAdmin() && !isPlatformAdmin()) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "需要租户管理员权限");
        }
    }

    /**
     * 校验是否是管理员（平台管理员或租户管理员），如果不是则抛出异常
     */
    public static void checkAdmin() {
        if (!isAdmin()) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "需要管理员权限");
        }
    }
}
