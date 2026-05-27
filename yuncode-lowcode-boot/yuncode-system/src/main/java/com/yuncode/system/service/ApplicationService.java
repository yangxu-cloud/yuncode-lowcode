package com.yuncode.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuncode.system.dto.ApplicationForm;
import com.yuncode.system.dto.DistributeResult;
import com.yuncode.system.entity.SysApplication;
import com.yuncode.system.entity.SysApplicationLog;

import java.util.List;

/**
 * 应用服务接口
 *
 * @author Yuncode
 * @since 2025-01-30
 */
public interface ApplicationService {

    /**
     * 分页查询应用列表
     *
     * @param page 分页参数
     * @param tenantId 租户ID
     * @param appName 应用名称（可选）
     * @param status 状态筛选（可选）
     * @return 应用分页列表
     */
    IPage<SysApplication> getApplicationPage(Page<?> page, Long tenantId, String appName, Integer status);

    /**
     * 获取应用详情
     *
     * @param id 应用ID
     * @return 应用实体
     */
    SysApplication getApplicationById(Long id);

    /**
     * 创建应用
     *
     * @param form 应用表单
     * @return 是否成功
     */
    boolean createApplication(ApplicationForm form);

    /**
     * 更新应用
     *
     * @param form 应用表单
     * @return 是否成功
     */
    boolean updateApplication(ApplicationForm form);

    /**
     * 删除应用
     *
     * @param id 应用ID
     * @return 是否成功
     */
    boolean deleteApplication(Long id);

    /**
     * 启动应用
     *
     * @param id 应用ID
     * @return 是否成功
     */
    boolean startApplication(Long id);

    /**
     * 停止应用
     *
     * @param id 应用ID
     * @return 是否成功
     */
    boolean stopApplication(Long id);

    /**
     * 安装应用
     *
     * @param id 应用ID
     * @return 是否成功
     */
    boolean installApplication(Long id);

    /**
     * 卸载应用
     *
     * @param id 应用ID
     * @return 是否成功
     */
    boolean uninstallApplication(Long id);

    /**
     * 重启应用
     *
     * @param id 应用ID
     * @return 是否成功
     */
    boolean restartApplication(Long id);

    /**
     * 还原应用（从已卸载状态恢复）
     *
     * @param id 应用ID
     * @return 是否成功
     */
    boolean restoreApplication(Long id);

    /**
     * 分发应用（打包为 .sap 文件）
     *
     * @param id 应用ID
     * @param includeData 是否包含应用数据
     * @return 分发结果
     */
    DistributeResult distributeApplication(Long id, boolean includeData);

    /**
     * 升级应用
     *
     * @param id 应用ID
     * @return 是否成功
     */
    boolean upgradeApplication(Long id);

    /**
     * 分页查询应用日志
     *
     * @param page 分页参数
     * @param appId 应用ID（可选）
     * @param operationType 操作类型（可选）
     * @return 日志分页列表
     */
    IPage<SysApplicationLog> getApplicationLogPage(Page<?> page, Long appId, Integer operationType);

    /**
     * 暂存 .sap 文件（不安装）
     *
     * @param file .sap 文件
     * @return 暂存包信息
     */
    java.util.Map<String, String> stageApplication(org.springframework.web.multipart.MultipartFile file);

    /**
     * 列出所有暂存的应用包
     *
     * @return 暂存包列表
     */
    java.util.List<java.util.Map<String, String>> listStagedApplications();

    /**
     * 部署暂存的应用包
     *
     * @param appId 应用标识
     * @return 部署结果
     */
    java.util.Map<String, Object> deployStagedApplication(String appId);

    /**
     * 删除暂存的应用包
     *
     * @param appId 应用标识
     * @return 是否成功
     */
    boolean deleteStagedApplication(String appId);

    /**
     * 获取应用资源统计
     * 扫描应用的 repository/ 目录，统计各类资源数量
     *
     * @param appId 应用标识
     * @param category 分类名称（可选，为空时统计全部）
     * @return 统计数据 {tables, forms, workflows, views, charts}
     */
    java.util.Map<String, Integer> getApplicationStats(String appId, String category);
}