package com.yuncode.auth.service;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuncode.system.entity.SysMenu;
import com.yuncode.system.entity.SysMenuPermission;
import com.yuncode.system.entity.SysRole;
import com.yuncode.system.entity.SysRoleUser;
import com.yuncode.system.mapper.SysMenuMapper;
import com.yuncode.system.mapper.SysMenuPermissionMapper;
import com.yuncode.system.mapper.SysRoleMapper;
import com.yuncode.system.mapper.SysRoleUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Sa-Token 权限验证接口实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final SysRoleUserMapper sysRoleUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuPermissionMapper sysMenuPermissionMapper;
    private final SysMenuMapper sysMenuMapper;

    /**
     * 返回指定账号 id 所拥有的权限码集合
     * 链路：用户 → 角色 → 菜单权限标识
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = toLong(loginId);
        if (userId == null) {
            return Collections.emptyList();
        }

        // 1. 查用户关联的角色ID
        List<Long> roleIds = getUserRoleIds(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 查角色绑定的菜单ID（target_type=0 表示按角色关联）
        List<SysMenuPermission> menuPerms = sysMenuPermissionMapper.selectList(
                Wrappers.lambdaQuery(SysMenuPermission.class)
                        .eq(SysMenuPermission::getTargetType, 0)
                        .in(SysMenuPermission::getTargetId, roleIds)
        );
        if (menuPerms.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> menuIds = menuPerms.stream()
                .map(SysMenuPermission::getMenuId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 3. 查菜单的权限标识
        List<SysMenu> menus = sysMenuMapper.selectList(
                Wrappers.lambdaQuery(SysMenu.class).in(SysMenu::getId, menuIds)
        );

        List<String> permissions = menus.stream()
                .map(SysMenu::getPermission)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.debug("获取用户权限列表: userId={}, permissions={}", userId, permissions);
        return permissions;
    }

    /**
     * 返回指定账号 id 所拥有的角色标识集合
     * 链路：用户 → 角色编码
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = toLong(loginId);
        if (userId == null) {
            return Collections.emptyList();
        }

        List<Long> roleIds = getUserRoleIds(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<SysRole> roles = sysRoleMapper.selectList(
                Wrappers.lambdaQuery(SysRole.class).in(SysRole::getId, roleIds)
        );

        List<String> roleCodes = roles.stream()
                .map(SysRole::getRoleCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.debug("获取用户角色列表: userId={}, roles={}", userId, roleCodes);
        return roleCodes;
    }

    /**
     * 查询用户关联的所有角色ID
     */
    private List<Long> getUserRoleIds(Long userId) {
        List<SysRoleUser> roleUsers = sysRoleUserMapper.selectList(
                Wrappers.lambdaQuery(SysRoleUser.class).eq(SysRoleUser::getUserId, userId)
        );
        if (roleUsers.isEmpty()) {
            return Collections.emptyList();
        }
        return roleUsers.stream()
                .map(SysRoleUser::getRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 将 loginId 转为 Long，支持 String 和 Number 类型
     */
    private Long toLong(Object loginId) {
        if (loginId == null) {
            return null;
        }
        if (loginId instanceof Number) {
            return ((Number) loginId).longValue();
        }
        try {
            return Long.valueOf(loginId.toString());
        } catch (NumberFormatException e) {
            log.warn("无法解析 loginId: {}", loginId);
            return null;
        }
    }
}
