package com.yuncode.system.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yuncode.system.service.ApplicationDirectoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 应用目录服务实现类
 * 负责创建和管理应用目录结构
 *
 * @author Yuncode
 * @since 2025-01-30
 */
@Slf4j
@Service
public class ApplicationDirectoryServiceImpl implements ApplicationDirectoryService {

    /**
     * 应用基础目录（配置文件中配置，默认：../yuncode-pure-admin/apps）
     */
    @Value("${yuncode.apps.path:../yuncode-pure-admin/apps}")
    private String appsBasePath;

    /**
     * 应用安装目录（install/uninstall/history）
     */
    private static final String INSTALL_DIR = "install";
    private static final String UNINSTALL_DIR = "uninstall";
    private static final String HISTORY_DIR = "history";

    /**
     * 时间格式化器
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String createApplicationDirectory(String appId, String appName, String appIcon,
                                          String appDescription, String version) {
        try {
            // 确保apps基础目录存在
            File appsDir = new File(appsBasePath);
            if (!appsDir.exists()) {
                appsDir.mkdirs();
            }

            // 创建install/uninstall/history子目录
            createBaseDirectories(appsDir);

            // 直接使用应用ID作为文件夹名
            String folderName = appId;
            String appPath = new File(new File(appsBasePath, INSTALL_DIR), folderName).getAbsolutePath();

            log.info("开始创建应用目录: {}", appPath);

            // 创建主目录
            File appDir = new File(appPath);
            if (!appDir.exists()) {
                appDir.mkdirs();
            }

            // 创建子目录
            createSubDirectories(appDir);

            // 创建manifest.xml
            createManifestXml(appDir, appId, appName, appDescription, version);

            // 创建pom.xml
            createPomXml(appDir, appId, appName, appDescription, version);

            // 创建图标文件
            if (StrUtil.isNotBlank(appIcon)) {
                createIconFile(appDir, appIcon);
            }

            log.info("应用目录创建成功: {}", appPath);
            return appPath;

        } catch (Exception e) {
            log.error("创建应用目录失败: appId={}, appName={}", appId, appName, e);
            throw new RuntimeException("创建应用目录失败: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteApplicationDirectory(String appId) {
        try {
            String folderName = appId;
            String appPath = new File(new File(appsBasePath, INSTALL_DIR), folderName).getAbsolutePath();

            File appDir = new File(appPath);
            if (!appDir.exists()) {
                log.warn("应用目录不存在: {}", appPath);
                return true;
            }

            boolean result = FileUtil.del(appDir);
            log.info("删除应用目录: {}, 结果: {}", appPath, result ? "成功" : "失败");
            return result;

        } catch (Exception e) {
            log.error("删除应用目录失败: appId={}", appId, e);
            throw new RuntimeException("删除应用目录失败: " + e.getMessage());
        }
    }

    @Override
    public String getApplicationDirectory(String appId) {
        String folderName = appId;
        File appDir = new File(new File(appsBasePath, INSTALL_DIR), folderName);

        if (!appDir.exists()) {
            return null;
        }

        return appDir.getAbsolutePath();
    }

    @Override
    public boolean existsApplicationDirectory(String appId) {
        String folderName = appId;
        File appDir = new File(new File(appsBasePath, INSTALL_DIR), folderName);
        return appDir.exists() && appDir.isDirectory();
    }

    /**
     * 创建基础目录（install/uninstall/history）
     */
    private void createBaseDirectories(File appsDir) {
        String[] baseDirs = {INSTALL_DIR, UNINSTALL_DIR, HISTORY_DIR};

        for (String dir : baseDirs) {
            File subDir = new File(appsDir, dir);
            if (!subDir.exists()) {
                subDir.mkdirs();
                log.debug("创建基础目录: {}", subDir.getAbsolutePath());
            }
        }
    }

    /**
     * 创建子目录
     */
    private void createSubDirectories(File appDir) {
        String[] subDirs = {"lib", "repository", "template", "web"};

        for (String subDir : subDirs) {
            File dir = new File(appDir, subDir);
            if (!dir.exists()) {
                dir.mkdirs();
                log.debug("创建子目录: {}", dir.getAbsolutePath());
            }
        }
    }

    /**
     * 创建manifest.xml文件
     */
    private void createManifestXml(File appDir, String appId, String appName,
                                  String appDescription, String version) {
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        StringBuilder content = new StringBuilder();
        content.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n");
        content.append("<app xmlns=\"http://www.yuncode.com.cn/app\">\n");
        content.append("    <name>").append(escapeXml(appName)).append("</name>\n");
        content.append("    <version>").append(version != null ? version : "1.0.0").append("</version>\n");
        content.append("    <buildNo>1</buildNo>\n");
        content.append("    <developer id=\"yuncode\" tablePrefix=\"YC\" url=\"http://www.yuncode.com\">Yuncode-LowCode</developer>\n");
        content.append("    <categoryVisible>true</categoryVisible>\n");
        content.append("    <description><![CDATA[").append(appDescription != null ? appDescription : "").append("]]></description>\n");
        content.append("    <details><![CDATA[").append(appDescription != null ? appDescription : "").append("]]></details>\n");
        content.append("    <installListener/>\n");
        content.append("    <pluginListener/>\n");
        content.append("    <startListener/>\n");
        content.append("    <stopListener/>\n");
        content.append("    <upgradeListener/>\n");
        content.append("    <uninstallListener/>\n");
        content.append("    <reloadable>true</reloadable>\n");
        content.append("    <requires/>\n");
        content.append("    <properties/>\n");
        content.append("    <allowStartup>true</allowStartup>\n");
        content.append("    <allowUpgradeByStore>true</allowUpgradeByStore>\n");
        content.append("    <depend versions=\"1.0\" env=\"\">_platform</depend>\n");
        content.append("    <modelAdministrator/>\n");
        content.append("    <installDate>").append(currentDate).append("</installDate>\n");
        content.append("    <icon code=\"\" color=\"#409EFF\"/>\n");
        content.append("    <productId/>\n");
        content.append("    <deployment/>\n");
        content.append("    <releaseDate>").append(currentDate).append("</releaseDate>\n");
        content.append("    <upgradeDate/>\n");
        content.append("    <restoreDate/>\n");
        content.append("</app>");

        File manifestFile = new File(appDir, "manifest.xml");
        FileUtil.writeUtf8String(content.toString(), manifestFile);
        log.debug("创建manifest.xml: {}", manifestFile.getAbsolutePath());
    }

    /**
     * 创建pom.xml文件
     */
    private void createPomXml(File appDir, String appId, String appName, String appDescription, String version) {
        // 从appId中提取artifactId（取最后一段）
        String artifactId = appId.substring(appId.lastIndexOf('.') + 1);
        if (artifactId.isEmpty()) {
            artifactId = "app";
        }

        StringBuilder content = new StringBuilder();
        content.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        content.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n");
        content.append("         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        content.append("         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0\n");
        content.append("         http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n");
        content.append("    <modelVersion>4.0.0</modelVersion>\n");
        content.append("\n");
        content.append("    <parent>\n");
        content.append("        <groupId>com.yuncode</groupId>\n");
        content.append("        <artifactId>yuncode-lowcode-boot</artifactId>\n");
        content.append("        <version>1.0.0</version>\n");
        content.append("    </parent>\n");
        content.append("\n");
        content.append("    <artifactId>").append(escapeXml(artifactId)).append("</artifactId>\n");
        content.append("    <version>").append(version != null ? version : "1.0.0").append("</version>\n");
        content.append("    <packaging>jar</packaging>\n");
        content.append("\n");
        content.append("    <name>").append(escapeXml(appName)).append("</name>\n");
        content.append("    <description>").append(escapeXml(appDescription != null ? appDescription : "")).append("</description>\n");
        content.append("\n");
        content.append("    <properties>\n");
        content.append("        <java.version>17</java.version>\n");
        content.append("        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n");
        content.append("    </properties>\n");
        content.append("\n");
        content.append("    <dependencies>\n");
        content.append("        <!-- Yuncode Common (平台公共库) -->\n");
        content.append("        <dependency>\n");
        content.append("            <groupId>com.yuncode</groupId>\n");
        content.append("            <artifactId>yuncode-common</artifactId>\n");
        content.append("            <version>1.0.0</version>\n");
        content.append("        </dependency>\n");
        content.append("\n");
        content.append("        <!-- Spring Boot Starter Web -->\n");
        content.append("        <dependency>\n");
        content.append("            <groupId>org.springframework.boot</groupId>\n");
        content.append("            <artifactId>spring-boot-starter-web</artifactId>\n");
        content.append("        </dependency>\n");
        content.append("\n");
        content.append("        <!-- Lombok -->\n");
        content.append("        <dependency>\n");
        content.append("            <groupId>org.projectlombok</groupId>\n");
        content.append("            <artifactId>lombok</artifactId>\n");
        content.append("            <optional>true</optional>\n");
        content.append("        </dependency>\n");
        content.append("    </dependencies>\n");
        content.append("\n");
        content.append("    <build>\n");
        content.append("        <plugins>\n");
        content.append("            <plugin>\n");
        content.append("                <groupId>org.apache.maven.plugins</groupId>\n");
        content.append("                <artifactId>maven-compiler-plugin</artifactId>\n");
        content.append("                <version>3.11.0</version>\n");
        content.append("                <configuration>\n");
        content.append("                    <source>17</source>\n");
        content.append("                    <target>17</target>\n");
        content.append("                    <encoding>UTF-8</encoding>\n");
        content.append("                </configuration>\n");
        content.append("            </plugin>\n");
        content.append("        </plugins>\n");
        content.append("    </build>\n");
        content.append("</project>");

        File pomFile = new File(appDir, "pom.xml");
        FileUtil.writeUtf8String(content.toString(), pomFile);
        log.debug("创建pom.xml: {}", pomFile.getAbsolutePath());
    }

    /**
     * 创建图标文件
     */
    private void createIconFile(File appDir, String appIcon) {
        // 解析图标数据（可能是JSON字符串或纯图标名称）
        String iconData = parseIconData(appIcon);

        // 判断是base64还是URL
        if (iconData.startsWith("data:image")) {
            // Base64格式，解码并保存
            String[] parts = iconData.split(",");
            if (parts.length == 2) {
                String header = parts[0];
                String data = parts[1];

                // 提取图片类型
                String extension = "png";
                if (header.contains("image/jpeg")) {
                    extension = "jpg";
                } else if (header.contains("image/gif")) {
                    extension = "gif";
                } else if (header.contains("image/webp")) {
                    extension = "webp";
                }

                // 解码并保存
                byte[] imageBytes = java.util.Base64.getDecoder().decode(data);
                File iconFile = new File(appDir, "icon.png");
                FileUtil.writeBytes(imageBytes, iconFile);
                log.debug("创建icon.png: {}", iconFile.getAbsolutePath());
            }
        } else if (iconData.startsWith("http")) {
            // URL格式，创建占位符文件（实际下载可以在前端完成）
            File iconFile = new File(appDir, "icon.png");
            FileUtil.writeUtf8String("icon-url: " + iconData, iconFile);
            log.debug("创建icon.png占位符: {}", iconFile.getAbsolutePath());
        } else {
            // 图标名称格式，创建占位符文件
            File iconFile = new File(appDir, "icon.png");
            FileUtil.writeUtf8String("icon-name: " + iconData, iconFile);
            log.debug("创建icon.png占位符: {}", iconFile.getAbsolutePath());
        }
    }

    /**
     * 解析图标数据
     * 处理JSON字符串格式：{"icon":"Box","color":"#409eff"}
     */
    private String parseIconData(String appIcon) {
        if (StrUtil.isBlank(appIcon)) {
            return "Box"; // 默认图标
        }

        // 尝试解析为JSON
        try {
            if (appIcon.startsWith("{")) {
                JSONObject json = JSONUtil.parseObj(appIcon);
                // 如果包含icon字段，返回icon值（颜色信息暂时忽略）
                return json.getStr("icon", appIcon);
            }
        } catch (Exception e) {
            // 不是JSON格式，直接返回原始值
            log.debug("图标数据不是JSON格式: {}", appIcon);
        }

        return appIcon;
    }

    /**
     * XML特殊字符转义
     */
    private String escapeXml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}
