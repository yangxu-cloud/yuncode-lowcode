package com.yuncode.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuncode.common.model.util.response.Result;
import com.yuncode.common.utils.SecurityUtil;
import com.yuncode.system.annotation.OperLog;
import com.yuncode.system.dto.ApplicationForm;
import com.yuncode.system.dto.DistributeResult;
import com.yuncode.system.entity.SysApplication;
import com.yuncode.system.entity.SysApplicationLog;
import com.yuncode.system.service.ApplicationDirectoryService;
import com.yuncode.system.service.ApplicationService;
import com.yuncode.system.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.util.List;

/**
 * 应用管理控制器
 *
 * @author Yuncode
 * @since 2025-01-30
 */
@Slf4j
@RestController
@RequestMapping("/system/application")
@RequiredArgsConstructor
@Tag(name = "应用管理", description = "应用管理接口")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ApplicationDirectoryService applicationDirectoryService;
    private final CategoryService categoryService;

    /**
     * 分页查询应用列表
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询应用列表", description = "分页查询应用列表，支持按应用ID和名称搜索")
    @OperLog(module = "应用管理", businessType = 1, description = "查询应用列表")
    public Result<IPage<SysApplication>> getApplicationList(
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词（支持应用ID和应用名称）") @RequestParam(required = false) String appName,
            @Parameter(description = "状态筛选") @RequestParam(required = false) Integer status) {

        Long tenantId = SecurityUtil.getTenantId();
        Page<?> page = new Page<>(current, size);
        IPage<SysApplication> result = applicationService.getApplicationPage(page, tenantId, appName, status);
        return Result.success(result);
    }

    /**
     * 获取应用详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取应用详情", description = "根据ID获取应用详情")
    public Result<SysApplication> getApplication(@PathVariable Long id) {
        SysApplication application = applicationService.getApplicationById(id);
        return Result.success(application);
    }

    /**
     * 创建应用
     */
    @PostMapping("/create")
    @Operation(summary = "创建应用", description = "创建新应用")
    @OperLog(module = "应用管理", businessType = 1, description = "创建应用")
    public Result<Void> createApplication(@RequestBody ApplicationForm form) {
        try {
            applicationService.createApplication(form);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新应用
     */
    @PutMapping("/update")
    @Operation(summary = "更新应用", description = "更新应用信息")
    @OperLog(module = "应用管理", businessType = 2, description = "更新应用")
    public Result<Void> updateApplication(@RequestBody ApplicationForm form) {
        try {
            applicationService.updateApplication(form);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除应用
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除应用", description = "删除应用")
    @OperLog(module = "应用管理", businessType = 3, description = "删除应用")
    public Result<Void> deleteApplication(@PathVariable Long id) {
        try {
            applicationService.deleteApplication(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 启动应用
     */
    @PostMapping("/start/{id}")
    @Operation(summary = "启动应用", description = "启动应用")
    @OperLog(module = "应用管理", businessType = 4, description = "启动应用")
    public Result<Void> startApplication(@PathVariable Long id) {
        try {
            applicationService.startApplication(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 停止应用
     */
    @PostMapping("/stop/{id}")
    @Operation(summary = "停止应用", description = "停止应用")
    @OperLog(module = "应用管理", businessType = 5, description = "停止应用")
    public Result<Void> stopApplication(@PathVariable Long id) {
        try {
            applicationService.stopApplication(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 重启应用
     */
    @PostMapping("/restart/{id}")
    @Operation(summary = "重启应用", description = "重启应用（重新加载 JAR）")
    @OperLog(module = "应用管理", businessType = 5, description = "重启应用")
    public Result<Void> restartApplication(@PathVariable Long id) {
        try {
            applicationService.restartApplication(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 还原应用（从已卸载状态恢复）
     */
    @PostMapping("/restore/{id}")
    @Operation(summary = "还原应用", description = "还原已卸载的应用")
    @OperLog(module = "应用管理", businessType = 6, description = "还原应用")
    public Result<Void> restoreApplication(@PathVariable Long id) {
        try {
            applicationService.restoreApplication(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 安装应用
     */
    @PostMapping("/install/{id}")
    @Operation(summary = "安装应用", description = "安装应用")
    @OperLog(module = "应用管理", businessType = 6, description = "安装应用")
    public Result<Void> installApplication(@PathVariable Long id) {
        try {
            applicationService.installApplication(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 卸载应用
     */
    @PostMapping("/uninstall/{id}")
    @Operation(summary = "卸载应用", description = "卸载应用")
    @OperLog(module = "应用管理", businessType = 7, description = "卸载应用")
    public Result<Void> uninstallApplication(@PathVariable Long id) {
        try {
            applicationService.uninstallApplication(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分发应用（打包为 .sap 文件）
     */
    @PostMapping("/distribute/{id}")
    @Operation(summary = "分发应用", description = "打包应用为 .sap 分发文件")
    @OperLog(module = "应用管理", businessType = 8, description = "分发应用")
    public Result<DistributeResult> distributeApplication(
            @PathVariable Long id,
            @Parameter(description = "是否包含应用数据") @RequestParam(defaultValue = "false") boolean includeData) {
        try {
            DistributeResult result = applicationService.distributeApplication(id, includeData);
            return Result.success(result);
        } catch (RuntimeException e) {
            log.error("分发应用失败: id={}", id, e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 下载分发文件
     */
    @GetMapping("/distribute/download")
    @Operation(summary = "下载分发文件", description = "下载已打包的 .sap 分发文件")
    public void downloadDistribution(
            @Parameter(description = "应用标识") @RequestParam String appId,
            @Parameter(description = "文件名（含.sap后缀）") @RequestParam String fileName,
            HttpServletResponse response) {

        File file = applicationDirectoryService.getDistributeFile(appId, fileName);
        if (file == null) {
            response.setStatus(404);
            return;
        }

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8) + "\"");
        response.setContentLengthLong(file.length());

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.flush();
            // 下载成功，清理临时 .sap 文件
            applicationDirectoryService.deleteDistributeFile(appId, fileName);
        } catch (Exception e) {
            log.error("下载分发文件失败: appId={}, fileName={}", appId, fileName, e);
        }
    }

    /**
     * 升级应用
     */
    @PostMapping("/upgrade/{id}")
    @Operation(summary = "升级应用", description = "升级应用")
    @OperLog(module = "应用管理", businessType = 8, description = "升级应用")
    public Result<Void> upgradeApplication(@PathVariable Long id) {
        try {
            applicationService.upgradeApplication(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 上传 .sap 文件到暂存区
     */
    @PostMapping("/deploy/upload")
    @Operation(summary = "上传部署包到暂存区", description = "上传 .sap 文件到暂存区，等待用户确认部署")
    @OperLog(module = "应用管理", businessType = 6, description = "上传部署包")
    public Result<java.util.Map<String, String>> deployUpload(
            @Parameter(description = ".sap 文件") @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            java.util.Map<String, String> result = applicationService.stageApplication(file);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询暂存区应用包列表
     */
    @GetMapping("/deploy/packages")
    @Operation(summary = "查询暂存区包列表", description = "查询所有暂存待部署的应用包")
    public Result<java.util.List<java.util.Map<String, String>>> getStagedPackages() {
        java.util.List<java.util.Map<String, String>> result = applicationService.listStagedApplications();
        return Result.success(result);
    }

    /**
     * 部署暂存区应用包
     */
    @PostMapping("/deploy/install/{appId}")
    @Operation(summary = "部署暂存包", description = "将暂存区的应用包安装到应用目录")
    @OperLog(module = "应用管理", businessType = 6, description = "部署应用")
    public Result<java.util.Map<String, Object>> deployInstall(@PathVariable String appId) {
        try {
            java.util.Map<String, Object> result = applicationService.deployStagedApplication(appId);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除暂存区应用包
     */
    @DeleteMapping("/deploy/packages/{appId}")
    @Operation(summary = "删除暂存包", description = "从暂存区删除应用包")
    @OperLog(module = "应用管理", businessType = 3, description = "删除暂存包")
    public Result<Void> deleteStagedPackage(@PathVariable String appId) {
        try {
            applicationService.deleteStagedApplication(appId);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取应用资源统计
     */
    @GetMapping("/stats/{appId}")
    @Operation(summary = "获取应用资源统计", description = "扫描应用 repository 目录，统计数据库表、表单、流程、视图、图表数量")
    public Result<java.util.Map<String, Integer>> getApplicationStats(
            @PathVariable String appId,
            @Parameter(description = "分类名称（可选，为空时统计全部）") @RequestParam(required = false) String category) {
        java.util.Map<String, Integer> stats = applicationService.getApplicationStats(appId, category);
        return Result.success(stats);
    }

    /**
     * 分页查询应用日志
     */
    @GetMapping("/logs")
    @Operation(summary = "分页查询应用日志", description = "分页查询应用操作日志")
    @OperLog(module = "应用管理", businessType = 1, description = "查询应用日志")
    public Result<IPage<SysApplicationLog>> getApplicationLogs(
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "应用ID") @RequestParam(required = false) Long appId,
            @Parameter(description = "操作类型") @RequestParam(required = false) Integer operationType) {

        Page<?> page = new Page<>(current, size);
        IPage<SysApplicationLog> result = applicationService.getApplicationLogPage(page, appId, operationType);
        return Result.success(result);
    }

    // ==================== 分类管理 ====================

    /**
     * 获取应用的分类树
     */
    @GetMapping("/{appId}/categories")
    @Operation(summary = "获取分类树", description = "获取应用下的分类树形结构")
    public Result<java.util.List<java.util.Map<String, Object>>> getCategoryTree(@PathVariable String appId) {
        return Result.success(categoryService.getCategoryTree(appId));
    }

    /**
     * 创建分类
     */
    @PostMapping("/{appId}/categories")
    @Operation(summary = "创建分类", description = "创建应用分类（支持一级和二级）")
    @OperLog(module = "应用管理", businessType = 1, description = "创建分类")
    public Result<com.yuncode.system.entity.SysAppCategory> createCategory(
            @PathVariable String appId,
            @RequestBody java.util.Map<String, Object> body) {
        try {
            String name = (String) body.get("name");
            Long parentId = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null;
            com.yuncode.system.entity.SysAppCategory category = categoryService.createCategory(appId, name, parentId);
            return Result.success(category);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 重命名分类
     */
    @PutMapping("/{appId}/categories/{id}")
    @Operation(summary = "重命名分类", description = "修改分类名称")
    @OperLog(module = "应用管理", businessType = 2, description = "重命名分类")
    public Result<Void> renameCategory(
            @PathVariable String appId,
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        try {
            categoryService.renameCategory(id, body.get("name"));
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除分类（级联删除子分类）
     */
    @DeleteMapping("/{appId}/categories/{id}")
    @Operation(summary = "删除分类", description = "删除分类及其子分类")
    @OperLog(module = "应用管理", businessType = 3, description = "删除分类")
    public Result<Void> deleteCategory(@PathVariable String appId, @PathVariable Long id) {
        try {
            categoryService.deleteCategory(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}