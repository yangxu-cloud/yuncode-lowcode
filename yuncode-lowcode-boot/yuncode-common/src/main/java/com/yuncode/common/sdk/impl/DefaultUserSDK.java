package com.yuncode.common.sdk.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.yuncode.common.sdk.UserSDK;
import com.yuncode.common.utils.SecurityUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * User SDK 默认实现 — 桥接到 Sa-Token + SecurityUtil
 */
public class DefaultUserSDK implements UserSDK {

    @Override public Long currentUserId() { return SecurityUtil.getUserIdOrNull(); }
    @Override public String currentUsername() { return SecurityUtil.getUsername(); }
    @Override public String currentNickname() { return SecurityUtil.getNickname(); }

    @Override public List<String> currentRoles() {
        try {
            return StpUtil.getRoleList();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override public boolean hasPermission(String permission) {
        try {
            return StpUtil.hasPermission(permission);
        } catch (Exception e) {
            return false;
        }
    }

    @Override public boolean hasRole(String role) {
        try {
            return StpUtil.hasRole(role);
        } catch (Exception e) {
            return false;
        }
    }
}
