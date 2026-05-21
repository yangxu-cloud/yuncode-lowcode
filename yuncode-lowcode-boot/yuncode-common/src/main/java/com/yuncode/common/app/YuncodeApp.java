package com.yuncode.common.app;

/**
 * App 插件核心 SPI 接口。
 * <p>
 * 所有动态应用必须实现此接口。平台在 Dev 模式下通过 Spring 包扫描，
 * 在 Prod 模式下通过 ServiceLoader SPI 机制发现实现类，
 * 并按照生命周期方法管理 App 的安装、启动、停止和卸载。
 * </p>
 */
public interface YuncodeApp {

    /**
     * 应用唯一标识（如 qms0805）
     */
    String getAppId();

    /**
     * 应用展示名称
     */
    String getAppName();

    /**
     * 应用版本
     */
    String getVersion();

    /**
     * 应用的基础包路径（用于 Prod 模式下 Spring 组件扫描）
     */
    default String getBasePackage() {
        return "com.yuncode.user.apps." + getAppId();
    }

    /**
     * 安装时调用
     */
    default void onInstall(AppContext ctx) {}

    /**
     * 卸载时调用
     */
    default void onUninstall(AppContext ctx) {}

    /**
     * 启动时调用
     */
    default void onStartup(AppContext ctx) {}

    /**
     * 停止时调用
     */
    default void onShutdown(AppContext ctx) {}
}
