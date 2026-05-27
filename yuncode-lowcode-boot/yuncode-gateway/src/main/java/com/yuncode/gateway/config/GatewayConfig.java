package com.yuncode.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关配置类
 */
@Configuration
public class GatewayConfig {

    /**
     * 根据 IP 限流
     */
    @Bean
    public KeyResolver remoteAddrKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown"
        );
    }

    /**
     * 根据用户限流（需要登录后使用）
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.justOrEmpty(
            exchange.getRequest().getHeaders().getFirst("userId")
        ).defaultIfEmpty("anonymous");
    }

    /**
     * 根据 API 路径限流
     */
    @Bean
    public KeyResolver apiKeyResolver() {
        return exchange -> Mono.justOrEmpty(
            exchange.getRequest().getPath().value()
        );
    }
}
