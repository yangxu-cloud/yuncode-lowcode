package com.yuncode.admin.app;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 基于 {@link WatchService} 的 JAR 文件监听器。
 * <p>
 * 监听每个 App 的 lib/ 目录下的 *.jar 文件增删改，以及安装目录下新增 App 子目录。
 * 变更事件按 appId 去抖（500ms 窗口），避免 Windows 重复事件触发多次重载。
 * </p>
 */
@Slf4j
public class JarFileWatcher implements AppWatcher {

    private static final long DEBOUNCE_MS = 500;

    private WatchService watchService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "app-watcher-debounce");
        t.setDaemon(true);
        return t;
    });

    /** WatchKey → 对应的 appId（lib 目录的 key）或 "_install"（安装目录的 key） */
    private final Map<WatchKey, String> keyMap = new ConcurrentHashMap<>();

    private final Map<String, ScheduledFuture<?>> pending = new ConcurrentHashMap<>();
    private volatile boolean running;
    private Consumer<String> onAppChanged;

    @Override
    public void start(Path installDir, Consumer<String> onAppChanged) {
        this.onAppChanged = onAppChanged;
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            log.error("Failed to create WatchService", e);
            return;
        }

        // 注册现有 App 的 lib/ 目录
        File[] appDirs = installDir.toFile().listFiles(File::isDirectory);
        if (appDirs != null) {
            for (File appDir : appDirs) {
                registerLibDir(appDir.toPath());
            }
        }

        // 注册安装目录本身，监听新增/删除子目录
        try {
            WatchKey key = installDir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE);
            keyMap.put(key, "_install");
        } catch (IOException e) {
            log.warn("Failed to watch install dir: {}", installDir, e);
        }

        running = true;
        Thread t = new Thread(this::eventLoop, "app-file-watcher");
        t.setDaemon(true);
        t.start();
        log.info("JarFileWatcher started, watching: {}", installDir);
    }

    private void registerLibDir(Path appDir) {
        Path libDir = appDir.resolve("lib");
        if (!Files.exists(libDir) || !Files.isDirectory(libDir)) return;
        try {
            WatchKey key = libDir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            String appId = appDir.getFileName().toString();
            keyMap.put(key, appId);
            log.debug("Watching lib dir: {}", libDir);
        } catch (IOException e) {
            log.warn("Failed to watch lib dir: {}", libDir, e);
        }
    }

    private void eventLoop() {
        while (running) {
            WatchKey key;
            try {
                key = watchService.poll(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (key == null) continue;

            Set<String> changedAppIds = new HashSet<>();
            String knownAppId = keyMap.get(key);

            for (WatchEvent<?> event : key.pollEvents()) {
                Object ctx = event.context();
                if (!(ctx instanceof Path filename)) continue;

                if ("_install".equals(knownAppId)) {
                    // 安装目录下新增或删除了子目录
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        Path newAppDir = ((Path) key.watchable()).resolve(filename);
                        if (Files.isDirectory(newAppDir)) {
                            registerLibDir(newAppDir);
                            String appId = newAppDir.getFileName().toString();
                            changedAppIds.add(appId);
                            log.info("[app-watcher] 发现新 App 目录: {}", appId);
                        }
                    }
                } else if (knownAppId != null) {
                    // lib/ 目录下 JAR 文件变更
                    if (filename.toString().endsWith(".jar")) {
                        changedAppIds.add(knownAppId);
                        log.debug("[app-watcher] JAR 变动: {} / {}", knownAppId, filename);
                    }
                }
            }

            key.reset();

            // 去抖通知
            for (String appId : changedAppIds) {
                debouncedNotify(appId);
            }
        }
    }

    /** 500ms 去抖：连续变更只通知一次 */
    private void debouncedNotify(String appId) {
        ScheduledFuture<?> existing = pending.remove(appId);
        if (existing != null) {
            existing.cancel(false);
        }
        pending.put(appId, scheduler.schedule(() -> {
            pending.remove(appId);
            log.info("[app-watcher] App 已变更: {}", appId);
            if (onAppChanged != null) {
                onAppChanged.accept(appId);
            }
        }, DEBOUNCE_MS, TimeUnit.MILLISECONDS));
    }

    @Override
    public void close() {
        running = false;
        scheduler.shutdownNow();
        try {
            if (watchService != null) watchService.close();
        } catch (IOException e) {
            log.debug("Error closing WatchService: {}", e.getMessage());
        }
        log.info("JarFileWatcher stopped");
    }
}
