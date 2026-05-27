package com.yuncode.auth.controller;

import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Token 调试控制器
 */
@Profile("dev")
@Tag(name = "Token调试", description = "Token调试相关接口（仅 dev 环境）")
@RestController
@RequestMapping("/auth/debug")
public class TokenDebugController {

    /**
     * 调试 Token 读取情况
     */
    @Operation(summary = "调试Token读取情况", description = "用于调试和查看Token的读取状态")
    @GetMapping("/token-info")
    public Object debugToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Token Debug Info ===\n");
        sb.append("Authorization Header: ").append(authHeader != null ? authHeader.substring(0, Math.min(50, authHeader.length())) : "null").append("\n");
        sb.append("Token Name: ").append(StpUtil.getTokenName()).append("\n");
        sb.append("Token Value: ").append(StpUtil.getTokenValue() != null ? StpUtil.getTokenValue().substring(0, Math.min(50, StpUtil.getTokenValue().length())) : "null").append("\n");
        sb.append("Is Login: ").append(StpUtil.isLogin()).append("\n");

        if (StpUtil.isLogin()) {
            sb.append("Login ID: ").append(StpUtil.getLoginId()).append("\n");
            sb.append("Token Info: ").append(StpUtil.getTokenInfo()).append("\n");
        }

        return sb.toString();
    }

    /**
     * 检查登录状态
     */
    @Operation(summary = "检查登录状态", description = "检查当前用户的登录状态")
    @GetMapping("/check-login")
    public String checkLogin() {
        try {
            StpUtil.checkLogin();
            return "已登录 - Login ID: " + StpUtil.getLoginId();
        } catch (Exception e) {
            return "未登录 - " + e.getMessage();
        }
    }
}
