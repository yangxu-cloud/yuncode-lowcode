package com.yuncode.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务对象表定义
 */
@Data
@TableName("sys_bo_table")
public class SysBoTable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String appId;

    private Long categoryId;

    private String categoryName;

    private String titleName;

    private String storageName;

    private String storageType;

    private String bizCode;

    private String indexes;

    private Integer designVersion;

    private String signature;

    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    private Integer deleted = 0;
}
