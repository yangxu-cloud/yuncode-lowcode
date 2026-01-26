package com.yuncode.system.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 组织查询对象
 *
 * @author Yuncode
 * @since 2025-01-22
 */
@Data
public class OrgQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 组织名称（模糊查询）
     */
    private String orgName;

    /**
     * 组织编码（模糊查询）
     */
    private String orgCode;

    /**
     * 父组织ID
     */
    private Long parentId;

    /**
     * 组织类型：1=集团/公司，2=部门
     */
    private Integer orgType;

    /**
     * 是否公司：0=否，1=是
     */
    private Integer isCompany;

    /**
     * 状态：0=禁用，1=启用
     */
    private Integer status;

    /**
     * 搜索关键词（搜索组织名、编码）
     */
    private String keyword;
}
