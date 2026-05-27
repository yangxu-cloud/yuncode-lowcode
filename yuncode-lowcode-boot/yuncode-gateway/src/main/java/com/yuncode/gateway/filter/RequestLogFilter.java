package com.yuncode.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 请求日志过滤器
 */
@Slf4j
@Component
public class RequestLogFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        long startTime = System.currentTimeMillis();

        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        log.info("[Gateway] {} {} - Start", method, path);

        return chain.filter(exchange).doOnSuccess(aVoid -> {
            long duration = System.currentTimeMillis() - startTime;
            var response = exchange.getResponse();
            int statusCode = response.getStatusCode() != null
                ? response.getStatusCode().value()
                : -1;
            log.info("[Gateway] {} {} - Complete, Status: {}, Duration: {}ms",
                method, path, statusCode, duration);
        }).doOnError(throwable -> {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[Gateway] {} {} - Error, Duration: {}ms, Error: {}",
                method, path, duration, throwable.getMessage());
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
