package com.yuncode.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuncode.system.dto.CompanyQueryDTO;
import com.yuncode.system.entity.SysCompany;
import com.yuncode.system.mapper.CompanyMapper;
import com.yuncode.system.service.CompanyService;
import com.yuncode.system.vo.CompanyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 公司信息 Service 实现类
 *
 * @author Yuncode
 * @since 2025-01-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyServiceImpl extends ServiceImpl<CompanyMapper, SysCompany> implements CompanyService {

    private final CompanyMapper companyMapper;

    @Override
    public List<CompanyVO> getCompanyList(CompanyQueryDTO queryDTO, Long tenantId) {
        log.info("查询公司列表, queryDTO={}, tenantId={}", queryDTO, tenantId);

        LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysCompany::getTenantId, tenantId);

        if (queryDTO != null) {
            if (StringUtils.hasText(queryDTO.getCompanyName())) {
                wrapper.like(SysCompany::getCompanyName, queryDTO.getCompanyName());
            }
            if (StringUtils.hasText(queryDTO.getCompanyCode())) {
                wrapper.like(SysCompany::getCompanyCode, queryDTO.getCompanyCode());
            }
            if (queryDTO.getCompanyType() != null) {
                wrapper.eq(SysCompany::getCompanyType, queryDTO.getCompanyType());
            }
            if (StringUtils.hasText(queryDTO.getCreditCode())) {
                wrapper.eq(SysCompany::getCreditCode, queryDTO.getCreditCode());
            }
            if (queryDTO.getStatus() != null) {
                wrapper.eq(SysCompany::getStatus, queryDTO.getStatus());
            }
            if (StringUtils.hasText(queryDTO.getKeyword())) {
                String keyword = queryDTO.getKeyword();
                wrapper.and(w -> w.like(SysCompany::getCompanyName, keyword)
                        .or().like(SysCompany::getCompanyCode, keyword)
                        .or().like(SysCompany::getCreditCode, keyword));
            }
        }

        wrapper.orderByAsc(SysCompany::getId);

        List<SysCompany> companies = companyMapper.selectList(wrapper);
        List<CompanyVO> result = new ArrayList<>();

        for (SysCompany company : companies) {
            result.add(convertToCompanyVO(company));
        }

        return result;
    }

    @Override
    public CompanyVO getCompanyById(Long id, Long tenantId) {
        log.info("查询公司详情, id={}, tenantId={}", id, tenantId);

        SysCompany company = companyMapper.selectById(id);
        if (company == null || !company.getTenantId().equals(tenantId)) {
            return null;
        }

        return convertToCompanyVO(company);
    }

    @Override
    public void addCompany(SysCompany company) {
        log.info("添加公司, company={}", company);

        // 检查公司编码是否存在
        if (checkCompanyCodeExists(company.getCompanyCode(), company.getTenantId(), null)) {
            throw new RuntimeException("公司编码已存在");
        }

        companyMapper.insert(company);
    }

    @Override
    public void updateCompany(SysCompany company) {
        log.info("更新公司, company={}", company);

        // 检查公司是否存在
        SysCompany existCompany = companyMapper.selectById(company.getId());
        if (existCompany == null) {
            throw new RuntimeException("公司不存在");
        }

        // 检查公司编码是否被其他公司使用
        if (checkCompanyCodeExists(company.getCompanyCode(), company.getTenantId(), company.getId())) {
            throw new RuntimeException("公司编码已被其他公司使用");
        }

        companyMapper.updateById(company);
    }

    @Override
    public void deleteCompany(Long id, Long tenantId) {
        log.info("删除公司, id={}, tenantId={}", id, tenantId);

        // 检查公司是否存在
        SysCompany company = companyMapper.selectById(id);
        if (company == null || !company.getTenantId().equals(tenantId)) {
            throw new RuntimeException("公司不存在");
        }

        // TODO: 检查公司下是否有组织或用户，如果有则不允许删除
        // Long orgCount = orgMapper.selectCount(new LambdaQueryWrapper<SysOrg>()
        //     .eq(SysOrg::getCompanyId, id));
        // if (orgCount > 0) {
        //     throw new RuntimeException("该公司下还有组织，不能删除");
        // }

        companyMapper.deleteById(id);
    }

    @Override
    public boolean checkCompanyCodeExists(String companyCode, Long tenantId, Long excludeId) {
        LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysCompany::getCompanyCode, companyCode)
                .eq(SysCompany::getTenantId, tenantId);

        if (excludeId != null) {
            wrapper.ne(SysCompany::getId, excludeId);
        }

        return companyMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<CompanyVO> searchCompanies(String keyword, Long tenantId) {
        log.info("搜索公司, keyword={}, tenantId={}", keyword, tenantId);

        if (!StringUtils.hasText(keyword)) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysCompany::getTenantId, tenantId)
                .and(w -> w.like(SysCompany::getCompanyName, keyword)
                        .or().like(SysCompany::getCompanyCode, keyword)
                        .or().like(SysCompany::getCreditCode, keyword))
                .orderByAsc(SysCompany::getId);

        List<SysCompany> companies = companyMapper.selectList(wrapper);
        List<CompanyVO> result = new ArrayList<>();

        for (SysCompany company : companies) {
            result.add(convertToCompanyVO(company));
        }

        return result;
    }

    /**
     * 转换为公司视图对象
     *
     * @param company 公司实体
     * @return 公司视图对象
     */
    private CompanyVO convertToCompanyVO(SysCompany company) {
        if (company == null) {
            return new CompanyVO();
        }

        CompanyVO vo = new CompanyVO();
        BeanUtils.copyProperties(company, vo);

        // 设置公司类型名称
        if (company.getCompanyType() != null) {
            String[] typeNames = {"", "有限公司", "股份公司", "个体工商户", "其他"};
            if (company.getCompanyType() >= 1 && company.getCompanyType() <= typeNames.length) {
                vo.setCompanyTypeName(typeNames[company.getCompanyType()]);
            }
        }

        // 设置状态名称
        if (company.getStatus() != null) {
            vo.setStatusName(company.getStatus() == 1 ? "启用" : "禁用");
        }

        // TODO: 统计组织数量和用户数量
        // Long orgCount = orgMapper.selectCount(new LambdaQueryWrapper<SysOrg>()
        //     .eq(SysOrg::getCompanyId, company.getId()));
        // Long userCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
        //     .eq(SysUser::getCompanyId, company.getId()));
        // vo.setOrgCount(orgCount.intValue());
        // vo.setUserCount(userCount.intValue());

        return vo;
    }
}
