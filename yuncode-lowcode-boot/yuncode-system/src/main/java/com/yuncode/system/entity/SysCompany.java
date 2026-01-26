package com.yuncode.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 公司信息实体类
 *
 * @author Yuncode
 * @since 2025-01-22
 */
@Data
@TableName("sys_company")
public class SysCompany implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 删除标记：0=未删除，1=已删除
     */
    @TableLogic
    private Integer deleted;
}
