package com.yuncode.system.adapter;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 达梦数据库适配器
 * <p>
 * 差异要点：
 * - 整数类型不带显示宽度（INT / BIGINT，而非 INT(11) / BIGINT(20)）
 * - MODIFY COLUMN 只改列属性，ALTER COLUMN 改类型（兼容写法用 MODIFY COLUMN）
 * - DROP INDEX 后不接 ON table
 * - 无 ENGINE=InnoDB，表注释用 COMMENT ON TABLE
 * </p>
 */
@Component
public class DmDatabaseAdapter implements DatabaseAdapter {

    @Override
    public String getDatabaseType() {
        return "dm";
    }

    @Override
    public String buildCreateTableSql(String tableName, List<ColumnDef> columns, String comment) {
        return buildCreateTableSql(tableName, columns, List.of(), comment);
    }

    @Override
    public String buildCreateTableSql(String tableName, List<ColumnDef> columns, List<IndexDef> indexes, String comment) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(tableName).append(" (\n");

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

        // 索引
        for (IndexDef idx : indexes) {
            sb.append("  , ");
            if ("UNIQUE".equalsIgnoreCase(idx.getIndexType())) {
                sb.append("CONSTRAINT ").append(idx.getIndexName()).append(" UNIQUE (").append(idx.getColumnName()).append(")");
            } else {
                sb.append("KEY ").append(idx.getIndexName()).append(" (").append(idx.getColumnName()).append(")");
            }
            sb.append("\n");
        }

        sb.append(")");
        if (comment != null && !comment.isEmpty()) {
            sb.append(";\n");
            sb.append("COMMENT ON TABLE ").append(tableName).append(" IS '").append(comment).append("'");
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
        // 达梦 ALTER TABLE t MODIFY COLUMN col TYPE 与 MySQL 兼容
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
        // 达梦 DROP INDEX 不需要 ON table
        return "DROP INDEX " + indexName + ";";
    }

    @Override
    public String mapToColumnType(String fieldType, Integer length) {
        return switch (fieldType != null ? fieldType.toLowerCase() : "varchar") {
            case "int", "integer" -> "INT";
            case "bigint" -> "BIGINT";
            case "datetime", "date" -> "DATETIME";
            case "decimal" -> "DECIMAL(" + (length != null && length > 0 ? length : 18) + ",2)";
            case "text", "longtext" -> "TEXT";
            default -> "VARCHAR(" + (length != null && length > 0 ? length : 255) + ")";
        };
    }

    @Override
    public String getTableInfoSql(String tableName) {
        return "SELECT TABLE_NAME FROM ALL_TABLES WHERE OWNER = CURRENT_SCHMEA AND TABLE_NAME = '" + tableName + "'";
    }

    @Override
    public String getColumnInfoSql(String tableName) {
        return "SELECT COLUMN_NAME, DATA_TYPE AS COLUMN_TYPE, NULLABLE AS IS_NULLABLE, "
                + "DATA_DEFAULT AS COLUMN_DEFAULT, CHAR_LENGTH AS CHARACTER_MAXIMUM_LENGTH, DATA_TYPE "
                + "FROM ALL_TAB_COLUMNS WHERE OWNER = CURRENT_SCHMEA AND TABLE_NAME = '" + tableName + "'";
    }

    // ========== 内部辅助 ==========

    private String mapToColumnType(ColumnDef column) {
        return mapToColumnType(column.getFieldType(), column.getFieldLength());
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
