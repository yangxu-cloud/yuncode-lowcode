package com.yuncode.system.service.impl;

import com.yuncode.system.service.MavenModuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maven 模块注册服务实现类。
 * <p>
 * 通过字符串匹配方式直接修改 pom.xml 文件，
 * 实现 App 子模块的自动注册和注销。
 * 仅在 Dev 模式有效（本地文件系统存在 pom.xml）。
 * </p>
 */
@Slf4j
@Service
public class MavenModuleServiceImpl implements MavenModuleService {

    /**
     * 应用基础目录（配置文件中配置，默认：./apps）
     * 相对于 user.dir（即 yuncode-lowcode-boot/）
     */
    @Value("${yuncode.apps.path:./apps}")
    private String appsBasePath;

    @Override
    public boolean registerModule(String appId) {
        String modulePath = resolveModulePath(appId);
        log.info("注册 Maven 模块: appId={}, modulePath={}", appId, modulePath);

        // 检查文件是否存在
        File parentPom = getParentPomFile();
        File adminPom = getAdminPomFile();
        if (!parentPom.exists()) {
            log.warn("父 pom.xml 不存在（非 Dev 环境）: {}", parentPom.getAbsolutePath());
            return false;
        }

        boolean parentUpdated = addModuleToParentPom(parentPom, modulePath);
        boolean adminUpdated = false;
        if (adminPom.exists()) {
            String artifactId = extractArtifactId(appId);
            adminUpdated = addDependencyToAdminPom(adminPom, artifactId);
        }

        if (parentUpdated) {
            log.info("Maven 模块注册成功: {}", appId);
        }
        return parentUpdated || adminUpdated;
    }

    @Override
    public boolean unregisterModule(String appId) {
        String modulePath = resolveModulePath(appId);
        log.info("注销 Maven 模块: appId={}, modulePath={}", appId, modulePath);

        File parentPom = getParentPomFile();
        File adminPom = getAdminPomFile();

        boolean parentUpdated = false;
        if (parentPom.exists()) {
            parentUpdated = removeModuleFromParentPom(parentPom, modulePath);
        }

        boolean adminUpdated = false;
        if (adminPom.exists()) {
            String artifactId = extractArtifactId(appId);
            adminUpdated = removeDependencyFromAdminPom(adminPom, artifactId);
        }

        return parentUpdated || adminUpdated;
    }

    @Override
    public boolean isModuleRegistered(String appId) {
        String modulePath = resolveModulePath(appId);
        try {
            File parentPom = getParentPomFile();
            if (!parentPom.exists()) return false;
            String content = Files.readString(parentPom.toPath());
            return content.contains("<module>" + modulePath + "</module>");
        } catch (Exception e) {
            log.warn("检查模块注册状态失败: {}", e.getMessage());
            return false;
        }
    }

    // ========== pom.xml 文件定位 ==========

    private File getParentPomFile() {
        // appsBasePath 是相对于 user.dir 的
        // 如果 appsBasePath = ./apps, user.dir = yuncode-lowcode-boot/
        // 则 parent pom = yuncode-lowcode-boot/pom.xml = appsBasePath/../pom.xml
        File appsDir = new File(appsBasePath);
        if (appsDir.isAbsolute()) {
            return new File(appsDir, "../pom.xml");
        }
        // 相对路径：相对于 user.dir
        String userDir = System.getProperty("user.dir", ".");
        return new File(userDir, "pom.xml");
    }

    private File getAdminPomFile() {
        File appsDir = new File(appsBasePath);
        if (appsDir.isAbsolute()) {
            return new File(appsDir, "../yuncode-admin/pom.xml");
        }
        String userDir = System.getProperty("user.dir", ".");
        return new File(userDir, "yuncode-admin/pom.xml");
    }

    // ========== 父 pom.xml 操作（字符串方式） ==========

    private boolean addModuleToParentPom(File pomFile, String modulePath) {
        try {
            String content = Files.readString(pomFile.toPath(), StandardCharsets.UTF_8);

            // 检查是否已存在
            if (content.contains("<module>" + modulePath + "</module>")) {
                log.info("模块已存在，跳过: {}", modulePath);
                return true;
            }

            // 在 </modules> 前插入新的 <module> 条目
            String modulesCloseTag = "</modules>";
            int idx = content.lastIndexOf(modulesCloseTag);
            if (idx == -1) {
                log.warn("父 pom.xml 未找到 </modules> 标签");
                return false;
            }

            String indent = "        ";
            String newModule = indent + "<module>" + modulePath + "</module>\n";
            String newContent = content.substring(0, idx) + newModule + content.substring(idx);

            Files.writeString(pomFile.toPath(), newContent, StandardCharsets.UTF_8);
            log.info("已添加模块到父 pom.xml: {}", modulePath);
            return true;

        } catch (IOException e) {
            log.error("修改父 pom.xml 失败", e);
            return false;
        }
    }

    private boolean removeModuleFromParentPom(File pomFile, String modulePath) {
        try {
            String content = Files.readString(pomFile.toPath(), StandardCharsets.UTF_8);
            String moduleEntry = "<module>" + modulePath + "</module>";

            if (!content.contains(moduleEntry)) {
                return true; // 不存在也算成功
            }

            // 移除整行（包括前面的空格缩进）
            String newContent = content.lines()
                .filter(line -> !line.trim().equals(moduleEntry))
                .collect(Collectors.joining("\n"));

            Files.writeString(pomFile.toPath(), newContent, StandardCharsets.UTF_8);
            log.info("已从父 pom.xml 移除模块: {}", modulePath);
            return true;

        } catch (IOException e) {
            log.error("修改父 pom.xml 失败", e);
            return false;
        }
    }

    // ========== yuncode-admin/pom.xml 操作（字符串方式） ==========

    private boolean addDependencyToAdminPom(File pomFile, String artifactId) {
        try {
            String content = Files.readString(pomFile.toPath(), StandardCharsets.UTF_8);

            // 检查是否已存在
            if (content.contains("<artifactId>" + artifactId + "</artifactId>")) {
                log.info("依赖已存在，跳过: {}", artifactId);
                return true;
            }

            // 在 </dependencies> 前插入新的 dependency 条目
            String depsCloseTag = "</dependencies>";
            int idx = content.lastIndexOf(depsCloseTag);
            if (idx == -1) {
                log.warn("yuncode-admin/pom.xml 未找到 </dependencies> 标签");
                return false;
            }

            String depBlock = """
                    <!-- App Module: %s -->
                    <dependency>
                        <groupId>com.yuncode</groupId>
                        <artifactId>%s</artifactId>
                        <version>1.0.0</version>
                    </dependency>
                """.formatted(artifactId, artifactId);

            String newContent = content.substring(0, idx) + depBlock + "\n    " + content.substring(idx);

            Files.writeString(pomFile.toPath(), newContent, StandardCharsets.UTF_8);
            log.info("已添加依赖到 yuncode-admin/pom.xml: {}", artifactId);
            return true;

        } catch (Exception e) {
            log.error("修改 yuncode-admin/pom.xml 失败", e);
            return false;
        }
    }

    private boolean removeDependencyFromAdminPom(File pomFile, String artifactId) {
        try {
            String content = Files.readString(pomFile.toPath(), StandardCharsets.UTF_8);

            // 找到 <dependency> 块并移除
            String marker = "<artifactId>" + artifactId + "</artifactId>";
            if (!content.contains(marker)) {
                return true;
            }

            // 从 <dependency> 开始到 </dependency> 结束
            int depStart = content.lastIndexOf("<dependency>", content.indexOf(marker));
            int depEnd = content.indexOf("</dependency>", depStart) + "</dependency>".length();
            if (depStart == -1 || depEnd == -1) {
                log.warn("未找到依赖块: {}", artifactId);
                return false;
            }

            // 向前找到行首，移除整块
            int lineStart = content.lastIndexOf('\n', depStart);
            if (lineStart >= 0) {
                // 包含前面的注释行
                String beforeDep = content.substring(0, lineStart);
                int commentStart = beforeDep.lastIndexOf("<!-- App Module:");
                if (commentStart >= 0) {
                    int commentLineStart = content.lastIndexOf('\n', commentStart);
                    if (commentLineStart >= 0) {
                        depStart = commentLineStart;
                    }
                }
            }

            String newContent = content.substring(0, depStart) + content.substring(depEnd);
            Files.writeString(pomFile.toPath(), newContent, StandardCharsets.UTF_8);
            log.info("已从 yuncode-admin/pom.xml 移除依赖: {}", artifactId);
            return true;

        } catch (Exception e) {
            log.error("修改 yuncode-admin/pom.xml 失败", e);
            return false;
        }
    }

    // ========== 工具方法 ==========

    /**
     * 将 appId 转换为 Maven 模块路径
     */
    private String resolveModulePath(String appId) {
        return "apps/install/" + appId;
    }

    /**
     * 从 appId 中提取 artifactId（取最后一段）
     */
    private String extractArtifactId(String appId) {
        int idx = appId.lastIndexOf('.');
        return idx >= 0 ? appId.substring(idx + 1) : appId;
    }
}
