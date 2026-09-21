package com.yuncode.common.sdk.impl;

import com.yuncode.common.sdk.TenantSDK;
import com.yuncode.common.utils.SecurityUtil;

/**
 * Tenant SDK 默认实现 — 桥接到 SecurityUtil
 */
public class DefaultTenantSDK implements TenantSDK {

    @Override public Long currentTenantId() { return SecurityUtil.getTenantIdOrNull(); }
    @Override public String currentTenantCode() { return SecurityUtil.getTenantCode(); }
    @Override public String currentTenantName() {
        try {
            Object obj = cn.dev33.satoken.stp.StpUtil.getSession().get("tenantName");
            return obj != null ? obj.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    @Override public String getConfig(String key) {
        // TODO: 从 SysSettings 读取租户配置
        return System.getenv(key);
    }
}
