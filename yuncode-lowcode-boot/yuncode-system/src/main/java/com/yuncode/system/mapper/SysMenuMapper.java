package com.yuncode.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuncode.system.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜单Mapper接口
 *
 * @author Yuncode
 * @since 2025-01-30
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 获取菜单树
     *
     * @return 菜单树列表
     */
    List<SysMenu> selectMenuTree();

    /**
     * 根据租户ID获取菜单列表
     *
     * @param tenantId 租户ID
     * @return 菜单列表
     */
    List<SysMenu> selectMenusByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 获取用户可访问的菜单列表
     *
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @return 菜单列表
     */
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    /**
     * 更新菜单排序
     *
     * @param menuId 菜单ID
     * @param newSortOrder 新排序号
     * @return 是否成功
     */
    int updateSortOrder(@Param("menuId") Long menuId, @Param("newSortOrder") Integer newSortOrder);

    /**
     * 交换两个菜单的排序号
     *
     * @param menuId1 菜单ID1
     * @param menuId2 菜单ID2
     * @return 是否成功
     */
    int swapSortOrder(@Param("menuId1") Long menuId1, @Param("menuId2") Long menuId2);

    /**
     * 获取子菜单数量
     *
     * @param parentId 父菜单ID
     * @return 子菜单数量
     */
    Long countChildMenus(@Param("parentId") Long parentId);

    /**
     * 删除菜单及其所有子菜单（逻辑删除）
     *
     * @param menuId 菜单ID
     * @return 删除的菜单数量
     */
    int deleteMenuRecursively(@Param("menuId") Long menuId);

    /**
     * 检查菜单下是否有权限配置
     *
     * @param menuId 菜单ID
     * @return 权限配置数量
     */
    Long countPermissionsByMenuId(@Param("menuId") Long menuId);
}
