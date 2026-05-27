package com.yuncode.auth.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;

/**
 * 安全响应头过滤器 + 基础 CSRF 纵深防御
 *
 * <h3>安全响应头</h3>
 * <ul>
 *   <li>X-Content-Type-Options: nosniff — 禁用 MIME 类型嗅探</li>
 *   <li>X-Frame-Options: DENY — 禁止被嵌入 iframe（点击劫持）</li>
 *   <li>X-XSS-Protection: 0 — 显式禁用已废弃的 XSS 过滤器</li>
 *   <li>Strict-Transport-Security — 强制 HTTPS（max-age=1 年）</li>
 *   <li>Content-Security-Policy — 限制资源加载来源</li>
 *   <li>Referrer-Policy — 控制 Referer 头发送策略</li>
 *   <li>X-Permitted-Cross-Domain-Policies — 禁止 Flash 跨域</li>
 * </ul>
 *
 * <h3>CSRF 纵深防御</h3>
 * 本系统使用 header 传 token（配置 sa-token.is-read-cookie=false），
 * 标准 CSRF（依赖浏览器自动携带 cookie）无法实现。
 * 但作为纵深防御，对无 token 的写请求校验 Origin/Referer。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class SecurityHeadersFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(SecurityHeadersFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        setSecurityHeaders(httpResponse);

        // CSRF 纵深防御：对无 token 的写请求校验 Origin
        // （有 token 的请求已由 Sa-Token 保护，且浏览器无法跨域添加自定义 header）
        if (isStateChangingMethod(httpRequest.getMethod())) {
            String tokenHeader = httpRequest.getHeader("token");
            if (tokenHeader == null || tokenHeader.isEmpty()) {
                if (!isOriginAllowed(httpRequest)) {
                    log.warn("[CSRF] 请求被拦截: method={}, uri={}, origin={}",
                            httpRequest.getMethod(), httpRequest.getRequestURI(),
                            httpRequest.getHeader("Origin"));
                    httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF validation failed");
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }

    private void setSecurityHeaders(HttpServletResponse response) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "0");
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        response.setHeader("Content-Security-Policy", "default-src 'self'");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("X-Permitted-Cross-Domain-Policies", "none");
    }

    private static boolean isStateChangingMethod(String method) {
        return "POST".equals(method) || "PUT".equals(method)
                || "DELETE".equals(method) || "PATCH".equals(method);
    }

    private static boolean isOriginAllowed(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");

        // 无 Origin 也无 Referer（如 curl、内部调用），放行
        if (origin == null && referer == null) {
            return true;
        }

        String checkOrigin = origin;
        if (checkOrigin == null && referer != null) {
            try {
                URL url = new URL(referer);
                checkOrigin = url.getProtocol() + "://" + url.getAuthority();
            } catch (Exception e) {
                return false;
            }
        }

        return checkOrigin != null && isTrustedOrigin(checkOrigin);
    }

    private static boolean isTrustedOrigin(String origin) {
        return origin.equals("http://localhost:3000")
                || origin.equals("http://localhost:8080")
                || origin.equals("http://127.0.0.1:3000")
                || origin.equals("http://127.0.0.1:8080")
                || origin.startsWith("http://localhost:")
                || origin.startsWith("http://127.0.0.1:");
    }

    @Override
    public void init(FilterConfig filterConfig) {
        log.info("SecurityHeadersFilter 初始化成功");
    }

    @Override
    public void destroy() {
        log.info("SecurityHeadersFilter 销毁");
    }
}
