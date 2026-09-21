package com.yuncode.system.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuncode.common.utils.SecurityUtil;
import com.yuncode.system.adapter.DatabaseAdapterFactory;
import com.yuncode.system.app.dto.ApplicationForm;
import com.yuncode.system.app.dto.DistributeResult;
import com.yuncode.system.app.entity.SysApplication;
import com.yuncode.system.app.entity.SysApplicationLog;
import com.yuncode.system.app.entity.SysBoTable;
import com.yuncode.common.event.AppLifecycleEvent;
import com.yuncode.system.app.mapper.SysApplicationLogMapper;
import com.yuncode.system.app.mapper.SysApplicationMapper;
import com.yuncode.system.app.service.ApplicationLifecycleService;
import com.yuncode.system.app.service.ApplicationDistributionService;
import com.yuncode.system.app.mapper.SysBoTableMapper;
import com.yuncode.system.app.service.ApplicationDirectoryService;
import com.yuncode.system.app.service.ApplicationService;
import com.yuncode.system.app.service.MavenModuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
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
    private final SysBoTableMapper boTableMapper;
    private final ApplicationDirectoryService applicationDirectoryService;
    private final MavenModuleService mavenModuleService;
    private final ApplicationEventPublisher eventPublisher;
    private final DatabaseAdapterFactory databaseAdapterFactory;
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationLifecycleService lifecycleService;
    private final ApplicationDistributionService distributionService;

    @Override
    public long count() {
        try {
            return applicationMapper.selectCount(null);
        } catch (Exception e) {
            log.warn("统计应用数失败: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public long countByStatus(Integer status) {
        try {
            if (status == null) return count();
            return applicationMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysApplication>()
                    .eq(SysApplication::getStatus, status));
        } catch (Exception e) {
            log.warn("按状态统计应用数失败: {}", e.getMessage());
            return 0;
        }
    }

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
    public IPage<SysApplication> getApplicationPage(Page<?> page, Long tenantId, String appName, Integer status) {
        log.info("开始查询应用列表 - tenantId: {}, appName: {}, status: {}", tenantId, appName, status);

        // 手动处理租户过滤（多租户插件已忽略该表）
        LambdaQueryWrapper<SysApplication> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null && tenantId > 0) {
            wrapper.eq(SysApplication::getTenantId, tenantId);
        }
        if (appName != null && !appName.isEmpty()) {
            wrapper.like(SysApplication::getAppName, appName);
        }
        if (status != null) {
            wrapper.eq(SysApplication::getStatus, status);
        }

        IPage<SysApplication> result = applicationMapper.selectPage((Page<SysApplication>) page, wrapper);
        log.info("查询完成 - 总记录数: {}, 当前页数据: {}", result.getTotal(), result.getRecords().size());

        return result;
    }

    @Override
    public SysApplication getApplicationById(Long id) {
        SysApplication app = applicationMapper.selectById(id);
        if (app != null) {
            checkTenantAccess(app);
        }
        return app;
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
        checkTenantAccess(existing);

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
        checkTenantAccess(application);

        String appId = application.getAppId();

        // 物理删除数据库记录（彻底删除，不走逻辑删除避免唯一索引冲突）
        boolean result = applicationMapper.physicalDeleteById(id) > 0;

        if (result) {
            // 先清理该应用下所有 BO 的物理数据库表
            try {
                List<SysBoTable> boTables = boTableMapper.selectByAppId(appId);
                for (SysBoTable bo : boTables) {
                    try {
                        String dropSql = databaseAdapterFactory.getAdapter().buildDropTableSql(bo.getStorageName());
                        jdbcTemplate.execute(dropSql);
                        log.info("BO 物理表已删除: {}", bo.getStorageName());
                    } catch (Exception e) {
                        log.warn("BO 物理表删除失败（可忽略）: {}", bo.getStorageName());
                    }
                }
            } catch (Exception e) {
                log.warn("清理 BO 物理表失败: appId={}", appId, e);
            }
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
    public boolean startApplication(Long id) {
        return lifecycleService.startApplication(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean stopApplication(Long id) {
        return lifecycleService.stopApplication(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean restartApplication(Long id) {
        return lifecycleService.restartApplication(id);
    }

    @Override
    public boolean restoreApplication(Long id) {
        return lifecycleService.restoreApplication(id);
    }

    @Override
    public boolean installApplication(Long id) {
        return lifecycleService.installApplication(id);
    }

    @Override
    public boolean uninstallApplication(Long id) {
        return lifecycleService.uninstallApplication(id);
    }

    @Override
    public DistributeResult distributeApplication(Long id, boolean includeData) {
        return distributionService.distributeApplication(id, includeData);
    }

    @Override
    public Map<String, String> stageApplication(MultipartFile file) {
        return distributionService.stageApplication(file);
    }

    @Override
    public List<Map<String, String>> listStagedApplications() {
        return distributionService.listStagedApplications();
    }

    @Override
    public Map<String, Object> deployStagedApplication(String appId) {
        return distributionService.deployStagedApplication(appId);
    }

    @Override
    public boolean deleteStagedApplication(String appId) {
        return distributionService.deleteStagedApplication(appId);
    }

    @Override
    public boolean upgradeApplication(Long id) {
        return lifecycleService.upgradeApplication(id);
    }

    @Override
    public IPage<SysApplicationLog> getApplicationLogPage(Page<?> page, Long appId, Integer operationType) {
        // 非管理员只能查看自己租户的应用日志
        Long tenantId = SecurityUtil.getTenantIdOrNull();
        if (!SecurityUtil.isPlatformAdmin() && tenantId != null) {
            LambdaQueryWrapper<SysApplicationLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysApplicationLog::getTenantId, tenantId);
            if (appId != null) {
                wrapper.eq(SysApplicationLog::getAppId, appId);
            }
            if (operationType != null) {
                wrapper.eq(SysApplicationLog::getOperationType, operationType);
            }
            wrapper.orderByDesc(SysApplicationLog::getCreateTime);
            return applicationLogMapper.selectPage((IPage<SysApplicationLog>) page, wrapper);
        }
        return applicationLogMapper.selectPage(page, appId, operationType);
    }

    /**
     * 校验租户访问权限：非管理员只能操作自己租户的数据
     */
    private void checkTenantAccess(SysApplication app) {
        if (!SecurityUtil.isPlatformAdmin()) {
            Long tenantId = SecurityUtil.getTenantIdOrNull();
            if (tenantId != null && !tenantId.equals(app.getTenantId())) {
                log.warn("跨租户访问应用被拦截: appId={}, appTenantId={}, userTenantId={}",
                        app.getId(), app.getTenantId(), tenantId);
                throw new RuntimeException("应用不存在");
            }
        }
    }

    /**
     * 从 manifest.xml 读取应用版本号，回退到 DB 中的版本
     */
    private String readAppVersion(String appId, String dbVersion) {
        String version = applicationDirectoryService.readManifestVersion(appId);
        if (version == null) {
            if (dbVersion != null && !dbVersion.isEmpty()) {
                log.warn("manifest.xml版本读取失败，使用DB版本: appId={}, dbVersion={}", appId, dbVersion);
                return dbVersion;
            }
            log.warn("manifest.xml与DB均无版本号，使用默认版本: appId={}", appId);
            return "1.0.0";
        }
        return version;
    }

    /**
     * 小版本自动升级（1.0.0 → 1.1.0）
     */
    private String bumpMinorVersion(String version) {
        if (version == null || version.isEmpty()) {
            return "1.0.0";
        }
        String[] parts = version.split("\\.");
        try {
            int major = parts.length > 0 ? Integer.parseInt(parts[0]) : 1;
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return major + "." + (minor + 1) + ".0";
        } catch (NumberFormatException e) {
            log.warn("版本号解析失败: {}, 使用默认版本 1.0.0", version);
            return "1.0.0";
        }
    }

    /**
     * 比较版本号（支持 x.y.z 格式）
     * @return 负数=v1&lt;v2, 0=v1==v2, 正数=v1&gt;v2
     */
    private int compareVersion(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int maxLen = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLen; i++) {
            int n1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int n2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (n1 != n2) return n1 - n2;
        }
        return 0;
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

    @Override
    public Map<String, Integer> getApplicationStats(String appId, String category) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("tables", 0);
        stats.put("forms", 0);
        stats.put("workflows", 0);
        stats.put("views", 0);
        stats.put("charts", 0);

        // BO 表统计：从数据库查询，更准确
        try {
            long tableCount;
            if (category != null && !category.isEmpty()) {
                tableCount = boTableMapper.selectCountByAppIdAndCategory(appId, category);
            } else {
                tableCount = boTableMapper.selectCountByAppId(appId);
            }
            stats.put("tables", (int) tableCount);
        } catch (Exception e) {
            log.warn("查询 BO 表数量失败: {}", e.getMessage());
        }

        String appDir = applicationDirectoryService.getApplicationDirectory(appId);
        if (appDir == null) {
            return stats;
        }

        // 其他资源（表单、流程、视图、图表）仍从 repository/ 目录统计
        File repository = new File(appDir, "repository");
        if (!repository.exists() || !repository.isDirectory()) {
            return stats;
        }

        File[] subDirs = repository.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File sub : subDirs) {
                String dirName = sub.getName().toLowerCase();
                String statKey = switch (dirName) {
                    case "forms", "form" -> "forms";
                    case "workflows", "workflow", "process", "bpm" -> "workflows";
                    case "views", "view", "report", "reports" -> "views";
                    case "charts", "chart" -> "charts";
                    default -> null;
                };
                if (statKey == null) continue;

                int count;
                if (category != null && !category.isEmpty()) {
                    File catDir = findSubDirectoryIgnoreCase(sub, category);
                    count = catDir != null ? countFilesRecursive(catDir) : 0;
                } else {
                    count = countFilesRecursive(sub);
                }
                stats.put(statKey, stats.get(statKey) + count);
            }
        }

        return stats;
    }

    /**
     * 在目录下递归查找名称匹配的子目录（忽略大小写）
     * 支持查找任意深度的分类目录，如 "基础数据" 可匹配 tables/订单管理/基础数据/
     */
    private File findSubDirectoryIgnoreCase(File parent, String name) {
        File[] children = parent.listFiles(File::isDirectory);
        if (children != null) {
            for (File child : children) {
                if (child.getName().equalsIgnoreCase(name)) {
                    return child;
                }
            }
            // 未在直接子目录中找到，递归查找更深层
            for (File child : children) {
                File found = findSubDirectoryIgnoreCase(child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * 递归统计目录下的文件数
     */
    private int countFilesRecursive(File dir) {
        int count = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    count++;
                } else if (f.isDirectory()) {
                    count += countFilesRecursive(f);
                }
            }
        }
        return count;
    }
}