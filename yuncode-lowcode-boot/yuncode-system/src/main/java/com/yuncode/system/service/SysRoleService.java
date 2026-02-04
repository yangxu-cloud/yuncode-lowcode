package com.yuncode.system.service;

import com.yuncode.system.entity.SysRole;
import com.yuncode.system.vo.RoleDetailVO;
import com.yuncode.system.vo.RoleNodeVO;

import java.util.List;

/**
 * 角色服务接口
 */
public interface SysRoleService {

    /**
     * 获取角色树
     *
     * @return 角色树
     */
    List<RoleNodeVO> getRoleTree();

    /**
     * 获取角色详情
     *
     * @param id 角色ID
     * @return 角色详情
     */
    RoleDetailVO getRoleDetail(Long id);

    /**
     * 新增角色
     *
     * @param role 角色信息
     */
    void createRole(SysRole role);

    /**
     * 编辑角色
     *
     * @param id   角色ID
     * @param role 角色信息
     */
    void updateRole(Long id, SysRole role);

    /**
     * 删除角色
     *
     * @param id 角色ID
     */
    void deleteRole(Long id);

    /**
     * 添加人员到角色
     *
     * @param roleId  角色ID
     * @param userIds 用户ID列表
     */
    void addUsersToRole(Long roleId, List<Long> userIds);

    /**
     * 从角色移除人员
     *
     * @param roleId 角色ID
     * @param userId 用户ID
     */
    void removeUserFromRole(Long roleId, Long userId);

    /**
     * 添加部门到角色
     *
     * @param roleId  角色ID
     * @param deptIds 部门ID列表
     */
    void addDeptsToRole(Long roleId, List<Long> deptIds);

    /**
     * 从角色移除部门
     *
     * @param roleId 角色ID
     * @param deptId 部门ID
     */
    void removeDeptFromRole(Long roleId, Long deptId);

    /**
     * 添加权限到角色
     *
     * @param roleId        角色ID
     * @param permissionIds 权限ID列表
     */
    void addPermissionsToRole(Long roleId, List<Long> permissionIds);

    /**
     * 从角色移除权限
     *
     * @param roleId       角色ID
     * @param permissionId 权限ID
     */
    void removePermissionFromRole(Long roleId, Long permissionId);
}
