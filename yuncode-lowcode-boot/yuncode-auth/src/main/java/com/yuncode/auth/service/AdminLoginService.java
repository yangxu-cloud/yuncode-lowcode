package com.yuncode.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.yuncode.auth.dto.LoginDTO;
import com.yuncode.auth.properties.SaTokenProperties;
import com.yuncode.auth.vo.LoginVO;
import com.yuncode.common.exception.BusinessException;
import com.yuncode.common.exception.ErrorCode;
import com.yuncode.common.utils.web.ServletUtils;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 管理员登录服务
 * 平台超级管理员登录，使用系统内置租户，不需要租户编码
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AdminLoginService {

    private static final String SYSTEM_TENANT_CODE = "system";

    private final SaTokenProperties saTokenProperties;
    private final SysTenantMapper sysTenantMapper;
    private final SysUserMapper sysUserMapper;
    private final SysLoginLogService sysLoginLogService;
    private final OnlineUserService onlineUserService;
    private final UserCacheService userCacheService;

    /**
     * 管理员登录
     * 管理员使用系统租户（ID=2）登录
     */
    @LoginLog(loginType = "admin")
    public LoginVO login(LoginDTO loginDTO, HttpServletRequest request) {
        String username = loginDTO.getUsername();

        try {
            // 1. 查询系统租户（固定ID=2）
            SysTenant systemTenant = sysTenantMapper.selectById(2L);
            if (systemTenant == null) {
                throw new BusinessException(ErrorCode.TENANT_NOT_FOUND, "系统租户不存在，请先初始化系统租户（ID=2）");
            }

            // 2. 校验系统租户状态
            if (systemTenant.getStatus() == 1) {
                throw new BusinessException(ErrorCode.TENANT_DISABLED);
            }

            // 3. 使用系统租户ID（固定=2）查询用户
            Long systemTenantId = 2L;
            SysUser user = sysUserMapper.selectByUsernameAndTenantId(username, systemTenantId);
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

            // 将用户类型、系统租户ID、角色编码等信息存入 Token Session（统一存储位置）
            StpUtil.getTokenSession().set("loginType", "admin");
            StpUtil.getTokenSession().set("tenantId", systemTenantId);
            StpUtil.getTokenSession().set("userId", user.getId());
            StpUtil.getTokenSession().set("username", user.getUsername());
            StpUtil.getTokenSession().set("nickname", user.getNickname() != null ? user.getNickname() : "");
            StpUtil.getTokenSession().set("avatar", user.getAvatar() != null ? user.getAvatar() : "");
            StpUtil.getTokenSession().set("roleCode", user.getRoleCode() != null ? user.getRoleCode() : "PLATFORM_ADMIN");

            // 将用户信息存入 Session（统一存储位置，供日志切面等组件读取）
            StpUtil.getSession().set("loginType", "admin");
            StpUtil.getSession().set("tenantId", systemTenantId);
            StpUtil.getSession().set("userId", user.getId());
            StpUtil.getSession().set("username", user.getUsername());
            StpUtil.getSession().set("nickname", user.getNickname() != null ? user.getNickname() : "");
            StpUtil.getSession().set("avatar", user.getAvatar() != null ? user.getAvatar() : "");
            StpUtil.getSession().set("roleCode", user.getRoleCode() != null ? user.getRoleCode() : "PLATFORM_ADMIN");

            // 7. 缓存用户信息到 Redis（30分钟）- 注意：如果 Redis 未连接会抛出异常
            try {
                userCacheService.cacheUser(user.getId(), user, 1800);
                log.info("用户信息缓存成功: userId={}", user.getId());
            } catch (Exception cacheEx) {
                log.error("用户信息缓存失败（Redis未连接或配置错误）: userId={}, error={}", user.getId(), cacheEx.getMessage());
                // 缓存失败不影响登录，仅影响在线用户同步等功能
                // 继续执行后续步骤
            }

            // 8. 生成业务会话ID（UUID，用于 Redis key 和前端 Cookie）
            String sessionId = java.util.UUID.randomUUID().toString().replace("-", "");
            log.info("生成业务会话ID: userId={}, sessionId={}", user.getId(), sessionId);

            // 将 sessionId 存入 Sa-Token session，供退出时使用
            StpUtil.getTokenSession().set("sessionId", sessionId);
            StpUtil.getSession().set("sessionId", sessionId);
            log.info("sessionId 已存入 Sa-Token session，验证: {}", StpUtil.getTokenSession().get("sessionId"));

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
            onlineUser.setTenantId(systemTenantId);  // 使用系统租户ID
            onlineUser.setTenantName(systemTenant.getTenantName());
            onlineUser.setIp(ServletUtils.getClientIP(request));
            onlineUser.setLocation("");
            onlineUser.setUserAgent(request.getHeader("User-Agent"));

            try {
                onlineUserService.addOnlineUser(sessionId, onlineUser);
                log.info("在线用户记录添加成功: sessionId={}", sessionId);
            } catch (Exception onlineEx) {
                log.error("在线用户记录添加失败（Redis未连接）: sessionId={}, error={}", sessionId, onlineEx.getMessage());
                // 在线用户记录失败不影响登录返回
            }

            // 11. 构建返回结果
            LoginVO loginVO = new LoginVO();
            loginVO.setToken(token);
            loginVO.setSessionId(sessionId);  // 返回业务会话ID给前端
            loginVO.setTokenName(saTokenProperties.getTokenName());
            loginVO.setUserId(StpUtil.getLoginIdAsLong());
            loginVO.setUsername(username);
            loginVO.setNickname(StpUtil.getSession().get("nickname", ""));
            loginVO.setAvatar(StpUtil.getSession().get("avatar", ""));
            loginVO.setTenantId(systemTenantId);  // 使用系统租户ID
            loginVO.setTenantName(systemTenant.getTenantName());

            return loginVO;

        } catch (BusinessException e) {
            log.error("登录业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("登录系统异常: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败：" + e.getMessage());
        }
    }

}
