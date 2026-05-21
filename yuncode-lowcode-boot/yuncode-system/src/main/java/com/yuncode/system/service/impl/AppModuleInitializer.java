package com.yuncode.system.service.impl;

import com.yuncode.system.service.MavenModuleService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * 应用模块初始化器。
 * <p>
 * 在 Spring 启动完成后，扫描 apps/install/ 目录下已有的应用目录，
 * 自动将其注册为 Maven 子模块（Dev 模式），
 * 确保历史创建的 App 也能参与项目构建。
 * </p>
 */
@Slf4j
@Component
public class AppModuleInitializer {

    @Value("${yuncode.apps.path:./apps}")
    private String appsBasePath;

    private final MavenModuleService mavenModuleService;

    public AppModuleInitializer(MavenModuleService mavenModuleService) {
        this.mavenModuleService = mavenModuleService;
    }

    @PostConstruct
    public void init() {
        try {
            File installDir = new File(appsBasePath, "install");
            if (!installDir.exists() || !installDir.isDirectory()) {
                log.info("应用安装目录不存在，跳过启动扫描: {}", installDir.getAbsolutePath());
                return;
            }

            File[] appDirs = installDir.listFiles(File::isDirectory);
            if (appDirs == null || appDirs.length == 0) {
                return;
            }

            log.info("扫描到 {} 个已安装应用，开始注册 Maven 模块", appDirs.length);
            for (File appDir : appDirs) {
                String appId = appDir.getName();
                if (hasPomXml(appDir) && !mavenModuleService.isModuleRegistered(appId)) {
                    log.info("自动注册 Maven 模块: {}", appId);
                    mavenModuleService.registerModule(appId);
                }
            }
        } catch (Exception e) {
            log.warn("启动时扫描注册 Maven 模块失败（可忽略）", e);
        }
    }

    private boolean hasPomXml(File appDir) {
        return new File(appDir, "pom.xml").exists();
    }
}
