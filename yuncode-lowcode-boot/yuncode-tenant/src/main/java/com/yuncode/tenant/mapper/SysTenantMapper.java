package com.yuncode.tenant.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuncode.tenant.entity.SysTenant;

import java.time.LocalDateTime;

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

    /**
     * 根据ID删除租户（逻辑删除，忽略租户限制）
     * 注意：此方法用于删除租户时不受租户限制
     * 使用 @InterceptorIgnore 忽略多租户插件的自动过滤，不添加 tenant_id 条件
     * @TableLogic 会自动将删除转换为 UPDATE deleted=1
     *
     * @param tenantId 租户ID
     * @return 删除的记录数
     */
    @InterceptorIgnore(tenantLine = "true")
    default int deleteByIdIgnoreTenant(Long tenantId) {
        LambdaUpdateWrapper<SysTenant> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysTenant::getId, tenantId)
                .eq(SysTenant::getDeleted, 0)
                .set(SysTenant::getDeleted, 1)
                .set(SysTenant::getUpdateTime, LocalDateTime.now())
                .set(SysTenant::getUpdateBy, "system");
        return this.update(null, updateWrapper);
    }

    /**
     * 物理删除租户（设置 deleted=1）
     * 注意：这个方法会绕过 @TableLogic 注解，直接物理删除记录
     */
    default int deletePhysicalById(Long tenantId) {
        LambdaUpdateWrapper<SysTenant> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysTenant::getId, tenantId)
                .set(SysTenant::getDeleted, 1);
        return this.update(null, updateWrapper);
    }
}
