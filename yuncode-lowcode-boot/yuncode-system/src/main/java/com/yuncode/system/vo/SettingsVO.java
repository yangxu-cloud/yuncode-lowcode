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
     * 应用名称
     */
    private String appName;

    /**
     * 应用版本
     */
    private String appVersion;

    /**
     * 应用Logo
     */
    private String appLogo;

    /**
     * 应用描述
     */
    private String appDescription;

    /**
     * 版权信息
     */
    private String copyright;

    /**
     * 备案号
     */
    private String icp;

    /**
     * 其他设置（JSON格式）
     */
    private Map<String, Object> extra;
}
