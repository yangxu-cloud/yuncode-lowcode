package com.yuncode.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Gateway 认证标记过滤器
 *
 * 当请求经过 Gateway 时，Gateway 已完成 JWT 验证并将用户信息写入请求头。
 * 本过滤器标记该请求已经过 Gateway 认证，后续可通过 SecurityUtil 读取。
 *
 * 认证链：Gateway(JWT验证) → 本过滤器(标记) → API控制器(读标记)
 * 避免后端重复解析 JWT。
 */
@Slf4j
@Component
@Order(-200)
public class GatewayAuthFilter implements Filter {

    public static final String GATEWAY_AUTH_HEADER = "X-Gateway-Auth";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String LOGIN_TYPE_HEADER = "X-Login-Type";
    public static final String TENANT_ID_HEADER = "X-Tenant-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String gatewayAuth = httpRequest.getHeader(GATEWAY_AUTH_HEADER);

        if ("true".equalsIgnoreCase(gatewayAuth)) {
            // 将 Gateway 认证标记写入请求属性，供后续逻辑读取
            request.setAttribute(GATEWAY_AUTH_HEADER, "true");
            request.setAttribute(USER_ID_HEADER, httpRequest.getHeader(USER_ID_HEADER));
            request.setAttribute(LOGIN_TYPE_HEADER, httpRequest.getHeader(LOGIN_TYPE_HEADER));
            request.setAttribute(TENANT_ID_HEADER, httpRequest.getHeader(TENANT_ID_HEADER));

            if (log.isDebugEnabled()) {
                log.debug("[GatewayAuth] Gateway认证请求, userId={}, loginType={}",
                    httpRequest.getHeader(USER_ID_HEADER),
                    httpRequest.getHeader(LOGIN_TYPE_HEADER));
            }
        }

        chain.doFilter(request, response);
    }
}
