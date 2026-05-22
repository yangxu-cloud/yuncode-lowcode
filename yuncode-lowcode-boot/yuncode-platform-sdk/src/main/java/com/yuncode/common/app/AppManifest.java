package com.yuncode.common.app;

import java.time.LocalDateTime;

/**
 * App 发布清单元数据（对应 manifest.xml）。
 * <p>
 * 用于读取和传递应用的元信息，包括版本、开发者、依赖等。
 * </p>
 */
public class AppManifest {

    /**
     * 应用展示名称
     */
    private String name;

    /**
     * 应用版本号
     */
    private String version;

    /**
     * 构建编号
     */
    private Integer buildNo;

    /**
     * 开发者 ID
     */
    private String developer;

    /**
     * 是否支持热加载
     */
    private Boolean reloadable;

    /**
     * 是否允许启动
     */
    private Boolean allowStartup;

    /**
     * 依赖的平台版本
     */
    private String depends;

    /**
     * 安装时间
     */
    private LocalDateTime installDate;

    /**
     * 发布时间
     */
    private LocalDateTime releaseDate;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Integer getBuildNo() {
        return buildNo;
    }

    public void setBuildNo(Integer buildNo) {
        this.buildNo = buildNo;
    }

    public String getDeveloper() {
        return developer;
    }

    public void setDeveloper(String developer) {
        this.developer = developer;
    }

    public Boolean getReloadable() {
        return reloadable;
    }

    public void setReloadable(Boolean reloadable) {
        this.reloadable = reloadable;
    }

    public Boolean getAllowStartup() {
        return allowStartup;
    }

    public void setAllowStartup(Boolean allowStartup) {
        this.allowStartup = allowStartup;
    }

    public String getDepends() {
        return depends;
    }

    public void setDepends(String depends) {
        this.depends = depends;
    }

    public LocalDateTime getInstallDate() {
        return installDate;
    }

    public void setInstallDate(LocalDateTime installDate) {
        this.installDate = installDate;
    }

    public LocalDateTime getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDateTime releaseDate) {
        this.releaseDate = releaseDate;
    }
}
