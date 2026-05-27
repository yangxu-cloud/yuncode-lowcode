package com.yuncode.auth.controller;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.yuncode.auth.dto.LoginDTO;
import com.yuncode.auth.service.AdminLoginService;
import com.yuncode.auth.service.AuthService;
import com.yuncode.auth.service.TenantLoginService;
import com.yuncode.auth.service.UserLoginService;
import com.yuncode.auth.vo.LoginVO;
import com.yuncode.common.model.util.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证控制器
 */
@Tag(name = "认证接口", description = "用户登录、登出等认证相关接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final AdminLoginService adminLoginService;
    private final UserLoginService userLoginService;
    private final TenantLoginService tenantLoginService;
    private final List<StpLogic> stpLogics;

    /**
     * 管理员登录
     * 平台超级管理员登录，不需要租户编码
     */
    @Operation(summary = "平台超级管理员登录")
    @PostMapping("/admin/login")
    public Result<LoginVO> adminLogin(@Valid @RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        LoginVO loginVO = adminLoginService.login(loginDTO, request);
        return Result.success("管理员登录成功", loginVO);
    }

    /**
     * 用户登录
     * 普通用户登录，需要租户编码
     */
    @Operation(summary = "普通用户登录")
    @PostMapping("/user/login")
    public Result<LoginVO> userLogin(@Valid @RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        LoginVO loginVO = userLoginService.login(loginDTO, request);
        return Result.success("用户登录成功", loginVO);
    }

    /**
     * 租户登录
     * 租户管理员登录，需要租户编码
     */
    @Operation(summary = "租户管理员登录")
    @PostMapping("/tenant/login")
    public Result<LoginVO> tenantLogin(@Valid @RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        LoginVO loginVO = tenantLoginService.login(loginDTO, request);
        return Result.success("租户登录成功", loginVO);
    }

    /**
     * 用户登出
     */
    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.successWithoutData("登出成功");
    }

    /**
     * 检查登录状态
     * 使用 Sa-Token 的默认 JWT 模式
     */
    @Operation(summary = "检查登录状态")
    @GetMapping("/checkLogin")
    public Result<Boolean> checkLogin(HttpServletRequest request) {
        // 从请求头获取 token
        String authorizationHeader = request.getHeader("Authorization");
        log.info("[checkLogin] 开始检查登录状态");
        log.info("[checkLogin] Authorization Header 存在: {}", authorizationHeader != null);

        String tokenValue = null;
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            tokenValue = authorizationHeader.substring(7);
            log.info("[checkLogin] 提取到 Token: {}", tokenValue.substring(0, Math.min(20, tokenValue.length())) + "...");
            log.info("[checkLogin] Token 长度: {}", tokenValue.length());
        }

        // 如果没有 token，直接返回 false
        if (tokenValue == null || tokenValue.isEmpty()) {
            log.warn("[checkLogin] 未找到 token，返回 false");
            return Result.success(false);
        }

        // 使用 StpUtil 检查登录状态
        try {
            boolean isLoggedIn = StpUtil.isLogin();
            log.info("[checkLogin] StpUtil.isLogin(): {}", isLoggedIn);

            if (isLoggedIn) {
                Object loginId = StpUtil.getLoginIdDefaultNull();
                log.info("[checkLogin] 当前登录 ID: {}", loginId);
                return Result.success(true);
            } else {
                log.warn("[checkLogin] 未登录，返回 false");
                return Result.success(false);
            }
        } catch (Exception e) {
            log.warn("[checkLogin] 检查登录状态失败: {}", e.getMessage());
            return Result.success(false);
        }
    }

    /**
     * 获取当前用户信息
     */
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public Result<LoginVO> getCurrentUserInfo() {
        LoginVO userInfo = authService.getCurrentUserInfo();
        return Result.success("获取用户信息成功", userInfo);
    }

    /**
     * 刷新 Token
     * 验证当前 token 并重新签发（延长有效期）
     */
    @Operation(summary = "刷新 Token")
    @PostMapping("/refresh")
    public Result<LoginVO> refresh(HttpServletRequest request) {
        String tokenValue = request.getHeader("token");
        if (tokenValue == null || tokenValue.isEmpty()) {
            return Result.error(401, "缺少 token");
        }

        for (StpLogic stp : stpLogics) {
            try {
                // 尝试验证 token 是否属于此 StpLogic
                Object loginId = stp.getLoginIdByToken(tokenValue);
                if (loginId != null) {
                    // 重新登录以延长 token 有效期
                    stp.logoutByTokenValue(tokenValue);
                    stp.login(loginId);

                    LoginVO loginVO = new LoginVO();
                    loginVO.setToken(stp.getTokenValue());
                    loginVO.setTokenName(stp.getTokenName());
                    loginVO.setUserId(stp.getLoginIdAsLong());
                    log.info("Token 刷新成功: userId={}, loginType={}", loginId, stp.getLoginType());
                    return Result.success("Token 刷新成功", loginVO);
                }
            } catch (Exception e) {
                // 当前 StpLogic 不认领此 token，继续尝试下一个
                log.debug("StpLogic[{}] 不认领此 token: {}", stp.getLoginType(), e.getMessage());
            }
        }
        return Result.error(401, "token 无效或已过期");
    }
}
