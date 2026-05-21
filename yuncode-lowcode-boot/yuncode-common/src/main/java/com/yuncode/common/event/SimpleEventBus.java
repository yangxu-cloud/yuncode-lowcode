package com.yuncode.common.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 简单事件总线
 * 系统所有事件通过这里统一处理
 */
@Slf4j
@Component
public class SimpleEventBus implements EventPublisher {

    private final Map<String, List<EventConsumer>> consumers = new ConcurrentHashMap<>();

    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(4);

    public SimpleEventBus() {
        // 注册默认的日志消费者
        registerConsumer("*", new LoggingEventConsumer());
    }

    @Override
    public void publish(GatewayEvent event) {
        publishEvent(event, false);
    }

    @Override
    public void publishAsync(GatewayEvent event) {
        publishEvent(event, true);
    }

    private void publishEvent(GatewayEvent event, boolean async) {
        // 补充事件信息
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(LocalDateTime.now());
        }

        log.debug("[EventBus] Publishing event: [{}] from {}", event.getEventType(), event.getSource());

        // 获取该事件类型的消费者
        List<EventConsumer> eventConsumers = new ArrayList<>();

        // 添加该事件类型的特定消费者
        if (consumers.containsKey(event.getEventType())) {
            eventConsumers.addAll(consumers.get(event.getEventType()));
        }

        // 添加通配符消费者
        if (consumers.containsKey("*")) {
            eventConsumers.addAll(consumers.get("*"));
        }

        // 通知消费者
        for (EventConsumer consumer : eventConsumers) {
            try {
                if (async) {
                    asyncExecutor.submit(() -> invokeConsumer(consumer, event));
                } else {
                    invokeConsumer(consumer, event);
                }
            } catch (Exception e) {
                log.error("[EventBus] Event consumer error: {}", e.getMessage(), e);
            }
        }
    }

    private void invokeConsumer(EventConsumer consumer, GatewayEvent event) {
        try {
            consumer.onEvent(event);
        } catch (Exception e) {
            log.error("[EventBus] Error processing event: {}", e.getMessage(), e);
        }
    }

    /**
     * 注册事件消费者
     */
    public void registerConsumer(String eventType, EventConsumer consumer) {
        consumers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(consumer);
        log.info("[EventBus] Registered consumer for [{}]: {}", eventType, consumer.getClass().getSimpleName());
    }

    /**
     * 快捷发布事件
     */
    public void publish(String eventType, String source, Map<String, Object> data) {
        publish(GatewayEvent.builder()
            .eventType(eventType)
            .source(source)
            .data(data)
            .build());
    }

    /**
     * 快捷发布事件（异步）
     */
    public void publishAsync(String eventType, String source, Map<String, Object> data) {
        publishAsync(GatewayEvent.builder()
            .eventType(eventType)
            .source(source)
            .data(data)
            .build());
    }

    /**
     * 日志事件消费者
     */
    private static class LoggingEventConsumer implements EventConsumer {
        @Override
        public void onEvent(GatewayEvent event) {
            log.info("[Event] [{}] [{}] from: {}",
                event.getTimestamp(), event.getEventType(), event.getSource());
        }
    }
}
