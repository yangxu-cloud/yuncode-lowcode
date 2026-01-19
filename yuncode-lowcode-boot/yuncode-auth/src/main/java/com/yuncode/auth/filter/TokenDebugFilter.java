package com.yuncode.auth.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Token 调试过滤器
 * 用于调试 JWT Token 的读取情况
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TokenDebugFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TokenDebugFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // 记录 Authorization header
        String authHeader = httpRequest.getHeader("Authorization");
        String token = httpRequest.getHeader("satoken");

        if (log.isDebugEnabled()) {
            log.debug("=== Token Debug ===");
            log.debug("请求路径: {}", httpRequest.getRequestURI());
            log.debug("Authorization Header: {}", authHeader != null ? authHeader.substring(0, Math.min(50, authHeader.length())) + "..." : "null");
            log.debug("satoken Header: {}", token != null ? token.substring(0, Math.min(50, token.length())) + "..." : "null");
            log.debug("=================");
        }

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("TokenDebugFilter 初始化成功");
    }

    @Override
    public void destroy() {
        log.info("TokenDebugFilter 销毁");
    }
}
