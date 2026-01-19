package com.yuncode.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.yuncode.auth.dto.LoginDTO;
import com.yuncode.auth.vo.LoginVO;
import com.yuncode.common.exception.BusinessException;
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
import org.springframework.stereotype.Service;

/**
 * 管理员登录服务
 * 平台超级管理员登录，使用系统内置租户，不需要租户编码
 */
@Slf4j
@Service
public class AdminLoginService {

    private static final String SYSTEM_TENANT_CODE = "system";

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
    public LoginVO login(LoginDTO loginDTO, HttpServletRequest request) {
        Long startTime = System.currentTimeMillis();
        Long tenantId = null;
        String username = loginDTO.getUsername();
        Integer status = LoginStatus.SUCCESS.getCode();
        String msg = "登录成功";
        SysTenant tenant = null;

        try {
            // 1. 管理员登录使用系统内置租户，忽略传入的租户编码
            tenant = sysTenantMapper.selectByTenantCode(SYSTEM_TENANT_CODE);
            if (tenant == null) {
                status = LoginStatus.FAIL.getCode();
                msg = "系统租户不存在，请联系管理员";
                throw new BusinessException(msg);
            }

            // 2. 校验系统租户状态
            if (tenant.getStatus() == 1) {
                status = LoginStatus.FAIL.getCode();
                msg = "系统租户已被禁用";
                throw new BusinessException(msg);
            }

            tenantId = tenant.getId();

            // 3. 查询用户
            SysUser user = sysUserMapper.selectByUsernameAndTenantId(username, tenantId);
            if (user == null) {
                status = LoginStatus.FAIL.getCode();
                msg = "用户名或密码错误";
                throw new BusinessException(msg);
            }

            // 4. 校验用户状态
            if (user.getStatus() == 1) {
                status = LoginStatus.FAIL.getCode();
                msg = "账号已被禁用";
                throw new BusinessException(msg);
            }

            // 5. 校验密码
            log.info("密码校验开始 - 输入密码: [{}], 数据库哈希: [{}]", loginDTO.getPassword(), user.getPassword());

            // 测试BCrypt验证
            boolean passwordMatch = BCrypt.checkpw(loginDTO.getPassword(), user.getPassword());
            log.info("BCrypt验证结果: {}", passwordMatch);

            // 额外测试：生成新哈希并验证
            String newHash = BCrypt.hashpw(loginDTO.getPassword());
            log.info("新生成的哈希: [{}]", newHash);
            log.info("新哈希验证结果: {}", BCrypt.checkpw(loginDTO.getPassword(), newHash));

            if (!passwordMatch) {
                status = LoginStatus.FAIL.getCode();
                msg = "用户名或密码错误";
                log.error("密码验证失败 - 用户: [{}], 租户ID: [{}]", username, tenantId);
                throw new BusinessException(msg);
            }

            log.info("密码验证成功");

            // 6. 使用 Sa-Token 进行登录，将用户类型和租户信息存储在 Token Extra 中
            log.info("开始调用 StpUtil.login() with Extra, userId={}", user.getId());

            // 登录时设置 Extra 信息到 Token Payload 中
            StpUtil.login(user.getId());

            // 将用户类型、租户ID等信息存入 Token Extra
            StpUtil.getTokenSession().set("loginType", "admin");
            StpUtil.getTokenSession().set("tenantId", tenantId);
            StpUtil.getTokenSession().set("username", user.getUsername());
            StpUtil.getTokenSession().set("nickname", user.getNickname() != null ? user.getNickname() : "");

            log.info("StpUtil.login() 调用成功，TokenSession Extra 信息已设置");

            // 验证登录状态
            boolean isLoggedIn = StpUtil.isLogin();
            log.info("登录后验证 StpUtil.isLogin(): {}", isLoggedIn);
            Object loginId = StpUtil.getLoginIdDefaultNull();
            log.info("登录后验证 StpUtil.getLoginIdDefaultNull(): {}", loginId);

            // 验证 Extra 信息是否设置成功
            Object loginTypeObj = StpUtil.getTokenSession().get("loginType");
            Object tenantIdObj = StpUtil.getTokenSession().get("tenantId");
            String loginType = loginTypeObj != null ? loginTypeObj.toString() : null;
            String extraTenantId = tenantIdObj != null ? tenantIdObj.toString() : null;
            log.info("验证 TokenSession Extra 信息: loginType={}, tenantId={}", loginType, extraTenantId);

            log.info("管理员登录成功: userId={}, username={}, tenantId={}",
                    user.getId(), username, tenantId);

            // 将用户信息存入 Session（保持兼容性）
            StpUtil.getSession().set("userId", user.getId());
            StpUtil.getSession().set("username", user.getUsername());
            StpUtil.getSession().set("nickname", user.getNickname() != null ? user.getNickname() : "");
            StpUtil.getSession().set("avatar", user.getAvatar() != null ? user.getAvatar() : "");
            StpUtil.getSession().set("tenantId", tenantId);
            StpUtil.getSession().set("loginType", "admin");
            log.info("Session 信息已设置，检查是否可读取: username={}", StpUtil.getSession().get("username"));

            // 7. 缓存用户信息到 Redis（30分钟）
            userCacheService.cacheUser(user.getId(), user, 1800);

            // 8. 添加在线用户记录
            OnlineUser onlineUser = new OnlineUser();
            onlineUser.setUserId(user.getId());
            onlineUser.setUsername(username);
            onlineUser.setNickname(user.getNickname());
            onlineUser.setAvatar(user.getAvatar());
            onlineUser.setTenantId(tenantId);
            onlineUser.setTenantName(tenant.getTenantName());
            onlineUser.setIp(getClientIP(request));
            onlineUser.setLocation("");
            onlineUser.setUserAgent(request.getHeader("User-Agent"));

            String token = StpUtil.getTokenValue();
            log.info("生成的 Token: {}, 长度: {}", token.substring(0, Math.min(20, token.length())) + "...", token.length());

            // 验证 token 是否可反向获取 loginId
            Object loginIdByToken = StpUtil.getLoginIdByToken(token);
            log.info("验证 StpUtil.getLoginIdByToken(token): {}", loginIdByToken);

            onlineUserService.addOnlineUser(token, onlineUser);

        } catch (BusinessException e) {
            status = LoginStatus.FAIL.getCode();
            msg = e.getMessage();
            log.warn("管理员登录失败: username={}, status={}, message={}", username, status, e.getMessage());
            throw e;
        } catch (Exception e) {
            status = LoginStatus.FAIL.getCode();
            msg = "系统异常：" + e.getMessage();
            log.error("管理员登录系统异常: username={}, status={}", username, status, e);
            throw new BusinessException(msg);
        } finally {
            // 记录登录日志（无论成功或失败）
            Long costTime = System.currentTimeMillis() - startTime;
            log.info("记录管理员登录日志: username={}, status={}, msg={}, costTime={}ms",
                    username, status, msg, costTime);
            sysLoginLogService.recordLoginLog(tenantId, username, status, msg, request, costTime);
        }

        // 如果登录失败，不返回 Token
        if (!LoginStatus.SUCCESS.getCode().equals(status)) {
            throw new BusinessException(msg);
        }

        // 构建返回结果
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(StpUtil.getTokenValue());
        loginVO.setTokenName("satoken");
        loginVO.setUserId(StpUtil.getLoginIdAsLong());
        loginVO.setUsername(username);
        loginVO.setNickname(StpUtil.getSession().get("nickname", ""));
        loginVO.setAvatar(StpUtil.getSession().get("avatar", ""));
        loginVO.setTenantId(tenantId);

        if (tenant != null) {
            loginVO.setTenantName(tenant.getTenantName());
        }

        return loginVO;
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
