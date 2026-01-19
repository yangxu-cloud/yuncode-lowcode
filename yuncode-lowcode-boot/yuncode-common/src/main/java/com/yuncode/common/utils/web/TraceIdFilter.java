package com.yuncode.common.utils.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 链路追踪过滤器
 * 为每个请求生成唯一的 TraceId，并支持分布式追踪
 *
 * 工作原理：
 * 1. 请求开始时：从请求头中提取 TraceId，如果没有则生成新的
 * 2. 设置到 MDC 上下文中，供日志记录使用
 * 3. 请求结束时：清除 MDC 上下文，避免内存泄漏
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

    /**
     * HTTP 请求头中的 TraceId 键名
     * 支持常见的分布式追踪标准
     */
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String SKYWALKING_TRACE_ID_HEADER = "sw8-trace-id";
    private static final String B3_TRACE_ID_HEADER = "X-B3-TraceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            // 1. 尝试从请求头中提取 TraceId（支持分布式追踪）
            String traceId = extractTraceId(httpRequest);

            // 2. 设置 TraceId 到 MDC
            TraceIdContext.setTraceId(traceId);
            TraceIdContext.setSpanId(null); // 生成新的 SpanId

            // 3. 记录请求开始日志
            if (log.isDebugEnabled()) {
                log.debug("请求开始: {} {} | TraceId: {} | SpanId: {}",
                        httpRequest.getMethod(),
                        httpRequest.getRequestURI(),
                        TraceIdContext.getTraceId(),
                        TraceIdContext.getSpanId());
            }

            // 4. 将 TraceId 添加到响应头中，方便前端追踪
            if (response instanceof jakarta.servlet.http.HttpServletResponse) {
                ((jakarta.servlet.http.HttpServletResponse) response)
                        .setHeader(TRACE_ID_HEADER, TraceIdContext.getTraceId());
            }

            // 5. 继续处理请求
            chain.doFilter(request, response);

        } finally {
            // 6. 请求结束时清除 MDC，避免线程池复用时的上下文污染
            TraceIdContext.clearContext();

            if (log.isDebugEnabled()) {
                log.debug("请求结束: {} {} | 清除 TraceId 上下文",
                        httpRequest.getMethod(),
                        httpRequest.getRequestURI());
            }
        }
    }

    /**
     * 从请求头中提取 TraceId
     * 支持多种分布式追踪协议
     */
    private String extractTraceId(HttpServletRequest request) {
        // 优先使用自定义的 TraceId
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId != null && !traceId.isEmpty()) {
            return traceId;
        }

        // 支持 SkyWalking
        traceId = request.getHeader(SKYWALKING_TRACE_ID_HEADER);
        if (traceId != null && !traceId.isEmpty()) {
            return traceId;
        }

        // 支持 Zipkin B3
        traceId = request.getHeader(B3_TRACE_ID_HEADER);
        if (traceId != null && !traceId.isEmpty()) {
            return traceId;
        }

        // 如果请求头中没有，返回 null，将自动生成新的
        return null;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("TraceIdFilter 初始化成功");
    }

    @Override
    public void destroy() {
        log.info("TraceIdFilter 销毁");
    }
}
