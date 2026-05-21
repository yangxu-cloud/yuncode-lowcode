package com.yuncode.system.service;

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
}
