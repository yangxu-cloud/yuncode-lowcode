package com.yuncode.system.controller;

import com.yuncode.common.model.util.response.Result;
import com.yuncode.system.service.MenuService;
import com.yuncode.system.vo.RouteVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前端动态路由控制器
 * <p>
 * 为前端 pure-admin 提供动态路由数据，菜单数据来自系统菜单管理模块。
 *
 * @author Yuncode
 * @since 2025-01-30
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "前端路由", description = "前端动态路由接口（菜单 → 路由转换）")
public class RouteController {

    private final MenuService menuService;

    @GetMapping("/get-async-routes")
    @Operation(summary = "获取动态路由", description = "获取当前用户可访问的动态路由树（由菜单管理数据转换而来）")
    public Result<List<RouteVO>> getAsyncRoutes() {
        log.debug("获取动态路由");
        List<RouteVO> routes = menuService.getAsyncRoutes();
        return Result.success(routes);
    }
}
