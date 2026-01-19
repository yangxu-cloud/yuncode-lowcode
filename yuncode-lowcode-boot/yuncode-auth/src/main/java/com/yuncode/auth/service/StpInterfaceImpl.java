package com.yuncode.auth.service;

import cn.dev33.satoken.stp.StpInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 权限验证接口实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    /**
     * 返回指定账号 id 所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        List<String> permissions = new ArrayList<>();

        // TODO: 从数据库查询用户的权限列表
        // 1. 根据 loginId 查询用户
        // 2. 查询用户的角色
        // 3. 查询角色对应的权限（菜单）
        // 4. 返回权限标识列表

        log.debug("获取用户权限列表: userId={}", loginId);

        // 暂时返回空列表，后续实现数据库查询
        return permissions;
    }

    /**
     * 返回指定账号 id 所拥有的角色标识集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<String> roles = new ArrayList<>();

        // TODO: 从数据库查询用户的角色列表
        // 1. 根据 loginId 查询用户
        // 2. 查询用户的角色关联
        // 3. 返回角色编码列表

        log.debug("获取用户角色列表: userId={}", loginId);

        // 暂时返回空列表，后续实现数据库查询
        return roles;
    }
}
