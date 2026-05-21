package com.yuncode.system.service;

import java.util.List;

/**
 * 角色分配服务接口
 * 处理人员、部门、权限与角色的关联关系
 */
public interface RoleAssignmentService {

    void addUsersToRole(Long roleId, List<Long> userIds);

    void removeUserFromRole(Long roleId, Long userId);

    void addDeptsToRole(Long roleId, List<Long> deptIds);

    void removeDeptFromRole(Long roleId, Long deptId);

    void addPermissionsToRole(Long roleId, List<Long> permissionIds);

    void removePermissionFromRole(Long roleId, Long permissionId);
}
