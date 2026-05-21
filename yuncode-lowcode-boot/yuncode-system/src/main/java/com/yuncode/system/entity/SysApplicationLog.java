package com.yuncode.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用日志实体类
 *
 * @author Yuncode
 * @since 2025-01-30
 */
@Data
@TableName("sys_application_log")
public class SysApplicationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 应用ID
     */
    @Schema(description = "应用ID")
    private Long appId;

    /**
     * 操作类型(0-安装, 1-启动, 2-停止, 3-卸载, 4-升级)
     */
    @Schema(description = "操作类型")
    private Integer operationType;

    /**
     * 操作内容
     */
    @Schema(description = "操作内容")
    private String operationContent;

    /**
     * 状态(0-成功, 1-失败)
     */
    @Schema(description = "状态")
    private Integer status;

    /**
     * 错误信息
     */
    @Schema(description = "错误信息")
    private String errorMessage;

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
}