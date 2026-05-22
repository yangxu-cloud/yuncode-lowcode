package com.yuncode.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 前端动态路由VO
 * <p>
 * 匹配 pure-admin 前端 addAsyncRoutes() 的输入格式。
 *
 * @author Yuncode
 * @since 2025-01-30
 */
@Data
@Schema(description = "前端动态路由节点")
public class RouteVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "路由地址")
    private String path;

    @Schema(description = "路由名称（用于 keep-alive）")
    private String name;

    @Schema(description = "组件路径（相对 src/views/，如 system/user/index）")
    private String component;

    @Schema(description = "重定向地址")
    private String redirect;

    @Schema(description = "路由元信息")
    private RouteMeta meta;

    @Schema(description = "子路由")
    private List<RouteVO> children;

    @Data
    @Schema(description = "路由元信息")
    public static class RouteMeta implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "菜单标题")
        private String title;

        @Schema(description = "菜单图标")
        private String icon;

        @Schema(description = "排序等级（越小越靠前）")
        private Integer rank;

        @Schema(description = "可见的角色编码列表（为空表示所有角色可见）")
        private List<String> roles;

        @Schema(description = "是否在侧边栏显示")
        private Boolean showLink;

        @Schema(description = "iframe 地址")
        private String frameSrc;
    }
}
