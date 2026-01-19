package com.yuncode.auth.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录响应 VO
 */
@Data
public class LoginVO implements Serializable {

    /**
     * Token
     */
    private String token;

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
}
