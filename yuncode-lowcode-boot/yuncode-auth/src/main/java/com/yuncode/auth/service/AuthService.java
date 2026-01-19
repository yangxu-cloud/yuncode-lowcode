package com.yuncode.auth.service;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.yuncode.auth.dto.LoginDTO;
import com.yuncode.auth.factory.LoginStrategyFactory;
import com.yuncode.auth.strategy.LoginStrategy;
import com.yuncode.auth.vo.LoginVO;
import com.yuncode.system.service.LoginLogService;
import com.yuncode.system.service.OnlineUserService;
import jakarta.servlet.http.HttpServletRequest;
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

    private final LoginStrategyFactory loginStrategyFactory;
    private final LoginLogService loginLogService;
    private final OnlineUserService onlineUserService;
    private final List<StpLogic> stpLogics;

    public AuthService(
            LoginStrategyFactory loginStrategyFactory,
            LoginLogService loginLogService,
            OnlineUserService onlineUserService,
            List<StpLogic> stpLogics) {
        this.loginStrategyFactory = loginStrategyFactory;
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
                // 跳过自动配置的 StpLogic（它的 loginType 通常是 "login" 或为空）
                if ("login".equals(stpLogic.getLoginType()) || stpLogic.getLoginType() == null) {
                    continue;
                }

                try {
                    // 检查这个 StpLogic 是否能认领这个 token
                    // 通过尝试获取 loginId 来验证
                    if (stpLogic.isLogin()) {
                        Object loginId = stpLogic.getLoginIdDefaultNull();
                        if (loginId != null) {
                            // 验证这个 token 是否真的属于这个 StpLogic
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
     * 用户登录（带日志记录）
     * 使用策略模式处理不同登录类型
     */
    public LoginVO login(LoginDTO loginDTO, HttpServletRequest request) {
        // 获取对应的登录策略
        LoginStrategy strategy = loginStrategyFactory.getStrategy(loginDTO.getLoginType());

        // 执行登录
        log.info("开始登录: username={}, loginType={}", loginDTO.getUsername(), loginDTO.getLoginType());
        LoginVO loginVO = strategy.login(loginDTO, request);

        log.info("登录成功: userId={}, username={}, loginType={}",
                loginVO.getUserId(), loginVO.getUsername(), loginDTO.getLoginType());

        return loginVO;
    }

    /**
     * 用户登出
     */
    public void logout() {
        StpLogic stpLogic = getCurrentStpLogic();

        try {
            // 获取当前用户信息
            Long userId = stpLogic.getLoginIdAsLong();
            String username = stpLogic.getSession().get("username", "");

            // 更新登录日志的登出时间
            loginLogService.updateLogoutTime(username, LocalDateTime.now());

            log.info("用户登出成功: userId={}, username={}, loginType={}",
                    userId, username, stpLogic.getLoginType());

        } catch (cn.dev33.satoken.exception.NotLoginException e) {
            // token 无效或已过期，这是正常情况
            log.debug("登出时 token 已失效: {}", e.getMessage());
        } catch (Exception e) {
            log.error("更新登出时间失败", e);
        } finally {
            // 移除在线用户记录
            try {
                String token = stpLogic.getTokenValue();
                if (token != null) {
                    onlineUserService.removeOnlineUser(token);
                }
            } catch (Exception e) {
                log.debug("移除在线用户记录失败: {}", e.getMessage());
            }

            // 执行登出操作（即使 token 已失效也要尝试清理）
            try {
                stpLogic.logout();
            } catch (Exception e) {
                log.debug("执行 logout 失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 获取当前用户信息
     */
    public LoginVO getCurrentUserInfo() {
        StpLogic stpLogic = getCurrentStpLogic();

        // 检查是否登录（StpLogic.checkLogin() 会自动抛出 NotLoginException）
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
