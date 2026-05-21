package com.yuncode.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuncode.common.utils.SecurityUtil;
import com.yuncode.system.dto.ApplicationForm;
import com.yuncode.system.entity.SysApplication;
import com.yuncode.system.entity.SysApplicationLog;
import com.yuncode.system.mapper.SysApplicationLogMapper;
import com.yuncode.system.mapper.SysApplicationMapper;
import com.yuncode.system.service.ApplicationDirectoryService;
import com.yuncode.system.service.ApplicationService;
import com.yuncode.system.service.MavenModuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 应用服务实现类
 *
 * @author Yuncode
 * @since 2025-01-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final SysApplicationMapper applicationMapper;
    private final SysApplicationLogMapper applicationLogMapper;
    private final ApplicationDirectoryService applicationDirectoryService;
    private final MavenModuleService mavenModuleService;

    /**
     * 从图标对象中提取图标名称
     * 支持字符串格式直接返回，对象格式返回图标名称
     *
     * @param icon 图标对象（可能是字符串或对象）
     * @return 图标名称
     */
    private String extractIconName(Object icon) {
        if (icon == null) {
            return "Box"; // 默认图标
        }
        if (icon instanceof String) {
            return (String) icon;
        }
        // 对象格式 {icon, color}，只返回icon字段
        if (icon instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> iconMap = (java.util.Map<String, Object>) icon;
            Object iconField = iconMap.get("icon");
            return iconField != null ? iconField.toString() : "Box";
        }
        return "Box";
    }

    @Override
    public IPage<SysApplication> getApplicationPage(Page<?> page, Long tenantId, String appName) {
        log.info("开始查询应用列表 - tenantId: {}, appName: {}", tenantId, appName);

        // 手动处理租户过滤（多租户插件已忽略该表）
        LambdaQueryWrapper<SysApplication> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null && tenantId > 0) {
            wrapper.eq(SysApplication::getTenantId, tenantId);
        }
        if (appName != null && !appName.isEmpty()) {
            wrapper.like(SysApplication::getAppName, appName);
        }

        IPage<SysApplication> result = applicationMapper.selectPage((Page<SysApplication>) page, wrapper);
        log.info("查询完成 - 总记录数: {}, 当前页数据: {}", result.getTotal(), result.getRecords().size());

        return result;
    }

    @Override
    public SysApplication getApplicationById(Long id) {
        return applicationMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createApplication(ApplicationForm form) {
        log.info("开始创建应用 - appId: {}, appName: {}, 当前租户ID: {}", form.getAppId(), form.getAppName(), SecurityUtil.getTenantId());

        // 验证应用ID唯一性
        if (applicationMapper.existsByAppId(form.getAppId(), SecurityUtil.getTenantId())) {
            throw new RuntimeException("应用ID已存在");
        }

        SysApplication application = new SysApplication();
        BeanUtils.copyProperties(form, application);
        application.setStatus(0); // 默认状态：未运行
        application.setTenantId(SecurityUtil.getTenantId());

        // 插入应用记录
        boolean result = applicationMapper.insert(application) > 0;
        log.info("应用创建完成 - ID: {}, result: {}", application.getId(), result);

        if (result) {
            // 创建应用目录结构
            try {
                String appPath = applicationDirectoryService.createApplicationDirectory(
                    form.getAppId(),
                    form.getAppName(),
                    extractIconName(form.getAppIcon()),
                    form.getAppDescription(),
                    form.getVersion() != null ? form.getVersion() : "1.0.0"
                );
                log.info("应用创建成功，目录路径: {}", appPath);
            } catch (Exception e) {
                log.error("创建应用目录失败", e);
                // 目录创建失败不影响应用记录的创建
            }

            // 注册 Maven 子模块（Dev 模式），失败不影响主流程
            try {
                mavenModuleService.registerModule(form.getAppId());
            } catch (Exception e) {
                log.warn("注册 Maven 模块失败（可忽略）: appId={}", form.getAppId(), e);
            }
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateApplication(ApplicationForm form) {
        if (form.getId() == null) {
            throw new RuntimeException("应用ID不能为空");
        }

        SysApplication existing = applicationMapper.selectById(form.getId());
        if (existing == null) {
            throw new RuntimeException("应用不存在");
        }

        // 如果修改了应用ID，检查唯一性
        if (form.getAppId() != null && !form.getAppId().equals(existing.getAppId())) {
            if (applicationMapper.existsByAppId(form.getAppId(), SecurityUtil.getTenantId())) {
                throw new RuntimeException("应用ID已存在");
            }
        }

        SysApplication application = new SysApplication();
        BeanUtils.copyProperties(form, application);
        application.setTenantId(existing.getTenantId()); // 保持原有租户ID
        application.setStatus(existing.getStatus()); // 保持原有状态

        return applicationMapper.updateById(application) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteApplication(Long id) {
        SysApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new RuntimeException("应用不存在");
        }

        String appId = application.getAppId();

        // 删除数据库记录
        boolean result = applicationMapper.deleteById(id) > 0;

        if (result) {
            // 删除应用目录
            try {
                applicationDirectoryService.deleteApplicationDirectory(appId);
                log.info("应用目录删除成功: appId={}", appId);
            } catch (Exception e) {
                log.error("删除应用目录失败: appId={}", appId, e);
            }

            // 注销 Maven 子模块（Dev 模式），失败不影响主流程
            try {
                mavenModuleService.unregisterModule(appId);
            } catch (Exception e) {
                log.warn("注销 Maven 模块失败（可忽略）: appId={}", appId, e);
            }
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean startApplication(Long id) {
        SysApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new RuntimeException("应用不存在");
        }

        if (application.getStatus() != null && application.getStatus() == 1) {
            throw new RuntimeException("应用已在运行中");
        }

        application.setStatus(1);
        application.setStartTime(LocalDateTime.now());
        application.setStopTime(null);

        return applicationMapper.updateById(application) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean stopApplication(Long id) {
        SysApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new RuntimeException("应用不存在");
        }

        if (application.getStatus() == null || application.getStatus() == 0 || application.getStatus() == 2) {
            throw new RuntimeException("应用未运行，无法停止");
        }

        application.setStatus(2);
        application.setStopTime(LocalDateTime.now());

        return applicationMapper.updateById(application) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean installApplication(Long id) {
        SysApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new RuntimeException("应用不存在");
        }

        // 记录日志
        saveApplicationLog(id, 0, "安装应用", 0, null);

        // 注册 Maven 子模块（Dev 模式），失败不影响主流程
        try {
            mavenModuleService.registerModule(application.getAppId());
        } catch (Exception e) {
            log.warn("注册 Maven 模块失败（可忽略）: appId={}", application.getAppId(), e);
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean uninstallApplication(Long id) {
        SysApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new RuntimeException("应用不存在");
        }

        // 记录日志
        saveApplicationLog(id, 3, "卸载应用", 0, null);

        // 注销 Maven 子模块（Dev 模式），失败不影响主流程
        try {
            mavenModuleService.unregisterModule(application.getAppId());
        } catch (Exception e) {
            log.warn("注销 Maven 模块失败（可忽略）: appId={}", application.getAppId(), e);
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean upgradeApplication(Long id) {
        SysApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new RuntimeException("应用不存在");
        }

        // 记录日志
        saveApplicationLog(id, 4, "升级应用", 0, null);

        // 实际升级逻辑（这里简化为成功）
        return true;
    }

    @Override
    public IPage<SysApplicationLog> getApplicationLogPage(Page<?> page, Long appId, Integer operationType) {
        return applicationLogMapper.selectPage(page, appId, operationType);
    }

    /**
     * 保存应用日志
     *
     * @param appId     应用ID
     * @param operationType 操作类型
     * @param content     操作内容
     * @param status      状态
     * @param errorMessage 错误信息
     */
    private void saveApplicationLog(Long appId, Integer operationType, String content,
                                 Integer status, String errorMessage) {
        SysApplicationLog log = new SysApplicationLog();
        log.setAppId(appId);
        log.setOperationType(operationType);
        log.setOperationContent(content);
        log.setStatus(status != null ? status : 0);
        log.setErrorMessage(errorMessage);
        log.setTenantId(SecurityUtil.getTenantId());

        applicationLogMapper.insert(log);
    }
}