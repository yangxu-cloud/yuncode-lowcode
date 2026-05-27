package com.yuncode.system.service;

import java.io.File;

/**
 * 应用目录服务接口
 * 负责创建和管理应用目录结构
 *
 * @author Yuncode
 * @since 2025-01-30
 */
public interface ApplicationDirectoryService {

    /**
     * 创建应用目录结构
     * 创建以下目录和文件：
     * - manifest.xml：应用描述文件
     * - pom.xml：Maven构建文件
     * - icon.png：应用图标
     * - lib/：存放应用依赖jar包
     * - repository/：存放数据库表、表单、台账、定时任务、流程等
     * - template/：存放扩展后的controller页面
     * - web/：存放静态资源
     *
     * @param appId 应用ID
     * @param appName 应用名称
     * @param appIcon 应用图标数据（base64或URL）
     * @param appDescription 应用描述
     * @param version 版本号
     * @return 应用目录路径
     */
    String createApplicationDirectory(String appId, String appName, String appIcon,
                                      String appDescription, String version);

    /**
     * 删除应用目录
     *
     * @param appId 应用ID
     * @return 是否删除成功
     */
    boolean deleteApplicationDirectory(String appId);

    /**
     * 获取应用目录路径
     *
     * @param appId 应用ID
     * @return 应用目录路径，如果不存在返回null
     */
    String getApplicationDirectory(String appId);

    /**
     * 检查应用目录是否存在
     *
     * @param appId 应用ID
     * @return 是否存在
     */
    boolean existsApplicationDirectory(String appId);

    /**
     * 移动应用目录（用于卸载/还原）
     *
     * @param appId 应用ID
     * @param fromDir 源子目录（如 install）
     * @param toDir 目标子目录（如 uninstall）
     * @return 是否成功
     */
    boolean moveAppDirectory(String appId, String fromDir, String toDir);

    /**
     * 打包应用为 .sap 分发文件
     * 即时生成，存放在临时 dist 目录，下载后自动清理
     *
     * @param appId 应用ID
     * @param newVersion 新版本号
     * @return 生成的 .sap 文件
     */
    File packageApplication(String appId, String newVersion);

    /**
     * 创建应用当前状态的压缩快照（用于版本还原）
     * 快照包含完整的 install 目录（含 lib/），保存到 history/{appId}/snapshots/
     * 每个应用最多保留 5 个快照，旧的自动清理
     *
     * @param appId 应用ID
     * @param version 当前版本号
     * @return 快照文件
     */
    File snapshotApplication(String appId, String version);

    /**
     * 获取应用打包文件
     *
     * @param appId 应用ID
     * @param fileName 文件名（含.sap后缀）
     * @return .sap 文件
     */
    File getDistributeFile(String appId, String fileName);

    /**
     * 删除分发临时文件（下载完成后调用）
     *
     * @param appId 应用ID
     * @param fileName 文件名
     * @return 是否成功
     */
    boolean deleteDistributeFile(String appId, String fileName);

    /**
     * 更新 manifest.xml 中的版本号
     *
     * @param appId 应用ID
     * @param version 新版本号
     */
    void updateManifestVersion(String appId, String version);

    /**
     * 读取 manifest.xml 中的版本号
     *
     * @param appId 应用ID
     * @return 版本号，读取失败返回 null
     */
    String readManifestVersion(String appId);

    /**
     * 读取暂存区的 manifest.xml 版本号（用于版本校验）
     *
     * @param appId 应用标识
     * @return 版本号，读取失败返回 null
     */
    String readStagingManifestVersion(String appId);

    /**
     * 暂存 .sap 文件到 staging 目录（不安装）
     *
     * @param sapFile .sap 文件
     * @return {appId, appName, version, fileSize}
     */
    java.util.Map<String, String> stageFromSap(File sapFile);

    /**
     * 列出 staging 目录中所有暂存的应用包
     *
     * @return 暂存包信息列表，每项含 {appId, appName, version, fileSize}
     */
    java.util.List<java.util.Map<String, String>> listStagedPackages();

    /**
     * 将暂存包部署到安装目录
     *
     * @param appId 应用标识
     * @return {appId, appName, version}
     */
    java.util.Map<String, String> deployStagedPackage(String appId);

    /**
     * 删除暂存的应用包
     *
     * @param appId 应用标识
     * @return 是否成功
     */
    boolean deleteStagedPackage(String appId);
}
