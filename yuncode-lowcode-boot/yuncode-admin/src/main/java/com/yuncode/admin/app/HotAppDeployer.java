package com.yuncode.admin.app;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * App 热加载部署器。
 * <p>
 * 启动时加载 apps/lib/ 下已有的 JAR，启动后通过文件监听实时加载新放入的 JAR，
 * 将其中 @RestController / @Service / @Component 自动注册到正在运行的 Spring 容器中，
 * 无需重启平台。
 * </p>
 */
@Slf4j
@Component
public class HotAppDeployer implements ApplicationContextAware {

    private static final long POLL_INTERVAL_MS = 10000;

    /** 应用安装目录（各应用模块的父目录，包含 com.yuncode.user.apps.* 子目录） */
    @Value("${yuncode.app.install-dir:../apps/install}")
    private String appInstallDir;

    private DefaultListableBeanFactory beanFactory;
    private RequestMappingHandlerMapping handlerMapping;

    /** 已加载的 JAR → 类加载器及注册的 Bean 名称列表 */
    private final Map<String, LoadedApp> loadedApps = new ConcurrentHashMap<>();
    private volatile boolean watching = false;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        this.handlerMapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
    }

    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("HotAppDeployer initializing...");
        log.info("App install dir: {}", new File(appInstallDir).getAbsolutePath());
        log.info("========================================");
        loadAllExistingJars();
        startFileWatcher();
    }

    @PreDestroy
    public void destroy() {
        watching = false;
    }

    // ==================== 公开方法 ====================

    /**
     * 安装一个 JAR（动态注册其中的所有 Spring Bean）
     */
    public synchronized void installJar(File jarFile) {
        String jarName = jarFile.getName();

        if (loadedApps.containsKey(jarName)) {
            log.info("Replacing existing app: {}", jarName);
            uninstallJar(jarName);
        }

        log.info("Installing app JAR: {}", jarFile.getAbsolutePath());

        try {
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{jarFile.toURI().toURL()},
                    getClass().getClassLoader()
            );

            List<Class<?>> beanClasses = scanJarForBeans(jarFile, classLoader);
            if (beanClasses.isEmpty()) {
                log.warn("No Spring beans found in JAR: {}", jarName);
                classLoader.close();
                return;
            }

            List<String> beanNames = new ArrayList<>();
            for (Class<?> beanClass : beanClasses) {
                registerBean(beanClass, beanNames);
            }

            loadedApps.put(jarName, new LoadedApp(jarName, classLoader, beanNames, jarFile.lastModified()));
            log.info("App JAR installed successfully: {} ({} beans)", jarName, beanNames.size());

        } catch (Exception e) {
            log.error("Failed to install app JAR: " + jarName, e);
        }
    }

    /**
     * 卸载 JAR，移除其所有 Bean 及 Web 映射
     */
    public synchronized void uninstallJar(String jarName) {
        LoadedApp app = loadedApps.remove(jarName);
        if (app == null) return;

        log.info("Uninstalling app: {}", jarName);

        // 先清除 Handler 映射（避免更新 JAR 时旧映射与新版冲突）
        for (String beanName : app.beanNames) {
            removeHandlerMappings(beanName);
        }

        for (String beanName : app.beanNames) {
            if (beanFactory.containsBeanDefinition(beanName)) {
                // 先销毁 Bean 实例
                if (beanFactory.containsSingleton(beanName)) {
                    beanFactory.destroySingleton(beanName);
                }
                beanFactory.removeBeanDefinition(beanName);
                log.info("Removed bean: {}", beanName);
            }
        }

        try {
            app.classLoader.close();
        } catch (IOException e) {
            log.warn("Error closing classloader for " + jarName, e);
        }

        log.info("App uninstalled: {}", jarName);
    }

    // ==================== 内部方法 ====================

    private void loadAllExistingJars() {
        File installDir = new File(appInstallDir);
        if (!installDir.exists() || !installDir.isDirectory()) {
            log.info("App install dir not found: {}", installDir.getAbsolutePath());
            return;
        }

        File[] appDirs = installDir.listFiles(File::isDirectory);
        if (appDirs == null || appDirs.length == 0) return;

        for (File appDir : appDirs) {
            File appLibDir = new File(appDir, "lib");
            if (!appLibDir.exists() || !appLibDir.isDirectory()) continue;

            File[] jars = appLibDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jars == null || jars.length == 0) continue;

            log.info("Loading {} JAR(s) from {}/lib/", appDir.getName(), appInstallDir);
            for (File jar : jars) {
                try {
                    installJar(jar);
                } catch (Exception e) {
                    log.error("Failed to load JAR: " + jar.getName(), e);
                }
            }
        }
    }

    private void startFileWatcher() {
        watching = true;
        Thread watcher = new Thread(() -> {
            log.info("App file watcher started, polling {}/**/lib/ every {}ms", appInstallDir, POLL_INTERVAL_MS);
            while (watching) {
                try {
                    Thread.sleep(POLL_INTERVAL_MS);
                    scanForNewJars();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "app-file-watcher");
        watcher.setDaemon(true);
        watcher.start();
    }

    private void scanForNewJars() {
        File installDir = new File(appInstallDir);
        if (!installDir.exists()) return;

        File[] appDirs = installDir.listFiles(File::isDirectory);
        if (appDirs == null) return;

        for (File appDir : appDirs) {
            File appLibDir = new File(appDir, "lib");
            if (!appLibDir.exists() || !appLibDir.isDirectory()) continue;

            File[] jars = appLibDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jars == null) continue;

            for (File jar : jars) {
                LoadedApp existing = loadedApps.get(jar.getName());
                if (existing == null) {
                    log.info("Detected new app JAR: {}", jar.getName());
                    try { installJar(jar); } catch (Exception e) { log.error("Failed to install JAR: " + jar.getName(), e); }
                } else if (jar.lastModified() != existing.lastModified) {
                    log.info("Detected updated app JAR: {}", jar.getName());
                    try { installJar(jar); } catch (Exception e) { log.error("Failed to reload JAR: " + jar.getName(), e); }
                }
            }
        }
    }

    /**
     * 扫描 JAR 中的 @RestController / @Service / @Component 类
     */
    private List<Class<?>> scanJarForBeans(File jarFile, URLClassLoader classLoader) throws Exception {
        List<Class<?>> beanClasses = new ArrayList<>();

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.endsWith(".class")) continue;
                if (name.contains("module-info") || name.contains("package-info")) continue;

                String className = name.replace('/', '.').replace(".class", "");

                try {
                    // false = 不执行静态初始化块
                    Class<?> clazz = Class.forName(className, false, classLoader);
                    if (clazz.isAnnotation() || clazz.isInterface() || clazz.isEnum()) continue;

                    if (clazz.isAnnotationPresent(RestController.class) ||
                            clazz.isAnnotationPresent(Service.class) ||
                            clazz.isAnnotationPresent(org.springframework.stereotype.Component.class)) {
                        beanClasses.add(clazz);
                        log.debug("Found bean: {}", className);
                    }
                } catch (NoClassDefFoundError e) {
                    log.debug("Skipping {} (missing dependency: {})", className, e.getMessage());
                } catch (Exception e) {
                    log.debug("Skipping {}: {}", className, e.getMessage());
                }
            }
        }

        return beanClasses;
    }

    /**
     * 注册单个 Bean 到 Spring 容器，并触发实例化 + 注册 Web 映射
     */
    private void registerBean(Class<?> beanClass, List<String> beanNames) {
        try {
            String beanName = Character.toLowerCase(beanClass.getSimpleName().charAt(0))
                    + beanClass.getSimpleName().substring(1);

            if (beanFactory.containsBeanDefinition(beanName)) {
                log.warn("Bean already exists, skipping: {}", beanName);
                return;
            }

            // 注册 Bean 定义（启用构造器自动注入）
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(beanClass);
            builder.setScope("singleton");
            builder.getRawBeanDefinition().setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
            beanFactory.registerBeanDefinition(beanName, builder.getBeanDefinition());

            // 强制实例化 Bean
            beanFactory.getBean(beanName);

            beanNames.add(beanName);
            log.info("Bean registered & instantiated: {}", beanName);

            // 如果是 @RestController，单独注册其 Web 映射（不扫描已有 Bean，避免 Swagger 冲突）
            if (beanClass.isAnnotationPresent(RestController.class)) {
                registerControllerMappings(beanName);
            }

        } catch (Exception e) {
            log.error("Failed to register bean: " + beanClass.getName(), e);
        }
    }

    /**
     * 通过反射调用 detectHandlerMethods 只为单个 Controller 注册映射
     */
    private void registerControllerMappings(String beanName) {
        try {
            Method detectMethod = AbstractHandlerMethodMapping.class
                    .getDeclaredMethod("detectHandlerMethods", Object.class);
            detectMethod.setAccessible(true);
            detectMethod.invoke(this.handlerMapping, beanName);
            log.debug("Registered handler mappings for: {}", beanName);
        } catch (Exception e) {
            log.warn("Failed to register handler mappings for {}: {}", beanName, e.getMessage());
        }
    }

    /**
     * 从 HandlerMapping 中移除指定 Bean 已注册的 Web 映射
     */
    @SuppressWarnings("unchecked")
    private void removeHandlerMappings(String beanName) {
        try {
            Field registryField = AbstractHandlerMethodMapping.class
                    .getDeclaredField("mappingRegistry");
            registryField.setAccessible(true);
            Object mappingRegistry = registryField.get(this.handlerMapping);

            Class<?> registryClass = mappingRegistry.getClass();
            Field mappingLookupField = registryClass.getDeclaredField("mappingLookup");
            mappingLookupField.setAccessible(true);
            Map<Object, Object> mappingLookup = (Map<Object, Object>) mappingLookupField.get(mappingRegistry);

            Field urlLookupField = registryClass.getDeclaredField("urlLookup");
            urlLookupField.setAccessible(true);
            Map<String, List<Object>> urlLookup = (Map<String, List<Object>>) urlLookupField.get(mappingRegistry);

            Field registryMapField = registryClass.getDeclaredField("registry");
            registryMapField.setAccessible(true);
            Map<Object, Object> registryMap = (Map<Object, Object>) registryMapField.get(mappingRegistry);

            // 获取 Bean 实例用于精确比较（引用相等）
            Object beanInstance = beanFactory.getBean(beanName);

            // 查找此 Bean 的所有映射
            List<Object> toRemove = new ArrayList<>();
            for (Map.Entry<Object, Object> entry : mappingLookup.entrySet()) {
                Object handlerMethod = entry.getValue();
                if (handlerMethod != null) {
                    Method getBeanMethod = handlerMethod.getClass().getMethod("getBean");
                    Object bean = getBeanMethod.invoke(handlerMethod);
                    if (bean == beanInstance) {
                        toRemove.add(entry.getKey());
                    }
                }
            }

            // 移除映射
            for (Object mapping : toRemove) {
                mappingLookup.remove(mapping);
                urlLookup.values().forEach(list -> list.removeIf(m -> m.equals(mapping)));
                registryMap.remove(mapping);
            }

            if (!toRemove.isEmpty()) {
                log.debug("Removed {} handler mapping(s) for bean: {}", toRemove.size(), beanName);
            }
        } catch (Exception e) {
            log.warn("Failed to remove handler mappings for {}: {}", beanName, e.getMessage());
        }
    }

    private record LoadedApp(String jarName, URLClassLoader classLoader, List<String> beanNames, long lastModified) {}
}
