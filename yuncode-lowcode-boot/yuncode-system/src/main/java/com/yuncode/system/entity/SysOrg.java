package com.yuncode.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 组织实体类
 *
 * @author Yuncode
 * @since 2025-01-22
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_org")
public class SysOrg implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属公司ID
     */
    private Long companyId;

    /**
     * 组织名称
     */
    private String orgName;

    /**
     * 组织编码
     */
    private String orgCode;

    /**
     * 父组织ID，0表示根节点
     */
    private Long parentId;

    /**
     * 组织类型：0=根节点，1=集团/公司（租户），2=部门
     */
    private Integer orgType;

    /**
     * 租户编码（仅公司节点orgType=1时有值，用于登录租户识别）
     */
    private String tenantCode;

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
    @TableField(fill = FieldFill.INSERT)
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
