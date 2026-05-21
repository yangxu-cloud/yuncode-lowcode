package com.yuncode.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuncode.system.dto.ApplicationForm;
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
     * @return 应用分页列表
     */
    IPage<SysApplication> getApplicationPage(Page<?> page, Long tenantId, String appName);

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
}