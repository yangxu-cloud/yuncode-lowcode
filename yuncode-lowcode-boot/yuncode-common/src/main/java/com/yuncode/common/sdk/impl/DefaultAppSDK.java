package com.yuncode.common.sdk.impl;

import com.yuncode.common.sdk.AppSDK;

/**
 * App SDK 默认实现 — 桥接到平台 App 服务
 */
public class DefaultAppSDK implements AppSDK {

    private String appId;
    private String appName;
    private String version;

    public void setAppId(String appId) { this.appId = appId; }
    public void setAppName(String appName) { this.appName = appName; }
    public void setVersion(String version) { this.version = version; }

    @Override public String currentAppId() { return appId; }
    @Override public String currentAppName() { return appName; }
    @Override public String currentVersion() { return version; }

    @Override public String getConfig(String key) {
        // TODO: 从 App 配置表读取
        return System.getenv(key);
    }

    @Override public void setConfig(String key, String value) {
        // TODO: 写入 App 配置表
        System.setProperty(key, value);
    }
}
