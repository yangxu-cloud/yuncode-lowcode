package com.yuncode.system.controller;

import com.yuncode.common.model.util.response.Result;
import com.yuncode.system.annotation.OperLog;
import com.yuncode.system.entity.SysUser;
import com.yuncode.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 *
 * @author Yuncode
 * @since 2025-01-27
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户管理相关接口")
public class UserController {

    private final UserService userService;

    /**
     * 创建用户
     *
     * @param user 用户信息
     * @return 创建结果
     */
    @PostMapping("/create")
    @Operation(summary = "创建用户", description = "创建新用户")
    @OperLog(module = "用户管理", businessType = 1, description = "创建用户")
    public Result<Long> createUser(@RequestBody SysUser user) {
        log.info("创建用户, username={}, tenantId={}", user.getUsername(), user.getTenantId());
        try {
            Long userId = userService.createUser(user);
            return Result.success(userId);
        } catch (RuntimeException e) {
            log.error("创建用户失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据用户名获取用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    @GetMapping("/by-username")
    @Operation(summary = "根据用户名获取用户", description = "根据用户名查询用户信息")
    public Result<SysUser> getUserByUsername(
            @Parameter(description = "用户名") @RequestParam String username) {
        log.info("根据用户名获取用户, username={}", username);
        // 多租户插件会自动添加 tenant_id 条件
        SysUser user = userService.getUserByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    /**
     * 更新用户状态
     *
     * @param userId 用户ID
     * @param status 状态
     * @return 更新结果
     */
    @PutMapping("/status")
    @Operation(summary = "更新用户状态", description = "启用或禁用用户")
    @OperLog(module = "用户管理", businessType = 2, description = "更新用户状态")
    public Result<Void> updateUserStatus(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "状态") @RequestParam Integer status) {
        log.info("更新用户状态, userId={}, status={}", userId, status);
        try {
            // 多租户插件会自动添加 tenant_id 条件进行校验
            userService.updateUserStatus(userId, status);
            return Result.success();
        } catch (RuntimeException e) {
            log.error("更新用户状态失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新用户信息
     *
     * @param user 用户信息
     * @return 更新结果
     */
    @PutMapping("/update")
    @Operation(summary = "更新用户信息", description = "更新用户基本信息")
    @OperLog(module = "用户管理", businessType = 2, description = "更新用户信息")
    public Result<Void> updateUser(@RequestBody SysUser user) {
        log.info("更新用户信息, userId={}, username={}", user.getId(), user.getUsername());
        try {
            userService.updateUserInfo(user);
            return Result.success();
        } catch (RuntimeException e) {
            log.error("更新用户信息失败", e);
            return Result.error(e.getMessage());
        }
    }
}
