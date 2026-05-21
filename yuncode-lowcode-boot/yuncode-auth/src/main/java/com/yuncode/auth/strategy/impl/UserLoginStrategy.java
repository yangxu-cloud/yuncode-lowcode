package com.yuncode.auth.strategy.impl;

import com.yuncode.auth.dto.LoginDTO;
import com.yuncode.auth.properties.SaTokenProperties;
import com.yuncode.auth.strategy.AbstractLoginStrategy;
import com.yuncode.common.exception.BusinessException;
import com.yuncode.system.enums.LoginType;
import com.yuncode.system.mapper.SysUserMapper;
import com.yuncode.system.service.SysLoginLogService;
import com.yuncode.system.service.OnlineUserService;
import com.yuncode.system.service.UserCacheService;
import com.yuncode.tenant.entity.SysTenant;
import com.yuncode.tenant.mapper.SysTenantMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 普通用户登录策略
 * 普通用户登录，需要租户编码
 */
@Slf4j
@Component("userLoginStrategy")
public class UserLoginStrategy extends AbstractLoginStrategy {

    public UserLoginStrategy(SaTokenProperties saTokenProperties,
                            SysTenantMapper sysTenantMapper,
                            SysUserMapper sysUserMapper,
                            SysLoginLogService sysLoginLogService,
                            OnlineUserService onlineUserService,
                            UserCacheService userCacheService) {
        super(saTokenProperties, sysTenantMapper, sysUserMapper, sysLoginLogService, onlineUserService, userCacheService);
    }

    @Override
    protected SysTenant validateAndGetTenant(LoginDTO loginDTO) {
        // 1. 校验租户编码是否存在
        if (loginDTO.getTenantCode() == null || loginDTO.getTenantCode().trim().isEmpty()) {
            throw new BusinessException("租户编码不能为空");
        }

        // 2. 查询租户
        SysTenant tenant = sysTenantMapper.selectByTenantCode(loginDTO.getTenantCode());
        if (tenant == null) {
            throw new BusinessException("租户不存在");
        }

        // 3. 校验租户状态
        if (tenant.getStatus() == 1) {
            throw new BusinessException("租户已被禁用");
        }

        // 4. 校验租户是否过期
        if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("租户已过期");
        }

        log.info("用户登录验证通过: tenantCode={}, tenantId={}", loginDTO.getTenantCode(), tenant.getId());
        return tenant;
    }

    @Override
    public String getLoginType() {
        return LoginType.USER.getCode();
    }
}
