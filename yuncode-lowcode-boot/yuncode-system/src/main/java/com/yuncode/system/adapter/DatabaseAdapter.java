package com.yuncode.system.adapter;

import java.util.List;
import java.util.Map;

/**
 * 数据库适配器接口
 * <p>
 * 抽象不同数据库（MySQL、达梦、人大金仓等）的 DDL 方言、字段类型映射和元数据查询差异。
 * 新增数据库时只需实现本接口，无需修改业务代码。
 * </p>
 */
public interface DatabaseAdapter {

    /** 数据库类型标识：mysql / dm / kingbase / postgresql */
    String getDatabaseType();

    // ========== DDL：CREATE TABLE ==========

    /** 构建 CREATE TABLE SQL */
    String buildCreateTableSql(String tableName, List<ColumnDef> columns, String comment);

    /** 构建 CREATE TABLE SQL（含索引） */
    String buildCreateTableSql(String tableName, List<ColumnDef> columns, List<IndexDef> indexes, String comment);

    // ========== DDL：删除表 ==========

    /** 构建 DROP TABLE IF EXISTS SQL */
    default String buildDropTableSql(String tableName) {
        return "DROP TABLE IF EXISTS " + tableName + ";";
    }

    // ========== DDL：ALTER TABLE ==========

    /** 构建 ALTER TABLE ADD COLUMN SQL */
    String buildAddColumnSql(String tableName, ColumnDef column);

    /** 构建 ALTER TABLE MODIFY COLUMN SQL */
    String buildModifyColumnSql(String tableName, ColumnDef column);

    /** 构建 ALTER TABLE DROP COLUMN SQL */
    String buildDropColumnSql(String tableName, String columnName);

    // ========== DDL：索引 ==========

    /** 构建 CREATE INDEX SQL */
    String buildCreateIndexSql(String tableName, IndexDef index);

    /** 构建 DROP INDEX SQL */
    String buildDropIndexSql(String tableName, String indexName);

    // ========== 类型映射 ==========

    /**
     * 将逻辑字段类型映射为数据库特定的列类型字符串
     * 例如：("varchar", 128) → MySQL: "varchar(128)" / DM: "varchar(128)"
     * 例如：("int", 10) → MySQL: "int(10)" / DM: "INT"
     */
    String mapToColumnType(String fieldType, Integer length);

    // ========== INFORMATION_SCHEMA 查询 ==========

    /** 构建查询表是否存在及元数据的 SQL */
    String getTableInfoSql(String tableName);

    /** 构建查询列信息的 SQL（返回行包含 COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE, COLUMN_DEFAULT） */
    String getColumnInfoSql(String tableName);

    // ========== ID 策略 ==========

    /** MyBatis-Plus IdType：ASSIGN_ID / ASSIGN_UUID / AUTO */
    default String getIdType() {
        return "ASSIGN_ID";
    }
}
