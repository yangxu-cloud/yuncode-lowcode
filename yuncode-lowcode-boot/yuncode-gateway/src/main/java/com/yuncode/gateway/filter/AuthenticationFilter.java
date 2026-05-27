package com.yuncode.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuncode.common.model.util.response.Result;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Gateway 认证过滤器
 * 验证 JWT Token 签名，拒绝无效/过期 token
 *
 * 支持三种登录类型（admin/user/tenant），使用各自的 JWT 密钥验证。
 */
@Slf4j
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    /**
     * 白名单路径（无需认证）
     * 精确匹配；以 /** 结尾的按前缀匹配
     */
    private static final List<String> WHITE_LIST = Arrays.asList(
        "/api/auth/admin/login",
        "/api/auth/user/login",
        "/api/auth/tenant/login",
        "/api/auth/captcha",
        "/actuator/health",
        "/actuator/info",
        "/favicon.ico",
        "/doc.html",
        "/swagger-resources/**",
        "/v3/api-docs/**",
        "/webjars/**"
    );

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${sa-token.jwt-secret-key:yuncode-lowcode-default-key}")
    private String baseSecretKey;

    /** 三个登录类型的签名密钥 */
    private SecretKey adminKey;
    private SecretKey userKey;
    private SecretKey tenantKey;

    @PostConstruct
    public void initKeys() {
        adminKey = Keys.hmacShaKeyFor(baseSecretKey.getBytes(StandardCharsets.UTF_8));
        userKey = Keys.hmacShaKeyFor((baseSecretKey + "_user").getBytes(StandardCharsets.UTF_8));
        tenantKey = Keys.hmacShaKeyFor((baseSecretKey + "_tenant").getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单放行
        if (isWhiteList(path)) {
            log.debug("[Gateway] {} - whitelist, skip auth", path);
            return chain.filter(exchange);
        }

        // 提取 Token
        String token = getTokenFromHeader(request);
        if (token == null || token.isEmpty()) {
            log.debug("[Gateway] {} - no token, reject", path);
            return unauthorized(exchange, "缺少认证 token");
        }

        // 尝试验证 JWT 签名（依次尝试三个密钥）
        Claims claims = tryValidateToken(token);
        if (claims == null) {
            log.debug("[Gateway] {} - invalid token, reject", path);
            return unauthorized(exchange, "token 无效或已过期");
        }

        // Token 有效，提取用户信息并设置转发 header
        String loginId = claims.getSubject();
        String loginType = claims.get("loginType", String.class);
        if (loginId != null) {
            exchange.getRequest().mutate()
                    .header("X-User-Id", loginId)
                    .header("X-Login-Type", loginType != null ? loginType : "unknown");
        }

        log.debug("[Gateway] {} - token valid, userId={}, loginType={}", path, loginId, loginType);
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    /**
     * 尝试用三个密钥验证 JWT
     */
    private Claims tryValidateToken(String token) {
        // 先尝试 admin 密钥
        Claims claims = parseToken(token, adminKey);
        if (claims != null) return claims;

        // 再尝试 user 密钥
        claims = parseToken(token, userKey);
        if (claims != null) return claims;

        // 最后尝试 tenant 密钥
        return parseToken(token, tenantKey);
    }

    private Claims parseToken(String token, SecretKey key) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 返回 401 认证失败
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Result<?> result = Result.error(HttpStatus.UNAUTHORIZED.value(), message);
        try {
            byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(result);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            byte[] bytes = "{\"code\":401,\"message\":\"Unauthorized\",\"data\":null}".getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        }
    }

    private boolean isWhiteList(String path) {
        return WHITE_LIST.stream().anyMatch(pattern -> {
            if (pattern.endsWith("/**")) {
                String prefix = pattern.substring(0, pattern.length() - 2);
                return path.startsWith(prefix);
            }
            return path.equals(pattern);
        });
    }

    private String getTokenFromHeader(org.springframework.http.server.reactive.ServerHttpRequest request) {
        String token = request.getHeaders().getFirst("token");
        if (token == null) {
            String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authorization != null && authorization.startsWith("Bearer ")) {
                token = authorization.substring(7);
            }
        }
        return token;
    }
}
