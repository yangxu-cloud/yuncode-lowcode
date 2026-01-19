package com.yuncode.common.utils.web;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * 链路追踪上下文工具类
 * 用于生成和管理 TraceId、SpanId
 */
public class TraceIdContext {

    /**
     * TraceId 键名
     */
    public static final String TRACE_ID = "traceId";

    /**
     * SpanId 键名
     */
    public static final String SPAN_ID = "spanId";

    /**
     * 父 SpanId 键名
     */
    public static final String PARENT_SPAN_ID = "parentSpanId";

    /**
     * 生成唯一的 TraceId
     * 格式：雪花算法或 UUID
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成唯一的 SpanId
     */
    public static String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 设置 TraceId 到 MDC
     */
    public static void setTraceId(String traceId) {
        if (traceId != null && !traceId.isEmpty()) {
            MDC.put(TRACE_ID, traceId);
        } else {
            // 如果没有传入 traceId，生成新的
            MDC.put(TRACE_ID, generateTraceId());
        }
    }

    /**
     * 设置 SpanId 到 MDC
     */
    public static void setSpanId(String spanId) {
        if (spanId != null && !spanId.isEmpty()) {
            MDC.put(SPAN_ID, spanId);
        } else {
            MDC.put(SPAN_ID, generateSpanId());
        }
    }

    /**
     * 设置父 SpanId 到 MDC
     */
    public static void setParentSpanId(String parentSpanId) {
        if (parentSpanId != null && !parentSpanId.isEmpty()) {
            MDC.put(PARENT_SPAN_ID, parentSpanId);
        }
    }

    /**
     * 从 MDC 获取 TraceId
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    /**
     * 从 MDC 获取 SpanId
     */
    public static String getSpanId() {
        return MDC.get(SPAN_ID);
    }

    /**
     * 从 MDC 获取父 SpanId
     */
    public static String getParentSpanId() {
        return MDC.get(PARENT_SPAN_ID);
    }

    /**
     * 初始化新的链路追踪上下文
     * 用于请求开始时
     */
    public static void initContext() {
        clearContext();
        setTraceId(null);
        setSpanId(null);
    }

    /**
     * 从已有的 TraceId 创建子 Span
     * 用于异步调用或下游服务调用
     */
    public static void createChildSpan() {
        String currentSpanId = getSpanId();
        setParentSpanId(currentSpanId);
        setSpanId(null); // 生成新的 SpanId
    }

    /**
     * 清除 MDC 上下文
     * 用于请求结束时
     */
    public static void clearContext() {
        MDC.remove(TRACE_ID);
        MDC.remove(SPAN_ID);
        MDC.remove(PARENT_SPAN_ID);
    }

    /**
     * 从 HTTP 请求头中提取 TraceId
     * 支持分布式追踪
     */
    public static String extractTraceIdFromHeader(String traceIdHeader) {
        if (traceIdHeader != null && !traceIdHeader.isEmpty()) {
            return traceIdHeader;
        }
        return null;
    }
}
