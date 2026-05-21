package com.yuncode.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuncode.system.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色Mapper
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 查询角色列表（通过父级ID）
     *
     * @param tenantId 租户ID
     * @param parentId 父级ID
     * @return 角色列表
     */
    @InterceptorIgnore(tenantLine = "true")
    List<SysRole> selectByParentId(@Param("tenantId") Long tenantId, @Param("parentId") Long parentId);

    /**
     * 查询所有角色类型为分类的记录
     *
     * @param tenantId 租户ID
     * @return 角色分类列表
     */
    @InterceptorIgnore(tenantLine = "true")
    List<SysRole> selectCategories(@Param("tenantId") Long tenantId);

    /**
     * 根据ID查询角色（忽略租户限制）
     *
     * @param id 角色ID
     * @return 角色实体
     */
    @InterceptorIgnore(tenantLine = "true")
    SysRole selectByIdIgnoreTenant(@Param("id") Long id);
}
