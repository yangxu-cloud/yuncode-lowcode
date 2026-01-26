package com.yuncode.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuncode.system.entity.SysOrg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 组织Mapper接口
 *
 * @author Yuncode
 * @since 2025-01-22
 */
@Mapper
public interface SysOrgMapper extends BaseMapper<SysOrg> {

    /**
     * 查询组织的子组织列表
     *
     * @param parentId 父组织ID
     * @param tenantId 租户ID
     * @return 子组织列表
     */
    @Select("SELECT * FROM sys_org WHERE parent_id = #{parentId} AND tenant_id = #{tenantId} AND deleted = 0 ORDER BY sort_order")
    List<SysOrg> selectByParentId(@Param("parentId") Long parentId, @Param("tenantId") Long tenantId);

    /**
     * 查询组织树
     *
     * @param tenantId 租户ID
     * @return 组织列表
     */
    @Select("SELECT * FROM sys_org WHERE tenant_id = #{tenantId} AND deleted = 0 ORDER BY sort_order")
    List<SysOrg> selectOrgTree(@Param("tenantId") Long tenantId);

    /**
     * 搜索组织（按名称或编码）
     *
     * @param keyword 关键词
     * @param tenantId 租户ID
     * @return 组织列表
     */
    @Select("SELECT * FROM sys_org WHERE (org_name LIKE CONCAT('%', #{keyword}, '%') OR org_code LIKE CONCAT('%', #{keyword}, '%')) AND tenant_id = #{tenantId} AND deleted = 0")
    List<SysOrg> searchOrgs(@Param("keyword") String keyword, @Param("tenantId") Long tenantId);
}
