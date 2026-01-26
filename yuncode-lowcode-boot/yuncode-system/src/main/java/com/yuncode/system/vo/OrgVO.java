package com.yuncode.system.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 组织视图对象
 *
 * @author Yuncode
 * @since 2025-01-22
 */
@Data
public class OrgVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 组织名称
     */
    private String orgName;

    /**
     * 组织编码
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
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 状态：0=禁用，1=启用
     */
    private Integer status;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 组织下的人员数量
     */
    private Integer userCount;

    /**
     * 组织类型名称
     */
    private String orgTypeName;

    /**
     * 是否公司名称
     */
    private String isCompanyName;

    /**
     * 状态名称
     */
    private String statusName;
}
