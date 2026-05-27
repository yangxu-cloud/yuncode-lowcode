package com.yuncode.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuncode.common.model.util.response.Result;
import com.yuncode.common.utils.SecurityUtil;
import com.yuncode.system.entity.SysLoginLog;
import com.yuncode.system.service.LoginLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 登录日志控制器
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/log/user")
@Tag(name = "登录日志管理", description = "登录日志管理相关接口")
public class LoginLogController {

    private final LoginLogService loginLogService;

    /**
     * 分页查询登录日志
     *
     * @param page      页码
     * @param size      每页大小
     * @param username  用户名
     * @param status    登录状态
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 登录日志列表
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询登录日志", description = "根据条件分页查询登录日志列表")
    public Result<IPage<SysLoginLog>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        SecurityUtil.checkPlatformAdmin();

        Page<SysLoginLog> pageParam = new Page<>(page, size);
        IPage<SysLoginLog> pageResult = loginLogService.listLoginLogs(
                pageParam,
                null, // tenantId 可以从上下文获取
                username,
                status,
                startTime,
                endTime
        );

        return Result.success(pageResult);
    }

    /**
     * 根据ID查询登录日志详情
     *
     * @param id 日志ID
     * @return 登录日志详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询登录日志详情", description = "根据ID查询登录日志的详细信息")
    public Result<SysLoginLog> getById(@PathVariable Long id) {
        SecurityUtil.checkPlatformAdmin();
        SysLoginLog loginLog = loginLogService.getLoginLogById(id);
        if (loginLog == null) {
            return Result.error("登录日志不存在");
        }
        return Result.success(loginLog);
    }

    /**
     * 批量删除登录日志
     *
     * @param ids 日志ID列表
     * @return 删除结果
     */
    @DeleteMapping("/delete")
    @Operation(summary = "批量删除登录日志", description = "根据ID列表批量删除登录日志")
    public Result<Void> delete(@RequestBody Long[] ids) {
        SecurityUtil.checkPlatformAdmin();
        loginLogService.deleteLoginLogs(ids);
        return Result.success();
    }

    /**
     * 清空登录日志
     *
     * @return 清空结果
     */
    @DeleteMapping("/clean")
    @Operation(summary = "清空登录日志", description = "清空所有登录日志")
    public Result<Void> clean() {
        SecurityUtil.checkPlatformAdmin();
        loginLogService.cleanLoginLogs();
        return Result.success();
    }

    /**
     * 删除指定时间之前的日志
     *
     * @param beforeTime 时间点
     * @return 删除结果
     */
    @DeleteMapping("/clean/before")
    @Operation(summary = "删除历史登录日志", description = "删除指定时间之前的登录日志")
    public Result<Void> cleanBefore(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime beforeTime) {
        SecurityUtil.checkPlatformAdmin();
        loginLogService.deleteLoginLogsBefore(beforeTime);
        return Result.success();
    }
}
