package com.yuncode.system.controller;

import com.yuncode.common.model.util.response.Result;
import com.yuncode.system.dto.BoDesignSaveDTO;
import com.yuncode.system.dto.BoTableCreateDTO;
import com.yuncode.system.entity.SysBoField;
import com.yuncode.system.entity.SysBoTable;
import com.yuncode.system.service.BoTableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/system/application")
@RequiredArgsConstructor
@Tag(name = "业务对象管理", description = "BO表定义与字段管理")
public class BoTableController {

    private final BoTableService boTableService;

    @PostMapping("/{appId}/bo-tables")
    @Operation(summary = "创建业务对象", description = "创建业务对象表（含11个默认BO字段+XML导出）")
    public Result<SysBoTable> createBoTable(
            @PathVariable String appId,
            @RequestBody BoTableCreateDTO dto) {
        try {
            SysBoTable table = boTableService.createBoTable(
                    appId, dto.getTitleName(), dto.getSuffix(),
                    dto.getStorageType(), dto.getCategoryId());
            return Result.success("业务对象创建成功", table);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{appId}/bo-tables")
    @Operation(summary = "获取业务对象列表", description = "按应用和分类获取BO表列表")
    public Result<List<SysBoTable>> getBoTableList(
            @PathVariable String appId,
            @RequestParam(required = false) Long categoryId) {
        return Result.success(boTableService.getBoTableList(appId, categoryId));
    }

    @GetMapping("/bo-tables/{id}")
    @Operation(summary = "获取业务对象详情", description = "获取BO表信息及字段列表（含 indexes）")
    public Result<Map<String, Object>> getBoTableDetail(@PathVariable Long id) {
        try {
            return Result.success(boTableService.getBoTableDetailWithIndexes(id));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/bo-tables/{id}")
    @Operation(summary = "更新业务对象", description = "更新BO表元数据")
    public Result<Void> updateBoTable(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            boTableService.updateBoTable(id, body.get("titleName"), body.get("storageType"));
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/bo-tables/{id}")
    @Operation(summary = "删除业务对象", description = "逻辑删除BO表及字段")
    public Result<Void> deleteBoTable(@PathVariable Long id) {
        try {
            boTableService.deleteBoTable(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/bo-tables/{id}/fields")
    @Operation(summary = "批量保存字段", description = "全量替换BO表的字段列表")
    public Result<Void> batchSaveFields(
            @PathVariable Long id,
            @RequestBody List<SysBoField> fields) {
        try {
            boTableService.batchSaveFields(id, fields);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/bo-tables/{id}/generate-ddl")
    @Operation(summary = "生成DDL文件", description = "生成CREATE TABLE SQL文件到repository目录")
    public Result<String> generateDdl(@PathVariable Long id) {
        try {
            String path = boTableService.generateDdlFile(id);
            return Result.success("DDL文件已生成", path);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // ============ 新增端点 ============

    @PostMapping("/bo-tables/{id}/design")
    @Operation(summary = "保存完整设计", description = "保存字段+索引+XML导出，供BODesigner调用")
    public Result<String> saveBoDesign(
            @PathVariable Long id,
            @RequestBody BoDesignSaveDTO dto) {
        try {
            boTableService.saveBoDesign(id, dto.getFields(), dto.getIndexes());
            return Result.success("设计保存成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/bo-tables/{id}/ddl-diff")
    @Operation(summary = "DDL 变更预览", description = "比较 XML 设计与实际表结构，生成安全 DDL")
    public Result<Map<String, Object>> getDdlDiff(@PathVariable Long id) {
        try {
            return Result.success(boTableService.generateSafeDdl(id));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/bo-tables/{id}/ddl-execute")
    @Operation(summary = "执行 DDL", description = "执行安全 DDL，返回执行结果")
    public Result<Map<String, Object>> executeDdl(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<String> confirmed = body != null ? (List<String>) body.getOrDefault("confirmed", List.of()) : List.of();
            return Result.success(boTableService.executeSafeDdl(id, confirmed));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/bo-tables/{id}/deploy-sync")
    @Operation(summary = "部署同步", description = "备份XML→写新XML→同步元数据→DDL")
    public Result<String> deploySync(@PathVariable Long id) {
        try {
            boTableService.deploySync(id);
            return Result.success("部署同步完成");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/bo-tables/{id}/rollback")
    @Operation(summary = "回滚设计", description = "回滚到指定版本")
    public Result<String> rollback(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            Integer targetVersion = (Integer) body.get("version");
            if (targetVersion == null) {
                return Result.error("版本号不能为空");
            }
            boTableService.rollback(id, targetVersion);
            return Result.success("回滚完成");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
