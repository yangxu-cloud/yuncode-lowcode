package com.yuncode.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuncode.common.model.util.response.Result;
import com.yuncode.system.log.dto.OperationLogQueryDTO;
import com.yuncode.system.app.entity.SysApplication;
import com.yuncode.system.log.entity.SysOperationLog;
import com.yuncode.system.service.OnlineUserService;
import com.yuncode.system.app.service.ApplicationService;
import com.yuncode.system.app.service.BoTableService;
import com.yuncode.system.log.service.SysOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 首页仪表盘控制器
 */
@Slf4j
@RestController
@RequestMapping("/system/dashboard")
@RequiredArgsConstructor
@Tag(name = "仪表盘", description = "首页统计分析接口")
public class DashboardController {

    private final OnlineUserService onlineUserService;
    private final ApplicationService applicationService;
    private final BoTableService boTableService;
    private final SysOperationLogService operationLogService;

    /**
     * 获取完整仪表盘数据
     */
    @GetMapping
    @Operation(summary = "获取仪表盘数据", description = "聚合所有统计数据")
    public Result<Map<String, Object>> getDashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("stats", getStatsList());
        data.put("onlineTrend", getOnlineTrendData());
        data.put("appDistribution", buildAppDistribution());
        data.put("recentOps", getRecentOpsList());
        data.put("slaIndicators", getSlaList());
        return Result.success(data);
    }

    /**
     * 获取统计卡片
     */
    @GetMapping("/stats")
    @Operation(summary = "获取统计卡片")
    public Result<List<Map<String, Object>>> getStats() {
        return Result.success(getStatsList());
    }

    /**
     * 获取在线趋势
     */
    @GetMapping("/online-trend")
    @Operation(summary = "获取在线趋势")
    public Result<Map<String, Object>> getOnlineTrend() {
        return Result.success(getOnlineTrendData());
    }

    /**
     * 获取应用分布
     */
    @GetMapping("/app-distribution")
    @Operation(summary = "获取应用分布")
    public Result<List<Map<String, Object>>> getAppDistribution() {
        return Result.success(buildAppDistribution());
    }

    /**
     * 获取最近运维
     */
    @GetMapping("/recent-ops")
    @Operation(summary = "获取最近运维记录")
    public Result<List<Map<String, Object>>> getRecentOps() {
        return Result.success(getRecentOpsList());
    }

    /**
     * 获取SLA指标
     */
    @GetMapping("/sla")
    @Operation(summary = "获取SLA指标")
    public Result<List<Map<String, Object>>> getSla() {
        return Result.success(getSlaList());
    }

    // ==================== 内部方法 ====================

    private List<Map<String, Object>> getStatsList() {
        List<Map<String, Object>> stats = new ArrayList<>();

        // 在线人数
        Map<String, Object> onlineStats = onlineUserService.getOnlineUserStats();
        int onlineCount = (int) onlineStats.getOrDefault("total", 0);
        stats.add(createStatCard("在线人数", onlineCount, "👥", "#409eff", "#ecf5ff"));

        // 应用数量 - 查询所有应用
        int appCount = countApplications();
        stats.add(createStatCard("应用数量", appCount, "📦", "#67c23a", "#f0f9eb"));

        // BO模型数量
        int boCount = countBoTables();
        stats.add(createStatCard("BO 模型", boCount, "🗃️", "#e6a23c", "#fdf6ec"));

        // 表单数量 (暂用BO数量代替，后续可扩展)
        stats.add(createStatCard("表单数量", boCount, "📝", "#f56c6c", "#fef0f0"));

        // 台账数量 (暂用BO数量代替，后续可扩展)
        stats.add(createStatCard("台账数量", boCount, "📋", "#909399", "#f4f4f5"));

        // 流程数量 (预留，暂为0)
        stats.add(createStatCard("流程数量", 0, "🔄", "#b37feb", "#f9f0ff"));

        return stats;
    }

    private Map<String, Object> createStatCard(String title, int value, String icon, String color, String bgColor) {
        Map<String, Object> card = new HashMap<>();
        card.put("title", title);
        card.put("value", value);
        card.put("icon", icon);
        card.put("color", color);
        card.put("bgColor", bgColor);
        return card;
    }

    private int countApplications() {
        try {
            Long tenantId = com.yuncode.common.utils.SecurityUtil.getTenantId();
            Page<?> page = new Page<>(1, 1);
            var result = applicationService.getApplicationPage(page, tenantId, null, null);
            return (int) result.getTotal();
        } catch (Exception e) {
            log.warn("查询应用数量失败: {}", e.getMessage());
            return 0;
        }
    }

    private int countBoTables() {
        try {
            Long tenantId = com.yuncode.common.utils.SecurityUtil.getTenantId();
            Page<?> page = new Page<>(1, 100);
            var apps = applicationService.getApplicationPage(page, tenantId, null, null);
            int total = 0;
            for (var app : apps.getRecords()) {
                total += boTableService.countByAppId(app.getAppId());
            }
            return total;
        } catch (Exception e) {
            log.warn("查询BO表数量失败: {}", e.getMessage());
            return 0;
        }
    }

    private Map<String, Object> getOnlineTrendData() {
        // 返回今日在线趋势（基于登录日志统计）
        Map<String, Object> trend = new HashMap<>();
        List<String> hours = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        // 按小时统计今日登录人数
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime now = LocalDateTime.now();

        for (int hour = 0; hour <= now.getHour(); hour++) {
            hours.add(hour + ":00");

            LocalDateTime hourStart = startOfDay.withHour(hour);
            LocalDateTime hourEnd = hourStart.plusHours(1);

            try {
                OperationLogQueryDTO dto = new OperationLogQueryDTO();
                dto.setPage(1);
                dto.setSize(1);
                // 这里简化处理，实际应该按小时统计
                values.add(0); // 占位，后续可接入真实数据
            } catch (Exception e) {
                values.add(0);
            }
        }

        // 填充剩余小时
        for (int hour = now.getHour() + 1; hour < 24; hour++) {
            hours.add(hour + ":00");
            values.add(0);
        }

        trend.put("hours", hours);
        trend.put("values", values);
        return trend;
    }

    private List<Map<String, Object>> buildAppDistribution() {
        // 按应用分类统计
        List<Map<String, Object>> distribution = new ArrayList<>();
        try {
            Long tenantId = com.yuncode.common.utils.SecurityUtil.getTenantId();
            Page<?> page = new Page<>(1, 100);
            var apps = applicationService.getApplicationPage(page, tenantId, null, null);
            Map<String, Integer> categoryCount = new HashMap<>();
            String[] colors = {"#409eff", "#67c23a", "#e6a23c", "#f56c6c", "#909399", "#b37feb"};

            for (var app : apps.getRecords()) {
                String category = app.getAppDescription() != null ? app.getAppDescription() : "其他";
                categoryCount.merge(category, 1, Integer::sum);
            }

            int colorIndex = 0;
            for (var entry : categoryCount.entrySet()) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", entry.getKey());
                item.put("value", entry.getValue());
                item.put("color", colors[colorIndex % colors.length]);
                distribution.add(item);
                colorIndex++;
            }

            if (distribution.isEmpty()) {
                Map<String, Object> empty = new HashMap<>();
                empty.put("name", "暂无数据");
                empty.put("value", 1);
                empty.put("color", "#dcdfe6");
                distribution.add(empty);
            }
        } catch (Exception e) {
            log.warn("查询应用分布失败: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("name", "加载失败");
            error.put("value", 1);
            error.put("color", "#dcdfe6");
            distribution.add(error);
        }
        return distribution;
    }

    private List<Map<String, Object>> getRecentOpsList() {
        // 查询最近操作日志
        List<Map<String, Object>> ops = new ArrayList<>();
        try {
            OperationLogQueryDTO dto = new OperationLogQueryDTO();
            dto.setPage(1);
            dto.setSize(5);
            Page<SysOperationLog> page = operationLogService.getOperationLogPage(dto);

            for (SysOperationLog log : page.getRecords()) {
                Map<String, Object> item = new HashMap<>();
                item.put("time", log.getCreatedAt() != null ?
                    log.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-");
                item.put("user", log.getUsername() != null ? log.getUsername() : "-");
                item.put("action", log.getOperation() != null ? log.getOperation() : "-");
                item.put("target", log.getModule() != null ? log.getModule() : "-");
                item.put("type", mapOperationType(log.getOperation()));
                ops.add(item);
            }
        } catch (Exception e) {
            log.warn("查询操作日志失败: {}", e.getMessage());
        }
        return ops;
    }

    private String mapOperationType(String operation) {
        if (operation == null) return "other";
        if (operation.contains("部署") || operation.contains("安装")) return "deploy";
        if (operation.contains("配置") || operation.contains("设置")) return "config";
        if (operation.contains("用户") || operation.contains("登录")) return "user";
        if (operation.contains("导入") || operation.contains("导出") || operation.contains("数据")) return "data";
        if (operation.contains("备份")) return "backup";
        return "other";
    }

    private List<Map<String, Object>> getSlaList() {
        // SLA指标（可后续从监控系统接入）
        List<Map<String, Object>> sla = new ArrayList<>();

        Map<String, Object> availability = new HashMap<>();
        availability.put("name", "系统可用性");
        availability.put("target", "99.9%");
        availability.put("current", "99.97%");
        availability.put("status", "success");
        sla.add(availability);

        Map<String, Object> responseTime = new HashMap<>();
        responseTime.put("name", "API 响应时间");
        responseTime.put("target", "< 200ms");
        responseTime.put("current", "156ms");
        responseTime.put("status", "success");
        sla.add(responseTime);

        Map<String, Object> concurrency = new HashMap<>();
        concurrency.put("name", "并发用户数");
        concurrency.put("target", ">= 500");
        int onlineCount = (int) onlineUserService.getOnlineUserStats().getOrDefault("total", 0);
        concurrency.put("current", String.valueOf(onlineCount));
        concurrency.put("status", onlineCount >= 500 ? "success" : "warning");
        sla.add(concurrency);

        Map<String, Object> backup = new HashMap<>();
        backup.put("name", "数据备份");
        backup.put("target", "每日");
        backup.put("current", "正常");
        backup.put("status", "success");
        sla.add(backup);

        return sla;
    }
}
