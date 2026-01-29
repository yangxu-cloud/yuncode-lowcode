package com.yuncode.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户组织关联实体类
 *
 * @author Yuncode
 * @since 2025-01-22
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_user_org")
public class SysUserOrg implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 组织ID
     */
    private Long orgId;

    /**
     * 是否负责人：0=否，1=是
     */
    private Integer isLeader;

    /**
     * 是否主部门：0=否，1=是（与sys_user.dept_id对应）
     */
    private Integer isMainDept;

    /**
     * 租户ID（注意：此字段不自动填充，在业务代码中手动设置为组织的tenantId）
     */
    private Long tenantId;

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
     * 注意：sys_user_org 是关联关系表，使用物理删除而非逻辑删除
     * 因此不添加 @TableLogic 注解
     */
    private Integer deleted;
}
