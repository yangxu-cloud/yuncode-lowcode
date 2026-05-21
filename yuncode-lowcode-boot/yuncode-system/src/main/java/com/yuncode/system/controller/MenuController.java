package com.yuncode.system.controller;

import com.yuncode.common.model.util.response.Result;
import com.yuncode.system.annotation.OperLog;
import com.yuncode.system.dto.AddMenuPermissionDTO;
import com.yuncode.system.dto.MenuForm;
import com.yuncode.system.entity.SysMenu;
import com.yuncode.system.service.MenuService;
import com.yuncode.system.vo.MenuPermissionVO;
import com.yuncode.system.vo.MenuTreeNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理控制器
 *
 * @author Yuncode
 * @since 2025-01-30
 */
@Slf4j
@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
@Tag(name = "菜单管理", description = "菜单管理相关接口")
public class MenuController {

    private final MenuService menuService;

    /**
     * 获取菜单树
     *
     * @return 菜单树节点列表
     */
    @GetMapping("/tree")
    @Operation(summary = "获取菜单树", description = "获取菜单树结构")
    public Result<List<MenuTreeNode>> getMenuTree() {
        log.info("获取菜单树");
        List<MenuTreeNode> tree = menuService.getMenuTree();
        return Result.success(tree);
    }

    /**
     * 获取用户可访问的菜单树
     *
     * @return 菜单树节点列表
     */
    @GetMapping("/user/tree")
    @Operation(summary = "获取用户菜单树", description = "获取当前用户可访问的菜单树")
    public Result<List<MenuTreeNode>> getUserMenuTree() {
        log.info("获取用户菜单树");
        // TODO: 从当前登录用户获取userId和tenantId
        List<MenuTreeNode> tree = menuService.getMenuTree();
        return Result.success(tree);
    }

    /**
     * 获取菜单详情
     *
     * @param id 菜单ID
     * @return 菜单详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取菜单详情", description = "根据ID获取菜单详细信息")
    public Result<SysMenu> getMenuById(@Parameter(description = "菜单ID") @PathVariable Long id) {
        log.info("获取菜单详情, id={}", id);
        SysMenu menu = menuService.getMenuById(id);
        if (menu == null) {
            return Result.error("菜单不存在");
        }
        return Result.success(menu);
    }

    /**
     * 添加菜单
     *
     * @param menuForm 菜单表单
     * @return 是否成功
     */
    @PostMapping
    @Operation(summary = "添加菜单", description = "添加新的菜单")
    @OperLog(module = "菜单管理", businessType = 1, description = "添加菜单")
    public Result<Void> addMenu(@RequestBody MenuForm menuForm) {
        log.info("添加菜单, menuForm={}", menuForm);
        try {
            menuService.addMenu(menuForm);
            return Result.success();
        } catch (RuntimeException e) {
            log.error("添加菜单失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新菜单
     *
     * @param menuForm 菜单表单
     * @return 是否成功
     */
    @PutMapping
    @Operation(summary = "更新菜单", description = "更新菜单信息")
    @OperLog(module = "菜单管理", businessType = 2, description = "更新菜单")
    public Result<Void> updateMenu(@RequestBody MenuForm menuForm) {
        log.info("更新菜单, menuForm={}", menuForm);
        try {
            menuService.updateMenu(menuForm);
            return Result.success();
        } catch (RuntimeException e) {
            log.error("更新菜单失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除菜单
     *
     * @param id 菜单ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除菜单", description = "删除指定的菜单")
    @OperLog(module = "菜单管理", businessType = 3, description = "删除菜单")
    public Result<Void> deleteMenu(@Parameter(description = "菜单ID") @PathVariable Long id) {
        log.info("删除菜单, id={}", id);
        try {
            menuService.deleteMenu(id);
            return Result.success();
        } catch (RuntimeException e) {
            log.error("删除菜单失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 上移菜单
     *
     * @param menuId 菜单ID
     * @return 是否成功
     */
    @PutMapping("/move-up/{menuId}")
    @Operation(summary = "上移菜单", description = "将菜单向上移动")
    @OperLog(module = "菜单管理", businessType = 2, description = "上移菜单")
    public Result<Void> moveUp(@Parameter(description = "菜单ID") @PathVariable Long menuId) {
        log.info("上移菜单, menuId={}", menuId);
        try {
            menuService.moveUp(menuId);
            return Result.success();
        } catch (RuntimeException e) {
            log.error("上移菜单失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 下移菜单
     *
     * @param menuId 菜单ID
     * @return 是否成功
     */
    @PutMapping("/move-down/{menuId}")
    @Operation(summary = "下移菜单", description = "将菜单向下移动")
    @OperLog(module = "菜单管理", businessType = 2, description = "下移菜单")
    public Result<Void> moveDown(@Parameter(description = "菜单ID") @PathVariable Long menuId) {
        log.info("下移菜单, menuId={}", menuId);
        try {
            menuService.moveDown(menuId);
            return Result.success();
        } catch (RuntimeException e) {
            log.error("下移菜单失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 设置菜单可见性
     *
     * @param menuId 菜单ID
     * @param visible 是否可见（0=显示, 1=隐藏）
     * @return 是否成功
     */
    @PutMapping("/visible")
    @Operation(summary = "设置菜单可见性", description = "设置菜单是否可见")
    @OperLog(module = "菜单管理", businessType = 2, description = "设置菜单可见性")
    public Result<Void> setVisible(
            @Parameter(description = "菜单ID") @RequestParam Long menuId,
            @Parameter(description = "是否可见（0=显示, 1=隐藏）") @RequestParam Integer visible) {
        log.info("设置菜单可见性, menuId={}, visible={}", menuId, visible);
        try {
            menuService.setVisible(menuId, visible);
            return Result.success();
        } catch (RuntimeException e) {
            log.error("设置菜单可见性失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 搜索菜单
     *
     * @param keyword 关键词
     * @return 菜单列表
     */
    @GetMapping("/search")
    @Operation(summary = "搜索菜单", description = "根据关键词搜索菜单")
    public Result<List<SysMenu>> searchMenus(@Parameter(description = "搜索关键词") @RequestParam String keyword) {
        log.info("搜索菜单, keyword={}", keyword);
        List<SysMenu> menus = menuService.searchMenus(keyword);
        return Result.success(menus);
    }

    /**
     * 获取菜单的权限列表
     *
     * @param menuId 菜单ID
     * @return 权限列表
     */
    @GetMapping("/permissions/{menuId}")
    @Operation(summary = "获取菜单权限", description = "获取菜单的权限配置列表")
    public Result<List<MenuPermissionVO>> getMenuPermissions(
            @Parameter(description = "菜单ID") @PathVariable Long menuId) {
        log.info("获取菜单权限, menuId={}", menuId);
        List<MenuPermissionVO> permissions = menuService.getMenuPermissions(menuId);
        return Result.success(permissions);
    }

    /**
     * 添加权限到菜单
     *
     * @param dto 添加权限DTO
     * @return 是否成功
     */
    @PostMapping("/permissions")
    @Operation(summary = "添加菜单权限", description = "为菜单添加权限配置")
    @OperLog(module = "菜单管理", businessType = 1, description = "添加菜单权限")
    public Result<Void> addPermissions(@RequestBody AddMenuPermissionDTO dto) {
        log.info("添加菜单权限, dto={}", dto);
        try {
            menuService.addPermissions(dto.getMenuId(), dto.getTargetType(), dto.getTargetIds());
            return Result.success();
        } catch (RuntimeException e) {
            log.error("添加菜单权限失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 移除菜单权限
     *
     * @param menuId 菜单ID
     * @param targetType 目标类型
     * @param targetId 目标ID
     * @return 是否成功
     */
    @DeleteMapping("/permissions")
    @Operation(summary = "移除菜单权限", description = "移除菜单的权限配置")
    @OperLog(module = "菜单管理", businessType = 3, description = "移除菜单权限")
    public Result<Void> removePermission(
            @Parameter(description = "菜单ID") @RequestParam Long menuId,
            @Parameter(description = "目标类型") @RequestParam Integer targetType,
            @Parameter(description = "目标ID") @RequestParam Long targetId) {
        log.info("移除菜单权限, menuId={}, targetType={}, targetId={}", menuId, targetType, targetId);
        try {
            menuService.removePermission(menuId, targetType, targetId);
            return Result.success();
        } catch (RuntimeException e) {
            log.error("移除菜单权限失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 权限追加到下级菜单
     *
     * @param menuId 菜单ID
     * @return 追加的权限数量
     */
    @PostMapping("/permissions/copy-to-children/{menuId}")
    @Operation(summary = "权限追加到下级", description = "将当前菜单的权限追加到所有子菜单")
    @OperLog(module = "菜单管理", businessType = 1, description = "权限追加到下级")
    public Result<Integer> copyPermissionsToChildren(
            @Parameter(description = "菜单ID") @PathVariable Long menuId) {
        log.info("权限追加到下级, menuId={}", menuId);
        try {
            int count = menuService.copyPermissionsToChildren(menuId);
            return Result.success(count);
        } catch (RuntimeException e) {
            log.error("权限追加到下级失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 初始化默认菜单
     *
     * @return 是否成功
     */
    @PostMapping("/init")
    @Operation(summary = "初始化默认菜单", description = "初始化系统的默认菜单")
    @OperLog(module = "菜单管理", businessType = 1, description = "初始化默认菜单")
    public Result<Void> initDefaultMenus() {
        log.info("初始化默认菜单");
        try {
            menuService.initDefaultMenus();
            return Result.success();
        } catch (RuntimeException e) {
            log.error("初始化默认菜单失败", e);
            return Result.error(e.getMessage());
        }
    }
}
