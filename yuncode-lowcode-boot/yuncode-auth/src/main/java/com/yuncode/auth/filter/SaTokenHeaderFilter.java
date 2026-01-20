package com.yuncode.auth.filter;

import com.yuncode.auth.properties.SaTokenProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * Sa-Token 请求头处理过滤器
 * 将 Authorization header 中的 Bearer Token 转换为 Sa-Token 可以识别的格式
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class SaTokenHeaderFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(SaTokenHeaderFilter.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private SaTokenProperties saTokenProperties;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // 获取 Authorization header
        String authHeader = httpRequest.getHeader(AUTHORIZATION_HEADER);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            // 提取 Bearer token
            String token = authHeader.substring(BEARER_PREFIX.length());

            if (log.isDebugEnabled()) {
                log.debug("从 Authorization header 提取 Token: {}", token.substring(0, Math.min(20, token.length())) + "...");
            }

            // 从配置文件读取 token 名称
            String tokenHeader = saTokenProperties.getTokenName();

            // 创建一个包装请求，将 token 添加到配置的 header 名称
            HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(httpRequest) {
                @Override
                public String getHeader(String name) {
                    // 如果请求的是 token header，返回从 Authorization 提取的 token
                    if (tokenHeader.equalsIgnoreCase(name)) {
                        return token;
                    }
                    // 其他 header 保持不变
                    return super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if (tokenHeader.equalsIgnoreCase(name)) {
                        return new Vector<String>() {{
                            add(token);
                        }}.elements();
                    }
                    return super.getHeaders(name);
                }

                @Override
                public Enumeration<String> getHeaderNames() {
                    Set<String> names = new HashSet<>();
                    Enumeration<String> originalNames = super.getHeaderNames();
                    while (originalNames.hasMoreElements()) {
                        names.add(originalNames.nextElement());
                    }
                    names.add(tokenHeader);
                    return new Vector<>(names).elements();
                }
            };

            chain.doFilter(wrappedRequest, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("SaTokenHeaderFilter 初始化成功");
    }

    @Override
    public void destroy() {
        log.info("SaTokenHeaderFilter 销毁");
    }
}
