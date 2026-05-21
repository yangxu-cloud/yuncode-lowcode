package com.yuncode.common.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 事件总线使用示例
 */
@Slf4j
@Component
public class EventBusDemo {

    private final SimpleEventBus eventBus;

    public EventBusDemo(SimpleEventBus eventBus) {
        this.eventBus = eventBus;
        registerCustomConsumer();
    }

    /**
     * 发布用户登录事件
     */
    public void publishLoginEvent(String userId, String username) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("username", username);
        data.put("loginTime", System.currentTimeMillis());

        eventBus.publish(EventTypes.USER_LOGIN, "auth-service", data);
        log.info("Published login event for user: {}", username);
    }

    /**
     * 发布应用创建事件
     */
    public void publishAppCreateEvent(String appId, String appName, String creator) {
        Map<String, Object> data = new HashMap<>();
        data.put("appId", appId);
        data.put("appName", appName);
        data.put("creator", creator);

        eventBus.publish(EventTypes.APP_CREATE, "app-service", data);
        log.info("Published app create event: {}", appName);
    }

    /**
     * 发布应用部署事件（异步）
     */
    public void publishAppDeployEventAsync(String appId, String appName) {
        Map<String, Object> data = new HashMap<>();
        data.put("appId", appId);
        data.put("appName", appName);

        eventBus.publishAsync(EventTypes.APP_DEPLOY, "app-service", data);
        log.info("Published async app deploy event: {}", appName);
    }

    /**
     * 注册自定义消费者
     * EventConsumer 接口可以用 Lambda 表达式实现
     */
    private void registerCustomConsumer() {
        // 注册登录事件消费者
        // EventConsumer 只有一个抽象方法 onEvent(GatewayEvent)，可直接使用 lambda
        eventBus.registerConsumer(EventTypes.USER_LOGIN, event -> {
            String userId = event.getData("userId");
            String username = event.getData("username");
            log.info("[Custom Consumer] User logged in: {} ({})", username, userId);
            // 这里可以做更多事情：
            // - 更新用户最后登录时间
            // - 发送通知
            // - 记录审计日志
        });

        // 注册应用创建事件消费者
        eventBus.registerConsumer(EventTypes.APP_CREATE, event -> {
            String appId = event.getData("appId");
            String appName = event.getData("appName");
            log.info("[Custom Consumer] App created: {} ({})", appName, appId);
            // 这里可以做更多事情：
            // - 创建应用目录
            // - 初始化应用数据
            // - 发送通知
        });
    }
}
