package com.yuncode.tenant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统租户实体
 */
@Data
@TableName("sys_tenant")
public class SysTenant {

    /**
     * 租户ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * 租户编码（唯一标识）
     */
    private String tenantCode;

    /**
     * 联系人
     */
    private String contactName;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 联系邮箱
     */
    private String contactEmail;

    /**
     * 企业地址
     */
    private String address;

    /**
     * 租户类型（0试用 1标准 2高级 3企业）
     */
    private Integer tenantType;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 用户数量限制
     */
    private Integer userLimit;

    /**
     * 存储空间限制（MB）
     */
    private Integer storageLimit;

    /**
     * 状态（0正常 1禁用）
     */
    private Integer status;

    /**
     * 删除标志（0正常 1删除）
     */
    @TableLogic
    private Integer deleted;

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
     * 创建者
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 更新者
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
}
