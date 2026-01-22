package com.yuncode.system.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 系统设置 VO
 */
@Data
public class SettingsVO implements Serializable {

    /**
     * 系统名称
     */
    private String systemName;

    /**
     * 应用版本
     */
    private String appVersion;

    /**
     * 系统 Logo
     */
    private String systemLogo;

    /**
     * 系统描述
     */
    private String systemDescription;

    /**
     * 版权信息
     */
    private String copyright;

    /**
     * 备案号
     */
    private String icp;

    /**
     * 系统URL
     */
    private String systemUrl;

    /**
     * 语言
     */
    private String language;

    /**
     * 时区
     */
    private String timezone;

    /**
     * 日期格式
     */
    private String dateFormat;

    /**
     * 时间格式
     */
    private String timeFormat;

    /**
     * 密码最小长度
     */
    private Integer passwordMinLength;

    /**
     * 密码必须大写
     */
    private Boolean passwordRequireUppercase;

    /**
     * 密码必须小写
     */
    private Boolean passwordRequireLowercase;

    /**
     * 密码必须数字
     */
    private Boolean passwordRequireNumber;

    /**
     * 密码必须特殊字符
     */
    private Boolean passwordRequireSpecial;

    /**
     * 密码有效期（天）
     */
    private Integer passwordExpireDays;

    /**
     * 登录最大尝试次数
     */
    private Integer loginMaxAttempts;

    /**
     * 登录锁定时长（分钟）
     */
    private Integer loginLockDuration;

    /**
     * 会话超时（分钟）
     */
    private Integer loginSessionTimeout;

    /**
     * 是否启用验证码
     */
    private Boolean loginEnableCaptcha;

    /**
     * 其他设置（JSON格式）
     */
    private Map<String, Object> extra;
}
