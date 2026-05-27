package com.yuncode.auth.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录响应 VO
 */
@Data
public class LoginVO implements Serializable {

    /**
     * Token（Sa-Token JWT，用于 API 认证）
     */
    private String token;

    /**
     * 会话ID（业务会话标识，用于前端 Cookie 和 SSE 连接）
     */
    private String sessionId;

    /**
     * Token 名称
     */
    private String tokenName;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 权限标识列表
     */
    private java.util.List<String> permissions;

    /**
     * 是否需要修改密码（首次登录使用默认密码时为 true）
     */
    private Boolean requireChange;
}
