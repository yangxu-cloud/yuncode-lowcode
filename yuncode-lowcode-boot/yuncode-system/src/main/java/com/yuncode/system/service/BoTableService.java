package com.yuncode.system.service;

import com.yuncode.system.entity.SysBoField;
import com.yuncode.system.entity.SysBoTable;

import java.util.List;
import java.util.Map;

public interface BoTableService {

    /**
     * 创建业务对象表（含 11 个默认 BO 字段 + 导出 XML）
     */
    SysBoTable createBoTable(String appId, String titleName, String suffix, String storageType, Long categoryId);

    /**
     * 获取应用下某个分类的 BO 表列表
     */
    List<SysBoTable> getBoTableList(String appId, Long categoryId);

    /**
     * 获取 BO 表详情（含字段列表）
     */
    Map<String, Object> getBoTableDetail(Long id);

    /**
     * 删除 BO 表（逻辑删除）
     */
    void deleteBoTable(Long id);

    /**
     * 更新 BO 表元数据
     */
    void updateBoTable(Long id, String titleName, String storageType);

    /**
     * 批量保存字段（全量替换）
     */
    void batchSaveFields(Long tableId, List<SysBoField> fields);

    /**
     * 生成 DDL 文件到 repository 目录
     */
    String generateDdlFile(Long tableId);

    /**
     * 保存完整设计（字段 + 索引 + XML 导出），供 BODesigner 调用
     *
     * @param tableId BO 表 ID
     * @param fields  字段列表
     * @param indexes 索引定义 JSON
     */
    void saveBoDesign(Long tableId, List<SysBoField> fields, String indexes);

    /**
     * 获取 BO 表详情（含 indexes JSON 字段）
     */
    Map<String, Object> getBoTableDetailWithIndexes(Long id);

    /**
     * 生成安全 DDL diff（比较 XML 设计 vs 实际 DB 结构）
     *
     * @param tableId BO 表 ID
     * @return DDL diff 信息，含 sql 语句列表和风险等级
     */
    Map<String, Object> generateSafeDdl(Long tableId);

    /**
     * 执行安全 DDL（自动执行安全变更，返回需确认的变更）
     *
     * @param tableId  BO 表 ID
     * @param confirmed 已确认的危险操作列表（JSON 数组）
     * @return 执行结果
     */
    Map<String, Object> executeSafeDdl(Long tableId, List<String> confirmed);

    /**
     * 部署同步：备份 XML → 写新 XML → 同步元数据 → DDL
     *
     * @param tableId BO 表 ID
     */
    void deploySync(Long tableId);

    /**
     * 回滚到指定版本
     *
     * @param tableId       BO 表 ID
     * @param targetVersion 目标版本号
     */
    void rollback(Long tableId, Integer targetVersion);
}
