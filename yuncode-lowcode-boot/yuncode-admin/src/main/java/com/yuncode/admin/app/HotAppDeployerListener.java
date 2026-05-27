package com.yuncode.admin.app;

import com.yuncode.system.event.AppLifecycleEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * HotAppDeployer 事件监听器
 * <p>
 * 监听 AppLifecycleEvent，调用 HotAppDeployer 执行实际的 JAR 加载/卸载。
 * 由于 ApplicationServiceImpl 在 yuncode-system 中无法直接引用 HotAppDeployer，
 * 通过 Spring 事件机制实现跨模块通信。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotAppDeployerListener {

    private final HotAppDeployer hotAppDeployer;

    @EventListener
    public void handleAppLifecycle(AppLifecycleEvent event) {
        String appDirName = event.getAppDirName();
        if (appDirName == null || appDirName.isEmpty()) {
            log.warn("AppLifecycleEvent: appDirName is empty, skip");
            return;
        }

        log.info("AppLifecycleEvent received: appId={}, appDirName={}, operation={}",
                event.getAppId(), appDirName, event.getOperation());

        switch (event.getOperation()) {
            case "start" -> handleStart(appDirName);
            case "stop" -> handleStop(appDirName);
            case "restart" -> handleRestart(appDirName);
            case "uninstall" -> handleStop(appDirName);
            default -> log.warn("Unknown operation: {}", event.getOperation());
        }
    }

    private void handleStart(String appDirName) {
        // 需要找到 app 目录 → resolve 路径
        File appDir = resolveAppDir(appDirName);
        if (appDir == null) {
            log.warn("App directory not found: {}", appDirName);
            return;
        }
        hotAppDeployer.installApp(appDir);
    }

    private void handleStop(String appDirName) {
        hotAppDeployer.uninstallApp(appDirName);
    }

    private void handleRestart(String appDirName) {
        handleStop(appDirName);
        handleStart(appDirName);
        log.info("App restarted: {}", appDirName);
    }

    /**
     * 根据 appId（目录名）解析完整路径
     */
    private File resolveAppDir(String appDirName) {
        // 尝试从安装目录加载
        File installDir = new File(hotAppDeployer.getAppInstallDir());
        File appDir = new File(installDir, appDirName);
        if (appDir.exists() && appDir.isDirectory()) {
            return appDir;
        }
        return null;
    }
}
