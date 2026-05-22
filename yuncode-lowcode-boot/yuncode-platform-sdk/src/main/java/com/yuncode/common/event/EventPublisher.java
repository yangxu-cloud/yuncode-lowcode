package com.yuncode.common.event;

/**
 * 事件发布器接口
 */
public interface EventPublisher {

    /**
     * 发布事件
     */
    void publish(GatewayEvent event);

    /**
     * 异步发布事件
     */
    void publishAsync(GatewayEvent event);
}
