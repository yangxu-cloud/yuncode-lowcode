package com.yuncode.system.service;

/**
 * Maven 模块注册服务接口。
 * <p>
 * 负责在 Dev 模式下自动将新建 App 注册为 Maven 子模块，
 * 使其能够参与父项目的统一构建。
 * </p>
 */
public interface MavenModuleService {

    /**
     * 注册 App 为 Maven 子模块
     * <ul>
     *   <li>在父 pom.xml 的 &lt;modules&gt; 中添加 &lt;module&gt; 条目</li>
     *   <li>在 yuncode-admin/pom.xml 的 &lt;dependencies&gt; 中添加依赖</li>
     * </ul>
     *
     * @param appId 应用 ID（如 com.yuncode.user.apps.qms0805）
     * @return 是否成功
     */
    boolean registerModule(String appId);

    /**
     * 从 Maven 构建中移除 App 模块
     * <ul>
     *   <li>从父 pom.xml 的 &lt;modules&gt; 中移除 &lt;module&gt; 条目</li>
     *   <li>从 yuncode-admin/pom.xml 的 &lt;dependencies&gt; 中移除依赖</li>
     * </ul>
     *
     * @param appId 应用 ID（如 com.yuncode.user.apps.qms0805）
     * @return 是否成功
     */
    boolean unregisterModule(String appId);

    /**
     * 检查 App 是否已注册为 Maven 子模块
     *
     * @param appId 应用 ID
     * @return 是否已注册
     */
    boolean isModuleRegistered(String appId);
}
