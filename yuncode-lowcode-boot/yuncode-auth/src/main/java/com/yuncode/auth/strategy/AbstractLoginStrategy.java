package com.yuncode.auth.strategy;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.yuncode.auth.dto.LoginDTO;
import com.yuncode.auth.properties.SaTokenProperties;
import com.yuncode.auth.vo.LoginVO;
import com.yuncode.common.exception.BusinessException;
import com.yuncode.common.exception.ErrorCode;
import com.yuncode.system.entity.SysUser;
import com.yuncode.system.entity.OnlineUser;
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

/**
 * 登录策略抽象基类
 */
@Slf4j
public abstract class AbstractLoginStrategy implements LoginStrategy {

    @Autowired
    protected SaTokenProperties saTokenProperties;

    protected final SysTenantMapper sysTenantMapper;
    protected final SysUserMapper sysUserMapper;
    protected final SysLoginLogService sysLoginLogService;
    protected final OnlineUserService onlineUserService;
    protected final UserCacheService userCacheService;

    protected AbstractLoginStrategy(SysTenantMapper sysTenantMapper,
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

    @Override
    public LoginVO login(LoginDTO loginDTO, HttpServletRequest request) {
        Long startTime = System.currentTimeMillis();
        Long tenantId = null;
        String username = loginDTO.getUsername();
        Integer status = LoginStatus.SUCCESS.getCode();
        String msg = "登录成功";
        SysTenant tenant = null;

        try {
            // 1. 获取并验证租户信息
            tenant = validateAndGetTenant(loginDTO);
            tenantId = tenant.getId();

            // 2. 查询用户
            SysUser user = sysUserMapper.selectByUsernameAndTenantId(username, tenantId);
            if (user == null) {
                status = LoginStatus.FAIL.getCode();
                msg = "用户名或密码错误";
                throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR);
            }

            // 3. 校验用户状态
            if (user.getStatus() == 1) {
                status = LoginStatus.FAIL.getCode();
                msg = "账号已被禁用";
                throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
            }

            // 4. 校验密码
            log.debug("密码校验 - 输入密码: {}, 数据库哈希: {}", loginDTO.getPassword(), user.getPassword());

            if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
                status = LoginStatus.FAIL.getCode();
                msg = "用户名或密码错误";
                throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR);
            }

            // 5. 使用 Sa-Token 进行登录
            StpUtil.login(user.getId());

            log.info("用户登录成功: userId={}, username={}, tenantId={}, loginType={}",
                    user.getId(), username, tenantId, getLoginType());

            // 将用户信息存入 Session
            StpUtil.getSession().set("userId", user.getId());
            StpUtil.getSession().set("username", user.getUsername());
            StpUtil.getSession().set("nickname", user.getNickname() != null ? user.getNickname() : "");
            StpUtil.getSession().set("avatar", user.getAvatar() != null ? user.getAvatar() : "");
            StpUtil.getSession().set("tenantId", tenantId);
            StpUtil.getSession().set("roleCode", user.getRoleCode() != null ? user.getRoleCode() : "NORMAL");
            StpUtil.getSession().set("loginType", getLoginType());

            // 6. 缓存用户信息到 Redis（30分钟）
            userCacheService.cacheUser(user.getId(), user, 1800);

            // 7. 添加在线用户记录
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
            onlineUserService.addOnlineUser(token, onlineUser);

        } catch (BusinessException e) {
            status = LoginStatus.FAIL.getCode();
            msg = e.getMessage();
            throw e;
        } catch (Exception e) {
            status = LoginStatus.FAIL.getCode();
            msg = "系统异常：" + e.getMessage();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败，请稍后重试");
        } finally {
            // 记录登录日志（无论成功或失败）
            Long costTime = System.currentTimeMillis() - startTime;
            log.info("记录登录日志: username={}, loginType={}, status={}, msg={}, costTime={}ms",
                    username, getLoginType(), status, msg, costTime);
            sysLoginLogService.recordLoginLog(tenantId, username, status, msg, request, costTime);
        }

        // 如果登录失败，不返回 Token
        if (!LoginStatus.SUCCESS.getCode().equals(status)) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, msg);
        }

        // 构建返回结果
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(StpUtil.getTokenValue());
        loginVO.setTokenName(saTokenProperties.getTokenName());  // 从配置读取
        loginVO.setUserId(StpUtil.getLoginIdAsLong());
        loginVO.setUsername(username);
        loginVO.setNickname(StpUtil.getSession().get("nickname", ""));
        loginVO.setAvatar(StpUtil.getSession().get("avatar", ""));
        loginVO.setTenantId(tenantId);
        loginVO.setRoleCode(StpUtil.getSession().get("roleCode", "NORMAL"));

        if (tenant != null) {
            loginVO.setTenantName(tenant.getTenantName());
        }

        return loginVO;
    }

    /**
     * 验证并获取租户信息（由子类实现具体逻辑）
     *
     * @param loginDTO 登录参数
     * @return 租户信息
     */
    protected abstract SysTenant validateAndGetTenant(LoginDTO loginDTO);

    /**
     * 获取客户端IP地址
     */
    protected String getClientIP(HttpServletRequest request) {
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
