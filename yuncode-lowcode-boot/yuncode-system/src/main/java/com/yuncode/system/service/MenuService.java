package com.yuncode.system.service;

import com.yuncode.system.dto.MenuForm;
import com.yuncode.system.entity.SysMenu;
import com.yuncode.system.entity.SysMenuPermission;
import com.yuncode.system.vo.MenuTreeNode;
import com.yuncode.system.vo.MenuPermissionVO;

import java.util.List;

/**
 * 菜单服务接口
 *
 * @author Yuncode
 * @since 2025-01-30
 */
public interface MenuService {

    /**
     * 获取菜单树
     *
     * @return 菜单树节点列表
     */
    List<MenuTreeNode> getMenuTree();

    /**
     * 根据租户ID获取菜单列表
     *
     * @param tenantId 租户ID（null表示获取默认菜单）
     * @return 菜单树列表
     */
    List<MenuTreeNode> getMenuTreeByTenantId(Long tenantId);

    /**
     * 获取用户可访问的菜单列表
     *
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @return 菜单树列表
     */
    List<MenuTreeNode> getMenuTreeByUserId(Long userId, Long tenantId);

    /**
     * 获取菜单详情
     *
     * @param id 菜单ID
     * @return 菜单实体
     */
    SysMenu getMenuById(Long id);

    /**
     * 添加菜单
     *
     * @param menuForm 菜单表单
     * @return 是否成功
     */
    boolean addMenu(MenuForm menuForm);

    /**
     * 更新菜单
     *
     * @param menuForm 菜单表单
     * @return 是否成功
     */
    boolean updateMenu(MenuForm menuForm);

    /**
     * 删除菜单（级联删除子菜单）
     *
     * @param id 菜单ID
     * @return 是否成功
     */
    boolean deleteMenu(Long id);

    /**
     * 上移菜单
     *
     * @param menuId 菜单ID
     * @return 是否成功
     */
    boolean moveUp(Long menuId);

    /**
     * 下移菜单
     *
     * @param menuId 菜单ID
     * @return 是否成功
     */
    boolean moveDown(Long menuId);

    /**
     * 设置菜单可见性
     *
     * @param menuId 菜单ID
     * @param visible 是否可见（0=显示, 1=隐藏）
     * @return 是否成功
     */
    boolean setVisible(Long menuId, Integer visible);

    /**
     * 搜索菜单
     *
     * @param keyword 关键词
     * @return 菜单列表
     */
    List<SysMenu> searchMenus(String keyword);

    /**
     * 获取菜单的权限列表
     *
     * @param menuId 菜单ID
     * @return 权限列表
     */
    List<MenuPermissionVO> getMenuPermissions(Long menuId);

    /**
     * 添加权限到菜单
     *
     * @param menuId 菜单ID
     * @param targetType 目标类型（0=角色, 1=用户, 2=部门）
     * @param targetIds 目标ID列表
     * @return 是否成功
     */
    boolean addPermissions(Long menuId, Integer targetType, List<Long> targetIds);

    /**
     * 移除菜单权限
     *
     * @param menuId 菜单ID
     * @param targetType 目标类型
     * @param targetId 目标ID
     * @return 是否成功
     */
    boolean removePermission(Long menuId, Integer targetType, Long targetId);

    /**
     * 权限追加到下级菜单
     * 将当前菜单的所有权限追加到所有子菜单
     *
     * @param menuId 菜单ID
     * @return 追加的权限数量
     */
    int copyPermissionsToChildren(Long menuId);

    /**
     * 初始化默认菜单
     * 在系统启动时调用，插入默认的办公菜单
     */
    void initDefaultMenus();
}
