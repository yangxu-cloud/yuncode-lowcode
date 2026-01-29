package com.yuncode.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 菜单树节点VO
 *
 * @author Yuncode
 * @since 2025-01-30
 */
@Data

@Schema(description = "菜单树节点VO")
public class MenuTreeNode implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "菜单ID")
    private Long id;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "父菜单ID")
    private Long parentId;

    @Schema(description = "层级")
    private Integer level;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "路由地址")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "菜单类型（0目录 1菜单 2按钮）")
    private Integer menuType;

    @Schema(description = "是否可见")
    private Integer visible;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "子菜单")
    private List<MenuTreeNode> children;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "租户编码")
    private String tenantCode;
}
