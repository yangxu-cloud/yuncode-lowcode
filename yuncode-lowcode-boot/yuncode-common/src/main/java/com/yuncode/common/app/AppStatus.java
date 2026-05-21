package com.yuncode.common.app;

/**
 * App 运行时状态枚举。
 */
public enum AppStatus {

    INSTALLED(0, "已安装"),
    RUNNING(1, "运行中"),
    STOPPED(2, "已停止"),
    ERROR(3, "异常");

    private final int code;
    private final String description;

    AppStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取状态描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据状态码获取枚举
     */
    public static AppStatus fromCode(int code) {
        for (AppStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return INSTALLED;
    }
}
