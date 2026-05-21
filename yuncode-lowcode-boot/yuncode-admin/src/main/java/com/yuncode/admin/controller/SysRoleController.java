package com.yuncode.admin.controller;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.yuncode.common.model.util.response.Result;
import com.yuncode.system.entity.SysRole;
import com.yuncode.system.service.RoleAssignmentService;
import com.yuncode.system.service.SysRoleService;
import com.yuncode.system.vo.RoleDetailVO;
import com.yuncode.system.vo.RoleNodeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/role")
public class SysRoleController {

    private final SysRoleService roleService;
    private final RoleAssignmentService roleAssignmentService;

    /**
     * 获取角色树
     * 平台管理员需要查看所有租户的角色，忽略租户拦截器
     */
    @GetMapping("/tree")
    public Result<List<RoleNodeVO>> getRoleTree() {
        // 打印当前用户信息
        try {
            String loginType = cn.dev33.satoken.stp.StpUtil.getSession().get("loginType", "");
            Long tenantId = cn.dev33.satoken.stp.StpUtil.getSession().get("tenantId", null);
            log.info("获取角色树 - 当前用户信息: loginType={}, tenantId={}", loginType, tenantId);
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
        }

        List<RoleNodeVO> tree = roleService.getRoleTree();
        log.info("角色树节点数量: {}", tree.size());
        return Result.success(tree);
    }

    /**
     * 获取角色详情
     */
    @GetMapping("/{id}")
    public Result<RoleDetailVO> getRoleDetail(@PathVariable Long id) {
        RoleDetailVO detail = roleService.getRoleDetail(id);
        return Result.success(detail);
    }

    /**
     * 新增角色
     */
    @PostMapping
    public Result<Long> createRole(@RequestBody SysRole role) {
        roleService.createRole(role);
        return Result.success(role.getId());
    }

    /**
     * 编辑角色
     */
    @PutMapping("/{id}")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody SysRole role) {
        roleService.updateRole(id, role);
        return Result.success();
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }

    /**
     * 添加人员到角色
     */
    @PostMapping("/{roleId}/users")
    public Result<Void> addUsers(@PathVariable Long roleId, @RequestBody List<Long> userIds) {
        roleAssignmentService.addUsersToRole(roleId, userIds);
        return Result.success();
    }

    /**
     * 从角色移除人员
     */
    @DeleteMapping("/{roleId}/users/{userId}")
    public Result<Void> removeUser(@PathVariable Long roleId, @PathVariable Long userId) {
        roleAssignmentService.removeUserFromRole(roleId, userId);
        return Result.success();
    }

    /**
     * 添加部门到角色
     */
    @PostMapping("/{roleId}/depts")
    public Result<Void> addDepts(@PathVariable Long roleId, @RequestBody List<Long> deptIds) {
        roleAssignmentService.addDeptsToRole(roleId, deptIds);
        return Result.success();
    }

    /**
     * 从角色移除部门
     */
    @DeleteMapping("/{roleId}/depts/{deptId}")
    public Result<Void> removeDept(@PathVariable Long roleId, @PathVariable Long deptId) {
        roleAssignmentService.removeDeptFromRole(roleId, deptId);
        return Result.success();
    }

    /**
     * 添加权限到角色
     */
    @PostMapping("/{roleId}/permissions")
    public Result<Void> addPermissions(@PathVariable Long roleId, @RequestBody List<Long> permissionIds) {
        roleAssignmentService.addPermissionsToRole(roleId, permissionIds);
        return Result.success();
    }

    /**
     * 从角色移除权限
     */
    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    public Result<Void> removePermission(@PathVariable Long roleId, @PathVariable Long permissionId) {
        roleAssignmentService.removePermissionFromRole(roleId, permissionId);
        return Result.success();
    }
}
