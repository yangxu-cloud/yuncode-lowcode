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
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * App 热加载部署器。
 * <p>
 * 以 App 目录为身份标识，管理 {appId}/lib/*.jar 中所有 JAR 的加载/卸载。
 * 启动时扫描安装目录下已有 App，启动后通过文件监听实时检测变更并热替换。
 * </p>
 */
@Slf4j
@Component
public class HotAppDeployer implements ApplicationContextAware {

    /** 应用安装目录（包含 com.yuncode.user.apps.* 子目录） */
    @Value("${yuncode.apps.install-dir:../apps/install}")
    private String appInstallDir;

    private DefaultListableBeanFactory beanFactory;
    private RequestMappingHandlerMapping handlerMapping;
    private AppWatcher appWatcher;

    /** 已加载的 App：key = 目录名（appId），value = 加载信息 */
    private final Map<String, LoadedApp> loadedApps = new ConcurrentHashMap<>();

    /** App 状态枚举 */
    public enum AppStatus {
        LOADING,
        LOADED,
        FAILED,
        UNLOADING
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        this.handlerMapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
    }

    @PostConstruct
    public void init() {
        this.appInstallDir = resolveInstallDir();
        log.info("========================================");
        log.info("HotAppDeployer initializing...");
        log.info("App install dir: {}", new File(appInstallDir).getAbsolutePath());
        log.info("HandlerMapping class: {}", this.handlerMapping.getClass().getName());
        log.info("========================================");
        loadAllExistingApps();
        this.appWatcher = new JarFileWatcher();
        appWatcher.start(Path.of(appInstallDir), this::onAppChanged);
    }

    /**
     * 解析安装目录，不依赖 user.dir。
     * 按优先级尝试多个路径，选择第一个包含 App 子目录的。
     */
    private String resolveInstallDir() {
        List<String> candidates = new ArrayList<>();

        // 候选1：从类路径反推 yuncode-lowcode-boot/apps/install/
        try {
            URL classUrl = getClass().getProtectionDomain().getCodeSource().getLocation();
            if (classUrl != null) {
                Path classRoot = Path.of(classUrl.toURI());
                // classRoot = .../yuncode-admin/target/classes/ 或 out/production/classes/
                Path p = classRoot.resolve("../../apps/install").normalize();
                if (Files.exists(p) && Files.isDirectory(p)) {
                    candidates.add(p.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Classpath install-dir resolution failed: {}", e.getMessage());
        }

        // 候选2：./apps/install（user.dir = yuncode-lowcode-boot 时正确）
        candidates.add(new File("./apps/install").getAbsolutePath());

        // 候选3：../apps/install（user.dir = yuncode-admin 时正确）
        candidates.add(new File("../apps/install").getAbsolutePath());

        // 候选4：配置值
        candidates.add(new File(appInstallDir).getAbsolutePath());

        // 选第一个包含 App 子目录的
        for (String path : candidates) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory() && hasAppSubdirs(dir)) {
                log.info("install-dir = {}", dir.getAbsolutePath());
                return dir.getAbsolutePath();
            }
        }

        // 都没找到，用配置值（让后续流程打印错误日志）
        File fallback = new File(appInstallDir);
        log.warn("install-dir: no valid app dir found, using: {}", fallback.getAbsolutePath());
        return appInstallDir;
    }

    /** 目录下是否存在 App 子目录（有 lib/ 目录即可，不要求必须有 JAR） */
    private boolean hasAppSubdirs(File installDir) {
        File[] subdirs = installDir.listFiles(File::isDirectory);
        if (subdirs == null) return false;
        for (File sub : subdirs) {
            File libDir = new File(sub, "lib");
            if (libDir.exists() && libDir.isDirectory()) return true;
        }
        return false;
    }

    @PreDestroy
    public void destroy() {
        if (appWatcher != null) {
            appWatcher.close();
        }
    }

    // ==================== 公开方法 ====================

    /**
     * 获取当前解析后的应用安装目录路径
     */
    public String getAppInstallDir() {
        return this.appInstallDir;
    }

    /**
     * 安装一个 App 目录（加载其 lib/ 下所有 JAR，注册所有 Spring Bean）。
     * 如果没有 JAR，仍会注册为已加载的低代码应用（无 Spring Bean）。
     */
    public synchronized void installApp(File appDir) {
        String appId = appDir.getName();

        if (loadedApps.containsKey(appId)) {
            log.info("App already loaded, replacing: {}", appId);
            uninstallApp(appId);
        }

        LoadedApp app = new LoadedApp(appId, new ArrayList<>(), new ArrayList<>(), System.currentTimeMillis());
        app.status = AppStatus.LOADING;
        loadedApps.put(appId, app);

        File libDir = new File(appDir, "lib");
        File[] jars = libDir.listFiles((dir, name) -> name.endsWith(".jar"));

        if (jars == null || jars.length == 0) {
            log.info("App installed (no JARs): {} - 低代码应用", appId);
            app.status = AppStatus.LOADED;
            return;
        }

        log.info("Installing app: {} ({} JARs)", appId, jars.length);

        for (File jar : jars) {
            try {
                URLClassLoader cl = new ChildFirstURLClassLoader(
                        new URL[]{jar.toURI().toURL()},
                        getClass().getClassLoader()
                );
                List<Class<?>> beanClasses = scanJarForBeans(jar, cl);
                for (Class<?> beanClass : beanClasses) {
                    try {
                        registerBean(beanClass, app.beanNames);
                    } catch (Exception e) {
                        log.error("Failed to register bean {} in JAR {}: {}", beanClass.getName(), jar.getName(), e.getMessage(), e);
                        // 回滚已注册的 bean
                        rollbackBeans(app);
                        app.status = AppStatus.FAILED;
                        return;
                    }
                }
                app.classLoaders.add(cl);
                log.debug("Loaded JAR: {} ({} beans)", jar.getName(), beanClasses.size());
            } catch (Exception e) {
                log.error("Failed to load JAR {} in app {}: {}", jar.getName(), appId, e.getMessage(), e);
                // 回滚
                rollbackBeans(app);
                app.status = AppStatus.FAILED;
                return;
            }
        }

        app.status = AppStatus.LOADED;
        log.info("App installed successfully: {} ({} beans)", appId, app.beanNames.size());
        refreshOpenApiCache();
    }

    /**
     * 回滚已注册的 bean
     */
    private void rollbackBeans(LoadedApp app) {
        if (app == null || app.beanNames == null) return;
        log.info("Rolling back {} beans for app: {}", app.beanNames.size(), app.appId);
        for (String beanName : app.beanNames) {
            removeHandlerMappings(beanName);
            if (beanFactory.containsBeanDefinition(beanName)) {
                if (beanFactory.containsSingleton(beanName)) {
                    beanFactory.destroySingleton(beanName);
                }
                beanFactory.removeBeanDefinition(beanName);
                log.debug("Rolled back bean: {}", beanName);
            }
        }
        app.beanNames.clear();
        for (URLClassLoader cl : app.classLoaders) {
            try { cl.close(); } catch (IOException ignored) {}
        }
        app.classLoaders.clear();
    }

    /**
     * 卸载一个 App（移除所有 Bean、Handler 映射、关闭 ClassLoader）
     */
    public synchronized void uninstallApp(String appId) {
        LoadedApp app = loadedApps.remove(appId);
        if (app == null) return;

        log.info("Uninstalling app: {}", appId);

        // 先清除 Handler 映射
        for (String beanName : app.beanNames) {
            removeHandlerMappings(beanName);
        }

        // 销毁并移除 Bean 定义
        for (String beanName : app.beanNames) {
            if (beanFactory.containsBeanDefinition(beanName)) {
                if (beanFactory.containsSingleton(beanName)) {
                    beanFactory.destroySingleton(beanName);
                }
                beanFactory.removeBeanDefinition(beanName);
                log.debug("Removed bean: {}", beanName);
            }
        }

        // 关闭所有 ClassLoader
        for (URLClassLoader cl : app.classLoaders) {
            try {
                cl.close();
            } catch (IOException e) {
                log.warn("Error closing classloader for app {}", appId, e);
            }
        }

        log.info("App uninstalled: {} ({} beans)", appId, app.beanNames.size());
        // 卸载后也清理 SpringDoc 缓存，避免残留的 API 文档
        refreshOpenApiCache();
    }

    /**
     * 获取已加载 App 数量
     */
    public int getLoadedAppCount() {
        return loadedApps.size();
    }

    /**
     * 获取所有已加载 App 的 ID 列表
     */
    public List<String> getLoadedAppIds() {
        return List.copyOf(loadedApps.keySet());
    }

    /**
     * 获取安装目录下所有 App 目录数（包括已停用）
     */
    public int getTotalAppCount() {
        File installDir = new File(appInstallDir);
        if (!installDir.exists() || !installDir.isDirectory()) return 0;
        File[] subdirs = installDir.listFiles(File::isDirectory);
        return subdirs != null ? subdirs.length : 0;
    }

    /**
     * 获取所有 App 及其运行状态
     */
    public List<Map<String, Object>> getAllAppsWithStatus() {
        List<Map<String, Object>> result = new ArrayList<>();
        File installDir = new File(appInstallDir);
        if (!installDir.exists() || !installDir.isDirectory()) return result;

        File[] subdirs = installDir.listFiles(File::isDirectory);
        if (subdirs == null) return result;

        for (File appDir : subdirs) {
            String appId = appDir.getName();
            LoadedApp loaded = loadedApps.get(appId);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("appId", appId);
            if (loaded != null && loaded.status == AppStatus.LOADED) {
                item.put("status", "running");
                item.put("beanCount", loaded.beanNames.size());
            } else {
                item.put("status", "stopped");
                item.put("beanCount", 0);
            }
            result.add(item);
        }
        return result;
    }

    // ==================== 内部方法 ====================

    private void loadAllExistingApps() {
        File installDir = new File(appInstallDir);
        if (!installDir.exists() || !installDir.isDirectory()) {
            log.info("App install dir not found: {}", installDir.getAbsolutePath());
            return;
        }

        File[] appDirs = installDir.listFiles(File::isDirectory);
        if (appDirs == null || appDirs.length == 0) return;

        for (File appDir : appDirs) {
            try {
                installApp(appDir);
            } catch (Exception e) {
                log.error("Failed to load app from {}: {}", appDir.getName(), e.getMessage(), e);
            }
        }
    }

    /**
     * JarFileWatcher 回调：收到 JAR 变更通知后重新加载 App
     */
    private void onAppChanged(String appId) {
        File appDir = new File(appInstallDir, appId);
        if (appDir.exists() && appDir.isDirectory()) {
            try {
                installApp(appDir);
            } catch (Exception e) {
                log.error("Failed to reload app: " + appId, e);
            }
        } else {
            // App 目录被删除
            LoadedApp existing = loadedApps.get(appId);
            if (existing != null) {
                log.info("[app-watcher] App 目录已删除，卸载: {}", appId);
                uninstallApp(appId);
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
     * 注册单个 Bean 到 Spring 容器
     */
    private void registerBean(Class<?> beanClass, List<String> beanNames) {
        try {
            String beanName = Character.toLowerCase(beanClass.getSimpleName().charAt(0))
                    + beanClass.getSimpleName().substring(1);

            if (beanFactory.containsBeanDefinition(beanName)) {
                log.warn("Bean already exists, skipping: {}", beanName);
                return;
            }

            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(beanClass);
            builder.setScope("singleton");
            builder.getRawBeanDefinition().setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
            beanFactory.registerBeanDefinition(beanName, builder.getBeanDefinition());

            beanFactory.getBean(beanName);

            beanNames.add(beanName);
            log.info("Bean registered & instantiated: {}", beanName);

            if (beanClass.isAnnotationPresent(RestController.class)) {
                registerControllerMappings(beanName);
            }

        } catch (Exception e) {
            log.error("Failed to register bean: " + beanClass.getName(), e);
        }
    }

    /**
     * 通过反射调用 detectHandlerMethods 只为单个 Controller 注册映射
     * <p>
     * 必须传入 bean 名称字符串（而非实例），因为 Spring 6 的 HandlerMethod.getBean()
     * 直接返回原始 bean 字段（不解析）。传入名称字符串时，getBean().toString() 返回
     * bean 名称，与 SpringDoc 的 mappingsMap key 一致，API 文档才能正确显示。
     * </p>
     */
    private void registerControllerMappings(String beanName) {
        try {
            Method detectMethod = AbstractHandlerMethodMapping.class
                    .getDeclaredMethod("detectHandlerMethods", Object.class);
            detectMethod.setAccessible(true);
            detectMethod.invoke(this.handlerMapping, beanName);
            log.debug("Registered handler mappings for: {}", beanName);
        } catch (Exception e) {
            String causeMsg = null;
            if (e instanceof java.lang.reflect.InvocationTargetException ite) {
                causeMsg = (ite.getCause() != null)
                        ? ite.getCause().getMessage() + " (" + ite.getCause().getClass().getSimpleName() + ")"
                        : "InvocationTargetException(null cause)";
            }
            log.warn("Failed to register handler mappings for {}: {} [cause: {}]",
                    beanName, e.getMessage(), causeMsg, e);
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

            if (mappingRegistry == null) {
                log.warn("MappingRegistry is null for handler: {}", this.handlerMapping);
                return;
            }

            Method getRegsMethod = mappingRegistry.getClass().getMethod("getRegistrations");
            getRegsMethod.setAccessible(true);
            Map<Object, Object> registrations = (Map<Object, Object>) getRegsMethod.invoke(mappingRegistry);

            List<Object> mappingsToRemove = new ArrayList<>();
            for (Map.Entry<Object, Object> entry : registrations.entrySet()) {
                Object registration = entry.getValue();
                Method getHandlerMethodMethod = registration.getClass().getMethod("getHandlerMethod");
                getHandlerMethodMethod.setAccessible(true);
                Object handlerMethod = getHandlerMethodMethod.invoke(registration);
                Method getBeanMethod = handlerMethod.getClass().getMethod("getBean");
                getBeanMethod.setAccessible(true);
                Object bean = getBeanMethod.invoke(handlerMethod);
                if (bean != null && bean.toString().equals(beanName)) {
                    mappingsToRemove.add(entry.getKey());
                }
            }

            Method unregisterMethod = AbstractHandlerMethodMapping.class
                    .getMethod("unregisterMapping", Object.class);
            for (Object mapping : mappingsToRemove) {
                unregisterMethod.invoke(this.handlerMapping, mapping);
                log.debug("Unregistered mapping: {}", mapping);
            }

            if (!mappingsToRemove.isEmpty()) {
                log.debug("Removed {} handler mapping(s) for bean: {}", mappingsToRemove.size(), beanName);
            }
        } catch (Exception e) {
            log.warn("Failed to remove handler mappings for {}: {}", beanName, e.getMessage(), e);
        }
    }

    /**
     * 清除 Knife4j / SpringDoc OpenAPI 缓存，使动态注册的 Controller 立即出现在接口文档中
     */
    private void refreshOpenApiCache() {
        try {
            Class<?> serviceClass = Class.forName("org.springdoc.core.service.OpenAPIService");
            String[] serviceNames = beanFactory.getBeanNamesForType(serviceClass);
            if (serviceNames.length > 0) {
                Object service = beanFactory.getBean(serviceNames[0]);
                Method setCached = serviceClass.getMethod("setCachedOpenAPI",
                        Class.forName("io.swagger.v3.oas.models.OpenAPI"), Locale.class);
                setCached.invoke(service, null, Locale.getDefault());
                Method getMappingsMap = serviceClass.getMethod("getMappingsMap");
                getMappingsMap.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<String, Object> mappingsMap = (Map<String, Object>) getMappingsMap.invoke(service);
                if (mappingsMap != null) {
                    mappingsMap.clear();
                }
                log.info("SpringDoc OpenAPI cache cleared for locale: {}", Locale.getDefault());
            }

            try {
                Class<?> providerClass = Class.forName("org.springdoc.webmvc.core.providers.SpringWebMvcProvider");
                String[] providerNames = beanFactory.getBeanNamesForType(providerClass);
                if (providerNames.length > 0) {
                    Object provider = beanFactory.getBean(providerNames[0]);
                    Field handlerMethodsField = providerClass.getSuperclass().getDeclaredField("handlerMethods");
                    handlerMethodsField.setAccessible(true);
                    handlerMethodsField.set(provider, null);
                    log.debug("SpringWebProvider.handlerMethods cache cleared");
                }
            } catch (Exception e2) {
                log.debug("Failed to clear SpringWebProvider cache: {}", e2.getMessage());
            }

        } catch (Exception e) {
            log.debug("SpringDoc not available or cache refresh failed: {}", e.getMessage());
        }
    }

    private static class LoadedApp {
        final String appId;
        final List<URLClassLoader> classLoaders;
        final List<String> beanNames;
        final long lastLoadedAt;
        volatile AppStatus status;

        LoadedApp(String appId, List<URLClassLoader> classLoaders, List<String> beanNames, long lastLoadedAt) {
            this.appId = appId;
            this.classLoaders = classLoaders;
            this.beanNames = beanNames;
            this.lastLoadedAt = lastLoadedAt;
            this.status = AppStatus.LOADED;
        }
    }

    /**
     * Child-first URLClassLoader：优先从 JAR 中加载类，再委托给父 ClassLoader。
     *
     * 安全限制：
     * - 禁止加载系统敏感类（java.lang.reflect、java.io.File、java.net等）
     * - 禁止加载 Spring 内部 BeanFactory 类
     * - 禁止加载与 App 部署相关的类
     */
    private static class ChildFirstURLClassLoader extends URLClassLoader {

        /** 被禁止访问的系统包前缀 */
        private static final String[] BLOCKED_PACKAGES = {
            "java.lang.reflect",
            "java.lang.invoke",
            "java.lang.management",
            "java.io",
            "java.net",
            "java.nio.file",
            "java.nio.channels",
            "java.security",
            "java.rmi",
            "javax.management",
            "javax.script",
            "javax.tools",
            "sun.reflect",
            "sun.misc",
            "jdk.internal",
        };

        /** 被禁止访问的 Spring 内部类 */
        private static final String[] BLOCKED_SPRING_CLASSES = {
            "org.springframework.beans.factory",
            "org.springframework.context.support",
            "org.springframework.boot",
        };

        ChildFirstURLClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            // 1. 安全检查
            checkBlocked(name);

            // 2. 标准 JVM 类必须从父类加载
            if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("sun.")) {
                return super.loadClass(name);
            }

            // 3. 已加载的直接返回
            Class<?> loaded = findLoadedClass(name);
            if (loaded != null) return loaded;

            // 4. 优先从 JAR 加载（child-first）
            try {
                return findClass(name);
            } catch (ClassNotFoundException e) {
                return super.loadClass(name);
            }
        }

        /**
         * 安全检查：如果类在禁止列表中，抛出异常
         */
        private void checkBlocked(String name) {
            // 禁止反射 API
            if (name.startsWith("java.lang.reflect.")) {
                throw new SecurityException("App 不允许使用反射 API: " + name);
            }
            // 禁止进程执行（防止执行系统命令）
            if (name.equals("java.lang.Runtime")
                || name.equals("java.lang.ProcessBuilder")
                || name.equals("java.lang.Process")
                || name.startsWith("java.lang.Process")) {
                throw new SecurityException("App 不允许执行系统命令: " + name);
            }
            // 禁止文件 IO
            if (name.startsWith("java.io.")) {
                throw new SecurityException("App 不允许直接文件 IO: " + name);
            }
            // 禁止网络访问
            if (name.startsWith("java.net.")) {
                throw new SecurityException("App 不允许直接网络访问: " + name);
            }
            // 禁止 NIO 文件
            if (name.startsWith("java.nio.file.") || name.startsWith("java.nio.channels.")) {
                throw new SecurityException("App 不允许文件 NIO 操作: " + name);
            }
            // 禁止安全管理器操控
            if (name.startsWith("java.security.")) {
                throw new SecurityException("App 不允许操作安全管理器: " + name);
            }
            // 禁止 System 关键操作（退出、属性修改等）
            if (name.equals("java.lang.System")) {
                throw new SecurityException("App 不允许操作 System 类: " + name);
            }
            // 禁止线程操控（修改上下文类加载器）
            if (name.equals("java.lang.Thread")) {
                throw new SecurityException("App 不允许操作 Thread 类: " + name);
            }
            // 禁止脚本引擎（防止 JavaScript 逃逸）
            if (name.startsWith("javax.script.")) {
                throw new SecurityException("App 不允许使用脚本引擎: " + name);
            }
            // 禁止 JDBC 直接访问（绕过平台数据层）
            if (name.equals("java.sql.DriverManager")
                || name.startsWith("java.sql.DriverManager")) {
                throw new SecurityException("App 不允许直接 JDBC 访问: " + name);
            }
            // 禁止安全管理器
            if (name.equals("java.lang.SecurityManager")) {
                throw new SecurityException("App 不允许设置 SecurityManager: " + name);
            }
            // 禁止 Spring 内部容器
            if (name.startsWith("org.springframework.beans.factory.")
                || name.startsWith("org.springframework.context.support.")) {
                throw new SecurityException("App 不允许访问 Spring 内部容器: " + name);
            }
        }
    }
}
