package com.yuncode.common.event;

/**
 * 事件消费者接口
 */
public interface EventConsumer {

    /**
     * 处理事件
     */
    void onEvent(GatewayEvent event);

    /**
     * 是否支持该事件类型
     */
    default boolean supports(String eventType) {
        return true;
    }

    /**
     * 获取事件类型
     */
    default String getEventType() {
        return "*";
    }
}
