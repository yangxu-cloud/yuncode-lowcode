package com.yuncode.system.controller;

import com.yuncode.common.model.util.response.Result;
import com.yuncode.system.dto.CompanyQueryDTO;
import com.yuncode.system.entity.SysCompany;
import com.yuncode.system.service.CompanyService;
import com.yuncode.system.vo.CompanyVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公司管理控制器
 *
 * @author Yuncode
 * @since 2025-01-22
 */
@Slf4j
@RestController
@RequestMapping("/company")
@RequiredArgsConstructor
@Tag(name = "公司管理", description = "公司管理相关接口")
public class CompanyController {

    private final CompanyService companyService;

    /**
     * 获取公司列表
     *
     * @param queryDTO 查询条件
     * @param tenantId 租户ID
     * @return 公司列表
     */
    @PostMapping("/list")
    @Operation(summary = "获取公司列表", description = "根据条件查询公司列表")
    public Result<List<CompanyVO>> getCompanyList(
            @RequestBody CompanyQueryDTO queryDTO,
            @RequestHeader(value = "tenantId", defaultValue = "0") Long tenantId) {
        log.info("获取公司列表, queryDTO={}, tenantId={}", queryDTO, tenantId);
        List<CompanyVO> list = companyService.getCompanyList(queryDTO, tenantId);
        return Result.success(list);
    }

    /**
     * 获取公司详情
     *
     * @param id 公司ID
     * @param tenantId 租户ID
     * @return 公司详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取公司详情", description = "根据ID获取公司详细信息")
    public Result<CompanyVO> getCompanyById(
            @Parameter(description = "公司ID") @PathVariable Long id,
            @RequestHeader(value = "tenantId", defaultValue = "0") Long tenantId) {
        log.info("获取公司详情, id={}, tenantId={}", id, tenantId);
        CompanyVO company = companyService.getCompanyById(id, tenantId);
        if (company == null) {
            return Result.error("公司不存在");
        }
        return Result.success(company);
    }

    /**
     * 添加公司
     *
     * @param company 公司实体
     * @return 是否成功
     */
    @PostMapping
    @Operation(summary = "添加公司", description = "添加新的公司")
    public Result<Void> addCompany(@RequestBody SysCompany company) {
        log.info("添加公司, company={}", company);
        try {
            companyService.addCompany(company);
            return Result.success();
        } catch (RuntimeException e) {
            log.error("添加公司失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新公司
     *
     * @param company 公司实体
     * @return 是否成功
     */
    @PutMapping
    @Operation(summary = "更新公司", description = "更新公司信息")
    public Result<Void> updateCompany(@RequestBody SysCompany company) {
        log.info("更新公司, company={}", company);
        try {
            companyService.updateCompany(company);
            return Result.success();
        } catch (RuntimeException e) {
            log.error("更新公司失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除公司
     *
     * @param id 公司ID
     * @param tenantId 租户ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除公司", description = "删除指定的公司")
    public Result<Void> deleteCompany(
            @Parameter(description = "公司ID") @PathVariable Long id,
            @RequestHeader(value = "tenantId", defaultValue = "0") Long tenantId) {
        log.info("删除公司, id={}, tenantId={}", id, tenantId);
        try {
            companyService.deleteCompany(id, tenantId);
            return Result.success();
        } catch (RuntimeException e) {
            log.error("删除公司失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 搜索公司
     *
     * @param keyword 关键词
     * @param tenantId 租户ID
     * @return 公司列表
     */
    @GetMapping("/search")
    @Operation(summary = "搜索公司", description = "根据关键词搜索公司")
    public Result<List<CompanyVO>> searchCompanies(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @RequestHeader(value = "tenantId", defaultValue = "0") Long tenantId) {
        log.info("搜索公司, keyword={}, tenantId={}", keyword, tenantId);
        List<CompanyVO> list = companyService.searchCompanies(keyword, tenantId);
        return Result.success(list);
    }

    /**
     * 检查公司编码是否存在
     *
     * @param companyCode 公司编码
     * @param tenantId 租户ID
     * @param excludeId 排除的ID
     * @return 是否存在
     */
    @GetMapping("/check-code")
    @Operation(summary = "检查公司编码", description = "检查公司编码是否已存在")
    public Result<Boolean> checkCompanyCodeExists(
            @Parameter(description = "公司编码") @RequestParam String companyCode,
            @RequestHeader(value = "tenantId", defaultValue = "0") Long tenantId,
            @Parameter(description = "排除的ID") @RequestParam(required = false) Long excludeId) {
        log.info("检查公司编码, companyCode={}, tenantId={}, excludeId={}", companyCode, tenantId, excludeId);
        boolean exists = companyService.checkCompanyCodeExists(companyCode, tenantId, excludeId);
        return Result.success(exists);
    }
}
