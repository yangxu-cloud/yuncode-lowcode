package com.yuncode.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuncode.common.model.util.response.Result;
import com.yuncode.system.entity.SysOperLog;
import com.yuncode.system.service.OperLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 操作日志控制器
 */
@RestController
@RequestMapping("/log/oper")
@Tag(name = "操作日志管理", description = "操作日志管理相关接口")
public class OperLogController {

    @Autowired
    private OperLogService operLogService;

    /**
     * 分页查询操作日志
     *
     * @param page          页码
     * @param pageSize      每页大小
     * @param module        模块名称
     * @param businessType  业务类型
     * @param operName      操作人员
     * @param status        操作状态
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @return 操作日志列表
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询操作日志", description = "根据条件分页查询操作日志列表")
    public Result<IPage<SysOperLog>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Integer businessType,
            @RequestParam(required = false) String operName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        Page<SysOperLog> pageParam = new Page<>(page, pageSize);
        IPage<SysOperLog> pageResult = operLogService.listOperLogs(
                pageParam,
                null, // tenantId 可以从上下文获取
                module,
                businessType,
                operName,
                status,
                startTime,
                endTime
        );

        return Result.success(pageResult);
    }

    /**
     * 根据ID查询操作日志详情
     *
     * @param id 日志ID
     * @return 操作日志详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询操作日志详情", description = "根据ID查询操作日志的详细信息")
    public Result<SysOperLog> getById(@PathVariable Long id) {
        SysOperLog operLog = operLogService.getOperLogById(id);
        if (operLog == null) {
            return Result.error("操作日志不存在");
        }
        return Result.success(operLog);
    }

    /**
     * 批量删除操作日志
     *
     * @param ids 日志ID列表
     * @return 删除结果
     */
    @DeleteMapping("/delete")
    @Operation(summary = "批量删除操作日志", description = "根据ID列表批量删除操作日志")
    public Result<Void> delete(@RequestBody Long[] ids) {
        operLogService.deleteOperLogs(ids);
        return Result.success();
    }

    /**
     * 清空操作日志
     *
     * @return 清空结果
     */
    @DeleteMapping("/clean")
    @Operation(summary = "清空操作日志", description = "清空所有操作日志")
    public Result<Void> clean() {
        operLogService.cleanOperLogs();
        return Result.success();
    }

    /**
     * 删除指定时间之前的日志
     *
     * @param beforeTime 时间点
     * @return 删除结果
     */
    @DeleteMapping("/clean/before")
    @Operation(summary = "删除历史操作日志", description = "删除指定时间之前的操作日志")
    public Result<Void> cleanBefore(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime beforeTime) {
        operLogService.deleteOperLogsBefore(beforeTime);
        return Result.success();
    }
}
