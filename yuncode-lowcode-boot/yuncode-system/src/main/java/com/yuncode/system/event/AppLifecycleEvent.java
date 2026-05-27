package com.yuncode.system.event;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 应用生命周期事件
 * <p>
 * ApplicationServiceImpl 在启动/停止/重启/卸载时发布此事件，
 * HotAppDeployerListener 监听后调用 HotAppDeployer 执行实际的 JAR 加载/卸载。
 * </p>
 */
@Data
@AllArgsConstructor
public class AppLifecycleEvent {

    /** 应用 ID（数据库主键） */
    private Long appId;

    /** 应用标识（目录名） */
    private String appDirName;

    /** 操作类型：start / stop / restart / uninstall */
    private String operation;
}
