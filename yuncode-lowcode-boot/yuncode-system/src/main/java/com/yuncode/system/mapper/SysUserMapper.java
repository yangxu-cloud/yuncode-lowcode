package com.yuncode.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuncode.system.entity.SysUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统用户 Mapper
 *
 * 多租户说明：MyBatis-Plus 多租户插件会自动在所有 SQL 语句中添加 tenant_id 条件
 * 因此无需在 Mapper 方法中手动传递 tenantId 参数
 *
 * 例外场景：
 * 1. selectByUsernameAndTenantId - 租户用户登录，需要手动指定租户ID
 * 2. selectByUsernameForAdminLogin - 管理员登录，不使用租户限制
 *
 * 注意：这些方法使用 XML 映射文件实现，并使用 @InterceptorIgnore 忽略多租户插件
 * XML 文件位置：resources/mapper/SysUserMapper.xml
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户名和租户ID查询用户
     * 注意：此方法仅用于登录场景，需要手动指定租户ID
     * 使用 @InterceptorIgnore 忽略多租户插件的自动过滤，避免重复添加 tenant_id 条件
     *
     * @param username 用户名
     * @param tenantId 租户ID
     * @return 用户信息
     */
    @InterceptorIgnore(tenantLine = "true")
    SysUser selectByUsernameAndTenantId(@Param("username") String username, @Param("tenantId") Long tenantId);

    /**
     * 根据用户名查询用户（用于管理员登录，忽略租户限制）
     * 注意：此方法仅用于管理员登录，可以跨租户查询管理员账户
     * 使用 @InterceptorIgnore 忽略多租户插件的自动过滤
     *
     * @param username 用户名
     * @return 用户信息
     */
    @InterceptorIgnore(tenantLine = "true")
    SysUser selectByUsernameForAdminLogin(@Param("username") String username);

    /**
     * 根据ID查询用户（忽略租户限制）
     * 注意：此方法用于跨租户查询用户，需要忽略租户限制
     * 使用 @InterceptorIgnore 忽略多租户插件的自动过滤
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @InterceptorIgnore(tenantLine = "true")
    SysUser selectByIdIgnoreTenant(@Param("id") Long id);

    /**
     * 根据ID更新用户（忽略租户限制）
     * 注意：此方法用于跨租户更新用户，需要忽略租户限制
     * 使用 @InterceptorIgnore 忽略多租户插件的自动过滤
     *
     * @param user 用户信息
     * @return 更新结果
     */
    @InterceptorIgnore(tenantLine = "true")
    int updateByIdIgnoreTenant(SysUser user);

    /**
     * 根据ID删除用户（逻辑删除，忽略租户限制）
     * 注意：此方法用于删除用户时不受租户限制
     * 使用 @InterceptorIgnore 忽略多租户插件的自动过滤，不添加 tenant_id 条件
     * @TableLogic 会自动将删除转换为 UPDATE deleted=1
     *
     * @param id 用户ID
     * @return 删除的记录数
     */
    @InterceptorIgnore(tenantLine = "true")
    int deleteByIdIgnoreTenant(@Param("id") Long id);

    /**
     * 根据ID物理删除用户（绕过 @TableLogic 逻辑删除）
     * 注意：此方法会真正从数据库删除记录，而不是设置 deleted = 1
     *
     * @param id 用户ID
     * @return 删除结果
     */
    @InterceptorIgnore(tenantLine = "true")
    int deletePhysicalById(@Param("id") Long id);

    /**
     * 批量物理删除用户（绕过 @TableLogic 逻辑删除）
     * 注意：此方法会真正从数据库删除记录，而不是设置 deleted = 1
     *
     * @param ids 用户ID列表
     * @return 删除结果
     */
    @InterceptorIgnore(tenantLine = "true")
    int deletePhysicalByIds(@Param("ids") List<Long> ids);
}
