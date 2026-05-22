package com.yuncode.common.app;

import com.yuncode.common.event.EventPublisher;

import java.nio.file.Path;
import java.util.Properties;

/**
 * App 运行时上下文。
 * <p>
 * 提供平台能力给 App 使用，包括目录访问、配置获取、
 * Controller/Service 注册、事件发布等。
 * </p>
 */
public interface AppContext {

    /**
     * 获取应用数据目录（apps/install/{appId}/）
     */
    Path getAppDirectory();

    /**
     * 获取应用 lib 目录（apps/install/{appId}/lib/）
     */
    Path getLibDirectory();

    /**
     * 获取应用静态资源目录（apps/install/{appId}/web/）
     */
    Path getWebDirectory();

    /**
     * 获取应用发布清单元数据
     */
    AppManifest getManifest();

    /**
     * 获取自定义属性
     */
    Properties getProperties();

    /**
     * 注册 Controller 实例（Prod 模式使用）
     */
    void registerController(Object controller);

    /**
     * 注册 Service 实例（Prod 模式使用）
     */
    void registerService(Object service);

    /**
     * 获取事件发布器
     */
    EventPublisher getEventPublisher();

    /**
     * 获取租户 ID
     */
    Long getTenantId();
}
