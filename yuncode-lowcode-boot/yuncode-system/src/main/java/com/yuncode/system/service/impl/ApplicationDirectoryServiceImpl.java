package com.yuncode.system.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yuncode.system.service.ApplicationDirectoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final String STAGING_DIR = "staging";

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

    @Override
    public boolean moveAppDirectory(String appId, String fromDir, String toDir) {
        try {
            String folderName = appId;
            File sourceDir = new File(new File(appsBasePath, fromDir), folderName);
            File targetDir = new File(new File(appsBasePath, toDir), folderName);

            if (!sourceDir.exists()) {
                log.warn("源目录不存在: {}", sourceDir.getAbsolutePath());
                return false;
            }

            // 确保目标父目录存在
            targetDir.getParentFile().mkdirs();

            // 如果目标已存在先删除
            if (targetDir.exists()) {
                FileUtil.del(targetDir);
            }

            // File.renameTo 在 Windows 上可能静默失败，使用 Hutool 的 FileUtil.move 确保跨平台可靠
            FileUtil.move(sourceDir, targetDir, true);
            log.info("移动应用目录: {} → {} 成功", sourceDir.getAbsolutePath(), targetDir.getAbsolutePath());
            return true;

        } catch (Exception e) {
            log.error("移动应用目录失败: appId={}, from={}, to={}", appId, fromDir, toDir, e);
            throw new RuntimeException("移动应用目录失败: " + e.getMessage());
        }
    }

    @Override
    public File packageApplication(String appId, String newVersion) {
        File installDir = new File(new File(appsBasePath, INSTALL_DIR), appId);
        if (!installDir.exists()) {
            log.error("应用目录不存在，无法打包: {}", installDir.getAbsolutePath());
            throw new RuntimeException("应用目录不存在，无法打包");
        }

        // 临时 dist 目录（分发文件即时生成，下载后清理）
        File distDir = new File(new File(new File(appsBasePath, HISTORY_DIR), appId), "dist");
        distDir.mkdirs();

        // 清理上次的残留 .sap 文件
        File[] oldFiles = distDir.listFiles((dir, name) -> name.endsWith(".sap"));
        if (oldFiles != null) {
            for (File f : oldFiles) {
                FileUtil.del(f);
            }
        }

        // 创建临时目录用于处理打包内容
        File tempDir = null;
        try {
            tempDir = java.nio.file.Files.createTempDirectory("yuncode-dist-").toFile();
            // 复制安装目录内容（排除 src/ 和 target/）
            FileUtil.copyContent(installDir, tempDir, true);
            FileUtil.del(new File(tempDir, "src"));
            FileUtil.del(new File(tempDir, "target"));

            // 更新临时目录中 manifest.xml 的版本号
            updateManifestVersionInDir(tempDir, newVersion);

            // 创建 .sap 压缩包
            String fileName = appId + ".sap";
            File sapFile = new File(distDir, fileName);
            ZipUtil.zip(sapFile, false, tempDir);

            log.info("应用打包成功: {}, 文件: {}, 大小: {} bytes",
                    appId, sapFile.getAbsolutePath(), sapFile.length());
            return sapFile;

        } catch (Exception e) {
            log.error("打包应用失败: appId={}", appId, e);
            throw new RuntimeException("打包应用失败: " + e.getMessage());
        } finally {
            // 清理临时目录
            if (tempDir != null) {
                FileUtil.del(tempDir);
            }
        }
    }

    @Override
    public File snapshotApplication(String appId, String version) {
        File installDir = new File(new File(appsBasePath, INSTALL_DIR), appId);
        if (!installDir.exists()) {
            log.warn("安装目录不存在，跳过快照: {}", installDir.getAbsolutePath());
            return null;
        }

        File snapshotDir = new File(new File(new File(appsBasePath, HISTORY_DIR), appId), "snapshots");
        snapshotDir.mkdirs();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String snapshotName = "v" + version + "-" + timestamp + ".zip";
        File snapshotFile = new File(snapshotDir, snapshotName);

        ZipUtil.zip(snapshotFile, false, installDir);

        log.info("应用快照创建成功: {}, 大小: {} bytes", snapshotFile.getAbsolutePath(), snapshotFile.length());

        // 只保留最新 5 个快照
        cleanupOldSnapshots(snapshotDir, 5);

        return snapshotFile;
    }

    @Override
    public File getDistributeFile(String appId, String fileName) {
        File distDir = new File(new File(new File(appsBasePath, HISTORY_DIR), appId), "dist");
        File file = new File(distDir, fileName);
        if (file.exists() && file.isFile()) {
            return file;
        }
        log.warn("分发文件不存在: {}", file.getAbsolutePath());
        return null;
    }

    @Override
    public boolean deleteDistributeFile(String appId, String fileName) {
        File file = getDistributeFile(appId, fileName);
        if (file != null) {
            try {
                boolean deleted = FileUtil.del(file);
                log.info("删除分发临时文件: {}, 结果: {}", file.getAbsolutePath(), deleted);
                return deleted;
            } catch (Exception e) {
                log.warn("删除分发临时文件失败（文件可能被占用）: {}, {}", file.getAbsolutePath(), e.getMessage());
                return false;
            }
        }
        return false;
    }

    /**
     * 清理旧快照，只保留最新 N 个
     */
    private void cleanupOldSnapshots(File snapshotDir, int maxKeep) {
        File[] files = snapshotDir.listFiles((dir, name) -> name.endsWith(".zip"));
        if (files == null || files.length <= maxKeep) {
            return;
        }

        java.util.Arrays.sort(files, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));

        for (int i = 0; i < files.length - maxKeep; i++) {
            FileUtil.del(files[i]);
            log.debug("清理旧快照: {}", files[i].getName());
        }
    }

    @Override
    public void updateManifestVersion(String appId, String newVersion) {
        File appDir = new File(new File(appsBasePath, INSTALL_DIR), appId);
        updateManifestVersionInDir(appDir, newVersion);
    }

    @Override
    public Map<String, String> stageFromSap(File sapFile) {
        File tempDir = null;
        try {
            tempDir = java.nio.file.Files.createTempDirectory("yuncode-stage-").toFile();
            ZipUtil.unzip(sapFile, tempDir);

            // 处理 .sap 打包时可能包含单个根目录的情况
            // 如果解压后只有一个子目录，则将工作目录指向该子目录
            File workDir = tempDir;
            File[] topFiles = tempDir.listFiles();
            if (topFiles != null && topFiles.length == 1 && topFiles[0].isDirectory()) {
                workDir = topFiles[0];
                log.debug("检测到 .sap 包含外层目录，使用子目录: {}", topFiles[0].getName());
            }

            String appId = resolveAppId(workDir);
            String appName = readManifestValue(workDir, "name");
            String version = readManifestValue(workDir, "version");

            if (appId == null || appId.isEmpty()) {
                // 日志输出解压后目录结构，方便排查
                log.warn("manifest.xml 未找到应用标识，解压目录内容: {}", Arrays.toString(tempDir.list()));
                throw new RuntimeException("manifest.xml 缺少应用标识 <id>/<sapid> 或 deployment/system/@appId");
            }

            File stagingDir = new File(new File(appsBasePath, STAGING_DIR), appId);
            // 如果暂存区已有同 appId 的包，先删除
            if (stagingDir.exists()) {
                FileUtil.del(stagingDir);
            }
            stagingDir.getParentFile().mkdirs();
            if (workDir != tempDir) {
                // .sap 有外层目录：移出子目录，再清理 tempDir 空壳
                FileUtil.move(workDir, stagingDir, true);
                if (tempDir.exists()) {
                    FileUtil.del(tempDir);
                }
            } else {
                // 常规：直接移动整个 tempDir
                FileUtil.move(tempDir, stagingDir, true);
            }
            tempDir = null;

            long fileSize = FileUtil.size(stagingDir);

            Map<String, String> result = new HashMap<>();
            result.put("appId", appId);
            result.put("appName", appName != null ? appName : appId);
            result.put("version", version != null ? version : "1.0.0");
            result.put("fileSize", String.valueOf(fileSize));
            log.info(".sap 暂存成功: appId={}, appName={}, version={}", appId, appName, version);
            return result;
        } catch (Exception e) {
            log.error("暂存.sap文件失败", e);
            throw new RuntimeException("暂存失败: " + e.getMessage());
        } finally {
            if (tempDir != null) {
                FileUtil.del(tempDir);
            }
        }
    }

    @Override
    public List<Map<String, String>> listStagedPackages() {
        List<Map<String, String>> result = new java.util.ArrayList<>();
        File stagingDir = new File(appsBasePath, STAGING_DIR);
        if (!stagingDir.exists()) {
            return result;
        }
        File[] dirs = stagingDir.listFiles(File::isDirectory);
        if (dirs == null) {
            return result;
        }
        for (File dir : dirs) {
            String appName = readManifestValue(dir, "name");
            String version = readManifestValue(dir, "version");
            Map<String, String> info = new HashMap<>();
            info.put("appId", dir.getName());
            info.put("appName", appName != null ? appName : dir.getName());
            info.put("version", version != null ? version : "1.0.0");
            info.put("fileSize", String.valueOf(FileUtil.size(dir)));
            result.add(info);
        }
        return result;
    }

    @Override
    public Map<String, String> deployStagedPackage(String appId) {
        File stagingDir = new File(new File(appsBasePath, STAGING_DIR), appId);
        if (!stagingDir.exists()) {
            throw new RuntimeException("暂存包不存在: " + appId);
        }

        String appName = readManifestValue(stagingDir, "name");
        String version = readManifestValue(stagingDir, "version");

        File installDir = new File(new File(appsBasePath, INSTALL_DIR), appId);
        if (installDir.exists()) {
            FileUtil.del(installDir);
        }
        installDir.getParentFile().mkdirs();
        FileUtil.move(stagingDir, installDir, true);

        Map<String, String> result = new HashMap<>();
        result.put("appId", appId);
        result.put("appName", appName != null ? appName : appId);
        result.put("version", version != null ? version : "1.0.0");
        log.info("暂存包部署成功: appId={}", appId);
        return result;
    }

    @Override
    public boolean deleteStagedPackage(String appId) {
        File stagingDir = new File(new File(appsBasePath, STAGING_DIR), appId);
        if (!stagingDir.exists()) {
            return true;
        }
        boolean deleted = FileUtil.del(stagingDir);
        log.info("删除暂存包: appId={}, 结果={}", appId, deleted);
        return deleted;
    }

    /**
     * 从 manifest.xml 中读取应用标识 &lt;id&gt;
     */
    private String resolveAppId(File workDir) {
        return readManifestValue(workDir, "id");
    }

    /**
     * 从 manifest.xml 读取指定标签的值
     */
    private String readManifestValue(File dir, String tagName) {
        File manifestFile = new File(dir, "manifest.xml");
        if (!manifestFile.exists()) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(manifestFile);
            // 先尝试 namespace-aware 查找
            NodeList nodes = doc.getElementsByTagNameNS("*", tagName);
            if (nodes.getLength() > 0) {
                return nodes.item(0).getTextContent().trim();
            }
            // 降级：非 namespace-aware 查找
            NodeList legacyNodes = doc.getElementsByTagName(tagName);
            if (legacyNodes.getLength() > 0) {
                return legacyNodes.item(0).getTextContent().trim();
            }
        } catch (Exception e) {
            log.warn("读取manifest.xml字段失败: tag={}", tagName, e);
        }
        return null;
    }

    @Override
    public String readManifestVersion(String appId) {
        File appDir = new File(new File(appsBasePath, INSTALL_DIR), appId);
        File manifestFile = new File(appDir, "manifest.xml");
        if (!manifestFile.exists()) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(manifestFile);
            NodeList versionNodes = doc.getElementsByTagNameNS("*", "version");
            if (versionNodes.getLength() > 0) {
                return versionNodes.item(0).getTextContent().trim();
            }
            // 降级：非 namespace-aware 查找
            NodeList legacyNodes = doc.getElementsByTagName("version");
            if (legacyNodes.getLength() > 0) {
                return legacyNodes.item(0).getTextContent().trim();
            }
        } catch (Exception e) {
            log.error("读取manifest.xml版本失败: appId={}", appId, e);
        }
        return null;
    }

    @Override
    public String readStagingManifestVersion(String appId) {
        File stagingDir = new File(new File(appsBasePath, STAGING_DIR), appId);
        File manifestFile = new File(stagingDir, "manifest.xml");
        if (!manifestFile.exists()) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(manifestFile);
            NodeList versionNodes = doc.getElementsByTagNameNS("*", "version");
            if (versionNodes.getLength() > 0) {
                return versionNodes.item(0).getTextContent().trim();
            }
            NodeList legacyNodes = doc.getElementsByTagName("version");
            if (legacyNodes.getLength() > 0) {
                return legacyNodes.item(0).getTextContent().trim();
            }
        } catch (Exception e) {
            log.error("读取暂存区manifest.xml版本失败: appId={}", appId, e);
        }
        return null;
    }

    /**
     * 在指定目录中更新 manifest.xml 的版本号
     */
    private void updateManifestVersionInDir(File appDir, String newVersion) {
        File manifestFile = new File(appDir, "manifest.xml");
        if (!manifestFile.exists()) {
            log.warn("manifest.xml 不存在: {}", manifestFile.getAbsolutePath());
            return;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(manifestFile);

            NodeList versionNodes = doc.getElementsByTagNameNS("*", "version");
            if (versionNodes.getLength() > 0) {
                versionNodes.item(0).setTextContent(newVersion);
            } else {
                // 降级：非 namespace-aware 查找
                NodeList legacyNodes = doc.getElementsByTagName("version");
                if (legacyNodes.getLength() > 0) {
                    legacyNodes.item(0).setTextContent(newVersion);
                }
            }

            // 写回文件
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(manifestFile);
            transformer.transform(source, result);

            log.info("manifest.xml版本已更新为: {}", newVersion);
        } catch (Exception e) {
            log.error("更新manifest.xml版本失败", e);
            throw new RuntimeException("更新manifest.xml版本失败: " + e.getMessage());
        }
    }

    /**
     * 创建基础目录（install/uninstall/history）
     */
    private void createBaseDirectories(File appsDir) {
        String[] baseDirs = {INSTALL_DIR, UNINSTALL_DIR, HISTORY_DIR, STAGING_DIR};

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
     * 创建manifest.xml文件（Yuncode 格式 v1）
     *
     * 格式说明：
     * <app> - 根元素
     *   <id> 应用唯一标识（包名格式）
     *   <name> 应用显示名称
     *   <version> 版本号
     *   <description> 应用描述
 *   <details> 详细描述
     *   <icon> 图标 code + color
     *   <developer> 开发者信息
     *   <listeners> 生命周期钩子
     *   <dependencies> 依赖的平台/模块
     *   <properties> 扩展配置
     *   <deployment> 部署菜单结构（<system id="..." name="..."> → <menu>）
     *   <dates> 时间戳
     */
    private void createManifestXml(File appDir, String appId, String appName,
                                  String appDescription, String version) {
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        StringBuilder content = new StringBuilder();
        content.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n");
        content.append("<app xmlns=\"http://www.yuncode.com/app\">\n");
        content.append("    <id>").append(escapeXml(appId)).append("</id>\n");
        content.append("    <name>").append(escapeXml(appName)).append("</name>\n");
        content.append("    <version>").append(version != null ? version : "1.0.0").append("</version>\n");
        content.append("    <description><![CDATA[").append(appDescription != null ? appDescription : "").append("]]></description>\n");
        content.append("    <details><![CDATA[").append(appDescription != null ? appDescription : "").append("]]></details>\n");
        content.append("    <icon code=\"Box\" color=\"#409EFF\"/>\n");
        content.append("    <developer id=\"yuncode\">Yuncode-LowCode</developer>\n");
        content.append("    <listeners>\n");
        content.append("        <install/>\n");
        content.append("        <start/>\n");
        content.append("        <stop/>\n");
        content.append("        <uninstall/>\n");
        content.append("        <upgrade/>\n");
        content.append("    </listeners>\n");
        content.append("    <dependencies>\n");
        content.append("        <platform min-version=\"1.0\"/>\n");
        content.append("    </dependencies>\n");
        content.append("    <properties/>\n");
        content.append("    <deployment>\n");
        content.append("        <system id=\"").append(escapeXml(appId)).append("\" name=\"").append(escapeXml(appName)).append("\"/>\n");
        content.append("    </deployment>\n");
        content.append("    <dates>\n");
        content.append("        <created>").append(currentDate).append("</created>\n");
        content.append("    </dates>\n");
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
