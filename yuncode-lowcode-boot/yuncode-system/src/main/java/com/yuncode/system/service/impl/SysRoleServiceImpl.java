package com.yuncode.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuncode.common.utils.SecurityUtil;
import com.yuncode.system.entity.SysRole;
import com.yuncode.system.entity.SysRoleUser;
import com.yuncode.system.entity.SysUser;
import com.yuncode.system.entity.SysOrg;
import com.yuncode.system.entity.SysMenu;
import com.yuncode.system.mapper.SysRoleMapper;
import com.yuncode.system.mapper.SysRoleUserMapper;
import com.yuncode.system.mapper.SysRoleDeptMapper;
import com.yuncode.system.mapper.SysRolePermissionMapper;
import com.yuncode.system.mapper.SysUserMapper;
import com.yuncode.system.mapper.SysOrgMapper;
import com.yuncode.system.mapper.SysMenuMapper;
import com.yuncode.system.service.SysRoleService;
import com.yuncode.system.vo.RoleDetailVO;
import com.yuncode.system.vo.RoleNodeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleUserMapper roleUserMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserMapper userMapper;
    private final SysOrgMapper orgMapper;
    private final SysMenuMapper menuMapper;

    /**
     * 获取角色树（三层结构：根节点 -> 分类 -> 角色）
     */
    @Override
    public List<RoleNodeVO> getRoleTree() {
        // 从 Sa-Token Session 获取登录类型
        String loginType = cn.dev33.satoken.stp.StpUtil.getSession().get("loginType", "");
        Long tenantId = cn.dev33.satoken.stp.StpUtil.getSession().get("tenantId", 0L);

        log.info("获取角色树 - loginType={}, tenantId={}", loginType, tenantId);

        // 查询所有分类（role_type = 1）
        List<SysRole> categories;
        if ("admin".equals(loginType)) {
            // 平台管理员：查询所有租户的角色分类
            categories = roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getRoleType, 1)
                    .eq(SysRole::getDeleted, 0)
                    .orderByAsc(SysRole::getSortOrder)
            );
            log.info("平台管理员查询到 {} 个分类", categories.size());
        } else {
            // 租户用户：只查询本租户的角色分类
            categories = roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getRoleType, 1)
                    .eq(SysRole::getTenantId, tenantId)
                    .eq(SysRole::getDeleted, 0)
                    .orderByAsc(SysRole::getSortOrder)
            );
            log.info("租户用户查询到 {} 个分类", categories.size());
        }

        // 创建虚拟        rootNode.setId(-1L);  // 使用负ID表示虚拟节点根节点
        RoleNodeVO rootNode = new RoleNodeVO();
        rootNode.setParentId(0L);
        rootNode.setLabel("角色");
        rootNode.setRoleType(0);  // 0 表示根节点
        rootNode.setSortOrder(0);
        rootNode.setStatus(0);

        // 构建子节点（分类）
        List<RoleNodeVO> categoryNodes = new ArrayList<>();
        for (SysRole category : categories) {
            RoleNodeVO categoryNode = convertToNodeVO(category);

            // 查询该分类下的角色
            List<SysRole> roles;
            if ("admin".equals(loginType)) {
                // 平台管理员：查询所有租户的角色
                roles = roleMapper.selectList(
                    new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getParentId, category.getId())
                        .eq(SysRole::getDeleted, 0)
                        .orderByAsc(SysRole::getSortOrder)
                );
            } else {
                // 租户用户：只查询本租户的角色
                roles = roleMapper.selectList(
                    new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getParentId, category.getId())
                        .eq(SysRole::getTenantId, tenantId)
                        .eq(SysRole::getDeleted, 0)
                        .orderByAsc(SysRole::getSortOrder)
                );
            }

            log.info("分类 {} 下查询到 {} 个角色", category.getRoleName(), roles.size());

            List<RoleNodeVO> children = new ArrayList<>();
            for (SysRole role : roles) {
                children.add(convertToNodeVO(role));
            }
            categoryNode.setChildren(children);

            categoryNodes.add(categoryNode);
        }

        rootNode.setChildren(categoryNodes);

        // 返回包含根节点的单元素列表
        List<RoleNodeVO> tree = new ArrayList<>();
        tree.add(rootNode);
        return tree;
    }

    /**
     * 获取角色详情
     */
    @Override
    public RoleDetailVO getRoleDetail(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        checkTenantAccess(role);

        RoleDetailVO detail = new RoleDetailVO();
        BeanUtils.copyProperties(role, detail);

        // 查询分类名称
        if (role.getParentId() != null && role.getParentId() != 0L) {
            SysRole parent = roleMapper.selectById(role.getParentId());
            if (parent != null) {
                detail.setCategoryName(parent.getRoleName());
            }
        }

        // 查询用户信息
        List<Long> userIds = roleUserMapper.selectUserIdsByRoleId(id);
        List<RoleDetailVO.RoleUserVO> users = new ArrayList<>();
        if (userIds != null && !userIds.isEmpty()) {
            List<SysUser> userList = userMapper.selectBatchIds(userIds);
            // 过滤掉无效的用户（可能租户不同或已删除）
            if (userList != null && !userList.isEmpty()) {
                for (SysUser user : userList) {
                    if (user != null) {
                        RoleDetailVO.RoleUserVO userVO = new RoleDetailVO.RoleUserVO();
                        userVO.setUserId(user.getId());
                        userVO.setUserName(user.getUsername());
                        userVO.setRealName(user.getRealName());
                        users.add(userVO);
                    } else {
                        log.warn("发现无效的用户ID，role_id={}", id);
                    }
                }
            }
        }
        detail.setUsers(users);

        // 查询部门信息
        List<Long> deptIds = roleDeptMapper.selectDeptIdsByRoleId(id);
        List<RoleDetailVO.RoleDeptVO> depts = new ArrayList<>();
        if (deptIds != null && !deptIds.isEmpty()) {
            List<SysOrg> deptList = orgMapper.selectBatchIds(deptIds);
            for (SysOrg dept : deptList) {
                RoleDetailVO.RoleDeptVO deptVO = new RoleDetailVO.RoleDeptVO();
                deptVO.setDeptId(dept.getId());
                deptVO.setDeptName(dept.getOrgName());
                depts.add(deptVO);
            }
        }
        detail.setDepts(depts);

        // 查询权限信息
        List<Long> permissionIds = rolePermissionMapper.selectPermissionIdsByRoleId(id);
        List<RoleDetailVO.RolePermissionVO> permissions = new ArrayList<>();
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<SysMenu> menuList = menuMapper.selectBatchIds(permissionIds);
            for (SysMenu menu : menuList) {
                RoleDetailVO.RolePermissionVO permVO = new RoleDetailVO.RolePermissionVO();
                permVO.setPermissionId(menu.getId());
                permVO.setPermissionName(menu.getMenuName());
                permVO.setPermissionCode(menu.getPermission() != null ? menu.getPermission() : "");
                permissions.add(permVO);
            }
        }
        detail.setPermissions(permissions);

        return detail;
    }

    /**
     * 新增角色
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRole(SysRole role) {
        // 从 Sa-Token Session 获取当前登录用户的租户ID
        Long tenantId = cn.dev33.satoken.stp.StpUtil.getSession().get("tenantId", 2L);  // 默认使用系统租户ID
        role.setTenantId(tenantId);
        role.setCreateTime(LocalDateTime.now());

        roleMapper.insert(role);
        log.info("创建角色成功: tenantId={}, role={}", tenantId, role.getRoleName());
    }

    /**
     * 编辑角色
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Long id, SysRole role) {
        SysRole existing = roleMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("角色不存在");
        }
        checkTenantAccess(existing);

        role.setId(id);
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.updateById(role);

        log.info("更新角色成功: {}", role);
    }

    /**
     * 删除角色
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        checkTenantAccess(role);

        // 检查是否有子角色
        Long count = roleMapper.selectCount(
            new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getParentId, id)
        );
        if (count > 0) {
            throw new RuntimeException("该角色下存在子角色，无法删除");
        }

        // 删除关联数据
        roleUserMapper.deleteByRoleId(id);
        roleDeptMapper.deleteByRoleId(id);
        rolePermissionMapper.deleteByRoleId(id);

        // 删除角色
        roleMapper.deleteById(id);

        log.info("删除角色成功: id={}", id);
    }

    private RoleNodeVO convertToNodeVO(SysRole role) {
        RoleNodeVO node = new RoleNodeVO();
        node.setId(role.getId());
        node.setParentId(role.getParentId());
        node.setLabel(role.getRoleName());
        node.setRoleName(role.getRoleName());  // 同时设置 roleName
        node.setRoleType(role.getRoleType());
        node.setRoleCode(role.getRoleCode());
        node.setDescription(role.getDescription());
        node.setSortOrder(role.getSortOrder());
        node.setStatus(role.getStatus());
        return node;
    }

    /**
     * 校验租户访问权限：非管理员只能操作自己租户的数据
     */
    private void checkTenantAccess(SysRole role) {
        if (!SecurityUtil.isPlatformAdmin()) {
            Long tenantId = SecurityUtil.getTenantIdOrNull();
            if (tenantId != null && !tenantId.equals(role.getTenantId())) {
                log.warn("跨租户访问角色被拦截: roleId={}, roleTenantId={}, userTenantId={}",
                        role.getId(), role.getTenantId(), tenantId);
                throw new RuntimeException("角色不存在");
            }
        }
    }
}
