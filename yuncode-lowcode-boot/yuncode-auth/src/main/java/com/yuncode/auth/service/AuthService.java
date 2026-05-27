package com.yuncode.auth.service;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.yuncode.auth.vo.LoginVO;
import com.yuncode.system.service.LoginLogService;
import com.yuncode.system.service.OnlineUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证服务
 */
@Slf4j
@Service
public class AuthService {

    private final LoginLogService loginLogService;
    private final OnlineUserService onlineUserService;
    private final List<StpLogic> stpLogics;

    public AuthService(
            LoginLogService loginLogService,
            OnlineUserService onlineUserService,
            List<StpLogic> stpLogics) {
        this.loginLogService = loginLogService;
        this.onlineUserService = onlineUserService;
        this.stpLogics = stpLogics;
    }

    /**
     * 根据当前 token 获取对应的 StpLogic
     * 遍历所有 StpLogic，找到当前 token 对应的那个
     */
    private StpLogic getCurrentStpLogic() {
        // 获取当前请求的 token 值
        String tokenValue = null;
        try {
            tokenValue = StpUtil.getTokenValue();
        } catch (Exception e) {
            log.debug("无法从 StpUtil 获取 token: {}", e.getMessage());
        }

        // 如果获取到了 token，遍历所有 StpLogic 找到能验证这个 token 的那个
        if (tokenValue != null && !tokenValue.isEmpty()) {
            for (StpLogic stpLogic : stpLogics) {
                if ("login".equals(stpLogic.getLoginType()) || stpLogic.getLoginType() == null) {
                    continue;
                }

                try {
                    if (stpLogic.isLogin()) {
                        Object loginId = stpLogic.getLoginIdDefaultNull();
                        if (loginId != null) {
                            String stpToken = stpLogic.getTokenValue();
                            if (tokenValue.equals(stpToken)) {
                                log.debug("通过 token 匹配到 StpLogic: {}, loginId: {}", stpLogic.getLoginType(), loginId);
                                return stpLogic;
                            }
                        }
                    }
                } catch (Exception e) {
                    // 忽略异常，继续检查下一个
                }
            }
        }

        // 如果通过 token 没找到，按顺序遍历所有 StpLogic，返回第一个已登录的
        for (StpLogic stpLogic : stpLogics) {
            if ("login".equals(stpLogic.getLoginType()) || stpLogic.getLoginType() == null) {
                continue;
            }

            try {
                if (stpLogic.isLogin()) {
                    Object loginId = stpLogic.getLoginIdDefaultNull();
                    if (loginId != null) {
                        log.debug("使用已登录的 StpLogic: {}, loginId: {}", stpLogic.getLoginType(), loginId);
                        return stpLogic;
                    }
                }
            } catch (Exception e) {
                // 忽略异常
            }
        }

        // 最后的 fallback：返回第一个自定义的 StpLogic
        for (StpLogic stpLogic : stpLogics) {
            if (!"login".equals(stpLogic.getLoginType()) && stpLogic.getLoginType() != null) {
                log.warn("未找到已登录的 StpLogic，使用默认: {}", stpLogic.getLoginType());
                return stpLogic;
            }
        }

        return stpLogics.get(0);
    }

    /**
     * 用户登出
     */
    public void logout() {
        StpLogic stpLogic = getCurrentStpLogic();

        String sessionId = null;
        String token = null;
        try {
            token = stpLogic.getTokenValue();
            sessionId = stpLogic.getSession().get("sessionId", "");
            log.info("[Logout] 开始登出流程，sessionId={}, token (前32位): {}",
                sessionId, token != null ? token.substring(0, Math.min(32, token.length())) : "null");
        } catch (Exception e) {
            log.warn("[Logout] 获取 token 或 sessionId 失败: {}", e.getMessage());
        }

        try {
            Long userId = stpLogic.getLoginIdAsLong();
            String username = stpLogic.getSession().get("username", "");

            loginLogService.updateLogoutTime(username, LocalDateTime.now());

            log.info("[Logout] 用户登出成功: userId={}, username={}, loginType={}",
                    userId, username, stpLogic.getLoginType());
        } catch (cn.dev33.satoken.exception.NotLoginException e) {
            log.debug("[Logout] 登出时 token 已失效: {}", e.getMessage());
        } catch (Exception e) {
            log.error("[Logout] 更新登出时间失败", e);
        } finally {
            try {
                if (sessionId != null && !sessionId.isEmpty()) {
                    log.info("[Logout] 准备移除在线用户记录，sessionId={}", sessionId);
                    onlineUserService.removeOnlineUser(sessionId);
                    log.info("[Logout] 已移除在线用户记录，sessionId={}", sessionId);
                } else {
                    log.warn("[Logout] sessionId 为空，无法移除在线用户记录");
                }
            } catch (Exception e) {
                log.error("[Logout] 移除在线用户记录失败: {}", e.getMessage(), e);
            }

            try {
                stpLogic.logout();
                log.info("[Logout] StpLogic.logout() 执行成功");
            } catch (Exception e) {
                log.debug("[Logout] 执行 logout 失败: {}", e.getMessage());
            }
        }

        log.info("[Logout] 登出流程完成");
    }

    /**
     * 获取当前用户信息
     */
    public LoginVO getCurrentUserInfo() {
        StpLogic stpLogic = getCurrentStpLogic();

        stpLogic.checkLogin();

        try {
            Long userId = stpLogic.getLoginIdAsLong();
            String username = stpLogic.getSession().get("username", "");
            String nickname = stpLogic.getSession().get("nickname", "");
            String avatar = stpLogic.getSession().get("avatar", "");
            Long tenantId = stpLogic.getSession().get("tenantId", 0L);
            String tenantName = stpLogic.getSession().get("tenantName", "");
            String loginType = stpLogic.getLoginType();

            LoginVO loginVO = new LoginVO();
            loginVO.setUserId(userId);
            loginVO.setUsername(username);
            loginVO.setNickname(nickname);
            loginVO.setAvatar(avatar);
            loginVO.setTenantId(tenantId);
            loginVO.setTenantName(tenantName);
            loginVO.setTokenName(stpLogic.getTokenName());
            loginVO.setToken(stpLogic.getTokenValue());

            log.debug("获取用户信息: userId={}, username={}, loginType={}",
                    userId, username, loginType);

            return loginVO;
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            throw new RuntimeException("获取用户信息失败: " + e.getMessage());
        }
    }
}
