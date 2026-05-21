package com.yuncode.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuncode.common.model.util.response.Result;
import com.yuncode.common.utils.SecurityUtil;
import com.yuncode.system.annotation.OperLog;
import com.yuncode.system.dto.ApplicationForm;
import com.yuncode.system.entity.SysApplication;
import com.yuncode.system.entity.SysApplicationLog;
import com.yuncode.system.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应用管理控制器
 *
 * @author Yuncode
 * @since 2025-01-30
 */
@Slf4j
@RestController
@RequestMapping("/system/application")
@RequiredArgsConstructor
@Tag(name = "应用管理", description = "应用管理接口")
public class ApplicationController {

    private final ApplicationService applicationService;

    /**
     * 分页查询应用列表
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询应用列表", description = "分页查询应用列表，支持按应用ID和名称搜索")
    @OperLog(module = "应用管理", businessType = 1, description = "查询应用列表")
    public Result<IPage<SysApplication>> getApplicationList(
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词（支持应用ID和应用名称）") @RequestParam(required = false) String appName) {

        Long tenantId = SecurityUtil.getTenantId();
        Page<?> page = new Page<>(current, size);
        IPage<SysApplication> result = applicationService.getApplicationPage(page, tenantId, appName);
        return Result.success(result);
    }

    /**
     * 获取应用详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取应用详情", description = "根据ID获取应用详情")
    public Result<SysApplication> getApplication(@PathVariable Long id) {
        SysApplication application = applicationService.getApplicationById(id);
        return Result.success(application);
    }

    /**
     * 创建应用
     */
    @PostMapping("/create")
    @Operation(summary = "创建应用", description = "创建新应用")
    @OperLog(module = "应用管理", businessType = 1, description = "创建应用")
    public Result<Void> createApplication(@RequestBody ApplicationForm form) {
        try {
            applicationService.createApplication(form);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新应用
     */
    @PutMapping("/update")
    @Operation(summary = "更新应用", description = "更新应用信息")
    @OperLog(module = "应用管理", businessType = 2, description = "更新应用")
    public Result<Void> updateApplication(@RequestBody ApplicationForm form) {
        try {
            applicationService.updateApplication(form);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除应用
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除应用", description = "删除应用")
    @OperLog(module = "应用管理", businessType = 3, description = "删除应用")
    public Result<Void> deleteApplication(@PathVariable Long id) {
        try {
            applicationService.deleteApplication(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 启动应用
     */
    @PostMapping("/start/{id}")
    @Operation(summary = "启动应用", description = "启动应用")
    @OperLog(module = "应用管理", businessType = 4, description = "启动应用")
    public Result<Void> startApplication(@PathVariable Long id) {
        try {
            applicationService.startApplication(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 停止应用
     */
    @PostMapping("/stop/{id}")
    @Operation(summary = "停止应用", description = "停止应用")
    @OperLog(module = "应用管理", businessType = 5, description = "停止应用")
    public Result<Void> stopApplication(@PathVariable Long id) {
        try {
            applicationService.stopApplication(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 安装应用
     */
    @PostMapping("/install/{id}")
    @Operation(summary = "安装应用", description = "安装应用")
    @OperLog(module = "应用管理", businessType = 6, description = "安装应用")
    public Result<Void> installApplication(@PathVariable Long id) {
        try {
            applicationService.installApplication(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 卸载应用
     */
    @PostMapping("/uninstall/{id}")
    @Operation(summary = "卸载应用", description = "卸载应用")
    @OperLog(module = "应用管理", businessType = 7, description = "卸载应用")
    public Result<Void> uninstallApplication(@PathVariable Long id) {
        try {
            applicationService.uninstallApplication(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 升级应用
     */
    @PostMapping("/upgrade/{id}")
    @Operation(summary = "升级应用", description = "升级应用")
    @OperLog(module = "应用管理", businessType = 8, description = "升级应用")
    public Result<Void> upgradeApplication(@PathVariable Long id) {
        try {
            applicationService.upgradeApplication(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分页查询应用日志
     */
    @GetMapping("/logs")
    @Operation(summary = "分页查询应用日志", description = "分页查询应用操作日志")
    @OperLog(module = "应用管理", businessType = 1, description = "查询应用日志")
    public Result<IPage<SysApplicationLog>> getApplicationLogs(
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "应用ID") @RequestParam(required = false) Long appId,
            @Parameter(description = "操作类型") @RequestParam(required = false) Integer operationType) {

        Page<?> page = new Page<>(current, size);
        IPage<SysApplicationLog> result = applicationService.getApplicationLogPage(page, appId, operationType);
        return Result.success(result);
    }
}