package com.yuncode.system.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 公司查询对象
 *
 * @author Yuncode
 * @since 2025-01-22
 */
@Data
public class CompanyQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 公司名称（模糊查询）
     */
    private String companyName;

    /**
     * 公司编码（模糊查询）
     */
    private String companyCode;

    /**
     * 公司类型：1=有限公司，2=股份公司，3=个体工商户，4=其他
     */
    private Integer companyType;

    /**
     * 统一社会信用代码（精确查询）
     */
    private String creditCode;

    /**
     * 状态：0=禁用，1=启用
     */
    private Integer status;

    /**
     * 关键词搜索（公司名称、公司编码、统一社会信用代码）
     */
    private String keyword;
}
