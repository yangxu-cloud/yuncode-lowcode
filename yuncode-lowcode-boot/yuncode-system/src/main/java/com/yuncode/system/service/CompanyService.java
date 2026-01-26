package com.yuncode.system.service;

import com.yuncode.system.dto.CompanyQueryDTO;
import com.yuncode.system.entity.SysCompany;
import com.yuncode.system.vo.CompanyVO;

import java.util.List;

/**
 * 公司信息 Service 接口
 *
 * @author Yuncode
 * @since 2025-01-22
 */
public interface CompanyService {

    /**
     * 查询公司列表
     *
     * @param queryDTO 查询条件
     * @param tenantId 租户ID
     * @return 公司列表
     */
    List<CompanyVO> getCompanyList(CompanyQueryDTO queryDTO, Long tenantId);

    /**
     * 根据ID查询公司详情
     *
     * @param id 公司ID
     * @param tenantId 租户ID
     * @return 公司详情
     */
    CompanyVO getCompanyById(Long id, Long tenantId);

    /**
     * 添加公司
     *
     * @param company 公司实体
     */
    void addCompany(SysCompany company);

    /**
     * 更新公司
     *
     * @param company 公司实体
     */
    void updateCompany(SysCompany company);

    /**
     * 删除公司
     *
     * @param id 公司ID
     * @param tenantId 租户ID
     */
    void deleteCompany(Long id, Long tenantId);

    /**
     * 检查公司编码是否存在
     *
     * @param companyCode 公司编码
     * @param tenantId 租户ID
     * @param excludeId 排除的ID（更新时使用）
     * @return 是否存在
     */
    boolean checkCompanyCodeExists(String companyCode, Long tenantId, Long excludeId);

    /**
     * 搜索公司
     *
     * @param keyword 关键词
     * @param tenantId 租户ID
     * @return 公司列表
     */
    List<CompanyVO> searchCompanies(String keyword, Long tenantId);
}
