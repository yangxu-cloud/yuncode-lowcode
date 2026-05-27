package com.yuncode.system.adapter;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MySQL 数据库适配器
 */
@Component
public class MySQLDatabaseAdapter implements DatabaseAdapter {

    @Override
    public String getDatabaseType() {
        return "mysql";
    }

    @Override
    public String buildCreateTableSql(String tableName, List<ColumnDef> columns, String comment) {
        return buildCreateTableSql(tableName, columns, List.of(), comment);
    }

    @Override
    public String buildCreateTableSql(String tableName, List<ColumnDef> columns, List<IndexDef> indexes, String comment) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(tableName).append(" (\n");

        // 列定义
        for (int i = 0; i < columns.size(); i++) {
            ColumnDef col = columns.get(i);
            sb.append("  ").append(col.getFieldName()).append(" ").append(mapToColumnType(col));
            if (col.isPrimaryKey()) {
                sb.append(" NOT NULL PRIMARY KEY");
            } else if (col.isNotNull()) {
                sb.append(" NOT NULL");
            }
            if (col.getDefaultValue() != null && !col.getDefaultValue().isEmpty()) {
                sb.append(" DEFAULT '").append(col.getDefaultValue()).append("'");
            }
            if (i < columns.size() - 1) sb.append(",");
            sb.append("\n");
        }

        // 索引定义（CREATE TABLE 内联）
        for (IndexDef idx : indexes) {
            sb.append("  , ");
            if ("UNIQUE".equalsIgnoreCase(idx.getIndexType())) {
                sb.append("UNIQUE KEY ").append(idx.getIndexName());
            } else {
                sb.append("KEY ").append(idx.getIndexName());
            }
            sb.append(" (").append(idx.getColumnName()).append(")\n");
        }

        sb.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        if (comment != null && !comment.isEmpty()) {
            sb.append(" COMMENT='").append(comment).append("'");
        }
        sb.append(";\n");
        return sb.toString();
    }

    @Override
    public String buildAddColumnSql(String tableName, ColumnDef column) {
        return "ALTER TABLE " + tableName
                + " ADD COLUMN " + column.getFieldName()
                + " " + mapToColumnType(column)
                + notNullClause(column)
                + defaultClause(column)
                + ";";
    }

    @Override
    public String buildModifyColumnSql(String tableName, ColumnDef column) {
        return "ALTER TABLE " + tableName
                + " MODIFY COLUMN " + column.getFieldName()
                + " " + mapToColumnType(column)
                + notNullClause(column)
                + defaultClause(column)
                + ";";
    }

    @Override
    public String buildDropColumnSql(String tableName, String columnName) {
        return "ALTER TABLE " + tableName + " DROP COLUMN " + columnName + ";";
    }

    @Override
    public String buildCreateIndexSql(String tableName, IndexDef index) {
        String type = "UNIQUE".equalsIgnoreCase(index.getIndexType()) ? "UNIQUE INDEX" : "INDEX";
        return "CREATE " + type + " " + index.getIndexName()
                + " ON " + tableName + " (" + index.getColumnName() + ");";
    }

    @Override
    public String buildDropIndexSql(String tableName, String indexName) {
        return "DROP INDEX " + indexName + " ON " + tableName + ";";
    }

    @Override
    public String mapToColumnType(String fieldType, Integer length) {
        return switch (fieldType != null ? fieldType.toLowerCase() : "varchar") {
            case "int", "integer" -> "int(" + (length != null && length > 0 ? length : 11) + ")";
            case "bigint" -> "bigint(" + (length != null && length > 0 ? length : 20) + ")";
            case "datetime", "date" -> "datetime";
            case "decimal" -> "decimal(" + (length != null && length > 0 ? length : 18) + ",2)";
            case "text", "longtext" -> "text";
            default -> "varchar(" + (length != null && length > 0 ? length : 255) + ")";
        };
    }

    @Override
    public String getTableInfoSql(String tableName) {
        return "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE TABLE_SCHEMA = (SELECT DATABASE()) AND TABLE_NAME = '" + tableName + "'";
    }

    @Override
    public String getColumnInfoSql(String tableName) {
        return "SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, "
                + "CHARACTER_MAXIMUM_LENGTH, DATA_TYPE "
                + "FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = (SELECT DATABASE()) AND TABLE_NAME = '" + tableName + "'";
    }

    // ========== 内部辅助 ==========

    private String mapToColumnType(ColumnDef column) {
        String typeStr = mapToColumnType(column.getFieldType(), column.getFieldLength());
        // bigint/int 在 MySQL 中不设长度时也可不指定括号，但保留兼容
        return typeStr;
    }

    private String notNullClause(ColumnDef column) {
        return column.isNotNull() || column.isPrimaryKey() ? " NOT NULL" : "";
    }

    private String defaultClause(ColumnDef column) {
        if (column.getDefaultValue() != null && !column.getDefaultValue().isEmpty()) {
            return " DEFAULT '" + column.getDefaultValue() + "'";
        }
        return "";
    }
}
