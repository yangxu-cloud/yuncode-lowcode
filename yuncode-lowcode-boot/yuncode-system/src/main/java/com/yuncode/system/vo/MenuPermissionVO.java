package com.yuncode.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 菜单权限VO
 *
 * @author Yuncode
 * @since 2025-01-30
 */
@Data

@Schema(description = "菜单权限VO")
public class MenuPermissionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "权限ID")
    private Long id;

    @Schema(description = "菜单ID")
    private Long menuId;

    @Schema(description = "目标类型:0=角色,1=用户,2=部门")
    private Integer targetType;

    @Schema(description = "目标ID")
    private Long targetId;

    @Schema(description = "目标名称")
    private String targetName;

    @Schema(description = "目标类型名称")
    private String targetTypeName;
}
