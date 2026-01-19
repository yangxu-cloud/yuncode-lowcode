package com.yuncode.system.enums;

import lombok.Getter;

/**
 * 登录类型枚举
 */
@Getter
public enum LoginType {

    /**
     * 租户登录
     * 租户管理员登录，需要租户编码
     */
    TENANT("tenant", "租户登录"),

    /**
     * 管理员登录
     * 平台超级管理员，不需要租户编码
     */
    ADMIN("admin", "管理员登录"),

    /**
     * 普通用户登录
     * 普通用户登录，需要租户编码
     */
    USER("user", "用户登录");

    /**
     * 类型编码
     */
    private final String code;

    /**
     * 类型描述
     */
    private final String desc;

    LoginType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码获取枚举
     */
    public static LoginType fromCode(String code) {
        for (LoginType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的登录类型: " + code);
    }

    /**
     * 获取类型编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取类型描述
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 判断是否需要租户编码
     */
    public boolean requiresTenantCode() {
        return this == TENANT || this == USER;
    }
}
