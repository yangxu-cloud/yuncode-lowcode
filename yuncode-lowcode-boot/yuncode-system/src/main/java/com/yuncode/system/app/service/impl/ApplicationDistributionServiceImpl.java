package com.yuncode.system.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuncode.common.utils.SecurityUtil;
import com.yuncode.system.app.dto.DistributeResult;
import com.yuncode.system.app.entity.SysApplication;
import com.yuncode.system.app.mapper.SysApplicationMapper;
import com.yuncode.system.app.service.ApplicationDirectoryService;
import com.yuncode.system.app.service.ApplicationDistributionService;
import com.yuncode.system.app.service.MavenModuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 应用分发/部署服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationDistributionServiceImpl implements ApplicationDistributionService {

    private final SysApplicationMapper applicationMapper;
    private final ApplicationDirectoryService applicationDirectoryService;
    private final MavenModuleService mavenModuleService;

    @Override
    public DistributeResult distributeApplication(Long id, boolean includeData) {
        SysApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new RuntimeException("应用不存在");
        }
        // 实际分发逻辑（简化）
        DistributeResult result = new DistributeResult();
        result.setId(id);
        result.setAppId(application.getAppId());
        result.setAppName(application.getAppName());
        result.setNewVersion(application.getVersion());
        return result;
    }

    @Override
    public Map<String, String> stageApplication(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("上传文件为空");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.endsWith(".sap")) {
            throw new RuntimeException("仅支持 .sap 格式文件");
        }

        File tempSap = null;
        try {
            tempSap = File.createTempFile("yuncode-upload-", ".sap");
            file.transferTo(tempSap);
            return applicationDirectoryService.stageFromSap(tempSap);
        } catch (Exception e) {
            log.error("暂存.sap失败", e);
            throw new RuntimeException("暂存失败: " + e.getMessage());
        } finally {
            if (tempSap != null && tempSap.exists()) {
                tempSap.delete();
            }
        }
    }

    @Override
    public List<Map<String, String>> listStagedApplications() {
        return applicationDirectoryService.listStagedPackages();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> deployStagedApplication(String appId) {
        Long tenantId = SecurityUtil.getTenantId();

        // 读取版本信息
        String installedVersion = applicationDirectoryService.readManifestVersion(appId);
        String stagedVersion = applicationDirectoryService.readStagingManifestVersion(appId);
        if (stagedVersion == null) {
            throw new RuntimeException("部署失败：无法读取暂存包版本号");
        }

        // 版本校验
        if (installedVersion != null && !installedVersion.isEmpty()) {
            int cmp = compareVersion(stagedVersion, installedVersion);
            if (cmp <= 0) {
                throw new RuntimeException("部署失败：应用版本必须高于当前系统版本");
            }
        }

        // 部署
        Map<String, String> info = applicationDirectoryService.deployStagedPackage(appId);
        String appName = info.get("appName");
        String version = info.get("version");

        // 检查是否已存在
        LambdaQueryWrapper<SysApplication> existQuery = new LambdaQueryWrapper<>();
        existQuery.eq(SysApplication::getAppId, appId);
        if (tenantId != null && tenantId > 0) {
            existQuery.eq(SysApplication::getTenantId, tenantId);
        }
        SysApplication existing = applicationMapper.selectOne(existQuery);

        if (existing != null) {
            existing.setAppName(appName);
            existing.setVersion(version);
            existing.setStatus(2);
            existing.setDeleted(0);
            applicationMapper.updateById(existing);
            Map<String, Object> result = new HashMap<>();
            result.put("id", existing.getId());
            result.put("appId", appId);
            result.put("appName", appName);
            result.put("version", version);
            return result;
        }

        // 创建新记录
        SysApplication application = new SysApplication();
        application.setAppId(appId);
        application.setAppName(appName);
        application.setVersion(version);
        application.setStatus(2);
        application.setTenantId(tenantId);
        application.setDeleted(0);
        applicationMapper.insert(application);

        try {
            mavenModuleService.registerModule(appId);
        } catch (Exception e) {
            log.warn("注册 Maven 模块失败: {}", appId, e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", application.getId());
        result.put("appId", appId);
        result.put("appName", appName);
        result.put("version", version);
        return result;
    }

    @Override
    public boolean deleteStagedApplication(String appId) {
        return applicationDirectoryService.deleteStagedPackage(appId);
    }

    private int compareVersion(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (p1 != p2) return p1 - p2;
        }
        return 0;
    }
}
