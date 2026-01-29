package com.yuncode.system.controller;

import com.yuncode.common.model.util.response.Result;
import com.yuncode.common.util.security.SecurityUtil;
import com.yuncode.system.annotation.OperLog;
import com.yuncode.system.dto.OrgCreateDTO;
import com.yuncode.system.dto.OrgQueryDTO;
import com.yuncode.system.entity.SysOrg;
import com.yuncode.system.service.OrgService;
import com.yuncode.system.vo.OrgTreeNode;
import com.yuncode.system.vo.OrgVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组织管理控制器
 *
 * @author Yuncode
 * @since 2025-01-22
 */
@Slf4j
@RestController
@RequestMapping("/org")
@RequiredArgsConstructor
@Tag(name = "组织管理", description = "组织管理相关接口")
public class OrgController {

    private final OrgService orgService;

    /**
     * 获取组织树（包含人员）
     *
     * @return 组织树节点列表
     */
    @GetMapping("/tree")
    @Operation(summary = "获取组织树", description = "获取组织树结构，包含组织下的人员信息")
    public Result<List<OrgTreeNode>> getOrgTree() {
        log.info("获取组织树");
        // 多租户插件会自动添加 tenant_id 条件
        List<OrgTreeNode> tree = orgService.getOrgTree();
        return Result.success(tree);
    }

    /**
     * 获取组织列表
     *
     * @param queryDTO 查询条件
     * @return 组织列表
     */
    @PostMapping("/list")
    @Operation(summary = "获取组织列表", description = "根据条件查询组织列表")
    public Result<List<OrgVO>> getOrgList(@RequestBody OrgQueryDTO queryDTO) {
        log.info("获取组织列表, queryDTO={}", queryDTO);
        // 多租户插件会自动添加 tenant_id 条件
        List<OrgVO> list = orgService.getOrgList(queryDTO);
        return Result.success(list);
    }

    /**
     * 获取组织详情
     *
     * @param id 组织ID
     * @return 组织详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取组织详情", description = "根据ID获取组织详细信息")
    public Result<OrgVO> getOrgById(@Parameter(description = "组织ID") @PathVariable Long id) {
        log.info("获取组织详情, id={}", id);
        // 多租户插件会自动添加 tenant_id 条件
        OrgVO org = orgService.getOrgById(id);
        if (org == null) {
            return Result.error("组织不存在");
        }
        return Result.success(org);
    }

    /**
     * 添加组织
     *
     * @param dto 组织创建DTO（包含组织信息和租户配置）
     * @return 是否成功
     */
    @PostMapping
    @Operation(summary = "添加组织", description = "添加新的组织，公司节点会同步创建租户")
    @OperLog(module = "组织管理", businessType = 1, description = "添加组织")
    public Result<Void> addOrg(@RequestBody OrgCreateDTO dto) {
        log.info("添加组织, dto={}", dto);
        try {
            // 如果是公司节点（orgType=1），检查是否有平台管理员权限
            if (dto.getOrgType() != null && dto.getOrgType() == 1) {
                SecurityUtil.checkPlatformAdmin();
                log.info("创建公司（租户）: tenantCode={}, orgName={}", dto.getTenantCode(), dto.getOrgName());
            }

            // 多租户插件和 MetaObjectHandler 会自动处理 tenant_id
            orgService.addOrg(dto, dto.getTenantConfig());
            return Result.success();
        } catch (RuntimeException e) {
            log.error("添加组织失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新组织
     *
     * @param org 组织实体
     * @return 是否成功
     */
    @PutMapping
    @Operation(summary = "更新组织", description = "更新组织信息")
    @OperLog(module = "组织管理", businessType = 2, description = "更新组织")
    public Result<Void> updateOrg(@RequestBody SysOrg org) {
        log.info("更新组织, org={}", org);
        try {
            // 多租户插件会自动添加 tenant_id 条件进行校验
            orgService.updateOrg(org);
            return Result.success();
        } catch (RuntimeException e) {
            log.error("更新组织失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除组织
     *
     * @param id 组织ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除组织", description = "删除指定的组织")
    @OperLog(module = "组织管理", businessType = 3, description = "删除组织")
    public Result<Void> deleteOrg(@Parameter(description = "组织ID") @PathVariable Long id) {
        log.info("删除组织, id={}", id);
        try {
            // 多租户插件会自动添加 tenant_id 条件进行校验
            orgService.deleteOrg(id);
            return Result.success();
        } catch (RuntimeException e) {
            log.error("删除组织失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 搜索组织
     *
     * @param keyword 关键词
     * @return 组织列表
     */
    @GetMapping("/search")
    @Operation(summary = "搜索组织", description = "根据关键词搜索组织")
    public Result<List<OrgVO>> searchOrgs(@Parameter(description = "搜索关键词") @RequestParam String keyword) {
        log.info("搜索组织, keyword={}", keyword);
        // 多租户插件会自动添加 tenant_id 条件
        List<OrgVO> list = orgService.searchOrgs(keyword);
        return Result.success(list);
    }

    /**
     * 检查组织编码是否存在
     *
     * @param orgCode 组织编码
     * @param excludeId 排除的ID
     * @return 是否存在
     */
    @GetMapping("/check-code")
    @Operation(summary = "检查组织编码", description = "检查组织编码是否已存在")
    public Result<Boolean> checkOrgCodeExists(
            @Parameter(description = "组织编码") @RequestParam String orgCode,
            @Parameter(description = "排除的ID") @RequestParam(required = false) Long excludeId) {
        log.info("检查组织编码, orgCode={}, excludeId={}", orgCode, excludeId);
        // 多租户插件会自动添加 tenant_id 条件
        boolean exists = orgService.checkOrgCodeExists(orgCode, excludeId);
        return Result.success(exists);
    }

    /**
     * 添加人员到组织
     *
     * @param orgId 组织ID
     * @param userId 用户ID
     * @param isLeader 是否负责人
     * @param isMainDept 是否主部门
     * @return 是否成功
     */
    @PostMapping("/add-user")
    @Operation(summary = "添加人员到组织", description = "将用户添加到指定组织")
    @OperLog(module = "组织管理", businessType = 1, description = "添加人员到组织")
    public Result<Void> addUserToOrg(
            @Parameter(description = "组织ID") @RequestParam Long orgId,
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "是否负责人") @RequestParam(required = false, defaultValue = "0") Integer isLeader,
            @Parameter(description = "是否主部门") @RequestParam(required = false, defaultValue = "0") Integer isMainDept) {
        log.info("添加人员到组织, orgId={}, userId={}, isLeader={}, isMainDept={}",
                orgId, userId, isLeader, isMainDept);
        try {
            // 多租户插件会自动添加 tenant_id 条件进行校验
            orgService.addUserToOrg(orgId, userId, isLeader, isMainDept);
            return Result.success();
        } catch (RuntimeException e) {
            log.error("添加人员到组织失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 从组织移除人员
     *
     * @param orgId 组织ID
     * @param userId 用户ID
     * @return 是否成功
     */
    @DeleteMapping("/remove-user")
    @Operation(summary = "从组织移除人员", description = "将用户从指定组织移除")
    @OperLog(module = "组织管理", businessType = 3, description = "从组织移除人员")
    public Result<Void> removeUserFromOrg(
            @Parameter(description = "组织ID") @RequestParam Long orgId,
            @Parameter(description = "用户ID") @RequestParam Long userId) {
        log.info("从组织移除人员, orgId={}, userId={}", orgId, userId);
        try {
            // 多租户插件会自动添加 tenant_id 条件进行校验
            orgService.removeUserFromOrg(orgId, userId);
            return Result.success();
        } catch (RuntimeException e) {
            log.error("从组织移除人员失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 设置用户为负责人
     *
     * @param orgId 组织ID
     * @param userId 用户ID
     * @param isLeader 是否负责人
     * @return 是否成功
     */
    @PutMapping("/set-leader")
    @Operation(summary = "设置用户为负责人", description = "设置或取消用户的负责人身份")
    @OperLog(module = "组织管理", businessType = 2, description = "设置用户为负责人")
    public Result<Void> setUserAsLeader(
            @Parameter(description = "组织ID") @RequestParam Long orgId,
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "是否负责人") @RequestParam Integer isLeader) {
        log.info("设置用户为负责人, orgId={}, userId={}, isLeader={}", orgId, userId, isLeader);
        try {
            // 多租户插件会自动添加 tenant_id 条件进行校验
            orgService.setUserAsLeader(orgId, userId, isLeader);
            return Result.success();
        } catch (RuntimeException e) {
            log.error("设置用户为负责人失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户的所有组织关系（包括主部门和兼职部门）
     *
     * @param userId 用户ID
     * @return 用户组织关系列表
     */
    @GetMapping("/user-orgs/{userId}")
    @Operation(summary = "获取用户组织关系", description = "获取用户的所有组织关系，包括主部门和兼职部门")
    public Result<List<com.yuncode.system.vo.UserOrgVO>> getUserOrgs(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        log.info("获取用户组织关系, userId={}", userId);
        try {
            List<com.yuncode.system.vo.UserOrgVO> userOrgs = orgService.getUserOrgs(userId);
            return Result.success(userOrgs);
        } catch (RuntimeException e) {
            log.error("获取用户组织关系失败", e);
            return Result.error(e.getMessage());
        }
    }
}
