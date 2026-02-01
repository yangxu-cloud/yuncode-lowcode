package com.yuncode.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuncode.common.util.security.SecurityUtil;
import com.yuncode.system.dto.MenuForm;
import com.yuncode.system.entity.SysMenu;
import com.yuncode.system.entity.SysMenuPermission;
import com.yuncode.system.mapper.SysMenuMapper;
import com.yuncode.system.mapper.SysMenuPermissionMapper;
import com.yuncode.system.service.MenuService;
import com.yuncode.system.vo.MenuPermissionVO;
import com.yuncode.system.vo.MenuTreeNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单服务实现类
 *
 * @author Yuncode
 * @since 2025-01-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final SysMenuMapper menuMapper;
    private final SysMenuPermissionMapper menuPermissionMapper;

    @Override
    public List<MenuTreeNode> getMenuTree() {
        List<SysMenu> allMenus;
        try {
            // 查询所有菜单（包括默认菜单和所有租户菜单）
            allMenus = menuMapper.selectMenuTree();
            log.info("查询到菜单数量: {}", allMenus.size());

            // 如果不是平台管理员，过滤出当前租户的菜单和默认菜单
            if (!SecurityUtil.isPlatformAdmin()) {
                Long tenantId = SecurityUtil.getTenantId();
                log.info("过滤租户菜单（租户ID: {}）", tenantId);
                // 保留：默认菜单（tenantId=null）+ 当前租户的菜单
                allMenus = allMenus.stream()
                        .filter(menu -> menu.getTenantId() == null || menu.getTenantId().equals(tenantId))
                        .collect(Collectors.toList());
                log.info("过滤后菜单数量: {}", allMenus.size());
            }
        } catch (Exception e) {
            log.warn("获取菜单失败", e);
            allMenus = new ArrayList<>();
        }

        // 构建菜单树
        List<MenuTreeNode> treeNodes = new ArrayList<>();
        for (SysMenu menu : allMenus) {
            MenuTreeNode node = convertToTreeNode(menu);
            treeNodes.add(node);
        }

        return buildMenuTree(treeNodes, 0L);
    }

    @Override
    public List<MenuTreeNode> getMenuTreeByTenantId(Long tenantId) {
        List<SysMenu> menus = menuMapper.selectMenusByTenantId(tenantId);
        List<MenuTreeNode> treeNodes = new ArrayList<>();
        for (SysMenu menu : menus) {
            MenuTreeNode node = convertToTreeNode(menu);
            treeNodes.add(node);
        }
        return buildMenuTree(treeNodes, 0L);
    }

    @Override
    public List<MenuTreeNode> getMenuTreeByUserId(Long userId, Long tenantId) {
        List<SysMenu> menus = menuMapper.selectMenusByUserId(userId, tenantId);
        List<MenuTreeNode> treeNodes = new ArrayList<>();
        for (SysMenu menu : menus) {
            MenuTreeNode node = convertToTreeNode(menu);
            treeNodes.add(node);
        }
        return buildMenuTree(treeNodes, 0L);
    }

    @Override
    public SysMenu getMenuById(Long id) {
        return menuMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addMenu(MenuForm menuForm) {
        SysMenu menu = new SysMenu();
        BeanUtils.copyProperties(menuForm, menu);

        // 设置默认值
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (menu.getSortOrder() == null) {
            menu.setSortOrder(0);
        }
        if (menu.getVisible() == null) {
            menu.setVisible(0); // 0=显示
        }
        if (menu.getStatus() == null) {
            menu.setStatus(0); // 0=正常
        }

        // 如果是子菜单，继承父菜单的租户信息
        if (menu.getParentId() != null && menu.getParentId() > 0) {
            SysMenu parentMenu = menuMapper.selectById(menu.getParentId());
            if (parentMenu != null) {
                if (menu.getTenantId() == null) {
                    menu.setTenantId(parentMenu.getTenantId());
                }
                if (menu.getTenantCode() == null) {
                    menu.setTenantCode(parentMenu.getTenantCode());
                }
            }
        }

        return menuMapper.insert(menu) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine = "true")
    public boolean updateMenu(MenuForm menuForm) {
        // 先查询出原始菜单，保留租户信息（使用忽略多租户的方法）
        SysMenu existingMenu = menuMapper.selectByIdIgnoreTenant(menuForm.getId());
        if (existingMenu == null) {
            throw new RuntimeException("菜单不存在");
        }

        SysMenu menu = new SysMenu();
        BeanUtils.copyProperties(menuForm, menu);

        // 保留原始租户信息，防止误修改
        menu.setTenantId(existingMenu.getTenantId());
        menu.setTenantCode(existingMenu.getTenantCode());

        // 使用忽略多租户的更新方法
        return menuMapper.updateByIdIgnoreTenant(menu) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMenu(Long id) {
        log.info("删除菜单: id={}", id);

        // 检查是否有子菜单
        Long childCount = menuMapper.countChildMenus(id);
        if (childCount > 0) {
            throw new RuntimeException("该菜单下有子菜单，不能删除");
        }

        // 检查是否有权限配置
        Long permissionCount = menuMapper.countPermissionsByMenuId(id);
        if (permissionCount > 0) {
            throw new RuntimeException("该菜单已配置权限，请先删除权限配置");
        }

        return menuMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean moveUp(Long menuId) {
        SysMenu currentMenu = menuMapper.selectById(menuId);
        if (currentMenu == null) {
            throw new RuntimeException("菜单不存在");
        }

        // 查找同级上一个菜单
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getParentId, currentMenu.getParentId())
                .lt(SysMenu::getSortOrder, currentMenu.getSortOrder())
                .eq(SysMenu::getDeleted, 0)
                .orderByDesc(SysMenu::getSortOrder)
                .last("LIMIT 1");

        SysMenu prevMenu = menuMapper.selectOne(wrapper);
        if (prevMenu == null) {
            throw new RuntimeException("已经是第一个菜单了");
        }

        // 交换排序号
        return menuMapper.swapSortOrder(menuId, prevMenu.getId()) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean moveDown(Long menuId) {
        SysMenu currentMenu = menuMapper.selectById(menuId);
        if (currentMenu == null) {
            throw new RuntimeException("菜单不存在");
        }

        // 查找同级下一个菜单
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getParentId, currentMenu.getParentId())
                .gt(SysMenu::getSortOrder, currentMenu.getSortOrder())
                .eq(SysMenu::getDeleted, 0)
                .orderByAsc(SysMenu::getSortOrder)
                .last("LIMIT 1");

        SysMenu nextMenu = menuMapper.selectOne(wrapper);
        if (nextMenu == null) {
            throw new RuntimeException("已经是最后一个菜单了");
        }

        // 交换排序号
        return menuMapper.swapSortOrder(menuId, nextMenu.getId()) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setVisible(Long menuId, Integer visible) {
        SysMenu menu = new SysMenu();
        menu.setId(menuId);
        menu.setVisible(visible);
        return menuMapper.updateById(menu) > 0;
    }

    @Override
    public List<SysMenu> searchMenus(String keyword) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.like(SysMenu::getMenuName, keyword)
                .or()
                .like(SysMenu::getPath, keyword));
        return menuMapper.selectList(wrapper);
    }

    @Override
    public List<MenuPermissionVO> getMenuPermissions(Long menuId) {
        List<SysMenuPermission> permissions = menuPermissionMapper.selectPermissionsByMenuId(menuId);
        return permissions.stream()
                .map(this::convertToPermissionVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addPermissions(Long menuId, Integer targetType, List<Long> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            return false;
        }

        // 获取菜单信息
        SysMenu menu = menuMapper.selectById(menuId);
        if (menu == null) {
            throw new RuntimeException("菜单不存在");
        }

        // 批量插入权限
        List<SysMenuPermission> permissions = new ArrayList<>();
        for (Long targetId : targetIds) {
            SysMenuPermission permission = new SysMenuPermission();
            permission.setMenuId(menuId);
            permission.setTargetType(targetType);
            permission.setTargetId(targetId);
            permission.setTenantId(menu.getTenantId());
            permission.setDeleted(0);
            permissions.add(permission);
        }

        return menuPermissionMapper.batchInsert(permissions) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removePermission(Long menuId, Integer targetType, Long targetId) {
        int count = menuPermissionMapper.deleteByMenuId(menuId);
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int copyPermissionsToChildren(Long menuId) {
        return menuPermissionMapper.copyPermissionsToChildren(menuId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initDefaultMenus() {
        log.info("开始初始化默认菜单...");

        // 检查是否已存在默认菜单（使用 selectMenuTree 避免多租户插件拦截）
        List<SysMenu> allMenus = menuMapper.selectMenuTree();
        boolean exists = allMenus.stream()
                .anyMatch(menu -> "办公".equals(menu.getMenuName()) && menu.getTenantId() == null);

        if (exists) {
            log.info("默认菜单已存在，跳过初始化");
            return;
        }

        // 创建默认菜单结构：办公 > 常用、工具、通讯录
        // 办公（一级菜单）
        SysMenu office = new SysMenu();
        office.setMenuName("办公");
        office.setIcon("OfficeBuilding");
        office.setParentId(0L);
        office.setMenuType(0); // 目录
        office.setSortOrder(0);
        office.setVisible(0); // 显示
        office.setStatus(0); // 正常
        office.setTenantId(null); // 默认菜单，所有租户可见
        menuMapper.insert(office);

        // 常用（二级菜单）
        SysMenu common = new SysMenu();
        common.setMenuName("常用");
        common.setParentId(office.getId());
        common.setMenuType(0); // 目录
        common.setSortOrder(0);
        common.setVisible(0);
        common.setStatus(0);
        common.setTenantId(null);
        menuMapper.insert(common);

        // 流程中心（三级菜单）
        SysMenu workflow = new SysMenu();
        workflow.setMenuName("流程中心");
        workflow.setIcon("Operation");
        workflow.setParentId(common.getId());
        workflow.setMenuType(1); // 菜单
        workflow.setPath("/workflow");
        workflow.setSortOrder(0);
        workflow.setVisible(0);
        workflow.setStatus(0);
        workflow.setTenantId(null);
        menuMapper.insert(workflow);

        // 邮件（三级菜单）
        SysMenu email = new SysMenu();
        email.setMenuName("邮件");
        email.setIcon("Message");
        email.setParentId(common.getId());
        email.setMenuType(1); // 菜单
        email.setPath("/email");
        email.setSortOrder(1);
        email.setVisible(0);
        email.setStatus(0);
        email.setTenantId(null);
        menuMapper.insert(email);

        // 网盘（三级菜单）
        SysMenu netdisk = new SysMenu();
        netdisk.setMenuName("网盘");
        netdisk.setIcon("FolderOpened");
        netdisk.setParentId(common.getId());
        netdisk.setMenuType(1); // 菜单
        netdisk.setPath("/netdisk");
        netdisk.setSortOrder(2);
        netdisk.setVisible(0);
        netdisk.setStatus(0);
        netdisk.setTenantId(null);
        menuMapper.insert(netdisk);

        // 工具（二级菜单）
        SysMenu tools = new SysMenu();
        tools.setMenuName("工具");
        tools.setParentId(office.getId());
        tools.setMenuType(0); // 目录
        tools.setSortOrder(1);
        tools.setVisible(0);
        tools.setStatus(0);
        tools.setTenantId(null);
        menuMapper.insert(tools);

        // 通讯录（二级菜单）
        SysMenu contacts = new SysMenu();
        contacts.setMenuName("通讯录");
        contacts.setIcon("AddressBook");
        contacts.setParentId(office.getId());
        contacts.setMenuType(0); // 目录
        contacts.setSortOrder(2);
        contacts.setVisible(0);
        contacts.setStatus(0);
        contacts.setTenantId(null);
        menuMapper.insert(contacts);

        // 单位通讯录（三级菜单）
        SysMenu companyContacts = new SysMenu();
        companyContacts.setMenuName("单位通讯录");
        companyContacts.setIcon("User");
        companyContacts.setParentId(contacts.getId());
        companyContacts.setMenuType(1); // 菜单
        companyContacts.setPath("/contacts/company");
        companyContacts.setSortOrder(0);
        companyContacts.setVisible(0);
        companyContacts.setStatus(0);
        companyContacts.setTenantId(null);
        menuMapper.insert(companyContacts);

        // 个人通讯录（三级菜单）
        SysMenu personalContacts = new SysMenu();
        personalContacts.setMenuName("个人通讯录");
        personalContacts.setIcon("UserFilled");
        personalContacts.setParentId(contacts.getId());
        personalContacts.setMenuType(1); // 菜单
        personalContacts.setPath("/contacts/personal");
        personalContacts.setSortOrder(1);
        personalContacts.setVisible(0);
        personalContacts.setStatus(0);
        personalContacts.setTenantId(null);
        menuMapper.insert(personalContacts);

        log.info("默认菜单初始化完成");
    }

    /**
     * 构建菜单树
     */
    private List<MenuTreeNode> buildMenuTree(List<MenuTreeNode> nodes, Long parentId) {
        List<MenuTreeNode> result = new ArrayList<>();

        for (MenuTreeNode node : nodes) {
            if (node.getParentId().equals(parentId)) {
                // 递归获取子节点
                List<MenuTreeNode> children = buildMenuTree(nodes, node.getId());
                node.setChildren(children);
                result.add(node);
            }
        }

        // 按照排序号排序
        result.sort((a, b) -> {
            Integer sortA = a.getSortOrder() != null ? a.getSortOrder() : 999;
            Integer sortB = b.getSortOrder() != null ? b.getSortOrder() : 999;
            return sortA.compareTo(sortB);
        });

        return result;
    }

    /**
     * 转换为树节点
     */
    private MenuTreeNode convertToTreeNode(SysMenu menu) {
        MenuTreeNode node = new MenuTreeNode();
        BeanUtils.copyProperties(menu, node);
        return node;
    }

    /**
     * 转换为权限VO
     */
    private MenuPermissionVO convertToPermissionVO(SysMenuPermission permission) {
        MenuPermissionVO vo = new MenuPermissionVO();
        vo.setId(permission.getId());
        vo.setMenuId(permission.getMenuId());
        vo.setTargetType(permission.getTargetType());
        vo.setTargetId(permission.getTargetId());

        // 根据targetType设置类型名称
        switch (permission.getTargetType()) {
            case 0:
                vo.setTargetTypeName("角色");
                break;
            case 1:
                vo.setTargetTypeName("用户");
                break;
            case 2:
                vo.setTargetTypeName("部门");
                break;
            default:
                vo.setTargetTypeName("未知");
        }

        return vo;
    }
}
