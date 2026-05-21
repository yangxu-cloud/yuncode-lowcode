package com.yuncode.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 应用表单DTO
 *
 * @author Yuncode
 * @since 2025-01-30
 */
@Data
@Schema(description = "应用表单DTO")
public class ApplicationForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "应用ID")
    private String appId;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "应用图标")
    private String appIcon;

    @Schema(description = "应用描述")
    private String appDescription;

    @Schema(description = "版本号")
    private String version;
}