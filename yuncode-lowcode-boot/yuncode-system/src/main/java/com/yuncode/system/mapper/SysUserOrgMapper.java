package com.yuncode.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuncode.system.entity.SysUserOrg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户组织关联Mapper接口
 *
 * @author Yuncode
 * @since 2025-01-22
 */
@Mapper
public interface SysUserOrgMapper extends BaseMapper<SysUserOrg> {

    /**
     * 查询组织的用户列表
     *
     * @param orgId 组织ID
     * @param tenantId 租户ID
     * @return 用户组织关联列表
     */
    @Select("SELECT * FROM sys_user_org WHERE org_id = #{orgId} AND tenant_id = #{tenantId} AND deleted = 0")
    List<SysUserOrg> selectByOrgId(@Param("orgId") Long orgId, @Param("tenantId") Long tenantId);

    /**
     * 查询用户所属的组织列表
     *
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @return 用户组织关联列表
     */
    @Select("SELECT * FROM sys_user_org WHERE user_id = #{userId} AND tenant_id = #{tenantId} AND deleted = 0")
    List<SysUserOrg> selectByUserId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    /**
     * 查询组织的用户数量
     *
     * @param orgId 组织ID
     * @param tenantId 租户ID
     * @return 用户数量
     */
    @Select("SELECT COUNT(*) FROM sys_user_org WHERE org_id = #{orgId} AND tenant_id = #{tenantId} AND deleted = 0")
    int countUsersByOrgId(@Param("orgId") Long orgId, @Param("tenantId") Long tenantId);
}
