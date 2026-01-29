package com.yuncode.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 菜单表单对象
 *
 * @author Yuncode
 * @since 2025-01-30
 */
@Data
@Schema(description = "菜单表单对象")
public class MenuForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "菜单ID")
    private Long id;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "父菜单ID")
    private Long parentId;

    @Schema(description = "菜单类型（0目录 1菜单 2按钮）")
    private Integer menuType;

    @Schema(description = "路由地址")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "权限标识")
    private String permission;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "是否可见（0显示 1隐藏）")
    private Integer visible;

    @Schema(description = "状态（0正常 1禁用）")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
