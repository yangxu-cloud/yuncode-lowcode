package com.yuncode.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 添加菜单权限DTO
 *
 * @author Yuncode
 * @since 2025-01-30
 */
@Data
@Schema(description = "添加菜单权限请求参数")
public class AddMenuPermissionDTO {

    @Schema(description = "菜单ID")
    private Long menuId;

    @Schema(description = "目标类型（0=角色, 1=用户, 2=部门）")
    private Integer targetType;

    @Schema(description = "目标ID列表")
    private List<Long> targetIds;
}
