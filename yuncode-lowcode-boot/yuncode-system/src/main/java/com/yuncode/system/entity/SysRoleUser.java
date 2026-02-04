package com.yuncode.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色用户关联实体
 */
@Data
@TableName("sys_role_user")
public class SysRoleUser {

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
