package com.yuncode.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuncode.common.model.util.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * IP 黑白名单过滤器
 *
 * 支持白名单和黑名单两种模式：
 * - 白名单模式：只有白名单中的 IP 可以访问
 * - 黑名单模式：黑名单中的 IP 被拒绝访问
 * 判断优先级：黑名单 > 白名单
 */
@Slf4j
@Component
public class IpAccessFilter implements GlobalFilter, Ordered {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final List<String> IP_WHITE_LIST = Arrays.asList(
        // 白名单路径（无需 IP 检查）
        "/api/auth/admin/login",
        "/api/auth/user/login",
        "/api/auth/tenant/login",
        "/actuator/health",
        "/actuator/info",
        "/doc.html",
        "/swagger-resources/**",
        "/v3/api-docs/**",
        "/webjars/**"
    );

    /** IP 白名单（逗号分隔，支持 CIDR 格式） */
    @Value("${gateway.ip-whitelist:}")
    private String ipWhitelist;

    /** IP 黑名单（逗号分隔，支持 CIDR 格式） */
    @Value("${gateway.ip-blacklist:}")
    private String ipBlacklist;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 白名单路径放行
        if (isWhiteListPath(path)) {
            return chain.filter(exchange);
        }

        String clientIp = getClientIp(exchange.getRequest());
        if (clientIp == null) {
            return chain.filter(exchange);
        }

        // 黑名单优先检查
        if (isInList(clientIp, ipBlacklist)) {
            log.warn("[IpAccess] {} - blacklisted, rejected", clientIp);
            return forbidden(exchange, "IP 已被列入黑名单");
        }

        // 白名单检查（白名单不为空时才启用白名单模式）
        if (!ipWhitelist.isEmpty() && !isInList(clientIp, ipWhitelist)) {
            log.warn("[IpAccess] {} - not in whitelist, rejected", clientIp);
            return forbidden(exchange, "IP 不在白名单中");
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String getClientIp(ServerHttpRequest request) {
        // 优先取 X-Forwarded-For 头（经过代理时）
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        // 直接取客户端 IP
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return null;
    }

    private boolean isInList(String ip, String list) {
        if (list == null || list.isEmpty()) return false;
        List<String> items = Arrays.stream(list.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
        for (String item : items) {
            if (item.contains("/")) {
                // CIDR 格式
                if (cidrMatch(ip, item)) return true;
            } else {
                // 精确匹配
                if (ip.equals(item)) return true;
            }
        }
        return false;
    }

    private boolean cidrMatch(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            int prefixLen = Integer.parseInt(parts[1]);
            long ipLong = ipToLong(ip);
            long cidrLong = ipToLong(parts[0]);
            long mask = prefixLen == 0 ? 0 : (0xFFFFFFFFL << (32 - prefixLen));
            return (ipLong & mask) == (cidrLong & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private long ipToLong(String ip) {
        String[] octets = ip.split("\\.");
        long result = 0;
        for (String octet : octets) {
            result = (result << 8) | Integer.parseInt(octet);
        }
        return result;
    }

    private boolean isWhiteListPath(String path) {
        return IP_WHITE_LIST.stream().anyMatch(pattern -> {
            if (pattern.endsWith("/**")) {
                return path.startsWith(pattern.substring(0, pattern.length() - 2));
            }
            return path.equals(pattern);
        });
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(Result.error(403, message));
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            byte[] bytes = ("{\"code\":403,\"message\":\"" + message + "\",\"data\":null}").getBytes();
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        }
    }
}
