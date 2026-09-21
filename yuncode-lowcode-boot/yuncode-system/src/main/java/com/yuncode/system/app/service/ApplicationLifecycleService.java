package com.yuncode.system.app.service;

/**
 * 应用生命周期管理服务
 */
public interface ApplicationLifecycleService {
    boolean startApplication(Long id);
    boolean stopApplication(Long id);
    boolean restartApplication(Long id);
    boolean restoreApplication(Long id);
    boolean installApplication(Long id);
    boolean uninstallApplication(Long id);
    boolean upgradeApplication(Long id);
}
