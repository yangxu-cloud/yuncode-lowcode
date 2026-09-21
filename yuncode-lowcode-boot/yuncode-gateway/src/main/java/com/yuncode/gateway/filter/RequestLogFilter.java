package com.yuncode.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 请求日志过滤器
 * 添加 X-Request-Id trace 追踪，记录请求耗时和响应大小
 */
@Slf4j
@Component
public class RequestLogFilter implements GlobalFilter, Ordered {

    /** 慢请求阈值（毫秒），默认 5000ms */
    @Value("${gateway.slow-request-threshold:5000}")
    private long slowRequestThreshold;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        long startTime = System.currentTimeMillis();

        // 获取或生成 X-Request-Id（必须为 effectively final 才能在 lambda 中使用）
        String requestIdHeader = request.getHeaders().getFirst("X-Request-Id");
        String requestId = (requestIdHeader != null && !requestIdHeader.isEmpty())
            ? requestIdHeader
            : UUID.randomUUID().toString().replace("-", "");

        // 将 requestId 写入请求头，传递给下游服务
        ServerHttpRequest mutatedRequest = request.mutate()
            .header("X-Request-Id", requestId)
            .build();
        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        return chain.filter(mutatedExchange).doOnSuccess(aVoid -> {
            long duration = System.currentTimeMillis() - startTime;
            var response = mutatedExchange.getResponse();
            int statusCode = response.getStatusCode() != null
                ? response.getStatusCode().value()
                : -1;

            // 慢请求告警
            if (duration > slowRequestThreshold) {
                log.warn("[Gateway] SLOW REQUEST [{}] {} {} - Status: {}, Duration: {}ms (threshold: {}ms)",
                    requestId, method, path, statusCode, duration, slowRequestThreshold);
            } else {
                log.info("[Gateway] [{}] {} {} - Status: {}, Duration: {}ms",
                    requestId, method, path, statusCode, duration);
            }
        }).doOnError(throwable -> {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[Gateway] [{}] {} {} - Error, Duration: {}ms, Error: {}",
                requestId, method, path, duration, throwable.getMessage());
        });
    }

    @Override
    public int getOrder() {
        // 在认证过滤器之后运行，确保日志包含认证信息
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
