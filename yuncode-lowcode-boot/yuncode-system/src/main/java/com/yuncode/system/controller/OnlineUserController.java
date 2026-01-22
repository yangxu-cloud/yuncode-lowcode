package com.yuncode.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuncode.common.model.util.response.Result;
import com.yuncode.system.annotation.OperLog;
import com.yuncode.system.entity.OnlineUser;
import com.yuncode.system.service.OnlineUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 在线用户管理控制器
 */
@Tag(name = "在线用户管理", description = "在线用户管理相关接口")
@RestController
@RequestMapping("/system/online-users")
public class OnlineUserController {

    @Autowired
    private OnlineUserService onlineUserService;

    /**
     * 获取在线用户列表
     */
    @Operation(summary = "获取在线用户列表", description = "分页查询在线用户列表，支持按用户名和租户筛选")
    @GetMapping
    public Result<Page<OnlineUser>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Long tenantId) {

        Page<OnlineUser> pageParam = new Page<>(page, size);
        Page<OnlineUser> result = onlineUserService.listOnlineUsers(pageParam, username, tenantId);

        return Result.success(result);
    }

    /**
     * 获取在线用户统计
     */
    @Operation(summary = "获取在线用户统计", description = "获取在线用户的统计信息")
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> stats = onlineUserService.getOnlineUserStats();
        return Result.success(stats);
    }

    /**
     * 踢出在线用户
     */
    @OperLog(module = "在线用户", businessType = 3, description = "踢出在线用户")
    @Operation(summary = "踢出在线用户", description = "根据会话ID踢出指定的在线用户")
    @PostMapping("/{sessionId}/kick")
    public Result<String> kickOut(@PathVariable String sessionId) {
        onlineUserService.kickOutUser(sessionId);
        return Result.success("踢出用户成功");
    }

    /**
     * 批量踢出用户
     */
    @OperLog(module = "在线用户", businessType = 3, description = "批量踢出在线用户")
    @Operation(summary = "批量踢出用户", description = "批量踢出多个在线用户")
    @PostMapping("/batch-kick")
    public Result<String> batchKickOut(@RequestBody List<String> sessionIds) {
        onlineUserService.batchKickOutUsers(sessionIds);
        return Result.success("批量踢出用户成功");
    }

    /**
     * 获取当前在线用户信息
     */
    @Operation(summary = "获取当前在线用户信息", description = "获取当前登录用户的在线信息")
    @GetMapping("/current")
    public Result<OnlineUser> getCurrentUser() {
        String sessionId = StpUtil.getSession().get("sessionId", "");
        if (sessionId == null || sessionId.isEmpty()) {
            return Result.error("未找到会话信息");
        }
        OnlineUser user = onlineUserService.getOnlineUser(sessionId);
        return Result.success(user);
    }
}
