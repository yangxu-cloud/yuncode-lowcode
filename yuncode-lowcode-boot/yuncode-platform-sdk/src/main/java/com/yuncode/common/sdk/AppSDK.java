package com.yuncode.common.sdk;

/**
 * App SDK — 当前应用相关操作
 */
public interface AppSDK {

    /** 获取当前 App ID */
    String currentAppId();

    /** 获取当前 App 名称 */
    String currentAppName();

    /** 获取当前 App 版本 */
    String currentVersion();

    /** 获取 App 配置值 */
    String getConfig(String key);

    /** 设置 App 配置值 */
    void setConfig(String key, String value);
}
