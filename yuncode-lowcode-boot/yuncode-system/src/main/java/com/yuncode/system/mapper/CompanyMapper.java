package com.yuncode.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuncode.system.entity.SysCompany;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 公司信息 Mapper 接口
 *
 * @author Yuncode
 * @since 2025-01-22
 */
@Mapper
public interface CompanyMapper extends BaseMapper<SysCompany> {

    /**
     * 根据租户ID查询公司列表
     *
     * @param tenantId 租户ID
     * @return 公司列表
     */
    @Select("SELECT * FROM sys_company WHERE tenant_id = #{tenantId} AND deleted = 0 ORDER BY id")
    List<SysCompany> selectByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 根据公司编码查询
     *
     * @param companyCode 公司编码
     * @param tenantId 租户ID
     * @return 公司信息
     */
    @Select("SELECT * FROM sys_company WHERE company_code = #{companyCode} AND tenant_id = #{tenantId} AND deleted = 0 LIMIT 1")
    SysCompany selectByCode(@Param("companyCode") String companyCode, @Param("tenantId") Long tenantId);

    /**
     * 根据统一社会信用代码查询
     *
     * @param creditCode 统一社会信用代码
     * @param tenantId 租户ID
     * @return 公司信息
     */
    @Select("SELECT * FROM sys_company WHERE credit_code = #{creditCode} AND tenant_id = #{tenantId} AND deleted = 0 LIMIT 1")
    SysCompany selectByCreditCode(@Param("creditCode") String creditCode, @Param("tenantId") Long tenantId);
}
