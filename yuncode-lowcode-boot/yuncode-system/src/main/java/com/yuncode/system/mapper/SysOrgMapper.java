package com.yuncode.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuncode.system.entity.SysOrg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 组织Mapper接口
 *
 * 多租户说明：MyBatis-Plus 多租户插件会自动在所有 SQL 语句中添加 tenant_id 条件
 * 因此无需在 Mapper 方法中手动传递 tenantId 参数
 *
 * 例外场景：
 * 1. selectAllForPlatformAdmin - 平台管理员查询所有组织，忽略租户限制
 * 2. selectByIdIgnoreTenant - 根据ID查询组织，忽略租户限制（用于获取父组织信息）
 *
 * 注意：这些方法使用 XML 映射文件实现，并使用 @InterceptorIgnore 忽略多租户插件
 * XML 文件位置：resources/mapper/SysOrgMapper.xml
 *
 * @author Yuncode
 * @since 2025-01-22
 */
@Mapper
public interface SysOrgMapper extends BaseMapper<SysOrg> {

    /**
     * 查询所有组织（用于平台管理员，忽略租户限制）
     * 注意：此方法仅用于平台管理员查看所有租户的组织
     * 使用 @InterceptorIgnore 忽略多租户插件的自动过滤，不添加 tenant_id 条件
     *
     * @return 所有组织列表
     */
    @InterceptorIgnore(tenantLine = "true")
    List<SysOrg> selectAllForPlatformAdmin();

    /**
     * 根据ID查询组织（忽略租户限制）
     * 注意：此方法用于查询父组织以继承 company_id，需要忽略租户限制
     * 使用 @InterceptorIgnore 忽略多租户插件的自动过滤，不添加 tenant_id 条件
     *
     * @param id 组织ID
     * @return 组织实体
     */
    @InterceptorIgnore(tenantLine = "true")
    SysOrg selectByIdIgnoreTenant(@Param("id") Long id);

    /**
     * 根据ID删除组织（逻辑删除，忽略租户限制）
     * 注意：此方法用于删除组织时不受租户限制
     * 使用 @InterceptorIgnore 忽略多租户插件的自动过滤，不添加 tenant_id 条件
     *
     * @param id 组织ID
     * @return 删除的记录数
     */
    @InterceptorIgnore(tenantLine = "true")
    int deleteByIdIgnoreTenant(@Param("id") Long id);

    /**
     * 根据ID物理删除组织（绕过 @TableLogic 逻辑删除）
     * 注意：此方法会真正从数据库删除记录，而不是设置 deleted = 1
     *
     * @param id 组织ID
     * @return 删除的记录数
     */
    @InterceptorIgnore(tenantLine = "true")
    int deletePhysicalById(@Param("id") Long id);

    /**
     * 批量物理删除组织（绕过 @TableLogic 逻辑删除）
     * 注意：此方法会真正从数据库删除记录，而不是设置 deleted = 1
     *
     * @param ids 组织ID列表
     * @return 删除的记录数
     */
    @InterceptorIgnore(tenantLine = "true")
    int deletePhysicalByIds(@Param("ids") List<Long> ids);
}
