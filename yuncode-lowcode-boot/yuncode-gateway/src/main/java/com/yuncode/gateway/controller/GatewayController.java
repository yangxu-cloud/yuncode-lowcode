package com.yuncode.gateway.controller;

import com.yuncode.common.event.EventTypes;
import com.yuncode.common.event.GatewayEvent;
import com.yuncode.common.event.SimpleEventBus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 网关管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/gateway")
@RequiredArgsConstructor
public class GatewayController {

    private final RouteLocator routeLocator;
    private final SimpleEventBus eventBus;

    @Value("${nacos.discovery.enabled:false}")
    private boolean nacosEnabled;

    @Value("${gateway.backend.admin-url:http://localhost:8080}")
    private String backendUrl;

    /**
     * 获取网关状态
     */
    @GetMapping("/status")
    public Mono<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("success", true);
        status.put("status", "UP");
        status.put("service", "yuncode-gateway");
        status.put("port", 9000);
        status.put("backendUrl", backendUrl);
        status.put("nacosEnabled", nacosEnabled);
        status.put("mode", nacosEnabled ? "Nacos Service Discovery" : "Fixed Routing");
        return Mono.just(status);
    }

    /**
     * 获取路由列表
     */
    @GetMapping("/routes")
    public Mono<Map<String, Object>> getRoutes() {
        return routeLocator.getRoutes()
            .collectList()
            .map(routes -> {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("count", routes.size());
                result.put("routes", routes.stream()
                    .map(r -> {
                        Map<String, Object> routeInfo = new HashMap<>();
                        routeInfo.put("id", r.getId());
                        routeInfo.put("uri", r.getUri().toString());
                        routeInfo.put("order", r.getOrder());
                        return routeInfo;
                    })
                    .toList());
                return result;
            });
    }

    /**
     * 发布测试事件
     */
    @PostMapping("/event/test")
    public Mono<Map<String, Object>> publishTestEvent(@RequestBody Map<String, Object> data) {
        String eventType = (String) data.getOrDefault("eventType", EventTypes.SYSTEM_ALERT);
        String source = (String) data.getOrDefault("source", "gateway");

        GatewayEvent event = GatewayEvent.builder()
            .eventType(eventType)
            .source(source)
            .build();

        Object eventData = data.get("data");
        if (eventData instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) eventData;
            map.forEach(event::addData);
        }

        eventBus.publish(event);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Test event published");
        result.put("eventId", event.getEventId());
        return Mono.just(result);
    }
}
