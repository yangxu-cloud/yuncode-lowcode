package com.yuncode.admin.app;

import com.yuncode.common.app.YuncodeApp;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * App JAR 加载器（Prod 模式）。
 * <p>
 * 在 Spring 启动前将 apps/lib/ 目录下的所有应用 JAR 动态添加到系统类加载器中，
 * 使得 Spring 的组件扫描能够发现并注册其中的 Bean。
 * </p>
 */
@Slf4j
public class AppJarLoader {

    private static final String LIB_DIR = "apps/lib";
    private static final List<YuncodeApp> loadedApps = new ArrayList<>();

    /**
     * 加载 apps/lib/ 下所有应用 JAR，需要在 Spring 启动前调用
     */
    public static void loadJars() {
        File libDir = new File(LIB_DIR);
        if (!libDir.exists() || !libDir.isDirectory()) {
            log.info("App lib directory not found: {}, skipping JAR loading", libDir.getAbsolutePath());
            return;
        }

        File[] jars = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            log.info("No app JARs found in {}", libDir.getAbsolutePath());
            return;
        }

        log.info("Found {} app JAR(s) in {}, loading...", jars.length, libDir.getAbsolutePath());

        ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();
        if (!(sysClassLoader instanceof URLClassLoader)) {
            log.warn("System ClassLoader is not URLClassLoader (Java 17+ may need --add-opens), JARs will not be loaded dynamically");
            return;
        }

        try {
            Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addURL.setAccessible(true);

            for (File jar : jars) {
                URL jarUrl = jar.toURI().toURL();
                addURL.invoke(sysClassLoader, jarUrl);
                log.info("Loaded app JAR: {} ({})", jar.getName(), jarUrl);
            }

            // 通过 SPI 发现 YuncodeApp 实现
            discoverApps();

        } catch (Exception e) {
            log.error("Failed to load app JARs", e);
        }
    }

    /**
     * 获取已加载的 YuncodeApp 实例列表
     */
    public static List<YuncodeApp> getLoadedApps() {
        return loadedApps;
    }

    private static void discoverApps() {
        ServiceLoader<YuncodeApp> serviceLoader = ServiceLoader.load(YuncodeApp.class);
        for (YuncodeApp app : serviceLoader) {
            loadedApps.add(app);
            log.info("Discovered app via SPI: {} v{} ({})",
                    app.getAppName(), app.getVersion(), app.getAppId());
        }
    }
}
