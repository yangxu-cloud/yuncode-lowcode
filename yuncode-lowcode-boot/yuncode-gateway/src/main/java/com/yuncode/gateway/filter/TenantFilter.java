package com.yuncode.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 租户过滤器
 * 解析租户信息并传递给下游服务
 */
@Slf4j
@Component
public class TenantFilter implements GlobalFilter, Ordered {

    public static final String TENANT_ID_HEADER = "X-Tenant-Id";
    public static final String LOGIN_TYPE_HEADER = "X-Login-Type";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 从请求头获取租户信息
        String tenantId = request.getHeaders().getFirst(TENANT_ID_HEADER);
        String loginType = request.getHeaders().getFirst(LOGIN_TYPE_HEADER);

        // 禁止从请求参数读取 tenantId（安全原因：URL 参数会被记录在日志中）
        // 租户信息必须通过 X-Tenant-Id 请求头传递

        // 传递租户信息给下游（如果有）
        if (tenantId != null) {
            ServerHttpRequest modifiedRequest = request.mutate()
                .header(TENANT_ID_HEADER, tenantId)
                .build();
            log.debug("[Gateway] Pass tenantId: {} to downstream", tenantId);
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        }

        // 没有租户信息，直接继续
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // 在认证过滤器之后执行
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
