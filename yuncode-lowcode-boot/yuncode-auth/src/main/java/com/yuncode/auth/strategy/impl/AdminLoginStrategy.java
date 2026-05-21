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

/**
 * 管理员登录策略
 * 平台超级管理员登录，使用系统内置租户，不需要租户编码
 */
@Slf4j
@Component("adminLoginStrategy")
public class AdminLoginStrategy extends AbstractLoginStrategy {

    private static final String SYSTEM_TENANT_CODE = "system";

    public AdminLoginStrategy(SaTokenProperties saTokenProperties,
                             SysTenantMapper sysTenantMapper,
                             SysUserMapper sysUserMapper,
                             SysLoginLogService sysLoginLogService,
                             OnlineUserService onlineUserService,
                             UserCacheService userCacheService) {
        super(saTokenProperties, sysTenantMapper, sysUserMapper, sysLoginLogService, onlineUserService, userCacheService);
    }

    @Override
    protected SysTenant validateAndGetTenant(LoginDTO loginDTO) {
        // 管理员登录使用系统内置租户，忽略传入的租户编码
        SysTenant tenant = sysTenantMapper.selectByTenantCode(SYSTEM_TENANT_CODE);
        if (tenant == null) {
            throw new BusinessException("系统租户不存在，请联系管理员");
        }

        // 校验系统租户状态
        if (tenant.getStatus() == 1) {
            throw new BusinessException("系统租户已被禁用");
        }

        log.info("管理员登录验证通过: tenantCode={}, tenantId={}", SYSTEM_TENANT_CODE, tenant.getId());
        return tenant;
    }

    @Override
    public String getLoginType() {
        return LoginType.ADMIN.getCode();
    }
}
