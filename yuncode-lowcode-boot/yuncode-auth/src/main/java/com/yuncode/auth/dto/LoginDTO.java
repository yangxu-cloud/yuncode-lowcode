package com.yuncode.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录请求 DTO
 */
@Data
public class LoginDTO implements Serializable {

    /**
     * 登录类型：tenant-租户登录，admin-管理员登录，user-用户登录
     */
    @NotBlank(message = "登录类型不能为空")
    private String loginType;

    /**
     * 租户编码
     */
    private String tenantCode;

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在6-64位之间")
    private String password;
}
