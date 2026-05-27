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

    /** 目录下是否存在含有 lib/*.jar 的 App 子目录 */
    private boolean hasAppSubdirs(File installDir) {
        File[] subdirs = installDir.listFiles(File::isDirectory);
        if (subdirs == null) return false;
        for (File sub : subdirs) {
            File libDir = new File(sub, "lib");
            File[] jars = libDir.listFiles((d, n) -> n.endsWith(".jar"));
            if (jars != null && jars.length > 0) return true;
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
     * 安装一个 App 目录（加载其 lib/ 下所有 JAR，注册所有 Spring Bean）
     */
    public synchronized void installApp(File appDir) {
        String appId = appDir.getName();

        if (loadedApps.containsKey(appId)) {
            log.info("App already loaded, replacing: {}", appId);
            uninstallApp(appId);
        }

        File libDir = new File(appDir, "lib");
        File[] jars = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            log.info("No JARs in {}/lib/, skipping: {}", appId, appDir.getAbsolutePath());
            return;
        }

        log.info("Installing app: {} ({} JARs)", appId, jars.length);

        List<URLClassLoader> classLoaders = new ArrayList<>();
        List<String> allBeanNames = new ArrayList<>();

        for (File jar : jars) {
            try {
                URLClassLoader cl = new ChildFirstURLClassLoader(
                        new URL[]{jar.toURI().toURL()},
                        getClass().getClassLoader()
                );
                List<Class<?>> beanClasses = scanJarForBeans(jar, cl);
                for (Class<?> beanClass : beanClasses) {
                    registerBean(beanClass, allBeanNames);
                }
                classLoaders.add(cl);
                log.debug("Loaded JAR: {} ({} beans)", jar.getName(), beanClasses.size());
            } catch (Exception e) {
                log.error("Failed to load JAR {} in app {}: {}", jar.getName(), appId, e.getMessage(), e);
                // 已创建的 classloader 需要关闭，已注册的 bean 不回滚（由 uninstallApp 在重试时清理）
                for (URLClassLoader cl : classLoaders) {
                    try { cl.close(); } catch (IOException ignored) {}
                }
                return;
            }
        }

        loadedApps.put(appId, new LoadedApp(appId, classLoaders, allBeanNames, System.currentTimeMillis()));
        log.info("App installed successfully: {} ({} beans)", appId, allBeanNames.size());
        refreshOpenApiCache();
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

    private record LoadedApp(String appId, List<URLClassLoader> classLoaders,
                             List<String> beanNames, long lastLoadedAt) {}

    /**
     * Child-first URLClassLoader：优先从 JAR 中加载类，再委托给父 ClassLoader。
     */
    private static class ChildFirstURLClassLoader extends URLClassLoader {
        ChildFirstURLClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("sun.")) {
                return super.loadClass(name);
            }

            Class<?> loaded = findLoadedClass(name);
            if (loaded != null) return loaded;

            try {
                return findClass(name);
            } catch (ClassNotFoundException e) {
                return super.loadClass(name);
            }
        }
    }
}
