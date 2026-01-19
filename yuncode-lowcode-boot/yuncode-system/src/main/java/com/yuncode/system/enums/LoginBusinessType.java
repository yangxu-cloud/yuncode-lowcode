package com.yuncode.system.enums;

import lombok.Getter;

/**
 * 登录业务类型枚举
 */
@Getter
public enum LoginBusinessType {
    /**
     * 其它
     */
    OTHER(0, "其它"),

    /**
     * 新增
     */
    INSERT(1, "新增"),

    /**
     * 修改
     */
    UPDATE(2, "修改"),

    /**
     * 删除
     */
    DELETE(3, "删除"),

    /**
     * 授权
     */
    GRANT(4, "授权"),

    /**
     * 导出
     */
    EXPORT(5, "导出"),

    /**
     * 导入
     */
    IMPORT(6, "导入"),

    /**
     * 强退
     */
    KICKOUT(7, "强退");

    /**
     * 业务码
     */
    private final Integer code;

    /**
     * 业务描述
     */
    private final String desc;

    LoginBusinessType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
