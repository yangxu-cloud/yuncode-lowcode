package com.yuncode.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuncode.system.entity.SysUserOrg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户组织关联Mapper接口
 *
 * 多租户说明：MyBatis-Plus 多租户插件会自动在所有 SQL 语句中添加 tenant_id 条件
 * 因此无需在 Mapper 方法中手动传递 tenantId 参数
 *
 * 例外场景：
 * 1. selectByOrgIdForPlatformAdmin - 平台管理员查询某组织的所有用户，忽略租户限制
 *
 * 注意：这些方法使用 XML 映射文件实现，并使用 @InterceptorIgnore 忽略多租户插件
 * XML 文件位置：resources/mapper/SysUserOrgMapper.xml
 *
 * @author Yuncode
 * @since 2025-01-22
 */
@Mapper
public interface SysUserOrgMapper extends BaseMapper<SysUserOrg> {

    /**
     * 根据组织ID查询用户组织关系（用于平台管理员，忽略租户限制）
     * 注意：此方法仅用于平台管理员查看所有租户的用户组织关系
     * 使用 @InterceptorIgnore 忽略多租户插件的自动过滤，不添加 tenant_id 条件
     *
     * @param orgId 组织ID
     * @return 用户组织关系列表
     */
    @InterceptorIgnore(tenantLine = "true")
    List<SysUserOrg> selectByOrgIdForPlatformAdmin(@Param("orgId") Long orgId);

    /**
     * 根据ID物理删除用户组织关系（绕过 @TableLogic 逻辑删除）
     * 注意：此方法会真正从数据库删除记录，而不是设置 deleted = 1
     *
     * @param id 用户组织关系ID
     * @return 删除结果
     */
    int deletePhysicalById(@Param("id") Long id);

    /**
     * 根据组织ID列表批量物理删除用户组织关系（绕过 @TableLogic 逻辑删除）
     * 注意：此方法会真正从数据库删除记录，而不是设置 deleted = 1
     *
     * @param orgIds 组织ID列表
     * @return 删除结果
     */
    int deletePhysicalByOrgIds(@Param("orgIds") List<Long> orgIds);

    /**
     * 根据用户ID列表批量物理删除用户组织关系（绕过 @TableLogic 逻辑删除）
     * 注意：此方法会真正从数据库删除记录，而不是设置 deleted = 1
     *
     * @param userIds 用户ID列表
     * @return 删除结果
     */
    int deletePhysicalByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * 查询列表（忽略租户限制）
     * 注意：此方法用于查询用户组织关系时不受租户限制
     * 使用 @InterceptorIgnore 忽略多租户插件的自动过滤，不添加 tenant_id 条件
     *
     * @param wrapper 查询条件
     * @return 用户组织关系列表
     */
    @InterceptorIgnore(tenantLine = "true")
    List<SysUserOrg> selectListIgnoreTenant(@Param("ew") LambdaQueryWrapper<SysUserOrg> wrapper);
}
