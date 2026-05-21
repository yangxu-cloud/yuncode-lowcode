package com.yuncode.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用实体类
 *
 * @author Yuncode
 * @since 2025-01-30
 */
@Data
@TableName("sys_application")
public class SysApplication implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 应用ID（com.xxx.xxx格式）
     */
    @Schema(description = "应用ID")
    private String appId;

    /**
     * 应用名称
     */
    @Schema(description = "应用名称")
    private String appName;

    /**
     * 应用图标
     */
    @Schema(description = "应用图标")
    private String appIcon;

    /**
     * 应用描述
     */
    @Schema(description = "应用描述")
    private String appDescription;

    /**
     * 运行状态(0-未运行, 1-运行中, 2-已停止, 3-异常)
     */
    @Schema(description = "运行状态")
    private Integer status;

    /**
     * 版本号
     */
    @Schema(description = "版本号")
    private String version;

    /**
     * 启动时间
     */
    @Schema(description = "启动时间")
    private LocalDateTime startTime;

    /**
     * 停止时间
     */
    @Schema(description = "停止时间")
    private LocalDateTime stopTime;

    /**
     * 租户ID
     */
    @Schema(description = "租户ID")
    private Long tenantId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 删除标记:0=未删除, 1=已删除
     */
    @TableLogic
    private Integer deleted;
}