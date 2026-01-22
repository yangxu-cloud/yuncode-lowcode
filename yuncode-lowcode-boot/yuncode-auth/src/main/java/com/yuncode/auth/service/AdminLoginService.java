package com.yuncode.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.yuncode.auth.dto.LoginDTO;
import com.yuncode.auth.properties.SaTokenProperties;
import com.yuncode.auth.vo.LoginVO;
import com.yuncode.common.exception.BusinessException;
import com.yuncode.common.exception.ErrorCode;
import com.yuncode.system.annotation.LoginLog;
import com.yuncode.system.entity.OnlineUser;
import com.yuncode.system.entity.SysUser;
import com.yuncode.system.enums.LoginStatus;
import com.yuncode.system.mapper.SysUserMapper;
import com.yuncode.system.service.SysLoginLogService;
import com.yuncode.system.service.OnlineUserService;
import com.yuncode.system.service.UserCacheService;
import com.yuncode.tenant.entity.SysTenant;
import com.yuncode.tenant.mapper.SysTenantMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 管理员登录服务
 * 平台超级管理员登录，使用系统内置租户，不需要租户编码
 */
@Slf4j
@Service
public class AdminLoginService {

    private static final String SYSTEM_TENANT_CODE = "system";

    @Autowired
    private SaTokenProperties saTokenProperties;

    private final SysTenantMapper sysTenantMapper;
    private final SysUserMapper sysUserMapper;
    private final SysLoginLogService sysLoginLogService;
    private final OnlineUserService onlineUserService;
    private final UserCacheService userCacheService;

    public AdminLoginService(
            SysTenantMapper sysTenantMapper,
            SysUserMapper sysUserMapper,
            SysLoginLogService sysLoginLogService,
            OnlineUserService onlineUserService,
            UserCacheService userCacheService) {
        this.sysTenantMapper = sysTenantMapper;
        this.sysUserMapper = sysUserMapper;
        this.sysLoginLogService = sysLoginLogService;
        this.onlineUserService = onlineUserService;
        this.userCacheService = userCacheService;
    }

    /**
     * 管理员登录
     */
    @LoginLog(loginType = "admin")
    public LoginVO login(LoginDTO loginDTO, HttpServletRequest request) {
        Long tenantId = null;
        String username = loginDTO.getUsername();
        SysTenant tenant = null;

        try {
            // 1. 管理员登录使用系统内置租户
            tenant = sysTenantMapper.selectByTenantCode(SYSTEM_TENANT_CODE);
            if (tenant == null) {
                throw new BusinessException(ErrorCode.TENANT_NOT_FOUND);
            }

            // 2. 校验系统租户状态
            if (tenant.getStatus() == 1) {
                throw new BusinessException(ErrorCode.TENANT_DISABLED);
            }

            tenantId = tenant.getId();

            // 3. 查询用户
            SysUser user = sysUserMapper.selectByUsernameAndTenantId(username, tenantId);
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR);
            }

            // 4. 校验用户状态
            if (user.getStatus() == 1) {
                throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
            }

            // 5. 校验密码
            if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
                throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR);
            }

            // 6. 使用 Sa-Token 进行登录
            StpUtil.login(user.getId());

            // 将用户类型、租户ID等信息存入 Token Extra
            StpUtil.getTokenSession().set("loginType", "admin");
            StpUtil.getTokenSession().set("tenantId", tenantId);
            StpUtil.getTokenSession().set("username", user.getUsername());
            StpUtil.getTokenSession().set("nickname", user.getNickname() != null ? user.getNickname() : "");

            // 将用户信息存入 Session
            StpUtil.getSession().set("userId", user.getId());
            StpUtil.getSession().set("username", user.getUsername());
            StpUtil.getSession().set("nickname", user.getNickname() != null ? user.getNickname() : "");
            StpUtil.getSession().set("avatar", user.getAvatar() != null ? user.getAvatar() : "");
            StpUtil.getSession().set("tenantId", tenantId);
            StpUtil.getSession().set("loginType", "admin");

            // 7. 缓存用户信息到 Redis（30分钟）
            userCacheService.cacheUser(user.getId(), user, 1800);

            // 8. 生成业务会话ID（UUID，用于 Redis key 和前端 Cookie）
            String sessionId = java.util.UUID.randomUUID().toString().replace("-", "");
            log.info("生成业务会话ID: userId={}, sessionId={}", user.getId(), sessionId);

            // 将 sessionId 存入 Sa-Token session，供退出时使用
            StpUtil.getSession().set("sessionId", sessionId);
            log.info("sessionId 已存入 Sa-Token session，验证: {}", StpUtil.getSession().get("sessionId"));

            // 9. 获取 Sa-Token 的 JWT Token
            String token = StpUtil.getTokenValue();
            log.info("获取 Sa-Token: userId={}, token (前32位)={}", user.getId(), token.substring(0, Math.min(32, token.length())));

            // 10. 添加在线用户记录
            OnlineUser onlineUser = new OnlineUser();
            onlineUser.setSessionId(sessionId);  // 业务会话ID
            onlineUser.setToken(token);          // Sa-Token JWT
            onlineUser.setUserId(user.getId());
            onlineUser.setUsername(username);
            onlineUser.setNickname(user.getNickname());
            onlineUser.setAvatar(user.getAvatar());
            onlineUser.setTenantId(tenantId);
            onlineUser.setTenantName(tenant.getTenantName());
            onlineUser.setIp(getClientIP(request));
            onlineUser.setLocation("");
            onlineUser.setUserAgent(request.getHeader("User-Agent"));

            onlineUserService.addOnlineUser(sessionId, onlineUser);

            // 构建返回结果
            LoginVO loginVO = new LoginVO();
            loginVO.setToken(token);
            loginVO.setSessionId(sessionId);  // 返回业务会话ID给前端
            loginVO.setTokenName(saTokenProperties.getTokenName());
            loginVO.setUserId(StpUtil.getLoginIdAsLong());
            loginVO.setUsername(username);
            loginVO.setNickname(StpUtil.getSession().get("nickname", ""));
            loginVO.setAvatar(StpUtil.getSession().get("avatar", ""));
            loginVO.setTenantId(tenantId);

            if (tenant != null) {
                loginVO.setTenantName(tenant.getTenantName());
            }

            return loginVO;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败，请稍后重试");
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
