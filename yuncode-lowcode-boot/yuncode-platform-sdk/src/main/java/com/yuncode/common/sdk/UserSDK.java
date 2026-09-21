package com.yuncode.common.sdk;

import java.util.List;

/**
 * User SDK — 当前用户相关操作
 */
public interface UserSDK {

    /** 获取当前用户 ID */
    Long currentUserId();

    /** 获取当前用户名 */
    String currentUsername();

    /** 获取当前用户昵称 */
    String currentNickname();

    /** 获取当前用户角色列表 */
    List<String> currentRoles();

    /** 是否具有指定权限 */
    boolean hasPermission(String permission);

    /** 是否具有指定角色 */
    boolean hasRole(String role);
}
