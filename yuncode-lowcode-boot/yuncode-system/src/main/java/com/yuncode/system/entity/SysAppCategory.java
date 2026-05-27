package com.yuncode.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用分类实体类
 *
 * @author Yuncode
 * @since 2025-05-24
 */
@Data
@TableName("sys_app_category")
public class SysAppCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "应用标识")
    private String appId;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "父分类ID，NULL表示一级分类")
    private Long parentId;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "租户ID")
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
