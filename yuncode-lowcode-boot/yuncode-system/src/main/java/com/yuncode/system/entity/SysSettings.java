package com.yuncode.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统设置实体
 */
@Data
@TableName("sys_settings")
public class SysSettings {

    /**
     * 设置ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 设置分组
     */
    private String settingGroup;

    /**
     * 设置键
     */
    private String settingKey;

    /**
     * 设置值
     */
    private String settingValue;

    /**
     * 设置名称
     */
    private String settingName;

    /**
     * 设置描述
     */
    private String description;

    /**
     * 数据类型（string, number, boolean, json）
     */
    private String dataType;

    /**
     * 是否系统设置（0否 1是）
     */
    private Integer isSystem;

    /**
     * 排序
     */
    private Integer sort;

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
