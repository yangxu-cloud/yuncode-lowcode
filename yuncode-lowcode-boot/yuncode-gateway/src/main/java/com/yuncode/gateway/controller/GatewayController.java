package com.yuncode.gateway.controller;

import com.yuncode.common.event.EventTypes;
import com.yuncode.common.event.GatewayEvent;
import com.yuncode.common.event.SimpleEventBus;
import com.yuncode.gateway.filter.MetricsFilter;
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
    private final MetricsFilter metricsFilter;

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
     * 获取请求指标（JSON 格式）
     */
    @GetMapping("/metrics")
    public Mono<Map<String, Object>> getMetrics() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", metricsFilter.getMetricsSnapshot());
        result.put("timestamp", System.currentTimeMillis());
        return Mono.just(result);
    }

    /**
     * 获取 Prometheus 格式指标
     */
    @GetMapping(value = "/actuator/prometheus", produces = "text/plain; charset=utf-8")
    public Mono<String> getPrometheusMetrics() {
        Map<String, Object> data = metricsFilter.getMetricsSnapshot();
        StringBuilder sb = new StringBuilder();

        sb.append("# HELP yuncode_gateway_requests_total 请求总数\n");
        sb.append("# TYPE yuncode_gateway_requests_total counter\n");
        sb.append("# HELP yuncode_gateway_request_duration_ms 请求耗时\n");
        sb.append("# TYPE yuncode_gateway_request_duration_ms gauge\n");

        data.forEach((key, value) -> {
            if (key.equals("_total")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> total = (Map<String, Object>) value;
                sb.append("yuncode_gateway_requests_total{path=\"_total\"} ").append(total.get("requests")).append("\n");
                sb.append("yuncode_gateway_success_total{path=\"_total\"} ").append(total.get("success")).append("\n");
                sb.append("yuncode_gateway_fail_total{path=\"_total\"} ").append(total.get("fail")).append("\n");
            } else {
                @SuppressWarnings("unchecked")
                Map<String, Object> pathData = (Map<String, Object>) value;
                String pathLabel = sanitizePrometheusLabel(key);
                sb.append("yuncode_gateway_requests_total{path=\"").append(pathLabel).append("\"} ").append(pathData.get("requests")).append("\n");
                sb.append("yuncode_gateway_success_total{path=\"").append(pathLabel).append("\"} ").append(pathData.get("success")).append("\n");
                sb.append("yuncode_gateway_fail_total{path=\"").append(pathLabel).append("\"} ").append(pathData.get("fail")).append("\n");
                sb.append("yuncode_gateway_request_duration_ms{path=\"").append(pathLabel).append("\"} ").append(pathData.get("avgDuration")).append("\n");
            }
        });

        sb.append("yuncode_gateway_up 1\n");
        return Mono.just(sb.toString());
    }

    private String sanitizePrometheusLabel(String s) {
        return s.replace("/", "_").replace("-", "_").toLowerCase();
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
