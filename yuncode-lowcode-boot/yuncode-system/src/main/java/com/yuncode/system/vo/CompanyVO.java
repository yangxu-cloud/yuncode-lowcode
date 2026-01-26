package com.yuncode.system.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 公司信息视图对象
 *
 * @author Yuncode
 * @since 2025-01-22
 */
@Data
public class CompanyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 公司编码
     */
    private String companyCode;

    /**
     * 公司类型：1=有限公司，2=股份公司，3=个体工商户，4=其他
     */
    private Integer companyType;

    /**
     * 公司名称
     */
    private String companyTypeName;

    /**
     * 统一社会信用代码
     */
    private String creditCode;

    /**
     * 法定代表人
     */
    private String legalPerson;

    /**
     * 注册资本（万元）
     */
    private BigDecimal registerCapital;

    /**
     * 成立日期
     */
    private LocalDate establishDate;

    /**
     * 注册地址
     */
    private String registerAddress;

    /**
     * 经营地址
     */
    private String businessAddress;

    /**
     * 经营范围
     */
    private String businessScope;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 联系邮箱
     */
    private String contactEmail;

    /**
     * 营业执照图片URL
     */
    private String businessLicense;

    /**
     * 状态：0=禁用，1=启用
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 组织数量
     */
    private Integer orgCount;

    /**
     * 人员数量
     */
    private Integer userCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 创建人
     */
    private String createBy;
}
