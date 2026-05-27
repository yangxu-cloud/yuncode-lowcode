package com.yuncode.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuncode.common.model.util.response.Result;
import com.yuncode.common.utils.SecurityUtil;
import com.yuncode.system.dto.OperationLogQueryDTO;
import com.yuncode.system.entity.SysOperationLog;
import com.yuncode.system.service.SysOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 操作日志控制器
 */
@Slf4j
@RestController
@RequestMapping("/log/operation")
@RequiredArgsConstructor
@Tag(name = "操作日志", description = "操作日志相关接口")
public class OperationLogController {

    private final SysOperationLogService operationLogService;

    /**
     * 分页查询操作日志
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询操作日志", description = "根据条件分页查询操作日志")
    public Result<Page<SysOperationLog>> getOperationLogPage(@Valid OperationLogQueryDTO dto) {
        SecurityUtil.checkPlatformAdmin();
        Page<SysOperationLog> page = operationLogService.getOperationLogPage(dto);
        return Result.success(page);
    }

    /**
     * 根据 TraceId 查询操作日志
     */
    @GetMapping("/trace/{traceId}")
    @Operation(summary = "根据 TraceId 查询操作日志", description = "查询指定 TraceId 的所有操作日志")
    public Result<List<SysOperationLog>> getLogsByTraceId(@PathVariable String traceId) {
        SecurityUtil.checkPlatformAdmin();
        List<SysOperationLog> logs = operationLogService.getLogsByTraceId(traceId);
        return Result.success(logs);
    }
}
