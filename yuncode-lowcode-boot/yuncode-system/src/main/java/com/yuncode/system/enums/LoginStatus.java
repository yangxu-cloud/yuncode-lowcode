package com.yuncode.system.enums;

import lombok.Getter;

/**
 * 登录状态枚举
 */
@Getter
public enum LoginStatus {
    /**
     * 成功
     */
    SUCCESS(1, "成功"),

    /**
     * 失败
     */
    FAIL(0, "失败");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 状态描述
     */
    private final String desc;

    LoginStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
