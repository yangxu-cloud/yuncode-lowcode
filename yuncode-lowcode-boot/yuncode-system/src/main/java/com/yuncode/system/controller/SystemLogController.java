package com.yuncode.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuncode.common.model.util.response.Result;
import com.yuncode.system.entity.SysSystemLog;
import com.yuncode.system.service.SysSystemLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 系统日志控制器
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/log/system")
@Tag(name = "系统日志管理", description = "系统日志管理相关接口")
public class SystemLogController {

    private final SysSystemLogService sysSystemLogService;

    /**
     * 分页查询系统日志
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询系统日志", description = "根据条件分页查询系统日志列表")
    public Result<Page<SysSystemLog>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String traceId) {

        Page<SysSystemLog> pageParam = new Page<>(page, size);
        Page<SysSystemLog> result = sysSystemLogService.listLogs(pageParam, level, module, message, startTime, endTime, traceId);

        return Result.success(result);
    }

    /**
     * 删除系统日志
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除系统日志", description = "根据ID删除单条系统日志")
    public Result<Void> delete(@PathVariable Long id) {
        sysSystemLogService.removeById(id);
        return Result.success();
    }

    /**
     * 批量删除系统日志
     */
    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除系统日志", description = "根据ID列表批量删除系统日志")
    public Result<Void> batchDelete(@RequestBody Long[] ids) {
        sysSystemLogService.removeBatchByIds(Arrays.asList(ids));
        return Result.success();
    }

    /**
     * 清空过期日志
     */
    @PostMapping("/clean")
    @Operation(summary = "清空过期系统日志", description = "清空指定天数之前的过期系统日志")
    public Result<Void> cleanExpired(@RequestParam Integer days) {
        // 计算过期时间
        String expireTime = LocalDateTime.now().minusDays(days)
                .toString();

        LambdaQueryWrapper<SysSystemLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.le(SysSystemLog::getCreatedAt, expireTime);

        sysSystemLogService.remove(wrapper);
        return Result.success();
    }
}
