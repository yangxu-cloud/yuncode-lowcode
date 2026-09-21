package com.yuncode.common.sdk;

/**
 * 云码低代码平台 SDK 入口
 *
 * App 开发者通过此入口访问平台能力，无需依赖任何内部实现模块。
 *
 * <pre>
 * // 获取当前用户
 * String name = SDK.getUser().currentUsername();
 *
 * // 获取当前应用
 * Long appId = SDK.getApp().currentAppId();
 *
 * // 操作数据库
 * List&lt;Map&lt;String, Object&gt;&gt; rows = SDK.getDB().query("SELECT * FROM my_table");
 * </pre>
 */
public final class SDK {

    private static volatile AppSDK appSDK;
    private static volatile UserSDK userSDK;
    private static volatile TenantSDK tenantSDK;
    private static volatile DatabaseSDK databaseSDK;

    private SDK() {}

    /** 获取 App SDK */
    public static AppSDK getApp() {
        if (appSDK == null) throw new IllegalStateException("SDK 未初始化，请先通过 SDK.init() 注册实现");
        return appSDK;
    }

    /** 获取用户 SDK */
    public static UserSDK getUser() {
        if (userSDK == null) throw new IllegalStateException("SDK 未初始化，请先通过 SDK.init() 注册实现");
        return userSDK;
    }

    /** 获取租户 SDK */
    public static TenantSDK getTenant() {
        if (tenantSDK == null) throw new IllegalStateException("SDK 未初始化，请先通过 SDK.init() 注册实现");
        return tenantSDK;
    }

    /** 获取数据库 SDK */
    public static DatabaseSDK getDB() {
        if (databaseSDK == null) throw new IllegalStateException("SDK 未初始化，请先通过 SDK.init() 注册实现");
        return databaseSDK;
    }

    /**
     * 初始化所有 SDK 实现（由平台在启动时调用一次）
     */
    public static void init(AppSDK app, UserSDK user, TenantSDK tenant, DatabaseSDK db) {
        appSDK = app;
        userSDK = user;
        tenantSDK = tenant;
        databaseSDK = db;
    }
}
