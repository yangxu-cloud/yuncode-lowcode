package com.yuncode.system.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 系统信息 VO
 */
@Data
public class SystemInfoVO implements Serializable {

    /**
     * 系统名称
     */
    private String name;

    /**
     * 系统版本
     */
    private String version;

    /**
     * 运行环境
     */
    private String env;

    /**
     * 框架
     */
    private String framework;

    /**
     * Java版本
     */
    private String javaVersion;

    /**
     * 启动时间
     */
    private String startTime;

    /**
     * 运行时长
     */
    private String uptime;

    /**
     * 服务器IP
     */
    private String serverIp;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 系统架构
     */
    private String arch;
}
