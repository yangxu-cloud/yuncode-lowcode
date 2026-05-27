package com.yuncode.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuncode.common.utils.SecurityUtil;
import com.yuncode.system.entity.*;
import com.yuncode.system.mapper.*;
import com.yuncode.system.service.RoleAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 角色分配服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RoleAssignmentServiceImpl implements RoleAssignmentService {

    private final SysRoleUserMapper roleUserMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserMapper userMapper;
    private final SysOrgMapper orgMapper;

    @Override
    public void addUsersToRole(Long roleId, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        Long tenantId = StpUtil.getSession().get("tenantId", 2L);
        String loginType = StpUtil.getSession().get("loginType", "user");

        log.info("添加人员到角色 - roleId={}, userIds={}, loginType={}, tenantId={}",
            roleId, userIds, loginType, tenantId);

        List<SysUser> validUsers;
        if ("admin".equals(loginType)) {
            validUsers = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                    .in(SysUser::getId, userIds)
                    .eq(SysUser::getDeleted, 0)
            );
        } else {
            validUsers = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                    .in(SysUser::getId, userIds)
                    .eq(SysUser::getTenantId, tenantId)
                    .eq(SysUser::getDeleted, 0)
            );
        }

        if (validUsers.isEmpty()) {
            throw new RuntimeException("未找到有效的用户");
        }

        List<Long> validUserIds = validUsers.stream()
            .map(SysUser::getId)
            .collect(Collectors.toList());

        if (validUserIds.size() < userIds.size()) {
            List<Long> invalidUserIds = new ArrayList<>(userIds);
            invalidUserIds.removeAll(validUserIds);
            log.warn("部分用户无效，将被忽略: invalidUserIds={}", invalidUserIds);
        }

        List<SysRoleUser> existingUsers = roleUserMapper.selectList(
            new LambdaQueryWrapper<SysRoleUser>()
                .eq(SysRoleUser::getRoleId, roleId)
                .in(SysRoleUser::getUserId, validUserIds)
        );

        List<Long> existingUserIds = existingUsers.stream()
            .map(SysRoleUser::getUserId)
            .collect(Collectors.toList());

        List<Long> newUserIds = validUserIds.stream()
            .filter(uid -> !existingUserIds.contains(uid))
            .collect(Collectors.toList());

        if (newUserIds.isEmpty()) {
            log.info("所有人员都已存在，无需添加: roleId={}", roleId);
            return;
        }

        List<SysRoleUser> list = newUserIds.stream()
            .map(uid -> {
                SysUser user = validUsers.stream()
                    .filter(u -> u.getId().equals(uid))
                    .findFirst().orElse(null);
                SysRoleUser ru = new SysRoleUser();
                ru.setRoleId(roleId);
                ru.setUserId(uid);
                ru.setTenantId(user != null ? user.getTenantId() : tenantId);
                ru.setCreateTime(LocalDateTime.now());
                return ru;
            })
            .collect(Collectors.toList());

        roleUserMapper.batchInsert(list);
        log.info("添加人员到角色成功: roleId={}, count={}", roleId, newUserIds.size());
    }

    @Override
    public void removeUserFromRole(Long roleId, Long userId) {
        LambdaQueryWrapper<SysRoleUser> wrapper = new LambdaQueryWrapper<SysRoleUser>()
            .eq(SysRoleUser::getRoleId, roleId)
            .eq(SysRoleUser::getUserId, userId);
        // 非管理员只能删除自己租户的角色分配
        if (!SecurityUtil.isPlatformAdmin()) {
            Long tenantId = SecurityUtil.getTenantIdOrNull();
            if (tenantId != null) {
                wrapper.eq(SysRoleUser::getTenantId, tenantId);
            }
        }
        roleUserMapper.delete(wrapper);
        log.info("从角色移除人员成功: roleId={}, userId={}", roleId, userId);
    }

    @Override
    public void addDeptsToRole(Long roleId, List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return;
        }

        Long tenantId = StpUtil.getTokenSession().get("tenantId", 2L);
        String loginType = StpUtil.getTokenSession().get("loginType", "user");

        log.info("添加部门到角色 - roleId={}, deptIds={}, loginType={}, tenantId={}",
            roleId, deptIds, loginType, tenantId);

        List<SysOrg> validDepts;
        if ("admin".equals(loginType)) {
            validDepts = orgMapper.selectList(
                new LambdaQueryWrapper<SysOrg>()
                    .in(SysOrg::getId, deptIds)
                    .eq(SysOrg::getDeleted, 0)
            );
        } else {
            validDepts = orgMapper.selectList(
                new LambdaQueryWrapper<SysOrg>()
                    .in(SysOrg::getId, deptIds)
                    .eq(SysOrg::getTenantId, tenantId)
                    .eq(SysOrg::getDeleted, 0)
            );
        }

        if (validDepts.isEmpty()) {
            throw new RuntimeException("未找到有效的部门");
        }

        Map<Long, Long> deptTenantMap = validDepts.stream()
            .collect(Collectors.toMap(SysOrg::getId, SysOrg::getTenantId));

        List<SysRoleDept> existingDepts = roleDeptMapper.selectList(
            new LambdaQueryWrapper<SysRoleDept>()
                .eq(SysRoleDept::getRoleId, roleId)
                .in(SysRoleDept::getDeptId, deptTenantMap.keySet())
        );

        List<Long> newDeptIds = deptTenantMap.keySet().stream()
            .filter(deptId -> existingDepts.stream()
                .noneMatch(existing -> existing.getDeptId().equals(deptId)
                    && existing.getTenantId().equals(deptTenantMap.get(deptId))))
            .collect(Collectors.toList());

        if (newDeptIds.isEmpty()) {
            log.info("所有部门都已存在，无需添加: roleId={}", roleId);
            return;
        }

        List<SysRoleDept> list = newDeptIds.stream()
            .map(deptId -> {
                SysOrg dept = validDepts.stream()
                    .filter(d -> d.getId().equals(deptId))
                    .findFirst().orElse(null);
                SysRoleDept rd = new SysRoleDept();
                rd.setRoleId(roleId);
                rd.setDeptId(deptId);
                rd.setTenantId(dept != null ? dept.getTenantId() : tenantId);
                rd.setCreateTime(LocalDateTime.now());
                return rd;
            })
            .collect(Collectors.toList());

        try {
            roleDeptMapper.batchInsert(list);
            log.info("添加部门到角色成功: roleId={}, count={}", roleId, newDeptIds.size());
        } catch (Exception e) {
            Throwable cause = e.getCause();
            String msg = e.getMessage();
            String causeMsg = cause != null ? cause.getMessage() : null;
            boolean isDuplicate = (msg != null && msg.contains("Duplicate entry"))
                || (causeMsg != null && causeMsg.contains("Duplicate entry"));
            if (isDuplicate) {
                log.warn("部分部门已存在，忽略重复添加: roleId={}", roleId);
            } else {
                throw new RuntimeException("添加部门失败: " + (causeMsg != null ? causeMsg : msg), e);
            }
        }
    }

    @Override
    public void removeDeptFromRole(Long roleId, Long deptId) {
        LambdaQueryWrapper<SysRoleDept> wrapper = new LambdaQueryWrapper<SysRoleDept>()
            .eq(SysRoleDept::getRoleId, roleId)
            .eq(SysRoleDept::getDeptId, deptId);
        // 非管理员只能删除自己租户的角色分配
        if (!SecurityUtil.isPlatformAdmin()) {
            Long tenantId = SecurityUtil.getTenantIdOrNull();
            if (tenantId != null) {
                wrapper.eq(SysRoleDept::getTenantId, tenantId);
            }
        }
        roleDeptMapper.delete(wrapper);
        log.info("从角色移除部门成功: roleId={}, deptId={}", roleId, deptId);
    }

    @Override
    public void addPermissionsToRole(Long roleId, List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return;
        }

        List<SysRolePermission> existing = rolePermissionMapper.selectList(
            new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId)
                .in(SysRolePermission::getPermissionId, permissionIds)
        );

        List<Long> existingIds = existing.stream()
            .map(SysRolePermission::getPermissionId)
            .collect(Collectors.toList());

        List<Long> newIds = permissionIds.stream()
            .filter(pid -> !existingIds.contains(pid))
            .collect(Collectors.toList());

        if (newIds.isEmpty()) {
            log.info("所有权限都已存在，无需添加: roleId={}", roleId);
            return;
        }

        Long tenantId = StpUtil.getSession().get("tenantId", 2L);

        List<SysRolePermission> list = newIds.stream()
            .map(pid -> {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(pid);
                rp.setTenantId(tenantId);
                rp.setCreateTime(LocalDateTime.now());
                return rp;
            })
            .collect(Collectors.toList());

        rolePermissionMapper.batchInsert(list);
        log.info("添加权限到角色成功: roleId={}, count={}", roleId, newIds.size());
    }

    @Override
    public void removePermissionFromRole(Long roleId, Long permissionId) {
        LambdaQueryWrapper<SysRolePermission> wrapper = new LambdaQueryWrapper<SysRolePermission>()
            .eq(SysRolePermission::getRoleId, roleId)
            .eq(SysRolePermission::getPermissionId, permissionId);
        // 非管理员只能删除自己租户的角色权限
        if (!SecurityUtil.isPlatformAdmin()) {
            Long tenantId = SecurityUtil.getTenantIdOrNull();
            if (tenantId != null) {
                wrapper.eq(SysRolePermission::getTenantId, tenantId);
            }
        }
        rolePermissionMapper.delete(wrapper);
        log.info("从角色移除权限成功: roleId={}, permissionId={}", roleId, permissionId);
    }
}
