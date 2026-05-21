package com.yuncode.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuncode.system.entity.SysMenuPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜单权限Mapper接口
 *
 * @author Yuncode
 * @since 2025-01-30
 */
@Mapper
public interface SysMenuPermissionMapper extends BaseMapper<SysMenuPermission> {

    /**
     * 根据菜单ID获取权限列表
     *
     * @param menuId 菜单ID
     * @return 权限列表
     */
    List<SysMenuPermission> selectPermissionsByMenuId(@Param("menuId") Long menuId);

    /**
     * 根据目标类型和目标ID获取菜单列表
     *
     * @param targetType 目标类型:0=角色,1=用户,2=部门
     * @param targetId 目标ID
     * @return 菜单列表
     */
    List<SysMenuPermission> selectMenusByTarget(@Param("targetType") Integer targetType,
                                             @Param("targetId") Long targetId);

    /**
     * 批量插入菜单权限
     * 注意：使用 @InterceptorIgnore 忽略多租户插件，因为菜单权限表有自己的 tenant_id 字段
     *
     * @param permissions 权限列表
     * @return 插入数量
     */
    @InterceptorIgnore(tenantLine = "true")
    int batchInsert(@Param("permissions") List<SysMenuPermission> permissions);

    /**
     * 根据菜单ID删除权限
     *
     * @param menuId 菜单ID
     * @return 删除数量
     */
    int deleteByMenuId(@Param("menuId") Long menuId);

    /**
     * 根据目标删除权限
     *
     * @param targetType 目标类型
     * @param targetId 目标ID
     * @return 删除数量
     */
    int deleteByTarget(@Param("targetType") Integer targetType, @Param("targetId") Long targetId);

    /**
     * 根据菜单ID、目标类型和目标ID删除权限
     *
     * @param menuId 菜单ID
     * @param targetType 目标类型
     * @param targetId 目标ID
     * @return 删除数量
     */
    int deleteByMenuIdAndTarget(@Param("menuId") Long menuId,
                                @Param("targetType") Integer targetType,
                                @Param("targetId") Long targetId);

    /**
     * 权限追加到下级
     * 将当前菜单的所有权限追加到所有子菜单
     *
     * @param parentMenuId 父菜单ID
     * @return 追加的权限数量
     */
    int copyPermissionsToChildren(@Param("parentMenuId") Long parentMenuId);
}
