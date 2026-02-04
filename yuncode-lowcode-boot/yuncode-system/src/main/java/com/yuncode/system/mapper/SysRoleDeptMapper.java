package com.yuncode.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuncode.system.entity.SysRoleDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色部门关联Mapper
 */
@Mapper
public interface SysRoleDeptMapper extends BaseMapper<SysRoleDept> {

    /**
     * 根据角色ID查询部门列表
     *
     * @param roleId 角色ID
     * @return 部门ID列表
     */
    List<Long> selectDeptIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据角色ID删除关联
     *
     * @param roleId 角色ID
     * @return 删除数量
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量插入角色部门关联
     *
     * @param list 关联列表
     * @return 插入数量
     */
    int batchInsert(@Param("list") List<SysRoleDept> list);
}
