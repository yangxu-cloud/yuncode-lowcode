package com.yuncode.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuncode.system.entity.SysRolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色权限关联Mapper
 */
@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    /**
     * 根据角色ID查询权限列表
     *
     * @param roleId 角色ID
     * @return 权限ID列表
     */
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据角色ID删除关联
     *
     * @param roleId 角色ID
     * @return 删除数量
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量插入角色权限关联
     *
     * @param list 关联列表
     * @return 插入数量
     */
    int batchInsert(@Param("list") List<SysRolePermission> list);
}
