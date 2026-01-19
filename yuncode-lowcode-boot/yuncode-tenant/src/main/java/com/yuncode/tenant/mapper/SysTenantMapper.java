package com.yuncode.tenant.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuncode.tenant.entity.SysTenant;

/**
 * 系统租户 Mapper
 */
public interface SysTenantMapper extends BaseMapper<SysTenant> {

    /**
     * 根据租户编码查询租户
     */
    default SysTenant selectByTenantCode(String tenantCode) {
        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysTenant::getTenantCode, tenantCode)
                .eq(SysTenant::getDeleted, 0);
        return selectOne(wrapper);
    }
}
