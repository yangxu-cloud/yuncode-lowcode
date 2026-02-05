package com.yuncode.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuncode.system.entity.*;
import com.yuncode.system.mapper.*;
import com.yuncode.system.service.SysRoleService;
import com.yuncode.system.vo.RoleDetailVO;
import com.yuncode.system.vo.RoleNodeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
@Service
public class SysRoleServiceImpl implements SysRoleService {

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysRoleUserMapper roleUserMapper;

    @Autowired
    private SysRoleDeptMapper roleDeptMapper;

    @Autowired
    private SysRolePermissionMapper rolePermissionMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysOrgMapper orgMapper;

    @Autowired
    private SysMenuMapper menuMapper;

    /**
     * 获取角色树（三层结构：根节点 -> 分类 -> 角色）
     */
    @Override
    public List<RoleNodeVO> getRoleTree() {
        // 查询所有分类（role_type = 1）
        List<SysRole> categories = roleMapper.selectList(
            new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleType, 1)
                .eq(SysRole::getDeleted, 0)
                .orderByAsc(SysRole::getSortOrder)
        );

        // 创建虚拟根节点
        RoleNodeVO rootNode = new RoleNodeVO();
        rootNode.setId(-1L);  // 使用负ID表示虚拟节点
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
            List<SysRole> roles = roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getParentId, category.getId())
                    .eq(SysRole::getDeleted, 0)
                    .orderByAsc(SysRole::getSortOrder)
            );
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
            for (SysUser user : userList) {
                RoleDetailVO.RoleUserVO userVO = new RoleDetailVO.RoleUserVO();
                userVO.setUserId(user.getId());
                userVO.setUserName(user.getUsername());
                userVO.setRealName(user.getRealName());
                users.add(userVO);
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

    /**
     * 添加人员到角色
     */
    @Override
    public void addUsersToRole(Long roleId, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        // 查询已存在的人员ID，过滤掉重复的
        List<SysRoleUser> existingUsers = roleUserMapper.selectList(
            new LambdaQueryWrapper<SysRoleUser>()
                .eq(SysRoleUser::getRoleId, roleId)
                .in(SysRoleUser::getUserId, userIds)
        );

        List<Long> existingUserIds = existingUsers.stream()
            .map(SysRoleUser::getUserId)
            .collect(Collectors.toList());

        // 过滤掉已存在的人员
        List<Long> newUserIds = userIds.stream()
            .filter(userId -> !existingUserIds.contains(userId))
            .collect(Collectors.toList());

        if (newUserIds.isEmpty()) {
            log.info("所有人员都已存在，无需添加: roleId={}", roleId);
            return;
        }

        // 从 Sa-Token Session 获取当前登录用户的租户ID
        Long tenantId = cn.dev33.satoken.stp.StpUtil.getSession().get("tenantId", 2L);

        List<SysRoleUser> list = newUserIds.stream()
            .map(userId -> {
                SysRoleUser roleUser = new SysRoleUser();
                roleUser.setRoleId(roleId);
                roleUser.setUserId(userId);
                roleUser.setTenantId(tenantId);
                roleUser.setCreateTime(LocalDateTime.now());
                return roleUser;
            })
            .collect(Collectors.toList());

        roleUserMapper.batchInsert(list);
        log.info("添加人员到角色成功: roleId={}, count={}", roleId, newUserIds.size());
    }

    /**
     * 从角色移除人员
     */
    @Override
    public void removeUserFromRole(Long roleId, Long userId) {
        roleUserMapper.delete(
            new LambdaQueryWrapper<SysRoleUser>()
                .eq(SysRoleUser::getRoleId, roleId)
                .eq(SysRoleUser::getUserId, userId)
        );
        log.info("从角色移除人员成功: roleId={}, userId={}", roleId, userId);
    }

    /**
     * 添加部门到角色
     */
    @Override
    public void addDeptsToRole(Long roleId, List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return;
        }

        // 查询已存在的部门ID，过滤掉重复的
        List<SysRoleDept> existingDepts = roleDeptMapper.selectList(
            new LambdaQueryWrapper<SysRoleDept>()
                .eq(SysRoleDept::getRoleId, roleId)
                .in(SysRoleDept::getDeptId, deptIds)
        );

        List<Long> existingDeptIds = existingDepts.stream()
            .map(SysRoleDept::getDeptId)
            .collect(Collectors.toList());

        // 过滤掉已存在的部门
        List<Long> newDeptIds = deptIds.stream()
            .filter(deptId -> !existingDeptIds.contains(deptId))
            .collect(Collectors.toList());

        if (newDeptIds.isEmpty()) {
            log.info("所有部门都已存在，无需添加: roleId={}", roleId);
            return;
        }

        // 从 Sa-Token Session 获取当前登录用户的租户ID
        Long tenantId = cn.dev33.satoken.stp.StpUtil.getSession().get("tenantId", 2L);

        List<SysRoleDept> list = newDeptIds.stream()
            .map(deptId -> {
                SysRoleDept roleDept = new SysRoleDept();
                roleDept.setRoleId(roleId);
                roleDept.setDeptId(deptId);
                roleDept.setTenantId(tenantId);
                roleDept.setCreateTime(LocalDateTime.now());
                return roleDept;
            })
            .collect(Collectors.toList());

        roleDeptMapper.batchInsert(list);
        log.info("添加部门到角色成功: roleId={}, count={}", roleId, newDeptIds.size());
    }

    /**
     * 从角色移除部门
     */
    @Override
    public void removeDeptFromRole(Long roleId, Long deptId) {
        roleDeptMapper.delete(
            new LambdaQueryWrapper<SysRoleDept>()
                .eq(SysRoleDept::getRoleId, roleId)
                .eq(SysRoleDept::getDeptId, deptId)
        );
        log.info("从角色移除部门成功: roleId={}, deptId={}", roleId, deptId);
    }

    /**
     * 添加权限到角色
     */
    @Override
    public void addPermissionsToRole(Long roleId, List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return;
        }

        // 查询已存在的权限ID，过滤掉重复的
        List<SysRolePermission> existingPermissions = rolePermissionMapper.selectList(
            new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId)
                .in(SysRolePermission::getPermissionId, permissionIds)
        );

        List<Long> existingPermissionIds = existingPermissions.stream()
            .map(SysRolePermission::getPermissionId)
            .collect(Collectors.toList());

        // 过滤掉已存在的权限
        List<Long> newPermissionIds = permissionIds.stream()
            .filter(permissionId -> !existingPermissionIds.contains(permissionId))
            .collect(Collectors.toList());

        if (newPermissionIds.isEmpty()) {
            log.info("所有权限都已存在，无需添加: roleId={}", roleId);
            return;
        }

        // 从 Sa-Token Session 获取当前登录用户的租户ID
        Long tenantId = cn.dev33.satoken.stp.StpUtil.getSession().get("tenantId", 2L);

        List<SysRolePermission> list = newPermissionIds.stream()
            .map(permissionId -> {
                SysRolePermission rolePermission = new SysRolePermission();
                rolePermission.setRoleId(roleId);
                rolePermission.setPermissionId(permissionId);
                rolePermission.setTenantId(tenantId);
                rolePermission.setCreateTime(LocalDateTime.now());
                return rolePermission;
            })
            .collect(Collectors.toList());

        rolePermissionMapper.batchInsert(list);
        log.info("添加权限到角色成功: roleId={}, count={}", roleId, newPermissionIds.size());
    }

    /**
     * 从角色移除权限
     */
    @Override
    public void removePermissionFromRole(Long roleId, Long permissionId) {
        rolePermissionMapper.delete(
            new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId)
                .eq(SysRolePermission::getPermissionId, permissionId)
        );
        log.info("从角色移除权限成功: roleId={}, permissionId={}", roleId, permissionId);
    }

    /**
     * 转换为节点VO
     */
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
}
