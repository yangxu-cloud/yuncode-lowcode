package com.yuncode.gateway.event;

import com.yuncode.common.event.EventConsumer;
import com.yuncode.common.event.EventPublisher;
import com.yuncode.common.event.GatewayEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 网关事件发布器
 */
@Slf4j
@Component
public class GatewayEventPublisher implements EventPublisher {

    private final List<EventConsumer> consumers = new ArrayList<>();

    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(4);

    public GatewayEventPublisher() {
        // 注册默认的日志消费者
        registerConsumer(new LoggingEventConsumer());
    }

    @Override
    public void publish(GatewayEvent event) {
        // 补充事件信息
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(LocalDateTime.now());
        }

        log.debug("[Gateway] Publishing event: {}", event.getEventType());

        // 通知所有消费者
        for (EventConsumer consumer : consumers) {
            try {
                if (consumer.supports(event.getEventType())) {
                    consumer.onEvent(event);
                }
            } catch (Exception e) {
                log.error("[Gateway] Event consumer error: {}", e.getMessage(), e);
            }
        }
    }

    @Override
    public void publishAsync(GatewayEvent event) {
        asyncExecutor.submit(() -> publish(event));
    }

    /**
     * 注册事件消费者
     */
    public void registerConsumer(EventConsumer consumer) {
        consumers.add(consumer);
        log.info("[Gateway] Registered event consumer: {}", consumer.getClass().getSimpleName());
    }

    /**
     * 注销事件消费者
     */
    public void unregisterConsumer(EventConsumer consumer) {
        consumers.remove(consumer);
    }

    /**
     * 日志事件消费者
     */
    private static class LoggingEventConsumer implements EventConsumer {
        @Override
        public void onEvent(GatewayEvent event) {
            log.info("[Gateway Event] [{}] [{}] from: {}",
                event.getTimestamp(), event.getEventType(), event.getSource());
        }
    }
}
