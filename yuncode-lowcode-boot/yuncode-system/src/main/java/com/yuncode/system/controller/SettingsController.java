package com.yuncode.system.controller;

import com.yuncode.common.model.util.response.Result;
import com.yuncode.system.service.SysSettingsService;
import com.yuncode.system.vo.SettingsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统设置控制器
 */
@Slf4j
@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
@Tag(name = "系统设置", description = "系统设置相关接口")
public class SettingsController {

    private final SysSettingsService settingsService;

    /**
     * 获取基础设置
     */
    @GetMapping("/basic")
    @Operation(summary = "获取基础设置", description = "获取应用的基础设置信息")
    public Result<SettingsVO> getBasicSettings() {
        SettingsVO settings = settingsService.getBasicSettings();
        return Result.success(settings);
    }

    /**
     * 更新基础设置
     */
    @PutMapping("/basic")
    @Operation(summary = "更新基础设置", description = "更新应用的基础设置信息")
    public Result<Void> updateBasicSettings(@RequestBody SettingsVO settingsVO) {
        settingsService.updateBasicSettings(settingsVO);
        return Result.success();
    }

    /**
     * 根据分组获取设置
     */
    @GetMapping("/group/{group}")
    @Operation(summary = "获取分组设置", description = "根据分组获取设置")
    public Result<Map<String, String>> getSettingsByGroup(@PathVariable String group) {
        Map<String, String> settings = settingsService.getSettingsByGroup(group);
        return Result.success(settings);
    }
}
