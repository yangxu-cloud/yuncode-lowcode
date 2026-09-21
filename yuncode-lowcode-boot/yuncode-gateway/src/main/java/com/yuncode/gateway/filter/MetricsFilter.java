package com.yuncode.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 请求指标过滤器
 *
 * 收集请求总数、成功数、失败数、按路径分组的计数和响应时间分布。
 * 通过 /gateway/metrics 端点暴露（需要在GatewayController中添加）。
 */
@Slf4j
@Component
public class MetricsFilter implements GlobalFilter, Ordered {

    /** 按路径分组的请求计数 */
    private final Map<String, PathMetrics> metricsMap = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        PathMetrics metrics = metricsMap.computeIfAbsent(getPathGroup(path), k -> new PathMetrics());
        metrics.totalCount.increment();

        long startTime = System.currentTimeMillis();

        return chain.filter(exchange).doOnSuccess(aVoid -> {
            long duration = System.currentTimeMillis() - startTime;
            metrics.successCount.increment();
            metrics.recordDuration(duration);
        }).doOnError(throwable -> {
            long duration = System.currentTimeMillis() - startTime;
            metrics.failCount.increment();
            metrics.recordDuration(duration);
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 200;
    }

    /**
     * 获取指标快照
     */
    public Map<String, Object> getMetricsSnapshot() {
        Map<String, Object> snapshot = new ConcurrentHashMap<>();
        long totalRequests = 0;
        long totalSuccess = 0;
        long totalFail = 0;

        for (Map.Entry<String, PathMetrics> entry : metricsMap.entrySet()) {
            PathMetrics m = entry.getValue();
            totalRequests += m.totalCount.sum();
            totalSuccess += m.successCount.sum();
            totalFail += m.failCount.sum();
            snapshot.put(entry.getKey(), m.toSnapshot());
        }

        snapshot.put("_total", Map.of(
            "requests", totalRequests,
            "success", totalSuccess,
            "fail", totalFail
        ));

        return snapshot;
    }

    private String getPathGroup(String path) {
        // 按 /api/{module}/** 分组
        if (path.startsWith("/api/")) {
            String[] parts = path.split("/");
            if (parts.length >= 3) {
                return "/api/" + parts[2];
            }
        }
        return path;
    }

    /** 路径级别指标 */
    public static class PathMetrics {
        final LongAdder totalCount = new LongAdder();
        final LongAdder successCount = new LongAdder();
        final LongAdder failCount = new LongAdder();
        final AtomicLong totalDuration = new AtomicLong(0);
        final AtomicLong maxDuration = new AtomicLong(0);
        final AtomicLong minDuration = new AtomicLong(Long.MAX_VALUE);

        void recordDuration(long ms) {
            totalDuration.addAndGet(ms);
            maxDuration.updateAndGet(m -> Math.max(m, ms));
            minDuration.updateAndGet(m -> Math.min(m, ms));
        }

        Map<String, Object> toSnapshot() {
            long total = totalCount.sum();
            return Map.of(
                "requests", total,
                "success", successCount.sum(),
                "fail", failCount.sum(),
                "avgDuration", total > 0 ? totalDuration.get() / total : 0,
                "maxDuration", maxDuration.get(),
                "minDuration", minDuration.get() == Long.MAX_VALUE ? 0 : minDuration.get()
            );
        }
    }
}
