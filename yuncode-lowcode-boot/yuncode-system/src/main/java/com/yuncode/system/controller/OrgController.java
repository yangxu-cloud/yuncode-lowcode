package com.yuncode.system.controller;

import com.yuncode.common.model.util.response.Result;
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
    public Result<List<OrgTreeNode>> getOrgTree(
            @RequestHeader(value = "tenantId", defaultValue = "0") Long tenantId) {
        log.info("获取组织树, tenantId={}", tenantId);
        List<OrgTreeNode> tree = orgService.getOrgTree(tenantId);
        return Result.success(tree);
    }

    /**
     * 获取组织列表
     *
     * @param queryDTO 查询条件
     * @param tenantId 租户ID
     * @return 组织列表
     */
    @PostMapping("/list")
    @Operation(summary = "获取组织列表", description = "根据条件查询组织列表")
    public Result<List<OrgVO>> getOrgList(
            @RequestBody OrgQueryDTO queryDTO,
            @RequestHeader(value = "tenantId", defaultValue = "0") Long tenantId) {
        log.info("获取组织列表, queryDTO={}, tenantId={}", queryDTO, tenantId);
        List<OrgVO> list = orgService.getOrgList(queryDTO, tenantId);
        return Result.success(list);
    }

    /**
     * 获取组织详情
     *
     * @param id 组织ID
     * @param tenantId 租户ID
     * @return 组织详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取组织详情", description = "根据ID获取组织详细信息")
    public Result<OrgVO> getOrgById(
            @Parameter(description = "组织ID") @PathVariable Long id,
            @RequestHeader(value = "tenantId", defaultValue = "0") Long tenantId) {
        log.info("获取组织详情, id={}, tenantId={}", id, tenantId);
        OrgVO org = orgService.getOrgById(id, tenantId);
        if (org == null) {
            return Result.error("组织不存在");
        }
        return Result.success(org);
    }

    /**
     * 添加组织
     *
     * @param org 组织实体
     * @return 是否成功
     */
    @PostMapping
    @Operation(summary = "添加组织", description = "添加新的组织")
    public Result<Void> addOrg(@RequestBody SysOrg org) {
        log.info("添加组织, org={}", org);
        try {
            orgService.addOrg(org);
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
    public Result<Void> updateOrg(@RequestBody SysOrg org) {
        log.info("更新组织, org={}", org);
        try {
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
     * @param tenantId 租户ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除组织", description = "删除指定的组织")
    public Result<Void> deleteOrg(
            @Parameter(description = "组织ID") @PathVariable Long id,
            @RequestHeader(value = "tenantId", defaultValue = "0") Long tenantId) {
        log.info("删除组织, id={}, tenantId={}", id, tenantId);
        try {
            orgService.deleteOrg(id, tenantId);
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
     * @param tenantId 租户ID
     * @return 组织列表
     */
    @GetMapping("/search")
    @Operation(summary = "搜索组织", description = "根据关键词搜索组织")
    public Result<List<OrgVO>> searchOrgs(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @RequestHeader(value = "tenantId", defaultValue = "0") Long tenantId) {
        log.info("搜索组织, keyword={}, tenantId={}", keyword, tenantId);
        List<OrgVO> list = orgService.searchOrgs(keyword, tenantId);
        return Result.success(list);
    }

    /**
     * 检查组织编码是否存在
     *
     * @param orgCode 组织编码
     * @param tenantId 租户ID
     * @param excludeId 排除的ID
     * @return 是否存在
     */
    @GetMapping("/check-code")
    @Operation(summary = "检查组织编码", description = "检查组织编码是否已存在")
    public Result<Boolean> checkOrgCodeExists(
            @Parameter(description = "组织编码") @RequestParam String orgCode,
            @RequestHeader(value = "tenantId", defaultValue = "0") Long tenantId,
            @Parameter(description = "排除的ID") @RequestParam(required = false) Long excludeId) {
        log.info("检查组织编码, orgCode={}, tenantId={}, excludeId={}", orgCode, tenantId, excludeId);
        boolean exists = orgService.checkOrgCodeExists(orgCode, tenantId, excludeId);
        return Result.success(exists);
    }
}
