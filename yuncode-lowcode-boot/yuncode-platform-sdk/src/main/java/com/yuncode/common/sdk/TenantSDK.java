package com.yuncode.common.sdk;

/**
 * Tenant SDK — 当前租户相关操作
 */
public interface TenantSDK {

    /** 获取当前租户 ID */
    Long currentTenantId();

    /** 获取当前租户编码 */
    String currentTenantCode();

    /** 获取当前租户名称 */
    String currentTenantName();

    /** 获取租户配置 */
    String getConfig(String key);
}
