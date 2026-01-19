package com.yuncode.system.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuncode.system.entity.SysUser;

/**
 * 系统用户 Mapper
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户名和租户ID查询用户
     */
    default SysUser selectByUsernameAndTenantId(String username, Long tenantId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username)
                .eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getDeleted, 0);
        return selectOne(wrapper);
    }

    /**
     * 根据 ID 查询用户
     */
    default SysUser selectById(Long userId) {
        return selectById(userId);
    }
}
