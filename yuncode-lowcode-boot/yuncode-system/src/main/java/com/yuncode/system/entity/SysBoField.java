package com.yuncode.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务对象字段定义
 */
@Data
@TableName("sys_bo_field")
public class SysBoField {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tableId;

    private String fieldName;

    private String fieldTitle;

    private String fieldType;

    private Integer fieldLength;

    private String component;

    private String defaultValue;

    private Integer required;

    private Integer visible;

    private Integer readonly;

    private Integer copyable;

    private Integer sort;

    private String componentSetting;

    private Integer columnWidth;

    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    private Integer deleted = 0;
}
