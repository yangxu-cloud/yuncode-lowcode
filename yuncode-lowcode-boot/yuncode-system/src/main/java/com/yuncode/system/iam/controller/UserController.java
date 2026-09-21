package com.yuncode.system.iam.controller;
import jakarta.validation.Valid;
import com.yuncode.common.exception.BusinessException;
import com.yuncode.common.exception.ErrorCode;

import com.yuncode.common.model.util.response.Result;
import com.yuncode.system.annotation.OperLog;
import com.yuncode.system.iam.entity.SysUser;
import com.yuncode.system.iam.service.UserService;
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
@RequestMapping("/system/user")
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
    @PostMapping
    @Operation(summary = "创建用户", description = "创建新用户")
    @OperLog(module = "用户管理", businessType = 1, description = "创建用户")
    public Result<Long> createUser(@Valid @RequestBody SysUser user) {
        log.info("创建用户, username={}, tenantId={}", user.getUsername(), user.getTenantId());
        Long userId = userService.createUser(user);
        return Result.success(userId);
    }

    /**
     * 根据用户名获取用户
     */
    @GetMapping("/by-username")
    @Operation(summary = "根据用户名获取用户", description = "根据用户名查询用户信息")
    public Result<SysUser> getUserByUsername(
            @Parameter(description = "用户名") @RequestParam String username) {
        log.info("根据用户名获取用户, username={}", username);
        SysUser user = userService.getUserByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return Result.success(user);
    }

    /**
     * 更新用户状态
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "更新用户状态", description = "启用或禁用用户")
    @OperLog(module = "用户管理", businessType = 2, description = "更新用户状态")
    public Result<Void> updateUserStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam Integer status) {
        log.info("更新用户状态, userId={}, status={}", id, status);
        userService.updateUserStatus(id, status);
        return Result.success();
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新用户信息", description = "更新用户基本信息")
    @OperLog(module = "用户管理", businessType = 2, description = "更新用户信息")
    public Result<Void> updateUser(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Valid @RequestBody SysUser user) {
        user.setId(id);
        log.info("更新用户信息, userId={}, username={}", user.getId(), user.getUsername());
        userService.updateUserInfo(user);
        return Result.success();
    }
}
